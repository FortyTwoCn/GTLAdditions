package com.gtladd.gtladditions.api.machine.multiblock

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

open class GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder: BlockEntityCreationInfo, logic: com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic = com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic()) : GTLAddWorkableElectricMultipleRecipesMachine(holder, logic) {
    override fun getMaxParallel(): Int = GTLRecipeModifiers.getHatchParallel(this)
}