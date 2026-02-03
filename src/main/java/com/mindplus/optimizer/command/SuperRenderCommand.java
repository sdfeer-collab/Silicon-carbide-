package com.mindplus.optimizer.command;

import com.mindplus.optimizer.client.SuperRenderController;
import com.mindplus.optimizer.config.SuperRenderConfig;
import com.mindplus.optimizer.config.SuperRenderConfigManager;

/**
 * 超渲染命令
 * /long - 设置长距离渲染（面朝方向），最大2000区块
 * 使用此命令时禁用玩家移动和视角改变
 */
public class SuperRenderCommand {
    private static SuperRenderController controller;
    private static boolean longModeActive = false;

    public static void init(SuperRenderController controller) {
        SuperRenderCommand.controller = controller;
        // 命令注册已禁用以避免类加载问题
        // 可以通过代码 API 直接调用 enterLongMode/exitLongMode
    }

    /**
     * 进入长距离渲染模式
     */
    public static void enterLongMode(int distance) {
        // 更新配置
        SuperRenderConfig config = SuperRenderConfigManager.getSuperRenderConfig();
        config.longDistanceRender = distance;
        longModeActive = true;

        // 向渲染进程发送指令
        if (controller != null) {
            controller.setLookAheadDistance(distance);
        }
    }

    /**
     * 退出长距离渲染模式
     */
    public static void exitLongMode() {
        // 重置配置
        SuperRenderConfig config = SuperRenderConfigManager.getSuperRenderConfig();
        config.longDistanceRender = 0;
        config.renderDistance = 150;
        config.simulationDistance = 150;
        longModeActive = false;

        // 向渲染进程发送指令
        if (controller != null) {
            controller.setLookAheadDistance(0);
            controller.setRenderDistance(150);
        }
    }

    public static boolean isLongModeActive() {
        return longModeActive;
    }
}