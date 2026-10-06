package com.gtladd.gtladditions.api.machine.logic

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate
import java.util.function.Predicate

open class GTLAddMultipleRecipesLogic(
    protected val recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>? = null,
    protected val beforeWorking: Predicate<IRecipeLogicMachine>? = null
) : MultipleRecipesLogic(), org.gtlcore.gtlcore.api.machine.trait.ILockRecipe,
    org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus {
    protected val limited: IGTLAddMultiRecipeMachine get() = getMachine()
    constructor(beforeWorking: Predicate<IRecipeLogicMachine>?) : this(null, beforeWorking)

    override fun collectRecipes(): GTRecipe? = getGTRecipe()

    override fun getMachine(): GTLAddWorkableElectricMultipleRecipesMachine = super.getMachine() as GTLAddWorkableElectricMultipleRecipesMachine

    public override fun getMultipleThreads(): Int = Ints.saturatedCast(MAX_THREADS + getMachine().getAdditionalThread())

    protected open fun getGTRecipe(): GTRecipe? {
        if (!checkBeforeWorking()) return null

        val parallelData = calculateParallels() ?: return null

        val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
        return if (wirelessTrait != null) {
            buildFinalWirelessRecipe(parallelData, wirelessTrait)
        } else {
            buildFinalNormalRecipe(parallelData)
        }
    }

    protected open fun calculateParallels(): ParallelData? {
        val recipes = lookupRecipeSet()
        val totalParallel = parallel.maxParallel.toLong() * getMultipleThreads()

        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
            recipes,
            totalParallel
        ) { recipe -> LongBooleanPair.of(getMaxParallel(recipe, totalParallel), true) }
    }

    protected open fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
        val maxEUt = getMachine().overclockVoltage
        val batch = RecipeCalculationHelper.processParallelDataNormal(
            parallelData,
            machine,
            maxEUt,
            euMultiplier,
            { getTotalEuOfRecipe(it) }
        )

        val (itemOutputs, fluidOutputs, totalEu) = batch
        if (batch.consumedRecipes == 0) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, limited.getLimitedDuration())
    }

    protected open fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        val batch = RecipeCalculationHelper.processParallelDataWireless(
            parallelData,
            machine,
            wirelessTrait.maxAvailableEnergy,
            euMultiplier,
            ::getWirelessRecipeEut,
            isEnergyConsumer()
        )

        val (itemOutputs, fluidOutputs, totalEu) = batch
        if (batch.consumedRecipes == 0) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return buildWirelessRecipe(itemOutputs, fluidOutputs, totalEu)
    }

    protected open fun buildWirelessRecipe(
        itemOutputs: ObjectArrayList<Content>,
        fluidOutputs: ObjectArrayList<Content>,
        totalEu: BigInteger
    ): WirelessGTRecipe = RecipeCalculationHelper.buildWirelessRecipe(
        itemOutputs,
        fluidOutputs,
        limited.getLimitedDuration(),
        totalEu
    )

    protected open fun getWirelessRecipeEut(recipe: GTRecipe): Long = recipe.inputEUt.totalEU

    protected open fun isEnergyConsumer(): Boolean = true

    protected open fun lookupRecipeSet(): Set<GTRecipe> = if (isLock) {
        when {
            lockRecipe == null -> {
                lockRecipe = machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe).asSequence().firstOrNull()
                lockRecipe?.let { Collections.singleton(it) } ?: emptySet()
            }
            matchesSourceRecipe(lockRecipe) -> Collections.singleton(lockRecipe)
            else -> emptySet()
        }
    } else {
        machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe).asSequence()
            .toCollection(ObjectOpenHashSet())
    }

    protected open fun matchesSourceRecipe(recipe: GTRecipe): Boolean = matchRecipe(machine, recipe) &&
        IGTRecipe.of(recipe).euTier <= getMachine().tier &&
        RecipeHelper.checkConditions(recipe, this).isSuccess &&
        (recipeCheck == null || recipeCheck.test(recipe, machine))

    protected open fun checkBeforeWorking(): Boolean {
        if (!machine.hasCapabilityProxies()) return false
        if (getMachine().overclockVoltage <= 0) return false
        return beforeWorking == null || beforeWorking.test(machine)
    }

    protected open fun getMaxParallel(recipe: GTRecipe, limit: Long): Long = IParallelLogic.getMaxParallel(this.machine, recipe, limit)

    companion object {
        private const val MAX_THREADS = 128L
    }
}
