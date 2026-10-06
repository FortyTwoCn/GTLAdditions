package com.gtladd.gtladditions.mixin.gtceu.registry;

import com.gtladd.gtladditions.api.registry.NativeQueuedMachines;
import com.gregtechceu.gtceu.api.registry.registrate.builder.MachineBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MachineBuilder.class)
public abstract class QueuedMachineBuilderMixin {
    @Inject(method = "register()Lcom/gregtechceu/gtceu/api/registry/registrate/entry/MachineEntry;",
            at = @At("HEAD"), remap = false, require = 1)
    private void gtladd$captureQueuedBuilder(CallbackInfoReturnable<?> cir) {
        NativeQueuedMachines.capture((MachineBuilder<?, ?, ?>) (Object) this);
    }

    @Inject(method = "createAdditionalObjects", at = @At("HEAD"), remap = false, require = 1)
    private void gtladd$sealQueuedBuilder(CallbackInfo ci) {
        NativeQueuedMachines.seal((MachineBuilder<?, ?, ?>) (Object) this);
    }
}
