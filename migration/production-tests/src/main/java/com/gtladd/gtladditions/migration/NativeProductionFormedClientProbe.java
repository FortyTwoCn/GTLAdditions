package com.gtladd.gtladditions.migration;

import brachy.modularui.api.IMuiScreen;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gtlcore.gtlcore.api.gui.preview.NativePreviewShapes;
import org.gtlcore.gtlcore.common.machine.multiblock.SimulationMachine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Original structures and real bound parts; every menu opens through the ordinary client interaction. */
final class NativeProductionFormedClientProbe {
    private static final BlockPos ORIGIN = new BlockPos(8192, 96, 8192);
    private record Target(BlockPos position, ResourceLocation id, boolean controller, Vec3 arrival, BlockHitResult hit,
                          ResourceLocation coilTexture) {}
    private static List<MetaMachineBlock> definitions;
    private static final List<BlockPos> placed = new ArrayList<>();
    private static final ArrayDeque<Target> targets = new ArrayDeque<>();
    private static final Set<ResourceLocation> boundDefinitions = new HashSet<>();
    private static CompletableFuture<Target> preparation;
    private static Target target;
    private static int nextDefinition, ticks, controllers, parts, waitingTicks, coilMenus;
    private static boolean opened, capturing, done;

    static boolean tick(Minecraft client) {
        if (done) return true;
        if (capturing) return false;
        if (preparation == null && target == null) {
            UUID player = client.player.getUUID();
            preparation = client.getSingleplayerServer().submit(() -> prepareNext(client.getSingleplayerServer(), player));
            return false;
        }
        if (preparation != null) {
            if (!preparation.isDone()) return false;
            target = preparation.join(); preparation = null;
            if (target == null) {
                if (controllers != 148 || parts == 0 || coilMenus == 0)
                    throw new AssertionError("Incomplete real formed controller/bound-part GUI coverage: " + controllers + "/" + parts);
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_FORMED_CLIENT=PASS controllers={} bound_part_definitions={} coil_texture_menus={} original_patterns=true actual_block_use_c2s=true formed_and_bound_s2c=true native_render=true", controllers, parts, coilMenus);
                done = true; return true;
            }
        }
        if (++waitingTicks > 600) throw new AssertionError("Native formed GUI stalled before completion: " + target
                + " opened=" + opened + " player=" + client.player.position());
        if (!(client.level.getBlockEntity(target.position()) instanceof MetaMachine machine)
                || !machine.getDefinition().getId().equals(target.id())) return false;
        if (!(client.level.getBlockEntity(ORIGIN) instanceof MultiblockControllerMachine controller) || !controller.isFormed()) return false;
        if (!target.controller() && (!(machine instanceof MultiblockPartMachine part) || !part.hasController(ORIGIN))) return false;
        if (target.coilTexture() != null) {
            var field = com.gregtechceu.gtceu.api.sync_system.ClassSyncData.getClassData(controller.getClass())
                    .getClientSyncFields().get("gtlcore$coilTexture");
            if (field == null || field.transformer == null) throw new AssertionError("Native coil texture sync codec is missing");
            if (!target.coilTexture().equals(field.handle.get(controller))) return false;
        }
        if (!opened) {
            // Adjacent parts can be in reach before the new teleport/ack finishes.
            // Native ServerGamePacketListener deliberately rejects use packets during teleportation.
            // Ordinary client collision settling can move the player slightly after teleportation.
            // Keep a bounded arrival check and verify reach/visibility from the actual settled eye.
            if (client.player.position().distanceToSqr(target.arrival()) > .25) return false;
            if (++ticks < 20) return false;
            var eye = client.player.getEyePosition();
            if (eye.distanceToSqr(target.hit().getLocation()) > 4.25 * 4.25) return false;
            // Clip into the block: ending exactly on the face can round to the neighboring air block.
            var face = target.hit().getDirection();
            var inside = target.hit().getLocation().add(-face.getStepX() * .02,
                    -face.getStepY() * .02, -face.getStepZ() * .02);
            var visible = client.level.clip(new ClipContext(eye, inside,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            if (!visible.getBlockPos().equals(target.position())) return false;
            client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, target.hit());
            opened = true; ticks = 0; return false;
        }
        if (!(client.screen instanceof IMuiScreen ui)) {
            if (++ticks > 80) throw new AssertionError("Real formed right-click did not open native GUI: " + target
                    + " player=" + client.player.position() + " screen=" + client.screen);
            return false;
        }
        if (!ui.screen().getMainPanel().isValid() || ui.screen().getContext().getGraphics() == null || ++ticks < 16) return false;
        for (var panel : ui.screen().getPanelManager().getOpenPanels()) {
            var area = panel.getArea();
            if (area.x < 0 || area.y < 0 || area.x + area.width > client.screen.width || area.y + area.height > client.screen.height)
                throw new AssertionError("Real formed native GUI exceeds scaled window: " + target.id());
        }
        if (target.controller()) { controllers++; if (target.coilTexture() != null) coilMenus++; } else parts++;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_FORMED_MENU id={} controller={} formed=true bound={} actual_block_use_c2s=true native_render=true native_sync=true coil_texture_s2c={}",
                target.id(), target.controller(), !target.controller(), target.coilTexture() != null);
        if (target.controller() && (target.id().getPath().equals("steam_piston_hammer")
                || target.id().getPath().equals("arcanic_astrograph")
                || target.id().getPath().equals("planetary_ionisation_convergence_tower"))) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "production-formed-" + target.id().getPath() + ".png",
                    client.getMainRenderTarget(), message -> client.execute(() -> next(client)));
        } else next(client);
        return false;
    }

    private static Target prepareNext(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerList().getPlayer(uuid);
        if (player == null) throw new AssertionError("Real formed GUI fixture has no server player");
        var level = player.serverLevel();
        if (definitions == null) {
            player.setInvulnerable(true);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            definitions = BuiltInRegistries.BLOCK.stream().filter(MetaMachineBlock.class::isInstance)
                    .map(MetaMachineBlock.class::cast).filter(block -> block.getDefinition() instanceof MultiblockMachineDefinition)
                    .filter(block -> {
                        var entity = block.getDefinition().getBlockEntityType().create(BlockPos.ZERO, block.defaultBlockState());
                        String name = entity.getClass().getName();
                        return name.startsWith("org.gtlcore.") || name.startsWith("com.gtladd.");
                    }).sorted(Comparator.comparing(block -> block.getDefinition().getId().toString())).toList();
            // This arena belongs to the probe. A previous interrupted run may have saved a larger shape here.
            var ownedPositions = new HashSet<BlockPos>();
            for (var block : definitions) {
                var definition = (MultiblockMachineDefinition) block.getDefinition();
                var front = definition.getRotationState().defaultDirection;
                var up = front.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
                for (var position : NativePreviewShapes.get(definition).getFirst().oriented(front, up, false).states().keySet())
                    ownedPositions.add(ORIGIN.offset(position));
            }
            level.setBlockAndUpdate(ORIGIN, Blocks.AIR.defaultBlockState());
            for (var position : ownedPositions)
                if (!level.getBlockState(position).isAir()) level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
        }
        while (targets.isEmpty()) {
            // Remove the controller first, allowing the actual native part lifecycle to unbind.
            level.setBlockAndUpdate(ORIGIN, Blocks.AIR.defaultBlockState());
            for (var position : placed) if (!position.equals(ORIGIN)) level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
            placed.clear();
            if (nextDefinition == definitions.size()) return null;
            var definition = (MultiblockMachineDefinition) definitions.get(nextDefinition++).getDefinition();
            var front = definition.getRotationState().defaultDirection;
            var up = front.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
            var geometry = NativePreviewShapes.get(definition).getFirst().oriented(front, up, false);
            int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
            for (var position : geometry.states().keySet()) {
                minX = Math.min(minX, position.getX()); minY = Math.min(minY, position.getY()); minZ = Math.min(minZ, position.getZ());
                maxX = Math.max(maxX, position.getX()); maxY = Math.max(maxY, position.getY()); maxZ = Math.max(maxZ, position.getZ());
            }
            // Include required air and ordinary player standing space, not only the sparse occupied preview map.
            for (var position : BlockPos.betweenClosed(ORIGIN.offset(minX - 4, minY - 4, minZ - 4),
                    ORIGIN.offset(maxX + 4, maxY + 3, maxZ + 4)))
                if (!level.getBlockState(position).isAir()) level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
            for (var entry : geometry.states().entrySet()) {
                if (entry.getKey().equals(BlockPos.ZERO)) continue;
                var position = ORIGIN.offset(entry.getKey());
                if (!level.setBlockAndUpdate(position, entry.getValue()) && !level.getBlockState(position).equals(entry.getValue()))
                    throw new AssertionError("Cannot place real formed GUI structure: " + definition.getId() + " at " + position);
                placed.add(position);
            }
            level.setBlockAndUpdate(ORIGIN, geometry.states().get(BlockPos.ZERO)); placed.add(ORIGIN);
            var machine = (MultiblockControllerMachine) level.getBlockEntity(ORIGIN);
            machine.checkAndFormStructure();
            if (machine instanceof SimulationMachine) {
                if (machine.isFormed()) throw new AssertionError("Virtual simulator formed a physical GUI fixture");
                continue;
            }
            if (!machine.isFormed()) {
                for (var error : machine.getDefaultPatternState().getErrors()) {
                    LogUtils.getLogger().error("GTL_NATIVE_PRODUCTION_FORMED_PATTERN_ERROR id={} relative={} actual={} data={}",
                            definition.getId(), error.getPos().subtract(ORIGIN), level.getBlockState(error.getPos()),
                            com.gregtechceu.gtceu.api.multiblock.error.PatternError.CODEC
                                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, error).getOrThrow());
                }
                throw new AssertionError("Real GUI structure did not form: " + definition.getId());
            }
            if (machine instanceof IControllable controllable) controllable.setWorkingEnabled(false);
            targets.add(new Target(ORIGIN, definition.getId(), true, null, null, null));
            for (var part : machine.getParts().stream().sorted(Comparator.comparing(value -> value.getDefinition().getId().toString())).toList()) {
                var hit = new BlockHitResult(part.getBlockPos().getCenter(), Direction.NORTH, part.getBlockPos(), false);
                if (part.getDefinition().getUI() == null && !(part instanceof IMuiMachine)) continue;
                if (part instanceof IMuiMachine ui && !ui.shouldOpenUI(player, InteractionHand.MAIN_HAND, hit)) continue;
                if (!part.hasController(ORIGIN)) throw new AssertionError("Real GUI fixture part failed to bind");
                if (boundDefinitions.add(part.getDefinition().getId())) targets.add(new Target(part.getBlockPos(), part.getDefinition().getId(), false, null, null, null));
            }
        }
        var result = targets.removeFirst();
        // Search all sides, including underneath vertical controllers. Require ordinary collision and reach checks.
        var positions = new ArrayList<Vec3>();
        var targetMachine = (MetaMachine) level.getBlockEntity(result.position());
        var front = targetMachine.getFrontFacing();
        positions.add(result.position().getCenter().add(front.getStepX() * 2.5,
                front.getStepY() * 2.5 - player.getEyeHeight(), front.getStepZ() * 2.5));
        for (double dy : new double[]{-.4, .6, -1.4, -2.4, 1.6, -3.4, 2.6})
            for (double dx = -3; dx <= 3; dx += .75)
                for (double dz = -3; dz <= 3; dz += .75)
                    positions.add(result.position().getCenter().add(dx, dy, dz));
        for (var arrival : positions) {
            // noCollision intentionally treats an unloaded neighboring chunk as a wall.
            // Load the complete candidate box before asking the ordinary collision solver.
            level.getChunk(BlockPos.containing(arrival.add(-.4, 0, -.4)));
            level.getChunk(BlockPos.containing(arrival.add(-.4, 0, .4)));
            level.getChunk(BlockPos.containing(arrival.add(.4, 0, -.4)));
            level.getChunk(BlockPos.containing(arrival.add(.4, 0, .4)));
            if (!level.noCollision(player, player.getBoundingBox().move(arrival.subtract(player.position())))) continue;
            var eye = arrival.add(0, player.getEyeHeight(), 0);
            for (var side : Direction.values()) {
                var insideFace = result.position().getCenter().add(side.getStepX() * .49,
                        side.getStepY() * .49, side.getStepZ() * .49);
                if (eye.distanceToSqr(insideFace) > 4.25 * 4.25) continue;
                var hit = level.clip(new ClipContext(eye, insideFace, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (!hit.getBlockPos().equals(result.position())) continue;
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.teleportTo(arrival.x, arrival.y, arrival.z);
                var controller = (MultiblockControllerMachine) level.getBlockEntity(ORIGIN);
                var texture = controller instanceof com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine coil
                        ? coil.getCoilType().getTexture() : null;
                return new Target(result.position(), result.id(), result.controller(), arrival, hit, texture);
            }
        }
        throw new AssertionError("No collision-free, visible player position in native interaction reach: " + result
                + " front=" + front);
    }

    private static void next(Minecraft client) {
        client.player.closeContainer();
        target = null; opened = capturing = false; ticks = waitingTicks = 0;
    }
    private NativeProductionFormedClientProbe() {}
}
