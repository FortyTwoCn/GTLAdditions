package com.gtladd.gtladditions.api.machine.trait

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableComputationContainer
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.canBridgeCloud

/** A fresh native trait whose inputs come from the bound team's cloud providers. */
class CloudOpticalComputationContainer(handlerIO: IO, transmitter: Boolean) :
    NotifiableComputationContainer(handlerIO, transmitter) {

    var lastResearchCwu: Int = 0
        private set

    private fun getTeamId() = (machine as? CloudOpticalComputationHatchMachine)?.teamId

    override fun handleRecipeInner(
        io: IO, recipe: GTRecipe?, left: MutableList<Int>, simulate: Boolean
    ): MutableList<Int> {
        if (io != IO.IN || handlerIO != IO.IN || isTransmitter) return left
        val required = left.sumOf { it.toLong().coerceAtLeast(0) }
        if (required == 0L) return mutableListOf()
        val totalCwu = recipe?.data?.getBoolean("duration_is_total_cwu") == true
        if (totalCwu && !simulate) lastResearchCwu = 0
        val available = CloudNetworkManager.requestCWU(getTeamId(), Int.MAX_VALUE.toLong(), true)
        if (available < required) return left
        val drawn = CloudNetworkManager.requestCWU(getTeamId(), if (totalCwu) available else required, simulate)
        if (!simulate && totalCwu) {
            lastResearchCwu = drawn.toInt()
            when (val owner = machine) {
                is IRecipeLogicMachine -> advanceResearch(owner, drawn)
                is MultiblockPartMachine -> owner.controllers.filterIsInstance<IRecipeLogicMachine>()
                    .forEach { advanceResearch(it, drawn) }
            }
        }
        var remainder = (required - drawn).coerceAtLeast(0)
        return mutableListOf<Int>().also { result ->
            while (remainder > 0) {
                val part = remainder.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                result.add(part)
                remainder -= part
            }
        }
    }

    private fun advanceResearch(owner: IRecipeLogicMachine, drawn: Long) {
        // The native recipe logic adds its ordinary one-tick progress after this handler returns.
        val logic = owner.recipeLogic
        logic.progress = (logic.progress.toLong() + drawn - 1L)
            .coerceIn(0L, Int.MAX_VALUE.toLong() - 1L).toInt()
    }

    override fun requestCWUt(
        cwut: Int, simulate: Boolean, seen: MutableCollection<IOpticalComputationProvider>
    ): Int {
        seen.add(this)
        if (cwut <= 0 || handlerIO != IO.IN || isTransmitter) return 0
        return CloudNetworkManager.requestCWU(getTeamId(), cwut.toLong(), simulate)
            .coerceIn(0L, cwut.toLong()).toInt()
    }

    override fun getComputationProvider(): IOpticalComputationProvider = this

    override fun getMaxCWUt(seen: MutableCollection<IOpticalComputationProvider>): Int {
        seen.add(this)
        if (handlerIO != IO.IN || isTransmitter) return 0
        return CloudNetworkManager.getMaxCWU(getTeamId()).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    override fun canBridge(seen: MutableCollection<IOpticalComputationProvider>): Boolean {
        seen.add(this)
        when (val owner = machine) {
            is IOpticalComputationProvider -> return owner.canBridgeCloud()
            is MultiblockPartMachine -> return owner.controllers.filterIsInstance<IOpticalComputationProvider>()
                .any { it.canBridgeCloud() }
        }
        return false
    }
}
