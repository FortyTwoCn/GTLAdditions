package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine

class CreateAggregation(holder: BlockEntityCreationInfo) :
    WorkableElectricMultiblockMachine(holder),
    IThreadModifierMachine,
    ParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        threadPartMachine = null
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    override fun getMaxParallel(): Int = Ints.saturatedCast(1L + getAdditionalThread())
}