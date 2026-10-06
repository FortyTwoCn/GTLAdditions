package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class MutableCoilElectricMultiblockMachine(holder: BlockEntityCreationInfo, logic: RecipeLogic = MutableRecipesLogic<MutableCoilElectricMultiblockMachine>()) :
    CoilWorkableElectricMultiblockMachine(holder, logic),
    IWirelessThreadModifierParallelMachine {

    private var threadPartMachine: IThreadModifierPart? = null

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableCoilElectricMultiblockMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutableCoilElectricMultiblockMachine>

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

    override fun getMaxParallel(): Int = min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 900.0).toInt())

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine
}