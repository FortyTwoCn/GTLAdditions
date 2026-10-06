package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.povstalec.sgjourney.common.blocks.dhd.PegasusDHDBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PegasusDHDBlock.class)
public abstract class PegasusDHDBlockMixin {
    // Select the native dialing menu for every DHD face instead of the crystal holder.
    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;"), remap = false, require = 1)
    private Direction selectDialingMenu(BlockHitResult hit) {
        return Direction.UP;
    }

    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isShiftKeyDown()Z"), remap = false, require = 1)
    private boolean selectDialingMenuWhileSneaking(Player player) {
        return false;
    }
}
