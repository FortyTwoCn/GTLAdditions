package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.machine.trait.MEPatternRecipeHandlePart
import org.gtlcore.gtlcore.api.machine.trait.RecipeHandlePart
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.NativeContentState
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidParallelLogic
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.NumberUtils
import java.math.BigInteger

/** Read-only quantity bounds for the matrix's reduced input chances on native, sized ingredients. */
object ChanceParallelLogic {
    fun getMaxParallel(
        holder: ParallelMachine, recipe: GTRecipe, parallelAmount: Long,
        chanceCaches: Map<RecipeCapability<*>, Object2IntMap<*>>, baseTier: Int, machineTier: Int
    ): Long {
        var parallel = parallelAmount.coerceAtLeast(0)
        if (recipe.inputs.containsKey(ItemRecipeCapability.CAP)) {
            parallel = getInputItemParallel(holder, recipe, parallel, chanceCaches[ItemRecipeCapability.CAP], baseTier, machineTier)
        }
        if (parallel > 0 && recipe.inputs.containsKey(FluidRecipeCapability.CAP)) {
            parallel = getInputFluidParallel(holder, recipe, parallel, chanceCaches[FluidRecipeCapability.CAP], baseTier, machineTier)
        }
        return parallel
    }

    fun getInputItemParallel(
        holder: ParallelMachine, recipe: GTRecipe, parallelAmount: Long,
        cache: Object2IntMap<*>?, baseTier: Int, machineTier: Int
    ): Long {
        if (parallelAmount <= 0) return 0
        if (holder !is IRecipeCapabilityMachine) return minOf(1, parallelAmount)
        if (holder.emptyRecipeHandlePart()) return 0
        val inventory = Object2LongOpenCustomHashMap<ItemStack>(ItemStackHashStrategy.comparingAllButCount())
        val confirmStock = holder.needConfirmMEStock()
        val contents: Object2LongMap<ItemStack>? = when (val handle = holder.getActiveRecipeHandle(recipe)) {
            is MEPatternRecipeHandlePart -> handle.getMEContent(ItemRecipeCapability.CAP, recipe)
            is RecipeHandlePart -> handle.getSelfContent(ItemRecipeCapability.CAP, confirmStock)
            else -> holder.sharedRecipeHandlePart?.getSelfContent(ItemRecipeCapability.CAP, confirmStock)
        }
        contents?.let {
            for (entry in Object2LongMaps.fastIterable(it)) {
                inventory.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
            }
        }
        return limitItemInputs(recipe.getInputContents(ItemRecipeCapability.CAP), inventory, parallelAmount,
            cache, baseTier, machineTier, IGTRecipe.of(recipe).realParallels)
    }

    fun getInputFluidParallel(
        holder: ParallelMachine, recipe: GTRecipe, parallelAmount: Long,
        cache: Object2IntMap<*>?, baseTier: Int, machineTier: Int
    ): Long {
        if (parallelAmount <= 0) return 0
        if (holder !is IRecipeCapabilityMachine) return minOf(1, parallelAmount)
        if (holder.emptyRecipeHandlePart()) return 0
        val inventory = LongFluidParallelLogic.newFluidAmountMap()
        val confirmStock = holder.needConfirmMEStock()
        val handle = holder.getActiveRecipeHandle(recipe)
        val contents: Object2LongMap<FluidStack>? = when (handle) {
            is MEPatternRecipeHandlePart -> handle.getMEContent(FluidRecipeCapability.CAP, recipe)
            is RecipeHandlePart -> if (holder.isDistinct) handle.getSelfContent(FluidRecipeCapability.CAP, confirmStock)
                else handle.getContentWithShared(FluidRecipeCapability.CAP, confirmStock)
            else -> null
        }
        if (contents != null) {
            for (entry in Object2LongMaps.fastIterable(contents)) {
                inventory.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
            }
        } else if (handle !is RecipeHandlePart && handle !is MEPatternRecipeHandlePart) {
            holder.sharedRecipeHandlePart?.let { shared ->
                for (handler in shared.getCapability(FluidRecipeCapability.CAP)) {
                    if (handler is CatalystFluidStackHandler) continue
                    for (stack in handler.contents) {
                        // Native handlers expose sized ingredients; retain exact Core long quantities.
                        val sized = FluidRecipeCapability.CAP.of(stack)
                        val fluid = LongFluidIngredient.matchingIngredient(sized).getStacks().firstOrNull { !it.isEmpty } ?: continue
                        inventory.mergeLong(fluid, LongFluidIngredient.amount(sized), NumberUtils::saturatedAdd)
                    }
                }
            }
        }
        return limitFluidInputs(recipe.getInputContents(FluidRecipeCapability.CAP), inventory, parallelAmount,
            cache, baseTier, machineTier, IGTRecipe.of(recipe).realParallels)
    }

