package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.mui.factory.MachineUIFactory;
import com.mojang.logging.LogUtils;
import brachy.modularui.api.IMuiScreen;
import brachy.modularui.widget.WidgetTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Complete production registrations and menus, actual native GUI packets and resource reload in a real client. */
@EventBusSubscriber(modid = "gtl_native_probe", value = Dist.CLIENT)
public final class NativeProductionClientFixtures {
    private static List<MetaMachineBlock> machines;
    private static volatile BlockPos position;
    private static volatile ResourceLocation currentId;
    private static volatile boolean prepared, done;
    private static int nextIndex, completed, ticks, maintenanceChecks, stage;
    private static boolean loading, opened, capturing, clicked, integrationsChecked;
    private static long started;
    private static CompletableFuture<Float> duration;
    private static CompletableFuture<Void> reload;

    private static void prepareNext(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player == null) throw new AssertionError("Native production client has no server player");
        var level = player.serverLevel();
        if (machines == null) {
            player.setInvulnerable(true);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            machines = BuiltInRegistries.BLOCK.stream().filter(MetaMachineBlock.class::isInstance)
                    .map(MetaMachineBlock.class::cast).filter(block -> {
                        var machine = block.getDefinition().getBlockEntityType().create(BlockPos.ZERO, block.defaultBlockState());
                        String name = machine.getClass().getName();
                        return name.startsWith("com.gtladd.") || name.startsWith("org.gtlcore.");
                    }).sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString())).toList();
        }
        position = player.blockPosition().offset(3, 0, 0);
        while (nextIndex < machines.size()) {
            var block = machines.get(nextIndex++);
            level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(position, block.defaultBlockState());
            var machine = (MetaMachine) level.getBlockEntity(position);
            machine.setFrontFacing(Direction.NORTH);
            var hit = new BlockHitResult(position.getCenter(), Direction.NORTH, position, false);
            if (machine.getDefinition().getUI() == null && !(machine instanceof IMuiMachine)) continue;
            if (machine instanceof IMuiMachine ui && !ui.shouldOpenUI(player, InteractionHand.MAIN_HAND, hit)) continue;
            if (machine instanceof AutoConfigurationMaintenanceHatchPartMachine maintenance) maintenance.setDurationMultiplier(1f);
            currentId = BuiltInRegistries.BLOCK.getKey(block);
            prepared = true;
            return;
        }
        level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        done = true;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("gtl.nativeProductionClient")) return;
        var client = Minecraft.getInstance();
        if (started == 0) started = System.nanoTime();
        if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(25))
            throw new AssertionError("Complete production client timed out: stage=" + stage + " machine=" + currentId);
        if (!loading && client.getOverlay() == null && client.screen instanceof TitleScreen) {
            loading = true;
            client.createWorldOpenFlows().openWorld("Native Production Validation", () -> {
                throw new AssertionError("Complete production world failed to load");
            });
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null || capturing) return;
        var server = client.getSingleplayerServer();
        if (!integrationsChecked) {
            if (!NativeProductionJeiProbe.isReady()) return;
            NativeProductionJeiProbe.verifyTranslations();
            NativeProductionJeiProbe.verifyBookmarks();
            CreativeModeTabs.tryRebuildTabContents(client.level.enabledFeatures(), true, client.level.registryAccess());
            NativeProductionRocketGameTests.assertCreativeRocketFuels(client.level.enabledFeatures(), client.level.registryAccess());
            var tab = com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs.INSTANCE.getGTLADD_MACHINE().get();
            int count = tab.getDisplayItems().size();
            // Cross-namespace hatches keep their original GregTech/GTMThings tabs.
            var expected = org.gtlcore.gtlcore.common.data.NativeMachineAssetRegistry.assets().stream()
                    .map(org.gtlcore.gtlcore.common.data.NativeMachineAssetRegistry.Asset::definition)
                    .filter(definition -> definition.getId().getNamespace().equals("gtladditions")).toList();
            if (expected.isEmpty() || count < expected.size())
                throw new AssertionError("Native Additions machine creative tab is incomplete: " + count + " expected=" + expected.size());
            for (var definition : expected) {
                if (!tab.contains(definition.asStack()))
                    throw new AssertionError("Native Additions creative tab is missing " + definition.getId());
            }
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_CREATIVE_TAB additions_machines={} expected={} all_registered_items_present=true", count, expected.size());
            verifyModels(client);
            integrationsChecked = true;
        }
        if (stage == 1) {
            if (!reload.isDone() || client.getOverlay() != null) return;
            reload.join();
            if (++ticks < 20) return;
            NativeProductionJeiProbe.verifyTranslations();
            verifyModels(client);
            if (!Boolean.getBoolean("gtl.nativeProductionFormedOnly") && (completed < 200 || maintenanceChecks == 0))
                throw new AssertionError("Incomplete native production client coverage");
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_CLIENT=PASS menus={} maintenance_click_c2s={} resource_reload=true world_loaded=true formed_only={}",
                    completed, maintenanceChecks, Boolean.getBoolean("gtl.nativeProductionFormedOnly"));
            client.stop();
            stage = 2;
            return;
        }
        if (stage == 2) return;
        if (done || Boolean.getBoolean("gtl.nativeProductionFormedOnly")) {
            if (!NativeProductionFormedClientProbe.tick(client)) return;
            if (!NativeProductionWildcardClientProbe.tick(client)) return;
            client.player.closeContainer();
            reload = client.reloadResourcePacks();
            stage = 1;
            ticks = 0;
            return;
        }
        if (!prepared) {
            prepared = true;
            server.execute(() -> prepareNext(server, client.player.getUUID()));
            return;
        }
        if (!opened && currentId != null && position != null
                && client.level.getBlockEntity(position) instanceof MetaMachine machine
                && machine.getDefinition().getId().equals(currentId)) {
            var blockModel = client.getBlockRenderer().getBlockModel(machine.getBlockState());
            var itemModel = client.getItemRenderer().getModel(new ItemStack(machine.getBlockState().getBlock()), client.level, null, 0);
            if (blockModel == client.getModelManager().getMissingModel()
                    || blockModel.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                throw new AssertionError("Production block model is missing: " + currentId
                        + " baked=" + blockModel.getClass().getName() + " particle=" + blockModel.getParticleIcon().contents().name()
                        + " resources=" + describeResources(client, currentId));
            if (itemModel == client.getModelManager().getMissingModel()
                    || itemModel.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                throw new AssertionError("Production item model is missing: " + currentId);
            opened = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(client.player.getUUID());
                MachineUIFactory.INSTANCE.open(player, position);
            });
            return;
        }
        if (!opened || !(client.screen instanceof IMuiScreen ui) || !ui.screen().getMainPanel().isValid()
                || ui.screen().getContext().getGraphics() == null) return;
        if (++ticks < 12) return;
        for (var panel : ui.screen().getPanelManager().getOpenPanels()) {
            var area = panel.getArea();
            if (area.x < 0 || area.y < 0 || area.x + area.width > client.screen.width || area.y + area.height > client.screen.height)
                throw new AssertionError("Production panel exceeds the scaled screen: " + currentId);
        }
        var decrease = WidgetTree.findFirstWithNameNullable(ui.screen().getMainPanel(), "maintenanceDecreaseButton");
        if (decrease != null && !clicked) {
            clicked = true;
            var area = decrease.getArea();
            double x = area.x + area.width / 2.0, y = area.y + area.height / 2.0;
            var context = ui.screen().getContext();
            context.updateState((int) x, (int) y, 0);
            ui.screen().onFrameUpdate();
            if (context.getTopHovered() != decrease) throw new AssertionError("Production maintenance control is obscured: " + currentId);
            client.screen.afterMouseAction();
            boolean handled = net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPre(client.screen, x, y, 0);
            if (!handled) handled = net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPost(
                    client.screen, x, y, 0, client.screen.mouseClicked(x, y, 0));
            if (!handled) throw new AssertionError("Production maintenance rejected mouse input");
            if (!net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPre(client.screen, x, y, 0))
                net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPost(
                        client.screen, x, y, 0, client.screen.mouseReleased(x, y, 0));
            ticks = 0;
            return;
        }
        if (clicked) {
            if (duration == null) duration = server.submit(() -> ((AutoConfigurationMaintenanceHatchPartMachine)
                    server.getPlayerList().getPlayer(client.player.getUUID()).serverLevel().getBlockEntity(position)).getDurationMultiplier());
            if (!duration.isDone()) return;
            if (Math.abs(duration.join() - .99f) > .00001f)
                throw new AssertionError("Production maintenance click did not reach server: " + currentId + " value=" + duration.join());
            maintenanceChecks++;
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_CLIENT_MENU id={} native_render=true native_sync=true", currentId);
        completed++;
        if (completed == 1 || currentId.getPath().contains("dual_hatch") || currentId.getPath().equals("planetary_ionisation_convergence_tower")
                || currentId.getPath().equals("cloud_data_machine")) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "production-menu-" + currentId.getPath() + ".png", client.getMainRenderTarget(), message -> client.execute(() -> next(client)));
        } else next(client);
    }

    private static void next(Minecraft client) {
        client.player.closeContainer();
        currentId = null;
        prepared = opened = capturing = clicked = false;
        duration = null;
        ticks = 0;
    }

    private static void verifyModels(Minecraft client) {
        int models = 0, states = 0;
        var failures = new java.util.LinkedHashSet<String>();
        for (var asset : org.gtlcore.gtlcore.common.data.NativeMachineAssetRegistry.assets()) {
            var definition = asset.definition();
            boolean blockFailed = false;
            for (var state : definition.getBlock().getStateDefinition().getPossibleStates()) {
                var baked = client.getBlockRenderer().getBlockModel(state);
                if (baked == client.getModelManager().getMissingModel()
                        || baked.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                    if (!blockFailed) LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_MODEL_FAILURE block={} state={} baked={} particle={}",
                            definition.getId(), state, baked.getClass().getName(), baked.getParticleIcon().contents().name());
                    blockFailed = true;
                    failures.add(definition.getId() + "[block]");
                }
                states++;
            }
            var item = client.getItemRenderer().getModel(definition.asStack(), client.level, null, 0);
            if (item == client.getModelManager().getMissingModel()
                    || item.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                failures.add(definition.getId() + "[item]");
                LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_MODEL_FAILURE item={} baked={} particle={}",
                        definition.getId(), item.getClass().getName(), item.getParticleIcon().contents().name());
            }
            models++;
        }
        if (!failures.isEmpty()) throw new AssertionError("Native production model audit failed: " + failures);
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_MODELS machines={} blockstates={} item_models={} native_baked=true", models, states, models);
    }

    private static String describeResources(Minecraft client, ResourceLocation id) {
        var output = new StringBuilder();
        client.getResourceManager().listPacks().filter(pack -> pack.packId().equals("gtlcore:native_machine_assets")).forEach(pack -> {
            for (String path : new String[] {"models", "blockstates"}) {
                var found = new java.util.ArrayList<ResourceLocation>();
                pack.listResources(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, id.getNamespace(), path, (location, input) -> found.add(location));
                output.append("native_pack_").append(path).append(" count=").append(found.size())
                        .append(" examples=").append(found.stream().limit(3).toList());
            }
        });
        for (String path : new String[] {"blockstates/" + id.getPath() + ".json",
                "models/block/machine/" + id.getPath() + ".json", "models/item/" + id.getPath() + ".json"}) {
            var resourceId = id.withPath(path);
            output.append(resourceId).append('=');
            for (var resource : client.getResourceManager().getResourceStack(resourceId)) {
                try (var input = resource.open()) {
                    output.append(resource.sourcePackId()).append(':')
                            .append(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                } catch (java.io.IOException error) {
                    throw new AssertionError("Cannot read native machine resource " + resourceId, error);
                }
            }
        }
        return output.toString();
    }
}
