package com.gtladd.gtladditions.common.machine.multiblock.part

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine
import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.IntSyncValue
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import brachy.modularui.widgets.textfield.TextFieldWidget
import net.minecraft.util.Mth

// GT8 discovers parallel hatches by their native class, rather than the removed IParallelHatch.
class SuperParallelHatchPartMachine(info: BlockEntityCreationInfo) :
    ParallelHatchPartMachine(info, GTValues.MAX) {
    @field:SaveField
    @field:SyncToClient
    private var configuredParallel: Int = MAX_PARALLEL

    override fun setCurrentParallel(parallelAmount: Int) {
        configuredParallel = Mth.clamp(parallelAmount, MIN_PARALLEL, MAX_PARALLEL)
        for (controller in controllers) {
            if (controller is IRecipeLogicMachine) controller.recipeLogic.markLastRecipeDirty()
        }
    }

    override fun getCurrentParallel(): Int = configuredParallel

    override fun buildMainUI(
        mainWidget: ParentWidget<*>, guiData: PosGuiData,
        syncManager: PanelSyncManager, settings: UISettings
    ) {
        val parallels = IntSyncValue(this::getCurrentParallel, this::setCurrentParallel).allowC2S()
        mainWidget.child(
            TextFieldWidget().width(100).height(20)
                .setNumbers(MIN_PARALLEL, MAX_PARALLEL).value(parallels)
        )
    }

    override fun canShared(controller: MultiblockControllerMachine, substructureName: String?): Boolean = true

    companion object {
        private const val MAX_PARALLEL = Int.MAX_VALUE
        private const val MIN_PARALLEL = 1
    }
}
