package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.mui.factory.MachineUIFactory;
import brachy.modularui.api.RecipeViewerSettings;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.WidgetTree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.Comparator;

/** Exercises real factory registrations, owner-bound traits, registry-aware saves and native server menus. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionMachineGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void verifyMachines(GameTestHelper helper, boolean additions) {
        var firstPos = new BlockPos(3, 4, 3);
        var secondPos = new BlockPos(6, 4, 3);
        int count = 0, menus = 0;
        var blocks = BuiltInRegistries.BLOCK.stream().filter(MetaMachineBlock.class::isInstance)
                .map(MetaMachineBlock.class::cast)
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString())).toList();
        for (var block : blocks) {
            // Creating an unplaced entity also catches factory/trait failures before any world callbacks run.
            var state = block.defaultBlockState();
            var preview = block.getDefinition().getBlockEntityType().create(BlockPos.ZERO, state);
            if (!(preview instanceof MetaMachine machine)) continue;
            String className = machine.getClass().getName();
            boolean owned = additions ? className.startsWith("com.gtladd.") : className.startsWith("org.gtlcore.");
            if (!owned) continue;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            LOGGER.info("GTL_NATIVE_PRODUCTION_CHECK {} {}", id, className);
            helper.setBlock(firstPos, block);
            helper.setBlock(secondPos, block);
            var first = (MetaMachine) helper.getBlockEntity(firstPos);
            var second = (MetaMachine) helper.getBlockEntity(secondPos);
            helper.assertTrue(first != second && first.getClass() == machine.getClass(), "Invalid production factory for " + id);
            for (var trait : first.getTraits(MachineTrait.class)) {
                helper.assertTrue(trait.getMachine() == first, "Trait attached to the wrong production machine: " + id);
                helper.assertTrue(!second.getTraits(MachineTrait.class).contains(trait), "Shared mutable production trait: " + id);
            }
            var registries = helper.getLevel().registryAccess();
            var saved = first.saveWithoutMetadata(registries);
            var restored = (MetaMachine) block.getDefinition().getBlockEntityType().create(first.getBlockPos(), first.getBlockState());
            helper.assertTrue(restored != null, "Missing production block entity type for " + id);
            restored.loadAdditional(saved, registries);
            var savedAgain = restored.saveWithoutMetadata(registries);
            helper.assertTrue(saved.equals(savedAgain), "Production save/load is not stable: " + id);
            // Replacement uses the real native entity lifecycle, not just a detached deserialization fixture.
            helper.getLevel().removeBlockEntity(first.getBlockPos());
            helper.getLevel().setBlockEntity(restored);
            if (restored instanceof IMuiMachine || block.getDefinition().getUI() != null) {
                var player = FakePlayerFactory.getMinecraft(helper.getLevel());
                player.setPos(restored.getBlockPos().getCenter());
                var hit = new BlockHitResult(restored.getBlockPos().getCenter(), Direction.NORTH, restored.getBlockPos(), false);
                // Some parts never offer a menu; others require their original binding or proxy link.
                if (!(restored instanceof IMuiMachine ui) || ui.shouldOpenUI(player, InteractionHand.MAIN_HAND, hit)) {
                    var sync = new PanelSyncManager(new ModularSyncManager(false), true);
                    var panel = MachineUIFactory.INSTANCE.createPanel(new PosGuiData(player, restored.getBlockPos()), sync,
                            new UISettings(RecipeViewerSettings.DUMMY));
                    helper.assertTrue(panel != null, "Missing production native menu for " + id);
                    WidgetTree.collectSyncValues(sync, panel);
                    menus++;
                } else {
                    LOGGER.info("GTL_NATIVE_PRODUCTION_MENU_UNAVAILABLE_UNBOUND {} eligibility_preserved=true", id);
                }
            }
            helper.setBlock(firstPos, Blocks.AIR);
            helper.setBlock(secondPos, Blocks.AIR);
            count++;
        }
        helper.assertTrue(count > 0 && menus > 0, "Production machine registrations were not exercised");
        LOGGER.info("GTL_NATIVE_PRODUCTION_MACHINES {} machines={} menus={}", additions ? "additions" : "core", count, menus);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "gtl_production_additions", timeoutTicks = 400)
    public static void completeAdditionsMachinesCreateSaveReloadAndBuildNativeMenus(GameTestHelper helper) {
        verifyMachines(helper, true);
    }

    @GameTest(template = "empty", batch = "gtl_production_core", timeoutTicks = 400)
    public static void completeCoreMachinesCreateSaveReloadAndBuildNativeMenus(GameTestHelper helper) {
        verifyMachines(helper, false);
    }
}
