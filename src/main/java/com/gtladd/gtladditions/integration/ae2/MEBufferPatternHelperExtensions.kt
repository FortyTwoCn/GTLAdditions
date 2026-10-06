@file:JvmName("MEBufferPatternHelperExtensions")
package com.gtladd.gtladditions.integration.ae2

import appeng.api.crafting.IPatternDetails
import appeng.api.crafting.PatternDetailsHelper
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.GenericStack
import appeng.crafting.pattern.AEProcessingPattern
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine
import com.gtladd.gtladditions.utils.CycleContainerHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.gtlcore.gtlcore.integration.ae2.handler.MEBufferPatternHelper
import org.gtlcore.gtlcore.utils.NumberUtils
import java.util.function.Consumer

fun MEBufferPatternHelper.processForgeOfTheAntichristPattern(
    stack: ItemStack, circuitCacheSetter: Consumer<Int>, level: Level?, keepByProduct: Boolean, multiplier: Int
): IPatternDetails? {
    val pattern = processPatternWithCircuit(stack, circuitCacheSetter, level, keepByProduct)
    val processing = pattern as? AEProcessingPattern ?: return pattern
    val inputs = processing.sparseInputs.filterNotNull()
    return rewriteForgeOfTheAntichristPattern(processing, level, multiplier, ForgePatternRules.matchingSpecialInput(inputs, level))
}

/** Pure native transformation after rule selection; keys retain all item/fluid data components. */
fun rewriteForgeOfTheAntichristPattern(
    pattern: IPatternDetails?, level: Level?, multiplier: Int, specialInput: GenericStack?
): IPatternDetails? {
    val processing = pattern as? AEProcessingPattern ?: return pattern
    val bounded = multiplier.coerceIn(MESuperPatternBufferPartMachine.MIN_MULTIPLIER, MESuperPatternBufferPartMachine.MAX_MULTIPLIER)
    if (bounded == 1) return processing
    val inputs = processing.sparseInputs.filterNotNull().toMutableList()
    if (specialInput != null) {
        val extraAmount = NumberUtils.saturatedMultiply(specialInput.amount(), (bounded - 1).toLong())
        val index = inputs.indexOfFirst { it.what() == specialInput.what() }
        if (index >= 0) {
            val previous = inputs[index]
            inputs[index] = GenericStack(previous.what(), NumberUtils.saturatedAdd(previous.amount(), extraAmount))
        } else {
            // Publishing scaled outputs without the required extra input would advertise a false craft.
            if (inputs.size >= AEProcessingPattern.MAX_INPUT_SLOTS) return processing
            inputs.add(GenericStack(specialInput.what(), extraAmount))
        }
    } else {
        for (index in inputs.indices) {
            val input = inputs[index]
            if (isCycleContainer(input)) inputs[index] = GenericStack(input.what(), NumberUtils.saturatedMultiply(input.amount(), bounded.toLong()))
        }
    }
    val outputs = processing.sparseOutputs.filterNotNull().map { output ->
        if (isCycleContainer(output)) output else GenericStack(output.what(), NumberUtils.saturatedMultiply(output.amount(), bounded.toLong()))
    }
    val encoded = PatternDetailsHelper.encodeProcessingPattern(inputs, outputs)
    return AEProcessingPattern(checkNotNull(AEItemKey.of(encoded)))
}
private fun isCycleContainer(stack: GenericStack): Boolean {
    val key = stack.what()
    return key is AEItemKey && CycleContainerHelper.isItem(key.item)
}
