package com.gtladd.gtladditions.common.machine.hatch

import appeng.api.config.Actionable
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKeyType
import appeng.api.stacks.GenericStack
import appeng.api.storage.MEStorage
import appeng.api.storage.cells.StorageCell
import appeng.items.storage.BasicStorageCell
import appeng.me.cells.BasicCellHandler
import brachy.modularui.api.drawable.Text
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.BooleanSyncValue
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.slot.ItemSlot
import brachy.modularui.widgets.slot.ModularSlot
import brachy.modularui.widgets.slot.SlotGroup
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.capability.IControllable
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.item.MetaMachineItem
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler
import com.gregtechceu.gtceu.integration.ae2.gui.AEConfigWidget
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot
import com.gregtechceu.gtceu.utils.ISubscription
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput
import org.gtlcore.gtlcore.api.machine.trait.MEStock.IMETransfer
import org.gtlcore.gtlcore.integration.ae2.AEBufferedSlotTransactions
import org.gtlcore.gtlcore.integration.ae2.FastInfinityCell
import org.gtlcore.gtlcore.integration.ae2.IBufferedMEInventory
import org.gtlcore.gtlcore.integration.ae2.InfinityCell
import org.gtlcore.gtlcore.integration.ae2.storage.FastInfinityCellHandler
import org.gtlcore.gtlcore.integration.ae2.storage.InfinityCellHandler
import java.util.EnumSet

