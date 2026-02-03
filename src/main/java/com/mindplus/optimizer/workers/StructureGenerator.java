package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.tasks.ChunkResult;
import com.mindplus.optimizer.tasks.ChunkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class StructureGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("StructureGenerator");
    
    private final IPCChannel channel;
    private volatile boolean running = true;
    
    public StructureGenerator() {
        this.channel = new IPCChannel(ZMQ.REP, "tcp://*:5555");
    }
    
    public void start() {
        channel.bind();
        LOGGER.info("Structure Generator started");
        
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
            ChunkTask task = ChunkTask.deserialize(new String(request));
            LOGGER.info("Generating structures for chunk ({}, {})", task.getChunkX(), task.getChunkZ());
            
            // 模拟结构生成处理
            Thread.sleep(5); // 模拟计算时间
            
            ChunkResult result = new ChunkResult(true, 
                "Structures generated for chunk (" + task.getChunkX() + ", " + task.getChunkZ() + ")");
            
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
        StructureGenerator generator = new StructureGenerator();
        generator.start();
    }
}