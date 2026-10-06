package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gtladd.gtladditions.api.recipe.NativeWirelessRecipeTick;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeLogic.class)
public abstract class NativeWirelessRecipeTickMixin {
    @Shadow(remap = false) public abstract IRecipeLogicMachine getRLMachine();

    @WrapMethod(method = "handleTickRecipe", remap = false)
    private ActionResult gtladditions$payNativeTick(GTRecipe recipe, Operation<ActionResult> original) {
        return NativeWirelessRecipeTick.run(getRLMachine(), recipe, () -> original.call(recipe));
    }
}
