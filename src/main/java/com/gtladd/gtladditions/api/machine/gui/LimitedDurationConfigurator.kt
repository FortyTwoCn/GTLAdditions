package com.gtladd.gtladditions.api.machine.gui

import brachy.modularui.api.drawable.Text
import brachy.modularui.api.widget.IWidget
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.textfield.TextFieldWidget
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine

/** Native menu field; its server setter validates client requests as well. */
class LimitedDurationConfigurator(private val machine: IGTLAddMultiRecipeMachine) {
    fun createConfigurator(syncManager: PanelSyncManager): IWidget {
        val duration = syncManager.getOrCreateSyncHandler("gtladdLimitedDuration", IntSyncValue::class.java) {
            IntSyncValue(java.util.function.IntSupplier(machine::getLimitedDuration),
                java.util.function.IntConsumer { value -> machine.setLimitedDuration(value.coerceIn(10, 200)) }).allowC2S()
        }
        return Flow.col().coverChildren().childPadding(2)
            .child(Text.lang("gtceu.machine.limitduration_configurator").asWidget())
            .child(TextFieldWidget().setNumbers(10, 200).value(duration).size(60, 16))
    }
}
