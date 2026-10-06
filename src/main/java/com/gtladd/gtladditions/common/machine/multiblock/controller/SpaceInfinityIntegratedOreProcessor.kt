package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.OreProcessorRecipeHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.MachineIO
import java.util.function.Predicate

class SpaceInfinityIntegratedOreProcessor(holder: BlockEntityCreationInfo, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(
        holder
    , InfinityIntegratedOreProcessorLogic(BEFORE_RECIPE)) {
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


    override fun getRecipeLogic(): InfinityIntegratedOreProcessorLogic = super.getRecipeLogic() as InfinityIntegratedOreProcessorLogic

    override fun needConfirmMEStock(): Boolean = true

    override fun addParallelDisplay(textList: MutableList<Component>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class InfinityIntegratedOreProcessorLogic(
            beforeWorking: Predicate<IRecipeLogicMachine>?
        ) : GTLAddMultipleRecipesLogic(beforeWorking) {
            override fun getMachine(): SpaceInfinityIntegratedOreProcessor = super.getMachine() as SpaceInfinityIntegratedOreProcessor

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel,
                    modifyRecipe = { recipe ->
                        OreProcessorRecipeHelper.copyForOreProcessor(
                            recipe,
                            outputMultiplier = if (getMachine().opHatch?.matchAll == true) 2.0 else 1.0,
                            itemChanceBoost = getMachine().opHatch?.itemChanceBoost ?: 1
                        )
                    },
                    useModifiedRecipe = true
                )
            }
        }

        private val BEFORE_RECIPE = Predicate { machine: IRecipeLogicMachine ->
            if (machine is SpaceInfinityIntegratedOreProcessor) {
                return@Predicate MachineIO.inputFluid(
                    machine,
                    GTLMaterials.StellarEnergyRocketFuel.getFluid(100000)
                )
            }
            false
        }
    }
}