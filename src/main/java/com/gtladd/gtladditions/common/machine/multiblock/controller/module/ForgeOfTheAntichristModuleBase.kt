package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gtladd.gtladditions.utils.antichrist.AntichristPosHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper

abstract class ForgeOfTheAntichristModuleBase(holder: BlockEntityCreationInfo, logic: ForgeOfTheAntichristModuleBaseLogic) :
    GTLAddWirelessWorkableElectricMultipleRecipesMachine(
        holder,
        logic
    ),
    IModularMachineModule<ForgeOfTheAntichrist, ForgeOfTheAntichristModuleBase> {
    override fun getRecipeLogic(): ForgeOfTheAntichristModuleBaseLogic =
        super<GTLAddWirelessWorkableElectricMultipleRecipesMachine>.getRecipeLogic() as ForgeOfTheAntichristModuleBaseLogic

    @field:SaveField
    private var hostPosition: BlockPos? = null
    private var host: ForgeOfTheAntichrist? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }
    override fun getHost(): ForgeOfTheAntichrist? = host
    override fun setHost(host: ForgeOfTheAntichrist?) {
        this.host = host
    }
    override fun getHostType(): Class<ForgeOfTheAntichrist> = ForgeOfTheAntichrist::class.java
    override fun getHostScanPositions(): Array<BlockPos> = AntichristPosHelper.calculatePossibleHostPositions(blockPos, frontFacing)

    // ========================================
    // ForgeOfTheAntichrist connection
    // ========================================

    override fun onConnected(host: ForgeOfTheAntichrist) {
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



    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("ForgeOfTheAntichristModuleBaseStatus", syncManager, {
            mutableListOf<Component>().also(::collectForgeOfTheAntichristModuleBaseStatusText)
        }, 130)
    )

    private fun collectForgeOfTheAntichristModuleBaseStatusText(textList: MutableList<Component>) {
        
        if (!this.isFormed) return

        if (isConnectedToHost && getRecipeLogic().enableModify(this.recipeType)) {
            textList.add(
                if (host!!.runningSecs >= ForgeOfTheAntichrist.MAX_EFFICIENCY_SEC) {
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.achieve_max_efficiency".toComponent
                    )
                } else {
                    "gtladditions.multiblock.forge_of_the_antichrist.output_multiplier".toComponent(createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(host!!.recipeOutputMultiplyPreview)))
                }
            )
        }

        textList.add(
            (if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed").toComponent
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component>) {
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
    }

    override fun needConfirmMEStock(): Boolean = true

    companion object {

        val FAIL_HOST_NOT_WORKING: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.host_not_working".toComponent
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            ((machine as ForgeOfTheAntichristModuleBase).host?.let { return@let it.isActiveOrStasisAnchored() } ?: false)
                .also { if (!it) RecipeResult.of(machine, FAIL_HOST_NOT_WORKING) }
        }

        fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier): GTRecipe {
            val copy = recipe.copy()
            copy.outputs.clear()
            copy.outputs.putAll(modifyOutputContents(recipe.outputs, modifier))
            RecipeExtensionCopier.copy(recipe, copy)
            return copy
        }

        private fun modifyOutputContents(
            before: Map<RecipeCapability<*>, List<Content>>,
            modifier: ContentModifier
        ): Map<RecipeCapability<*>, List<Content>> {
            val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
            for (entry in before) {
                val cap = entry.key
                val contentList = entry.value
                val copyList = ObjectArrayList<Content>(contentList.size)
                for (content in contentList) {
                    copyList.add(content.copy(cap, modifier))
                }
                after[cap] = copyList
            }
            return after
        }

        open class ForgeOfTheAntichristModuleBaseLogic(
            beforeWorking: Predicate<IRecipeLogicMachine>
        ) : GTLAddMultipleWirelessRecipesLogic(beforeWorking) {
            override fun getMachine(): ForgeOfTheAntichristModuleBase = super.getMachine() as ForgeOfTheAntichristModuleBase

            override fun getReductionEUt(): Double =
                getMachine().host?.let { ForgeOfTheAntichrist.getEuReduction(it) * super.getReductionEUt() }
                    ?: super.getReductionEUt()

            override fun getEuMultiplier(): Double =
                getMachine().host?.let { ForgeOfTheAntichrist.getEuReduction(it) * super.getEuMultiplier() }
                    ?: super.getEuMultiplier()

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                val modifier = ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)

                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel,
                    modifyRecipe = { recipe ->
                        if (enableModify(recipe.recipeType)) copyAndModifyRecipe(recipe, modifier) else recipe
                    }
                )
            }

            override fun matchesSourceRecipe(recipe: GTRecipe): Boolean = RecipeRunnerHelper.matchRecipe(machine, recipe)

            open fun enableModify(recipeType: GTRecipeType): Boolean = false
        }
    }
}