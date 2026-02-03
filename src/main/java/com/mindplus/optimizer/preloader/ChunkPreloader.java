package com.mindplus.optimizer.preloader;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.tasks.ChunkTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkPreloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkPreloader");

    private final IPCChannel pushChannel;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Long> pendingChunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChunkData> preloadedChunks = new ConcurrentHashMap<>();

    private Object server;

    public ChunkPreloader() {
        this.pushChannel = new IPCChannel(ZMQ.PUSH, "tcp://localhost:5560");
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void initialize(Object server) {
        this.server = server;
        pushChannel.connect();
        running.set(true);

        // 高频预加载，追求速度
        scheduler.scheduleAtFixedRate(this::preloadAroundPlayer, 0, 50, TimeUnit.MILLISECONDS);

        // 启动清理任务
        scheduler.scheduleAtFixedRate(this::cleanupOldChunks, 0, 10, TimeUnit.SECONDS);

        // 高频预测性预加载
        scheduler.scheduleAtFixedRate(this::predictivePreload, 0, 100, TimeUnit.MILLISECONDS);

        LOGGER.info("Chunk preloader initialized with maximum speed");
    }

    private void preloadAroundPlayer() {
        if (!running.get() || server == null) return;

        try {
            // 使用反射获取 overworld
            ServerWorld world = (ServerWorld) server.getClass().getMethod("getOverworld").invoke(server);
            if (world == null) return;

            // 快速检查玩家数量，如果太多玩家则减少预加载
            int playerCount = world.getPlayers().size();
            if (playerCount > 4) {
                return;
            }

            // 最大预加载半径
            int preloadRadius = 10;

            // 获取玩家位置
            world.getPlayers().forEach(player -> {
                ChunkPos playerChunk = new ChunkPos(player.getBlockPos());

                // 预加载玩家周围的区块
                for (int x = -preloadRadius; x <= preloadRadius; x++) {
                    for (int z = -preloadRadius; z <= preloadRadius; z++) {
                        ChunkPos chunkPos = new ChunkPos(playerChunk.x + x, playerChunk.z + z);
                        String chunkKey = chunkPos.toString();

                        // 快速检查，避免复杂操作
                        if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                            continue;
                        }
                        if (pendingChunks.containsKey(chunkKey)) {
                            continue;
                        }

                        // 检查是否已经预生成过
                        if (preloadedChunks.containsKey(chunkKey)) {
                            ChunkData data = preloadedChunks.get(chunkKey);
                            if (System.currentTimeMillis() - data.timestamp > 60000) {
                                preloadedChunks.remove(chunkKey);
                            } else {
                                continue;
                            }
                        }

                        // 简化的任务创建
                        String taskData = chunkPos.x + "," + chunkPos.z + "," + world.getSeed();

                        try {
                            pushChannel.send(taskData.getBytes(), ZMQ.NOBLOCK);
                            pendingChunks.put(chunkKey, System.currentTimeMillis());
                        } catch (Exception e) {
                            // 静默失败，不影响性能
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in preloadAroundPlayer", e);
        }
    }

    private void cleanupOldChunks() {
        long currentTime = System.currentTimeMillis();

        // 清理超过60秒的预生成数据
        preloadedChunks.entrySet().removeIf(entry ->
            currentTime - entry.getValue().timestamp > 60000
        );

        // 清理超过30秒的待处理任务
        pendingChunks.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > 30000
        );
    }

    private void predictivePreload() {
        if (!running.get() || server == null) return;

        try {
            // 使用反射获取 overworld
            ServerWorld world = (ServerWorld) server.getClass().getMethod("getOverworld").invoke(server);
            if (world == null) return;

            // 基于玩家移动方向预测并预加载更多区块
            world.getPlayers().forEach(player -> {
                ChunkPos playerChunk = new ChunkPos(player.getBlockPos());

                // 获取玩家移动方向
                double dx = player.getVelocity().x;
                double dz = player.getVelocity().z;
                double speed = Math.sqrt(dx * dx + dz * dz);

                // 如果玩家在快速移动，预加载更远的区块
                if (speed > 0.1) {
                    int directionX = (int) Math.signum(dx);
                    int directionZ = (int) Math.signum(dz);
                    int extraRadius = 8; // 在移动方向上额外预加载

                    for (int i = 1; i <= extraRadius; i++) {
                        ChunkPos predictedChunk = new ChunkPos(
                            playerChunk.x + directionX * i,
                            playerChunk.z + directionZ * i
                        );

                        String chunkKey = predictedChunk.toString();

                        // 只处理未加载的区块
                        if (!world.getChunkManager().isChunkLoaded(predictedChunk.x, predictedChunk.z)
                            && !pendingChunks.containsKey(chunkKey)) {

                            String taskData = predictedChunk.x + "," + predictedChunk.z + "," + world.getSeed();

                            try {
                                pushChannel.send(taskData.getBytes(), ZMQ.NOBLOCK);
                                pendingChunks.put(chunkKey, System.currentTimeMillis());
                            } catch (Exception e) {
                                // 忽略
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in predictivePreload", e);
        }
    }

    public boolean isChunkPreloaded(String chunkKey) {
        return preloadedChunks.containsKey(chunkKey);
    }

    public void markChunkPreloaded(String chunkKey) {
        preloadedChunks.put(chunkKey, new ChunkData(System.currentTimeMillis()));
    }

    public void markChunkProcessingComplete(String chunkKey) {
        pendingChunks.remove(chunkKey);
    }

    public void shutdown() {
        running.set(false);
        scheduler.shutdown();
        pushChannel.close();
        LOGGER.info("Chunk preloader shutdown");
    }

    private static class ChunkData {
        final long timestamp;

        ChunkData(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    // 获取预加载统计信息
    public PreloadStats getStats() {
        return new PreloadStats(
            pendingChunks.size(),
            preloadedChunks.size()
        );
    }

    public static class PreloadStats {
        public final int pending;
        public final int preloaded;

        public PreloadStats(int pending, int preloaded) {
            this.pending = pending;
            this.preloaded = preloaded;
        }
    }
}