package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.toolHeadDrill
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STAR_CORE_STRIPPER
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.config.ConfigHolder
import kotlin.math.max
import kotlin.math.min

object SkyTearsAndGregHeart {
    private val worldFragments = setOf("overworld", "nether", "end", "reactor", "moon", "mars", "venus",
        "mercury", "ceres", "io", "ganymede", "pluto", "enceladus", "titan", "glacio", "barnarda")
        .mapTo(mutableSetOf()) { ResourceLocation.fromNamespaceAndPath("gtlcore", "world_fragments_$it") }
    private val targetDrillId = ResourceLocation.fromNamespaceAndPath("kubejs", "machine_casing_grinding_head")
    private val miracleId = ResourceLocation.fromNamespaceAndPath("gtlcore", "miracle_crystal")

    fun init() {}

    private fun itemModifier(): ContentModifier = ContentModifier.multiplier(
        min(4.0 / max(ConfigHolder.INSTANCE.durationMultiplier, 1.0), 4096.0))

    private fun stacks(content: Content): Array<ItemStack> = LongIngredient.matchingIngredient(
        ItemRecipeCapability.CAP.of(content.content())).items

    /** Called by the constructor-time type hook; contents are immutable native records. */
    fun modifyRecipe(recipeBuilder: GTRecipeBuilder, provider: RecipeOutput) {
        val exceptDrills = arrayOf(GTMaterials.Steel, GTMaterials.Titanium, GTMaterials.NaquadahAlloy,
            GTMaterials.Neutronium).map { ChemicalHelper.get(toolHeadDrill, it) }
        for (content in recipeBuilder.input[ItemRecipeCapability.CAP].orEmpty()) {
            val stack = stacks(content).firstOrNull() ?: continue
            if (exceptDrills.any { ItemStack.isSameItemSameComponents(it, stack) }) return
            if (BuiltInRegistries.ITEM.getKey(stack.item) == targetDrillId) {
                genTargetDrillRecipe(recipeBuilder, provider)
                return
            }
        }
        val derived = STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(GTValues.VA[GTValues.UHV].toLong())
        val modifier = itemModifier()
        for ((cap, contents) in derived.output) {
            val copies = contents.map { content ->
                if (cap == ItemRecipeCapability.CAP) {
                    val item = stacks(content).firstOrNull()?.item
                    val id = item?.let(BuiltInRegistries.ITEM::getKey)
                    when {
                        id == null || id == miracleId -> content
                        content.chance() >= content.maxChance() && id !in worldFragments -> content.copy(cap, modifier)
                        else -> Content(cap.copyContent(content.content()),
                            min(content.maxChance().toLong(), content.chance().toLong() * 100).toInt(), content.maxChance())
                    }
                } else if (content.chance() >= content.maxChance()) content
                else Content(cap.copyContent(content.content()), content.maxChance(), content.maxChance())
            }
            contents.clear()
            contents.addAll(copies)
        }
        derived.save(provider)
    }

    fun genTargetDrillRecipe(recipeBuilder: GTRecipeBuilder, provider: RecipeOutput) {
        val derived = STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(GTValues.VA[GTValues.UHV].toLong())
        val targetDrill = requiredItem(targetDrillId)
        derived.input[ItemRecipeCapability.CAP]?.removeIf { content -> stacks(content).firstOrNull()?.item === targetDrill }
        derived.chancedInput(ItemStack(targetDrill), 1)
        derived.output[ItemRecipeCapability.CAP]?.let { contents ->
            val modifier = itemModifier()
            val copies = contents.map { content ->
                val copy = content.copy(ItemRecipeCapability.CAP, modifier)
                Content(copy.content(), min(copy.maxChance().toLong(), copy.chance().toLong() * 200).toInt(), copy.maxChance())
            }
            contents.clear()
            contents.addAll(copies)
        }
        derived.save(provider)
    }

    private fun requiredItem(id: ResourceLocation): Item = BuiltInRegistries.ITEM.getOptional(id)
        .orElseThrow { IllegalStateException("Skyblock recipe requires registered item $id") }

    fun init(provider: RecipeOutput) {
        if (!ConfigHolder.INSTANCE.enableSkyBlokeMode) return
        val fragment = requiredItem(ResourceLocation.fromNamespaceAndPath("gtlcore", "world_fragments_overworld"))
        val treasure = requiredItem(ResourceLocation.fromNamespaceAndPath("gtlcore", "treasures_crystal"))
        STAR_CORE_STRIPPER.recipeBuilder(ResourceLocation.fromNamespaceAndPath("gtladditions", "for_stargate"))
            .notConsumable(fragment)
            .chancedOutput(ItemStack(fragment), 5000)
            .chancedOutput(ItemStack(treasure), 500)
            .outputItems(ItemStack(Items.GRASS_BLOCK, 8), ItemStack(Items.MOSS_BLOCK, 8), ItemStack(Items.SNOW_BLOCK, 8),
                ItemStack(Items.COCOA_BEANS, 16), ItemStack(Items.SWEET_BERRIES, 16), ItemStack(Items.APPLE, 16),
                ItemStack(Items.TURTLE_SCUTE, 4), ItemStack(Items.OCHRE_FROGLIGHT, 2), ItemStack(Items.VERDANT_FROGLIGHT, 2),
                ItemStack(Items.PEARLESCENT_FROGLIGHT, 2))
            .outputFluids(GTMaterials.Lava.getFluid(1000))
            .circuitMeta(30).EUt(GTValues.VA[GTValues.UHV].toLong()).duration(200).save(provider)
    }
}
