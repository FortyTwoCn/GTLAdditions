package com.gtladd.gtladditions.common.machine.hatch

import brachy.modularui.api.drawable.Text
import brachy.modularui.drawable.DynamicDrawable
import brachy.modularui.drawable.UITexture
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widget.Widget
import brachy.modularui.widgets.layout.Flow
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gtladd.gtladditions.api.gui.widget.SliderTextFieldWidget
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min

class OreProcessorHatch(holder: BlockEntityCreationInfo) : MultiblockPartMachine(holder), IMuiMachine {
    @field:SaveField
    @field:SyncToClient
    var color: Int = ThreadLocalRandom.current().nextInt(0x1000000) or (0xff shl 24)
        private set
    @field:SaveField
    @field:SyncToClient
    private var red = 0
    @field:SaveField
    @field:SyncToClient
    private var green = 0
    @field:SaveField
    @field:SyncToClient
    private var blue = 0

    val matchAll get() = this.red == color.red && this.green == color.green && this.blue == color.blue
    val firstTier: Int get() = findTier(this.red, color.red)
    val secondTier: Int get() = findTier(this.green, color.green)
    val thirdTier: Int get() = findTier(this.blue, color.blue)
    val parallelMultiplier: Int
        get() = when (firstTier) {
            3 -> 10
            2 -> 8
            1 -> 6
            else -> 4
        }
    val advancedThreadBonus: Int
        get() = when (firstTier) {
            3 -> 144
            2 -> 128
            1 -> 96
            else -> 72
        }
    val itemChanceBoost: Int get() = secondTier + 1
    val durationMultiplier: Double
        get() = when (thirdTier) {
            3 -> 0.4
            2 -> 0.5
            1 -> 0.65
            else -> 0.8
        }
    val fluidMultiplier: Double
        get() = when (thirdTier) {
            3 -> 0.4
            2 -> 0.5
            1 -> 0.65
            else -> 0.8
        }

    private fun findTier(x: Int, t: Int): Int = when {
        matchRange(x, t, 5) -> 3
        matchRange(x, t, 35) -> 2
        matchRange(x, t, 80) -> 1
        else -> 0
    }

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        val column = Flow.column().width(178).coverChildrenHeight().center().childPadding(3)
        val getters = listOf({ red }, { green }, { blue })
        val setters = listOf<(Int) -> Unit>({ red = it }, { green = it }, { blue = it })
        val fieldNames = listOf("red", "green", "blue")
        val targets = listOf({ color.red }, { color.green }, { color.blue })
        for (i in 0..2) {
            val value = IntSyncValue(getters[i], { incoming ->
                setters[i](incoming.coerceIn(0, 255))
                setChanged()
                syncDataHolder.markClientSyncFieldDirty(fieldNames[i])
            }).allowC2S()
            manager.syncValue("oreChannel$i", value)
            val row = Flow.row().coverChildren().childPadding(4)
                .child(SliderTextFieldWidget(0, 0, 80, 24, 0, 255, value))
            for (range in listOf(80, 35, 5)) {
                val matched = brachy.modularui.value.sync.BooleanSyncValue { matchRange(getters[i](), targets[i](), range) }
                manager.syncValue("oreLamp${i}_$range", matched)
                row.child(Lamp().size(20).background(DynamicDrawable { if (matched.boolValue) GREEN_LAMP else RED_LAMP }))
            }
            column.child(Text.lang("gtceu.machine.ore_hatch.gui.${i + 1}").asWidget()).child(row)
        }
        main.child(column)
    }

    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)
        val state = CompoundTag()
        state.putInt("color", color)
        state.putInt("red", red)
        state.putInt("green", green)
        state.putInt("blue", blue)
        val custom = components.build().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        custom.put("gtladdOreProcessor", state)
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(custom))
    }

    override fun applyImplicitComponents(input: DataComponentInput) {
        super.applyImplicitComponents(input)
        val custom = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        if (!custom.contains("gtladdOreProcessor", 10)) return
        val state = custom.getCompound("gtladdOreProcessor")
        color = state.getInt("color")
        red = state.getInt("red").coerceIn(0, 255)
        green = state.getInt("green").coerceIn(0, 255)
        blue = state.getInt("blue").coerceIn(0, 255)
        setChanged()
    }

    private class Lamp : Widget<Lamp>()

    companion object {
        private val RED_LAMP = UITexture.builder().location(ResourceLocation.fromNamespaceAndPath("gtladditions", "textures/gui/red_lamp.png")).build()
        private val GREEN_LAMP = UITexture.builder().location(ResourceLocation.fromNamespaceAndPath("gtladditions", "textures/gui/green_lamp.png")).build()
        private fun matchRange(x: Int, t: Int, range: Int): Boolean = x.isRange(t.safeLimitMinus(range, 0), t.safeLimitPlus(range, 0xff))

        private val Int.red: Int get() = (this shr 16) and 0xff
        private val Int.green: Int get() = (this shr 8) and 0xff
        private val Int.blue: Int get() = this and 0xff
        private fun Int.isRange(min: Int, max: Int): Boolean = this in min..max
        private fun Int.safeLimitPlus(plus: Int, limit: Int): Int = min((this + plus), limit)
        private fun Int.safeLimitMinus(minus: Int, limit: Int): Int = max((this - minus), limit)

    }
}
