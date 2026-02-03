package com.mindplus.optimizer.client;

import com.mindplus.optimizer.config.SuperRenderConfig;
import com.mindplus.optimizer.config.SuperRenderConfigManager;
import com.mindplus.optimizer.command.SuperRenderCommand;

/**
 * 超渲染键盘和鼠标事件处理器
 * 由于类加载问题，输入处理已禁用
 * 使用代码 API 直接控制缩放
 */
public class SuperRenderInputHandler {

    private static boolean cKeyPressed = false;
    private static boolean initialized = false;
    private static SuperRenderController controller;

    public static void init(SuperRenderController controller) {
        SuperRenderInputHandler.controller = controller;
        initialized = true;
    }

    /**
     * 手动增加缩放
     */
    public static void increaseZoom() {
        SuperRenderConfig config = SuperRenderConfigManager.getSuperRenderConfig();
        if (config != null) {
            config.increaseZoom();
            updateRenderDistanceBasedOnZoom(config);
            if (controller != null) {
                controller.setFovZoom(config.zoomLevel);
            }
        }
    }

    /**
     * 手动减少缩放
     */
    public static void decreaseZoom() {
        SuperRenderConfig config = SuperRenderConfigManager.getSuperRenderConfig();
        if (config != null) {
            config.decreaseZoom();
            updateRenderDistanceBasedOnZoom(config);
            if (controller != null) {
                controller.setFovZoom(config.zoomLevel);
            }
        }
    }

    /**
     * 根据缩放级别更新渲染距离
     * 最大支持2000区块
     */
    private static void updateRenderDistanceBasedOnZoom(SuperRenderConfig config) {
        if (SuperRenderCommand.isLongModeActive()) {
            int newDistance = (int) (config.zoomLevel * 2000);
            newDistance = Math.max(150, Math.min(2000, newDistance));
            config.longDistanceRender = newDistance;
            if (controller != null) {
                controller.setLookAheadDistance(newDistance);
            }
        } else {
            int newDistance = (int) (config.zoomLevel * 150);
            newDistance = Math.max(2, Math.min(150, newDistance));
            config.renderDistance = newDistance;
            if (controller != null) {
                controller.setRenderDistance(newDistance);
            }
        }
    }

    public static boolean isCKeyPressed() {
        return cKeyPressed;
    }
}