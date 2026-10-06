package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.DataModuleBindingError
import com.gtladd.gtladditions.api.machine.DataModuleBindingResult
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.IWirelessBindableTarget
import com.gtladd.gtladditions.common.data.RecursiveReverseBuffState
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo
import java.util.function.Consumer

class RecursiveReverseArray(holder: BlockEntityCreationInfo) :
    MultiblockControllerMachine(holder),
    IModularMachineHost<RecursiveReverseArray>,
    IWirelessBindableTarget,
    com.gregtechceu.gtceu.api.machine.feature.IMuiMachine,
    com.gregtechceu.gtceu.api.capability.IControllable {

    @field:SyncToClient
    @field:SaveField
    private var boundForgePos: BlockPos? = null

    @field:SaveField
    var enabled: Boolean = true

    @field:SaveField
    private var buffRollCounter: Long = 0

    private var boundForge: ForgeOfTheAntichrist? = null
    private val modules: MutableSet<IModularMachineModule<RecursiveReverseArray, *>> = ObjectOpenHashSet()
    private var tickSubscription: TickableSubscription? = null

    // ========================================
    // Wireless binding
    // ========================================

    override val bindingTargetType: ResourceLocation = BINDING_TYPE

    override fun acceptsBindingSource(type: ResourceLocation): Boolean = type == ForgeOfTheAntichrist.BINDING_TYPE

    override fun bindResolvedSource(source: IWirelessBindableSource<*>): DataModuleBindingResult {
        val forge = source as? ForgeOfTheAntichrist ?: return DataModuleBindingResult.Failure(
            DataModuleBindingError.TARGET_MISMATCH,
            "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
        )

        if (forge.level?.dimension() != level?.dimension()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.dimension_mismatch".toComponent
            )
        }
        if (!forge.isFormed() || !isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
            )
        }

        if (boundForge != forge) {
            boundForge?.onUnbound(this)
        }
        boundForge = forge
        boundForgePos = forge.blockPos

        forge.onBound(this)
        return DataModuleBindingResult.BoundSuccess(source, forge.blockPos, getBoundMessage(source, forge.blockPos))
    }

    override fun unbindSource() = unbindForge(clearBinding = true)

    fun unbindForge(clearBinding: Boolean) {
        val oldForge = boundForge
        boundForge = null
        if (clearBinding) boundForgePos = null
        oldForge?.disconnectRecursiveReverseArray(this, clearBinding)
    }

    private fun reconnectSource(): Boolean {
        val forgePos = boundForgePos ?: return false
        val machine = (level as? ServerLevel)?.let { MetaMachine.getMachine(it, forgePos) as? ForgeOfTheAntichrist }
            ?: return false
        return bindResolvedSource(machine).isSuccess
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<RecursiveReverseArray, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos> = MODULE_OFFSETS.map { blockPos!!.offset(it) }.toTypedArray()

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(33, 0, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.EAST,
            Direction.UP,
            MultiBlockMachine.CATALYTIC_CASCADE_ARRAY.get()
        ),
        ModuleRenderInfo(
            BlockPos(-33, 0, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.WEST,
            Direction.UP,
            MultiBlockMachine.MAGNETORHEOLOGICAL_CONVERGENCE_CORE.get()
        ),
        ModuleRenderInfo(
            BlockPos(0, 33, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            Direction.NORTH,
            MultiBlockMachine.SPACETIME_STASIS_DEVICE.get()
        ),
        ModuleRenderInfo(
            BlockPos(0, -33, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.DOWN,
            Direction.NORTH,
            MultiBlockMachine.SUPRATEMPORAL_BOOSTING_ENGINE.get()
        )
    )

    // ========================================
    // Buff state
    // ========================================

    fun getBuffState(): RecursiveReverseBuffState {
        val context = createBuffContext()
        val buffRolledActive = context.machinesReady && context.boostReady && rollBuffPackage(context.buffProbability)
        return buildBuffState(context, buffRolledActive)
    }

    fun getBoostPreview(): BoostPreview {
        val context = createBuffContext()
        return getBoostPreview(context)
    }

    fun isStarRitualGateActive(): Boolean {
        val context = createBuffContext()
        return context.machinesReady && context.boostReady && context.boost?.isTemperatureOptimal() == true
    }

    private fun createBuffContext(): BuffContext {
        val forge = boundForge
        val boost = modules.filterIsInstance<SupratemporalBoostingEngine>().firstOrNull()
        val boostReady = boost?.isBoostWindowActive() == true
        val machinesReady = enabled && forge?.isFormed() == true && isFormed()
        val buffProbability = if (enabled && boostReady) boost.getPerfectSupratemporalBoostParameter() else 0.0
        return BuffContext(forge, boost, boostReady, machinesReady, buffProbability)
    }

    private fun getBoostPreview(context: BuffContext): BoostPreview {
        val commonActive = context.machinesReady && context.boostReady && context.buffProbability > 0.0
        return BoostPreview(
            commonActive = commonActive,
            probability = context.buffProbability,
            outputMultiplier = getPreviewOutputMultiplier(commonActive),
            activeModuleKeys = getPreviewActiveModuleKeys(context, commonActive),
            diagnostics = buildDiagnostics(context)
        )
    }

    private fun getPreviewOutputMultiplier(commonActive: Boolean): Double {
        val catalyticActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseBuff() && it.hasOutputBoost() }
        return if (catalyticActive) 2.0 else 1.0
    }

    private fun getPreviewActiveModuleKeys(context: BuffContext, commonActive: Boolean): List<String> {
        if (!commonActive) return emptyList()

        val activeModules = mutableListOf<String>()
        if (context.boost != null) activeModules.add("block.gtladditions.supratemporal_boosting_engine")
        if (modules.any { it is CatalyticCascadeArray && (it.isReadyForRecursiveReverseBuff() || it.isReadyForRecursiveReverseEuBuff()) }) {
            activeModules.add("block.gtladditions.catalytic_cascade_array")
        }
        if (modules.any { it is MagnetorheologicalConvergenceCore && it.isReadyForRecursiveReverseBuff() && it.hasFocus() }) {
            activeModules.add("block.gtladditions.magnetorheological_convergence_core")
        }
        if (modules.any { it is SpacetimeStasisDevice && it.isReadyForRecursiveReverseBuff() }) {
            activeModules.add("block.gtladditions.spacetime_stasis_device")
        }
        return activeModules
    }

    private fun buildBuffState(context: BuffContext, buffRolledActive: Boolean): RecursiveReverseBuffState {
        val boost = context.boost
        val commonActive = context.machinesReady && context.boostReady && buffRolledActive
        val starRitualGateActive = context.machinesReady && context.boostReady && boost?.isTemperatureOptimal() == true
        val catalyticActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseBuff() && it.hasOutputBoost() }
        val catalyticEuActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseEuBuff() && it.hasEuBuff() }
        val catalyticEuMultiplier = if (catalyticEuActive) CATALYTIC_EU_MULTIPLIER else 1.0
        val magnetorheologicalActive = commonActive && modules.any { it is MagnetorheologicalConvergenceCore && it.isReadyForRecursiveReverseBuff() && it.hasFocus() }
        val stasisActive = commonActive && modules.any { it is SpacetimeStasisDevice && it.isReadyForRecursiveReverseBuff() }

        return RecursiveReverseBuffState(
            commonActive = commonActive,
            boostEngineInstalled = boost != null,
            boostEngineOptimal = boost?.isTemperatureOptimal() == true,
            boostEngineOverheated = boost?.isOverheated() == true,
            starRitualGateActive = starRitualGateActive,
            catalyticCascadeActive = catalyticActive,
            catalyticCascadeEuActive = catalyticEuActive,
            magnetorheologicalConvergenceActive = magnetorheologicalActive,
            spacetimeStasisActive = stasisActive,
            outputMultiplier = if (catalyticActive) 2.0 else 1.0,
            euMultiplier = if (commonActive) (boost?.getEuMultiplierBuff() ?: 1.0) * catalyticEuMultiplier else 1.0,
            diagnostics = buildDiagnostics(context)
        )
    }

    private fun rollBuffPackage(probability: Double): Boolean {
        if (probability >= 1.0) return true
        if (probability <= 0.0) return false
        return RandomSource.create(nextBuffRollSeed()).nextDouble() < probability
    }

    private fun nextBuffRollSeed(): Long {
        val roll = buffRollCounter++
        return (blockPos?.asLong() ?: 0L) xor (roll * BUFF_ROLL_COUNTER_SALT) xor BUFF_ROLL_SEED_SALT
    }

    private fun buildDiagnostics(context: BuffContext): List<String> {
        val diagnostics = mutableListOf<String>()
        if (!enabled) {
            diagnostics.add("gtladditions.machine.recursive_reverse_array.status.disabled")
            return diagnostics
        }
        val forge = context.forge
        val boost = context.boost
        if (forge == null) diagnostics.add("gtladditions.machine.recursive_reverse_array.status.no_forge")
        if (boost == null) diagnostics.add("gtladditions.machine.recursive_reverse_array.status.no_boost")
        if (boost != null) {
            if (boost.isOverheated()) {
                diagnostics.add("gtladditions.machine.recursive_reverse_array.status.boost_overheated")
            } else if (!boost.isReadyForRecursiveReverseBuff()) {
                diagnostics.add("gtladditions.machine.recursive_reverse_array.status.boost_not_running")
            }
        }
        return diagnostics
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        safeClearModules()
        scanAndConnectModules()
        reconnectSource()
        updateTickSubscription()
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        safeClearModules()
        unbindForge(clearBinding = false)
        updateTickSubscription()
    }

    override fun onMachineDestroyed() {
        safeClearModules()
        unbindForge(clearBinding = true)
    }

    override fun onUnload() {
        super.onUnload()
        boundForge?.disconnectRecursiveReverseArray(this, clearBinding = false)
        boundForge = null
    }

    private fun updateTickSubscription() {
        if (isFormed) {
            tickSubscription = subscribeServerTick(tickSubscription, ::updateTickSubscription)
        } else {
            tickSubscription?.unsubscribe()
            tickSubscription = null
        }
    }

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("RecursiveReverseArrayStatus", syncManager, {
            mutableListOf<Component>().also(::collectRecursiveReverseArrayStatusText)
        }, 130)
    )

    private fun collectRecursiveReverseArrayStatusText(textList: MutableList<Component>) {
        
        val forgePos = boundForgePos
        if (forgePos == null) {
            textList.add("gtladditions.machine.recursive_reverse_array.unbound".toComponent)
        } else {
            textList.add(
                "gtladditions.machine.recursive_reverse_array.bound".toComponent(
                    Component.literal(forgePos.x.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(forgePos.y.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(forgePos.z.toString()).withStyle(ChatFormatting.LIGHT_PURPLE)
                )
            )
        }

        val boostPreview = getBoostPreview()
        textList.add(
            "gtladditions.machine.recursive_reverse_array.common_active".toComponent(
                Component.literal(if (boostPreview.commonActive) "✓" else "x")
                    .withStyle(if (boostPreview.commonActive) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(
            "gtladditions.machine.recursive_reverse_array.buff_probability".toComponent(
                Component.literal("${FormattingUtil.DECIMAL_FORMAT_2F.format(boostPreview.probability * 100.0)}%")
                    .withStyle(ChatFormatting.AQUA)
            )
        )
        textList.add(
            "gtladditions.machine.recursive_reverse_array.active_modules".toComponent(
                createActiveModulesComponent(boostPreview.activeModuleKeys)
            )
        )
        boostPreview.diagnostics.forEach { textList.add(it.toComponent) }
    }

    private fun createActiveModulesComponent(activeModuleKeys: List<String>): Component {
        if (activeModuleKeys.isEmpty()) return Component.literal("-").withStyle(ChatFormatting.GOLD)

        val component = Component.empty()
        activeModuleKeys.forEachIndexed { index, key ->
            if (index > 0) component.append(Component.literal(" / ").withStyle(ChatFormatting.GOLD))
            component.append(Component.translatable(key).withStyle(ChatFormatting.GOLD))
        }
        return component
    }






    override fun isWorkingEnabled(): Boolean = enabled

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        if (enabled == workingEnabled) return
        enabled = workingEnabled
        setChanged()
        updateTickSubscription()
    }

    override fun buildMainUI(main: brachy.modularui.widget.ParentWidget<*>, data: brachy.modularui.factory.PosGuiData,
                             manager: brachy.modularui.value.sync.PanelSyncManager, settings: brachy.modularui.screen.UISettings) {
        val column = brachy.modularui.widgets.layout.Flow.column().widthRel(1f).coverChildrenHeight()
        getWidgetsForDisplay(manager).forEach { column.child(it) }
        main.child(com.gtladd.gtladditions.api.gui.NativeHatchUI.scroll(column))
    }

    // ========================================
    // Metadata
    // ========================================

    override fun isRemote(): Boolean = super<MultiblockControllerMachine>.isRemote

    companion object {

        private val MODULE_OFFSETS = arrayOf(
            BlockPos(33, -7, 0),
            BlockPos(-33, -7, 0),
            BlockPos(0, -7, 33),
            BlockPos(0, -7, -33)
        )

        val BINDING_TYPE: ResourceLocation = ResourceLocation.fromNamespaceAndPath("gtladditions", "recursive_reverse_array")
        private const val CATALYTIC_EU_MULTIPLIER = 0.15
        private const val BUFF_ROLL_COUNTER_SALT = -7046029254386353131L
        private const val BUFF_ROLL_SEED_SALT = 7640891576956012809L

        private data class BuffContext(
            val forge: ForgeOfTheAntichrist?,
            val boost: SupratemporalBoostingEngine?,
            val boostReady: Boolean,
            val machinesReady: Boolean,
            val buffProbability: Double
        )

        data class BoostPreview(
            val commonActive: Boolean,
            val probability: Double,
            val outputMultiplier: Double,
            val activeModuleKeys: List<String>,
            val diagnostics: List<String>
        )
    }
}
