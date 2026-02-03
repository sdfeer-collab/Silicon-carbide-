package com.mindplus.optimizer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class ModConfigManager {

    public static void init() {
        // 初始化配置系统
        try {
            // SuperRenderConfig 已经在 MindPlusOptimizerClient 中初始化
            // 这里可以添加其他配置的初始化逻辑
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}