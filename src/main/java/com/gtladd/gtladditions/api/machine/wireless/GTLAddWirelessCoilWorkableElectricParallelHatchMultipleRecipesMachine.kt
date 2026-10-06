package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

open class GTLAddWirelessCoilWorkableElectricParallelHatchMultipleRecipesMachine(
    holder: BlockEntityCreationInfo,
    logic: com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic = com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic(com.gtladd.gtladditions.api.machine.EBFChecks.WIRELESS_EBF_CHECK)
) : GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(holder, logic) {
    override fun getMaxParallel(): Int = GTLRecipeModifiers.getHatchParallel(this)
}