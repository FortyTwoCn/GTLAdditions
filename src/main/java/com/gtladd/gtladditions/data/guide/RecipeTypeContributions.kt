package com.gtladd.gtladditions.data.guide

import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.api.gui.GTLytSlotGrid
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import guideme.compiler.tags.RecipeTypeMappingSupplier
import guideme.document.block.recipes.LytStandardRecipeBox
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.machines.GCyMMachines
import org.gtlcore.gtlcore.utils.Registries.getItemStack

class RecipeTypeContributions : RecipeTypeMappingSupplier {
    override fun collect(mappings: RecipeTypeMappingSupplier.RecipeTypeMappings) {
        for ((recipeType, icon) in TYPE_MAP) {
            mappings.add(recipeType) { recipe -> getGTRecipe(recipe, recipeType, icon) }
        }
    }

    private fun getGTRecipe(holder: net.minecraft.world.item.crafting.RecipeHolder<GTRecipe>, recipeType: GTRecipeType, icon: ItemStack): LytStandardRecipeBox<GTRecipe> {
        val recipe = holder.value()
        val builder = LytStandardRecipeBox.builder()
            .icon(icon)
            .title(recipeType.id.toLanguageKey().translatable)
        val slotGrid = GTLytSlotGrid.builder(recipe)
        builder.input(slotGrid.recipeInput)
        builder.output(slotGrid.recipeOutput)
        return builder.build(holder)
    }

    companion object {
        private val TYPE_MAP = Object2ObjectOpenHashMap<GTRecipeType, ItemStack>().also {
            it[GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD.value()] = MultiBlockMachine.SUBATOMIC_TRANSMUTATIOON_CORE.asStack()
            it[GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR.value()] = MultiBlockMachine.TITAN_CRIP_EARTHBORE.asStack()
            it[GTLAddRecipesTypes.MATTER_EXOTIC.value()] = MultiBlockMachine.HELIOFUSION_EXOTICIZER.asStack()
            it[GTLAddRecipesTypes.NIGHTMARE_CRAFTING.value()] = MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY.asStack()
            it[GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE.value()] = MultiBlockMachine.HELIOPHASE_LEYLINE_CRYSTALLIZER.asStack()
            it[GTLAddRecipesTypes.CHAOTIC_ALCHEMY.value()] = MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE.asStack()
            it[GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION.value()] = MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE.asStack()
            it[GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY.value()] = MultiBlockMachine.ARCANIC_ASTROGRAPH.asStack()
            it[GTLAddRecipesTypes.TIME_SPACE_DISTORTION.value()] = MultiBlockMachine.TIME_SPACE_DISTORTER.asStack()
            it[GCYMRecipeTypes.ALLOY_BLAST_RECIPES.value()] = GCyMMachines.BLAST_ALLOY_SMELTER.asStack()
            it[GTRecipeTypes.BLAST_RECIPES.value()] = GTMultiMachines.ELECTRIC_BLAST_FURNACE.asStack()
            it[GTRecipeTypes.ASSEMBLER_RECIPES.value()] = GTMachines.ASSEMBLER[10].asStack()
            it[GTRecipeTypes.ALLOY_SMELTER_RECIPES.value()] = GTMachines.ALLOY_SMELTER[2].asStack()
            it[GTRecipeTypes.LARGE_CHEMICAL_RECIPES.value()] = GTMultiMachines.LARGE_CHEMICAL_REACTOR.asStack()
            it[GTRecipeTypes.MIXER_RECIPES.value()] = GTMachines.MIXER[2].asStack()
            it[GTLRecipeTypes.MATTER_FABRICATOR_RECIPES.value()] = getItemStack("gtceu:matter_fabricator")
            it[GTLRecipeTypes.DISTORT_RECIPES.value()] = getItemStack("gtceu:chemical_distort")
            it[GTLRecipeTypes.QFT_RECIPES.value()] = getItemStack("gtceu:qft")
            it[GTLRecipeTypes.MAGIC_MANUFACTURER_RECIPES.value()] = getItemStack("gtceu:magic_manufacturer")
            it[GTLRecipeTypes.WORLD_DATA_SCANNER_RECIPES.value()] = getItemStack("gtceu:opv_world_data_scanner")
            it[GTLRecipeTypes.DECAY_HASTENER_RECIPES.value()] = MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE.asStack()
            it[GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.value()] = getItemStack("gtceu:door_of_create")
            it[GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.value()] = getItemStack("gtceu:create_aggregation")
            it[GTLRecipeTypes.DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES.value()] =
                getItemStack("gtceu:dimensionally_transcendent_plasma_forge")
            it[GTLRecipeTypes.STELLAR_FORGE_RECIPES.value()] = getItemStack("gtceu:stellar_forge")
        }
    }
}