package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GTRecipeBuilder.class, priority = 2000)
public abstract class GTRecipeBuilderMixin {

    @Shadow(remap = false)
    public int duration;

    @Shadow(remap = false)
    public GTRecipeType recipeType;

    @Inject(method = "toJson", at = @At("TAIL"), remap = false)
    public void toJson(HolderLookup.Provider access, JsonObject json, CallbackInfo ci) {
        if (recipeType == GTLAddRecipesTypes.GENESIS_ENGINE.get() || recipeType == GTLAddRecipesTypes.INTER_STELLAR.get() || recipeType == GTLAddRecipesTypes.CHAOS_WEAVE.get() || recipeType == GTLAddRecipesTypes.SPACETIME_STASIS.get() || recipeType == GTLAddRecipesTypes.SUPRATEMPORAL_BOOSTING.get() || recipeType == GTLAddRecipesTypes.TIME_SPACE_DISTORTION.get() || recipeType == GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY.get()) {
            json.remove("duration");
            json.addProperty("duration", duration);
        }
    }
}