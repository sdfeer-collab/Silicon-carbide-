package com.mindplus.optimizer.renderer;

import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.process.ProcessManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenderOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger("RenderOptimizer");

    private final IPCChannel pushChannel;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Long> renderingChunks = new ConcurrentHashMap<>();

    private Object client;
    private RendererType rendererType;
    private RenderPool renderPool; // 多进程渲染池
    private boolean useRenderPool = false; // 是否使用渲染池

    public RenderOptimizer() {
        this.pushChannel = new IPCChannel(ZMQ.PUSH, "tcp://localhost:5580");
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.rendererType = RendererType.VULKAN;
    }

    public void initialize(Object client) {
        this.client = client;

        // 从配置读取渲染器类型
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.runtime != null) {
            this.rendererType = RendererType.fromString(ModConfig.INSTANCE.runtime.rendererType);
            this.useRenderPool = ModConfig.INSTANCE.runtime.enableRenderProcess;
        }

        pushChannel.connect();
        running.set(true);

        // 高频渲染任务 - 60 FPS
        scheduler.scheduleAtFixedRate(this::scheduleRenderTasks, 0, 16, TimeUnit.MILLISECONDS);

        // 清理任务
        scheduler.scheduleAtFixedRate(this::cleanupOldTasks, 0, 5, TimeUnit.SECONDS);

        // 性能统计输出
        scheduler.scheduleAtFixedRate(this::printStats, 5, 5, TimeUnit.SECONDS);

        LOGGER.info("Render optimizer initialized with {} renderer at 60 FPS, RenderPool: {}",
            rendererType.getName(), useRenderPool);
    }

    /**
     * 初始化渲染池（需要在 ProcessManager 可用时调用）
     */
    public void initializeRenderPool(ProcessManager processManager) {
        if (useRenderPool) {
            this.renderPool = new RenderPool(processManager);
            this.renderPool.initialize();
            LOGGER.info("RenderPool initialized");
        }
    }

    private void scheduleRenderTasks() {
        if (!running.get() || client == null) return;

        try {
            // 使用反射访问 client.world
            Object world = client.getClass().getMethod("getWorld").invoke(client);
            if (world == null) return;

            // 使用反射获取相机实体
            Object cameraEntity = client.getClass().getMethod("getCameraEntity").invoke(client);
            if (cameraEntity == null) return;

            // 使用反射获取相机位置
            Class<?> entityClass = cameraEntity.getClass();
            Object blockPos = entityClass.getMethod("getBlockPos").invoke(cameraEntity);
            Class<?> blockPosClass = blockPos.getClass();
            int x = (int) blockPosClass.getMethod("getX").invoke(blockPos);
            int z = (int) blockPosClass.getMethod("getZ").invoke(blockPos);

            ChunkPos cameraChunk = new ChunkPos(x, z);

            // 使用反射获取渲染距离
            Object options = client.getClass().getMethod("getMethod", String.class).invoke(client, "options");
            Object gameOptions = client.getClass().getMethod("getOptions").invoke(client);
            Object viewDistance = gameOptions.getClass().getMethod("getViewDistance").invoke(gameOptions);
            int renderDistance = (int) viewDistance.getClass().getMethod("getValue").invoke(viewDistance);

            // 根据是否使用渲染池和 FPS 状态决定渲染范围
            int maxRenderRadius;
            if (useRenderPool && renderPool != null) {
                // 使用渲染池时，根据模拟距离渲染
                maxRenderRadius = renderPool.getSimulationDistance();
            } else if (rendererType == RendererType.VULKAN || rendererType == RendererType.DIRECTX_12) {
                maxRenderRadius = Math.min(renderDistance + 4, 32);
            } else {
                maxRenderRadius = Math.min(renderDistance + 2, 24);
            }

            // 优化渲染顺序 - 从相机位置向外扩展
            int radius = 0;
            while (radius <= maxRenderRadius) {
                for (int cx = -radius; cx <= radius; cx++) {
                    for (int cz = -radius; cz <= radius; cz++) {
                        if (Math.abs(cx) != radius && Math.abs(cz) != radius) continue;

                        ChunkPos chunkPos = new ChunkPos(cameraChunk.x + cx, cameraChunk.z + cz);
                        String chunkKey = chunkPos.toString();

                        if (renderingChunks.containsKey(chunkKey)) {
                            continue;
                        }

                        // 获取实体 Y 坐标
                        double entityY = (double) entityClass.getMethod("getY").invoke(cameraEntity);

                        // 如果使用渲染池，添加到渲染池
                        if (useRenderPool && renderPool != null) {
                            renderPool.addRenderTask(chunkPos.x, chunkPos.z, entityY);
                        } else {
                            // 否则直接发送到渲染进程
                            String taskData = chunkPos.x + "," + chunkPos.z + "," + entityY;
                            try {
                                pushChannel.send(taskData.getBytes(), ZMQ.NOBLOCK);
                                renderingChunks.put(chunkKey, System.currentTimeMillis());
                            } catch (Exception e) {
                                // 静默失败
                            }
                        }
                    }
                }
                radius++;
            }
        } catch (Exception e) {
            LOGGER.error("Error in scheduleRenderTasks", e);
        }
    }

    private void cleanupOldTasks() {
        long currentTime = System.currentTimeMillis();

        // 动态清理时间
        long cleanupThreshold = (useRenderPool && renderPool != null) ? 5000 : 10000;

        renderingChunks.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > cleanupThreshold
        );
    }

    private void printStats() {
        if (useRenderPool && renderPool != null) {
            RenderPool.PoolStats stats = renderPool.getStats();
            LOGGER.info("RenderPool - FPS: {:.1f}, Processes: {}, Queue: {}, Results: {}, MultiProcess: {}",
                stats.fps, stats.activeProcesses, stats.queuedTasks, stats.pendingResults, stats.multiProcessEnabled);
        } else {
            LOGGER.info("RenderOptimizer - Rendering: {}, Type: {}",
                renderingChunks.size(), rendererType.getName());
        }
    }

    public void markRenderComplete(String chunkKey) {
        renderingChunks.remove(chunkKey);
    }

    public void shutdown() {
        running.set(false);
        scheduler.shutdown();

        if (renderPool != null) {
            renderPool.shutdown();
        }

        pushChannel.close();
        LOGGER.info("Render optimizer shutdown");
    }

    public RenderStats getStats() {
        return new RenderStats(renderingChunks.size(), rendererType, useRenderPool && renderPool != null);
    }

    public RendererType getRendererType() {
        return rendererType;
    }

    public RenderPool getRenderPool() {
        return renderPool;
    }

    public static class RenderStats {
        public final int rendering;
        public final RendererType rendererType;
        public final boolean usingRenderPool;

        public RenderStats(int rendering, RendererType rendererType, boolean usingRenderPool) {
            this.rendering = rendering;
            this.rendererType = rendererType;
            this.usingRenderPool = usingRenderPool;
        }
    }
}