package com.mindplus.optimizer.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基础渲染器接口
 */
public interface IRenderer {
    record RendererStats(
        int fps,
        int renderedChunks,
        long frameTimeNs,
        long memoryUsageBytes
    ) {}

    /**
     * 初始化渲染器
     */
    void initialize(int width, int height);

    /**
     * 开始渲染帧
     */
    void beginFrame();

    /**
     * 渲染区块
     * @param chunkX 区块 X 坐标
     * * @param chunkZ 区块 Z 坐标
     * @param cameraY 摄像机 Y 坐标
     */
    void renderChunk(int chunkX, int chunkZ, double cameraY);

    /**
     * 结束渲染帧
     */
    void endFrame();

    /**
     * 获取性能统计
     */
    RendererStats getStats();

    /**
     * 关闭渲染器
     */
    void shutdown();

    /**
     * 设置渲染距离（用于超渲染）
     */
    void setRenderDistance(int distance);

    /**
     * 设置模拟距离（用于超渲染）
     */
    void setSimulationDistance(int distance);

    /**
     * 设置 FOV 缩放倍数
     */
    void setFovZoom(double zoom);

    /**
     * 应用面朝方向渲染（用于 /long 命令）
     */
    void setLookAheadDistance(int distance);
}