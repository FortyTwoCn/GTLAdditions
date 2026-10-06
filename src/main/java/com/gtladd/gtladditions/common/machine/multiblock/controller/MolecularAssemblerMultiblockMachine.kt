package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.machine.trait.MolecularAssemblerRecipesLogic
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class MolecularAssemblerMultiblockMachine(info: BlockEntityCreationInfo) :
    org.gtlcore.gtlcore.api.machine.multiblock.MolecularAssemblerMultiblockMachine(info, InfinityRecipesLogic()),
    IModularMachineHost<MolecularAssemblerMultiblockMachine> {

    private var mam = 0
    private val modules: Set<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>> =
        ReferenceOpenHashSet<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>>()
    var isInfinityMode = false

    private class InfinityRecipesLogic : MolecularAssemblerRecipesLogic() {
        override fun getRecipe(): GTRecipe? {
            val host = machine as MolecularAssemblerMultiblockMachine
            return if (host.isInfinityMode) host.maHandler?.extractGTRecipe(Long.MIN_VALUE, host.tickDuration)
            else super.getRecipe()
        }
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>> = modules
    override fun getModuleScanPositions(): Array<out BlockPos> = arrayOf(
        blockPos.offset(0, 2, -7),
        blockPos.offset(0, 2, 7),
        blockPos.offset(-7, 2, 0),
        blockPos.offset(7, 2, 0)
    )
    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(0, 2, -7),
            Direction.NORTH,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY.get()
        )
    )

    override fun invalidateStructure(name: String) {
        super.invalidateStructure(name)
        safeClearModules()
    }

    override fun onMachineDestroyed() {
        super.onMachineDestroyed()
        safeClearModules()
    }

    override fun formStructure(name: String) {
        super.formStructure(name)
        if (name != DEFAULT_STRUCTURE || !isFormed) return
        safeClearModules()
        scanAndConnectModules()
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = definition.additionalDisplay.apply(this, syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("MolecularAssemblerMultiblockMachineStatus", syncManager, {
            mutableListOf<Component>().also(::collectMolecularAssemblerMultiblockMachineStatusText)
        }, 130)
    )

    private fun collectMolecularAssemblerMultiblockMachineStatusText(textList: MutableList<Component>) {
        if (isFormed()) {
            textList.add(
                recipeType.id.toLanguageKey().toComponent
                    .setStyle(
                        Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    "gtceu.gui.machinemode.title".toComponent
                                )
                            )
                    )
            )
            if (!isWorkingEnabled) {
                textList.add("gtceu.multiblock.work_paused".toComponent)
            } else if (isActive) {
                textList.add("gtceu.multiblock.running".toComponent)
                val currentProgress = (recipeLogic.progressPercent * 100).toInt()
                textList.add("gtceu.multiblock.progress".toComponent(currentProgress))
            } else {
                textList.add("gtceu.multiblock.idling".toComponent)
            }
            if (recipeLogic.isWaiting) {
                textList.add(
                    "gtceu.multiblock.waiting".toComponent
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                )
            }
            if (isInfinityMode) {
                textList.add(
                    "gtceu.multiblock.parallel".toComponent(
                        CommonUtils.createLanguageRainbowComponentOnServer(
                            "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                        )
                    ).withStyle(ChatFormatting.GRAY)
                )
            } else if (maxParallel > 1) {
                textList.add(
                    "gtceu.multiblock.parallel".toComponent(
                        FormattingUtil.formatNumbers(maxParallel).literal
                            .withStyle(ChatFormatting.DARK_PURPLE)
                    ).withStyle(ChatFormatting.GRAY)
                )
            }
            textList.add(
                "gtlcore.multiblock.tick_Duration".toComponent(
                    (FormattingUtil.formatNumbers(if (isInfinityMode) 1 else tickDuration)).literal
                        .withStyle(ChatFormatting.BLUE)
                )
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                "gtlcore.multiblock.contains_Patttern".toComponent(FormattingUtil.formatNumbers(patternSize).literal.withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add("tooltip.gtlcore.installed_module_count".toComponent(getMAM()))
        } else {
            val tooltip: Component = "gtceu.multiblock.invalid_structure.tooltip".toComponent
                .withStyle(ChatFormatting.GRAY)
            textList.add(
                "gtceu.multiblock.invalid_structure".toComponent
                    .withStyle(
                        Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
                    )
            )
        }
    }

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }
}
