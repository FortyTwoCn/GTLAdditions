package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.api.machine.mutable.MutableCoilElectricMultiblockMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.recipe.RecipeMultiplierTracker
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SkeletonShiftRiftEngine(holder: BlockEntityCreationInfo) : MutableCoilElectricMultiblockMachine(holder, SkeletonShiftRecipeLogic()) {
    @field:SaveField
    private var casingTier: Int = 0

    @field:SaveField
    private var parallel = 0

    private class SkeletonShiftRecipeLogic : MutableRecipesLogic<SkeletonShiftRiftEngine>() {
        override val durationReductionMultiplier: Double
            get() = super.durationReductionMultiplier / max(getMachine().casingTier, 1)
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        this.casingTier = org.gtlcore.gtlcore.api.pattern.PatternMatchData.of(defaultPatternState.context).tier("SCTier").orElse(0)
        parallel = maxParallel
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        this.casingTier = 0
        parallel = 0
    }

    override fun getMaxParallel(): Int = min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 1200.0).toInt())

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("SkeletonShiftRiftEngineStatus", syncManager, {
            mutableListOf<Component>().also(::collectSkeletonShiftRiftEngineStatusText)
        }, 130)
    )

    private fun collectSkeletonShiftRiftEngineStatusText(textList: MutableList<Component>) {
        
        if (!this.isFormed) return
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                FormattingUtil.formatNumbers(parallel).literal
                    .withStyle(ChatFormatting.DARK_PURPLE)
            )
                .withStyle(ChatFormatting.GRAY)
        )
        textList.add("gtceu.casings.tier".toComponent(this.casingTier))
    }

    companion object {
        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe): com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction =
            com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction { source -> modifySkeletonRecipe(machine, source ?: return@ModifierFunction null) }

        private fun modifySkeletonRecipe(machine: MetaMachine, recipe: GTRecipe): GTRecipe? {
            if (machine is SkeletonShiftRiftEngine) {
                val pair = org.gtlcore.gtlcore.api.recipe.CoreRecipeModifiers.accurateParallel(machine, recipe, machine.parallel, false)
                if (pair.getFirst() == null || pair.getSecond()!! <= 0) return null
                val recipe1 = pair.getFirst() ?: return null
                RecipeMultiplierTracker.captureReduction(
                    machine,
                    recipe,
                    1.0,
                    1.0 / max(machine.casingTier, 1)
                )
                recipe1.duration = max(recipe1.duration / max(machine.casingTier, 1), 1)
                return OverclockingLogic.PERFECT_OVERCLOCK.getModifier(machine, recipe1, machine.overclockVoltage, false).apply(recipe1)
            }
            return null
        }

    }
}