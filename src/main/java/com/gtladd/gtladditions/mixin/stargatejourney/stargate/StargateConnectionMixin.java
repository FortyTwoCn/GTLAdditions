package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StargateConnection.class)
public abstract class StargateConnectionMixin {
    @Shadow(remap = false) @Final @Mutable
    public static boolean ENERGY_BYPASS_ENABLED;
    @Shadow(remap = false) @Final @Mutable
    public static boolean REQUIRE_ENERGY;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void disableEnergy(CallbackInfo ci) {
        ENERGY_BYPASS_ENABLED = true;
        REQUIRE_ENERGY = false;
    }

    @Inject(method = "shouldAutoclose", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void disableAutoclose(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/povstalec/sgjourney/common/sgjourney/stargate/Stargate;doWormhole(Lnet/povstalec/sgjourney/common/sgjourney/StargateConnection;ZLnet/povstalec/sgjourney/common/sgjourney/StargateInfo$WormholeTravel;)V",
            ordinal = 1), remap = false, require = 1)
    private void allowDialedSideTravel(Stargate stargate, StargateConnection connection,
            boolean isDialedSide, StargateInfo.WormholeTravel travel) {
        stargate.doWormhole(connection, isDialedSide, StargateInfo.WormholeTravel.ENABLED);
    }
}
