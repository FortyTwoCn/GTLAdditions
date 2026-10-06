package com.gtladd.gtladditions.migration;

import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Uses only the physical client's world, normal native actions and Ad Astra's public screen API. */
public final class NativeProductionDedicatedRocketClient {
    private static int phase, ticks;

    static boolean tick(Minecraft client) {
        if (phase == 0) {
            if (!client.level.dimension().equals(Level.OVERWORLD) || client.player.position()
                    .distanceToSqr(new net.minecraft.world.phys.Vec3(40.5, 97, 3.5)) > .25) return false;
            var stack = client.player.getInventory().getItem(1);
            if (!BuiltInRocket.isTierSeven(stack) || ++ticks < 20) return false;
            if (!client.player.getInventory().getItem(0).getHoverName().getString().equals(namedItem().getHoverName().getString()))
                throw new AssertionError("Physical player lost component cargo before rocket placement");
            client.player.getInventory().selected = 1;
            var pad = NativeProductionDedicatedRocketServer.PAD;
            if (!client.level.getBlockState(pad).is(earth.terrarium.adastra.common.registry.ModBlocks.LAUNCH_PAD.get()))
                return false;
            var hit = new BlockHitResult(pad.getCenter().add(0, .5, 0), Direction.UP, pad, false);
            // Native RocketItem intentionally returns PASS on the client and places only on the server.
            // Require the normal packet action, then verify the actual synchronized entity/fuel below.
            if (client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.FAIL)
                throw new AssertionError("Physical native rocket item-use action was rejected");
            phase = 1; ticks = 0; return false;
        }
        if (phase == 1) {
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof Rocket rocket && rocket.tier() == 7
                        && rocket.position().distanceToSqr(NativeProductionDedicatedRocketServer.PAD.getCenter()) < 16) {
                    if (++ticks < 10) return false;
                    if (client.gameMode.interact(client.player, rocket, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.FAIL)
                        throw new AssertionError("Physical player could not board the native tier-seven rocket");
                    phase = 2; ticks = 0; return false;
                }
            }
            return false;
        }
        if (phase == 2) {
            if (!(client.player.getVehicle() instanceof Rocket rocket)) return false;
            client.options.keyJump.setDown(true);
            if (!rocket.isLaunching() && !rocket.hasLaunched()) return false;
            client.options.keyJump.setDown(false);
            phase = 3; ticks = 0; return false;
        }
        if (phase == 3) {
            if (!(client.screen instanceof PlanetsScreen planets) || ++ticks < 20) return false;
            // Public native screen action sends the real ResourcefulLib landing request.
            // The survival server must authorize it from the actual rocket's tier/flight state.
            planets.land(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("ad_astra:moon")));
            phase = 4; ticks = 0; return false;
        }
        if (!client.level.dimension().location().equals(ResourceLocation.parse("ad_astra:moon")) || ++ticks < 20) return false;
        if (!(client.player.getVehicle() instanceof Lander)
                || !ItemStack.matches(client.player.getInventory().getItem(0), namedItem().copyWithCount(3))) {
            // A dimension respawn precedes asynchronous native entity tracking and passenger packets.
            if (ticks < 160) return false;
            throw new AssertionError("Physical client did not receive its native lander and component cargo on the Moon: vehicle="
                    + client.player.getVehicle() + ", cargo=" + client.player.getInventory().getItem(0));
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_CLIENT=PASS actual_tcp=true actual_flight=true native_planets_screen=true native_landing_c2s=true actual_dimension_respawn=true native_lander=true inventory_components=true reload={}", reload());
        return true;
    }

    private static final class BuiltInRocket {
        static boolean isTierSeven(ItemStack stack) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .equals(ResourceLocation.parse("ad_astra_rocketed:tier_7_rocket"));
        }
    }
}
