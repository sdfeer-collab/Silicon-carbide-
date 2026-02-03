package com.mindplus.optimizer.mixin;

import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修改视频设置中的模拟距离滑块范围
 * 从 5-12 改为 2-150
 * 已禁用
 */
// @Mixin(VideoOptionsScreen.class)
public class SimulationDistanceSliderMixin {

    /*
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;ofIntSlider(Ljava/lang/String;IILjava/util/function/BiConsumer;)Lnet/minecraft/client/option/SimpleOption;"),
        index = 2
    )
    */
    private int modifySimulationDistanceMaxValue(String key, int originalMin, int originalMax) {
        if ("options.simulationDistance".equals(key)) {
            return 150; // 最大模拟距离改为 150
        }
        return originalMax;
    }

    /*
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;ofIntSlider(Ljava/lang/String;IILjava/util/function/BiConsumer;)Lnet/minecraft/client/option/SimpleOption;"),
        index = 1
    )
    */
    private int modifySimulationDistanceMinValue(String key, int originalMin, int originalMax) {
        if ("options.simulationDistance".equals(key)) {
            return 2; // 最小模拟距离改为 2
        }
        return originalMin;
    }
}