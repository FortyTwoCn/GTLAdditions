package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.common.machine.hatch.VientianeTranscriptionNode
import com.gtladd.gtladditions.utils.MachineUtil.inputFluid
import net.neoforged.neoforge.fluids.FluidStack
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.level.material.Fluids
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.Registries
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SupratemporalBoostingEngine(holder: BlockEntityCreationInfo, vararg args: Any?) : RRFWorkableModuleMachine(holder) {

    @SaveField
    @SyncToClient
    private var temperature = MIN_TEMPERATURE

    @SaveField
    @SyncToClient
    private var overheated = false

    private var loadTemperatureLockTicks = 0

    private var redstoneNode: VientianeTranscriptionNode? = null

    // ========================================
    // Recursive reverse buff
    // ========================================

    fun isTemperatureOptimal(): Boolean = temperature in OPTIMAL_MIN..OPTIMAL_MAX

    fun isOverheated(): Boolean = overheated

    fun isBoostWindowActive(): Boolean = isReadyForRecursiveReverseBuff() && !overheated

    fun getEuMultiplierBuff(): Double = calculateEuMultiplier(getPerfectSupratemporalBoostParameter())

    // ========================================
    // Life cycle
    // ========================================

    override fun moduleTick() {
        if (offsetTimer % 20 != 0L) return

        if (loadTemperatureLockTicks > 0) {
            loadTemperatureLockTicks = max(loadTemperatureLockTicks - 20, 0)
            redstoneNode?.update(temperature)
            RecipeResult.of(this, null)
            return
        }

        val hostWorking = getHost()?.let { it.isFormed && !it.isRemoved } == true
        if (recipeLogic.isWorking && hostWorking && !overheated) {
            safePlusTemperature(WORKING_HEAT_PER_SECOND)
        } else {
            safeMinusTemperature(IDLE_COOLING_PER_SECOND)
        }

        consumeTemperatureFluidInput()
        RecipeResult.of(this, null)
    }

    override fun onLoad() {
        super.onLoad()
        if (!overheated && isTemperatureOptimal()) {
            loadTemperatureLockTicks = LOAD_TEMPERATURE_LOCK_TICKS
        }
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        redstoneNode = null
        for (part in parts) {
            if (part is VientianeTranscriptionNode) {
                redstoneNode = part
                part.controlMachine = true
            }
        }
    }

    override fun invalidateStructure(substructureName: String) {
        redstoneNode?.controlMachine = false
        redstoneNode = null
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
    }

    // ========================================
    // Temperature fluids
    // ========================================

    private fun consumeTemperatureFluidInput() {
        if (!overheated) {
            if (consumeTemperatureFluid(LAVA, 2500)) return
            if (consumeTemperatureFluid(BLAZE, 4600)) return
            if (consumeTemperatureFluid(RAW_STAR_MATTER_PLASMA, 14000)) return
        }
        if (consumeTemperatureFluid(ICE, -1900 - if (overheated) OVERHEAT_COOLING_PER_SECOND else 0)) return
        if (consumeTemperatureFluid(HELIUM, -3400 - if (overheated) OVERHEAT_COOLING_PER_SECOND else 0)) return
        consumeTemperatureFluid(CRYOTHEUM, -6700 - if (overheated) OVERHEAT_COOLING_PER_SECOND else 0)
    }

    private fun consumeTemperatureFluid(stack: FluidStack, delta: Int): Boolean {
        if (!inputFluid(this, stack)) return false
        if (delta >= 0) {
            safePlusTemperature(delta)
        } else {
            safeMinusTemperature(-delta)
        }
        return true
    }

    // ========================================
    // Utils
    // ========================================

    private fun safePlusTemperature(delta: Int) {
        temperature += delta
        if (temperature > OVERHEAT_TEMPERATURE) overheated = true
        redstoneNode?.update(temperature)
    }

    private fun safeMinusTemperature(delta: Int) {
        temperature = max(MIN_TEMPERATURE, temperature - delta)
        if (overheated && temperature <= MIN_TEMPERATURE) overheated = false
        redstoneNode?.update(temperature)
    }

    fun getPerfectSupratemporalBoostParameter(): Double {
        val ratio = when {
            temperature in OPTIMAL_MIN..OPTIMAL_MAX -> 1.0
            temperature < OPTIMAL_MIN -> 0.5 + 0.5 * ((temperature - MIN_TEMPERATURE).toDouble() / 45000.0).pow(28)
            else -> 1.0 - 0.85 * ((temperature - OPTIMAL_MAX).toDouble() / 4000.0).pow(0.42)
        }
        return max(0.0, min(1.0, ratio))
    }

    private fun calculateEuMultiplier(parameter: Double): Double = min(
        0.8,
        0.05 + 0.7932 * exp(-0.8473 * parameter.pow(2.326))
    )

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("SupratemporalBoostingEngineStatus", syncManager, {
            mutableListOf<Component>().also(::collectSupratemporalBoostingEngineStatusText)
        }, 130)
    )

    private fun collectSupratemporalBoostingEngineStatusText(textList: MutableList<Component>) {
        
        textList.add(Component.translatable("gtladditions.machine.supratemporal_boosting_engine.temperature", temperature))
        textList.add(
            Component.translatable(
                "gtladditions.machine.supratemporal_boosting_engine.overheated",
                Component.literal(if (overheated) "✓" else "x")
                    .withStyle(if (overheated) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(Component.translatable("gtladditions.machine.supratemporal_boosting_engine.eu_multiplier", getEuMultiplierBuff()))
    }

    // ========================================
    // Metadata
    // ========================================

    companion object {

        private const val MIN_TEMPERATURE = 48000
        private const val OPTIMAL_MIN = 93000
        private const val OPTIMAL_MAX = 97000
        private const val OVERHEAT_TEMPERATURE = 105000
        private const val WORKING_HEAT_PER_SECOND = 1300
        private const val IDLE_COOLING_PER_SECOND = 900
        private const val OVERHEAT_COOLING_PER_SECOND = 7125
        private const val LOAD_TEMPERATURE_LOCK_TICKS = 100

        private val LAVA = FluidStack(Fluids.LAVA, 100000)
        private val BLAZE = GTMaterials.Blaze.getFluid(100000)
        private val RAW_STAR_MATTER_PLASMA = GTLMaterials.RawStarMatter.getFluid(FluidStorageKeys.PLASMA, 100000)
        private val ICE = GTMaterials.Ice.getFluid(100000)
        private val HELIUM = GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, 100000)
        private val CRYOTHEUM by lazy { FluidStack(Registries.getFluid("kubejs:gelid_cryotheum"), 100000) }
    }
}