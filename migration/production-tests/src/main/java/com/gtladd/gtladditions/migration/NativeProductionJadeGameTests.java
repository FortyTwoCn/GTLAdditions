package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gregtechceu.gtceu.integration.jade.provider.ParallelProvider;
import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Native Jade fallback and the Additions long/thread extension must coexist. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionJadeGameTests {
    private static BlockAccessor accessor(GameTestHelper helper, MetaMachine machine, CompoundTag data) {
        return (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(), new Class<?>[]{BlockAccessor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> machine;
                    case "getPlayer" -> FakePlayerFactory.getMinecraft(helper.getLevel());
                    case "getServerData" -> data;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static List<Component> tooltip(ParallelProvider provider, BlockAccessor accessor) {
        var rows = new ArrayList<Component>();
        var tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(), new Class<?>[]{ITooltip.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("add") && args.length == 1 && args[0] instanceof Component row) {
                        rows.add(row);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        provider.appendTooltip(tooltip, accessor, null);
        return rows;
    }

    @GameTest(template = "empty", batch = "gtl_production_jade", timeoutTicks = 100)
    public static void nativeBatchAndSubtickTooltipsRemainAvailableWithAdditionsParallelExtension(GameTestHelper helper) {
        helper.assertTrue(!new org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProvider().getUid().equals(
                        new com.gregtechceu.gtceu.integration.jade.provider.MEPatternBufferProvider().getUid()),
                "Core and native GT pattern buffer Jade providers collide");
        helper.assertTrue(!new org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProxyProvider().getUid().equals(
                        new com.gregtechceu.gtceu.integration.jade.provider.MEPatternBufferProxyProvider().getUid()),
                "Core and native GT pattern proxy Jade providers collide");
        var position = new BlockPos(3, 4, 3);
        helper.setBlock(position, GCYMMachines.PARALLEL_HATCH[GTValues.IV].getBlock());
        var machine = (MetaMachine) helper.getBlockEntity(position);
        var provider = new ParallelProvider();
        var serverData = new CompoundTag();
        var accessor = accessor(helper, machine, serverData);
        provider.appendServerData(serverData, accessor);
        var data = serverData.getCompound(provider.getUid().toString());
        helper.assertTrue(data.get("parallel") instanceof IntTag && !data.getBoolean("gtladditions:parallel_display"),
                "Additions replaced the ordinary native parallel hatch provider");

        // Use the native provider's exact-run payload to exercise all three distinct tooltip rows.
        data.putInt("parallel", 2);
        data.putInt("batch", 3);
        data.putInt("subtickParallel", 4);
        data.putBoolean("exact", true);
        var keys = tooltip(provider, accessor).stream()
                .map(row -> ((TranslatableContents) row.getContents()).getKey()).toList();
        helper.assertTrue(keys.equals(List.of("gtceu.multiblock.total_runs", "gtceu.multiblock.parallel.exact",
                "gtceu.multiblock.batch_enabled", "gtceu.multiblock.subtick_parallels")),
                "Native batch/subtick tooltip rows were lost: " + keys);

        helper.setBlock(position, GTLAddMachines.INSTANCE.getSUPER_PARALLEL_HATCH().getBlock());
        machine = (MetaMachine) helper.getBlockEntity(position);
        serverData = new CompoundTag();
        accessor = accessor(helper, machine, serverData);
        provider.appendServerData(serverData, accessor);
        data = serverData.getCompound(provider.getUid().toString());
        helper.assertTrue(data.get("parallel") instanceof LongTag && data.getLong("parallel") == Integer.MAX_VALUE
                && data.getBoolean("gtladditions:parallel_display"), "Production super parallel hatch lost its extension");
        helper.assertTrue(tooltip(provider, accessor).size() == 1, "Production super parallel tooltip did not render");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_JADE native_fallback=true batch=true subtick=true super_parallel=true");
        helper.succeed();
    }
}
