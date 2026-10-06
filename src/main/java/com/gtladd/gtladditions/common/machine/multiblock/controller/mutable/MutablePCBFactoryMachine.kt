package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import it.unimi.dsi.fastutil.longs.LongLongPair
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.common.machine.multiblock.electric.PCBFactoryMachine

class MutablePCBFactoryMachine(holder: BlockEntityCreationInfo) :
    PCBFactoryMachine(holder, PcbLogic()),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    private val nanoSwarmMultiplier: Double
        get() {
            val itemStack = machineStorageItem
            return when (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.item).toString()) {
                "gtceu:vibranium_nanoswarm" -> (100 - itemStack.count) / 400.0
                "gtceu:gold_nanoswarm" -> (100 - itemStack.count * 0.5) / 100.0
                else -> 1.0
            }
        }

    private class PcbLogic : MutableRecipesLogic<MutablePCBFactoryMachine>() {

            override val energyReductionMultiplier: Double
                get() = super.energyReductionMultiplier * getMachine().nanoSwarmMultiplier

            override fun calculateParallel(
                machine: IRecipeLogicMachine,
                match: GTRecipe,
                remain: Long
            ): LongLongPair = if (match.inputEUt.totalEU <= GTValues.V[GTValues.LuV]) {
                LongLongPair.of(IParallelLogic.getMaxParallel(machine, match, Long.Companion.MAX_VALUE), 0)
            } else {
                super.calculateParallel(machine, match, remain)
            }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutablePCBFactoryMachine> = super.getRecipeLogic() as MutableRecipesLogic<MutablePCBFactoryMachine>

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun getMaxParallel(): Int = (this as IRecipeCapabilityMachine).parallelHatch.map { it.currentParallel }.orElse(1)

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine
}