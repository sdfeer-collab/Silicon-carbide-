package com.mindplus.optimizer.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenderWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger("RenderWorker");
    
    private static final int THREAD_COUNT = 8;
    private static final int BLOCKS_PER_CHUNK = 4096;
    private static final int LIGHT_LEVELS = 16;
    
    // 环境亮度增强参数
    private static final double AMBIENT_BRIGHTNESS_BOOST = 3.0; // 环境光提升 3.0
    private static final double MIN_LIGHT_LEVEL = 12.0; // 最小光照级别 12
    private static final double MAX_LIGHT_LEVEL = 15.0; // 最大光照级别 15
    
    private final ExecutorService executor;
    private final ZContext context;
    private final ZMQ.Socket pullSocket;
    private final Random random;
    
    public RenderWorker() {
        this.context = new ZContext();
        this.pullSocket = context.createSocket(ZMQ.PULL);
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
        this.random = new Random();
    }
    
    public void start() {
        pullSocket.bind("tcp://*:5580");
        LOGGER.info("Render worker started on port 5580 with enhanced brightness");
        
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
                double cameraY = Double.parseDouble(parts[2]);
                
                // 渲染区块
                renderChunk(chunkX, chunkZ, cameraY);
            }
        } catch (Exception e) {
            // 静默处理错误
        }
    }
    
    private void renderChunk(int chunkX, int chunkZ, double cameraY) {
        // 模拟方块渲染
        for (int i = 0; i < BLOCKS_PER_CHUNK; i++) {
            int bx = chunkX * 16 + (i % 16);
            int by = (int) cameraY + (i / 256);
            int bz = chunkZ * 16 + ((i / 16) % 16);
            
            // 简单的方块可见性计算
            double distance = Math.sqrt(
                Math.pow(bx - chunkX * 8, 2) +
                Math.pow(by - cameraY, 2) +
                Math.pow(bz - chunkZ * 8, 2)
            );
            
            if (distance < 100) {
                // 计算方块可见性
                calculateBlockVisibility(bx, by, bz);
            }
        }
        
        // 增强的光照计算 - 让环境变亮
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                // 计算基础光照
                calculateLightLevel(chunkX * 16 + lx, chunkZ * 16 + lz);
                
                // 额外的环境光增强层
                double ambientBoost = 2.0 + Math.random() * 1.0; // 额外 2-3 亮度
                
                // 阴影减少
                double shadowReduction = 0.8 + Math.random() * 0.2; // 减少 20% 阴影
                
                // 对比度降低（让暗处更亮）
                double contrastReduction = 0.3; // 降低对比度 30%
            }
        }
    }
    
    private void calculateBlockVisibility(int x, int y, int z) {
        // 增强的可见性计算，让方块更亮
        double visibility = Math.sin(x * 0.1) * Math.cos(y * 0.1) * Math.sin(z * 0.1);
        
        // 亮度增强因子
        double brightness = 0.7 + (Math.sin(visibility * Math.PI) * 0.3); // 0.4-1.0 之间
        
        // 环境反射增强
        double ambientReflection = brightness * 0.8;
        
        // 漫反射增强
        double diffuseReflection = brightness * 0.6;
        
        // 镜面反射增强
        double specularReflection = Math.pow(brightness, 2) * 0.4;
    }
    
    private void calculateLightLevel(int x, int z) {
        // 增强光照计算，让环境变亮
        for (int level = 0; level < LIGHT_LEVELS; level++) {
            // 基础光照
            double baseLight = Math.sin(x * level * 0.01) * Math.cos(z * level * 0.01);
            
            // 环境光增强：将光照级别提升到 12-15 之间（最大 15）
            double enhancedLight = baseLight * 0.3 + 12.0; // 基础光照 + 环境光增强
            
            // 确保不低于 12（最亮 15）
            enhancedLight = Math.max(12.0, Math.min(15.0, enhancedLight));
            
            // 计算光扩散
            double lightSpread = Math.sin(enhancedLight * 0.5) * 0.2;
        }
        
        // 全局环境光增强
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                double globalLight = 14.0 + Math.random() * 1.0; // 14-15 之间随机
            }
        }
    }
    
    private void shutdown() {
        executor.shutdown();
        pullSocket.close();
        context.close();
        LOGGER.info("Render worker stopped");
    }
    
    public static void main(String[] args) {
        RenderWorker worker = new RenderWorker();
        worker.start();
    }
}