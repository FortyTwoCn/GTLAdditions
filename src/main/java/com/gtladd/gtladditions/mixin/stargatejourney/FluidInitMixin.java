package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.povstalec.sgjourney.common.init.FluidInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(FluidInit.class)
public abstract class FluidInitMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/neoforged/neoforge/registries/DeferredHolder;"), remap = false, require = 1)
    private static DeferredHolder<Fluid, Fluid> modifyFluidRegistrationName(
                                                                     DeferredRegister<Fluid> instance, String name, Supplier<Fluid> supplier) {
        return instance.register(name + "_ban", supplier);
    }
}