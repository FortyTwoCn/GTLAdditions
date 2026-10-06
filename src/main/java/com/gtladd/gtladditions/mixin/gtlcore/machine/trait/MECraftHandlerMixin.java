package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MECraftHandler;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MECraftHandler.class)
public abstract class MECraftHandlerMixin {

    @Inject(method = "extractGTRecipe(JI)Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;", at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void gtladd$extractInfinityRecipe(long parallelAmount, int tickDuration, CallbackInfoReturnable<GTRecipe> cir) {
        if (parallelAmount == Long.MIN_VALUE) {
            cir.setReturnValue(((MECraftHandler) (Object) this).extractAllGTRecipe());
        }
    }
}
