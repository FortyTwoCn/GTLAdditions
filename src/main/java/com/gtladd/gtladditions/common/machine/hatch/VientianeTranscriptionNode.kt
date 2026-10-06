package com.gtladd.gtladditions.common.machine.hatch

import brachy.modularui.api.drawable.Text
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.BooleanSyncValue
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.ToggleButton
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.textfield.TextFieldWidget
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.common.mui.GTGuiTextures
import com.gregtechceu.gtceu.utils.RedstoneUtil
import net.minecraft.core.Direction

class VientianeTranscriptionNode(holder: BlockEntityCreationInfo) : MultiblockPartMachine(holder), IMuiMachine {
    @field:SaveField
    @field:SyncToClient
    private var min = MIN_TEMPERATURE
    @field:SaveField
    @field:SyncToClient
    private var max = MAX_TEMPERATURE
    @field:SaveField
    private var redStoneSignal = 0
    @field:SaveField
    @field:SyncToClient
    private var isInverted = false
    @field:SyncToClient
    var controlMachine: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            syncDataHolder.markClientSyncFieldDirty("controlMachine")
        }

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        if (!controlMachine) return
        val minimum = IntSyncValue({ min }, { incoming: Int -> min = incoming.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE); changed("min") }).allowC2S()
        val maximum = IntSyncValue({ max }, { incoming: Int -> max = incoming.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE); changed("max") }).allowC2S()
        val inverted = BooleanSyncValue({ isInverted }, { incoming: Boolean -> isInverted = incoming; changed("isInverted") }).allowC2S()
        main.child(Flow.column().coverChildren().center().childPadding(8)
            .child(ToggleButton().size(20).value(inverted).overlay(GTGuiTextures.BUTTON_DETECTOR_INVERT)
                .tooltip(false) { it.add("gui.gtceu.vientiane_transcription_node.invert.disabled") }
                .tooltip(true) { it.add("gui.gtceu.vientiane_transcription_node.invert.enabled") })
            .child(Flow.row().coverChildren().childPadding(5)
                .child(Text.lang("gui.gtceu.vientiane_transcription_node.min_temperature").asWidget().width(65))
                .child(TextFieldWidget().size(85, 18).setNumbers(MIN_TEMPERATURE, MAX_TEMPERATURE).value(minimum)))
            .child(Flow.row().coverChildren().childPadding(5)
                .child(Text.lang("gui.gtceu.vientiane_transcription_node.max_temperature").asWidget().width(65))
                .child(TextFieldWidget().size(85, 18).setNumbers(MIN_TEMPERATURE, MAX_TEMPERATURE).value(maximum))))
    }

    private fun changed(field: String) {
        setChanged()
        syncDataHolder.markClientSyncFieldDirty(field)
    }

    fun update(signal: Int) {
        // Temperatures are below 2^24, so the native float overload retains each integral temperature.
        val output = RedstoneUtil.computeRedstoneBetweenValues(signal.toFloat(), max.toFloat(), min.toFloat(), isInverted)
        if (redStoneSignal != output) setRedStoneSignal(output)
    }

    fun setRedStoneSignal(signal: Int) {
        redStoneSignal = signal.coerceIn(0, 15)
        setChanged()
        notifyBlockUpdate()
    }

    override fun getOutputSignal(side: Direction?): Int = if (side == frontFacing.opposite) redStoneSignal else 0
    override fun canConnectRedstone(side: Direction?): Boolean = false

    companion object {
        private const val MIN_TEMPERATURE = 48000
        private const val MAX_TEMPERATURE = 105000
    }
}
