package com.gtladd.gtladditions.common.machine.hatch

import appeng.api.config.Actionable
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEKey
import appeng.api.storage.MEStorage
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerList
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.TransferHelper
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler
import net.neoforged.neoforge.fluids.FluidStack
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block

class InfinityDualHatchPartMachine(holder: BlockEntityCreationInfo) :
    TieredIOPartMachine(holder, 14, IO.IN),
    IDistinctPart,
    MEStorage,
    com.gregtechceu.gtceu.api.machine.feature.IMuiMachine {

    @field:SaveField
    private val shareTank: CatalystFluidStackHandler = attachTrait(CatalystFluidStackHandler(9, 16000, IO.IN, IO.NONE))

    @field:SaveField
    private val shareInventory: CatalystItemStackHandler = attachTrait(CatalystItemStackHandler(9, IO.IN, IO.NONE))

    @field:SaveField
    private val circuitInventory: com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait = attachTrait(com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait())

    @field:SaveField
    private val tank: FastNotifiableInputFluidTank = attachTrait(FastNotifiableInputFluidTank())

    @field:SaveField
    private val inventory: FastNotifiableInputItemStack = attachTrait(FastNotifiableInputItemStack())

    private val mainHandlers by lazy { RecipeHandlerList.of(IO.IN, paintingColor, inventory, tank, circuitInventory)
        .also { it.setDistinct(inventory.isDistinct) } }
    private val sharedHandlers by lazy { RecipeHandlerList.of(IO.IN, paintingColor, shareInventory, shareTank) }

    override fun getHandlerList(): RecipeHandlerList = mainHandlers
    override fun getRecipeHandlers(): List<RecipeHandlerList> = listOf(mainHandlers, sharedHandlers)

    override fun onPaintingColorChanged(color: Int) {
        super.onPaintingColorChanged(color)
        mainHandlers.setColor(color, true)
        sharedHandlers.setColor(color, true)
    }

    private var autoIOSubs: TickableSubscription? = null
    private var inventorySubs: com.gregtechceu.gtceu.utils.ISubscription? = null
    private var tankSubs: com.gregtechceu.gtceu.utils.ISubscription? = null
    private var hasFluidTransfer = false
    private var hasItemTransfer = false

    val itemIterator: ObjectIterator<Object2LongMap.Entry<ItemStack>>
        get() = Object2LongMaps.unmodifiable(inventory.getItemStorage()).object2LongEntrySet().iterator()

    val fluidIterator: ObjectIterator<Object2LongMap.Entry<FluidStack>>
        get() = Object2LongMaps.unmodifiable(tank.getFluidStorage()).object2LongEntrySet().iterator()

    override fun onLoad() {
        super<TieredIOPartMachine>.onLoad()
        // Native controllers may initialize these groups before saved trait data has been read.
        mainHandlers.setDistinct(inventory.isDistinct)
        mainHandlers.setColor(paintingColor)
        sharedHandlers.setDistinct(false)
        sharedHandlers.setColor(paintingColor)
        (this.level as? ServerLevel)?.let { serverLevel ->
            serverLevel.server.tell(TickTask(0, this::updateInventorySubscription))
            inventorySubs = inventory.addChangedListener(this::updateInventorySubscription)
            tankSubs = tank.addChangedListener(this::updateInventorySubscription)
        }
    }

    override fun onUnload() {
        autoIOSubs?.unsubscribe().also { autoIOSubs = null }
        super.onUnload()
        this.inventorySubs?.unsubscribe().also { this.inventorySubs = null }
        this.tankSubs?.unsubscribe().also { this.tankSubs = null }
    }

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        super.setWorkingEnabled(workingEnabled)
        this.updateInventorySubscription()
    }

    override fun onNeighborChanged(block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.onNeighborChanged(block, fromPos, isMoving)
        this.updateInventorySubscription()
    }



    private fun updateInventorySubscription() {
        this.level?.let { level ->
            this.hasItemTransfer = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                this.blockPos.relative(this.frontFacing),
                this.frontFacing.opposite
            ) != null
            this.hasFluidTransfer = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                this.blockPos.relative(this.frontFacing),
                this.frontFacing.opposite
            ) != null
        } ?: run {
            this.hasItemTransfer = false
            this.hasFluidTransfer = false
        }

        if (!this.isWorkingEnabled || !this.hasItemTransfer && !this.hasFluidTransfer) {
            this.autoIOSubs?.unsubscribe().also { this.autoIOSubs = null }
        } else {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs) { this.autoIO() }
        }
    }

    private fun autoIO() {
        if (this.offsetTimer % 5L == 0L && this.isWorkingEnabled) {
            if (this.hasItemTransfer) {
                this.inventory.importFromNearby(this.frontFacing)
            }
            if (this.hasFluidTransfer) {
                this.tank.importFromNearby(this.frontFacing)
            }
        }
    }

    private fun refundAll() {
        var changed = false
        if (this.hasItemTransfer) {
            this.inventory.exportToNearby(this.frontFacing)
            changed = true
        }
        if (this.hasFluidTransfer) {
            this.tank.exportToNearby(this.frontFacing)
            changed = true
        }
        if (changed) setWorkingEnabled(false)
    }

    // ========================================
    // ME integration
    // ========================================

    override fun insert(what: AEKey?, amount: Long, mode: Actionable?, source: IActionSource?): Long {
        MEStorage.checkPreconditions(what, amount, mode, source)

        return TransferHelper.insertFromME(
            this.inventory,
            this.tank,
            what!!,
            amount,
            mode!!
        )
    }

    override fun extract(what: AEKey?, amount: Long, mode: Actionable?, source: IActionSource?): Long = 0

    override fun getDescription(): Component? = "block.gtladditions.infinity_input_dual_hatch".toComponent

    // ========================================
    // GUI
    // ========================================



    override fun buildMainUI(main: brachy.modularui.widget.ParentWidget<*>, data: brachy.modularui.factory.PosGuiData,
                             manager: brachy.modularui.value.sync.PanelSyncManager, settings: brachy.modularui.screen.UISettings) {
        val column = brachy.modularui.widgets.layout.Flow.column().widthRel(1f).coverChildrenHeight().padding(5).childPadding(4)
            .child(com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("infinityContents", manager, {
                mutableListOf<Component>().also(::addDisplayText)
            }, 95))
            .child(com.gtladd.gtladditions.api.gui.NativeHatchUI.refund(::refundAll))
            .child(com.gtladd.gtladditions.api.gui.NativeHatchUI.sharedItems("infinitySharedItems", shareInventory.storage, manager))
            .child(com.gtladd.gtladditions.api.gui.NativeHatchUI.sharedFluids("infinitySharedFluids", shareTank))
        main.child(com.gtladd.gtladditions.api.gui.NativeHatchUI.scroll(column))
    }

    private fun addDisplayText(textList: MutableList<Component>) {
        inventory.addDisplayText(textList)
        tank.addDisplayText(textList)

        if (textList.isEmpty()) {
            textList.add("gtmthings.machine.huge_item_bus.tooltip.3".toComponent)
        }

        textList.add(
            0,
            "gtmthings.machine.huge_item_bus.tooltip.2".toComponent(
                inventory.realSize,
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            )
                .setStyle(
                    Style.EMPTY.withColor(ChatFormatting.GREEN)
                )
        )
        textList.add(
            1,
            "gtmthings.machine.huge_dual_hatch.tooltip.2".toComponent(
                tank.realSize,
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).setStyle(
                Style.EMPTY.withColor(ChatFormatting.GREEN)
            )
        )
    }



    override fun supportsDistinct(): Boolean = true

    override fun isDistinct(): Boolean = mainHandlers.isDistinct

    override fun setDistinct(isDistinct: Boolean) {
        mainHandlers.setDistinctAndNotify(isDistinct)
        setChanged()
    }

    init {
        this.workingEnabled = false
    }

    companion object {

    }
}
