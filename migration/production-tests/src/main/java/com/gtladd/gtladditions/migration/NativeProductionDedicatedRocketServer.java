package com.gtladd.gtladditions.migration;

import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.common.blocks.LaunchPadBlock;
import earth.terrarium.adastra.common.blocks.properties.LaunchPadPartProperty;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.registry.ModBlocks;
import earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.gtlcore.gtlcore.common.data.GTLRocketFuels;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Provides starting resources, then observes the real player's native placement, launch and landing. */
public final class NativeProductionDedicatedRocketServer {
    static final BlockPos PAD = new BlockPos(40, 96, 0);
    private static int phase, delay;
    private static Rocket rocket;

    static boolean completed() { return phase == 3; }

    static void tick(MinecraftServer server) {
        if (phase == 3 || server.getPlayerList().getPlayers().isEmpty()) return;
        var player = server.getPlayerList().getPlayers().getFirst();
        if (phase == 0) {
            // Allow the physical client to verify the preceding Lantea respawn before returning.
            if (++delay < 100) return;
            if (!player.level().dimension().equals(TRAVEL_DIMENSION))
                throw new AssertionError("Rocket journey began before the protected native journey completed");
            player.stopRiding();
            player.teleportTo(server.overworld(), 40.5, 97, 3.5, 180, 0);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.onUpdateAbilities();
            if (server.getPlayerList().isOp(player.getGameProfile()) || player.isCreative() || player.isSpectator())
                throw new AssertionError("Rocket planet permission test has elevated player privileges");
            var level = server.overworld();
            level.setBlockAndUpdate(PAD, ModBlocks.LAUNCH_PAD.get().defaultBlockState()
                    .setValue(LaunchPadBlock.PART, LaunchPadPartProperty.CENTER));
            level.setBlockAndUpdate(new BlockPos(40, 96, 3), Blocks.STONE.defaultBlockState());
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra_rocketed:tier_7_rocket"));
            var stack = new ItemStack(item);
            var storage = new ModifyOnlyContext(stack).find(FluidApi.ITEM);
            if (storage == null || storage.insert(FluidResource.of(GTLRocketFuels.forTier(7)), 16_000, false) != 16_000)
                throw new AssertionError("Actual tier-seven rocket cannot receive its native tier fuel");
            player.getInventory().setItem(1, stack);
            player.inventoryMenu.broadcastChanges();
            phase = 1;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_READY survival=true op=false tier=7 actual_item_fuel=16000 reload={}", reload());
            return;
        }
        if (phase == 1) {
            if (!(player.getVehicle() instanceof Rocket candidate) || candidate.tier() != 7) return;
            if (rocket == null) {
                rocket = candidate;
                if (rocket.fluidContainer().getAmount(0) != 16_000
                        || !rocket.fluidContainer().getResource(0).equals(FluidResource.of(GTLRocketFuels.forTier(7)))
                        || !player.getInventory().getItem(1).isEmpty())
                    throw new AssertionError("Native physical placement or boarding lost the paid rocket fuel/item");
            }
            if (!rocket.isLaunching()) return;
            if (rocket.fluidContainer().getAmount(0) != 0 || rocket.launchTicks() <= 0 || rocket.launchTicks() >= 200)
                throw new AssertionError("Normal native launch did not debit exactly one 16000 mB tank and start its countdown");
            phase = 2;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_LAUNCH actual_item_use_c2s=true actual_entity_interact_c2s=true native_space_key=true actual_countdown=true fuel_paid=16000 reload={}", reload());
            return;
        }
        if (!player.level().dimension().location().equals(ResourceLocation.parse("ad_astra:moon"))) return;
        if (!(player.getVehicle() instanceof Lander) || !rocket.hasLaunched()
                || !ItemStack.matches(player.getInventory().getItem(0), namedItem().copyWithCount(3))
                || player.getTags().contains("spaceelevatorst") || player.getTags().contains("canTeleportToPlanet"))
            throw new AssertionError("Native survival rocket landing lost its vehicle/cargo or used an elevator permit");
        phase = 3;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_SERVER=PASS actual_flight=true native_land_packet=true destination=ad_astra:moon native_lander=true inventory_components=true survival_permission=true reload={}", reload());
    }
}
