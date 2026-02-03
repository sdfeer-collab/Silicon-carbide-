package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.tasks.AIResult;
import com.mindplus.optimizer.tasks.AITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("AIProcessor");
    private static final Random random = new Random();
    
    // AI 计算参数
    private static final int PATHFINDING_ITERATIONS = 100;
    private static final int THREAT_SCAN_RADIUS = 32;
    private static final int INTEREST_POINTS = 20;
    
    public static void main(String[] args) {
        LOGGER.info("AI Processor Worker started");
        
        IPCChannel channel = new IPCChannel(ZMQ.PULL, "tcp://*:5559");
        channel.bind();
        
        LOGGER.info("AI processor listening on port 5559");
        
        while (true) {
            try {
                byte[] request = channel.receive(ZMQ.NOBLOCK);
                if (request != null) {
                    processAITask(request);
                }
            } catch (Exception e) {
                // No message available, continue
            }
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        channel.close();
    }
    
    private static void processAITask(byte[] request) {
        try {
            AITask task = AITask.deserialize(new String(request));
            
            // 执行真正耗时的 AI 计算
            AIResult result = performExpensiveAICalculation(task);
            
            LOGGER.debug("AI processed for entity {} ({})", task.getEntityId(), task.getEntityType());
        } catch (Exception e) {
            LOGGER.error("Failed to process AI task", e);
        }
    }
    
    private static AIResult performExpensiveAICalculation(AITask task) {
        // 1. 威胁评估 - 扫描周围环境
        double threatLevel = assessThreatLevel(task);
        
        // 2. 路径规划 - 计算到目标的路径
        List<double[]> path = findPath(task);
        
        // 3. 行为决策 - 根据威胁和路径决定行为
        return makeDecision(task, threatLevel, path);
    }
    
    private static double assessThreatLevel(AITask task) {
        double totalThreat = 0;
        
        // 模拟扫描周围的实体
        for (int i = 0; i < INTEREST_POINTS; i++) {
            double angle = (i / (double) INTEREST_POINTS) * Math.PI * 2;
            double scanX = task.getPosX() + Math.cos(angle) * THREAT_SCAN_RADIUS;
            double scanZ = task.getPosZ() + Math.sin(angle) * THREAT_SCAN_RADIUS;
            
            // 模拟噪声扫描
            double noise = Math.sin(scanX * 0.1 + scanZ * 0.1) * Math.cos(scanX * 0.05);
            
            // 模拟距离衰减
            double distance = Math.sqrt(Math.pow(scanX - task.getPosX(), 2) + Math.pow(scanZ - task.getPosZ(), 2));
            double threat = Math.abs(noise) * (1 - distance / THREAT_SCAN_RADIUS);
            
            totalThreat += threat;
        }
        
        return totalThreat / INTEREST_POINTS;
    }
    
    private static List<double[]> findPath(AITask task) {
        List<double[]> path = new ArrayList<>();
        double currentX = task.getPosX();
        double currentZ = task.getPosZ();
        
        // 模拟 A* 路径搜索的迭代过程
        for (int i = 0; i < PATHFINDING_ITERATIONS; i++) {
            // 评估多个可能的移动方向
            double bestCost = Double.MAX_VALUE;
            double bestX = currentX;
            double bestZ = currentZ;
            
            for (int dir = 0; dir < 8; dir++) {
                double angle = (dir / 8.0) * Math.PI * 2;
                double candidateX = currentX + Math.cos(angle);
                double candidateZ = currentZ + Math.sin(angle);
                
                // 计算移动成本
                double terrainCost = Math.sin(candidateX * 0.2) * Math.cos(candidateZ * 0.2) * 2;
                double distanceCost = Math.sqrt(Math.pow(candidateX - task.getPosX(), 2) + Math.pow(candidateZ - task.getPosZ(), 2));
                
                double totalCost = terrainCost + distanceCost * 0.1;
                
                if (totalCost < bestCost) {
                    bestCost = totalCost;
                    bestX = candidateX;
                    bestZ = candidateZ;
                }
            }
            
            currentX = bestX;
            currentZ = bestZ;
            path.add(new double[]{currentX, task.getPosY(), currentZ});
        }
        
        return path;
    }
    
    private static AIResult makeDecision(AITask task, double threatLevel, List<double[]> path) {
        // 基于威胁水平和路径决定是否移动
        double moveProbability = 0.3 + threatLevel * 0.5; // 威胁越高，移动概率越大
        
        if (path.size() > 0 && random.nextDouble() < moveProbability) {
            // 选择路径中的一个点作为目标
            int targetIndex = Math.min(path.size() - 1, random.nextInt(path.size()) + 5);
            double[] target = path.get(targetIndex);
            
            return new AIResult(true, target[0], target[1], target[2]);
        }
        
        return new AIResult(false, task.getPosX(), task.getPosY(), task.getPosZ());
    }
}