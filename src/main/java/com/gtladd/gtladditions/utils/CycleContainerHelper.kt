package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.content.Content
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.api.recipe.NativeContentState

/** Native Content records no longer carry named slots; identify the same returnable containers by their IDs. */
object CycleContainerHelper {
    const val RECIPE_CYCLE_CONTAINER = "recipe_cycle_container"
    @JvmField val ITEM_IDS: Set<ResourceLocation> = setOf(
        ResourceLocation.fromNamespaceAndPath("kubejs", "extremely_durable_plasma_cell"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "time_dilation_containment_unit"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "plasma_containment_cell")
    )
    fun isItem(item: Item): Boolean = BuiltInRegistries.ITEM.getKey(item) in ITEM_IDS
    fun isContent(content: Content): Boolean = NativeContentState.of(content).`gtlcore$getSlotName`() == RECIPE_CYCLE_CONTAINER || LongIngredient.matchingIngredient(
        ItemRecipeCapability.CAP.of(content.content())).items.firstOrNull()?.item?.let(::isItem) ?: false
}
