package com.gtladd.gtladditions.migration;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.gtlcore.gtlcore.integration.ae2.WirelessTerminalGridResolver;
import org.gtlcore.gtlcore.integration.ae2.wireless.GTLWirelessAeContent;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Real finite-powered AE network, linked held terminals and production menu/data packets. */
final class NativeProductionDedicatedWirelessServer {
    static final BlockPos ACCESS_POINT = new BlockPos(3, 96, 8);
    private static int phase, terminal, ticks;
    private static WirelessAccessPointBlockEntity accessPoint;
    private static EnergyCellBlockEntity energy;
    private static DriveBlockEntity drive;

    static boolean tick(MinecraftServer server) {
        if (phase == 4) return true;
        var player = server.getPlayerList().getPlayers().getFirst();
        if (phase == 0) {
            var level = server.overworld();
            level.setBlockAndUpdate(ACCESS_POINT, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ACCESS_POINT.below(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ACCESS_POINT.below().west(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ACCESS_POINT, AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(8, 95, 10), Blocks.STONE.defaultBlockState());
            accessPoint = (WirelessAccessPointBlockEntity) level.getBlockEntity(ACCESS_POINT);
            // Native access points expose only their back, rather than all six faces.
            var back = accessPoint.getGridConnectableSides(accessPoint.getOrientation()).iterator().next();
            var energyPos = ACCESS_POINT.relative(back);
            var drivePos = energyPos.west();
            level.setBlockAndUpdate(energyPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(drivePos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(energyPos, AEBlocks.DENSE_ENERGY_CELL.block().defaultBlockState());
            level.setBlockAndUpdate(drivePos, AEBlocks.DRIVE.block().defaultBlockState());
            energy = (EnergyCellBlockEntity) level.getBlockEntity(energyPos);
            drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
            drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_1K.stack());
            energy.injectAEPower(100_000, Actionable.MODULATE);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL); player.onUpdateAbilities();
            phase = 1; return false;
        }
        if (phase == 1) {
            if (++ticks > 200)
                throw new AssertionError("Native wireless fixture did not become ready: access_point="
                        + accessPoint.isActive() + ", drive=" + drive.isPowered()
                        + ", energy=" + energy.getAECurrentPower());
            if (!accessPoint.isActive() || !drive.isPowered() || accessPoint.getGrid() == null || ticks < 40) return false;
            if (drive.getMainNode().getGrid() != accessPoint.getGrid() || energy.getMainNode().getGrid() != accessPoint.getGrid()
                    || energy.getAECurrentPower() <= 0 || player.isCreative() || player.isSpectator()
                    || server.getPlayerList().isOp(player.getGameProfile()))
                throw new AssertionError("Actual wireless terminal fixture is not a finite-powered native survival grid");
            provide(player); phase = 2; ticks = 0;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_READY native_access_point=true real_grid=true finite_energy=true real_storage_cell=true survival=true reload={}", reload());
            return false;
        }
        if (phase == 2) {
            if (player.containerMenu == player.inventoryMenu || player.containerMenu.getType() != menuType(terminal)) return false;
            var connection = WirelessTerminalGridResolver.findConnection(player, player.level());
            if (connection == null || connection.grid() != accessPoint.getGrid() || !connection.host().getLinkStatus().connected()
                    || connection.terminalStack() != player.getMainHandItem())
                throw new AssertionError("Actual held native wireless menu cannot resolve its powered linked grid");
            phase = 3; ticks = 0;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_MENU terminal={} real_item_use_c2s=true native_menu=true held_stack=true linked_grid=true reload={}", terminal, reload());
        }
        if (phase != 3) return false;
        if (terminal == 0 && ticks == 40) {
            var menu = (org.gtlcore.gtlcore.integration.ae2.throughput.ThroughputMonitorTerminalMenu) player.containerMenu;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_SYNC_DIAGNOSTIC menu_valid={} player_tick={} changing_dimension={} entries={} reload={}",
                    menu.isValidMenu(), player.tickCount, player.isChangingDimension(), menu.getEntries().size(), reload());
        }
        if (terminal == 0 && ticks > 400)
            throw new AssertionError("Native wireless monitor did not receive real-grid throughput within 400 ticks: player_tick="
                    + player.tickCount + ", changing_dimension=" + player.isChangingDimension());
        if (terminal == 0 && ++ticks % 10 == 0 && player.containerMenu != player.inventoryMenu) {
            var inventory = accessPoint.getGrid().getStorageService().getInventory();
            var key = AEItemKey.of(namedItem());
            var source = IActionSource.ofPlayer(player);
            if (inventory.insert(key, 17, Actionable.MODULATE, source) != 17
                    || inventory.extract(key, 17, Actionable.MODULATE, source) != 17)
                throw new AssertionError("Actual native drive did not execute component-preserving wireless monitor transactions");
        }
        if (player.containerMenu != player.inventoryMenu) return false;
        terminal++;
        if (terminal < 3) { provide(player); phase = 2; ticks = 0; return false; }
        phase = 4;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WIRELESS_SERVER=PASS native_terminal_types=3 held_item=true survival=true native_grid=true component_storage_transactions=true native_open_and_close_c2s=true reload={}", reload());
        return true;
    }

    private static void provide(net.minecraft.server.level.ServerPlayer player) {
        var item = item(terminal);
        var stack = new ItemStack(item);
        WirelessTerminalItem.LINKABLE_HANDLER.link(stack, GlobalPos.of(Level.OVERWORLD, ACCESS_POINT));
        item.injectAEPower(stack, 1600, Actionable.MODULATE);
        if (item.getAECurrentPower(stack) <= 0 || !GlobalPos.of(Level.OVERWORLD, ACCESS_POINT).equals(item.getLinkedPosition(stack)))
            throw new AssertionError("Native terminal link/power components were not installed");
        player.getInventory().setItem(2, stack);
        player.inventoryMenu.broadcastChanges();
    }

    static WirelessTerminalItem item(int index) {
        return switch (index) {
            case 0 -> GTLWirelessAeContent.WIRELESS_THROUGHPUT_MONITOR_TERMINAL.get();
            case 1 -> GTLWirelessAeContent.WIRELESS_EMITTER_MANAGER_TERMINAL.get();
            case 2 -> GTLWirelessAeContent.WIRELESS_ME_CHAMBER_MANAGER_TERMINAL.get();
            default -> throw new IllegalArgumentException("Unknown native wireless fixture terminal");
        };
    }

    static net.minecraft.world.inventory.MenuType<?> menuType(int index) {
        return switch (index) {
            case 0 -> GTLWirelessAeContent.WIRELESS_THROUGHPUT_MONITOR_TERMINAL_MENU.get();
            case 1 -> GTLWirelessAeContent.WIRELESS_EMITTER_MANAGER_TERMINAL_MENU.get();
            case 2 -> GTLWirelessAeContent.WIRELESS_ME_CHAMBER_MANAGER_TERMINAL_MENU.get();
            default -> throw new IllegalArgumentException("Unknown native wireless fixture menu");
        };
    }
}
