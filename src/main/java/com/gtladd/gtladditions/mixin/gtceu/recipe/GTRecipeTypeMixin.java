package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gtladd.gtladditions.common.modify.RecipesModify;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Installs native Additions callbacks without replacing existing type hooks. */
@Mixin(value = GTRecipeType.class, priority = 900)
public abstract class GTRecipeTypeMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$installRecipeCallbacks(CallbackInfo ci) {
        RecipesModify.INSTANCE.modifyOnRegistration((GTRecipeType) (Object) this);
    }
}
