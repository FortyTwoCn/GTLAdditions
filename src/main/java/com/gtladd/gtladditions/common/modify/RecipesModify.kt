package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.GAS
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTMaterials.Helium
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.TagUtil
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.PHOTON_MATRIX_ETCH
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.SPACE_ORE_PROCESSOR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE
import com.gtladd.gtladditions.utils.TempChemicalHelper
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient
import org.gtlcore.gtlcore.api.recipe.NativeContentState
import com.gtladd.gtladditions.utils.CycleContainerHelper
import org.gtlcore.gtlcore.config.ConfigHolder
import java.math.BigInteger
import java.util.function.BiConsumer

/** Installs callbacks on each native type before recipe loading or data generation. */
object RecipesModify {
    fun init() {}

    fun modifyOnRegistration(type: GTRecipeType) {
        val callback: BiConsumer<GTRecipeBuilder, RecipeOutput>? = when (type.id.toString()) {
            "gtceu:door_of_create" -> { maxIO(type, 1, 1, 0, 0); null }
            "gtceu:create_aggregation" -> { maxIO(type, 2, 1, 0, 0); null }
            "gtceu:dimensionally_transcendent_mixer" -> { maxIO(type, 9, 1, 16, 1); null }
            "gtceu:vacuum_freezer" -> BiConsumer { builder, output ->
                antientropy(builder, output, true)
            }
            "gtceu:plasma_condenser" -> BiConsumer { builder, output ->
                antientropy(builder, output, false)
            }
            "gtceu:alloy_blast_smelter" -> BiConsumer { builder, output ->
                val first = builder.output[FluidRecipeCapability.CAP]?.firstOrNull() ?: return@BiConsumer
                val ingredient = FluidRecipeCapability.CAP.of(first.content())
                val material = TempChemicalHelper.getMaterialFromIngredient(ingredient) ?: return@BiConsumer
                val liquid = material.getFluid(LIQUID) ?: return@BiConsumer
                val derived = CHAOTIC_ALCHEMY.copyFrom(builder)
                    .duration(duration(builder.duration, 3, 4))
                    .EUt(totalEU(builder).let { if (it > 16) it / 2 else it })
                derived.output[FluidRecipeCapability.CAP]?.clear()
                derived.outputFluids(LongFluidIngredient.create(liquid, LongFluidIngredient.amount(ingredient)))
                derived.save(output)
            }
            "gtceu:laser_engraver" -> BiConsumer { builder, output ->
                if (builder.output.containsKey(FluidRecipeCapability.CAP)) return@BiConsumer
                // Core already emits the dimensional-focus recipe. Composing it here would duplicate its ID.
                PHOTON_MATRIX_ETCH.copyFrom(builder).duration(duration(builder.duration, 1, 5)).save(output)
            }
            "gtceu:integrated_ore_processor" -> BiConsumer { builder, output ->
                val derived = SPACE_ORE_PROCESSOR.copyFrom(builder).EUt(GTValues.VA[GTValues.HV].toLong())
                derived.input[FluidRecipeCapability.CAP]?.clear()
                derived.save(output)
            }
            "gtceu:aggregation_device" -> BiConsumer { builder, output ->
                val derived = LEYLINE_CRYSTALLIZE.copyFrom(builder).EUt(scaleEU(builder, 4, 1))
                if (builder.id != ResourceLocation.fromNamespaceAndPath("gtladditions", "reaction_chamber")) {
                    val items = derived.output[ItemRecipeCapability.CAP] ?: return@BiConsumer
                    val first = items.firstOrNull() ?: return@BiConsumer
                    items.clear()
                    items.add(first.copy(ItemRecipeCapability.CAP, ContentModifier.multiplier(1.5)))
                }
                derived.save(output)
            }
            "gtceu:dimensionally_transcendent_plasma_forge", "gtceu:stellar_forge" -> BiConsumer { builder, _ ->
                for (contents in listOf(builder.input[ItemRecipeCapability.CAP], builder.output[ItemRecipeCapability.CAP])) {
                    for (content in contents.orEmpty()) {
                        if (CycleContainerHelper.isContent(content)) NativeContentState.of(content).`gtlcore$setSlotName`(CycleContainerHelper.RECIPE_CYCLE_CONTAINER)
                    }
                }
            }
            "gtceu:fragment_world_collection" -> BiConsumer { builder, output ->
                if (ConfigHolder.INSTANCE.enableSkyBlokeMode) SkyTearsAndGregHeart.modifyRecipe(builder, output)
            }
            else -> null
        }
        if (callback != null) {
            val previous = type.recipeBuilder.onSave
            type.recipeBuilder.onSave(if (previous == null) callback else previous.andThen(callback))
        }
    }

    private fun maxIO(type: GTRecipeType, itemIn: Int, itemOut: Int, fluidIn: Int, fluidOut: Int) {
        type.maxInputs.put(ItemRecipeCapability.CAP, itemIn)
        type.maxOutputs.put(ItemRecipeCapability.CAP, itemOut)
        type.maxInputs.put(FluidRecipeCapability.CAP, fluidIn)
        type.maxOutputs.put(FluidRecipeCapability.CAP, fluidOut)
    }

    private fun antientropy(builder: GTRecipeBuilder, output: RecipeOutput, vacuum: Boolean) {
        val derived = ANTIENTROPY_CONDENSATION.copyFrom(builder)
        if (vacuum) derived.duration(duration(builder.duration, 2, 3))
        derived.input[FluidRecipeCapability.CAP]?.removeIf { isHelium(it, LIQUID) }
        if (vacuum || builder.id.path != "helium_condenser") {
            derived.output[FluidRecipeCapability.CAP]?.removeIf { isHelium(it, GAS) }
        }
        derived.save(output)
    }

    private fun isHelium(content: Content, storageKey: FluidStorageKey): Boolean {
        val fluid = Helium.getFluid(storageKey) ?: return false
        val ingredient = LongFluidIngredient.matchingIngredient(FluidRecipeCapability.CAP.of(content.content()))
        if (ingredient is TagFluidIngredient) {
            // Tags have no display stacks during data generation. Compare their native identity instead.
            return ingredient.tag() == TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluid).path)
        }
        return ingredient.stacks.any { it.fluid === fluid }
    }

    private fun duration(original: Int, numerator: Int, denominator: Int): Int =
        (original.toLong() * numerator / denominator).coerceIn(1, Int.MAX_VALUE.toLong()).toInt()

    private fun totalEU(builder: GTRecipeBuilder): Long = scaleEU(builder, 1, 1)

    private fun scaleEU(builder: GTRecipeBuilder, numerator: Int, denominator: Int): Long =
        BigInteger.valueOf(builder.EUt().voltage()).multiply(BigInteger.valueOf(builder.EUt().amperage()))
            .multiply(BigInteger.valueOf(numerator.toLong())).divide(BigInteger.valueOf(denominator.toLong()))
            .coerceAtMost(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact()
}
