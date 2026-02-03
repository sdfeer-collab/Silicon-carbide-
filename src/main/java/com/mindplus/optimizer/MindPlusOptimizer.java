package com.mindplus.optimizer;

import com.mindplus.optimizer.coordinator.GenerationCoordinator;
import com.mindplus.optimizer.coordinator.RuntimeCoordinator;
import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.process.ProcessManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MindPlusOptimizer implements ModInitializer {
    public static final String MOD_ID = "mindplus-optimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ProcessManager processManager;
    private static GenerationCoordinator generationCoordinator;
    private static RuntimeCoordinator runtimeCoordinator;

    @Override
    public void onInitialize() {
        LOGGER.info("MindPlus Optimizer - Multi-process optimization initialized");

        if (ModConfig.INSTANCE == null) {
            LOGGER.warn("ModConfig is not initialized, using default configuration");
            ModConfig.INSTANCE = new ModConfig();
        }

        if (!ModConfig.INSTANCE.general.enabled) {
            LOGGER.info("MindPlus Optimizer is disabled in config");
            return;
        }

        processManager = new ProcessManager();
        generationCoordinator = new GenerationCoordinator(processManager);
        runtimeCoordinator = new RuntimeCoordinator(processManager);

        generationCoordinator.initialize();
        runtimeCoordinator.initialize();

        // 注册服务器停止事件监听器，确保游戏关闭时清理所有工作进程
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, shutting down all worker processes");
            gracefulShutdown();
        });

        LOGGER.info("All worker processes started successfully");
    }

    /**
     * 优雅关闭所有协调器和工作进程
     */
    private void gracefulShutdown() {
        if (generationCoordinator != null) {
            try {
                generationCoordinator.shutdown();
                LOGGER.info("GenerationCoordinator shutdown completed");
            } catch (Exception e) {
                LOGGER.error("Error shutting down GenerationCoordinator", e);
            }
        }

        if (runtimeCoordinator != null) {
            try {
                runtimeCoordinator.shutdown();
                LOGGER.info("RuntimeCoordinator shutdown completed");
            } catch (Exception e) {
                LOGGER.error("Error shutting down RuntimeCoordinator", e);
            }
        }

        LOGGER.info("All worker processes stopped");
    }

    public static ProcessManager getProcessManager() {
        return processManager;
    }

    public static GenerationCoordinator getGenerationCoordinator() {
        return generationCoordinator;
    }

    public static RuntimeCoordinator getRuntimeCoordinator() {
        return runtimeCoordinator;
    }
}