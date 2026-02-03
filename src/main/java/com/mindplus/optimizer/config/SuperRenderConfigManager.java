package com.mindplus.optimizer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class SuperRenderConfigManager {
    private static SuperRenderConfig instance;

    public static void init() {
        if (instance == null) {
            try {
                AutoConfig.register(SuperRenderConfig.class, JanksonConfigSerializer::new);
                instance = AutoConfig.getConfigHolder(SuperRenderConfig.class).getConfig();
            } catch (Exception e) {
                e.printStackTrace();
                instance = new SuperRenderConfig();
            }
        }
    }

    public static SuperRenderConfig getInstance() {
        if (instance == null) {
            init();
        }
        return instance;
    }

    public static void save() {
        if (instance != null) {
            try {
                AutoConfig.getConfigHolder(SuperRenderConfig.class).save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static SuperRenderConfig getSuperRenderConfig() {
        return getInstance();
    }
}