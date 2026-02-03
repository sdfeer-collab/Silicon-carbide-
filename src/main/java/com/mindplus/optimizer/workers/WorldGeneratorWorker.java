package com.mindplus.optimizer.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldGeneratorWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger("WorldGeneratorWorker");
    
    private static final int THREAD_COUNT = 8;
    private static final int NOISE_SAMPLES = 512;
    private static final int FEATURE_POINTS = 128;
    
    private final ExecutorService executor;
    private final ZContext context;
    private final ZMQ.Socket pullSocket;
    private final Random random;
    
    public WorldGeneratorWorker() {
        this.context = new ZContext();
        this.pullSocket = context.createSocket(ZMQ.PULL);
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
        this.random = new Random();
    }
    
    public void start() {
        pullSocket.bind("tcp://*:5570");
        LOGGER.info("World generator worker started on port 5570");
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] data = pullSocket.recv(ZMQ.NOBLOCK);
                if (data != null) {
                    String task = new String(data);
                    executor.submit(() -> processTask(task));
                }
            } catch (Exception e) {
                // 忽略异常，继续运行
            }
        }
        
        shutdown();
    }
    
    private void processTask(String task) {
        try {
            String[] parts = task.split(",");
            if (parts.length >= 3) {
                int chunkX = Integer.parseInt(parts[0]);
                int chunkZ = Integer.parseInt(parts[1]);
                long seed = Long.parseLong(parts[2]);
                
                // 生成区块噪声和特征点
                generateChunkData(chunkX, chunkZ, seed);
            }
        } catch (Exception e) {
            // 静默处理错误
        }
    }
    
    private void generateChunkData(int chunkX, int chunkZ, long seed) {
        // 生成噪声样本
        for (int i = 0; i < NOISE_SAMPLES; i++) {
            double nx = (chunkX * 16 + random.nextDouble() * 16) / 256.0;
            double nz = (chunkZ * 16 + random.nextDouble() * 16) / 256.0;
            
            // 简单的噪声计算
            double value = Math.sin(nx * seed * 0.01) * Math.cos(nz * seed * 0.01);
        }
        
        // 生成特征点
        for (int i = 0; i < FEATURE_POINTS; i++) {
            double fx = chunkX * 16 + random.nextDouble() * 16;
            double fz = chunkZ * 16 + random.nextDouble() * 16;
            
            // 简单的特征计算
            double feature = Math.sqrt((fx - chunkX * 8) * (fx - chunkX * 8) + 
                                       (fz - chunkZ * 8) * (fz - chunkZ * 8));
        }
    }
    
    private void shutdown() {
        executor.shutdown();
        pullSocket.close();
        context.close();
        LOGGER.info("World generator worker stopped");
    }
    
    public static void main(String[] args) {
        WorldGeneratorWorker worker = new WorldGeneratorWorker();
        worker.start();
    }
}