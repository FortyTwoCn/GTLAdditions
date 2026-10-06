package com.gtladd.gtladditions.mixin.stargatejourney;

import net.neoforged.bus.api.IEventBus;
import net.povstalec.sgjourney.common.init.VillagerInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerInit.class)
public abstract class VillagerInitMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void preventItemRegistration(IEventBus eventBus, CallbackInfo ci) {
        // Native village POI tags reference this holder. Keep it registered while
        // leaving the archeologist profession and its trades disabled in GTL.
        VillagerInit.POI_TYPES.register(eventBus);
        ci.cancel();
    }
}
