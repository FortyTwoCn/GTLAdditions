package com.gtladd.gtladditions.api.pattern

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry
import com.gtladd.gtladditions.api.registry.NativeQueuedMachines
import java.util.function.Function
import net.minecraft.world.level.block.Block
import java.util.IdentityHashMap

class PatternPredicateSelector private constructor(
    private val blocks: Set<() -> Block>,
    private val abilities: Set<PartAbility>,
    private val skipController: Boolean,
    private val requirePureAir: Boolean
) {
    fun containsBlock(block: Block): PatternPredicateSelector = containsBlock { block }

    fun containsBlock(block: () -> Block): PatternPredicateSelector =
        PatternPredicateSelector(blocks + block, abilities, skipController, requirePureAir)

    fun containsAbility(ability: PartAbility): PatternPredicateSelector =
        PatternPredicateSelector(blocks, abilities + ability, skipController, requirePureAir)

    fun containsAbilities(vararg abilities: PartAbility): PatternPredicateSelector =
        PatternPredicateSelector(blocks, this.abilities + abilities, skipController, requirePureAir)

    fun notController(): PatternPredicateSelector = PatternPredicateSelector(blocks, abilities, true, requirePureAir)

    fun pureAir(): PatternPredicateSelector = PatternPredicateSelector(blocks, abilities, skipController, true)

    fun matches(predicate: MultiPredicate): Boolean {
        if (skipController && predicate.isController) return false
        if (requirePureAir && !predicate.isAir) return false
        val candidateBlocks = predicate.candidateBlocks()
        return blocks.map { it() }.all(candidateBlocks::contains) &&
            abilities.all { ability -> ability.getAllBlocks().any(candidateBlocks::contains) }
    }

    companion object {
        fun any(): PatternPredicateSelector = PatternPredicateSelector(emptySet(), emptySet(), false, false)

        fun pureAir(): PatternPredicateSelector = any().pureAir()

        fun containsBlock(block: Block): PatternPredicateSelector = any().containsBlock(block)

        fun containsBlock(block: () -> Block): PatternPredicateSelector = any().containsBlock(block)

        fun containsAbility(ability: PartAbility): PatternPredicateSelector = any().containsAbility(ability)
    }
}

fun patternPredicateSelector(block: Block, vararg abilities: PartAbility): PatternPredicateSelector =
    PatternPredicateSelector.containsBlock(block)
        .containsAbilities(*abilities)
        .notController()

fun patternPredicateSelector(block: () -> Block, vararg abilities: PartAbility): PatternPredicateSelector =
    PatternPredicateSelector.containsBlock(block)
        .containsAbilities(*abilities)
        .notController()

fun MachineEntry<MultiblockMachineDefinition>.patchPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    append: MultiPredicate,
    requireAtLeastOne: Boolean = true
) {
    patchPatternPredicates(name, selector, { append }, requireAtLeastOne)
}

fun MachineEntry<MultiblockMachineDefinition>.patchPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    append: () -> MultiPredicate,
    requireAtLeastOne: Boolean = true
) {
    transformPatternPredicates(name, selector, requireAtLeastOne) { predicate -> predicate.or(append()) }
}

fun MachineEntry<MultiblockMachineDefinition>.replacePatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    replacement: MultiPredicate,
    requireAtLeastOne: Boolean = true
) {
    replacePatternPredicates(name, selector, { replacement }, requireAtLeastOne)
}

fun MachineEntry<MultiblockMachineDefinition>.replacePatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    replacement: () -> MultiPredicate,
    requireAtLeastOne: Boolean = true
) {
    transformPatternPredicates(name, selector, requireAtLeastOne) { replacement() }
}

fun MachineEntry<MultiblockMachineDefinition>.transformPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    requireAtLeastOne: Boolean = true,
    transform: (MultiPredicate) -> MultiPredicate
) {
    val factories = NativeQueuedMachines.builder(this).properties.patterns()
    val machineId = id.toString()
    factories.replaceAll { _, original ->
        Function<MultiblockMachineDefinition, IBlockPattern> { definition ->
            val pattern = original.apply(definition)
            require(pattern is BlockPattern) {
                "Pattern patch '$name' cannot edit ${pattern.javaClass.name} for $machineId"
            }
            pattern.patchPredicates(machineId, name, selector, requireAtLeastOne, transform)
        }
    }
}

private fun BlockPattern.patchPredicates(
    machineId: String,
    patchName: String,
    selector: PatternPredicateSelector,
    requireAtLeastOne: Boolean,
    transform: (MultiPredicate) -> MultiPredicate
): BlockPattern {
    val matches = IdentityHashMap<MultiPredicate, MultiPredicate>()
    val usedSymbols = slices.asSequence()
        .flatMap { it.pattern.asSequence() }
        .flatMap { it.asSequence() }
        .toSet()
    for (symbol in usedSymbols) {
        val predicate = predicates[symbol] ?: error("Unbound pattern symbol '$symbol' for $machineId")
        if (!matches.containsKey(predicate) && selector.matches(predicate)) {
            matches[predicate] = transform(predicate)
        }
    }
    if (matches.isEmpty() && requireAtLeastOne) {
        error("Pattern patch '$patchName' matched no predicates for $machineId")
    }
    // Preserve shared predicate identity, so native global/layer limits are shared across symbols.
    for (symbol in usedSymbols) {
        val predicate = predicates[symbol]!!
        matches[predicate]?.let { predicates.put(symbol, it) }
    }

    return this
}

private fun MultiPredicate.candidateBlocks(): Set<Block> =
    candidates.asSequence().flatten().map { it.blockState.block }.toSet()
