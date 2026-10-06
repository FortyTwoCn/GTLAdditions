package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import org.gtlcore.gtlcore.api.recipe.CoreRecipeModifiers
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.OreProcessorRecipeHelper
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.RecipeMultiplierTracker
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class BasicOreProcessorMachine(holder: BlockEntityCreationInfo) : WorkableElectricMultiblockMachine(holder) {

    private var opHatch: OreProcessorHatch? = null

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        parts.forEach {
            if (it is OreProcessorHatch) {
                opHatch = it
                return
            }
        }
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        opHatch = null
    }

    override fun getPanelBuilder(data: PosGuiData, sync: PanelSyncManager, settings: UISettings): MachineUIPanelBuilder =
        super.getPanelBuilder(data, sync, settings)
            .addDefaultConfigurators(false)
            .addTraitConfigurators(false)
            .leftConfigurators { controls ->
                controls.child(GTMuiWidgets.createPowerButton(this))
                IRecipeCapabilityMachine.attachConfigurators(controls, this)
            }

    fun getParallel(): Long = GTLRecipeModifiers.getHatchParallel(this).toLong() * (opHatch?.parallelMultiplier ?: 1)

    companion object {
        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe): ModifierFunction {
            val oreProcessor = machine as? BasicOreProcessorMachine ?: return ModifierFunction.NULL
            return ModifierFunction { source ->
                val hatch = oreProcessor.opHatch
                val parallelResult = CoreRecipeModifiers.accurateParallel(
                    oreProcessor, source, hatch?.parallelMultiplier ?: 1, false
                )
                val paralleledRecipe = parallelResult.first
                if (paralleledRecipe == null || parallelResult.second <= 0) return@ModifierFunction null
                val durationMultiplier = hatch?.durationMultiplier ?: 1.0
                RecipeMultiplierTracker.captureReduction(oreProcessor, source, 1.0, durationMultiplier)
                val modifiedRecipe = OreProcessorRecipeHelper.copyForOreProcessor(
                    paralleledRecipe, itemChanceBoost = hatch?.itemChanceBoost ?: 1,
                    durationMultiplier = durationMultiplier
                )
                val overclock = if (hatch?.matchAll == true) OverclockingLogic.PERFECT_OVERCLOCK_SUBTICK
                    else OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK
                overclock.getModifier(oreProcessor, modifiedRecipe, oreProcessor.overclockVoltage).apply(modifiedRecipe)
            }
        }
    }
}
