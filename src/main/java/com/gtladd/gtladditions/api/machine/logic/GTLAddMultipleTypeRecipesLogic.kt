package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.util.Collections
import java.util.function.Predicate

open class GTLAddMultipleTypeRecipesLogic(
    beforeWorking: Predicate<IRecipeLogicMachine>? = null
) : GTLAddMultipleRecipesLogic(beforeWorking) {
    override fun lookupRecipeSet(): Set<GTRecipe> = if (isLock) {
        when {
            lockRecipe == null -> {
                lockRecipe = machine.recipeTypes.asSequence()
                    .mapNotNull { it.searchRecipe(machine, this::matchesSourceRecipe).asSequence().firstOrNull() }
                    .firstOrNull()
                lockRecipe?.let { Collections.singleton(it) } ?: emptySet()
            }

            matchesSourceRecipe(lockRecipe) -> Collections.singleton(lockRecipe)
            else -> emptySet()
        }
    } else {
        machine.recipeTypes.asSequence()
            .flatMap { it.searchRecipe(machine, this::matchesSourceRecipe).asSequence() }
            .toCollection(ObjectOpenHashSet())
    }
}