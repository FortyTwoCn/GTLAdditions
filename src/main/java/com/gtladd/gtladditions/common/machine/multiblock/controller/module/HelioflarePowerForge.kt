package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes.ALLOY_BLAST_RECIPES

class HelioflarePowerForge(holder: BlockEntityCreationInfo, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(holder, HelioflarePowerForgeLogic()) {


    override fun getRecipeLogic(): HelioflarePowerForgeLogic = super<ForgeOfTheAntichristModuleBase>.getRecipeLogic() as HelioflarePowerForgeLogic

    companion object {
        class HelioflarePowerForgeLogic(
            
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HelioflarePowerForge = super.getMachine() as HelioflarePowerForge

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType == ALLOY_BLAST_RECIPES.get()
        }
    }
}