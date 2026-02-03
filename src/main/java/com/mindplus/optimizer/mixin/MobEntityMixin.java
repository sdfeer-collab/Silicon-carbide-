package com.mindplus.optimizer.mixin;

import com.mindplus.optimizer.MindPlusOptimizer;
import com.mindplus.optimizer.coordinator.RuntimeCoordinator;
import com.mindplus.optimizer.tasks.AITask;
import net.minecraft.entity.mob.MobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("MobEntityMixin");
    
    private static long aiCalculations = 0;
    private static long startTime = System.currentTimeMillis();
    
    // 只在极少部分实体上执行 AI 计算
    private static int entityCounter = 0;
    private static final int AI_SKIP_FACTOR = 8; // 每 8 个实体只处理 1 个

    @Inject(method = "tick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        entityCounter++;
        
        // 只在部分实体上执行 AI 计算，保护 FPS
        if (entityCounter % AI_SKIP_FACTOR != 0) {
            return;
        }
        
        aiCalculations++;
        
        MobEntity entity = (MobEntity)(Object)this;
        RuntimeCoordinator coordinator = MindPlusOptimizer.getRuntimeCoordinator();
        
        if (coordinator != null && !entity.hasVehicle()) {
            // 简化的任务创建
            String taskData = entity.getId() + "," + entity.getX() + "," + entity.getY() + "," + entity.getZ();
            
            // 异步发送任务，不等待结果
            coordinator.sendAITaskAsync(taskData.getBytes());
        }
        
        // 极简统计，每 500 个才输出一次
        if (aiCalculations % 500 == 0) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            double rate = elapsed > 0 ? aiCalculations / elapsed : 0;
            LOGGER.info("AI: {} tasks ({} tasks/sec)", aiCalculations, String.format("%.1f", rate));
        }
    }
}