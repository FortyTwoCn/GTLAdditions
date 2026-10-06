package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.gtlcore.gtlcore.api.gui.preview.NativePreviewShapes;
import java.util.ArrayList;
import java.util.Comparator;

/** Places the real minimum original preview structures, then uses native checks and part lifecycle. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionStructureGameTests {
    @GameTest(template = "empty", batch = "gtl_production_structures", timeoutTicks = 1200)
    public static void originalProductionMinimumStructuresFormBreakAndReform(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK.stream().filter(MetaMachineBlock.class::isInstance)
                .map(MetaMachineBlock.class::cast)
                .filter(block -> block.getDefinition() instanceof MultiblockMachineDefinition)
                .filter(block -> {
                    var entity = block.getDefinition().getBlockEntityType().create(BlockPos.ZERO, block.defaultBlockState());
                    var name = entity.getClass().getName();
                    return name.startsWith("org.gtlcore.") || name.startsWith("com.gtladd.");
                }).sorted(Comparator.comparing(block -> block.getDefinition().getId().toString())).toList();
        var failures = new ArrayList<String>();
        int[] completed = {0};
        // Outside all GameTest template areas; use one arena sequentially and clear it after each structure.
        var origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 4096, 96,
                helper.absolutePos(BlockPos.ZERO).getZ() + 4096);
        var sequence = helper.startSequence();
        for (var block : blocks) sequence.thenExecuteAfter(1, () -> {
            var definition = (MultiblockMachineDefinition) block.getDefinition();
            var id = definition.getId();
            var placed = new ArrayList<BlockPos>();
            try {
                var rotation = definition.getRotationState();
                var front = rotation.defaultDirection;
                var up = front.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
                var geometry = NativePreviewShapes.get(definition).getFirst().oriented(front, up, false);
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURE_CHECK id={} blocks={}", id, geometry.states().size());
                var level = helper.getLevel();
                // Place the controller last so placement notifications cannot inspect an incomplete structure.
                for (var entry : geometry.states().entrySet()) {
                    if (entry.getKey().equals(BlockPos.ZERO)) continue;
                    var target = origin.offset(entry.getKey());
                    if (level.isOutsideBuildHeight(target)) throw new AssertionError("Original structure exceeds arena build height: " + target);
                    if (!level.setBlockAndUpdate(target, entry.getValue()) && !level.getBlockState(target).equals(entry.getValue()))
                        throw new AssertionError("Cannot place original structure block at " + target);
                    placed.add(target);
                }
                level.setBlockAndUpdate(origin, geometry.states().get(BlockPos.ZERO));
                placed.add(origin);
                var machine = (MultiblockControllerMachine) level.getBlockEntity(origin);
                if (machine instanceof org.gtlcore.gtlcore.common.machine.multiblock.SimulationMachine) {
                    machine.checkAndFormStructure();
                    if (machine.isFormed() || !machine.getParts().isEmpty())
                        throw new AssertionError("Virtual simulator formed or bound a real structure");
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_SIMULATOR virtual=true does_not_form=true");
                    return;
                }
                machine.checkAndFormStructure();
                if (!machine.isFormed()) {
                    for (var error : machine.getDefaultPatternState().getErrors()) {
                        var actual = level.getBlockState(error.getPos());
                        var actualEntity = level.getBlockEntity(error.getPos());
                        LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_STRUCTURE_ERROR id={} data={}", id,
                                com.gregtechceu.gtceu.api.multiblock.error.PatternError.CODEC
                                        .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, error).getOrThrow());
                        LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_STRUCTURE_ACTUAL id={} relative={} state={} entity={} definition={}", id,
                                error.getPos().subtract(origin), actual, actualEntity == null ? null : actualEntity.getClass().getName(),
                                actualEntity instanceof com.gregtechceu.gtceu.api.machine.MetaMachine meta ? meta.getDefinition().getId() : null);
                    }
                    throw new AssertionError("Native original pattern did not form: " + machine.getDefaultPatternState().getState());
                }
                for (var part : machine.getParts()) if (!part.hasController(origin))
                    throw new AssertionError("Native original part did not bind to its controller");
                var candidates = geometry.states().entrySet().stream().filter(entry -> !entry.getKey().equals(BlockPos.ZERO)
                                && !entry.getValue().isAir() && entry.getValue().getFluidState().isEmpty())
                        .filter(entry -> geometry.predicates().containsKey(entry.getKey()))
                        .limit(64).toList();
                boolean invalidated = false;
                for (var breakAt : candidates) {
                    var removed = origin.offset(breakAt.getKey());
                    level.setBlockAndUpdate(removed, Blocks.AIR.defaultBlockState());
                    machine.checkAndFormStructure();
                    invalidated = !machine.isFormed();
                    level.setBlockAndUpdate(removed, breakAt.getValue());
                    machine.checkAndFormStructure();
                    if (!machine.isFormed()) throw new AssertionError("Repairing original structure did not reform");
                    if (invalidated) break;
                }
                if (!invalidated) throw new AssertionError("Removing original required blocks retained formation");
                for (var part : machine.getParts()) if (!part.hasController(origin))
                    throw new AssertionError("Native repaired original part did not rebind");
                var drops = net.minecraft.world.level.block.Block.getDrops(machine.getBlockState(), level, origin, machine);
                if (drops.stream().noneMatch(stack -> stack.is(block.asItem())))
                    throw new AssertionError("Original production controller has no native machine loot drop");
                completed[0]++;
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURE id={} formed=true break=true reform=true native_loot=true parts={}", id, machine.getParts().size());
            } catch (Exception | AssertionError error) {
                failures.add(id + ": " + error);
                LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_STRUCTURE_FAILURE id={}", id, error);
            } finally {
                helper.getLevel().setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                for (var target : placed) if (!target.equals(origin)) helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 2);
            }
        });
        sequence.thenExecute(() -> {
            helper.assertTrue(!blocks.isEmpty() && failures.isEmpty(), "Original production structure failures: " + failures);
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURES count={} native_original_minimum=true break_reform=true", completed[0]);
        }).thenSucceed();
    }
}
