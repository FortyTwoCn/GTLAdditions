package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines
import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry
import com.gtladd.gtladditions.api.registry.NativeQueuedMachines
import com.gtladd.gtladditions.api.registry.replaceQueuedFactory
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.multiblock.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction
import org.gtlcore.gtlcore.api.recipe.CoreRecipeModifiers
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines
import com.gregtechceu.gtceu.common.machine.multiblock.electric.ActiveTransformerMachine
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.pattern.PatternPredicateSelector
import com.gtladd.gtladditions.api.pattern.patchPatternPredicates
import com.gtladd.gtladditions.api.pattern.patternPredicateSelector
import com.gtladd.gtladditions.api.pattern.replacePatternPredicates
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.multiblock.controller.BasicOreProcessorMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.MolecularAssemblerMultiblockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.AdvancedInfiniteDrillMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.CreateAggregation
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.DoorOfCreate
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineB
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.MachineUtil
import org.gtlcore.gtlcore.utils.Registries
import org.gtlcore.gtlcore.utils.Registries.getItem
import java.util.function.Predicate
import net.minecraft.core.registries.Registries as MinecraftRegistries

@Suppress("DuplicatedCode")
object MultiBlockModify {

    private val createDimension = ResourceKey.create(
        MinecraftRegistries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath("kubejs", "create")
    )

