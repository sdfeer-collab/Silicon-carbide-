package com.mindplus.optimizer.mixin;

import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修改视频设置中的渲染距离滑块范围
 * 从 2-32 改为 2-150
 * 已禁用
 */
// @Mixin(VideoOptionsScreen.class)
public class RenderDistanceSliderMixin {

    /*
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;of(Ljava/lang/String;ILjava/util/function/BiConsumer;Ljava/util/function/Function;Ljava/util/function/Function;Ljava/lang/Object;)Lnet/minecraft/client/option/SimpleOption;"),
        index = 1
    )
    */
    private int modifyRenderDistanceMaxValue(String key, int originalMax) {
        if ("options.renderDistance".equals(key)) {
            return 150; // 最大渲染距离改为 150
        }
        return originalMax;
    }
}