package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractDHDEntity.class)
public abstract class AbstractDHDEntityMixin extends EnergyBlockEntity {
    @Shadow(remap = false) @Final @Mutable
    protected static boolean REQUIRE_ENERGY;

    protected AbstractDHDEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void disableButtonEnergyCost(CallbackInfo ci) {
        REQUIRE_ENERGY = false;
    }

    // The native capability provider calls this method; null means no exposed FE capability.
    @Override
    public @Nullable IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return null;
    }

    @Inject(method = "outputEnergy", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void disableEnergyTransfer(Direction side, CallbackInfo ci) {
        ci.cancel();
    }
}
