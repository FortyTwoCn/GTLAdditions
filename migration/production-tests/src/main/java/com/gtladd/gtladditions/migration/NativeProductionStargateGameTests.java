package com.gtladd.gtladditions.migration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.Dialing;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.Wormhole;

/** Complete production SG integration: original assembly, native connection/event/simulation and real dimension transfer. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionStargateGameTests {
    @GameTest(template = "empty", batch = "gtl_production_stargates", timeoutTicks = 400)
    public static void actualNativeGatesConnectWithoutEnergyAndKeepComponentItemsAcrossDimensions(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var destination = server.getLevel(Level.NETHER);
        var origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 8, 96,
                helper.absolutePos(BlockPos.ZERO).getZ() + 8);
        var target = new BlockPos(24, 96, 24);
        var first = NativeProductionStargates.assemble(level, origin, FakePlayerFactory.getMinecraft(level));
        var second = NativeProductionStargates.assemble(destination, target, FakePlayerFactory.getMinecraft(destination));
        var network = StargateNetwork.get(server);
        int before = network.getConnections().size();
        var simulated = network.createConnection(first.getStargate(), second.getStargate(), Address.Type.ADDRESS_9_CHEVRON, false, Dialing.Action.SIMULATE);
        helper.assertTrue(simulated.feedback().isConnectionEstablished() && network.getConnections().size() == before
                        && !first.isConnected() && !second.isConnected(), "Actual native connection simulation changed the network or required disabled energy: " + simulated);
        var connected = network.createConnection(first.getStargate(), second.getStargate(), Address.Type.ADDRESS_9_CHEVRON, false, Dialing.Action.EXECUTE);
        helper.assertTrue(connected.feedback().isConnectionEstablished() && network.getConnections().size() == before + 1
                        && first.isConnected() && second.isConnected(), "Actual GTL native stargates failed connection: " + connected);
        var connection = network.getConnection(first.getConnectionID());
        helper.assertTrue(connection != null && first.getConnectionID().equals(second.getConnectionID()), "Native gates did not share their real connection");
        var reloaded = StargateConnection.deserialize(server, connection.getID(), connection.serialize());
        helper.assertTrue(reloaded != null && reloaded.getDialingStargate().getDimension().equals(level.dimension())
                        && reloaded.getDialedStargates().getFirst().getDimension().equals(destination.dimension()),
                "Native connection persistence lost real cross-dimension endpoints");
        var stack = new ItemStack(Items.DIAMOND, 7);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("native cross-dimension SG component item"));
        var entity = new ItemEntity(level, origin.getX(), origin.getY() + 3, origin.getZ(), stack.copy());
        level.addFreshEntity(entity);
        var transferred = new Wormhole().receiveTraveler(connection, destination, second.getStargate(), entity,
                target.getCenter().add(0, 3, -2), Vec3.ZERO, new Vec3(0, 0, 1));
        helper.assertTrue(transferred instanceof ItemEntity item && item.level() == destination && entity.isRemoved()
                        && ItemStack.matches(item.getItem(), stack), "Native wormhole dimension transition lost or duplicated owned component items");
        transferred.discard();
        connection.terminate(server, StargateInfo.Feedback.NONE.withInfo());
        helper.assertTrue(!network.hasConnection(connection.getID()) && !first.isConnected() && !second.isConnected(),
                "Native connection termination left either endpoint connected");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STARGATE original_classic_assembly=true actual_native_network=true real_simulation=true no_energy=true connection_persistence=true actual_item_dimension_transition=true components=true native_termination=true");
        first.removeStargateFromNetwork(); second.removeStargateFromNetwork();
        helper.succeed();
    }
}
