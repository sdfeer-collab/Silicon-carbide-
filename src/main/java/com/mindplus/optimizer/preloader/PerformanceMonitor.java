package com.mindplus.optimizer.preloader;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceMonitor");
    
    private double currentFPS = 60.0;
    private double targetFPS = 60.0;
    private long lastFrameTime = System.currentTimeMillis();
    private int frameCount = 0;
    private long lastCheckTime = System.currentTimeMillis();
    
    // 性能阈值
    private static final double FPS_CRITICAL = 38.0;  // 严重下降阈值
    private static final double FPS_WARNING = 70.0;  // 警告阈值
    private static final double FPS_GOOD = 70.0;     // 恢复阈值
    
    // 状态
    private PerformanceStatus status = PerformanceStatus.GOOD;
    
    public enum PerformanceStatus {
        GOOD,       // FPS >= 55，正常预加载
        WARNING,    // 45 <= FPS < 55，减少预加载
        CRITICAL    // FPS < 30，停止所有预加载
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        frameCount++;
        
        // 每秒更新一次 FPS
        if (currentTime - lastCheckTime >= 1000) {
            currentFPS = frameCount * 1000.0 / (currentTime - lastCheckTime);
            
            // 更新状态
            updateStatus();
            
            frameCount = 0;
            lastCheckTime = currentTime;
        }
        
        lastFrameTime = currentTime;
    }
    
    private void updateStatus() {
        PerformanceStatus newStatus = status;
        
        if (currentFPS < FPS_CRITICAL) {
            newStatus = PerformanceStatus.CRITICAL;
        } else if (currentFPS < FPS_WARNING) {
            newStatus = PerformanceStatus.WARNING;
        } else if (currentFPS >= FPS_GOOD && status != PerformanceStatus.GOOD) {
            newStatus = PerformanceStatus.GOOD;
        }
        
        // 状态变化时记录日志
        if (newStatus != status) {
            LOGGER.info("Performance status changed: {} -> {} (FPS: {:.1f})", 
                status, newStatus, currentFPS);
            status = newStatus;
        }
    }
    
    public PerformanceStatus getStatus() {
        return status;
    }
    
    public double getCurrentFPS() {
        return currentFPS;
    }
    
    public boolean shouldPreload() {
        return status != PerformanceStatus.CRITICAL;
    }
    
    public boolean shouldFullPreload() {
        return status == PerformanceStatus.GOOD;
    }
    
    public int getPreloadMultiplier() {
        switch (status) {
            case GOOD:
                return 1;      // 100% 预加载
            case WARNING:
                return 2;      // 50% 预加载
            case CRITICAL:
                return 0;      // 0% 预加载
            default:
                return 1;
        }
    }
    
    public boolean isLowPerformance() {
        return status == PerformanceStatus.CRITICAL;
    }
}