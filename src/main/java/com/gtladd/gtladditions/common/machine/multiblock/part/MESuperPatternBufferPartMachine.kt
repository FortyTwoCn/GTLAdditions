package com.gtladd.gtladditions.common.machine.multiblock.part

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.common.data.item.GTDataComponents
import com.gtladd.gtladditions.api.machine.gui.FOAPatternConfigurator
import com.gtladd.gtladditions.integration.ae2.SuperPatternDataStick
import brachy.modularui.api.widget.IWidget
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager
import org.gtlcore.gtlcore.common.machine.multiblock.part.PaginationUIManager
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine

/** Native configurable pattern pages, FOA publication and independent data-stick binding. */
class MESuperPatternBufferPartMachine @JvmOverloads constructor(
    info: BlockEntityCreationInfo,
    patternsPerRow: Int = 9,
    rowsPerPage: Int = 6,
    maxPages: Int = 3
) : MEPatternBufferPartMachine(info, capacity(patternsPerRow, rowsPerPage, maxPages), IO.BOTH) {
    private val paginationUIManager = PaginationUIManager(
        patternsPerRow, rowsPerPage, maxPages, maxOf(patternsPerRow * 18 + 16, 106), rowsPerPage * 18 + 28,
        { index -> onPatternChange(index) }, { index -> cacheRecipe[index] }, patternInventory
    )

    @field:SaveField @field:SyncToClient
    private var foaModeEnabled = false
    @field:SaveField @field:SyncToClient
    private var foaPatternOutputMultiplier = DEFAULT_MULTIPLIER

    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult {
        if (stack.has(GTDataComponents.RESEARCH_ITEM)) return InteractionResult.PASS
        if (!isRemote) {
            SuperPatternDataStick.write(stack, blockPos, level!!.dimension().location())
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"))
        }
        return InteractionResult.sidedSuccess(isRemote)
    }

    fun isFOAModeEnabled() = foaModeEnabled
    fun getFOAPatternOutputMultiplier() = foaPatternOutputMultiplier
    fun setFOAModeEnabled(enabled: Boolean) {
        if (foaModeEnabled == enabled) return
        foaModeEnabled = enabled
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("foaModeEnabled")
        if (!isRemote) refreshAllByProduct()
    }
    fun setFOAPatternOutputMultiplier(multiplier: Int) {
        val bounded = multiplier.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
        if (foaPatternOutputMultiplier == bounded) return
        foaPatternOutputMultiplier = bounded
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("foaPatternOutputMultiplier")
        if (!isRemote && foaModeEnabled) refreshAllByProduct()
    }

    override fun getPanelBuilder(data: PosGuiData, sync: PanelSyncManager, settings: UISettings): MachineUIPanelBuilder {
        val builder = super.getPanelBuilder(data, sync, settings)
        val inherited = builder.leftConfigurators()
        val config = FOAPatternConfigurator(this)
        val popup = sync.syncedPanel("superPatternFOAConfig", true) { manager, _ -> config.createPanel(manager) }
        return builder.leftConfigurators { flow ->
            inherited.accept(flow)
            flow.child(config.createButton { popup.openPanel() })
        }
    }

    override fun buildPatternPagination(sync: PanelSyncManager, catalysts: MEPatternCatalystUIManager): IWidget =
        paginationUIManager.createPaginationUI(sync) { index -> catalysts.toggleFor(index) }

    companion object {
        private const val DEFAULT_MULTIPLIER = 15
        const val MIN_MULTIPLIER = 1
        const val MAX_MULTIPLIER = 30
        private fun capacity(columns: Int, rows: Int, pages: Int): Int {
            require(columns in 1..18 && rows in 1..10 && pages in 1..10) { "Invalid super pattern page dimensions" }
            return Math.multiplyExact(Math.multiplyExact(columns, rows), pages)
        }
    }
}
