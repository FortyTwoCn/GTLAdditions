package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry
import net.minecraft.resources.ResourceLocation
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineB
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB
import org.gtlcore.gtlcore.config.ConfigHolder
import kotlin.math.min
import kotlin.math.roundToInt

object ThreadMultiplierStrategy {
    private val BLOCK_MULTIPLIER_MAP = mapOf<ResourceLocation, Int>(
        MultiBlockMachineA.FISHING_GROUND.id to 4,
        MultiBlockMachineA.LARGE_GREENHOUSE.id to 4,
        MultiBlockMachineA.A_MASS_FABRICATOR.id to 16,
        AdditionalMultiBlockMachine.HUGE_INCUBATOR.id to 256,
        MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER.id to 1,
        AdvancedMultiBlockMachineA.SUPRACHRONAL_ASSEMBLY_LINE.id to 1,
        MultiBlockMachineA.NANO_CORE.id to 1,
        AdvancedMultiBlockMachineB.COMPRESSED_FUSION_REACTOR[GTValues.UEV]!!.id to 2,
        MultiBlockMachineA.LARGE_RECYCLER.id to 2048,
        MultiBlockMachineA.ADVANCED_SPS_CRAFTING.id to 1,
        MultiBlockMachineA.PETROCHEMICAL_PLANT.id to 1,
        MultiBlockMachineB.WOOD_DISTILLATION.id to 1,
        AdvancedMultiBlockMachineA.PCB_FACTORY.id to 2048,
        AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL.id to 4,
        MultiBlockMachineB.GRAVITATION_SHOCKBURST.id to 512,
        AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR.id to 2,
        MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE.id to 1,
        MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT.id to 1,
        MultiBlockMachineA.SUPER_PARTICLE_COLLIDER.id to 4,
        MultiBlockMachineA.MATTER_FABRICATOR.id to 3,
        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.id to 1,
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.id to 1,
        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.id to 256,
        MultiBlockMachine.DRACONIC_COLLAPSE_CORE.id to 2,
        MultiBlockMachine.TITAN_CRIP_EARTHBORE.id to 2,
        MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE.id to 2,
        MultiBlockMachine.PRIMORDIAL_EVOLUTION_NEXUS.id to 4,
        MultiBlockMachine.BIOSPHERE_III.id to 1024
    )

    fun getAdditionalMultiplier(definition: MultiblockMachineDefinition?): Int =
        getAdditionalMultiplier(definition?.id)

    fun getAdditionalMultiplier(entry: MachineEntry<MultiblockMachineDefinition>): Int =
        getAdditionalMultiplier(entry.id)

    private fun getAdditionalMultiplier(id: ResourceLocation?): Int {
        val result = min((1 / ConfigHolder.INSTANCE.durationMultiplier), 4096.0) * BLOCK_MULTIPLIER_MAP.getOrDefault(id, 2)
        return result.roundToInt()
    }
}