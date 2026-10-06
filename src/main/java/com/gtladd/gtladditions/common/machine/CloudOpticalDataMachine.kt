package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.IO
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.value.sync.SyncHandlers
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widget.scroll.VerticalScrollData
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.layout.Grid
import brachy.modularui.widgets.slot.ItemSlot
import brachy.modularui.widgets.slot.SlotGroup
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler
import com.gregtechceu.gtceu.common.data.item.GTDataComponents
import com.gtladd.gtladditions.api.machine.ICloudTeamBindable
import com.gtladd.gtladditions.api.gui.NativeHatchUI
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableEnergyContainer
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.client.util.TooltipHelper
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gregtechceu.gtceu.utils.ResearchManager
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.hepdd.gtmthings.utils.TeamUtil
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.utils.TextUtil
import java.util.UUID

class CloudOpticalDataMachine(holder: BlockEntityCreationInfo) :
    TieredEnergyMachine(holder, GTValues.UIV, NotifiableEnergyContainer.receiverContainer(
        64L * GTValues.V[GTValues.UIV], GTValues.V[GTValues.UIV], 16
    )),
    IMuiMachine,
    IDataStickInteractable,
    ICloudTeamBindable {

    companion object {

        private val ENERGY_PER_DATA = GTValues.V[GTValues.UV] * 3 / 4
    }

    @field:SaveField
    @field:SyncToClient
    var teamId: UUID? = null
        private set

    @SaveField
    protected val importItems = attachTrait(NotifiableItemStackHandler(90, IO.NONE, IO.BOTH))

    @SaveField
    protected val createItem = attachTrait(NotifiableItemStackHandler(1, IO.NONE, IO.BOTH) { slots ->
        object : CustomItemStackHandler(slots) {
            override fun getSlotLimit(slot: Int): Int = 1
        }
    }.shouldDropInventoryInWorld(false))

    private val recipes: MutableSet<GTRecipe> = ObjectOpenHashSet()
    private var recipesDirty = true

    @SaveField
    private var isCreate = false

    private var hasPower = true
    private var energySubs: TickableSubscription? = null

    init {
        importItems
            .setFilter { stack ->
                ResearchManager.isStackDataItem(stack, true) && stack.has(GTDataComponents.RESEARCH_ITEM)
            }
            .addChangedListener(::markRecipesDirty)
        createItem
            .setFilter { stack -> stack.`is`(GTResearchMachines.CREATIVE_DATA_ACCESS_HATCH.asItem()) }
            .addChangedListener {
                isCreate = createItem.getStackInSlot(0).`is`(GTResearchMachines.CREATIVE_DATA_ACCESS_HATCH.asItem())
            }
    }

    override fun isRemote(): Boolean = super<TieredEnergyMachine>.isRemote

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        if (teamId == uuid) return
        teamId = uuid
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("teamId")
    }

    override fun onLoad() {
        super<TieredEnergyMachine>.onLoad()
        if (!isRemote) {
            CloudNetworkManager.registerDataMachine(this)
            markRecipesDirty()
            energySubs = subscribeServerTick(energySubs, ::updateEnergy)
        }
    }

    override fun onUnload() {
        super.onUnload()
        if (!isRemote) CloudNetworkManager.unregisterDataMachine(this)
        energySubs?.unsubscribe()
        energySubs = null
    }

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        if (!isRemote) setUUID(uuid)
    }

    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickShiftUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun onMachineDestroyed() {
        if (!isRemote) CloudNetworkManager.unregisterDataMachine(this)
        createItem.setStackInSlot(0, ItemStack.EMPTY)
        super.onMachineDestroyed()
    }

    fun getDataCount(): Long {
        var count = 1L
        for (slot in 0 until importItems.slots) {
            val stack = importItems.getStackInSlot(slot)
            if (!stack.isEmpty && ResearchManager.isStackDataItem(stack, true)) count++
        }
        return count
    }

    fun getEnergyDemand(): Long = if (isCreate) ENERGY_PER_DATA else getDataCount() * ENERGY_PER_DATA

    private fun updateEnergy() {
        val demand = getEnergyDemand()
        if (demand <= 0L) {
            hasPower = true
            return
        }
        hasPower = energyContainer.removeEnergy(demand) >= demand
    }

    fun rebuildData() {
        if (level == null) return
        recipes.clear()
        for (slot in 0 until importItems.slots) {
            val stack = importItems.getStackInSlot(slot)
            if (stack.isEmpty) continue
            val research = stack.get(GTDataComponents.RESEARCH_ITEM) ?: continue
            if (!ResearchManager.isStackDataItem(stack, true)) continue
            research.recipeType().getDataStickEntry(research.researchId())?.let(recipes::addAll)
        }
    }

    private fun markRecipesDirty() {
        recipesDirty = true
    }

    private fun refreshRecipesIfNeeded() {
        if (!recipesDirty || level == null) return
        rebuildData()
        recipesDirty = false
    }

    private fun getRecipes(): Set<GTRecipe> {
        refreshRecipesIfNeeded()
        return recipes
    }

    fun providesRecipe(recipe: GTRecipe): Boolean = hasPower && (isCreate || recipe in getRecipes())

    fun tryStoreResearchData(dataStack: ItemStack): Boolean {
        if (
            dataStack.isEmpty ||
            !ResearchManager.isStackDataItem(dataStack, true) ||
            !dataStack.has(GTDataComponents.RESEARCH_ITEM)
        ) {
            return false
        }

        for (slot in 0 until importItems.slots) {
            if (!importItems.getStackInSlot(slot).isEmpty) continue
            if (!importItems.insertItem(slot, dataStack, true).isEmpty) continue
            return importItems.insertItem(slot, dataStack.copy(), false).isEmpty
        }
        return false
    }

    override fun buildMainUI(
        mainWidget: ParentWidget<*>, guiData: PosGuiData,
        syncManager: PanelSyncManager, settings: UISettings
    ) {
        val dataGroup = SlotGroup("cloudResearchData", 9, false)
        val creativeGroup = SlotGroup("cloudCreativeData", 1, false)
        syncManager.registerSlotGroup(dataGroup)
        syncManager.registerSlotGroup(creativeGroup)
        val contents = Flow.column().width(166).coverChildrenHeight().childPadding(4)
        contents.child(com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("cloudDataStatus", syncManager, {
            mutableListOf<Component>().also(::addDisplayText)
        }, 78))
        contents.child(ItemSlot().slot(SyncHandlers.itemSlot(createItem.storage, 0).slotGroup(creativeGroup))
            .tooltipDynamic { it.addLine(Component.translatable("gui.gtladditions.cloud_data_machine.create")) })
        contents.child(Grid().size(162, 54).scrollable(VerticalScrollData())
            .minElementMargin(0, 0).minColWidth(18).minRowHeight(18)
            .gridOfSizeWidth(90, 9) { _, _, slot ->
                ItemSlot().slot(SyncHandlers.itemSlot(importItems.storage, slot).slotGroup(dataGroup))
            })
        mainWidget.child(contents)
    }

    private fun addDisplayText(textList: MutableList<Component>) {
        if (isRemote) return
        textList.add(self().blockState.block.name)
        val boundTeamId = teamId
        if (boundTeamId == null) {
            textList.add(Component.translatable("gui.gtladditions.cloud.not_bound"))
        } else {
            if (TeamUtil.hasOwner(level, boundTeamId)) {
                textList.add(
                    Component.translatable(
                        "gui.gtladditions.cloud.bind_success",
                        TeamUtil.GetName(level, boundTeamId)
                    )
                )
            }
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.cloud_count",
                    CloudNetworkManager.getLoadedDataMachineCount(boundTeamId)
                )
            )
        }
        if (isCreate) {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.recipes_count.1",
                    Component.literal(
                        TextUtil.full_color(
                            Component.translatable("gui.gtladditions.cloud_data_machine.recipes_count.2").string
                        )
                    ).withStyle { style -> style.withColor(TooltipHelper.RAINBOW.current) }
                )
            )
        } else {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.recipes_count.0",
                    getRecipes().size
                )
            )
        }
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_data_machine.energy_demand",
                FormattingUtil.formatNumbers(getEnergyDemand())
            )
        )
        textList.add(
            Component.translatable(
                if (hasPower) {
                    "gui.gtladditions.cloud_data_machine.power_normal"
                } else {
                    "gui.gtladditions.cloud_data_machine.power_insufficient"
                }
            ).withStyle { style -> style.withColor(if (hasPower) 0x55FF55 else 0xFF5555) }
        )
    }
}
