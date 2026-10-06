package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine
import java.util.function.BiPredicate

open class MutableTierCasingMachine(holder: BlockEntityCreationInfo, tierType: String, logic: RecipeLogic = MutableRecipesLogic<MutableTierCasingMachine>(TIER_CHECK)) :
    TierCasingMachine(holder, tierType, logic),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableTierCasingMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutableTierCasingMachine>

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun getMaxParallel(): Int = (this as IRecipeCapabilityMachine).parallelHatch.map { it.currentParallel }.orElse(1)

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        val TIER_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe: GTRecipe, recipeLogicMachine: IRecipeLogicMachine ->
                if (recipeLogicMachine is TierCasingMachine) {
                    if (recipe.data.contains(recipeLogicMachine.tierType) &&
                        recipe.data.getInt(recipeLogicMachine.tierType) > recipeLogicMachine.casingTier
                    ) {
                        RecipeResult.of(recipeLogicMachine, RecipeResult.FAIL_NO_ENOUGH_TIER)
                        return@BiPredicate false
                    }
                }
                true
            }
    }
}