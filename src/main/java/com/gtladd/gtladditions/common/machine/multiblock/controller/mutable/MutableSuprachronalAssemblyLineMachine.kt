package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SuprachronalAssemblyLineMachine
import java.util.function.BiPredicate

class MutableSuprachronalAssemblyLineMachine(holder: BlockEntityCreationInfo) :
    SuprachronalAssemblyLineMachine(holder, MutableRecipesLogic<MutableSuprachronalAssemblyLineMachine>(DATA_CHECK, 1.0, 0.4)),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableSuprachronalAssemblyLineMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutableSuprachronalAssemblyLineMachine>

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

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun getMaxParallel(): Int = (this as IRecipeCapabilityMachine).parallelHatch.map { it.currentParallel }.orElse(1)

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        private val DATA_CHECK = BiPredicate { recipe: GTRecipe, recipeLogicMachine: IRecipeLogicMachine ->
            if (recipeLogicMachine is IRecipeCapabilityMachine) {
                val result = recipeLogicMachine.dataAccessHatch?.isRecipeAvailable(recipe)
                    ?: recipe.conditions.none { it is ResearchCondition }
                if (!result) RecipeResult.of(recipeLogicMachine, RecipeResult.FAIL_NO_FIND_RESEARCHED)
                return@BiPredicate result
            }
            false
        }
    }
}