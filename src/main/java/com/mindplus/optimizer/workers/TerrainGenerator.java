package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.tasks.ChunkResult;
import com.mindplus.optimizer.tasks.ChunkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class TerrainGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerrainGenerator");
    
    private final IPCChannel channel;
    private volatile boolean running = true;
    
    public TerrainGenerator() {
        this.channel = new IPCChannel(ZMQ.REP, "tcp://*:5556");
    }
    
    public void start() {
        channel.bind();
        LOGGER.info("Terrain Generator started");
        
        while (running) {
            byte[] request = channel.receive();
            if (request != null) {
                processRequest(request);
            }
        }
        
        channel.close();
    }
    
    private void processRequest(byte[] request) {
        try {
            // 解析任务
            ChunkTask task = ChunkTask.deserialize(new String(request));
            LOGGER.info("Processing chunk at ({}, {})", task.getChunkX(), task.getChunkZ());
            
            // 模拟地形生成处理
            Thread.sleep(10); // 模拟计算时间
            
            // 构建响应
            ChunkResult result = new ChunkResult(true, 
                "Terrain generated for chunk (" + task.getChunkX() + ", " + task.getChunkZ() + ")");
            
            channel.send(result.serialize().getBytes());
        } catch (Exception e) {
            LOGGER.error("Failed to process task", e);
            ChunkResult errorResult = new ChunkResult(false, "Invalid task format");
            channel.send(errorResult.serialize().getBytes());
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public static void main(String[] args) {
        TerrainGenerator generator = new TerrainGenerator();
        generator.start();
    }
}