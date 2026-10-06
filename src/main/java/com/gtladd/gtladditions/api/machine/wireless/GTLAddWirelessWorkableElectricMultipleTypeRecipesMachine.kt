package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic

open class GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
    holder: BlockEntityCreationInfo,
    protected val dummyRecipeType: GTRecipeType,
    logic: GTLAddMultipleTypeWirelessRecipesLogic = GTLAddMultipleTypeWirelessRecipesLogic()
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, logic) {


    override fun getRecipeLogic(): GTLAddMultipleTypeWirelessRecipesLogic = super.getRecipeLogic() as GTLAddMultipleTypeWirelessRecipesLogic

    override fun getRecipeType(): GTRecipeType = dummyRecipeType
}