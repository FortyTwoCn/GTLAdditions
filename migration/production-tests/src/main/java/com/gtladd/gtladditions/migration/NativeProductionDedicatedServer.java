package com.gtladd.gtladditions.migration;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Observes only real server block entities and normal production GUI packets. */
@EventBusSubscriber(modid = "gtl_native_probe")
public final class NativeProductionDedicatedServer {
    private static int stage;
    private static boolean prepareActorAfterLogin;
    private static int savedVehicleLandingTicks;
    private static int travelTicks;
    private static final java.util.Set<net.minecraft.world.inventory.AbstractContainerMenu> reloadMenus =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final boolean[] maximumObserved = new boolean[2], decrementedObserved = new boolean[2];
    private static boolean active() { return Boolean.getBoolean("gtl.nativeProductionDedicatedServer"); }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!active()) return;
        var level = event.getServer().overworld();
        if (reload()) {
            var machine = (InfinityDualHatchPartMachine) level.getBlockEntity(INPUT);
            var maintenance = (AutoConfigurationMaintenanceHatchPartMachine) level.getBlockEntity(MAINTENANCE);
            if (machine == null || !balancesMatch(machine, LARGE + 124, LARGE + 224) || !machine.isDistinct()
                    || !machine.getRecipeHandlers().getFirst().isDistinct()
                    || Math.abs(maintenance.getDurationMultiplier() - .99f) > .00001f
                    || machine.getTraits(CatalystItemStackHandler.class).getFirst().storage.getStackInSlot(8).getCount() != 19)
                throw new AssertionError("Actual dedicated world restart lost quantities, components, distinct or shared catalysts");
            for (int i = 0; i < 2; i++) {
                var ae = (MetaMachine) level.getBlockEntity(i == 0 ? AE_ITEM : AE_FLUID);
                if (ae == null || !aeMatches(ae, i == 1, Long.MAX_VALUE - 1))
                    throw new AssertionError("Actual dedicated world restart lost exact AE configuration, stock or component key: fluid=" + (i == 1));
            }
        } else {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gtladditions:infinity_input_dual_hatch"));
            level.setBlockAndUpdate(INPUT, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(MAINTENANCE, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(INPUT, block.defaultBlockState());
            level.setBlockAndUpdate(MAINTENANCE, GTLMachines.AUTO_CONFIGURATION_MAINTENANCE_HATCH.getBlock().defaultBlockState());
            var machine = (InfinityDualHatchPartMachine) level.getBlockEntity(INPUT);
            machine.setFrontFacing(Direction.NORTH);
            machine.setWorkingEnabled(false); machine.setDistinct(false);
            insert(machine, LARGE, LARGE + 2);
            machine.insert(AEItemKey.of(Items.DIAMOND), 31, Actionable.MODULATE, IActionSource.empty());
            machine.insert(AEFluidKey.of(Fluids.WATER), 37, Actionable.MODULATE, IActionSource.empty());
            machine.getTraits(CatalystItemStackHandler.class).getFirst().storage.setStackInSlot(8, new ItemStack(Items.EMERALD, 19));
            ((AutoConfigurationMaintenanceHatchPartMachine) level.getBlockEntity(MAINTENANCE)).setDurationMultiplier(1);
            for (int i = 0; i < 2; i++) {
                var position = i == 0 ? AE_ITEM : AE_FLUID;
                var definition = i == 0 ? GTAEMachines.ITEM_IMPORT_BUS_ME : GTAEMachines.FLUID_IMPORT_HATCH_ME;
                level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(position, definition.getBlock().defaultBlockState());
                var ae = (MetaMachine) level.getBlockEntity(position);
                ae.setFrontFacing(Direction.NORTH);
                ((IControllable) ae).setWorkingEnabled(false);
                aeSlot(ae, i == 1).setConfig(new GenericStack(aeKey(i == 1), LARGE));
                aeSlot(ae, i == 1).setStock(new GenericStack(aeKey(i == 1), Long.MAX_VALUE));
            }
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_READY reload={} physical_server=true", reload());
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!active() || !(event.getEntity() instanceof ServerPlayer player)) return;
        // Rocket validation saves this player in survival. Reset the owned GUI fixture's
        // starting mode so its original block-use distances are repeatable after any run.
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
        // Leave login and the saved dimension/vehicle lifecycle intact. Move the owned
        // actor only on the next server tick, after all login event subscribers finish.
        prepareActorAfterLogin = true;
    }

    private static void seedSpoilable(ServerPlayer player) {
        var level = player.serverLevel();
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(SPOIL_POSITION).inflate(12)))
            if (entity.getItem().getHoverName().getString().equals(SPOIL_NAME)) entity.discard();
        var handlerData = new net.minecraft.nbt.CompoundTag();
        handlerData.putLong("native_long", LARGE);
        var context = new com.gregtechceu.gtceu.api.item.component.SpoilContext(level, SPOIL_POSITION, null,
                com.gregtechceu.gtceu.api.item.component.SpoilContext.ItemHandlerSource.BLOCK_CAPABILITY, handlerData, 12);
        var decoded = com.gregtechceu.gtceu.api.item.component.SpoilContext.deserializeNBT(context.serializeNBT());
        if (decoded.level() != level || !SPOIL_POSITION.equals(decoded.pos()) || decoded.slot() != 12
                || !handlerData.equals(decoded.itemHandlerData()))
            throw new AssertionError("Native spoilage context lost real server world or handler metadata");
        var stack = new ItemStack(Items.APPLE, 2);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(SPOIL_NAME));
        stack.set(com.gregtechceu.gtceu.common.data.item.GTDataComponents.SPOILABLE,
                new com.gregtechceu.gtceu.common.item.SpoilableItemStack.SpoilableData(true, LARGE, LARGE + 1, context));
        var entity = new net.minecraft.world.entity.item.ItemEntity(level,
                SPOIL_POSITION.getX() + .5, SPOIL_POSITION.getY(), SPOIL_POSITION.getZ() + .5, stack);
        entity.setNoGravity(true); entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        entity.setPickUpDelay(32767); entity.setUnlimitedLifetime();
        if (!level.addFreshEntity(entity)) throw new AssertionError("Native spoilable entity did not spawn");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_SPOIL_SERVER real_world=true handler_metadata=true native_entity_data_packet=true reload={}", reload());
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!active() || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        if (prepareActorAfterLogin) {
            var player = event.getServer().getPlayerList().getPlayers().getFirst();
            if (player.getVehicle() instanceof earth.terrarium.adastra.common.entities.vehicles.Lander lander
                    && !lander.isSafeToDismount(player)) {
                // Ad Astra rejects airborne dismounts. Place this owned saved-vehicle
                // fixture over a real platform and let native collision/physics land it.
                // New rocket/elevator journeys below still use their actual full flight.
                if (savedVehicleLandingTicks++ == 0) {
                    for (int x = 6; x <= 10; x++) for (int z = 8; z <= 12; z++)
                        player.serverLevel().setBlockAndUpdate(new net.minecraft.core.BlockPos(x, 95, z), Blocks.STONE.defaultBlockState());
                    lander.teleportTo(8.5, 97, 10.5);
                    lander.fallDistance = 0;
                    lander.setDeltaMovement(0, -.05, 0);
                    player.connection.teleport(8.5, 97, 10.5, 0, 0);
                }
                // Native vehicles are driven by the connected physical client. Allow
                // dimension loading and its real collision packets before timing out.
                if (savedVehicleLandingTicks > 1200)
                    throw new AssertionError("Owned saved native lander did not land through actual physics");
                return;
            }
            player.stopRiding();
            if (player.isPassenger()) throw new AssertionError("Saved native vehicle still prevents GUI actor dismount");
            player.teleportTo(event.getServer().overworld(), 8.5, 96, 10.5, 0, 0);
            seedSpoilable(player);
            player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
            prepareActorAfterLogin = false;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_ACTOR_READY native_vehicle_restore_completed=true saved_lander_collision_ticks={} reload={}", savedVehicleLandingTicks, reload());
        }
        if (stage == 5) {
            if (NativeProductionDedicatedRocketServer.completed()) NativeProductionDedicatedElevatorServer.tick(event.getServer());
            else NativeProductionDedicatedRocketServer.tick(event.getServer());
            return;
        }
        var level = event.getServer().overworld();
        var machine = (InfinityDualHatchPartMachine) level.getBlockEntity(INPUT);
        var physicalPlayer = event.getServer().getPlayerList().getPlayers().getFirst();
        if (reload() && physicalPlayer.containerMenu != physicalPlayer.inventoryMenu)
            reloadMenus.add(physicalPlayer.containerMenu);
        if (reload() && stage == 0) {
            if (event.getServer().getPlayerList().getPlayers().stream().noneMatch(p -> p.containerMenu != p.inventoryMenu)) return;
            stage = 3; return;
        }
        if (stage == 3) {
            var player = event.getServer().getPlayerList().getPlayers().getFirst();
            // Reload opens Infinity, AE item and AE fluid menus before travelling.
            if (reload() && reloadMenus.size() < 3) return;
            if (!NativeProductionDedicatedWirelessServer.tick(event.getServer())) return;
            if (player.containerMenu != player.inventoryMenu) return;
            NativeProductionDedicatedTravel.verifyDeniedEntry(player); stage = 4; return;
        }
        if (stage == 4) {
            if (++travelTicks < 20) return;
            NativeProductionDedicatedTravel.transfer(event.getServer().getPlayerList().getPlayers().getFirst());
            stage = 5;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_SERVER{}=PASS physical_server=true native_c2s=true exact_long=true components=true incremental=true distinct=true native_menu=true ae_popup_and_wheel_exact=true actual_disk_reload={} protected_native_player_travel=true", reload() ? "_RELOAD" : "", reload());
            return;
        }
        var maintenance = (AutoConfigurationMaintenanceHatchPartMachine) level.getBlockEntity(MAINTENANCE);
        if (stage == 0 && Math.abs(maintenance.getDurationMultiplier() - .99f) < .00001f) {
            if (!balancesMatch(machine, LARGE, LARGE + 2)) throw new AssertionError("Native maintenance action altered another machine's paid inputs");
            insert(machine, 117, 211); stage = 1;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_INCREMENTAL first=true maintenance_real_c2s=true");
        } else if (stage == 1 && machine.isDistinct()) {
            if (!balancesMatch(machine, LARGE + 117, LARGE + 213) || !machine.getRecipeHandlers().getFirst().isDistinct()
                    || machine.getRecipeHandlers().getLast().isDistinct())
                throw new AssertionError("Native distinct GUI action lost exact inputs or made shared catalysts distinct");
            insert(machine, 7, 11); stage = 2;
            machine.insert(AEItemKey.of(changedItem()), 17, Actionable.MODULATE, IActionSource.empty());
            machine.insert(AEFluidKey.of(changedFluid()), 23, Actionable.MODULATE, IActionSource.empty());
        } else if (stage == 2) {
            for (int i = 0; i < 2; i++) {
                var ae = (MetaMachine) level.getBlockEntity(i == 0 ? AE_ITEM : AE_FLUID);
                if (aeMatches(ae, i == 1, Long.MAX_VALUE) && !maximumObserved[i]) {
                    maximumObserved[i] = true;
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_AE_SERVER fluid={} popup_maximum_exact=true", i == 1);
                }
                if (aeMatches(ae, i == 1, Long.MAX_VALUE - 1) && !decrementedObserved[i]) {
                    if (!maximumObserved[i]) throw new AssertionError("AE amount was decremented before the server observed the exact popup submission");
                    decrementedObserved[i] = true;
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_AE_SERVER fluid={} wheel_decrement_exact=true stock_unchanged=true component_key=true", i == 1);
                }
            }
            if (decrementedObserved[0] && decrementedObserved[1]) {
                if (!balancesMatch(machine, LARGE + 124, LARGE + 224)) throw new AssertionError("AE GUI actions altered another machine's exact inputs");
                stage = 3;
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_AE_COMPLETE physical_server=true exact_long=true native_popup_and_wheel=true");
            }
        }
    }
}
