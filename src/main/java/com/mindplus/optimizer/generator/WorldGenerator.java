package com.mindplus.optimizer.generator;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.process.ProcessManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("WorldGenerator");

    private final IPCChannel pushChannel;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Long> generatingChunks = new ConcurrentHashMap<>();

    private Object server;

    public WorldGenerator() {
        this.pushChannel = new IPCChannel(ZMQ.PUSH, "tcp://localhost:5570");
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void initialize(Object server) {
        this.server = server;
        pushChannel.connect();
        running.set(true);

        LOGGER.info("World generator initialized");
    }

    public void generateChunk(int chunkX, int chunkZ) {
        if (!running.get() || server == null) return;

        try {
            // 使用反射获取 overworld
            ServerWorld world = (ServerWorld) server.getClass().getMethod("getOverworld").invoke(server);
            if (world == null) return;

            String chunkKey = chunkX + "," + chunkZ;

            // 创建生成任务（不检查是否已生成，追求速度）
            String taskData = chunkX + "," + chunkZ + "," + world.getSeed();

            try {
                pushChannel.send(taskData.getBytes(), ZMQ.NOBLOCK);
                generatingChunks.put(chunkKey, System.currentTimeMillis());
            } catch (Exception e) {
                // 静默失败
            }
        } catch (Exception e) {
            LOGGER.error("Error in generateChunk", e);
        }
    }

    public void markChunkGenerated(String chunkKey) {
        generatingChunks.remove(chunkKey);
    }

    public boolean isGenerating(String chunkKey) {
        return generatingChunks.containsKey(chunkKey);
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();

        // 清理超过30秒的生成任务（延长超时时间以允许更长时间的处理）
        generatingChunks.entrySet().removeIf(entry ->
            currentTime - entry.getValue() > 30000
        );
    }

    public void shutdown() {
        running.set(false);
        scheduler.shutdown();
        pushChannel.close();
        LOGGER.info("World generator shutdown");
    }

    public GeneratorStats getStats() {
        return new GeneratorStats(generatingChunks.size());
    }

    public static class GeneratorStats {
        public final int generating;

        public GeneratorStats(int generating) {
            this.generating = generating;
        }
    }
}