package com.mindplus.optimizer.mixin;

import com.mindplus.optimizer.MindPlusOptimizer;
import com.mindplus.optimizer.coordinator.RuntimeCoordinator;
import com.mindplus.optimizer.generator.WorldGenerator;
import com.mindplus.optimizer.preloader.ChunkPreloader;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkGeneratorMixin");
    
    private static long generatedChunks = 0;
    private static long startTime = System.currentTimeMillis();
    private static long lastStatsTime = 0;
    
    private static RuntimeCoordinator coordinator;

    @Inject(method = "generateFeatures", at = @At("HEAD"))
    private void onGenerateFeatures(StructureWorldAccess world, Chunk chunk, CallbackInfo ci) {
        generatedChunks++;
        
        // 将区块生成任务发送到独立进程
        coordinator = MindPlusOptimizer.getRuntimeCoordinator();
        if (coordinator != null) {
            WorldGenerator worldGenerator = coordinator.getWorldGenerator();
            if (worldGenerator != null) {
                ChunkPos pos = chunk.getPos();
                worldGenerator.generateChunk(pos.x, pos.z);
            }
        }
        
        // 只在每 200 个区块才检查一次，减少主线程开销
        if (generatedChunks % 200 == 0) {
            if (coordinator != null) {
                ChunkPreloader preloader = coordinator.getChunkPreloader();
                if (preloader != null) {
                    ChunkPreloader.PreloadStats stats = preloader.getStats();
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    double rate = elapsed > 0 ? generatedChunks / elapsed : 0;
                    LOGGER.info("Optimization: {} chunks generated ({} chunks/sec), Preloader: {} pending", 
                        generatedChunks, String.format("%.1f", rate), stats.pending);
                }
            }
        }
        
        // 极简统计输出，避免频繁 I/O
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatsTime > 10000) { // 每10秒输出一次
            long elapsed = (currentTime - startTime) / 1000;
            double rate = elapsed > 0 ? generatedChunks / elapsed : 0;
            LOGGER.info("Generated {} chunks ({} chunks/sec)", generatedChunks, String.format("%.1f", rate));
            lastStatsTime = currentTime;
        }
    }
}