package com.gtladd.gtladditions.api.gui.widget

import brachy.modularui.api.GuiAxis
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.widgets.SliderWidget
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.textfield.TextFieldWidget
import java.util.function.Consumer
import java.util.function.Supplier

/** One native synchronized value drives both the slider and its integral text field. */
class SliderTextFieldWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val minValue: Int, private val maxValue: Int,
    private val current: IntSyncValue
) : Flow(GuiAxis.Y) {
    constructor(x: Int, y: Int, width: Int, height: Int,
                minValue: Int, maxValue: Int, getter: Supplier<Int>, setter: Consumer<Int>, onChange: Consumer<Int>) :
        this(x, y, width, height, minValue, maxValue,
            IntSyncValue({ getter.get() }, { value ->
                val bounded = value.coerceIn(minValue, maxValue)
                setter.accept(bounded)
                onChange.accept(bounded)
            }).allowC2S())

    init {
        require(minValue <= maxValue)
        pos(x, y).size(width, height)
        child(TextFieldWidget().size(width, 14).setNumbers(minValue, maxValue).value(current))
        child(SliderWidget().size(width, 8).bounds(minValue.toDouble(), maxValue.toDouble()).stopper(1.0).value(current))
    }

    fun setCurrentValue(value: Int): SliderTextFieldWidget {
        current.setIntValue(value.coerceIn(minValue, maxValue), true, true)
        return this
    }
}
