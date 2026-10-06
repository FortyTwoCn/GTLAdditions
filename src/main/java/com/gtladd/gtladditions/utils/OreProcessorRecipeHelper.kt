package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier
import org.gtlcore.gtlcore.api.recipe.NativeContentState
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient
import kotlin.math.max

object OreProcessorRecipeHelper {
    fun copyForOreProcessor(
        recipe: GTRecipe,
        itemChanceBoost: Int = 1,
        outputMultiplier: Double = 1.0,
        inputFluidMultiplier: Double = 1.0,
        durationMultiplier: Double = 1.0
    ): GTRecipe {
        if (itemChanceBoost == 1 && outputMultiplier == 1.0 && inputFluidMultiplier == 1.0 && durationMultiplier == 1.0) {
            return recipe
        }

        require(itemChanceBoost >= 0 && listOf(outputMultiplier, inputFluidMultiplier, durationMultiplier)
            .all { it.isFinite() && it > 0 }) { "Ore recipe multipliers must be finite and positive" }
        val copy = recipe.copy()
        copy.inputs.clear()
        copy.inputs.putAll(copyContents(recipe.inputs, inputFluidMultiplier = inputFluidMultiplier))
        copy.outputs.clear()
        copy.outputs.putAll(copyContents(recipe.outputs, itemChanceBoost = itemChanceBoost, outputMultiplier = outputMultiplier))
        copy.tickOutputs.clear()
        copy.tickOutputs.putAll(copyContents(recipe.tickOutputs, itemChanceBoost = itemChanceBoost, outputMultiplier = outputMultiplier))
        copy.duration = max((recipe.duration * durationMultiplier).toInt(), 1)
        RecipeExtensionCopier.copy(recipe, copy)
        return copy
    }

    private fun copyContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        itemChanceBoost: Int = 1,
        outputMultiplier: Double = 1.0,
        inputFluidMultiplier: Double = 1.0
    ): Map<RecipeCapability<*>, List<Content>> {
        if (contents.isEmpty()) return contents

        val copied = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
        for ((capability, contentList) in contents) {
            if (contentList.isEmpty()) continue
            val copyList = ObjectArrayList<Content>(contentList.size)
            for (content in contentList) {
                copyContent(content, capability, itemChanceBoost, outputMultiplier, inputFluidMultiplier)?.let(copyList::add)
            }
            if (copyList.isNotEmpty()) copied[capability] = copyList
        }
        return copied
    }

    private fun copyContent(
        content: Content,
        capability: RecipeCapability<*>,
        itemChanceBoost: Int,
        outputMultiplier: Double,
        inputFluidMultiplier: Double
    ): Content? {
        val modifier = when {
            outputMultiplier != 1.0 -> ContentModifier.multiplier(outputMultiplier)
            capability == FluidRecipeCapability.CAP && inputFluidMultiplier != 1.0 -> ContentModifier.multiplier(inputFluidMultiplier)
            else -> null
        }
        val copiedContent = if (modifier == null || content.chance == 0) {
            capability.copyContent(content.content)
        } else {
            capability.copyContent(content.content, modifier)
        }

        return if (capability == FluidRecipeCapability.CAP &&
            LongFluidIngredient.amount(FluidRecipeCapability.CAP.of(copiedContent)) <= 0
        ) {
            null
        } else {
            NativeContentState.copy(content, Content(copiedContent, content.chance, content.maxChance)).also {
                if (capability == ItemRecipeCapability.CAP) NativeContentState.of(it).`gtlcore$setTierChanceBoost`(
                    Math.multiplyExact(NativeContentState.of(content).`gtlcore$getTierChanceBoost`(), itemChanceBoost)
                )
            }
        }
    }
}
