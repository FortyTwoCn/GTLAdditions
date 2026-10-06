package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.gtlcore.gtlcore.api.gui.preview.NativePreviewShapes;
import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;
import org.gtlcore.gtlcore.common.machine.multiblock.steam.LargeSteamParallelMultiblockMachine;

import java.util.Comparator;

/** Real original piston hammer, registered production recipe, actual buses, paid steam and native ticking. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionSteamGameTests {
    @GameTest(template = "empty", batch = "gtl_production_steam", timeoutTicks = 400)
    public static void originalSteamHammerPreservesPaidRecipeAcrossPauseAndWorldEntityReload(GameTestHelper helper) {
        verify(helper, false);
    }

    @GameTest(template = "empty", batch = "gtl_production_steam", timeoutTicks = 400)
    public static void originalSteamHammerKeepsRunningRecipeOverclockCostWhenControlsChange(GameTestHelper helper) {
        verify(helper, true);
    }

    private static void verify(GameTestHelper helper, boolean overclocked) {
        var level = helper.getLevel();
        var definition = MultiBlockMachineA.STEAM_PISTON_HAMMER.get();
        var geometry = NativePreviewShapes.get(definition).getFirst().oriented(Direction.NORTH, Direction.UP, false);
        var origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 8, 96,
                helper.absolutePos(BlockPos.ZERO).getZ() + 8);
        for (var entry : geometry.states().entrySet()) if (!entry.getKey().equals(BlockPos.ZERO))
            level.setBlockAndUpdate(origin.offset(entry.getKey()), entry.getValue());
        var hatchPosition = origin.offset(geometry.states().entrySet().stream()
                .filter(entry -> entry.getValue().is(GTMachines.STEAM_HATCH.getBlock())).findFirst().orElseThrow().getKey());
        if (overclocked) level.setBlockAndUpdate(hatchPosition, GTLMachines.LARGE_STEAM_HATCH.getBlock().defaultBlockState());
        level.setBlockAndUpdate(origin, geometry.states().get(BlockPos.ZERO));
        var original = (LargeSteamParallelMultiblockMachine) level.getBlockEntity(origin);
        original.checkAndFormStructure(); original.setWorkingEnabled(false);
        helper.assertTrue(original.isFormed() && original.isOC() == overclocked, "Original production steam hammer did not form with its real steam hatch");
        var input = original.getParts().stream().filter(ItemBusPartMachine.class::isInstance).map(ItemBusPartMachine.class::cast)
                .filter(bus -> bus.getInventory().getHandlerIO() == IO.IN).findFirst().orElseThrow();
        var output = original.getParts().stream().filter(ItemBusPartMachine.class::isInstance).map(ItemBusPartMachine.class::cast)
                .filter(bus -> bus.getInventory().getHandlerIO() == IO.OUT).findFirst().orElseThrow();
        var tank = ((FluidHatchPartMachine) level.getBlockEntity(hatchPosition)).tank;
        var recipe = level.getRecipeManager().getAllRecipesFor(GTRecipeTypes.FORGE_HAMMER_RECIPES.get()).stream()
                .map(holder -> holder.value()).filter(candidate -> candidate.conditions.isEmpty()
                        && candidate.duration >= 10 && candidate.duration <= 100 && candidate.getInputEUt().getTotalEU() > 0
                        && candidate.getInputEUt().getTotalEU() <= 32
                        && candidate.getInputContents(FluidRecipeCapability.CAP).isEmpty()
                        && candidate.getOutputContents(FluidRecipeCapability.CAP).isEmpty()
                        && candidate.getInputContents(ItemRecipeCapability.CAP).size() == 1
                        && candidate.getOutputContents(ItemRecipeCapability.CAP).size() == 1)
                .filter(candidate -> {
                    var in = candidate.getInputContents(ItemRecipeCapability.CAP).getFirst();
                    var out = candidate.getOutputContents(ItemRecipeCapability.CAP).getFirst();
                    var ingredient = ItemRecipeCapability.CAP.of(in.content());
                    var product = ItemRecipeCapability.CAP.of(out.content());
                    return in.chance() == in.maxChance() && out.chance() == out.maxChance()
                            && ingredient.count() > 0 && ingredient.count() <= 20 && ingredient.getItems().length > 0
                            && product.count() > 0 && product.count() <= 8 && product.getItems().length > 0;
                }).sorted(Comparator.comparing(candidate -> candidate.id.toString())).findFirst().orElseThrow();
        var ingredient = ItemRecipeCapability.CAP.of(recipe.getInputContents(ItemRecipeCapability.CAP).getFirst().content());
        var product = ItemRecipeCapability.CAP.of(recipe.getOutputContents(ItemRecipeCapability.CAP).getFirst().content());
        var source = ingredient.getItems()[0].copyWithCount(ingredient.count() * 3);
        var expected = product.getItems()[0].copyWithCount(product.count() * 3);
        input.getInventory().setStackInSlot(0, source);
        tank.getStorages()[0].setFluid(GTMaterials.Steam.getFluid(32_000));
        for (int i = 0; i < output.getInventory().getSlots(); i++)
            output.getInventory().setStackInSlot(i, new ItemStack(Items.BARRIER, 64));
        helper.assertTrue(!original.getRecipeLogic().checkMatchedRecipeAvailable(recipe)
                        && input.getInventory().getStackInSlot(0).getCount() == source.getCount()
                        && tank.getStorages()[0].getFluidAmount() == 32_000,
                "Blocked original production output consumed paid inputs or steam");
        for (int i = 0; i < output.getInventory().getSlots(); i++) output.getInventory().setStackInSlot(i, ItemStack.EMPTY);
        if (overclocked) original.setAmountOC(2);
        original.setWorkingEnabled(true);
        var logic = original.getRecipeLogic();
        helper.assertTrue(logic.checkMatchedRecipeAvailable(recipe) && IGTRecipe.of(logic.getLastRecipe()).getRealParallels() == 3
                        && input.getInventory().getStackInSlot(0).isEmpty(),
                "Original production steam hammer did not consume exactly three registered recipe inputs");
        long eu = recipe.getInputEUt().getTotalEU() * 3;
        helper.assertTrue(logic.getLastRecipe().getInputEUt().getTotalEU() == eu, "Production steam parallel EU changed");
        long steamPerTick = overclocked ? Math.floorDiv(eu * 2 + 8, 9) : eu * 2;
        int duration = logic.getDuration();
        long expectedSteam = 32_000 - steamPerTick * duration;
        logic.setSuspendAfterFinish(true);
        var current = new LargeSteamParallelMultiblockMachine[]{original};
        int[] pausedProgress = {0}, pausedSteam = {0};
        var sequence = helper.startSequence();
        if (overclocked) sequence.thenExecute(() -> original.setAmountOC(4));
        else sequence.thenWaitUntil(() -> helper.assertTrue(logic.getProgress() >= 2, "Real native server subscription did not advance paid steam recipe"))
                .thenExecute(() -> {
                    // Native working-enable controls finish the current recipe before stopping.
                    // Suspend the engine explicitly to test its persisted in-flight state.
                    logic.setStatus(RecipeLogic.Status.SUSPEND);
                    pausedProgress[0] = logic.getProgress(); pausedSteam[0] = tank.getStorages()[0].getFluidAmount();
                }).thenIdle(3).thenExecute(() -> {
                    helper.assertTrue(logic.getProgress() == pausedProgress[0] && tank.getStorages()[0].getFluidAmount() == pausedSteam[0],
                            "Paused original production machine still advanced or consumed paid steam");
                    var saved = original.saveWithoutMetadata(level.registryAccess());
                    var restored = (LargeSteamParallelMultiblockMachine) definition.getBlockEntityType().create(origin, original.getBlockState());
                    restored.loadAdditional(saved, level.registryAccess());
                    level.removeBlockEntity(origin); level.setBlockEntity(restored);
                    restored.checkAndFormStructure();
                    helper.assertTrue(restored.isFormed() && restored.getRecipeLogic().getProgress() == pausedProgress[0]
                                    && restored.getRecipeLogic().getLastRecipe() != null && input.getInventory().getStackInSlot(0).isEmpty(),
                            "Actual original controller replacement lost or replayed its paid recipe");
                    restored.getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
                    // Entering native SUSPEND consumes its finish-after-current-cycle flag.
                    restored.getRecipeLogic().setSuspendAfterFinish(true); current[0] = restored;
                });
        sequence.thenWaitUntil(() -> helper.assertTrue(current[0].getRecipeLogic().isSuspend(), "Original registered paid recipe did not finish under actual native ticks"))
                .thenExecute(() -> {
                    helper.assertTrue(tank.getStorages()[0].getFluidAmount() == expectedSteam, "Original paid steam cost changed: actual="
                            + tank.getStorages()[0].getFluidAmount() + " expected=" + expectedSteam);
                    int delivered = 0;
                    for (int i = 0; i < output.getInventory().getSlots(); i++) {
                        var stack = output.getInventory().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, expected), "Original recipe delivered the wrong product");
                            delivered += stack.getCount();
                        }
                    }
                    helper.assertTrue(delivered == expected.getCount() && input.getInventory().getStackInSlot(0).isEmpty(),
                            "Original paid process lost or duplicated items");
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STEAM original_structure=true recipe={} parallel=3 real_server_ticks=true blocked_output_before_start=true overclocked={} pause_reload={} exact_inputs=true exact_steam=true exact_output=true duration={}",
                            recipe.id, overclocked, !overclocked, duration);
                    level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
                    geometry.states().keySet().forEach(position -> level.setBlock(origin.offset(position), Blocks.AIR.defaultBlockState(), 2));
                }).thenSucceed();
    }
}
