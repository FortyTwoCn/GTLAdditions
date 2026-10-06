package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gtladd.gtladditions.api.machine.multiblock.NativeCoilStructure
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(
    holder: BlockEntityCreationInfo,
    logic: GTLAddMultipleWirelessRecipesLogic = GTLAddMultipleWirelessRecipesLogic(EBFChecks.WIRELESS_EBF_CHECK)
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, logic) {
    var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL
        private set
    @field:SyncToClient private var coilTemperature: Int = coilType.coilTemperature
    @field:SyncToClient private var coilTier: Int = coilType.tier


    override fun getMaxParallel(): Int = maxOf(1, 2.0.pow(coilTemperature / 900.0).toInt())

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        NativeCoilStructure.match(this, substructureName)?.let(::updateCoil)
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName == DEFAULT_STRUCTURE) updateCoil(CoilBlock.CoilType.CUPRONICKEL)
    }

    private fun updateCoil(coil: ICoilType) {
        coilType = coil
        coilTemperature = coil.coilTemperature
        coilTier = coil.tier
        syncDataHolder.markClientSyncFieldDirty("coilTemperature")
        syncDataHolder.markClientSyncFieldDirty("coilTier")
    }

    fun getCoilTier(): Int = coilTier
}
