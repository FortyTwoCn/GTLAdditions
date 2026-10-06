package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;
import org.gtlcore.gtlcore.api.recipe.IMultipleRecipeSearch;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import java.util.Iterator;
import java.util.function.Predicate;

/** Compose Mutable machine mode with Core's grouped native database iterator. */
@Mixin(value = GTRecipeType.class, priority = 2000)
public abstract class GTRecipeLookupMixin {
    @WrapMethod(method = "searchRecipe", remap = false)
    private Iterator<GTRecipe> gtladditions$mutableSearch(IRecipeCapabilityHolder holder, Predicate<GTRecipe> predicate,
                                                       Operation<Iterator<GTRecipe>> original) {
        var iterator = original.call(holder, predicate);
        if (iterator instanceof IAdditionalRecipeIterator grouped && holder instanceof IRecipeLogicMachine machine) {
            var logic = machine.getRecipeLogic();
            grouped.setUseDiveIngredientTreeFind(logic instanceof MultipleRecipesLogic ||
                    logic instanceof IMultipleRecipeSearch mutable && mutable.isMultipleRecipeMode());
        }
        return iterator;
    }
}
