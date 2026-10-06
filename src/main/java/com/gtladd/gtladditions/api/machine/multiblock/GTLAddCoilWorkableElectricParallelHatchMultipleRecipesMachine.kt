package com.gtladd.gtladditions.api.machine.multiblock

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(holder: BlockEntityCreationInfo, logic: com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic = com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic(com.gtladd.gtladditions.api.machine.EBFChecks.EBF_CHECK)) : GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder, logic) {

    override fun getMaxParallel(): Int = GTLRecipeModifiers.getHatchParallel(this)
}