    private val doorOfCreateOnWorking = Predicate { machine: IRecipeLogicMachine ->
        if (machine.recipeLogic.progress == 5 && machine is DoorOfCreate) {
            (machine.level as? ServerLevel)?.let { level ->
                val pos = machine.self().blockPos.offset(0, -13, 0)

                level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    1000,
                    4.0,
                    4.0,
                    4.0,
                    0.01
                )

                val entities = level.getEntitiesOfClass(
                    Entity::class.java,
                    AABB(
                        pos.x - 10.0,
                        pos.y - 10.0,
                        pos.z - 10.0,
                        pos.x + 10.0,
                        pos.y + 10.0,
                        pos.z + 10.0
                    )
                )

                for (entity in entities) {
                    if (entity is ItemEntity) {
                        when {
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.item.item).toString() == "gtceu:magnetohydrodynamicallyconstrainedstarmatter_block" -> {
                                MachineUtil.createItemEntity(
                                    level,
                                    entity.x,
                                    entity.y,
                                    entity.z,
                                    ItemStack(Blocks.COMMAND_BLOCK, entity.item.count)
                                )
                                entity.discard()
                            }

                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.item.item).toString() == "gtceu:magmatter_ingot" && entity.item.count >= 64 -> {
                                MachineUtil.createItemEntity(
                                    level,
                                    entity.x,
                                    entity.y,
                                    entity.z,
                                    ItemStack(getItem("gtceu:magmatter_block"), entity.item.count / 64)
                                )
                                entity.discard()
                            }
                        }
                    } else if (entity is ServerPlayer) {
                        if (MachineUtil.hasFullArmorSet(entity)) {
                            level.server.getLevel(createDimension)?.let { targetLevel ->
                                entity.teleportTo(
                                    targetLevel,
                                    0.0,
                                    1.0,
                                    0.0,
                                    entity.yRot,
                                    entity.xRot
                                )
                            }
                        } else {
                            entity.displayClientMessage("message.gtlcore.equipment_incompatible_dimension".toComponent, true)
                        }
                    }
                }
            }
        }
        true
    }

    private val createAggregationOnWorking = Predicate { machine: IRecipeLogicMachine ->
        if (machine.recipeLogic.getProgress() == 19 && machine is CreateAggregation) {
            machine.level?.let { level ->
                val pos = machine.self().blockPos.offset(0, -16, 0)
                val blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).block).toString()
                when {
                    blockId == "kubejs:command_block_broken" &&
                        MachineIO.inputItem(
                            machine as WorkableMultiblockMachine,
                            Registries.getItemStack("kubejs:chain_command_block_core")
                        ) -> level.setBlockAndUpdate(pos, Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState())

                    blockId == "kubejs:chain_command_block_broken" &&
                        MachineIO.inputItem(
                            machine as WorkableMultiblockMachine,
                            Registries.getItemStack("kubejs:repeating_command_block_core")
                        ) -> level.setBlockAndUpdate(pos, Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState())
                }
            }
        }
        true
    }

    private val recipeModifierList = RecipeModifierList(
        { machine, _ ->
            ModifierFunction { source ->
                CoreRecipeModifiers.accurateParallel(
                    machine, source,
                    Ints.saturatedCast(1L + (machine as IThreadModifierMachine).getAdditionalThread()), false
                ).first
            }
        },
        GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.create(1.0, 1.0, false))
    )

    fun init() {
        MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR.replaceQueuedFactory { blockEntity: BlockEntityCreationInfo ->
            BasicOreProcessorMachine(blockEntity)
        }
        NativeQueuedMachines.builder(MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR).recipeModifier(RecipeModifierList(
            GTRecipeModifiers.PARALLEL_HATCH,
            BasicOreProcessorMachine::recipeModifier
        ))
        MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR.patchPatternPredicates(
            "ore_processor_hatch",
            patternPredicateSelector({ GTBlocks.CASING_STAINLESS_CLEAN.get() }, PartAbility.PARALLEL_HATCH, PartAbility.MAINTENANCE),
            { Predicates.blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.block).setMaxGlobalLimited(1) }
        )
        MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR.patchPatternPredicates(
            "ore_processor_hatch",
            patternPredicateSelector({ GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get() }, PartAbility.INPUT_LASER),
            { Predicates.blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.block).setMaxGlobalLimited(1) }
        )

        val threadModifierPredicate = { Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1) }
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { GTLBlocks.DIMENSION_CONNECTION_CASING.get() },
                PartAbility.IMPORT_ITEMS,
                PartAbility.EXPORT_ITEMS,
                PartAbility.INPUT_ENERGY
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.replaceQueuedFactory { blockEntity: BlockEntityCreationInfo ->
            DoorOfCreate(blockEntity)
        }
        NativeQueuedMachines.builder(AdvancedMultiBlockMachineA.DOOR_OF_CREATE).onWorking(doorOfCreateOnWorking)
        NativeQueuedMachines.builder(AdvancedMultiBlockMachineA.DOOR_OF_CREATE).recipeModifier(recipeModifierList)

        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { GTLBlocks.DIMENSION_CONNECTION_CASING.get() },
                PartAbility.IMPORT_ITEMS,
                PartAbility.EXPORT_ITEMS,
                PartAbility.INPUT_ENERGY,
                PartAbility.COMPUTATION_DATA_RECEPTION
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.replaceQueuedFactory { blockEntity: BlockEntityCreationInfo ->
            CreateAggregation(blockEntity)
        }
        NativeQueuedMachines.builder(AdvancedMultiBlockMachineA.CREATE_AGGREGATION).onWorking(createAggregationOnWorking)
        NativeQueuedMachines.builder(AdvancedMultiBlockMachineA.CREATE_AGGREGATION).recipeModifier(recipeModifierList)

        GTMultiMachines.ACTIVE_TRANSFORMER.replacePatternPredicates(
            "active_transformer_min_casing_limit",
            patternPredicateSelector({ GTBlocks.HIGH_POWER_CASING.get() }),
            {
                Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get())
                    .or(ActiveTransformerMachine.getHatchPredicates())
            }
        )

        AdditionalMultiBlockMachine.MOLECULAR_ASSEMBLER_MATRIX.replaceQueuedFactory { blockEntity: BlockEntityCreationInfo ->
            MolecularAssemblerMultiblockMachine(blockEntity)
        }

        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { Registries.getBlock("gtlcore:iridium_casing") },
                PartAbility.IMPORT_FLUIDS,
                PartAbility.EXPORT_FLUIDS,
                PartAbility.INPUT_ENERGY,
                PartAbility.INPUT_LASER
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.replaceQueuedFactory { blockEntity: BlockEntityCreationInfo ->
            AdvancedInfiniteDrillMachine(blockEntity)
        }

        val networkSwitch = GTResearchMachines.NETWORK_SWITCH
        networkSwitch.replacePatternPredicates(
            "cloud_computation_hatch_exclusion",
            patternPredicateSelector({ GTBlocks.COMPUTER_CASING.get() }),
            {
                Predicates.blocks(GTBlocks.COMPUTER_CASING.get()).setMinGlobalLimited(7)
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                    .or(
                        Predicates.blocks(
                            *PartAbility.COMPUTATION_DATA_RECEPTION.allBlocks.stream()
                                .filter { it != GTLAddMachines.CLOUD_COMPUTATION_HATCH_RECEIVER.block }
                                .toList()
                                .toTypedArray()
                        ).setMinGlobalLimited(1, 2)
                    )
                    .or(
                        Predicates.blocks(
                            *PartAbility.COMPUTATION_DATA_TRANSMISSION.allBlocks.stream()
                                .filter { it != GTLAddMachines.CLOUD_COMPUTATION_HATCH_TRANSMITTER.block }
                                .toList()
                                .toTypedArray()
                        ).setMinGlobalLimited(1, 1)
                    )
            }
        )

        val researchStation = GTResearchMachines.RESEARCH_STATION
        researchStation.replacePatternPredicates(
            "research_station_internal_air_relaxation",
            PatternPredicateSelector.pureAir(),
            { Predicates.any() }
        )
        val researchProperties = NativeQueuedMachines.builder(researchStation).properties
        val oldResearchTooltip = researchProperties.tooltipBuilder()
        researchProperties.tooltipBuilder { stack, components ->
            oldResearchTooltip?.accept(stack, components)
            components.add("tooltip.gtladditions.cloud_systems".toComponent)
            components.add("tooltip.gtladditions.research_station.cloud_upload".toComponent)
            components.add(GTLAddMachines.GTLAdd_MODIFY)
        }
    }
}