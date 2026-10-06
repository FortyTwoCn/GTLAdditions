package com.gtladd.gtladditions.api.gui.widget

import brachy.modularui.api.GuiAxis
import brachy.modularui.api.drawable.Text
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.widgets.ButtonWidget
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.textfield.TextFieldWidget
import com.google.common.primitives.Ints
import java.util.function.Consumer
import java.util.function.Supplier

/** Native integer editor whose adjustment buttons saturate instead of wrapping. */
class SafeIntInputWidget(getter: Supplier<Int?>?, setter: Consumer<Int?>?) : Flow(GuiAxis.X) {
    private val value = IntSyncValue({ getter?.get() ?: 0 }, { incoming: Int -> setter?.accept(incoming) }).allowC2S()

    init {
        widthRel(1f).coverChildrenHeight()
        child(AdjustmentButton().size(18).overlay(Text.of(net.minecraft.network.chat.Component.literal("−"))).onMousePressed { _, _ ->
            value.intValue = add(value.intValue, -1)
            true
        })
        child(TextFieldWidget().width(70).height(18).setNumbers(Int.MIN_VALUE, Int.MAX_VALUE).value(value))
        child(AdjustmentButton().size(18).overlay(Text.of(net.minecraft.network.chat.Component.literal("+"))).onMousePressed { _, _ ->
            value.intValue = add(value.intValue, 1)
            true
        })
    }

    private class AdjustmentButton : ButtonWidget<AdjustmentButton>()

    fun add(a: Int?, b: Int): Int = Ints.saturatedCast((a ?: 0).toLong() + b)
}
