package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.povstalec.sgjourney.common.data.StargateNetwork;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StargateNetwork.class)
public abstract class StargateNetworkMixin {
    @Shadow(remap = false) @Final @Mutable
    private static boolean REQUIRE_ENERGY;

    // Preserve native connection validation, events, simulation and multi-destination handling.
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void disableConnectionEnergyCost(CallbackInfo ci) {
        REQUIRE_ENERGY = false;
    }
}
