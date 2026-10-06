package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.AddMutableRecipesLogic

class AddMutableElectricParallelHatchMultiblockMachine(holder: BlockEntityCreationInfo) : MutableElectricParallelHatchMultiblockMachine(holder, AddMutableRecipesLogic<MutableElectricMultiblockMachine>()) {
    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): AddMutableRecipesLogic<MutableElectricMultiblockMachine> = super.getRecipeLogic() as AddMutableRecipesLogic<MutableElectricMultiblockMachine>

    override fun needConfirmMEStock(): Boolean = false
}