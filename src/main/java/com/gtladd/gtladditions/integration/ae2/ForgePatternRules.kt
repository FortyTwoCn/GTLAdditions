package com.gtladd.gtladditions.integration.ae2

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient

/** One set of original FOA extra-container rules shared by recipe calculation and AE publication. */
object ForgePatternRules {
    private val ruleItems = linkedMapOf(
        ResourceLocation.fromNamespaceAndPath("kubejs", "stellar_forge/contained_exotic_matter") to ResourceLocation.fromNamespaceAndPath("kubejs", "time_dilation_containment_unit"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "stellar_forge/extremely_durable_plasma_cell") to ResourceLocation.fromNamespaceAndPath("kubejs", "extremely_durable_plasma_cell"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "stellar_forge/contained_kerr_newmann_singularity") to ResourceLocation.fromNamespaceAndPath("kubejs", "time_dilation_containment_unit")
    )
    fun specialInputContents(): Map<ResourceLocation, Content> = ruleItems.mapValues { (_, itemId) ->
        check(BuiltInRegistries.ITEM.containsKey(itemId)) { "Missing FOA cycle container $itemId" }
        Content(LongIngredient.create(Ingredient.of(BuiltInRegistries.ITEM.get(itemId)), 1), ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue())
    }
    fun matchingSpecialInput(inputs: List<GenericStack>, level: Level?): GenericStack? {
        if (level == null) return null
        val keys = inputs.mapTo(mutableSetOf()) { it.what() }
        for ((recipeId, itemId) in ruleItems) {
            val recipe = level.recipeManager.byKey(recipeId).orElse(null)?.value() as? GTRecipe ?: continue
            if (!matchesRecipeInputKeys(keys, recipe)) continue
            check(BuiltInRegistries.ITEM.containsKey(itemId)) { "Missing FOA cycle container $itemId" }
            return GenericStack(AEItemKey.of(ItemStack(BuiltInRegistries.ITEM.get(itemId))), 1)
        }
        return null
    }
    private fun matchesRecipeInputKeys(keys: Set<AEKey>, recipe: GTRecipe): Boolean {
        val predicates = mutableListOf<(AEKey) -> Boolean>()
        recipe.getInputContents(ItemRecipeCapability.CAP).forEach { content ->
            val ingredient = LongIngredient.matchingIngredient(ItemRecipeCapability.CAP.of(content.content()))
            predicates.add { key -> key is AEItemKey && ingredient.test(key.toStack()) }
        }
        recipe.getInputContents(FluidRecipeCapability.CAP).forEach { content ->
            val ingredient = LongFluidIngredient.matchingIngredient(FluidRecipeCapability.CAP.of(content.content()))
            predicates.add { key -> key is AEFluidKey && ingredient.test(key.toStack(1)) }
        }
        // Component predicates cannot be reconstructed from their display stacks. Match the actual
        // native predicates in both directions, including all alternatives and excluding extra keys.
        return predicates.isNotEmpty() && predicates.all { predicate -> keys.any(predicate) } && keys.all { key -> predicates.any { it(key) } }
    }
}
