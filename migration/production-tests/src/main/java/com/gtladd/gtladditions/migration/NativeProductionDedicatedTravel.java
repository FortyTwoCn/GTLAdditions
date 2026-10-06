package com.gtladd.gtladditions.migration;

import com.gtladd.gtladditions.integration.sgjourney.StargateIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.Dialing;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.Wormhole;

import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** A connected physical player's native dimension transition, including the real restricted-dimension event. */
final class NativeProductionDedicatedTravel {
    static void verifyDeniedEntry(ServerPlayer player) {
        var target = player.server.getLevel(TRAVEL_DIMENSION);
        if (target == null) throw new AssertionError("Native protected SG destination is not loaded");
        if (player.getTags().contains(StargateIntegration.VALID_TAG)) throw new AssertionError("Player already has a stale Stargate transfer permit");
        player.teleportTo(target, 8.5, 99, 6.5, player.getYRot(), player.getXRot());
        if (player.serverLevel() != player.server.overworld())
            throw new AssertionError("The actual production dimension event accepted entry without a native wormhole permit");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_TRAVEL unauthorized_entry_rejected=true physical_player=true");
    }

    static void transfer(ServerPlayer player) {
        var target = player.server.getLevel(TRAVEL_DIMENSION);
        var first = NativeProductionStargates.assemble(player.server.overworld(), new BlockPos(16, 96, 8), player);
        var second = NativeProductionStargates.assemble(target, new BlockPos(8, 96, 8), FakePlayerFactory.getMinecraft(target));
        var network = StargateNetwork.get(player.server);
        var connected = network.createConnection(first.getStargate(), second.getStargate(), Address.Type.ADDRESS_9_CHEVRON, false, Dialing.Action.EXECUTE);
        if (!connected.feedback().isConnectionEstablished()) throw new AssertionError("Native physical-player gate connection failed: " + connected);
        var connection = network.getConnection(first.getConnectionID());
        var cargo = namedItem().copyWithCount(3);
        player.getInventory().setItem(0, cargo.copy());
        player.getInventory().setChanged();
        var received = new Wormhole().receiveTraveler(connection, target, second.getStargate(), player,
                second.getBlockPos().getCenter().add(0, 3, -2), Vec3.ZERO, new Vec3(0, 0, 1));
        if (received != player || player.serverLevel() != target || player.getTags().contains(StargateIntegration.VALID_TAG)
                || !ItemStack.matches(player.getInventory().getItem(0), cargo))
            throw new AssertionError("Actual protected native wormhole travel failed, leaked its permit or lost cargo components");
        connection.terminate(player.server, StargateInfo.Feedback.NONE.withInfo());
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_TRAVEL authorized_native_wormhole=true physical_player=true protected_dimension={} permit_consumed=true inventory_components=true", TRAVEL_DIMENSION.location());
    }
    private NativeProductionDedicatedTravel() {}
}
