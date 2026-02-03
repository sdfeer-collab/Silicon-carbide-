package com.mindplus.optimizer.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FPS 监控器
 * 检测 FPS 下降，决定是否启用多进程渲染
 */
public class FPSMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("FPSMonitor");

    // 配置参数
    private static final int CRITICAL_FPS_THRESHOLD = 15; // 临界 FPS 阈值（低于此值停止多进程）
    private static final int RECOVERY_FPS_THRESHOLD = 20; // 恢复 FPS 阈值（高于此值恢复多进程）
    private static final int MIN_FRAMES_TO_CHECK = 60; // 最小检查帧数
    private static final long FPS_CHECK_INTERVAL_MS = 500; // FPS 检查间隔（更频繁）

    private final long[] frameTimes;
    private int frameIndex;
    private long lastCheckTime;
    private double currentFPS;
    private boolean multiRenderingEnabled;

    public FPSMonitor() {
        this.frameTimes = new long[MIN_FRAMES_TO_CHECK];
        this.frameIndex = 0;
        this.lastCheckTime = System.currentTimeMillis();
        this.currentFPS = 60.0; // 默认 60 FPS
        this.multiRenderingEnabled = false;
    }

    /**
     * 更新 FPS 统计
     */
    public void updateFPS() {
        long currentTime = System.nanoTime();
        frameTimes[frameIndex] = currentTime;
        frameIndex = (frameIndex + 1) % MIN_FRAMES_TO_CHECK;

        // 定期检查 FPS
        if (currentTime - lastCheckTime >= FPS_CHECK_INTERVAL_MS * 1_000_000L) {
            calculateFPS();
            lastCheckTime = currentTime;
        }
    }

    /**
     * 计算 FPS 并决定是否启用多进程渲染
     */
    private void calculateFPS() {
        if (frameIndex == 0) return;

        long totalNs = 0;
        int count = 0;

        for (int i = 0; i < MIN_FRAMES_TO_CHECK; i++) {
            if (frameTimes[i] == 0) break;

            int nextIndex = (i + 1) % MIN_FRAMES_TO_CHECK;
            if (frameTimes[nextIndex] == 0) break;

            long diff = frameTimes[nextIndex] - frameTimes[i];
            if (diff > 0) {
                totalNs += diff;
                count++;
            }
        }

        if (count > 0) {
            double avgFrameTimeNs = totalNs / count;
            currentFPS = 1_000_000_000.0 / avgFrameTimeNs;

            // 智能调度：FPS < 15 时停止多进程（减轻负担），FPS >= 20 时恢复多进程
            if (multiRenderingEnabled && currentFPS < CRITICAL_FPS_THRESHOLD) {
                multiRenderingEnabled = false;
                LOGGER.warn("FPS critical ({:.1f}), stopping multi-process rendering to reduce load", currentFPS);
            } else if (!multiRenderingEnabled && currentFPS >= RECOVERY_FPS_THRESHOLD) {
                multiRenderingEnabled = true;
                LOGGER.info("FPS recovered to {:.1f}, resuming multi-process rendering", currentFPS);
            }
        }
    }

    /**
     * 获取当前 FPS
     */
    public double getCurrentFPS() {
        return currentFPS;
    }

    /**
     * 是否启用多进程渲染
     */
    public boolean isMultiRenderingEnabled() {
        return multiRenderingEnabled;
    }

    /**
     * 重置监控器
     */
    public void reset() {
        frameIndex = 0;
        for (int i = 0; i < MIN_FRAMES_TO_CHECK; i++) {
            frameTimes[i] = 0;
        }
        lastCheckTime = System.currentTimeMillis();
        currentFPS = 60.0;
        multiRenderingEnabled = false;
    }

    /**
     * 强制启用/禁用多进程渲染
     */
    public void setMultiRenderingEnabled(boolean enabled) {
        this.multiRenderingEnabled = enabled;
    }
}