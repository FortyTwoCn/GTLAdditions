package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.LightHunterSpaceStationPosHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class LightHunterSpaceStation(holder: BlockEntityCreationInfo, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder),
    IModularMachineHost<LightHunterSpaceStation>,
    IAstralArrayInteractionMachine {
    private val modules: MutableSet<IModularMachineModule<LightHunterSpaceStation, *>> =
        ReferenceOpenHashSet<IModularMachineModule<LightHunterSpaceStation, *>>()

    override fun modifyDrops(drops: MutableList<net.minecraft.world.item.ItemStack>) {
        super.modifyDrops(drops)
        addAstralArrayDrops(drops)
    }

    @field:SaveField
    @field:SyncToClient
    override var astralArrayCount: Int = 0
    private var mam = 0

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    fun unlockParadoxical(): Boolean = astralArrayCount == MAX_ASTRAL_ARRAY_COUNT

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
        }
        return actualIncrease
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("LightHunterSpaceStationStatus", syncManager, {
            mutableListOf<Component>().also(::collectLightHunterSpaceStationStatusText)
        }, 130)
    )

    private fun collectLightHunterSpaceStationStatusText(textList: MutableList<Component>) {
        
        if (!isFormed) return
        textList.add(
            if (unlockParadoxical()) {
                createLanguageRainbowComponentOnServer(
                    "tooltip.gtladditions.industrial_array_max".toComponent
                )
            } else {
                "tooltip.gtladditions.astral_array_count".toComponent("$astralArrayCount / $MAX_ASTRAL_ARRAY_COUNT".literal.withStyle(ChatFormatting.GOLD))
            }
        )
        textList.add("tooltip.gtlcore.installed_module_count".toComponent(getMAM()))
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<LightHunterSpaceStation, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos> = LightHunterSpaceStationPosHelper.calculateModulePositions(blockPos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(-41, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-49, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-57, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-65, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-73, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIV.get()
        ),
        ModuleRenderInfo(
            BlockPos(-81, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-89, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-97, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-105, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-113, 0, -9),
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIV.get()
        ),
        ModuleRenderInfo(
            BlockPos(-113, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-105, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIV.get()
        ),
        ModuleRenderInfo(
            BlockPos(-97, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-89, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIV.get()
        ),
        ModuleRenderInfo(
            BlockPos(-81, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-73, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-65, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIV.get()
        ),
        ModuleRenderInfo(
            BlockPos(-57, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIII.get()
        ),
        ModuleRenderInfo(
            BlockPos(-49, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKI.get()
        ),
        ModuleRenderInfo(
            BlockPos(-41, 0, 9),
            Direction.EAST,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            NEXUS_SATELLITE_FACTORY_MKIII.get()
        )
    )

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        safeClearModules()
    }

    override fun onMachineDestroyed() {
        safeClearModules()
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        safeClearModules()
        scanAndConnectModules()
    }

    companion object {
        private const val MAX_ASTRAL_ARRAY_COUNT = 64

    }
}