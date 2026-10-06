package com.gtladd.gtladditions.common.machine.multiblock.controller.bs

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeRecipesLogic
import com.gtladd.gtladditions.api.machine.logic.IWirelessRecipeLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.function.Predicate

class BiosphereIIIModule(holder: BlockEntityCreationInfo) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, BiosphereModuleRecipeLogic()),
    IModularMachineModule<BiosphereIIIController, BiosphereIIIModule> {
    @field:SaveField
    private var hostPosition: BlockPos? = null
    private var host: BiosphereIIIController? = null

    // ========================================
    // Module connection
    // ========================================

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }

    override fun getHost(): BiosphereIIIController? = host

    override fun setHost(host: BiosphereIIIController?) {
        this.host = host
        if (level?.isClientSide == false) recipeLogic.updateTickSubscription()
    }

    override fun getHostType(): Class<BiosphereIIIController> = BiosphereIIIController::class.java

    override fun getHostScanPositions(): Array<BlockPos> = BiosphereIIIPosHelper.calculatePossibleHostPositions(blockPos)

    // ========================================
    // Lifecycle
    // ========================================

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        if (!findAndConnectToHost()) removeFromHost(host)
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        removeFromHost(host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(host)
    }

    override fun onMachineDestroyed() = removeFromHost(host)

    // ========================================
    // Host capabilities
    // ========================================

    override fun getTier(): Int = host?.tier ?: GTValues.ULV

    override fun getOverclockVoltage(): Long = host?.getModuleRecipePower() ?: 0

    override fun getWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler? =
        host?.getWirelessNetworkEnergyHandler()

    override fun setWirelessNetworkEnergyHandler(trait: IWirelessNetworkEnergyHandler) = Unit

    override fun getAdditionalThread(): Int {
        val hostMachine = host ?: return 0
        val tierDelta = (hostMachine.tier - GTValues.UEV).coerceAtLeast(0).toLong()
        val engineExtraThreads = hostMachine.getAdditionalThread().toLong()
        return Ints.saturatedCast(32L * tierDelta * engineExtraThreads)
    }

    override fun getMaxParallel(): Int {
        val exponent = (host?.coilType?.coilTemperature ?: 0) / 1100
        return if (exponent >= 31) Int.MAX_VALUE else 1 shl exponent.coerceAtLeast(0)
    }

    private fun isHostReady(): Boolean =
        host?.let { it.isFormed && it.tier >= GTValues.UEV && it.isWorkingEnabled } == true

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("BiosphereIIIModuleStatus", syncManager, {
            mutableListOf<Component>().also(::collectBiosphereIIIModuleStatusText)
        }, 130)
    )

    private fun collectBiosphereIIIModuleStatusText(textList: MutableList<Component>) {
        
        if (isFormed) {
            textList.add(
                if (host == null) {
                    "gui.gtladditions.biosphere_iii_module_disconnect".toComponent
                } else {
                    "gui.gtladditions.biosphere_iii_module_connect".toComponent
                }
            )
        }
    }

    // ========================================
    // Recipe logic
    // ========================================


    private class BiosphereModuleRecipeLogic :
        GTLAddMultipleTypeRecipesLogic(Predicate { (it as BiosphereIIIModule).isHostReady() }),
        IWirelessRecipeLogic {
        private val module: BiosphereIIIModule get() = machine as BiosphereIIIModule
        override fun handleRecipeWorking() {
            val recipe = lastRecipe ?: return
            if (!module.isHostReady()) {
                setWaiting("gtladditions.recipe.fail.biosphere_host_not_working".toComponent)
                if (status == Status.WAITING) regressRecipe()
                return
            }

            val energyAvailable = if (recipe is IWirelessGTRecipe) {
                handleWirelessTickInput(recipe)
            } else {
                module.host?.requestEnergy(recipe.inputEUt.totalEU) == true
            }

            if (energyAvailable) {
                status = Status.WORKING
                if (!machine.onWorking()) {
                    interruptRecipe()
                    return
                }
                ++progress
                ++totalContinuousRunningTime
            } else {
                setWaiting(RecipeResult.FAIL_NO_ENOUGH_EU_IN.reason())
            }

            if (status == Status.WAITING) regressRecipe()
        }

        override fun handleWirelessTickInput(recipe: IWirelessGTRecipe): Boolean {
            val energy = recipe.getWirelessEuTickInputs() ?: return true
            return module.host?.requestWirelessEnergy(energy) == true
        }

        override fun getWirelessMachine(): IWirelessElectricMultiblockMachine = module
    }

    // ========================================
    // Metadata
    // ========================================

    companion object {

    }
}