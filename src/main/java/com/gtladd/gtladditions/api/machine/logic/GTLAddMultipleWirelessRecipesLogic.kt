package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe
import java.util.function.BiPredicate
import java.util.function.Predicate

open class GTLAddMultipleWirelessRecipesLogic(
    recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>? = null,
    beforeWorking: Predicate<IRecipeLogicMachine>? = null
) : GTLAddMultipleRecipesLogic(recipeCheck, beforeWorking), IWirelessRecipeLogic {
    constructor(beforeWorking: Predicate<IRecipeLogicMachine>?) : this(null, beforeWorking)

    override fun getMachine(): GTLAddWirelessWorkableElectricMultipleRecipesMachine = super.getMachine() as GTLAddWirelessWorkableElectricMultipleRecipesMachine

    override fun getWirelessMachine(): com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine = getMachine()

    override fun getGTRecipe(): GTRecipe? {
        if (!checkBeforeWorking()) return null

        val parallelData = calculateParallels() ?: return null

        return buildFinalWirelessRecipe(parallelData, getMachine().getWirelessNetworkEnergyHandler())
    }

    override fun checkBeforeWorking(): Boolean {
        if (!machine.hasCapabilityProxies()) return false
        if (!getMachine().getWirelessNetworkEnergyHandler().isOnline) return false
        return beforeWorking == null || beforeWorking.test(machine)
    }

    override fun matchesSourceRecipe(recipe: GTRecipe): Boolean = matchRecipe(machine, recipe) &&
        com.gregtechceu.gtceu.api.recipe.RecipeHelper.checkConditions(recipe, this).isSuccess &&
        (recipeCheck == null || recipeCheck.test(recipe, machine))

    // Disable
    override fun getTotalEuOfRecipe(recipe: GTRecipe): Double = 0.0
}
