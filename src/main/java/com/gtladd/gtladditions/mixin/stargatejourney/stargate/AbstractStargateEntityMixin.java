package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractStargateEntity.class)
public abstract class AbstractStargateEntityMixin extends EnergyBlockEntity {
    @Shadow(remap = false) @Final @Mutable
    private static boolean REQUIRE_ENERGY;

    protected AbstractStargateEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void disableEnergyInstability(CallbackInfo ci) {
        REQUIRE_ENERGY = false;
    }

    @Override
    public @Nullable IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return null;
    }

    /** @author Dragons @reason GTL stargates do not store energy. */
    @Overwrite(remap = false)
    public long getEnergyCapacity() { return 0; }

    /** @author Dragons @reason Disable external energy input. */
    @Overwrite(remap = false)
    public long getMaxEnergyReceive() { return 0; }

    /** @author Dragons @reason Disable ZPM input. */
    @Overwrite(remap = false)
    public boolean canReceiveZeroPointEnergy() { return false; }

    @Redirect(method = "disconnectStargate", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;get()Ljava/lang/Object;"), remap = false, require = 1)
    private Object allowDisconnectFromBothEnds(ModConfigSpec.BooleanValue value) {
        return value == CommonStargateConfig.end_connection_from_both_ends ? true : value.get();
    }
}
