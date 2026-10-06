package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeBusPartMachine
import net.neoforged.neoforge.fluids.FluidStack
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.ObjectIntPair
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.part.HugeFluidHatchPartMachine
import org.gtlcore.gtlcore.utils.Registries
import kotlin.random.Random

class MagnetorheologicalConvergenceCore(holder: BlockEntityCreationInfo) : RRFModuleMachine(holder) {

    @SaveField
    @SyncToClient
    private var focus = false

    @SaveField
    private var failItem1: Byte = 0

    @SaveField
    private var failItem2: Byte = 0

    @SaveField
    private var failFluid: Byte = 3

    @SaveField
    private var hasMagmatter = false

    @SaveField
    private var tick = 0

    private var itemHatch1: HugeBusPartMachine? = null
    private var itemHatch2: HugeBusPartMachine? = null
    private var fluidHatch: HugeFluidHatchPartMachine? = null
    private var magmatterHatch: ItemBusPartMachine? = null
    @field:SaveField
    private var requestedFluid: FluidStack = FluidStack.EMPTY
    @field:SaveField
    private var requestedItem1 = ItemStack.EMPTY
    @field:SaveField
    private var requestedItem2 = ItemStack.EMPTY
    @field:SaveField
    private var requestedAmount1 = 0
    @field:SaveField
    private var requestedAmount2 = 0
    private val requestedItems: Array<ObjectIntPair<Item>?> get() = arrayOf(
        if (requestedItem1.isEmpty) null else ObjectIntPair.of(requestedItem1.item, requestedAmount1),
        if (requestedItem2.isEmpty) null else ObjectIntPair.of(requestedItem2.item, requestedAmount2)
    )

    // ========================================
    // Recursive reverse buff
    // ========================================

    fun hasFocus(): Boolean = focus

    override fun isReadyForRecursiveReverseBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && focus

    // ========================================
    // Working sound
    // ========================================

    override fun getWorkingSound(): SoundEntry = GTSoundEntries.ELECTROLYZER.get()

    override fun shouldPlayWorkingSound(): Boolean = isReadyForRecursiveReverseBuff()

    // ========================================
    // Life cycle
    // ========================================

    override fun startupUpdate() {
        if (tick >= 2880) tick = 0
        if (tick % 240 == 0) consumeFocusInputs()
        if (tick % 20 == 0) consumeMagmatter()
        tick++
    }

    private fun initRequests() {
        val shuffled = (0..2).shuffled().take(2).sorted()
        requestedItem1 = ItemStack(itemForIndex(shuffled[0]))
        requestedItem2 = ItemStack(itemForIndex(shuffled[1]))
        requestedAmount1 = Random.nextInt(1, 16385)
        requestedAmount2 = Random.nextInt(1, 16385)

        requestedFluid = if (Random.nextBoolean()) {
            GTLMaterials.ExcitedDtec.getFluid(Random.nextInt(1, 1638401))
        } else {
            GTLMaterials.ExcitedDtsc.getFluid(Random.nextInt(1, 1638401))
        }
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        itemHatch1 = null
        itemHatch2 = null
        fluidHatch = null
        magmatterHatch = null
        for (part in parts) {
            when (part) {
                is HugeBusPartMachine -> if (itemHatch1 == null) {
                    itemHatch1 = part
                } else if (itemHatch2 == null) {
                    itemHatch2 = part
                }
                is HugeFluidHatchPartMachine -> fluidHatch = part
                is ItemBusPartMachine -> magmatterHatch = part
            }
        }
    }

    override fun invalidateStructure(substructureName: String) {
        itemHatch1 = null
        itemHatch2 = null
        fluidHatch = null
        magmatterHatch = null
        hasMagmatter = false
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
    }

    override fun onLoad() {
        super.onLoad()
        scheduleForNextServerTick {
            if (requestedItem1.isEmpty || requestedItem2.isEmpty || requestedFluid.isEmpty) {
                initRequests()
                setChanged()
            }
        }
    }

    // ========================================
    // Input consumption
    // ========================================

    private fun consumeFocusInputs() {
        failItem1 = 0
        failItem2 = 0
        failFluid = 3
        consumeItemHatch(itemHatch1)
        consumeItemHatch(itemHatch2)
        consumeTargetFluid()
        focus = failItem1 < 0 && failItem2 < 0 && failFluid < 0
    }

