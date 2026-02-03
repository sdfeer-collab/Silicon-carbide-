package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.renderer.IRenderer;
import com.mindplus.optimizer.renderer.RendererType;
import com.mindplus.optimizer.renderer.VulkanRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 多渲染器渲染进程
 * 支持 Vulkan、DirectX 12、OpenGL 和软渲染器
 * 支持超渲染功能：渲染距离、模拟距离、FOV 缩放、面朝方向渲染
 */
public class MultiRendererProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger("MultiRendererProcess");

    private static final String DEFAULT_RENDERER = "Vulkan";
    private static final int DEFAULT_PORT = 5580;
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    private final IPCChannel pullChannel;
    private final ScheduledExecutorService scheduler;
    private IRenderer renderer;
    private volatile boolean running = false;

    public MultiRendererProcess() {
        this.pullChannel = new IPCChannel(ZMQ.PULL, "tcp://*:" + DEFAULT_PORT);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start(String[] args) {
        // 解析参数
        RendererType rendererType = RendererType.fromString(
            args.length > 0 ? args[0] : DEFAULT_RENDERER
        );

        int width = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_WIDTH;
        int height = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_HEIGHT;

        // 初始化渲染器
        initializeRenderer(rendererType, width, height);

        // 启动 ZeroMQ 接收器
        pullChannel.bind();
        running = true;

        LOGGER.info("MultiRendererProcess started with {} renderer ({}x{})",
            rendererType.getName(), width, height);

        // 定时输出性能统计
        scheduler.scheduleAtFixedRate(this::printStats, 5, 5, TimeUnit.SECONDS);

        // 主循环
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] data = pullChannel.receive(ZMQ.NOBLOCK);
                if (data != null) {
                    String task = new String(data);
                    processTask(task);
                }
            } catch (Exception e) {
                // 忽略异常，继续运行
            }

            // 控制帧率，避免过载
            Thread.yield();
        }

        shutdown();
    }

    private void initializeRenderer(RendererType type, int width, int height) {
        switch (type) {
            case VULKAN:
                renderer = new VulkanRenderer();
                break;
            case DIRECTX_12:
                // DirectX 12 暂时使用 Vulkan 作为后端（未来可扩展）
                LOGGER.warn("DirectX 12 not yet implemented, falling back to Vulkan");
                renderer = new VulkanRenderer();
                break;
            case SOFTWARE:
                // 软渲染器使用简化实现
                LOGGER.warn("Software renderer not yet implemented, falling back to Vulkan");
                renderer = new VulkanRenderer();
                break;
            case OPENGL:
            default:
                // OpenGL 暂时使用 Vulkan 作为后端
                LOGGER.warn("OpenGL renderer not yet implemented, falling back to Vulkan");
                renderer = new VulkanRenderer();
                break;
        }

        if (renderer != null) {
            renderer.initialize(width, height);
        }
    }

    private void processTask(String task) {
        if (renderer == null) return;

        try {
            // 检查是否是超渲染指令
            if (task.startsWith("SUPER_RENDER:")) {
                handleSuperRenderCommand(task);
                return;
            }

            // 普通渲染任务
            String[] parts = task.split(",");
            if (parts.length >= 3) {
                int chunkX = Integer.parseInt(parts[0]);
                int chunkZ = Integer.parseInt(parts[1]);
                double cameraY = Double.parseDouble(parts[2]);

                // 开始帧
                renderer.beginFrame();

                // 渲染区块
                renderer.renderChunk(chunkX, chunkZ, cameraY);

                // 结束帧
                renderer.endFrame();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to process task: {}", task);
        }
    }

    /**
     * 处理超渲染指令
     * 格式: SUPER_RENDER:TYPE:VALUE
     * TYPE: RENDER_DISTANCE, SIMULATION_DISTANCE, FOV_ZOOM, LOOK_AHEAD
     */
    private void handleSuperRenderCommand(String command) {
        try {
            String[] parts = command.split(":");
            if (parts.length >= 3) {
                String type = parts[1];
                String valueStr = parts[2];

                switch (type) {
                    case "RENDER_DISTANCE":
                        int renderDist = Integer.parseInt(valueStr);
                        renderer.setRenderDistance(renderDist);
                        break;

                    case "SIMULATION_DISTANCE":
                        int simDist = Integer.parseInt(valueStr);
                        renderer.setSimulationDistance(simDist);
                        break;

                    case "FOV_ZOOM":
                        double fovZoom = Double.parseDouble(valueStr);
                        renderer.setFovZoom(fovZoom);
                        break;

                    case "LOOK_AHEAD":
                        int lookAhead = Integer.parseInt(valueStr);
                        renderer.setLookAheadDistance(lookAhead);
                        break;

                    default:
                        LOGGER.warn("Unknown super render command type: {}", type);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to process super render command: {}", command);
        }
    }

    private void printStats() {
        if (renderer != null) {
            IRenderer.RendererStats stats = renderer.getStats();
            LOGGER.info("Render Stats - FPS: {}, Chunks: {}, Frame Time: {}ns, Memory: {}KB",
                stats.fps(), stats.renderedChunks(), stats.frameTimeNs(),
                stats.memoryUsageBytes() / 1024);

            // 输出超渲染参数
            if (renderer instanceof VulkanRenderer vkRenderer) {
                LOGGER.info("Super Render - Render Dist: {}, Sim Dist: {}, FOV Zoom: {}, Look Ahead: {}",
                    vkRenderer.getRenderDistance(),
                    vkRenderer.getSimulationDistance(),
                    vkRenderer.getFovZoom(),
                    vkRenderer.getLookAheadDistance());
            }
        }
    }

    private void shutdown() {
        running = false;
        if (renderer != null) {
            renderer.shutdown();
        }
        pullChannel.close();
        scheduler.shutdown();
        LOGGER.info("MultiRendererProcess shutdown");
    }

    public static void main(String[] args) {
        MultiRendererProcess process = new MultiRendererProcess();
        process.start(args);
    }
}