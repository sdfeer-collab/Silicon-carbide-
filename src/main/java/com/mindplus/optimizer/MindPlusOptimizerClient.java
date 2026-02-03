package com.mindplus.optimizer.client;

import com.mindplus.optimizer.MindPlusOptimizer;
import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.config.SuperRenderConfigManager;
import com.mindplus.optimizer.command.SuperRenderCommand;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MindPlusOptimizerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MindPlus-Optimizer-Client");

    private static SuperRenderController superRenderController;

    @Override
    public void onInitializeClient() {
        LOGGER.info("MindPlus Optimizer Client initialized");

        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
        ModConfig.INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        LOGGER.info("ModConfig initialized: {}", ModConfig.INSTANCE != null);

        // 初始化超渲染配置
        SuperRenderConfigManager.init();
        LOGGER.info("Super render config initialized");

        // 初始化超渲染控制器（连接到渲染进程）
        try {
            superRenderController = new SuperRenderController();
            LOGGER.info("Super render controller initialized");
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize super render controller: {}", e.getMessage());
        }

        // 注册超渲染指令处理器
        SuperRenderCommand.init(superRenderController);
        LOGGER.info("Super render command registered");

        // 注册超渲染输入处理器（C 键 + 滚轮）
        SuperRenderInputHandler.init(superRenderController);
        LOGGER.info("Super render input handler registered");

        // 延迟初始化，等待游戏完全启动
        new Thread(() -> {
            try {
                // 等待 5 秒，确保游戏已经完全启动
                Thread.sleep(5000);

                // 获取 MinecraftClient 实例
                Object client = Class.forName("net.minecraft.client.MinecraftClient")
                    .getMethod("getInstance").invoke(null);

                // 初始化渲染优化器
                if (MindPlusOptimizer.getRuntimeCoordinator() != null) {
                    MindPlusOptimizer.getRuntimeCoordinator().setRenderOptimizerClient(client);
                    LOGGER.info("Render optimizer initialized on client");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to initialize client components", e);
            }
        }, "MindPlus-Initializer").start();
    }

    /**
     * 获取超渲染控制器
     */
    public static SuperRenderController getSuperRenderController() {
        return superRenderController;
    }
}