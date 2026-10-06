package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY

class HeliofluixMeltingCore(holder: BlockEntityCreationInfo, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(holder, HeliofluixMeltingCoreLogic()) {


    override fun getRecipeLogic(): HeliofluixMeltingCoreLogic = super<ForgeOfTheAntichristModuleBase>.getRecipeLogic() as HeliofluixMeltingCoreLogic

    companion object {
        class HeliofluixMeltingCoreLogic(
            
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliofluixMeltingCore = super.getMachine() as HeliofluixMeltingCore

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType == CHAOTIC_ALCHEMY.get()
        }
    }
}