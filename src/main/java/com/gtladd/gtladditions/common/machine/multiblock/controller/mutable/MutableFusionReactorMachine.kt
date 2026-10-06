package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import java.util.function.BiPredicate

class MutableFusionReactorMachine(holder: BlockEntityCreationInfo, tier: Int) :
    FusionReactorMachine(holder, tier),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    // Core supplies the original one-amp overclock voltage independently of the native tier cap.
    override fun getMaxVoltage(): Long = super.getOverclockVoltage()

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableFusionReactorMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutableFusionReactorMachine>

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
        fun newRecipeLogic(): RecipeLogic = MutableRecipesLogic<MutableFusionReactorMachine>(
            BiPredicate { recipe, machine ->
                val transaction = machine as? org.gtlcore.gtlcore.api.machine.trait.IFusionHeatTransaction
                    ?: return@BiPredicate false
                org.gtlcore.gtlcore.api.recipe.NativeFusionStartup.canStart(
                    transaction.`gtlcore$getFusionEnergy`(), transaction.`gtlcore$getFusionHeat`(),
                    org.gtlcore.gtlcore.api.recipe.NativeFusionStartup.requiredHeat(recipe)
                )
            }
        )
    }
}
