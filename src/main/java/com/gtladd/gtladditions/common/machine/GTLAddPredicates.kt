package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate
import com.gregtechceu.gtceu.api.multiblock.error.CoilMatchingError
import com.gregtechceu.gtceu.api.multiblock.predicates.PredicateBuilder
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo
import com.gregtechceu.gtceu.common.block.CoilBlock
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import org.gtlcore.gtlcore.api.pattern.PatternMatchData

object GTLAddPredicates {

    fun slabBlock(slabType: SlabType, vararg block: Block): MultiPredicate =
        PredicateBuilder("GTLSlab:$slabType")
            .predicate { context ->
                val state = context.state()
                block.any { state.`is`(it) } && state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == slabType
            }
            .candidatesSupplier {
                block.filterIsInstance<SlabBlock>().map { BlockInfo.fromBlockState(it.defaultBlockState().setValue(SlabBlock.TYPE, slabType)) }
            }.toMultiPredicate()

    fun heatingCoils(temperature: Int, equals: Boolean = false): MultiPredicate {
        val matches: (ICoilType) -> Boolean = { if (equals) it.coilTemperature == temperature else it.coilTemperature >= temperature }
        return PredicateBuilder("GTLCoils:$temperature:$equals")
            .predicate { context ->
                val coil = context.state().block as? CoilBlock
                if (coil == null || !GTCEuAPI.HEATING_COILS.containsKey(coil.coilType) || !matches(coil.coilType)) {
                    false
                } else {
                    val data = PatternMatchData.of(context)
                    if (!data.matchCoil(coil.coilType)) {
                        val first = requireNotNull(data.coil())
                        context.appendError(CoilMatchingError(context.pos(), first, coil.coilType))
                        false
                    } else true
                }
            }
            .candidatesSupplier {
                val entries = GTCEuAPI.HEATING_COILS.entries.sortedBy { it.key.coilTemperature }
                val (accepted, remaining) = entries.partition { matches(it.key) }
                (accepted + remaining).map { BlockInfo.fromBlock(it.value.get()) }
            }.toMultiPredicate()
            .addTooltips(Component.translatable("gtceu.multiblock.pattern.error.coils"))
    }
}
