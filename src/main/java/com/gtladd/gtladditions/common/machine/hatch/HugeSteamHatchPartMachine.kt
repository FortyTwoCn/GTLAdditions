package com.gtladd.gtladditions.common.machine.hatch

import brachy.modularui.api.drawable.Text
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.FluidSlotSyncHandler
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.slot.FluidSlot
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine
import com.gtladd.gtladditions.api.gui.NativeHatchUI
import net.minecraft.network.chat.Component

class HugeSteamHatchPartMachine(holder: BlockEntityCreationInfo) : FluidHatchPartMachine(
    holder, 0, IO.IN,
    NotifiableFluidTank(1, Int.MAX_VALUE, IO.IN).setFilter { it.fluid.`is`(GTMaterials.Steam.fluidTag) }
) {
    init { circuitSlot.setEnabled(false) }
    override fun swapIO(): Boolean = false

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        main.child(Flow.column().widthRel(1f).coverChildrenHeight().padding(5).childPadding(4)
            .child(Text.lang("gtceu.gui.fluid_amount").asWidget())
            .child(NativeHatchUI.componentList("hugeSteamAmount", manager, {
                listOf(Component.literal(tank.getFluidInTank(0).amount.toString()))
            }, 35))
            .child(FluidSlot().name("steam").syncHandler(FluidSlotSyncHandler(tank.storages[0]).canFillSlot(true))))
    }
}
