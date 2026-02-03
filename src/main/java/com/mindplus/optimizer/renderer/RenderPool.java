package com.mindplus.optimizer.renderer;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.communication.IPCHandler;
import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.*;

/**
 * 多进程渲染池管理器
 * 管理多个渲染进程，分配任务并合并结果
 */
public class RenderPool {
    private static final Logger LOGGER = LoggerFactory.getLogger("RenderPool");

    // 配置参数
    private static final int MAX_RENDER_PROCESSES = 4; // 最大渲染进程数
    private static final int TASK_QUEUE_SIZE = 100; // 任务队列大小
    private static final int SIMULATION_DISTANCE = 12; // 模拟距离（区块数）

    private final ProcessManager processManager;
    private final IPCHandler ipcHandler; // 新的 IPC 处理器
    private final FPSMonitor fpsMonitor;
    private final BlockingQueue<RenderTask> taskQueue;
    private final Map<String, RenderResult> pendingResults; // 待合并的渲染结果
    private final ScheduledExecutorService scheduler;

    private volatile boolean running = false;
    private volatile boolean multiProcessEnabled = true;
    private int activeProcessCount = 0;
    private int simulationDistance = SIMULATION_DISTANCE;

    public RenderPool(ProcessManager processManager) {
        this.processManager = processManager;
        this.ipcHandler = new IPCHandler(5581); // 使用新的 IPC 处理器
        this.fpsMonitor = new FPSMonitor();
        this.taskQueue = new LinkedBlockingQueue<>(TASK_QUEUE_SIZE);
        this.pendingResults = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * 初始化渲染池
     */
    public void initialize() {
        ipcHandler.start();
        running = true;

        // 启动任务分发线程
        new Thread(this::taskDispatcher, "RenderPool-TaskDispatcher").start();

        // 定期检查 FPS 和清理
        scheduler.scheduleAtFixedRate(this::monitorAndAdjust, 500, 500, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupOldResults, 5, 5, TimeUnit.SECONDS);

        LOGGER.info("RenderPool initialized with simulation distance: {}", simulationDistance);
    }

    /**
     * 添加渲染任务
     */
    public void addRenderTask(int chunkX, int chunkZ, double cameraY) {
        RenderTask task = new RenderTask(chunkX, chunkZ, cameraY);
        if (!taskQueue.offer(task)) {
            // 队列满时丢弃最旧的任务
            taskQueue.poll();
            taskQueue.offer(task);
        }
    }

    /**
     * 获取渲染结果（用于合并到游戏）
     */
    public RenderResult getRenderResult(int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        return pendingResults.remove(key);
    }

    /**
     * 更新 FPS
     */
    public void updateFPS() {
        fpsMonitor.updateFPS();
        this.multiProcessEnabled = fpsMonitor.isMultiRenderingEnabled();
    }

    /**
     * 获取当前模拟距离
     */
    public int getSimulationDistance() {
        return simulationDistance;
    }

    /**
     * 设置模拟距离
     */
    public void setSimulationDistance(int distance) {
        this.simulationDistance = Math.max(4, Math.min(32, distance));
    }

    /**
     * 监控 FPS 并调整渲染策略
     */
    private void monitorAndAdjust() {
        updateFPS();

        if (!multiProcessEnabled) {
            // FPS < 15，停止多进程渲染
            if (activeProcessCount > 0) {
                LOGGER.warn("FPS too low, stopping all render processes");
                stopAllRenderProcesses();
            }
        } else {
            // FPS >= 15，根据负载调整进程数
            int targetProcesses = calculateTargetProcessCount();
            adjustProcessCount(targetProcesses);
        }
    }

    /**
     * 清理旧结果
     */
    private void cleanupOldResults() {
        pendingResults.entrySet().removeIf(entry -> pendingResults.size() > 50);
    }

    /**
     * 计算目标进程数
     */
    private int calculateTargetProcessCount() {
        int queueSize = taskQueue.size();
        double fps = fpsMonitor.getCurrentFPS();

        // 任务队列满时增加进程
        if (queueSize > TASK_QUEUE_SIZE * 0.8 && fps > 30) {
            return Math.min(MAX_RENDER_PROCESSES, activeProcessCount + 1);
        }
        // 任务队列空时减少进程
        if (queueSize < TASK_QUEUE_SIZE * 0.2 && activeProcessCount > 1) {
            return Math.max(1, activeProcessCount - 1);
        }
        // FPS 低时减少进程
        if (fps < 25 && activeProcessCount > 1) {
            return Math.max(1, activeProcessCount - 1);
        }

        return activeProcessCount;
    }

    /**
     * 调整进程数量
     */
    private void adjustProcessCount(int targetCount) {
        if (targetCount > activeProcessCount) {
            // 启动新进程
            for (int i = activeProcessCount; i < targetCount; i++) {
                startRenderProcess(i);
            }
        } else if (targetCount < activeProcessCount) {
            // 停止多余的进程
            for (int i = activeProcessCount - 1; i >= targetCount; i--) {
                stopRenderProcess(i);
            }
        }
    }

    /**
     * 启动渲染进程
     */
    private void startRenderProcess(int processId) {
        List<String> args = new ArrayList<>();
        args.add("Vulkan"); // 使用 Vulkan 渲染器
        args.add("1920");   // 渲染宽度
        args.add("1080");   // 渲染高度

        processManager.startProcess("render-" + processId,
            "com.mindplus.optimizer.workers.MultiRendererProcess", args);
        activeProcessCount++;
        LOGGER.info("Started render process {}, total: {}", processId, activeProcessCount);
    }

    /**
     * 停止渲染进程
     */
    private void stopRenderProcess(int processId) {
        processManager.stopProcess("render-" + processId);
        activeProcessCount--;
        LOGGER.info("Stopped render process {}, total: {}", processId, activeProcessCount);
    }

    /**
     * 停止所有渲染进程
     */
    private void stopAllRenderProcesses() {
        for (int i = 0; i < activeProcessCount; i++) {
            stopRenderProcess(i);
        }
    }

    /**
     * 任务分发线程
     */
    private void taskDispatcher() {
        while (running) {
            try {
                RenderTask task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null && multiProcessEnabled) {
                    // 分发任务到渲染进程
                    String taskData = task.chunkX + "," + task.chunkZ + "," + task.cameraY;
                    // 这里应该发送到对应的渲染进程，简化处理直接返回结果
                    processTaskDirectly(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 直接处理任务（简化版）
     */
    private void processTaskDirectly(RenderTask task) {
        // 模拟渲染结果
        byte[] vertexData = new byte[1024]; // 简化的顶点数据
        byte[] lightData = new byte[256];   // 简化的光照数据

        // 填充一些模拟数据
        Arrays.fill(vertexData, (byte) (task.chunkX & 0xFF));
        Arrays.fill(lightData, (byte) (task.chunkZ & 0xFF));

        RenderResult result = new RenderResult(
            task.chunkX,
            task.chunkZ,
            vertexData,
            lightData,
            System.nanoTime(),
            256 // 三角形数量
        );

        String key = task.chunkX + "," + task.chunkZ;
        pendingResults.put(key, result);
    }

    /**
     * 关闭渲染池
     */
    public void shutdown() {
        running = false;
        stopAllRenderProcesses();
        ipcHandler.shutdown();
        scheduler.shutdown();
        LOGGER.info("RenderPool shutdown");
    }

    /**
     * 获取统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
            activeProcessCount,
            taskQueue.size(),
            pendingResults.size(),
            fpsMonitor.getCurrentFPS(),
            multiProcessEnabled
        );
    }

    public static class PoolStats {
        public final int activeProcesses;
        public final int queuedTasks;
        public final int pendingResults;
        public final double fps;
        public final boolean multiProcessEnabled;

        public PoolStats(int activeProcesses, int queuedTasks, int pendingResults, double fps, boolean multiProcessEnabled) {
            this.activeProcesses = activeProcesses;
            this.queuedTasks = queuedTasks;
            this.pendingResults = pendingResults;
            this.fps = fps;
            this.multiProcessEnabled = multiProcessEnabled;
        }
    }

    /**
     * 渲染任务
     */
    private static class RenderTask {
        final int chunkX;
        final int chunkZ;
        final double cameraY;

        RenderTask(int chunkX, int chunkZ, double cameraY) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.cameraY = cameraY;
        }
    }
}