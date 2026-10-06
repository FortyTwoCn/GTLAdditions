package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import java.util.function.BiPredicate

class DimensionallyTranscendentChemicalPlant(holder: BlockEntityCreationInfo) : GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder, DimensionallyTranscendentChemicalPlantLogic(EBFChecks.EBF_CHECK)) {

    override fun getRecipeLogic(): DimensionallyTranscendentChemicalPlantLogic = super.getRecipeLogic() as DimensionallyTranscendentChemicalPlantLogic

    companion object {
        class DimensionallyTranscendentChemicalPlantLogic(
            recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>
        ) : GTLAddMultipleRecipesLogic(recipeCheck) {
            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                val totalParallel = parallel.maxParallel.toLong() * getMultipleThreads()

                return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
                    recipes,
                    totalParallel
                ) { recipe ->
                    val consume = IGTRecipe.of(recipe).euTier > GTValues.UV
                    val p = getMaxParallel(
                        recipe,
                        if (consume) totalParallel else Long.MAX_VALUE
                    )
                    LongBooleanPair.of(p, consume)
                }
            }
        }
    }
}