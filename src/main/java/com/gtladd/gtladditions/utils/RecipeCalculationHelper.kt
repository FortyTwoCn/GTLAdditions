package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder
import com.gtladd.gtladditions.api.recipe.NativeWirelessRecipeState
import com.gtladd.gtladditions.common.data.ParallelData
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongLongPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.IAdvancedContentModifier
import org.gtlcore.gtlcore.api.recipe.NativeContentState
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeChancePreview
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.Registries
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.get
import kotlin.math.min
import kotlin.math.roundToInt

object RecipeCalculationHelper {

    data class NormalBatch(
        val itemOutputs: ObjectArrayList<Content>,
        val fluidOutputs: ObjectArrayList<Content>,
        val totalEu: Double,
        val consumedRecipes: Int
    )

    data class WirelessBatch(
        val itemOutputs: ObjectArrayList<Content>,
        val fluidOutputs: ObjectArrayList<Content>,
        val totalEu: BigInteger,
        val consumedRecipes: Int
    )

    // ===================================================
    // Process Recipe
    // ===================================================

    inline fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long,
        crossinline copyRecipe: (GTRecipe) -> GTRecipe
    ): GTRecipe {
        require(parallel > 0) { "Recipe parallel count must be positive" }
        val processed = if (parallel > 1) {
            copyRecipe(recipe).also { RecipeExtensionCopier.copy(recipe, it) }
        } else {
            recipe
        }
        IGTRecipe.of(processed).realParallels = Math.multiplyExact(IGTRecipe.of(recipe).realParallels, parallel)
        return processed
    }

    fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long
    ): GTRecipe = multipleRecipe(recipe, parallel) { recipe -> recipe.copy(IAdvancedContentModifier.preciseMultiplier(parallel), false) }

    inline fun processParallelDataNormal(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxEUt: Long,
        euMultiplier: Double,
        getTotalRecipeEu: (GTRecipe) -> Double,
        crossinline shouldBreak: (Double) -> Boolean = { totalEu -> totalEu / maxEUt > 20 * 500 }
    ): NormalBatch {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var totalEu = 0.0
        var consumedRecipes = 0

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]
            if (parallelData.shouldProcess) {
                val processedRecipe = multipleRecipe(r, p)
                RecipeChancePreview.output(
                    processedRecipe, machine, processedRecipe.outputs, machine.recipeLogic.chanceCaches, false
                )

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine,
                        processedRecipe
                    ) &&
                    RecipeRunnerHelper.handleRecipeInput(
                        machine,
                        processedRecipe
                    )
                ) {
                    totalEu += getTotalRecipeEu(r) * p * euMultiplier
                    consumedRecipes++
                    collectOutputs(IParallelLogic.getRecipeOutputChance(machine, processedRecipe), itemOutputs, fluidOutputs)
                }

                if (shouldBreak(totalEu)) break
            } else {
                totalEu += getTotalRecipeEu(r) * p * euMultiplier
                consumedRecipes++
                collectOutputs(parallelData.processedRecipeList!![i], itemOutputs, fluidOutputs)
            }
        }

        return NormalBatch(itemOutputs, fluidOutputs, totalEu, consumedRecipes)
    }

    inline fun processParallelDataWireless(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxTotalEu: BigInteger,
        euMultiplier: Double,
        getRecipeEut: (GTRecipe) -> Long,
        isEnergyConsumer: Boolean = true
    ): WirelessBatch {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var accumulatedEu = BigInteger.ZERO
        var consumedRecipes = 0

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]

            var parallelEUt = BigInteger.valueOf(getRecipeEut(r))
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))

            val tempAccumulatedEu = accumulatedEu.add(
                BigDecimal.valueOf(r.duration * euMultiplier)
                    .multiply(BigDecimal(parallelEUt)).toBigInteger()
            )

            if (parallelData.shouldProcess) {
                if (isEnergyConsumer && tempAccumulatedEu > maxTotalEu) {
                    if (accumulatedEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                    break
                }

                val paralleledRecipe = multipleRecipe(r, p)
                RecipeChancePreview.output(
                    paralleledRecipe, machine, paralleledRecipe.outputs, machine.recipeLogic.chanceCaches, false
                )

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine,
                        paralleledRecipe
                    ) &&
                    RecipeRunnerHelper.handleRecipeInput(
                        machine,
                        paralleledRecipe
                    )
                ) {
                    accumulatedEu = tempAccumulatedEu
                    consumedRecipes++
                    collectOutputs(IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe), itemOutputs, fluidOutputs)
                }
            } else {
                accumulatedEu = tempAccumulatedEu
                consumedRecipes++
                collectOutputs(parallelData.processedRecipeList!![i], itemOutputs, fluidOutputs)
            }
        }

        val totalEu = if (isEnergyConsumer) accumulatedEu else accumulatedEu.negate()
        return WirelessBatch(itemOutputs, fluidOutputs, totalEu, consumedRecipes)
    }

    fun buildNormalRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        totalEu: Double,
        maxEUt: Long,
        minDuration: Int
    ): GTRecipe {
        require(totalEu.isFinite() && totalEu >= 0 && maxEUt > 0 && minDuration > 0) {
            "Batch energy, voltage and duration must be finite and positive"
        }
        val recipe = GTRecipeBuilder.ofRaw().build()
        recipe.outputs[ItemRecipeCapability.CAP] = itemOutputs
        recipe.outputs[FluidRecipeCapability.CAP] = fluidOutputs

        val d = totalEu / maxEUt
        val eut = if (d > minDuration) maxEUt else (totalEu / minDuration).toLong()
        EURecipeCapability.putEUContent(recipe.tickInputs, EnergyStack(eut))
        recipe.duration = maxOf(d, minDuration.toDouble()).roundToInt()
        IGTRecipe.of(recipe).setHasTick(true)
        IGTRecipe.of(recipe).isBatchProcessed = true
        return recipe
    }

    fun buildWirelessRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        duration: Int,
        totalEu: BigInteger,
        recipeType: GTRecipeType = GTRecipeTypes.DUMMY_RECIPES.value()
    ): WirelessGTRecipe {
        require(duration > 0) { "Wireless batch duration must be positive" }
        val eut = totalEu.divide(BigInteger.valueOf(duration.toLong())).negate()
        val recipe = WirelessGTRecipeBuilder
            .ofRaw(recipeType)
            .output(ItemRecipeCapability.CAP, itemOutputs)
            .output(FluidRecipeCapability.CAP, fluidOutputs)
            .duration(duration)
            .setWirelessEut(eut)
            .buildRawRecipe()
        IGTRecipe.of(recipe).isBatchProcessed = true
        recipe.data.putByteArray(NativeWirelessRecipeState.TOTAL_KEY, totalEu.negate().toByteArray())
        return recipe
    }

    fun collectOutputs(
        recipe: GTRecipe,
        itemOutputs: MutableList<Content>,
        fluidOutputs: MutableList<Content>
    ) {
        recipe.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
        recipe.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
    }

    fun hasOutputs(itemOutputs: List<Content>, fluidOutputs: List<Content>): Boolean = itemOutputs.isNotEmpty() || fluidOutputs.isNotEmpty()

    // ===================================================
    // ParallelData Calculation
    // ===================================================

    inline fun calculateParallelsWithFairAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        crossinline getParallelAndIfConsumption: (GTRecipe) -> LongBooleanPair
    ): ParallelData? {
        val length = recipes.size
        if (length == 0) return null

        var remaining = totalParallel
        val parallels = LongArray(length)
        var index = 0
        val recipeList = ObjectArrayList<GTRecipe>(length)
        val remainingWants = LongArrayList(length)
        val remainingIndices = IntArrayList(length)

        for (r in recipes) {
            val pair = getParallelAndIfConsumption(r)
            val p = pair.firstLong()
            if (p <= 0) continue

            recipeList.add(r)
            if (!pair.secondBoolean()) {
                parallels[index] = p
                index++
                continue
            }

            val allocated = minOf(p, totalParallel / length)
            parallels[index] = allocated
            val want = p - allocated
            if (want > 0) {
                remainingWants.add(want)
                remainingIndices.add(index)
            }
            remaining -= allocated
            index++
        }

        if (recipeList.isEmpty) return null

        return getFinalParallelData(remaining, parallels, remainingWants, remainingIndices, recipeList)
    }

    fun calculateParallelsWithGreedyAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        machine: IRecipeLogicMachine,
        getParallelAndConsumption: (GTRecipe, Long) -> LongLongPair
    ): ParallelData? {
        var remain = totalParallel

        val recipeList = ObjectArrayList<GTRecipe>()
        val processedRecipeList = ObjectArrayList<GTRecipe>()
        val parallelsList = LongArrayList()

        for (match in recipes) {
            if (remain <= 0) break
            val pair = getParallelAndConsumption(match, remain)
            val p = pair.firstLong()
            if (p <= 0) continue

            val paralleledRecipe = multipleRecipe(match, p)
            RecipeChancePreview.output(
                paralleledRecipe, machine, paralleledRecipe.outputs, machine.recipeLogic.chanceCaches, false
            )
            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= pair.secondLong()
                recipeList.add(match)
                processedRecipeList.add(IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe))
                parallelsList.add(p)
            }
        }

        return if (recipeList.isEmpty) {
            null
        } else {
            ParallelData(recipeList, parallelsList.toLongArray(), false, processedRecipeList)
        }
    }

    inline fun calculateParallelsWithProcessing(
        recipes: Collection<GTRecipe>,
        machine: IRecipeLogicMachine,
        crossinline getParallelLimitForRecipe: (GTRecipe) -> Long,
        crossinline getMaxParallelForRecipe: (GTRecipe, Long) -> Long,
        crossinline modifyRecipe: (GTRecipe) -> GTRecipe = { it },
        crossinline createParalleledRecipe: (GTRecipe, Long) -> GTRecipe = { r, p -> multipleRecipe(r, p) },
        useModifiedRecipe: Boolean = false,
        preProcessRecipes: Boolean = true
    ): ParallelData? {
        val length = recipes.size
        if (length == 0) return null

        val recipeList = ObjectArrayList<GTRecipe>(length)
        val processedRecipeList = if (preProcessRecipes) ObjectArrayList<GTRecipe>(length) else null
        val parallelsList = LongArrayList(length)

        for (recipe in recipes) {
            val modified = modifyRecipe(recipe)
            val limit = getParallelLimitForRecipe(modified)
            val parallel = getMaxParallelForRecipe(modified, limit)

            if (parallel > 0) {
                if (preProcessRecipes) {
                    val paralleledRecipe = createParalleledRecipe(modified, parallel)
                    RecipeChancePreview.output(
                        paralleledRecipe, machine, paralleledRecipe.outputs, machine.recipeLogic.chanceCaches, false
                    )
                    if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                        recipeList.add(if (useModifiedRecipe) modified else recipe)
                        processedRecipeList!!.add(IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe))
                        parallelsList.add(parallel)
                    }
                } else {
                    recipeList.add(if (useModifiedRecipe) modified else recipe)
                    parallelsList.add(parallel)
                }
            }
        }

        return if (recipeList.isEmpty) {
            null
        } else {
            ParallelData(recipeList, parallelsList.toLongArray(), !preProcessRecipes, processedRecipeList)
        }
    }

    fun getFinalParallelData(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData? {
        if (recipeList.isEmpty()) return null
        if (remaining <= 0 || remainingWants.isEmpty()) return ParallelData(recipeList, parallels)

        return if (remainingWants.size <= 64) {
            getParallelDataBitmap(remaining, parallels, remainingWants, remainingIndices, recipeList)
        } else {
            getParallelDataIndexArray(remaining, parallels, remainingWants, remainingIndices, recipeList)
        }
    }

    private fun getParallelDataBitmap(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        val count = remainingWants.size
        var activeBits = (1L shl count) - 1
        var activeCount = count

        var remaining = remaining
        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var newActiveBits = 0L
            var newActiveCount = 0

            var bits = activeBits
            while (bits != 0L) {
                val i = bits.countTrailingZeroBits()
                bits = bits and (bits - 1)

                val idx = remainingIndices.getInt(i)
                val want = remainingWants.getLong(i)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give
                remainingWants.set(i, want - give)

                if (want - give > 0) {
                    newActiveBits = newActiveBits or (1L shl i)
                    newActiveCount++
                }
            }

            activeBits = newActiveBits
            activeCount = newActiveCount
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    private fun getParallelDataIndexArray(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        var activeCount = remainingWants.size
        var remaining = remaining

        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var writePos = 0

            for (readPos in 0 until activeCount) {
                val idx = remainingIndices.getInt(readPos)
                val want = remainingWants.getLong(readPos)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give

                val newWant = want - give
                if (newWant > 0) {
                    remainingWants.set(writePos, newWant)
                    remainingIndices.set(writePos, idx)
                    writePos++
                }
            }

            activeCount = writePos
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    // ===================================================
    // Recipe Copy Boost Fix ( > 10000 max chance)
    // ===================================================

    fun copyFixRecipe(origin: GTRecipe, modifier: ContentModifier, fixMultiplier: Int): GTRecipe {
        require(fixMultiplier > 0) { "Chance boost divisor must be positive" }
        val copy = origin.copy()
        copy.inputs.clear()
        copy.inputs.putAll(copyFixContents(origin.inputs, modifier, fixMultiplier))
        copy.outputs.clear()
        copy.outputs.putAll(copyFixContents(origin.outputs, modifier, fixMultiplier))
        copy.tickInputs.clear()
        copy.tickInputs.putAll(copyFixContents(origin.tickInputs, modifier, fixMultiplier))
        copy.tickOutputs.clear()
        copy.tickOutputs.putAll(copyFixContents(origin.tickOutputs, modifier, fixMultiplier))
        return RecipeExtensionCopier.copy(origin, copy)
    }

    fun copyFixContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Map<RecipeCapability<*>, List<Content>> =
        Reference2ReferenceArrayMap<RecipeCapability<*>, List<Content>>().apply {
            contents.forEach { (cap, contentList) ->
                if (contentList.isNotEmpty()) {
                    put(
                        cap,
                        ObjectArrayList(
                            contentList.map { content ->
                                copyFixBoost(content, cap, modifier, fixMultiplier)
                            }
                        )
                    )
                }
            }
        }

    fun copyFixBoost(
        content: Content,
        capability: RecipeCapability<*>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Content {
        require(fixMultiplier > 0) { "Chance boost divisor must be positive" }
        // Native chance logic applies realParallels when rolling. Scaling its one-run quantity
        // as well would square the requested parallels and overconsume probabilistic inputs.
        val newContent = if (content.chance >= content.maxChance) {
            capability.copyContent(content.content, modifier)
        } else {
            capability.copyContent(content.content)
        }

        return NativeContentState.copy(content, Content(newContent, content.chance, content.maxChance)).also {
            NativeContentState.of(it).`gtlcore$setTierChanceBoost`(
                NativeContentState.of(content).`gtlcore$getTierChanceBoost`() / fixMultiplier
            )
        }
    }

    // ===================================================
    // Recycle Recipe
    // ===================================================

    const val RECIPE_CYCLE_CONTAINER = "recipe_cycle_container"

    val RECIPE_CYCLE_CONTAINER_ITEM_IDS: Set<ResourceLocation> = setOf(
        ResourceLocation.fromNamespaceAndPath("kubejs", "extremely_durable_plasma_cell"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "time_dilation_containment_unit"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "plasma_containment_cell")
    )

    val RECIPE_CYCLE_CONTAINER_ITEMS: Set<Item> by lazy {
        RECIPE_CYCLE_CONTAINER_ITEM_IDS.mapTo(mutableSetOf()) { id -> Registries.getItem(id.toString()) }
    }

    val FORGE_OF_THE_ANTICHRIST_SPECIAL_INPUT_RULES: Map<ResourceLocation, Content>
        get() = com.gtladd.gtladditions.integration.ae2.ForgePatternRules.specialInputContents()

    fun isRecipeCycleContainerItem(item: Item): Boolean = CycleContainerHelper.isItem(item)

    fun isRecipeCycleContainerContent(content: Content): Boolean = CycleContainerHelper.isContent(content)

    fun appendSpecificInput(copyList: MutableList<Content>, id: ResourceLocation, modifier: ContentModifier) {
        FORGE_OF_THE_ANTICHRIST_SPECIAL_INPUT_RULES[id]?.let {
            if (modifier.multiplier >= 2) {
                copyList.add(
                    it.copy(
                        ItemRecipeCapability.CAP,
                        ContentModifier.multiplier(modifier.multiplier - 1)
                    )
                )
            }
        }
    }
}
