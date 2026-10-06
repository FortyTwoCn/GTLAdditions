package com.gtladd.gtladditions.migration;

import brachy.modularui.api.IMuiScreen;
import brachy.modularui.value.sync.GenericListSyncHandler;
import brachy.modularui.widget.WidgetTree;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.widgets.ToggleButton;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanel;
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import java.util.concurrent.TimeUnit;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** No server access or fixture packet: all observations are from a physically separate native client. */
@EventBusSubscriber(modid = "gtl_native_probe", value = Dist.CLIENT)
public final class NativeProductionDedicatedClient {
    private static long started;
    private static int stage, ticks, heartbeat;
    private static int clickWait;
    private static boolean connecting, capturing;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("gtl.nativeProductionDedicatedClient")) return;
        var client = Minecraft.getInstance();
        if (started == 0) started = System.nanoTime();
        if (++heartbeat % 200 == 0) {
            var host = client.level == null ? null : client.level.getBlockEntity(INPUT);
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_CLIENT_WAIT stage={} screen={} position={} host={} items={} fluids={}",
                    stage, client.screen == null ? "none" : client.screen.getClass().getSimpleName(),
                    client.player == null ? "none" : client.player.position(), host == null ? "none" : host.getClass().getSimpleName(),
                    host instanceof InfinityDualHatchPartMachine m ? items(m).getItemStorage().getLong(namedItem()) : -1,
                    host instanceof InfinityDualHatchPartMachine m ? fluids(m).getFluidStorage().getLong(namedFluid()) : -1);
        }
        if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(10)) throw new AssertionError("Native separate client timed out: stage=" + stage);
        if (client.getOverlay() == null && client.screen instanceof AccessibilityOnboardingScreen) {
            client.screen.onClose();
            return;
        }
        if (!connecting && client.getOverlay() == null && client.screen instanceof TitleScreen) {
            connecting = true;
            String address = "127.0.0.1:" + Integer.getInteger("gtl.nativeProductionDedicatedPort", 25576);
            ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(address),
                    new ServerData("Native Production Dedicated Validation", address, ServerData.Type.OTHER), false, null);
            return;
        }
        if (client.level == null || client.player == null || capturing || stage == 6) return;
        if (client.getSingleplayerServer() != null) throw new AssertionError("Dedicated client unexpectedly shares an integrated server");
        if (stage == 10) {
            if (!client.level.dimension().equals(TRAVEL_DIMENSION)) return;
            if (++ticks < 20) return;
            if (!net.minecraft.world.item.ItemStack.matches(client.player.getInventory().getItem(0), namedItem().copyWithCount(3)))
                throw new AssertionError("Physical client lost its native component cargo after protected wormhole travel");
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_TRAVEL_CLIENT protected_dimension={} real_respawn_s2c=true inventory_components=true reload={}", TRAVEL_DIMENSION.location(), reload());
            next(11); return;
        }
        if (stage == 11) {
            if (NativeProductionDedicatedRocketClient.tick(client)) next(12);
            return;
        }
        if (stage == 12) {
            if (NativeProductionDedicatedElevatorClient.tick(client)) finish(client, reload());
            return;
        }
        if (stage == 13) {
            if (NativeProductionDedicatedWirelessClient.tick(client)) next(10);
            return;
        }
        if (!(client.level.getBlockEntity(INPUT) instanceof InfinityDualHatchPartMachine machine)) return;
        if (++ticks < 20) return;
        if (stage == 0) {
            if (client.player.isPassenger()) return;
            if (!verifySpoilableEntity(client)) return;
            NativeProductionBloomProbe.verify();
            // Native GT keeps inventory server-side; the genuine menu synchronizes its component rows.
            open(client, INPUT); next(7); return;
        }
        if (stage == 7) {
            // A post-login teleport can reject item use until its normal acknowledgement.
            // Retry the same real block action while no screen has opened.
            if (client.screen == null && !client.player.isPassenger() && ticks % 20 == 0) {
                open(client, INPUT); return;
            }
            if (!(client.screen instanceof IMuiScreen ui) || !ready(ui)) return;
            verifyText(ui);
            if (reload()) { verifyIncrementalRows(ui); client.player.closeContainer(); next(9); return; }
            client.player.closeContainer(); next(8); return;
        }
        if (stage == 8) {
            if (client.screen != null) return;
            open(client, MAINTENANCE); next(1); return;
        }
        if (stage == 1) {
            if (!(client.screen instanceof IMuiScreen ui) || !ready(ui)) return;
            var decrease = WidgetTree.findFirstWithNameNullable(ui.screen().getMainPanel(), "maintenanceDecreaseButton");
            if (decrease == null) throw new AssertionError("Native dedicated maintenance control is missing");
            if (!click(client, ui, decrease)) return;
            next(2); return;
        }
        if (stage == 2) {
            var maintenance = (AutoConfigurationMaintenanceHatchPartMachine) client.level.getBlockEntity(MAINTENANCE);
            if (Math.abs(maintenance.getDurationMultiplier() - .99f) > .00001f) return;
            client.player.closeContainer(); next(3); return;
        }
        if (stage == 3) {
            if (client.screen != null) return;
            open(client, INPUT); next(4); return;
        }
        if (stage == 4) {
            if (!(client.screen instanceof IMuiScreen ui) || !ready(ui)) return;
            verifyText(ui);
            if (reload()) { finish(client, true); return; }
            if (!(ui.screen().getMainPanel() instanceof MachineUIPanel panel)) throw new AssertionError("Native production machine panel was replaced");
            var toggles = panel.getRightConfiguratorPanel().getChildren().stream().filter(ToggleButton.class::isInstance).toList();
            if (toggles.size() != 2) throw new AssertionError("Unexpected native power/distinct controls: " + toggles.size());
            if (!click(client, ui, toggles.get(1))) return;
            next(5); return;
        }
        if (stage == 5) {
            if (!(client.screen instanceof IMuiScreen ui) || !ready(ui)) return;
            if (!hasIncrementalRows(ui)) return;
            verifyText(ui); client.player.closeContainer(); next(9);
        }
        if (stage == 9) {
            if (NativeProductionDedicatedAEClient.tick(client)) {
                client.player.closeContainer(); next(13);
            }
        }
    }

    private static boolean verifySpoilableEntity(Minecraft client) {
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity item)
                    || !item.getItem().getHoverName().getString().equals(SPOIL_NAME)) continue;
            var data = item.getItem().get(com.gregtechceu.gtceu.common.data.item.GTDataComponents.SPOILABLE);
            if (data == null || !data.initialized() || !data.frozen() || data.frozenTicks() != LARGE
                    || data.creationTick() != LARGE + 1 || data.spoilContext().level() != null
                    || !SPOIL_POSITION.equals(data.spoilContext().pos()) || data.spoilContext().slot() != 12
                    || data.spoilContext().itemHandlerSource() != com.gregtechceu.gtceu.api.item.component.SpoilContext.ItemHandlerSource.BLOCK_CAPABILITY
                    || data.spoilContext().itemHandlerData() == null
                    || data.spoilContext().itemHandlerData().getLong("native_long") != LARGE)
                throw new AssertionError("Physical native entity-data decoder lost frozen spoilage data or handler metadata");
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_SPOIL_CLIENT actual_entity_data_s2c=true exact_long=true server_owned_world=true handler_metadata=true reload={}", reload());
            return true;
        }
        return false;
    }

    private static void next(int value) {
        stage = value; ticks = 0;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_CLIENT_STAGE stage={} reload={}", stage, reload());
    }
    private static boolean ready(IMuiScreen ui) {
        return ui.screen().getMainPanel().isValid() && ui.screen().getContext().getGraphics() != null;
    }
    private static void open(Minecraft client, BlockPos position) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(position.getCenter(), Direction.NORTH, position, false));
    }
    private static void verifyText(IMuiScreen ui) {
        var handler = ui.screen().getSyncManager().getPanelSyncManager(ui.screen().getMainPanel().getName())
                .findSyncHandlerNullable("infinityContents", 0);
        if (!(handler instanceof GenericListSyncHandler<?, ?> lines)
                || lines.getValue().stream().filter(Component.class::isInstance).map(Component.class::cast)
                .noneMatch(text -> text.getString().contains("dedicated exact named item"))
                || lines.getValue().stream().filter(Component.class::isInstance).map(Component.class::cast)
                // NeoForge FluidType supplies the displayed fluid name; custom components remain server identities.
                .noneMatch(text -> text.getString().contains(namedFluid().getHoverName().getString())))
            throw new AssertionError("Real native menu component rows failed physical server-to-client synchronization: handler="
                    + handler + " rows=" + (handler instanceof GenericListSyncHandler<?, ?> value ? value.getValue() : "none"));
    }
    private static boolean hasIncrementalRows(IMuiScreen ui) {
        var handler = ui.screen().getSyncManager().getPanelSyncManager(ui.screen().getMainPanel().getName())
                .findSyncHandlerNullable("infinityContents", 0);
        if (!(handler instanceof GenericListSyncHandler<?, ?> lines)) return false;
        var text = lines.getValue().stream().filter(Component.class::isInstance).map(Component.class::cast)
                .map(Component::getString).toList();
        return text.stream().anyMatch(value -> value.contains("incremental native menu item") && value.contains("17"))
                && text.stream().anyMatch(value -> value.contains(changedFluid().getHoverName().getString()) && value.contains("23"));
    }
    private static void verifyIncrementalRows(IMuiScreen ui) {
        if (!hasIncrementalRows(ui)) throw new AssertionError("Native disk-reloaded menu lost incrementally synchronized component rows");
    }
    static boolean click(Minecraft client, IMuiScreen ui, IWidget widget) {
        var area = widget.getArea();
        double x = area.x + area.width / 2.0, y = area.y + area.height / 2.0;
        var context = ui.screen().getContext(); context.updateState((int) x, (int) y, 0); ui.screen().onFrameUpdate();
        if (context.getTopHovered() != widget) {
            if (++clickWait > 160) throw new AssertionError("Native dedicated GUI control remains obscured: expected="
                    + widget + ", area=" + area + ", hovered=" + context.getTopHovered());
            return false;
        }
        clickWait = 0;
        client.screen.afterMouseAction();
        boolean handled = ClientHooks.onScreenMouseClickedPre(client.screen, x, y, 0);
        if (!handled) handled = ClientHooks.onScreenMouseClickedPost(client.screen, x, y, 0, client.screen.mouseClicked(x, y, 0));
        if (!handled) throw new AssertionError("Native dedicated GUI did not consume physical click");
        if (!ClientHooks.onScreenMouseReleasedPre(client.screen, x, y, 0))
            ClientHooks.onScreenMouseReleasedPost(client.screen, x, y, 0, client.screen.mouseReleased(x, y, 0));
        return true;
    }
    private static void finish(Minecraft client, boolean fromDisk) {
        stage = 6; capturing = true;
        Screenshot.grab(client.gameDirectory, fromDisk ? "production-dedicated-reloaded.png" : "production-dedicated-input.png",
                client.getMainRenderTarget(), message -> client.execute(() -> {
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_CLIENT{}=PASS physical_client=true no_integrated_server=true server_component_rows=true native_gui=true real_clicks=true incremental=true maintenance_chunk_sync=true ae_config_stock_exact=true",
                            fromDisk ? "_RELOAD" : "");
                    client.player.closeContainer(); client.stop();
                }));
    }
}
