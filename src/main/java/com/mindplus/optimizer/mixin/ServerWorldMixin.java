package com.mindplus.optimizer.mixin;

import com.mindplus.optimizer.MindPlusOptimizer;
import com.mindplus.optimizer.coordinator.RuntimeCoordinator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onServerWorldInit(CallbackInfo ci) {
        ServerWorld world = (ServerWorld)(Object)this;
        MinecraftServer server = world.getServer();
        
        if (server != null) {
            RuntimeCoordinator coordinator = MindPlusOptimizer.getRuntimeCoordinator();
            if (coordinator != null) {
                coordinator.setChunkPreloaderServer(server);
            }
        }
    }
}