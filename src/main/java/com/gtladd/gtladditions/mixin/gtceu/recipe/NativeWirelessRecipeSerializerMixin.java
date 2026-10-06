package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gtladd.gtladditions.api.recipe.NativeWirelessRecipeState;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.mojang.serialization.MapCodec;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Restores the subtype through GT's registry-aware native recipe codecs and stream. */
@Mixin(GTRecipeSerializer.class)
public abstract class NativeWirelessRecipeSerializerMixin {
    @ModifyReturnValue(method = "makeCodec", at = @At("RETURN"), remap = false, require = 1)
    private static MapCodec<GTRecipe> gtladditions$wirelessCodec(MapCodec<GTRecipe> original, boolean kubeLoaded) {
        return original.xmap(NativeWirelessRecipeState::restore, recipe -> recipe);
    }

    @ModifyReturnValue(method = "fromNetwork", at = @At("RETURN"), remap = false, require = 1)
    private static GTRecipe gtladditions$wirelessStream(GTRecipe original) {
        return NativeWirelessRecipeState.restore(original);
    }
}
