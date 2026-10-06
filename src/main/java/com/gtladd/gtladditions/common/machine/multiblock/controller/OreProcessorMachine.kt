package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.math.IntMath
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.utils.OreProcessorRecipeHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongLongPair
import org.gtlcore.gtlcore.utils.NumberUtils

class OreProcessorMachine(holder: BlockEntityCreationInfo) : GTLAddWorkableElectricMultipleRecipesMachine(holder, OreProcessorRecipeLogic()) {

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


    override fun getRecipeLogic(): OreProcessorRecipeLogic = super.getRecipeLogic() as OreProcessorRecipeLogic

    override fun getMaxParallel(): Int = Int.MAX_VALUE

    private fun modifyRecipe(recipe: GTRecipe): GTRecipe = OreProcessorRecipeHelper.copyForOreProcessor(
        recipe,
        itemChanceBoost = opHatch?.itemChanceBoost ?: 1,
        inputFluidMultiplier = opHatch?.fluidMultiplier ?: 1.0
    )

    companion object {
        class OreProcessorRecipeLogic() : GTLAddMultipleRecipesLogic() {
            override fun getMachine(): OreProcessorMachine = super.getMachine() as OreProcessorMachine

            @Suppress("UnstableApiUsage")
            override fun getMultipleThreads(): Int = IntMath.saturatedAdd(super.getMultipleThreads(), getMachine().opHatch?.advancedThreadBonus ?: 0)

            override fun calculateParallels(): ParallelData? {
                val hatch = getMachine().opHatch
                val totalParallel = NumberUtils.saturatedMultiply(
                    parallel.maxParallel.toLong(),
                    getMultipleThreads().toLong()
                ).let { if (hatch?.matchAll == true) NumberUtils.saturatedMultiply(it, 2) else it }

                val recipes = lookupRecipeSet().map { getMachine().modifyRecipe(it) }
                return RecipeCalculationHelper.calculateParallelsWithGreedyAllocation(
                    recipes,
                    totalParallel,
                    machine,
                    getParallelAndConsumption = { recipe, remaining ->
                        val p = getMaxParallel(recipe, remaining)
                        LongLongPair.of(p, p)
                    }
                )
            }
        }
    }
}