package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.machine.trait.AdvancedInfiniteDrillLogic
import kotlin.math.max
import kotlin.math.min

class AdvancedInfiniteDrillMachine(holder: BlockEntityCreationInfo) :
    org.gtlcore.gtlcore.common.machine.multiblock.electric.AdvancedInfiniteDrillMachine(holder, AdvancedInfiniteDrillMachineLogic(5)),
    IThreadModifierMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun getRecipeLogic(): AdvancedInfiniteDrillMachineLogic = super.getRecipeLogic() as AdvancedInfiniteDrillMachineLogic

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

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

    companion object {
        class AdvancedInfiniteDrillMachineLogic(range: Int) : AdvancedInfiniteDrillLogic(range) {
            override fun getMachine(): AdvancedInfiniteDrillMachine = super.getMachine() as AdvancedInfiniteDrillMachine

            override fun nextRecipe(): GTRecipe? {
                val machine = getMachine()
                val level = machine.level as? ServerLevel ?: return null
                getGridFluid(BedrockFluidVeinSavedData.getOrCreate(level))
                val fluids = veinFluids
                if (fluids.isEmpty() || machine.isEmpty || !machine.canRunnable()) return null
                try {
                    val total = fluids.values.fold(0L, Math::addExact)
                    val baseEUt = Math.addExact(GTValues.V[GTValues.UHV], total)
                    val threads = max(1, machine.threadPartMachine?.getThreadCount() ?: 0)
                    val rate = min(Math.multiplyExact(machine.rate.toLong(), threads.toLong()), machine.overclockVoltage / baseEUt)
                    if (rate <= 0) {
                        RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                        return null
                    }
                    val recipe = org.gtlcore.gtlcore.common.machine.trait.AdvancedFluidDrillProduction.recipe(fluids, rate)
                        ?: return null
                    return recipe.takeIf {
                        RecipeRunnerHelper.matchRecipe(machine, it) &&
                            com.gregtechceu.gtceu.api.recipe.RecipeHelper.matchTickRecipe(machine, it).isSuccess
                    }
                } catch (_: ArithmeticException) {
                    return null
                }
            }
        }
    }
}
