package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STELLAR_LGNITION

class HeliothermalPlasmaFabricator(holder: BlockEntityCreationInfo, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(holder, HeliothermalPlasmaFabricatorLogic()) {

    override fun getRecipeLogic(): HeliothermalPlasmaFabricatorLogic = super<ForgeOfTheAntichristModuleBase>.getRecipeLogic() as HeliothermalPlasmaFabricatorLogic

    companion object {
        class HeliothermalPlasmaFabricatorLogic(
            
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliothermalPlasmaFabricator = super.getMachine() as HeliothermalPlasmaFabricator

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType != STELLAR_LGNITION.get()
        }
    }
}