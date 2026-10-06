package com.gtladd.gtladditions.migration;

import brachy.modularui.api.IMuiScreen;
import brachy.modularui.widget.WidgetTree;
import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Opens the original machine UI, clicks its real departure control, then sends native planet landing. */
final class NativeProductionDedicatedElevatorClient {
    private static int phase, ticks;

    static boolean tick(Minecraft client) {
        if (phase == 0) {
            var pos = NativeProductionDedicatedElevatorServer.ELEVATOR;
            if (!client.level.dimension().equals(Level.OVERWORLD)
                    || client.player.position().distanceToSqr(pos.getCenter()) > 16
                    || !(client.level.getBlockEntity(pos) instanceof SpaceElevatorMachine machine)
                    || !machine.isFormed() || ++ticks < 10) return false;
            client.player.getInventory().selected = 2; // Empty hand; component cargo remains in slot zero.
            if (client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(pos.getCenter(), machine.getFrontFacing(), pos, false))
                    == net.minecraft.world.InteractionResult.FAIL)
                throw new AssertionError("Actual survival player could not open the formed Space Elevator");
            phase = 1; ticks = 0; return false;
        }
        if (phase == 1) {
            if (!(client.screen instanceof IMuiScreen ui) || ++ticks < 10) return false;
            var button = WidgetTree.findFirstWithNameNullable(ui.screen().getMainPanel(), "spaceElevatorDepart");
            if (button == null) throw new AssertionError("Original departure control is missing from native machine UI");
            if (!button.isEnabled()) return false;
            if (!NativeProductionDedicatedClient.click(client, ui, button)) return false;
            phase = 2; ticks = 0; return false;
        }
        if (phase == 2) {
            if (!(client.screen instanceof PlanetsScreen planets) || ++ticks < 10) return false;
            planets.land(Planet.MARS);
            phase = 3; ticks = 0; return false;
        }
        if (!client.level.dimension().equals(Planet.MARS) || ++ticks < 20) return false;
        if (!(client.player.getVehicle() instanceof Lander)
                || !ItemStack.matches(client.player.getInventory().getItem(0), namedItem().copyWithCount(3))) {
            if (ticks < 160) return false;
            throw new AssertionError("Physical client did not receive native Mars lander/component cargo");
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ELEVATOR_CLIENT=PASS actual_tcp=true real_gui_click=true native_planets_screen=true native_landing_c2s=true actual_dimension_respawn=true native_lander=true inventory_components=true reload={}", reload());
        return true;
    }
}
