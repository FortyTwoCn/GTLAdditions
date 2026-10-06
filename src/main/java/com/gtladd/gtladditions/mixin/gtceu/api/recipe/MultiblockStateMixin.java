package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.ForgeOfTheAntichristModuleBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Module activity updates do not detach their host; replacing or breaking the block still rechecks the structure. */
@Mixin(PatternState.class)
public abstract class MultiblockStateMixin {
    @Shadow(remap = false) protected MultiblockControllerMachine controller;
    @Shadow(remap = false) protected BlockPos controllerPos;
    @Shadow(remap = false) protected boolean isFormed;

    @Inject(method = "onBlockStateChanged", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$moduleActivity(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (isFormed && controller != null && !pos.equals(controllerPos) && oldState.getBlock() == newState.getBlock()
                && controller.getLevel().getBlockEntity(pos) instanceof ForgeOfTheAntichristModuleBase) ci.cancel();
    }
}
