package com.mindplus.optimizer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrightnessCommand {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BrightnessCommand");
    private static final Path CONFIG_FILE = Paths.get("config", "mindplus-brightness.txt");
    private static boolean brightnessEnhanced = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // 注册 /brightness 命令
        dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("brightness")
            .executes(context -> {
                // 没有参数时切换亮度
                toggleBrightness(context.getSource());
                return 1;
            })
            .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value", BoolArgumentType.bool())
                .executes(context -> {
                    boolean value = BoolArgumentType.getBool(context, "value");
                    setBrightness(context.getSource(), value);
                    return 1;
                })
            )
        );

        // 加载保存的亮度状态
        loadBrightnessState();
    }

    private static void toggleBrightness(FabricClientCommandSource source) {
        brightnessEnhanced = !brightnessEnhanced;
        setBrightness(source, brightnessEnhanced);
    }

    private static void setBrightness(FabricClientCommandSource source, boolean enhanced) {
        brightnessEnhanced = enhanced;
        
        try {
            // 获取 MinecraftClient 实例
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                source.sendFeedback(Text.literal("无法获取 Minecraft 客户端实例"));
                return;
            }

            // 使用反射获取 gamma 选项
            Object options = client.options;
            Field gammaField = options.getClass().getDeclaredField("gamma");
            gammaField.setAccessible(true);
            
            Object gammaOption = gammaField.get(options);
            double currentGamma = (Double) gammaOption.getClass().getMethod("getValue").invoke(gammaOption);
            
            // 设置新的 gamma 值
            double newGamma;
            if (enhanced) {
                newGamma = Math.min(16.0, currentGamma * 2.5);
            } else {
                newGamma = Math.max(0.0, currentGamma / 2.5);
            }
            
            gammaOption.getClass().getMethod("setValue", Double.class).invoke(gammaOption, newGamma);
            
            String message = enhanced ? "亮度增强已启用" : "亮度增强已禁用";
            source.sendFeedback(Text.literal(message));
            
            // 保存状态
            saveBrightnessState();
            
            LOGGER.info("Brightness set to: {}, gamma: {}", enhanced, newGamma);
        } catch (Exception e) {
            LOGGER.error("Failed to set brightness", e);
            source.sendFeedback(Text.literal("设置亮度失败: " + e.getMessage()));
        }
    }

    private static void saveBrightnessState() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, String.valueOf(brightnessEnhanced));
        } catch (Exception e) {
            LOGGER.warn("Failed to save brightness state", e);
        }
    }

    private static void loadBrightnessState() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String content = Files.readString(CONFIG_FILE).trim();
                brightnessEnhanced = Boolean.parseBoolean(content);
                LOGGER.info("Loaded brightness state: {}", brightnessEnhanced);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load brightness state", e);
        }
    }
}