package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate

open class GTLAddMultipleTypeWirelessRecipesLogic(
    recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>? = null
) : GTLAddMultipleWirelessRecipesLogic(recipeCheck) {
    override fun buildWirelessRecipe(
        itemOutputs: ObjectArrayList<Content>,
        fluidOutputs: ObjectArrayList<Content>,
        totalEu: BigInteger
    ): WirelessGTRecipe = RecipeCalculationHelper.buildWirelessRecipe(
        itemOutputs,
        fluidOutputs,
        limited.getLimitedDuration(),
        totalEu,
        getMachine().recipeType
    )

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