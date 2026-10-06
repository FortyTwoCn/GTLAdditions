package com.gtladd.gtladditions.migration;

import appeng.api.stacks.AEItemKey;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.gtlcore.gtlcore.integration.ae2.throughput.ThroughputMonitorTerminalMenu;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Uses the physical inventory and native item-use action; reads only real client menu packets. */
final class NativeProductionDedicatedWirelessClient {
    private static int phase, terminal, ticks;

    static boolean tick(Minecraft client) {
        if (phase == 0) {
            if (client.screen != null || !client.player.getInventory().getItem(2).is(NativeProductionDedicatedWirelessServer.item(terminal))
                    || ++ticks < 10) return false;
            client.player.getInventory().selected = 2;
            var stack = client.player.getMainHandItem();
            if (NativeProductionDedicatedWirelessServer.item(terminal).getLinkedPosition(stack) == null
                    || NativeProductionDedicatedWirelessServer.item(terminal).getAECurrentPower(stack) <= 0)
                throw new AssertionError("Physical client did not receive native terminal link/power components");
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            phase = 1; ticks = 0; return false;
        }
        if (client.player.containerMenu == client.player.inventoryMenu
                || client.player.containerMenu.getType() != NativeProductionDedicatedWirelessServer.menuType(terminal)
                || client.screen == null || ++ticks < 30) return false;
        if (terminal == 0) {
            var menu = (ThroughputMonitorTerminalMenu) client.player.containerMenu;
            var key = AEItemKey.of(namedItem());
            if (menu.getEntries().stream().noneMatch(entry -> key.equals(entry.key())
                    && entry.insertedPerSecond() > 0 && entry.extractedPerSecond() > 0)) return false;
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_CLIENT terminal={} actual_held_use_c2s=true native_menu_and_screen=true link_power_components_s2c=true real_throughput_component_rows={} reload={}", terminal, terminal == 0, reload());
        client.player.closeContainer();
        terminal++; phase = 0; ticks = 0;
        if (terminal < 3) return false;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_CLIENT=PASS terminal_types=3 actual_tcp=true native_item_use=true component_throughput_s2c=true reload={}", reload());
        return true;
    }
}
