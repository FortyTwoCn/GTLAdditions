package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import it.unimi.dsi.fastutil.longs.LongLongPair
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.IMultipleRecipeSearch
import org.gtlcore.gtlcore.api.recipe.RecipeMultiplierTracker
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import java.util.*
import java.util.function.BiPredicate
import kotlin.sequences.toCollection

open class MutableRecipesLogic<T>(
    protected val recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>? = null,
    reductionEUt: Double = 1.0,
    reductionDuration: Double = 1.0
) :
    org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic(null, null, reductionEUt, reductionDuration),
    ILockRecipe,
    IWirelessRecipeLogic,
    IRecipeStatus,
    IMultipleRecipeSearch
    where T : WorkableElectricMultiblockMachine,
          T : IRecipeLogicMachine,
          T : IWirelessElectricMultiblockMachine,
          T : IThreadModifierMachine,
          T : ParallelMachine {

    @field:com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @field:SyncToClient
    private var useMultipleRecipes = false
    constructor(reductionEUt: Double, reductionDuration: Double) : this(null, reductionEUt, reductionDuration)

    @Suppress("UNCHECKED_CAST")
    override fun getMachine(): T = super.getMachine() as T

    override fun usesMultipleRecipeExecution(): Boolean = useMultipleRecipes

    override fun collectRecipes(): GTRecipe? = getRecipe()

    protected open fun getRecipe(): GTRecipe? {
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
        val totalParallel = getMachine().maxParallel.toLong() * getMultipleThreads()

        return RecipeCalculationHelper.calculateParallelsWithGreedyAllocation(
            recipes,
            totalParallel,
            machine,
            getParallelAndConsumption = { recipe, remaining -> calculateParallel(machine, recipe, remaining) }
        )
    }

    protected open fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
        val maxEUt = getMachine().overclockVoltage
        val batch = RecipeCalculationHelper.processParallelDataNormal(
            parallelData,
            machine,
            maxEUt,
            batchEuMultiplier,
            { getRecipeEut(it).toDouble() * it.duration }
        )

        val (itemOutputs, fluidOutputs, totalEu) = batch
        if (batch.consumedRecipes == 0) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        val recipe = RecipeCalculationHelper.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, 20)
        RecipeMultiplierTracker.captureReduction(
            getMachine(),
            recipe,
            energyReductionMultiplier,
            durationReductionMultiplier
        )
        return recipe
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
            batchEuMultiplier,
            this::getRecipeEut
        )

        val (itemOutputs, fluidOutputs, totalEu) = batch
        if (batch.consumedRecipes == 0) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        val recipe = RecipeCalculationHelper.buildWirelessRecipe(
            itemOutputs,
            fluidOutputs,
            20,
            totalEu
        )
        RecipeMultiplierTracker.captureReduction(
            getMachine(),
            recipe,
            energyReductionMultiplier,
            durationReductionMultiplier
        )
        return recipe
    }

    protected open fun checkBeforeWorking(): Boolean {
        if (!machine.hasCapabilityProxies()) return false
        if (getMachine().overclockVoltage <= 0) return false
        return true
    }

    protected open fun calculateParallel(
        machine: IRecipeLogicMachine,
        match: GTRecipe,
        remain: Long
    ): LongLongPair {
        val p = IParallelLogic.getMaxParallel(machine, match, remain)
        return LongLongPair.of(p, p)
    }

    public override fun getMultipleThreads(): Int = if (getMachine().getAdditionalThread() > 0) getMachine().getAdditionalThread() else 1

    protected open val batchEuMultiplier: Double
        get() {
            val maintenanceMachine = (machine as IRecipeCapabilityMachine).maintenanceMachine
            return if (maintenanceMachine != null) {
                maintenanceMachine.durationMultiplier * energyReductionMultiplier * durationReductionMultiplier
            } else {
                energyReductionMultiplier * durationReductionMultiplier
            }
        }

    protected open val energyReductionMultiplier: Double
        get() = reductionEUt

    protected open val durationReductionMultiplier: Double
        get() = reductionDuration

    protected open fun getRecipeEut(recipe: GTRecipe): Long = recipe.inputEUt.totalEU

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

    protected open fun matchesSourceRecipe(recipe: GTRecipe): Boolean = RecipeRunnerHelper.matchRecipe(machine, recipe) &&
        IGTRecipe.of(recipe).euTier <= getMachine().tier &&
        RecipeHelper.checkConditions(recipe, this).isSuccess &&
        (recipeCheck == null || recipeCheck.test(recipe, machine))

    override fun getWirelessMachine(): IWirelessElectricMultiblockMachine = machine as IWirelessElectricMultiblockMachine

    fun setUseMultipleRecipes(useMultipleRecipes: Boolean) {
        if (this.useMultipleRecipes == useMultipleRecipes) return
        this.useMultipleRecipes = useMultipleRecipes
        syncDataHolder.markClientSyncFieldDirty("useMultipleRecipes")
        updateTickSubscription()
    }

    override fun isMultipleRecipeMode(): Boolean = this.useMultipleRecipes

    companion object {

    }
}