class MEBlockConversationHatch(holder: BlockEntityCreationInfo) :
    MultiblockPartMachine(holder), IGridConnectedMachine, IControllable, IMuiMachine, IBufferedMEInventory {
    @field:SyncToClient
    private var isOnline = false

    @field:SaveField
    @field:SyncToClient
    @field:RerenderOnChanged
    private var workingEnabled = true

    @field:SaveField
    private val nodeHolder = attachTrait(GridNodeHolder(this))

    @field:SaveField
    private val machineStorage = attachTrait(NotifiableItemStackHandler(1, IO.NONE, IO.BOTH) {
        object : CustomItemStackHandler(1) {
            override fun getSlotLimit(slot: Int): Int = 1
        }
    }.setFilter(::filter))

    @field:SaveField
    val aeItemHandler = attachTrait(ExportOnlyAEItemList(CONFIG_SIZE))
    private var autoIOSubs: TickableSubscription? = null
    private var inventorySubs: ISubscription? = null
    private val actionSource = IActionSource.ofMachine(nodeHolder.mainNode::getNode)
    private var stockInDroppedMachine = false

    private fun filter(stack: ItemStack): Boolean = when (val item = stack.item) {
        is FastInfinityCell -> true
        is InfinityCell -> item.keyType == AEKeyType.items()
        is BasicStorageCell -> item.keyType == AEKeyType.items()
        else -> false
    }

    fun getCellInventory(): StorageCell? {
        val cell = machineStorage.getStackInSlot(0)
        return when (cell.item) {
            is FastInfinityCell -> FastInfinityCellHandler.INSTANCE.getCellInventory(cell, null)
            is InfinityCell -> InfinityCellHandler.INSTANCE.getCellInventory(cell, null)
            is BasicStorageCell -> BasicCellHandler.INSTANCE.getCellInventory(cell, null)
            else -> null
        }
    }

    fun insertCell(what: AEItemKey, amount: Long, mode: Actionable): Long =
        getCellInventory()?.insert(what, amount, mode, actionSource) ?: 0L

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        val online = BooleanSyncValue { isOnline() }
        manager.syncValue("conversationOnline", online)
        registerConfigActions(manager)
        val group = SlotGroup("conversationCell", 1)
        manager.registerSlotGroup(group)
        main.child(Flow.column().coverChildren().center()
            .child(Text.dynamic { Component.translatable(if (online.boolValue) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline") }
                .asWidget().marginBottom(4))
            .child(AEConfigWidget(aeItemHandler, CONFIG_SIZE, false).syncManager(manager).size(8 * 18, 38))
            .child(ItemSlot().slot(ModularSlot(machineStorage.storage, 0).slotGroup(group)).marginTop(4)))
    }

    private fun registerConfigActions(manager: PanelSyncManager) {
        manager.registerServerSyncedAction("ae_config_set") { packet ->
            val index = packet.readVarInt()
            if (index !in 0 until CONFIG_SIZE) return@registerServerSyncedAction
            val held = manager.player.containerMenu.carried
            if (!held.isEmpty) aeItemHandler.inventory[index].config = GenericStack.fromItemStack(held)
        }
        manager.registerServerSyncedAction("ae_config_clear") { packet ->
            val index = packet.readVarInt()
            if (index in 0 until CONFIG_SIZE) aeItemHandler.inventory[index].config = null
        }
        manager.registerServerSyncedAction("ae_config_amount") { packet ->
            val index = packet.readVarInt()
            val amount = packet.readVarLong()
            if (index !in 0 until CONFIG_SIZE || amount <= 0) return@registerServerSyncedAction
            val slot = aeItemHandler.inventory[index]
            slot.config?.let { slot.config = GenericStack(it.what(), amount) }
        }
        manager.registerServerSyncedAction("ae_stock_pickup") { packet ->
            val index = packet.readVarInt()
            if (index !in 0 until CONFIG_SIZE || !manager.player.containerMenu.carried.isEmpty) return@registerServerSyncedAction
            val slot = aeItemHandler.inventory[index]
            val stock = slot.stock ?: return@registerServerSyncedAction
            val key = stock.what() as? AEItemKey ?: return@registerServerSyncedAction
            val amount = minOf(stock.amount(), key.toStack().maxStackSize.toLong())
            if (amount <= 0) return@registerServerSyncedAction
            (slot as IMETransfer).extractGenericStack(amount, false, true)
            manager.player.containerMenu.carried = key.toStack(amount.toInt())
        }
        manager.registerServerSyncedAction("ae_config_set_ghost") { packet ->
            val index = packet.readVarInt()
            if (index !in 0 until CONFIG_SIZE || packet.readBoolean()) return@registerServerSyncedAction
            val stack = ItemStack.STREAM_CODEC.decode(packet)
            if (!stack.isEmpty) aeItemHandler.inventory[index].config = GenericStack.fromItemStack(stack)
        }
    }

    override fun onRotated(oldFacing: Direction, newFacing: Direction) {
        super.onRotated(oldFacing, newFacing)
        mainNode.setExposedOnSides(EnumSet.of(newFacing))
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        updateInventorySubscription()
    }

    override fun onLoad() {
        super<MultiblockPartMachine>.onLoad()
        inventorySubs = aeItemHandler.addChangedListener(::updateInventorySubscription)
        scheduleForNextServerTick(::updateInventorySubscription)
    }

    override fun onUnload() {
        inventorySubs?.unsubscribe()
        inventorySubs = null
        autoIOSubs?.unsubscribe()
        autoIOSubs = null
        super<MultiblockPartMachine>.onUnload()
    }

    private fun updateInventorySubscription() {
        if (workingEnabled && isOnline) autoIOSubs = subscribeServerTick(autoIOSubs, ::autoIO)
        else {
            autoIOSubs?.unsubscribe()
            autoIOSubs = null
        }
    }

    private fun autoIO() {
        if (!workingEnabled || !shouldSyncME() || !updateMEStatus()) return
        val network = mainNode.grid?.storageService?.inventory ?: return
        refundCell(network)
        for (slot in aeItemHandler.inventory) AEBufferedSlotTransactions.balance(slot, network, actionSource)
        updateInventorySubscription()
    }

    private fun refundCell(network: MEStorage) {
        val cell = getCellInventory() ?: return
        for (entry in cell.availableStacks) {
            val accepted = network.insert(entry.key, entry.longValue, Actionable.MODULATE, actionSource)
            if (accepted > 0) cell.extract(entry.key, accepted, Actionable.MODULATE, actionSource)
        }
    }

    private fun flushStock() {
        val network = mainNode.grid?.storageService?.inventory ?: return
        if (isOnline) for (slot in aeItemHandler.inventory) AEBufferedSlotTransactions.flush(slot, network, actionSource)
    }

    override fun getBufferedMESlots(): Array<out ExportOnlyAESlot> = aeItemHandler.inventory

    override fun modifyDrops(drops: MutableList<ItemStack>) {
        super.modifyDrops(drops)
        flushStock()
        stockInDroppedMachine = drops.any { (it.item as? MetaMachineItem)?.block == blockState.block }
    }

    override fun onMachineDestroyed() {
        if (!stockInDroppedMachine) flushStock()
        super<MultiblockPartMachine>.onMachineDestroyed()
    }

    override fun saveToItem(stack: ItemStack, registries: HolderLookup.Provider) {
        super.saveToItem(stack, registries)
        val state = CompoundTag()
        aeItemHandler.inventory.forEachIndexed { i, slot -> state.put(i.toString(), slot.serializeNBT(registries)) }
        val custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        custom.put("gtladdConversationBuffer", state)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom))
    }

    override fun applyImplicitComponents(input: DataComponentInput) {
        super.applyImplicitComponents(input)
        val data = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        if (!data.contains("gtladdConversationBuffer", 10)) return
        val states = data.getCompound("gtladdConversationBuffer")
        aeItemHandler.inventory.forEachIndexed { i, slot -> slot.deserializeNBT(level!!.registryAccess(), states.getCompound(i.toString())) }
    }

    override fun getMainNode(): IManagedGridNode = nodeHolder.mainNode
    override fun isOnline(): Boolean = isOnline
    override fun setOnline(online: Boolean) {
        if (isOnline == online) return
        isOnline = online
        syncDataHolder.markClientSyncFieldDirty("isOnline")
        updateInventorySubscription()
    }

    override fun isWorkingEnabled(): Boolean = workingEnabled
    override fun setWorkingEnabled(enabled: Boolean) {
        if (workingEnabled == enabled) return
        workingEnabled = enabled
        setChanged()
        syncDataHolder.markClientSyncFieldDirty("workingEnabled")
        updateInventorySubscription()
    }

    companion object { private const val CONFIG_SIZE = 8 }
}