    @JvmStatic
    fun limitItemInputs(
        contents: List<Content>, inventory: Object2LongMap<ItemStack>, limit: Long,
        cache: Object2IntMap<*>?, recipeTier: Int, machineTier: Int, sourceRuns: Long
    ): Long = limitInputs(contents, inventory, limit, cache, recipeTier, machineTier, sourceRuns,
        { LongIngredient.amount(ItemRecipeCapability.CAP.of(it.content)) },
        { content, stack -> LongIngredient.matchingIngredient(ItemRecipeCapability.CAP.of(content.content)).test(stack) })

    @JvmStatic
    fun limitFluidInputs(
        contents: List<Content>, inventory: Object2LongMap<FluidStack>, limit: Long,
        cache: Object2IntMap<*>?, recipeTier: Int, machineTier: Int, sourceRuns: Long
    ): Long = limitInputs(contents, inventory, limit, cache, recipeTier, machineTier, sourceRuns,
        { LongFluidIngredient.amount(FluidRecipeCapability.CAP.of(it.content)) },
        { content, stack -> LongFluidIngredient.matchingIngredient(FluidRecipeCapability.CAP.of(content.content)).test(stack) })

    private fun <S> limitInputs(
        contents: List<Content>, inventory: Object2LongMap<S>, limit: Long,
        cache: Object2IntMap<*>?, recipeTier: Int, machineTier: Int, sourceRuns: Long,
        amount: (Content) -> Long, matches: (Content, S) -> Boolean
    ): Long {
        if (limit <= 0) return 0
        require(sourceRuns > 0) { "Recipe run count must be positive" }
        if (contents.isEmpty()) return limit
        // Reserve reusable inputs first; overlapping predicates draw from the same working ledger.
        val requirements = contents.sortedBy { if (it.chance == 0) 0 else 1 }
        val stacks = Object2LongMaps.fastIterable(inventory).map { it.key to it.longValue.coerceAtLeast(0) }
        val cached = requirements.map {
            require(it.maxChance > 0) { "Chance denominator must be positive" }
            // A missing cache uses the largest initial roll. Matching never advances RNG or caches.
            if (cache?.containsKey(it.content) == true) cache.getInt(it.content) else it.maxChance - 1
        }
        fun canSupply(parallels: Long): Boolean {
            val remaining = stacks.map { it.second }.toLongArray()
            for ((index, content) in requirements.withIndex()) {
                val quantity = amount(content)
                if (quantity <= 0) continue
                val runs = when {
                    content.chance == 0 -> BigInteger.ONE
                    content.chance >= content.maxChance -> BigInteger.valueOf(parallels)
                    else -> {
                        val chance = NativeContentState.boostedChance(content, recipeTier, machineTier).coerceAtLeast(0)
                        val totalChance = BigInteger.valueOf(parallels).multiply(BigInteger.valueOf(sourceRuns))
                            .multiply(BigInteger.valueOf(chance.toLong()))
                        val portions = totalChance.divideAndRemainder(BigInteger.valueOf(content.maxChance.toLong()))
                        val bonus = portions[1].add(BigInteger.valueOf(cached[index].toLong()))
                            .divide(BigInteger.valueOf(content.maxChance.toLong())).max(BigInteger.ZERO)
                        // The unrolled, one-run ingredient must also satisfy Core's input plan.
                        portions[0].add(bonus).max(BigInteger.ONE)
                    }
                }
                var needed = runs.multiply(BigInteger.valueOf(quantity))
                if (needed > BigInteger.valueOf(Long.MAX_VALUE)) return false
                for (slot in stacks.indices) {
                    if (remaining[slot] == 0L || !matches(content, stacks[slot].first)) continue
                    val used = minOf(remaining[slot], needed.longValueExact())
                    remaining[slot] -= used
                    needed = needed.subtract(BigInteger.valueOf(used))
                    if (needed.signum() == 0) break
                }
                if (needed.signum() != 0) return false
            }
            return true
        }
        var low = 0L
        var high = limit
        while (low < high) {
            val middle = low + (high - low) / 2 + (high - low) % 2
            if (canSupply(middle)) low = middle else high = middle - 1
        }
        return low
    }
}
