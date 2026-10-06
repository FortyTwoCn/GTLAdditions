package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import org.gtlcore.gtlcore.utils.MachineUtil
import kotlin.math.pow

class AdvancedSpaceElevatorModuleMachine(holder: BlockEntityCreationInfo) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, GTLAddMultipleRecipesLogic { machine ->
        (machine as AdvancedSpaceElevatorModuleMachine).host != null
    }),
    IModularMachineModule<SpaceElevatorMachine, AdvancedSpaceElevatorModuleMachine> {
    companion object {

    }

    @field:SyncToClient
    private var spaceElevatorTier = 0
    private var moduleTier = 0

    @field:SaveField
    private var hostPosition: BlockPos? = null
    private var host: SpaceElevatorMachine? = null

    override fun getHost(): SpaceElevatorMachine? = host

    override fun setHost(host: SpaceElevatorMachine?) {
        this.host = host
    }

    override fun getHostType(): Class<SpaceElevatorMachine> = SpaceElevatorMachine::class.java

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        this.hostPosition = pos
    }

    // ========================================
    // Update subscription for 80tick limit
    // ========================================

    private var updateSubs: TickableSubscription? = null
    private var remainingTicks: Int = 0

    private fun updateSubscription() {
        if (--this.remainingTicks > 0) {
            updateSubs = subscribeServerTick(updateSubs) { updateSubscription() }
        } else {
            update()
            updateSubs?.unsubscribe()
            updateSubs = null
        }
    }

    private fun update() {
        getSpaceElevatorTier()
        recipeLogic.updateTickSubscription()
    }

    private fun scheduleUpdate() {
        updateSubs?.unsubscribe()
        remainingTicks = 90
        updateSubscription()
    }

    // ========================================
    // Elevator connection
    // ========================================

    override fun onConnected(host: SpaceElevatorMachine) {
        if (host.recipeLogic.progress > 80) update() else scheduleUpdate()
    }

    override fun getHostScanPositions(): Array<out BlockPos?>? {
        (level as? ServerLevel)?.let { serverLevel ->
            val pos = blockPos
            val coordinates: Array<BlockPos> = arrayOf(
                pos.offset(8, -2, 3),
                pos.offset(8, -2, -3),
                pos.offset(-8, -2, 3),
                pos.offset(-8, -2, -3),
                pos.offset(3, -2, 8),
                pos.offset(-3, -2, 8),
                pos.offset(3, -2, -8),
                pos.offset(-3, -2, -8),
                *SpaceElevatorMKII.MODULE_OFFSETS.map { offset ->
                    pos.offset(offset[0], -2, offset[2])
                }.toTypedArray()
            )

            for (i in coordinates) {
                if (serverLevel.getBlockState(i).block === GTLBlocks.POWER_CORE.get()) {
                    return arrayOf(
                        i.offset(3, 2, 0),
                        i.offset(-3, 2, 0),
                        i.offset(0, 2, 3),
                        i.offset(0, 2, -3)
                    )
                }
            }
        }
        return MachineUtil.EMPTY_POS_ARRAY
    }

    private fun getSpaceElevatorTier() {
        host?.let { controller ->
            val logic = controller.getRecipeLogic()
            if (logic.isWorking && logic.progress > 80) {
                spaceElevatorTier = controller.tier - 7
                moduleTier = controller.casingTier
            } else {
                spaceElevatorTier = 0
                moduleTier = 0
            }
            return
        }
        spaceElevatorTier = 0
        moduleTier = 0
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

    override fun onWorking(): Boolean {
        val value = super.onWorking()
        if (this.offsetTimer % 10L == 0L) {
            this.getSpaceElevatorTier()
            if (this.spaceElevatorTier < 1) getRecipeLogic().setProgress(0)
        }
        return value
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("AdvancedSpaceElevatorModuleMachineStatus", syncManager, {
            mutableListOf<Component>().also(::collectAdvancedSpaceElevatorModuleMachineStatusText)
        }, 130)
    )

    private fun collectAdvancedSpaceElevatorModuleMachineStatusText(textList: MutableList<Component>) {
        
        if (this.isFormed) {
            if (this.offsetTimer % 10L == 0L) this.getSpaceElevatorTier()
            textList.add((if (this.spaceElevatorTier < 1) "tooltip.gtlcore.space_elevator_not_connected" else "tooltip.gtlcore.space_elevator_connected").toComponent)
        }
    }

    override fun getMaxParallel(): Int = if (host is SpaceElevatorMKII) {
        12.0.pow((moduleTier - 1).toDouble()).toInt()
    } else {
        8.0.pow((moduleTier - 1).toDouble()).toInt()
    }
}