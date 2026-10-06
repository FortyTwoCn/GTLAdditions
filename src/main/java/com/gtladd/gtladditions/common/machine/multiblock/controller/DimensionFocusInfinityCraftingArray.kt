package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class DimensionFocusInfinityCraftingArray(holder: BlockEntityCreationInfo) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<MolecularAssemblerMultiblockMachine, DimensionFocusInfinityCraftingArray> {

    @field:SaveField
    private var hostPosition: BlockPos? = null
    private var host: MolecularAssemblerMultiblockMachine? = null

    override fun getMaxParallel(): Int = 4096
    override fun getHost(): MolecularAssemblerMultiblockMachine? = host
    override fun getHostType(): Class<MolecularAssemblerMultiblockMachine> = MolecularAssemblerMultiblockMachine::class.java
    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHost(host: MolecularAssemblerMultiblockMachine?) {
        this.host = host
    }
    override fun setHostPosition(pos: BlockPos?) {
        this.hostPosition = pos
    }

    // ========================================
    // Host connection
    // ========================================

    override fun getHostScanPositions(): Array<out BlockPos> = arrayOf(
        blockPos.offset(0, -2, 7),
        blockPos.offset(0, -2, -7),
        blockPos.offset(7, -2, 0),
        blockPos.offset(-7, -2, 0)
    )

    override fun onConnected(host: MolecularAssemblerMultiblockMachine) {
        host.isInfinityMode = true
    }

    override fun removeFromHost(host: MolecularAssemblerMultiblockMachine?) {
        host?.isInfinityMode = false
        super.removeFromHost(host)
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineDestroyed() {
        removeFromHost(this.host)
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("DimensionFocusInfinityCraftingArrayStatus", syncManager, {
            mutableListOf<Component>().also(::collectDimensionFocusInfinityCraftingArrayStatusText)
        }, 130)
    )

    private fun collectDimensionFocusInfinityCraftingArrayStatusText(textList: MutableList<Component>) {
        
        if (!this.isFormed) return
        textList.add(
            (if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed").toComponent
        )
    }

    companion object {

    }
}