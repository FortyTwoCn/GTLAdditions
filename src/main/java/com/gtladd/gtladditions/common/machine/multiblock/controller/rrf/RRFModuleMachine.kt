package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import java.util.function.Consumer

abstract class RRFModuleMachine(holder: BlockEntityCreationInfo) :
    MultiblockControllerMachine(holder),
    IModularMachineModule<RecursiveReverseArray, RRFModuleMachine>,
    com.gregtechceu.gtceu.api.machine.feature.IMuiMachine,
    com.gregtechceu.gtceu.api.capability.IControllable {

    @field:SaveField
    @field:SyncToClient
    var enabled: Boolean = true

    @field:SaveField
    private var hostPosition: BlockPos? = null

    private var host: RecursiveReverseArray? = null
    private var tickSubscription: TickableSubscription? = null
    private var workingSound: Any? = null

    // ========================================
    // Recursive reverse buff
    // ========================================

    open fun isReadyForRecursiveReverseBuff(): Boolean = isFormed && enabled

    // ========================================
    // Module connection
    // ========================================

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }

    override fun getHost(): RecursiveReverseArray? = host

    override fun setHost(host: RecursiveReverseArray?) {
        this.host = host
    }

    override fun getHostType(): Class<RecursiveReverseArray> = RecursiveReverseArray::class.java

    override fun getHostScanPositions(): Array<BlockPos> = arrayOf(
        blockPos.offset(33, 7, 0),
        blockPos.offset(-33, 7, 0),
        blockPos.offset(0, 7, 33),
        blockPos.offset(0, 7, -33)
    )

    override fun onConnected(host: RecursiveReverseArray) {
        updateTickSubscription()
    }

    override fun onDisconnected() {
        updateTickSubscription()
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        if (!findAndConnectToHost()) removeFromHost(host)
        updateTickSubscription()
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        removeFromHost(host)
        updateTickSubscription()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(host)
    }

    override fun onMachineDestroyed() {
        removeFromHost(host)
    }

    protected fun updateTickSubscription() {
        if (isFormed && isConnectedToHost) {
            tickSubscription = subscribeServerTick(tickSubscription, ::moduleServerTick)
        } else {
            tickSubscription?.unsubscribe()
            tickSubscription = null
        }
    }

    private fun moduleServerTick() {
        if (enabled) startupUpdate()
        updateTickSubscription()
    }

    override fun clientTick() {
        super.clientTick()
        updateWorkingSound()
    }

    // ========================================
    // Working sound
    // ========================================

    private fun updateWorkingSound() {
        val sound = getWorkingSound()
        if (sound != null && shouldKeepWorkingSound()) {
            val currentSound = workingSound as? AutoReleasedSound
            if (currentSound?.soundEntry == sound && !currentSound.isStopped) return

            currentSound?.release()
            workingSound = sound.playAutoReleasedSound(::shouldKeepWorkingSound, blockPos, true, 0, 1.0f, 1.0f)
        } else {
            (workingSound as? AutoReleasedSound)?.release()
            workingSound = null
        }
    }

    private fun shouldKeepWorkingSound(): Boolean {
        val currentLevel = level ?: return false
        return ConfigHolder.INSTANCE.machines.machineSounds &&
            shouldPlayWorkingSound() &&
            !isRemoved &&
            currentLevel.isLoaded(blockPos) &&
            getMachine(currentLevel, blockPos) === this
    }

    open fun startupUpdate() = Unit

    protected open fun getWorkingSound(): SoundEntry? = null

    protected open fun shouldPlayWorkingSound(): Boolean = false

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = definition.additionalDisplay.apply(this, syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("RRFModuleMachineStatus", syncManager, {
            mutableListOf<Component>().also(::collectRRFModuleMachineStatusText)
        }, 130)
    )

    private fun collectRRFModuleMachineStatusText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed())
            .setWorkingStatus(enabled, enabled)
            .addComponent(
                "gtladditions.machine.recursive_reverse_array.module.bound".toComponent(
                    Component.literal(if (isConnectedToHost) "✓" else "x")
                        .withStyle(if (isConnectedToHost) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
    }








    override fun isWorkingEnabled(): Boolean = enabled

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        if (enabled == workingEnabled) return
        enabled = workingEnabled
        setChanged()
        updateTickSubscription()
    }

    override fun buildMainUI(main: brachy.modularui.widget.ParentWidget<*>, data: brachy.modularui.factory.PosGuiData,
                             manager: brachy.modularui.value.sync.PanelSyncManager, settings: brachy.modularui.screen.UISettings) {
        val column = brachy.modularui.widgets.layout.Flow.column().widthRel(1f).coverChildrenHeight()
        getWidgetsForDisplay(manager).forEach { column.child(it) }
        main.child(com.gtladd.gtladditions.api.gui.NativeHatchUI.scroll(column))
    }

    // ========================================
    // Metadata
    // ========================================

    open fun getModuleDisplayNameKey(): String = self().blockState.block.descriptionId

    override fun isRemote(): Boolean = super<MultiblockControllerMachine>.isRemote

    companion object {

    }
}
