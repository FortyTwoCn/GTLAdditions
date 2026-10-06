package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine

open class MutableElectricParallelHatchMultiblockMachine(holder: BlockEntityCreationInfo, logic: RecipeLogic = MutableRecipesLogic<MutableElectricParallelHatchMultiblockMachine>()) : MutableElectricMultiblockMachine(holder, logic) {
    override fun getMaxParallel(): Int = (this as IRecipeCapabilityMachine).parallelHatch.map { it.currentParallel }.orElse(1)
}