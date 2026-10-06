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
import java.util.LinkedHashSet;

/** Original larger pattern bounds and explicit preview variants, with unchanged native predicates. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionStructureVariantsGameTests {
    @GameTest(template = "empty", batch = "gtl_production_structure_variants", timeoutTicks = 2400)
    public static void originalMaximumAndIntermediateVariantsFormBreakAndReform(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK.stream().filter(MetaMachineBlock.class::isInstance)
                .map(MetaMachineBlock.class::cast)
                .filter(block -> block.getDefinition() instanceof MultiblockMachineDefinition)
                .filter(block -> {
                    var entity = block.getDefinition().getBlockEntityType().create(BlockPos.ZERO, block.defaultBlockState());
                    var name = entity.getClass().getName();
                    return name.startsWith("org.gtlcore.") || name.startsWith("com.gtladd.");
                }).sorted(Comparator.comparing(block -> block.getDefinition().getId().toString())).toList();
        var failures = new ArrayList<String>();
        int[] completed = {0}, maximums = {0};
        var origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 8192, 96,
                helper.absolutePos(BlockPos.ZERO).getZ() + 4096);
        var sequence = helper.startSequence();
        for (var block : blocks) {
            var definition = (MultiblockMachineDefinition) block.getDefinition();
            var pages = NativePreviewShapes.get(definition);
            if (pages.size() <= 1) continue;
            var indexes = new LinkedHashSet<Integer>();
            if (pages.size() <= 8) for (int i = 1; i < pages.size(); i++) indexes.add(i);
            else { indexes.add(1); indexes.add(pages.size() / 2); indexes.add(pages.size() - 1); }
            for (int index : indexes) sequence.thenExecuteAfter(1, () -> {
                var placed = new ArrayList<BlockPos>();
                var id = definition.getId();
                try {
                    var front = definition.getRotationState().defaultDirection;
                    var up = front.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
                    var geometry = pages.get(index).oriented(front, up, false);
                    var level = helper.getLevel();
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURE_VARIANT_CHECK id={} page={} pages={} blocks={}", id, index, pages.size(), geometry.states().size());
                    for (var entry : geometry.states().entrySet()) {
                        if (entry.getKey().equals(BlockPos.ZERO)) continue;
                        var target = origin.offset(entry.getKey());
                        if (level.isOutsideBuildHeight(target)) throw new AssertionError("Original variant exceeds world build height: " + target);
                        level.setBlockAndUpdate(target, entry.getValue()); placed.add(target);
                    }
                    level.setBlockAndUpdate(origin, geometry.states().get(BlockPos.ZERO)); placed.add(origin);
                    var machine = (MultiblockControllerMachine) level.getBlockEntity(origin);
                    machine.checkAndFormStructure();
                    if (!machine.isFormed()) throw new AssertionError("Native original variant failed formation: " + machine.getDefaultPatternState().getErrors());
                    for (var part : machine.getParts()) if (!part.hasController(origin))
                        throw new AssertionError("Native larger variant part did not bind");
                    boolean invalidated = false;
                    var candidates = geometry.states().entrySet().stream().filter(entry -> !entry.getKey().equals(BlockPos.ZERO)
                            && !entry.getValue().isAir() && entry.getValue().getFluidState().isEmpty()
                            && geometry.predicates().containsKey(entry.getKey())).limit(64).toList();
                    for (var entry : candidates) {
                        var target = origin.offset(entry.getKey());
                        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState()); machine.checkAndFormStructure();
                        invalidated = !machine.isFormed();
                        level.setBlockAndUpdate(target, entry.getValue()); machine.checkAndFormStructure();
                        if (!machine.isFormed()) throw new AssertionError("Native larger variant repair did not reform");
                        if (invalidated) break;
                    }
                    if (!invalidated) throw new AssertionError("Native larger variant ignored removal of required blocks");
                    completed[0]++; if (index == pages.size() - 1) maximums[0]++;
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURE_VARIANT id={} page={} maximum={} formed=true break=true reform=true parts={}", id, index, index == pages.size() - 1, machine.getParts().size());
                } catch (Exception | AssertionError error) {
                    failures.add(id + " page=" + index + ": " + error);
                    LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_STRUCTURE_VARIANT_FAILURE id={} page={}", id, index, error);
                } finally {
                    helper.getLevel().setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                    for (var target : placed) if (!target.equals(origin)) helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 2);
                }
            });
        }
        sequence.thenExecute(() -> {
            helper.assertTrue(completed[0] > 0 && maximums[0] > 0 && failures.isEmpty(), "Original larger variant failures: " + failures);
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STRUCTURE_VARIANTS count={} maximums={} original=true break_reform=true", completed[0], maximums[0]);
        }).thenSucceed();
    }
}
