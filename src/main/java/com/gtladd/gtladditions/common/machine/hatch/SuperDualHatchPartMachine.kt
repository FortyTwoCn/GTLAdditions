package com.gtladd.gtladditions.common.machine.hatch

import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.layout.Flow
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.utils.ISubscription
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.gui.NativeHatchUI
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeBusPartMachine
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.capabilities.Capabilities
import org.gtlcore.gtlcore.api.machine.trait.NativeLongFluidTank

open class SuperDualHatchPartMachine(holder: BlockEntityCreationInfo, tier: Int, vararg args: Any?) : HugeBusPartMachine(holder, tier, IO.IN, 9, *args) {
    @field:SaveField
    protected val tank = attachTrait(createTank())
    @field:SaveField
    protected val shareTank = attachTrait(CatalystFluidStackHandler(9, 16000, IO.IN, IO.NONE))
    protected var tankSubs: ISubscription? = null
    private var hasFluidTransfer = false
    private var hasItemTransfer = false

    init { workingEnabled = false }

    protected open fun createTank(): NativeLongFluidTank = object : NativeLongFluidTank(24, Long.MAX_VALUE shr 12, IO.IN) {
        override fun canCapOutput(): Boolean = true
    }

    fun getTankInventorySize(): Int = tank.tanks

    override fun mainRecipeHandlers(): MutableList<IRecipeHandler<*>> = super.mainRecipeHandlers().apply { add(tank) }
    override fun sharedRecipeHandlers(): MutableList<IRecipeHandler<*>> = super.sharedRecipeHandlers().apply { add(shareTank) }

    override fun onLoad() {
        super.onLoad()
        tankSubs = tank.addChangedListener(::updateInventorySubscription)
    }

    override fun onUnload() {
        tankSubs?.unsubscribe()
        tankSubs = null
        super.onUnload()
    }

    override fun refundAll() {
        super.refundAll()
        if (hasFluidTransfer) {
            setWorkingEnabled(false)
            tank.exportToNearby(frontFacing)
        }
    }

    override fun updateInventorySubscription() {
        val world = level
        hasItemTransfer = world?.getCapability(Capabilities.ItemHandler.BLOCK, blockPos.relative(frontFacing), frontFacing.opposite) != null
        hasFluidTransfer = world?.getCapability(Capabilities.FluidHandler.BLOCK, blockPos.relative(frontFacing), frontFacing.opposite) != null
        val canOutput = io == IO.OUT && (!tank.isEmpty || !getInventory().isEmpty)
        if (!isWorkingEnabled || (!canOutput && io != IO.IN) || (!hasItemTransfer && !hasFluidTransfer)) {
            autoIOSubs?.unsubscribe()
            autoIOSubs = null
        } else autoIOSubs = subscribeServerTick(autoIOSubs, ::autoIO)
    }

    override fun autoIO() {
        if (offsetTimer % 5L != 0L || !isWorkingEnabled) return
        if (io == IO.OUT) {
            if (hasItemTransfer) exportToNearby(getInventory(), frontFacing)
            if (hasFluidTransfer) tank.exportToNearby(frontFacing)
        } else if (io == IO.IN) {
            if (hasItemTransfer) getInventory().importFromNearby(frontFacing)
            if (hasFluidTransfer) tank.importFromNearby(frontFacing)
        }
        updateInventorySubscription()
    }

    override fun displayText(): MutableList<Component> {
        val lines = mutableListOf<Component>()
        var itemKinds = 0
        var fluidKinds = 0
        for (i in 0 until getInventory().slots) {
            val stack = getInventory().getStackInSlot(i)
            if (!stack.isEmpty) {
                lines.add(stack.hoverName.copy().withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" " + FormattingUtil.formatNumbers(stack.count)).withStyle(ChatFormatting.AQUA)))
                itemKinds++
            }
        }
        for (i in 0 until tank.tanks) {
            val stack = tank.getFluidInTank(i)
            if (!stack.isEmpty) {
                lines.add(stack.hoverName.copy().withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" " + FormattingUtil.formatNumbers(tank.`gtlcore$getStoredAmount`(i)) + " mB").withStyle(ChatFormatting.AQUA)))
                fluidKinds++
            }
        }
        if (lines.isEmpty()) lines.add(Component.translatable("gtmthings.machine.huge_item_bus.tooltip.3"))
        lines.add(0, Component.translatable("gtmthings.machine.huge_item_bus.tooltip.2", itemKinds, getInventory().slots).withStyle(ChatFormatting.GREEN))
        lines.add(1, Component.translatable("gtmthings.machine.huge_dual_hatch.tooltip.2", fluidKinds, tank.tanks).withStyle(ChatFormatting.GREEN))
        return lines
    }

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        val column = Flow.column().widthRel(1f).coverChildrenHeight().padding(5).childPadding(4)
            .child(com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("dualContents", manager, ::displayText, 95))
            .child(NativeHatchUI.refund(::refundAll))
            .child(NativeHatchUI.sharedItems("dualSharedItems", shareInventory.storage, manager))
            .child(NativeHatchUI.sharedFluids("dualSharedFluids", shareTank))
        main.child(NativeHatchUI.scroll(column))
    }
}
