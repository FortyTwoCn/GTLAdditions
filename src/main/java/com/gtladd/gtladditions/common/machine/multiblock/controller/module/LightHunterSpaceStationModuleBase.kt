package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.LightHunterSpaceStationPosHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class LightHunterSpaceStationModuleBase(holder: BlockEntityCreationInfo) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, LightHunterSpaceStationModuleBaseLogic()),
    IModularMachineModule<LightHunterSpaceStation, LightHunterSpaceStationModuleBase> {
    @field:SaveField
    private var hostPosition: BlockPos? = null
    private var host: LightHunterSpaceStation? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }
    override fun getHost(): LightHunterSpaceStation? = host
    override fun setHost(host: LightHunterSpaceStation?) {
        this.host = host
    }
    override fun getHostType(): Class<LightHunterSpaceStation> = LightHunterSpaceStation::class.java
    override fun getHostScanPositions(): Array<BlockPos> = LightHunterSpaceStationPosHelper.calculatePossibleHostPositions(blockPos)


    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("LightHunterSpaceStationModuleBaseStatus", syncManager, {
            mutableListOf<Component>().also(::collectLightHunterSpaceStationModuleBaseStatusText)
        }, 130)
    )

    private fun collectLightHunterSpaceStationModuleBaseStatusText(textList: MutableList<Component>) {
        
        if (!this.isFormed) return
        textList.add(
            (if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed").toComponent
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component>) {
        if (isConnectedToHost && host!!.unlockParadoxical()) {
            textList.add(
                "gtceu.multiblock.parallel".toComponent(
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                "gtladditions.multiblock.threads".toComponent(
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
        } else {
            super.addParallelDisplay(textList)
        }
    }

    // ===============================================
    // LightHunterSpaceStation connection
    // ===============================================

    override fun onConnected(host: LightHunterSpaceStation) {
        recipeLogic.updateTickSubscription()
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineDestroyed() {
        removeFromHost(this.host)
    }

    override fun needConfirmMEStock(): Boolean = true

    companion object {

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            (machine as LightHunterSpaceStationModuleBase).host?.let { it -> return@let it.isActive } ?: false
        }

        open class LightHunterSpaceStationModuleBaseLogic(
            
        ) : GTLAddMultipleRecipesLogic(BEFORE_WORKING) {

            override fun getMachine(): LightHunterSpaceStationModuleBase = super.getMachine() as LightHunterSpaceStationModuleBase

            override fun calculateParallels(): ParallelData? {
                if (!getMachine().host!!.unlockParadoxical()) {
                    return super.calculateParallels()
                }

                val recipes = lookupRecipeSet()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}