    private fun consumeItemHatch(hatch: HugeBusPartMachine?) {
        val storage = hatch?.inventory ?: return
        val stack = storage.getStackInSlot(0)
        if (stack.isEmpty) return

        val first = requestedItems[0]
        val second = requestedItems[1]
        if (failItem1 == 0.toByte() && first != null && stack.item == first.first()) {
            failItem1 = compareItemAmount(stack.count, first.rightInt())
        } else if (failItem2 == 0.toByte() && second != null && stack.item == second.first()) {
            failItem2 = compareItemAmount(stack.count, second.rightInt())
        }
        storage.extractItemInternal(0, stack.count, false)
    }

    private fun compareItemAmount(actual: Int, expected: Int): Byte = when {
        actual > expected -> 1
        actual < expected -> 2
        else -> -1
    }

    private fun consumeTargetFluid() {
        val requested = requestedFluid.takeUnless { it.isEmpty } ?: return
        val stored = fluidHatch?.tank?.storages?.firstOrNull()?.fluid ?: FluidStack.EMPTY
        if (!stored.isEmpty) {
            if (stored.fluid == requested.fluid) {
                failFluid = when {
                    stored.amount > requested.amount -> 4
                    stored.amount < requested.amount -> 5
                    else -> -1
                }
            }
            fluidHatch?.tank?.drainInternal(requested, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
        }
    }

    private fun consumeMagmatter() {
        val inventory = magmatterHatch?.inventory ?: run {
            hasMagmatter = false
            focus = false
            return
        }
        for (slot in 0 until inventory.size) {
            val stack = inventory.getStackInSlot(slot)
            if (stack.item == MAGMATTER_BLOCK && stack.count >= 2) {
                inventory.extractItemInternal(slot, 2, false)
                hasMagmatter = true
                return
            }
        }
        hasMagmatter = false
        focus = false
    }

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("MagnetorheologicalConvergenceCoreStatus", syncManager, {
            mutableListOf<Component>().also(::collectMagnetorheologicalConvergenceCoreStatusText)
        }, 130)
    )

    private fun collectMagnetorheologicalConvergenceCoreStatusText(textList: MutableList<Component>) {
        
        if (!isFormed) return
        textList.add(
            Component.translatable(
                "gtladditions.machine.magnetorheological_convergence_core.focus",
                Component.literal(if (focus) "✓" else "x")
                    .withStyle(if (focus) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(Component.translatable("gtladditions.machine.recursive_reverse_array.cycle_second", (tick % 240) / 20))
        addRequestDisplayText(textList)
        textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.6"))
        if (focus) {
            if (enabled) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.7"))
            return
        }
        if (failItem1 > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failItem1", 1))
        if (failItem2 > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failItem2", 2))
        if (failFluid > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failFluid"))
        if (!hasMagmatter) textList.add(Component.translatable("gtladditions.machine.magnetorheological_convergence_core.missing_magmatter"))
    }

    private fun addRequestDisplayText(textList: MutableList<Component>) {
        requestedItems.forEachIndexed { index, item ->
            if (item != null) {
                textList.add(
                    Component.translatable(
                        "gtladditions.machine.magnetorheological_convergence_core.required_item",
                        index + 1,
                        ItemStack(item.first()).displayName.copy().withStyle(ChatFormatting.GOLD),
                        FormattingUtil.formatNumbers(item.rightInt())
                    )
                )
            }
        }

        requestedFluid?.takeUnless { it.isEmpty }?.let { fluid ->
            textList.add(
                Component.translatable(
                    "gtladditions.machine.magnetorheological_convergence_core.required_fluid",
                    fluid.displayName.copy().withStyle(ChatFormatting.AQUA),
                    FormattingUtil.formatNumbers(fluid.amount)
                )
            )
        }
    }

    // ========================================
    // Persistence
    // ========================================

    // ========================================
    // Metadata
    // ========================================

    override fun getModuleDisplayNameKey(): String = "block.gtladditions.magnetorheological_convergence_core"

    companion object {

        private val BLACK_BODY_NAQUADRIA_SUPERSOLID = Registries.getItem("kubejs:black_body_naquadria_supersolid")
        private val QUANTUM_ANOMALY = Registries.getItem("kubejs:quantum_anomaly")
        private val HYPER_STABLE_SELF_HEALING_ADHESIVE = Registries.getItem("kubejs:hyper_stable_self_healing_adhesive")
        private val MAGMATTER_BLOCK = Registries.getItem("gtceu:magmatter_block")

        private fun itemForIndex(index: Int): Item = when (index) {
            0 -> BLACK_BODY_NAQUADRIA_SUPERSOLID
            1 -> QUANTUM_ANOMALY
            2 -> HYPER_STABLE_SELF_HEALING_ADHESIVE
            else -> Items.AIR
        }
    }
}