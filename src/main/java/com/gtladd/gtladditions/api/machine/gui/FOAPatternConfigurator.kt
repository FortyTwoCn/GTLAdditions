package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.common.mui.GTGuiTextures
import com.gregtechceu.gtceu.common.mui.widgets.PopupPanel
import com.gregtechceu.gtceu.data.lang.LangHandler
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine
import brachy.modularui.api.drawable.Text
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.value.sync.SyncHandlers
import brachy.modularui.widgets.ButtonWidget
import brachy.modularui.widgets.ToggleButton
import brachy.modularui.widgets.textfield.TextFieldWidget
import net.minecraft.network.chat.Component

class FOAPatternConfigurator(private val machine: MESuperPatternBufferPartMachine) {
    private class FOAButton : ButtonWidget<FOAButton>()
    fun getTitle(): Component = Component.translatable("gtladditions.machine.me_super_pattern_buffer.foa_config.title")
    fun createButton(open: Runnable): ButtonWidget<*> = FOAButton().name("superPatternFOAButton").size(18)
        .overlay(Text.str("F"))
        .tooltipDynamic { tooltip ->
            tooltip.addLine(getTitle())
            LangHandler.getMultiLang("gtladditions.machine.me_super_pattern_buffer.foa_config.tooltip").forEach(tooltip::addLine)
        }
        .onMousePressed { _, button -> if (button == 0) { open.run(); true } else false }

    fun createPanel(sync: PanelSyncManager): PopupPanel {
        val enabled = SyncHandlers.bool(machine::isFOAModeEnabled, machine::setFOAModeEnabled).allowC2S()
        val multiplier = SyncHandlers.string({ machine.getFOAPatternOutputMultiplier().toString() }) { text ->
            text.toIntOrNull()?.let(machine::setFOAPatternOutputMultiplier)
        }.allowC2S()
        sync.syncValue("superPatternFOAMode", enabled)
        sync.syncValue("superPatternFOAMultiplier", multiplier)
        return PopupPanel.createPopupPanel("superPatternFOAConfigPanel", 190, 92)
            .child(Text.lang("gtladditions.machine.me_super_pattern_buffer.foa_config.title").asWidget().pos(6, 6))
            .child(ToggleButton().name("superPatternFOAToggle").value(enabled).size(20).pos(6, 28).overlay(*GTGuiTextures.BUTTON_POWER))
            .child(Text.lang("gtladditions.machine.me_super_pattern_buffer.foa_mode").asWidget().pos(32, 33))
            .child(Text.lang("gtladditions.machine.me_super_pattern_buffer.foa_multiplier").asWidget().pos(6, 65))
            .child(TextFieldWidget().name("superPatternFOAMultiplierInput").value(multiplier).size(54, 18).pos(126, 59))
    }
}
