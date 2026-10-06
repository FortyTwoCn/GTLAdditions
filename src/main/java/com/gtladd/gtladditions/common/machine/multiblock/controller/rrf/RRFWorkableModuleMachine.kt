package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult

abstract class RRFWorkableModuleMachine(holder: BlockEntityCreationInfo, logic: com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic = com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic()) :
    WorkableElectricMultiblockMachine(holder, logic),
    IModularMachineModule<RecursiveReverseArray, RRFWorkableModuleMachine> {

    @SaveField
    private var hostPosition: BlockPos? = null

    private var host: RecursiveReverseArray? = null
    private var arrayTickSubscription: TickableSubscription? = null

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    // ========================================
    // Recursive reverse buff
    // ========================================

    open fun isReadyForRecursiveReverseBuff(): Boolean = isFormed && recipeLogic.isWorking

    // ========================================
    // Module connection
    // ========================================

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }

    override fun getHost(): RecursiveReverseArray? = host

    override fun setHost(host: RecursiveReverseArray?) {
        this.host = host
    }

    override fun getHostType(): Class<RecursiveReverseArray> = RecursiveReverseArray::class.java

    override fun getHostScanPositions(): Array<BlockPos> = arrayOf(
        blockPos.offset(33, 7, 0),
        blockPos.offset(-33, 7, 0),
        blockPos.offset(0, 7, 33),
        blockPos.offset(0, 7, -33)
    )

    override fun onConnected(host: RecursiveReverseArray) {
        recipeLogic.updateTickSubscription()
    }

    override fun onDisconnected() {
        failDisconnectedRecipe()
    }

    // ========================================
    // Recipe gating
    // ========================================

    override fun beforeWorking(recipe: GTRecipe?): Boolean {
        if (isConnectedToHost) return true
        failDisconnectedRecipe()
        return false
    }

    private fun failDisconnectedRecipe() {
        getRecipeLogic().interruptRecipe()
        RecipeResult.of(this, FAIL_NOT_CONNECTED_TO_RECURSIVE_REVERSE_ARRAY)
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        if (!findAndConnectToHost()) removeFromHost(host)
        updateArrayTickSubscription()
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        removeFromHost(host)
        updateArrayTickSubscription()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(host)
    }

    override fun onMachineDestroyed() {
        removeFromHost(host)
    }

    open fun moduleTick() = Unit

    private fun updateArrayTickSubscription() {
        if (isFormed) {
            arrayTickSubscription = subscribeServerTick(arrayTickSubscription, ::moduleArrayServerTick)
        } else {
            arrayTickSubscription?.unsubscribe()
            arrayTickSubscription = null
        }
    }

    private fun moduleArrayServerTick() {
        moduleTick()
        updateArrayTickSubscription()
    }

    // ========================================
    // GUI
    // ========================================

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = definition.additionalDisplay.apply(this, syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("RRFWorkableModuleMachineStatus", syncManager, {
            mutableListOf<Component>().also(::collectRRFWorkableModuleMachineStatusText)
        }, 130)
    )

    private fun collectRRFWorkableModuleMachineStatusText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed())
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addMachineModeLine(recipeType)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
            .addRecipeStatus(recipeLogic as IRecipeStatus)
            .addComponent(
                "gtladditions.machine.recursive_reverse_array.module.bound".toComponent(
                    Component.literal(if (isConnectedToHost) "✓" else "x")
                        .withStyle(if (isConnectedToHost) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
    }

    // ========================================
    // Metadata
    // ========================================

    companion object {

        val FAIL_NOT_CONNECTED_TO_RECURSIVE_REVERSE_ARRAY: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.not_connected_to_recursive_reverse_array".toComponent
        )
    }
}