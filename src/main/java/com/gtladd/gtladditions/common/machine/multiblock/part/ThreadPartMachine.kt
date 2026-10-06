package com.gtladd.gtladditions.common.machine.multiblock.part

import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.layout.Flow
import brachy.modularui.widgets.slot.ItemSlot
import brachy.modularui.widgets.slot.ModularSlot
import brachy.modularui.widgets.slot.SlotGroup
import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy.getAdditionalMultiplier
import com.gtladd.gtladditions.api.gui.NativeHatchUI
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

open class ThreadPartMachine(holder: BlockEntityCreationInfo) : MultiblockPartMachine(holder), IThreadModifierPart, IMuiMachine {
    @field:SaveField
    private val astralArrayInventory = attachTrait(NotifiableItemStackHandler(1, IO.NONE, IO.BOTH)
        .setFilter { it.`is`(GTLAddItems.ASTRAL_ARRAY.asItem()) })
    private var threadMultiplier = 0
    private var threadCount = 0

    init { astralArrayInventory.addChangedListener(::reCalculateThreadCount) }

    private fun reCalculateThreadCount() {
        threadCount = Ints.saturatedCast(astralArrayInventory.getStackInSlot(0).count * 64L * threadMultiplier)
    }

    override fun buildMainUI(main: ParentWidget<*>, data: PosGuiData, manager: PanelSyncManager, settings: UISettings) {
        val group = SlotGroup("astralArray", 1)
        manager.registerSlotGroup(group)
        main.child(Flow.column().widthRel(1f).coverChildrenHeight().padding(4)
            .child(NativeHatchUI.componentList("threadModifier", manager, {
                listOf(
                    "gtladditions.thread_modifier_hatch.thread_multiplier".toComponent(threadMultiplier.toString().literal.withStyle(ChatFormatting.GOLD))
                        .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            "gtladditions.thread_modifier_hatch.hover".toComponent))),
                    "gtladditions.thread_modifier_hatch.thread_count".toComponent(threadCount.toString().literal.withStyle(ChatFormatting.GOLD))
                )
            }, 65))
            .child(ItemSlot().slot(ModularSlot(astralArrayInventory.storage, 0).slotGroup(group))
                .tooltip { it.addLine("gtladditions.thread_modifier_hatch.base_thread".toComponent) }))
    }

    override fun addedToController(controller: MultiblockControllerMachine, substructureName: String) {
        super.addedToController(controller, substructureName)
        threadMultiplier = 0
        if (controller is IThreadModifierMachine) {
            controller.setThreadPartMachine(this)
            if (controller is IRecipeLogicMachine) {
                (controller.recipeLogic as? MutableRecipesLogic<*>)?.setUseMultipleRecipes(true)
                threadMultiplier = getAdditionalMultiplier(controller.definition)
            }
        }
        reCalculateThreadCount()
    }

    override fun removedFromController(controller: MultiblockControllerMachine) {
        super.removedFromController(controller)
        threadMultiplier = 0
        threadCount = 0
    }

    override fun canShared(controller: MultiblockControllerMachine, substructureName: String): Boolean = false
    override fun getThreadCount(): Int = threadCount
}
