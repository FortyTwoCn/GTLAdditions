package com.gtladd.gtladditions.migration;

import com.mojang.logging.LogUtils;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.core.definitions.AEBlocks;
import net.minecraft.nbt.CompoundTag;
import org.gtlcore.gtlcore.mixin.ae2.logic.ExecutingCraftingJobAccessor;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Conditional integration resources must be selected by the real native server pack repository. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionOptionalDataGameTests {
    @GameTest(template = "empty", batch = "gtl_production_optional_data", timeoutTicks = 200)
    public static void extendedAeProjectELootUsesNativeModCondition(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(server.getPackRepository().getSelectedPacks().stream().anyMatch(
                        pack -> pack.getId().contains("gtlcore") && pack.getId().contains("optional_integrations")),
                "Native optional integration data pack was not selected");
        var id = ResourceLocation.fromNamespaceAndPath("extendedae", "blocks/ex_emc_interface");
        var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        boolean projectE = ModList.get().isLoaded("projecte");
        boolean appliedE = ModList.get().isLoaded("appliede");
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("extendedae", "ex_emc_interface"));
        // Native ExtendedAE gates its EMC block on AppliedE, which itself requires ProjectE.
        // Installing ProjectE alone must not make a loot table reference an absent registry item.
        helper.assertTrue(appliedE ? projectE && item != Items.AIR && table != LootTable.EMPTY
                                  : item == Items.AIR && table == LootTable.EMPTY,
                "ExtendedAE native AppliedE registration and optional loot condition disagree");
        if (appliedE) {
            var pos = new BlockPos(1, 1, 1);
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("extendedae", "ex_emc_interface"));
            helper.setBlock(pos, block);
            var absolute = helper.absolutePos(pos);
            var drops = Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute,
                    helper.getLevel().getBlockEntity(absolute));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    "Actual native EMC interface does not drop its registered item exactly once");
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_OPTIONAL_DATA native_pack=true projecte_present={} appliede_present={} conditional_loot=true actual_interface_drop={}",
                projectE, appliedE, appliedE);
        if (!appliedE) {
            helper.succeed();
            return;
        }
        var cpuPosition = new BlockPos(3, 1, 1);
        helper.setBlock(cpuPosition, AEBlocks.CRAFTING_STORAGE_1K.block());
        var cpu = (CraftingBlockEntity) helper.getBlockEntity(cpuPosition);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                cpu.getCluster() != null && cpu.getCluster().getGrid() != null,
                "Real native crafting CPU and grid are not ready"))
                .thenExecute(() -> verifyTemporaryPatterns(helper, cpu)).thenSucceed();
    }

    /** Actual optional classes; reflection only exposes package-private native job setup for this fixture. */
    private static void verifyTemporaryPatterns(GameTestHelper helper, CraftingBlockEntity cpu) {
        try {
            var cluster = cpu.getCluster();
            var logic = cluster.craftingLogic;
            var pattern = (IPatternDetails) Class.forName("gripe._90.appliede.me.service.TransmutationPattern")
                    .getConstructor(AEItemKey.class, long.class, int.class)
                    .newInstance(AEItemKey.of(Items.STONE), 1L, 73);
            var serviceType = Class.forName("gripe._90.appliede.me.service.KnowledgeService");
            Object service = cluster.getGrid().getService(serviceType.asSubclass(appeng.api.networking.IGridService.class));
            var add = serviceType.getMethod("addTemporaryPattern", IPatternDetails.class);
            var patternsField = serviceType.getDeclaredField("temporaryPatterns");
            patternsField.setAccessible(true);
            var temporary = (Set<?>) patternsField.get(service);
            var jobField = CraftingCpuLogic.class.getDeclaredField("job");
            jobField.setAccessible(true);
            try {
                for (long remaining : new long[]{0, 1}) {
                    add.invoke(service, pattern);
                    helper.assertTrue(temporary.contains(pattern), "Actual AppliedE service did not retain its temporary pattern");
                    var plan = new CraftingPlan(new GenericStack(AEItemKey.of(Items.STONE), 1), 1, false, false,
                            new KeyCounter(), new KeyCounter(), new KeyCounter(), Map.of(pattern, remaining));
                    var listenerType = Class.forName("appeng.crafting.execution.ExecutingCraftingJob$CraftingDifferenceListener");
                    Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType},
                            (proxy, method, arguments) -> { cluster.markDirty(); return null; });
                    var constructor = ExecutingCraftingJob.class.getDeclaredConstructor(
                            ICraftingPlan.class, listenerType, CraftingLink.class, Integer.class);
                    constructor.setAccessible(true);
                    var linkData = new CompoundTag();
                    linkData.putUUID("craftId", UUID.randomUUID());
                    linkData.putBoolean("req", false);
                    linkData.putBoolean("standalone", true);
                    var job = constructor.newInstance(plan, listener, new CraftingLink(linkData, cluster), null);
                    jobField.set(logic, job);
                    if (remaining == 0) {
                        helper.assertTrue(logic.executeCrafting(1, (appeng.me.service.CraftingService) cluster.getGrid().getCraftingService(),
                                        cluster.getGrid().getEnergyService(), helper.getLevel()) == 0,
                                "Completed temporary task dispatched an extra operation");
                        helper.assertTrue(((ExecutingCraftingJobAccessor) job).getTasks().isEmpty(),
                                "Native CPU retained the completed temporary task");
                    } else {
                        logic.cancel();
                        helper.assertTrue(!logic.hasJob(), "Native CPU retained its cancelled optional job");
                    }
                    helper.assertTrue(!temporary.contains(pattern), "AppliedE temporary pattern cleanup was bypassed");
                    jobField.set(logic, null);
                }
            } finally {
                jobField.set(logic, null);
            }
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_APPLIEDE_CPU actual_cpu=true actual_optional_patterns=true native_completion_cleanup=true native_cancel_cleanup=true");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Native optional crafting lifecycle check failed", error);
        }
    }
}
