package com.mindplus.optimizer.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "super_render")
public class SuperRenderConfig implements ConfigData {
    // 基础渲染距离设置（最大150区块）
    @ConfigEntry.Gui.Excluded
    public int renderDistance = 150;

    // 模拟距离设置（最大150区块）
    @ConfigEntry.Gui.Excluded
    public int simulationDistance = 150;

    // 长距离模式视距（最大2000区块，/long命令专用）
    public int longDistanceRender = 2000;

    // 缩放级别（支持到2000区块）
    public double zoomLevel = 1.0;

    // 是否启用超渲染
    public boolean enabled = true;

    // 缩放倍数范围（最大200支持2000区块渲染）
    public double minZoom = 0.5;
    public double maxZoom = 200.0;

    // 当前缩放状态
    @ConfigEntry.Gui.Excluded
    private transient boolean isZooming = false;

    // 默认 FOV
    @ConfigEntry.Gui.Excluded
    public transient double defaultFov = 70.0;

    public static SuperRenderConfig getInstance() {
        return SuperRenderConfigManager.getInstance();
    }

    public boolean isZooming() {
        return isZooming;
    }

    public void setZooming(boolean zooming) {
        this.isZooming = zooming;
    }

    public void increaseZoom() {
        if (zoomLevel < maxZoom) {
            // 大幅缩放时使用更大的步进值
            if (zoomLevel < 10) {
                zoomLevel += 0.5;
            } else if (zoomLevel < 50) {
                zoomLevel += 2.0;
            } else if (zoomLevel < 100) {
                zoomLevel += 5.0;
            } else {
                zoomLevel += 10.0;
            }
        }
    }

    public void decreaseZoom() {
        if (zoomLevel > minZoom) {
            // 大幅缩放时使用更大的步进值
            if (zoomLevel <= 10) {
                zoomLevel -= 0.5;
            } else if (zoomLevel <= 50) {
                zoomLevel -= 2.0;
            } else if (zoomLevel <= 100) {
                zoomLevel -= 5.0;
            } else {
                zoomLevel -= 10.0;
            }
        }
    }

    public void resetZoom() {
        zoomLevel = 1.0;
    }
}