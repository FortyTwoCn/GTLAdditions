package com.gtladd.gtladditions.common.recipe

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.gui.ProgressBarTextureSet
import com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE
import com.gregtechceu.gtceu.common.mui.GTGuiTextures
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.modify.GTLAddSoundEntries
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.COSMOS_SIMULATION_RECIPES
import org.gtlcore.gtlcore.common.data.GTLSoundEntries
import org.gtlcore.gtlcore.common.recipe.GTLRecipeUIModifiers
import brachy.modularui.drawable.UITexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/** Native queued entries retain the original GT namespace and bind during registry events. */
object GTLAddRecipesTypes {
    fun init() {}

    @JvmField val PHOTON_MATRIX_ETCH: GTRecipeTypeEntry = REGISTRATE.recipeType("photon_matrix_etch", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(3, 1, 1, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val EM_RESONANCE_CONVERSION_FIELD: GTRecipeTypeEntry = REGISTRATE.recipeType("em_resonance_conversion_field", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 1, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val TECTONIC_FAULT_GENERATOR: GTRecipeTypeEntry = REGISTRATE.recipeType("tectonic_fault_generator", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 1, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val BIOLOGICAL_SIMULATION: GTRecipeTypeEntry = REGISTRATE.recipeType("biological_simulation", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(3, 7, 2, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val VOIDFLUX_REACTION: GTRecipeTypeEntry = REGISTRATE.recipeType("voidflux_reaction", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(3, 0, 0, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val STELLAR_LGNITION: GTRecipeTypeEntry = REGISTRATE.recipeType("stellar_lgnition", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 0, 1, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(ProgressBarTextureSet(GTGuiTextures.PROGRESS_BAR_ARC_FURNACE))
            .addRecipeUIModifier(GTLRecipeUIModifiers.componentInfo { data ->
                Component.translatable("gtceu.recipe.temperature", FormattingUtil.formatNumbers(data.getInt("ebf_temp")))
            })
            .addRecipeUIModifier(GTLRecipeUIModifiers.componentInfo { data ->
                val material = ICoilType.getMinRequiredType(data.getInt("ebf_temp"))?.material
                if (material == null) Component.empty() else
                    Component.translatable("gtceu.recipe.coil.tier", Component.translatable(material.unlocalizedName))
            })
            .addRecipeUIModifier(GTLRecipeUIModifiers.coils(GTLRecipeUIModifiers.STANDARD_COILS)) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val CHAOTIC_ALCHEMY: GTRecipeTypeEntry = REGISTRATE.recipeType("chaotic_alchemy", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(9, 0, 3, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW)
            .addRecipeUIModifier(GTLRecipeUIModifiers.componentInfo { data ->
                Component.translatable("gtceu.recipe.temperature", FormattingUtil.formatNumbers(data.getInt("ebf_temp")))
            })
            .addRecipeUIModifier(GTLRecipeUIModifiers.componentInfo { data ->
                val material = ICoilType.getMinRequiredType(data.getInt("ebf_temp"))?.material
                if (material == null) Component.empty() else
                    Component.translatable("gtceu.recipe.coil.tier", Component.translatable(material.unlocalizedName))
            })
            .addRecipeUIModifier(GTLRecipeUIModifiers.coils(GTLRecipeUIModifiers.STANDARD_COILS))
            for (slot in 0 until 9) ui.setItemSlotOverlay(IO.IN, slot, GTGuiTextures.FURNACE_OVERLAY_1)
            for (slot in 0 until 3) ui.setFluidSlotOverlay(IO.IN, slot, GTGuiTextures.FURNACE_OVERLAY_2)
            ui.setFluidSlotOverlay(IO.OUT, 0, GTGuiTextures.FURNACE_OVERLAY_2)
            ui }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val ANTIENTROPY_CONDENSATION: GTRecipeTypeEntry = REGISTRATE.recipeType("antientropy_condensation", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 2, 2, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val MOLECULAR_DECONSTRUCTION: GTRecipeTypeEntry = REGISTRATE.recipeType("molecular_deconstruction", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 0, 0, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val UNIVERSE_SANDBOX: GTRecipeTypeEntry = REGISTRATE.recipeType("universe_sandbox", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 120, 1, 18)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val CHAOS_WEAVE: GTRecipeTypeEntry = REGISTRATE.recipeType("chaos_weave", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 1, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val GENESIS_ENGINE: GTRecipeTypeEntry = REGISTRATE.recipeType("genesis_engine", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 1, 0, 0)
        .setEUIO(IO.OUT)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.GENESIS_ENGINE)
        .register()

    @JvmField val STAR_CORE_STRIPPER: GTRecipeTypeEntry = REGISTRATE.recipeType("star_core_stripper", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(3, 12, 1, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_MACERATE) }
        .setSound(GTSoundEntries.MINER)
        .register()

    @JvmField val MATTER_EXOTIC: GTRecipeTypeEntry = REGISTRATE.recipeType("matter_exotic", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 1, 4, 1)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW)
            .addRecipeUIModifier { _, widget ->
                widget.textComponents.child(UITexture.fullImage(
                    ResourceLocation.fromNamespaceAndPath("gtladditions", "textures/gui/matter_exotic_catalyst.png"))
                    .asWidget().size(16))
            } }
        .setSound(GTLSoundEntries.FUSIONLOOP)
        .register()

    @JvmField val LEYLINE_CRYSTALLIZE: GTRecipeTypeEntry = REGISTRATE.recipeType("leyline_crystallize", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(9, 1, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.ARC)
        .register()

    @JvmField val INTER_STELLAR: GTRecipeTypeEntry = REGISTRATE.recipeType("inter_stellar", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 0, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.INTER_STELLAR)
        .register()

    @JvmField val NIGHTMARE_CRAFTING: GTRecipeTypeEntry = REGISTRATE.recipeType("nightmare_crafting", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(16, 1, 1, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.SCIENCE)
        .register()

    @JvmField val SPACE_ORE_PROCESSOR: GTRecipeTypeEntry = REGISTRATE.recipeType("space_ore_processor", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(2, 9, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.MACERATOR)
        .register()

    @JvmField val TRANSMUTATION_BLOCK_CONVERSION: GTRecipeTypeEntry = REGISTRATE.recipeType("transmutation_block_conversion", GTRecipeTypes.DUMMY)
        .setMaxIOSize(0, 0, 0, 0)
        .setXEIVisible(false)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.INTER_STELLAR)
        .register()

    @JvmField val SPACETIME_STASIS: GTRecipeTypeEntry = REGISTRATE.recipeType("spacetime_stasis", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(0, 0, 1, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.SCIENCE)
        .register()

    @JvmField val SUPRATEMPORAL_BOOSTING: GTRecipeTypeEntry = REGISTRATE.recipeType("supratemporal_boosting", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 0, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.GENESIS_ENGINE)
        .register()

    @JvmField val TIME_SPACE_DISTORTION: GTRecipeTypeEntry = REGISTRATE.recipeType("time_space_distortion", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 0, 0, 0)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.QUANTUM_OSCILLATION)
        .register()

    @JvmField val COMPRESSED_ASTRAL_ARRAY: GTRecipeTypeEntry = REGISTRATE.recipeType("compressed_astral_array", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(4, 0, 1, 0)
        .setEUIO(IO.IN)
        .setXEIVisible(false)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.SCIENCE)
        .register()

    @JvmField val EVOLUTION_OF_PRIMORDIAL: GTRecipeTypeEntry = REGISTRATE.recipeType("evolution_of_primordial", GTRecipeTypes.MULTIBLOCK)
        .setMaxIOSize(1, 1, 2, 16)
        .setEUIO(IO.IN)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTSoundEntries.CHEMICAL)
        .register()

    @JvmField val FORGE_OF_THE_ANTICHRIST: GTRecipeTypeEntry = REGISTRATE.recipeType("forge_of_the_antichrist", GTRecipeTypes.DUMMY)
        .setMaxIOSize(0, 0, 0, 0)
        .setXEIVisible(false)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.FORGE_OF_THE_ANTICHRIST)
        .register()

    @JvmField val QUANTUM_OSCILLATION: GTRecipeTypeEntry = REGISTRATE.recipeType("quantum_oscillation", GTRecipeTypes.DUMMY)
        .setMaxIOSize(0, 0, 0, 0)
        .setXEIVisible(false)
        .UI { ui -> ui.setProgressBar(GTGuiTextures.PROGRESS_ARROW) }
        .setSound(GTLAddSoundEntries.QUANTUM_OSCILLATION)
        .register()

    @JvmField val MULTIPLE_TYPE_RECIPES: Set<GTRecipeTypeEntry> = setOf(
        FORGE_OF_THE_ANTICHRIST, QUANTUM_OSCILLATION, COSMOS_SIMULATION_RECIPES)

    @JvmStatic fun isMultiRecipeType(type: GTRecipeType): Boolean = MULTIPLE_TYPE_RECIPES.any { it.id == type.id }
}
