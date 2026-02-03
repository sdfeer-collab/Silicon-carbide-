package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.tasks.ChunkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChunkPreloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkPreloader");
    private static final Random random = new Random();
    
    private static final int THREAD_COUNT = 8;
    private static final int NOISE_SAMPLES = 512;
    private static final int FEATURE_POINTS = 128;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
    
    public static void main(String[] args) {
        LOGGER.info("Chunk Preloader Worker started");
        
        IPCChannel channel = new IPCChannel(ZMQ.PULL, "tcp://*:5560");
        channel.bind();
        
        LOGGER.info("Chunk preloader listening on port 5560 with {} threads", THREAD_COUNT);
        
        while (true) {
            try {
                byte[] request = channel.receive(ZMQ.NOBLOCK);
                if (request != null) {
                    // 异步处理区块任务
                    final byte[] taskData = request;
                    threadPool.submit(() -> {
                        try {
                            processChunkRequest(taskData);
                        } catch (Exception e) {
                            LOGGER.error("Error processing chunk task", e);
                        }
                    });
                }
            } catch (Exception e) {
                // No message available, continue
            }
            
            // 添加短暂延迟避免 CPU 占用过高
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        threadPool.shutdown();
        channel.close();
    }
    
    private static void processChunkRequest(byte[] request) {
        try {
            ChunkTask task = ChunkTask.deserialize(new String(request));
            
            // 执行真正耗时的计算（并行优化）
            performExpensiveCalculationParallel(task);
            
            LOGGER.debug("Preloaded chunk ({}, {})", task.getChunkX(), task.getChunkZ());
        } catch (Exception e) {
            LOGGER.error("Failed to process preload request", e);
        }
    }
    
    private static void performExpensiveCalculationParallel(ChunkTask task) {
        // 并行处理多个噪声层
        double[] noiseResults = new double[NOISE_SAMPLES];
        
        // 使用多线程计算噪声
        int samplesPerThread = NOISE_SAMPLES / THREAD_COUNT;
        
        for (int thread = 0; thread < THREAD_COUNT; thread++) {
            final int threadNum = thread;
            final int startIdx = threadNum * samplesPerThread;
            final int endIdx = startIdx + samplesPerThread;
            
            threadPool.submit(() -> {
                for (int i = startIdx; i < endIdx && i < NOISE_SAMPLES; i++) {
                    noiseResults[i] = simplexNoise(task.getChunkX() * 0.01, task.getChunkZ() * 0.01, i);
                    noiseResults[i] += simplexNoise(task.getChunkX() * 0.05, task.getChunkZ() * 0.05, i) * 0.5;
                    noiseResults[i] += simplexNoise(task.getChunkX() * 0.1, task.getChunkZ() * 0.1, i) * 0.25;
                    
                    // 应用幂函数模拟山脉
                    noiseResults[i] = Math.pow(Math.abs(noiseResults[i]), 1.5) * Math.signum(noiseResults[i]);
                }
            });
        }
        
        // 等待所有线程完成
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // 继续执行
        }
        
        // 并行处理特征点
        int featuresPerThread = FEATURE_POINTS / THREAD_COUNT;
        for (int thread = 0; thread < THREAD_COUNT; thread++) {
            final int threadNum = thread;
            final int startIdx = threadNum * featuresPerThread;
            final int endIdx = startIdx + featuresPerThread;
            
            threadPool.submit(() -> {
                for (int i = startIdx; i < endIdx && i < FEATURE_POINTS; i++) {
                    double fx = (task.getChunkX() * 16 + random.nextDouble() * 16);
                    double fz = (task.getChunkZ() * 16 + random.nextDouble() * 16);
                    double noise = simplexNoise(fx * 0.02, fz * 0.02, i + 1000);
                    
                    if (noise > 0.7) {
                        simulateStructureGeneration(task, i);
                    } else if (noise > 0.4) {
                        simulateCaveGeneration(task, i);
                    }
                }
            });
        }
    }
    
    private static double simplexNoise(double x, double y, int seed) {
        double n = Math.sin(x * 12.9898 + y * 78.233 + seed) * 43758.5453;
        n -= Math.floor(n);
        return n * 2 - 1;
    }
    
    private static void simulateStructureGeneration(ChunkTask task, int seed) {
        for (int i = 0; i < 10; i++) {
            double x = task.getChunkX() * 16 + Math.sin(seed + i) * 50;
            double z = task.getChunkZ() * 16 + Math.cos(seed + i) * 50;
            
            for (int j = 0; j < 5; j++) {
                double dx = x + Math.sin(j) * 10;
                double dz = z + Math.cos(j) * 10;
                simplexNoise(dx * 0.05, dz * 0.05, seed + j + 100);
            }
        }
    }
    
    private static void simulateCaveGeneration(ChunkTask task, int seed) {
        for (int y = 0; y < 64; y += 4) {
            double noise = simplexNoise(task.getChunkX() * 0.1, y * 0.1, seed + 200);
            if (noise > 0.5) {
                for (int i = 0; i < 3; i++) {
                    double nx = task.getChunkX() + Math.sin(i) * 0.5;
                    double nz = task.getChunkZ() + Math.cos(i) * 0.5;
                    simplexNoise(nx * 0.15, nz * 0.15, seed + i + 300);
                }
            }
        }
    }
}