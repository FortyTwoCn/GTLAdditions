package com.gtladd.gtladditions.mixin.stargatejourney;

import net.neoforged.bus.api.IEventBus;
import net.povstalec.sgjourney.common.init.RecipeTypeInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeTypeInit.class)
public abstract class RecipeTypeInitMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void preventItemRegistration(IEventBus eventBus, CallbackInfo ci) {
        ci.cancel();
    }
}