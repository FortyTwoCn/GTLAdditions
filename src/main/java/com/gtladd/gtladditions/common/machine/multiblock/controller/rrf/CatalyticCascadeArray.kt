package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gtladd.gtladditions.common.machine.hatch.VientianeTranscriptionNode
import com.gtladd.gtladditions.common.modify.GTLAddSoundEntries
import net.neoforged.neoforge.fluids.FluidStack
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.part.HugeFluidHatchPartMachine
import kotlin.random.Random

class CatalyticCascadeArray(holder: BlockEntityCreationInfo) : RRFModuleMachine(holder) {

    @SaveField
    @SyncToClient
    private var outputBoost = false

    @SaveField
    @SyncToClient
    private var euBuff = false

    @SaveField
    @SyncToClient
    private var failedCycle = false

    @SaveField
    private var tick: Byte = 0

    @field:SaveField
    private var requestedFluid: FluidStack = FluidStack.EMPTY
    private var redstoneNode: VientianeTranscriptionNode? = null
    private var fluidHatch: HugeFluidHatchPartMachine? = null

    // ========================================
    // Recursive reverse buffs
    // ========================================

    fun hasOutputBoost(): Boolean = outputBoost && !failedCycle

    fun hasEuBuff(): Boolean = euBuff

    fun isReadyForRecursiveReverseEuBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && hasEuBuff()

    override fun isReadyForRecursiveReverseBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && hasOutputBoost()

    // ========================================
    // Working sound
    // ========================================

    override fun getWorkingSound(): SoundEntry = GTLAddSoundEntries.QUANTUM_OSCILLATION.get()

    override fun shouldPlayWorkingSound(): Boolean = super.isReadyForRecursiveReverseBuff() && (euBuff || hasOutputBoost())

    // ========================================
    // Life cycle
    // ========================================

    override fun startupUpdate() {
        if (offsetTimer % 20 != 0L) return

        if (tick == 0.toByte()) {
            val signal = Random.nextInt(1, 16)
            redstoneNode?.setRedStoneSignal(signal)
            requestedFluid = catalystForSignal(signal)
        }

        if (tick >= CATALYST_CHECK_SECOND) {
            val stored = fluidHatch?.tank?.storages?.firstOrNull()?.fluid ?: FluidStack.EMPTY
            when {
                stored.isEmpty -> {
                    outputBoost = false
                    euBuff = false
                    failedCycle = true
                }
                !failedCycle && stored.fluid == requestedFluid.fluid && stored.amount >= requestedFluid.amount -> {
                    fluidHatch?.tank?.drainInternal(requestedFluid, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
                    outputBoost = true
                    euBuff = true
                }
                else -> {
                    fluidHatch?.tank?.drainInternal(stored, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
                    outputBoost = false
                    euBuff = true
                    failedCycle = true
                }
            }
        }

        tick++
        if (tick >= CYCLE_SECONDS) {
            failedCycle = false
            tick = 0
            requestedFluid = FluidStack.EMPTY
        }
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        redstoneNode = null
        fluidHatch = null
        for (part in parts) {
            when (part) {
                is VientianeTranscriptionNode -> redstoneNode = part
                is HugeFluidHatchPartMachine -> fluidHatch = part
            }
        }
    }

    override fun invalidateStructure(substructureName: String) {
        redstoneNode = null
        fluidHatch = null
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
    }

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("CatalyticCascadeArrayStatus", syncManager, {
            mutableListOf<Component>().also(::collectCatalyticCascadeArrayStatusText)
        }, 130)
    )

    private fun collectCatalyticCascadeArrayStatusText(textList: MutableList<Component>) {
        
        textList.add(
            Component.translatable(
                "gtladditions.machine.catalytic_cascade_array.boost",
                Component.literal(if (hasOutputBoost()) "✓" else "x")
                    .withStyle(if (hasOutputBoost()) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(Component.translatable("gtladditions.machine.recursive_reverse_array.cycle_second", tick))
    }

    // ========================================
    // Persistence
    // ========================================

    // ========================================
    // Metadata
    // ========================================

    override fun getModuleDisplayNameKey(): String = "block.gtladditions.catalytic_cascade_array"

    companion object {

        private const val CATALYST_CHECK_SECOND = 7
        private const val CYCLE_SECONDS = 32

        private fun catalystForSignal(signal: Int): FluidStack = when (signal) {
            in 1..3 -> GTLMaterials.DimensionallyTranscendentCrudeCatalyst.getFluid(40000)
            in 4..6 -> GTLMaterials.DimensionallyTranscendentProsaicCatalyst.getFluid(40000)
            in 7..9 -> GTLMaterials.DimensionallyTranscendentResplendentCatalyst.getFluid(40000)
            in 10..12 -> GTLMaterials.DimensionallyTranscendentExoticCatalyst.getFluid(40000)
            in 13..15 -> GTLMaterials.DimensionallyTranscendentStellarCatalyst.getFluid(40000)
            else -> FluidStack.EMPTY
        }
    }
}