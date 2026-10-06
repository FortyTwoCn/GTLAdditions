package com.gtladd.gtladditions.common.machine.multiblock.structure

import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection

object MultiBlockStructure {
    val EYE_OF_HARMONY_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/eye_of_harmony.bin", "eye_of_harmony")
    }

    val FUXI_BAGUA_HEAVEN_FORGING_FURNACE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/fuxi_bagua_heaven_forging_furnace.bin", "fuxi_bagua_heaven_forging_furnace")
    }

    val BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/biological_simulation_laboratory.bin", "biological_simulation_laboratory")
    }

    val TITAN_CRIP_EARTHBORE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/titan_crip_earthbore.bin", "titan_crip_earthbore")
    }

    val DRACONIC_COLLAPSE_CORE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/draconic_collapse_core.bin", "draconic_collapse_core")
    }

    val LUCID_ETCHDREAMER_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/lucid_etchdreamer.bin", "lucid_etchdreamer")
    }

    val QUANTUM_SYPHON_MATRIX_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/quantum_syphon_matrix.bin", "quantum_syphon_matrix")
    }

    val ANTIENTROPY_CONDENSATION_CENTER_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/antientropy_condensation_center.bin", "antientropy_condensation_center")
    }

    val TAIXU_TURBID_ARRAY_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/taixu_turbid_array.bin",
            "taixu_turbid_array",
            listOf(StructureResourceLoader.RepeatableAisle(10, 1, 16)),
            RelativeDirection.RIGHT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val INFERNO_CLEFT_SMELTING_VAULT: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/inferno_cleft_smelting_vault.bin", "inferno_cleft_smelting_vault")
    }

    val SKELETON_SHIFT_RIFT_ENGINE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/skeleton_shift_rift_engine.bin",
            "skeleton_shift_rift_engine",
            RelativeDirection.FRONT,
            RelativeDirection.UP,
            RelativeDirection.RIGHT
        )
    }

    val TIME_SPACE_DISTORTER_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/time_space_distorter.bin", "time_space_distorter")
    }

    val APOCALYPTIC_TORSION_QUANTUM_MATRIX: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/apocalyptic_torsion_quantum_matrix.bin",
            "apocalyptic_torsion_quantum_matrix",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val FORGE_OF_THE_ANTICHRIST: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/forge_of_the_antichrist.bin",
            "forge_of_the_antichrist",
            RelativeDirection.LEFT,
            RelativeDirection.DOWN,
            RelativeDirection.BACK
        )
    }

    val ANNIHILATE_GENERATOR_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/annihilate_generator.bin", "annihilate_generator")
    }

    val FORGE_OF_THE_ANTICHRIST_MODULE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/forge_of_the_antichrist_module.bin", "forge_of_the_antichrist_module")
    }

    val DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/dimension_focus_infinity_crafting_array.bin",
            "dimension_focus_infinity_crafting_array",
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val LIGHT_HUNTER_SPACE_STATION: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/light_hunter_space_station.bin",
            "light_hunter_space_station",
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.FRONT
        )
    }

    val LIGHT_HUNTER_SPACE_STATION_MODULE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/light_hunter_space_station_module.bin",
            "light_hunter_space_station_module",
            RelativeDirection.RIGHT,
            RelativeDirection.UP,
            RelativeDirection.FRONT
        )
    }

    val SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/space_infinity_integrated_ore_processor.bin",
            "space_infinity_integrated_ore_processor",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/macro_atomic_resonant_fragment_stripper.bin",
            "macro_atomic_resonant_fragment_stripper",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val PLANETARY_IONISATION_CONVERGENCE_TOWER: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/planetary_ionisation_convergence_tower.bin", "planetary_ionisation_convergence_tower")
    }

    val ARCANE_CACHE_VAULT_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/arcane_cache_vault.bin", "arcane_cache_vault")
    }

    val SUBATOMIC_TRANSMUTATIOON_CORE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/subatomic_transmutatioon_core.bin", "subatomic_transmutatioon_core")
    }

    val RECURSIVE_REVERSE_FORGE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/recursive_reverse_forge.bin", "recursive_reverse_forge")
    }

    val RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern(
            "multiblock/recursive_reverse_forge_module.bin",
            "recursive_reverse_forge_module",
            RelativeDirection.BACK,
            RelativeDirection.UP,
            RelativeDirection.RIGHT
        )
    }

    val PRIMORDIAL_EVOLUTION_NEXUS_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/primordial_evolution_nexus.bin", "primordial_evolution_nexus")
    }

    val BIOSPHERE_III_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/biosphere_iii.bin", "biosphere_iii")
    }

    val BIOSPHERE_III_MODULE_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/biosphere_iii_module.bin", "biosphere_iii_module")
    }

    val SPACE_ELEVATOR_MKII_STRUCTURE: MultiblockPatternBuilder get() {
        return StructureResourceLoader.loadFactoryPattern("multiblock/space_elevator_mkii.bin", "space_elevator_mkii")
    }
}