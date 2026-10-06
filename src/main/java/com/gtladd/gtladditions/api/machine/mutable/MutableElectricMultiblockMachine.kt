package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic

open class MutableElectricMultiblockMachine(holder: BlockEntityCreationInfo, logic: RecipeLogic = MutableRecipesLogic<MutableElectricMultiblockMachine>()) :
    WorkableElectricMultiblockMachine(holder, logic),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableElectricMultiblockMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutableElectricMultiblockMachine>

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun getMaxParallel(): Int = Int.MAX_VALUE

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine
}