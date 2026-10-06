package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.blockentity.OpticalPipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;
import com.mojang.logging.LogUtils;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.entities.vehicles.Lander;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.gtlcore.gtlcore.api.gui.preview.NativePreviewShapes;
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineB;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.ComputationProviderMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Uses original structures, native optical pipes, finite stored energy and the original pack recipe. */
final class NativeProductionDedicatedElevatorServer {
    static final BlockPos ELEVATOR = new BlockPos(200, 96, 200);
    private static int phase, delay;
    private static SpaceElevatorMachine elevator;
    private static EnergyHatchPartMachine elevatorEnergy, providerEnergy;
    private static long elevatorBudget, providerBudget;
    private static long elevatorReserve, providerReserve;

    static void tick(MinecraftServer server) {
        if (phase == 4 || server.getPlayerList().getPlayers().isEmpty()) return;
        var player = server.getPlayerList().getPlayers().getFirst();
        if (phase == 0) {
            // Give the physical client enough time to observe the actual Moon lander/passenger packets.
            if (++delay < 200) return;
            if (!player.level().dimension().equals(Planet.MOON))
                throw new AssertionError("Space Elevator validation began before native rocket landing");
            player.stopRiding();
            player.getInventory().setItem(2, ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.teleportTo(server.overworld(), ELEVATOR.getX() + .5, ELEVATOR.getY(), ELEVATOR.getZ() - 2.5, 0, 0);
            if (player.isCreative() || player.isSpectator() || server.getPlayerList().isOp(player.getGameProfile())
                    || player.getTags().contains("spaceelevatorst") || player.getTags().contains("canTeleportToPlanet")
                    || ModUtils.canTeleportToPlanet(player, PlanetApi.API.getPlanet(Planet.MARS)))
                throw new AssertionError("Survival elevator fixture already has permission to land on Mars");
            var level = server.overworld();
            elevator = (SpaceElevatorMachine) place(level, ELEVATOR, AdvancedMultiBlockMachineB.SPACE_ELEVATOR.get(), GTValues.UV);
            var provider = (ComputationProviderMachine) place(level, new BlockPos(250, 96, 200),
                    AdvancedMultiBlockMachineB.CREATE_COMPUTATION.get(), GTValues.MAX);
            var receiver = elevator.getParts().stream().filter(OpticalComputationHatchMachine.class::isInstance)
                    .map(OpticalComputationHatchMachine.class::cast).filter(part -> !part.isTransmitter()).findFirst().orElseThrow();
            var transmitter = provider.getParts().stream().filter(OpticalComputationHatchMachine.class::isInstance)
                    .map(OpticalComputationHatchMachine.class::cast).filter(OpticalComputationHatchMachine::isTransmitter).findFirst().orElseThrow();
            connect(level, transmitter.getBlockPos(), receiver.getBlockPos());
            elevator.checkAndFormStructure(); provider.checkAndFormStructure();
            if (!elevator.isFormed() || !provider.isFormed())
                throw new AssertionError("Native optical connection invalidated an original structure");
            elevatorEnergy = energy(elevator); providerEnergy = energy(provider);
            elevatorBudget = elevatorEnergy.energyContainer.getEnergyCapacity();
            providerBudget = providerEnergy.energyContainer.getEnergyCapacity();
            if (elevatorEnergy.energyContainer.changeEnergy(elevatorBudget) != elevatorBudget
                    || providerEnergy.energyContainer.changeEnergy(providerBudget) != providerBudget)
                throw new AssertionError("Original native energy hatches did not accept the finite fixture budgets");
            // A bounded fixture supply leaves time for native dimension loading and GUI interaction.
            // Only native hatch energy is supplied; the original recipe and computation debit stay active.
            elevatorReserve = Math.multiplyExact(elevatorBudget, 16);
            providerReserve = Math.multiplyExact(providerBudget, 16);
            for (var part : elevator.getParts()) {
                if (part instanceof ItemBusPartMachine bus) bus.getCircuitSlot().setCurrentCircuit(1);
                if (part instanceof MaintenanceHatchPartMachine maintenance) maintenance.setMaintenanceProblems((byte) 63);
            }
            var front = elevator.getFrontFacing();
            var stand = ELEVATOR.relative(front, 2);
            level.setBlockAndUpdate(stand.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.teleportTo(level, stand.getX() + .5, stand.getY(), stand.getZ() + .5, 0, 0);
            elevator.setWorkingEnabled(true); provider.setWorkingEnabled(true);
            phase = 1;
            return;
        }
        if (phase >= 1 && phase <= 3) {
            long supplied = supply(elevatorEnergy, elevatorReserve);
            elevatorReserve -= supplied; elevatorBudget = Math.addExact(elevatorBudget, supplied);
            supplied = supply(providerEnergy, providerReserve);
            providerReserve -= supplied; providerBudget = Math.addExact(providerBudget, supplied);
        }
        if (phase == 1) {
            if (!elevator.getRecipeLogic().isWorking() || elevator.getRecipeLogic().getProgress() < 3) return;
            long providerPaid = providerBudget - providerEnergy.energyContainer.getEnergyStored();
            long elevatorPaid = elevatorBudget - elevatorEnergy.energyContainer.getEnergyStored();
            if (providerPaid <= 0 || providerPaid % Integer.MAX_VALUE != 0 || elevatorPaid <= 0
                    || elevatorPaid % GTValues.VA[GTValues.UV] != 0)
                throw new AssertionError("Actual Space Elevator progression did not pay native EU and computation costs");
            phase = 2;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ELEVATOR_READY original_structure=true native_optical_pipe=true actual_recipe_ticks=true finite_energy=true CWUt=128 elevator_EU_paid={} provider_EU_paid={} survival=true unauthorized_landing_rejected=true reload={}",
                    elevatorPaid, providerPaid, reload());
        }
        if (phase == 2 && player.getTags().contains("spaceelevatorst")) {
            if (!elevator.isFormed() || !elevator.getRecipeLogic().isWorking())
                throw new AssertionError("Native departure button authorized an idle or unformed elevator");
            phase = 3;
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ELEVATOR_BUTTON native_gui_click_c2s=true actual_working_machine=true native_planets_menu=true reload={}", reload());
        }
        if (phase != 3 || !player.level().dimension().equals(Planet.MARS)) return;
        if (!(player.getVehicle() instanceof Lander)
                || !ItemStack.matches(player.getInventory().getItem(0), namedItem().copyWithCount(3))
                || player.getTags().contains("spaceelevatorst") || player.getTags().contains("canTeleportToPlanet"))
            throw new AssertionError("Actual survival elevator landing lost its native lander/cargo or leaked its permit");
        phase = 4;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ELEVATOR_SERVER=PASS destination=ad_astra:mars survival=true native_landing_c2s=true permit_consumed=true inventory_components=true native_lander=true reload={}", reload());
    }

    private static EnergyHatchPartMachine energy(MultiblockControllerMachine machine) {
        return machine.getParts().stream().filter(EnergyHatchPartMachine.class::isInstance)
                .map(EnergyHatchPartMachine.class::cast).findFirst().orElseThrow();
    }

    private static long supply(EnergyHatchPartMachine hatch, long reserve) {
        long missing = hatch.energyContainer.getEnergyCapacity() - hatch.energyContainer.getEnergyStored();
        long requested = Math.min(missing, reserve);
        if (requested == 0) return 0;
        long supplied = hatch.energyContainer.changeEnergy(requested);
        if (supplied != requested || supplied < 0)
            throw new AssertionError("Native fixture supply exceeded its finite energy reserve");
        return supplied;
    }

    private static MultiblockControllerMachine place(ServerLevel level, BlockPos origin,
                                                     MultiblockMachineDefinition definition, int tier) {
        var front = definition.getRotationState().defaultDirection;
        var up = front.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        var geometry = NativePreviewShapes.get(definition).getFirst().oriented(front, up, false);
        var states = new LinkedHashMap<>(geometry.states());
        var energyBlock = GTMachines.ENERGY_INPUT_HATCH_16A[tier].getBlock();
        var slot = geometry.predicates().entrySet().stream().filter(entry -> !entry.getKey().equals(BlockPos.ZERO))
                .filter(entry -> !(states.get(entry.getKey()).getBlock() instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock))
                .filter(entry -> entry.getValue().getCandidates().stream().flatMap(java.util.Collection::stream)
                        .anyMatch(candidate -> candidate.getBlockState().is(energyBlock))).findFirst().orElseThrow();
        // Replace the original preview energy hatch when present; the provider preview may omit this optional hatch.
        var previewEnergy = states.entrySet().stream().filter(entry -> entry.getValue().getBlock()
                instanceof com.gregtechceu.gtceu.api.block.MetaMachineBlock block
                && block.getDefinition().getId().getPath().contains("energy_input_hatch")).findFirst();
        states.put(previewEnergy.map(Map.Entry::getKey).orElse(slot.getKey()), energyBlock.defaultBlockState());
        level.setBlockAndUpdate(origin, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        for (var entry : states.entrySet()) if (!entry.getKey().equals(BlockPos.ZERO)) {
            level.setBlockAndUpdate(origin.offset(entry.getKey()), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(entry.getKey()), entry.getValue());
        }
        level.setBlockAndUpdate(origin, states.get(BlockPos.ZERO));
        var machine = (MultiblockControllerMachine) level.getBlockEntity(origin);
        machine.checkAndFormStructure();
        if (!machine.isFormed()) throw new AssertionError("Original elevator/provider structure did not form: "
                + definition.getId() + " " + machine.getDefaultPatternState().getErrors());
        if (machine instanceof com.gregtechceu.gtceu.api.capability.IControllable controllable)
            controllable.setWorkingEnabled(false);
        return machine;
    }

    private static void connect(ServerLevel level, BlockPos from, BlockPos to) {
        record Node(BlockPos pos, int cost) {}
        var previous = new java.util.HashMap<BlockPos, BlockPos>();
        var distance = new java.util.HashMap<BlockPos, Integer>();
        var queue = new java.util.PriorityQueue<Node>(java.util.Comparator.comparingInt(node -> node.cost() + manhattan(node.pos(), to)));
        queue.add(new Node(from, 0)); distance.put(from, 0);
        int checked = 0;
        while (!queue.isEmpty() && checked++ < 20_000) {
            var node = queue.remove();
            if (distance.get(node.pos()) != node.cost()) continue;
            if (node.pos().equals(to)) break;
            for (var side : Direction.values()) {
                var next = node.pos().relative(side);
                if (next.getY() < 80 || next.getY() > 128
                        || next.getX() < Math.min(from.getX(), to.getX()) - 20 || next.getX() > Math.max(from.getX(), to.getX()) + 20
                        || next.getZ() < Math.min(from.getZ(), to.getZ()) - 20 || next.getZ() > Math.max(from.getZ(), to.getZ()) + 20) continue;
                if (!next.equals(to) && !level.getBlockState(next).isAir()
                        && !(level.getBlockEntity(next) instanceof OpticalPipeBlockEntity)) continue;
                int cost = node.cost() + 1;
                if (cost >= distance.getOrDefault(next, Integer.MAX_VALUE)) continue;
                distance.put(next, cost); previous.put(next, node.pos()); queue.add(new Node(next, cost));
            }
        }
        if (!previous.containsKey(to)) throw new AssertionError("No non-destructive native optical route between original hatches");
        var path = new ArrayList<BlockPos>();
        for (var cursor = to; cursor != null; cursor = previous.get(cursor)) path.add(cursor);
        java.util.Collections.reverse(path);
        for (int i = 1; i < path.size() - 1; i++) {
            var pos = path.get(i);
            if (!level.getBlockState(pos).isAir() && !(level.getBlockEntity(pos) instanceof OpticalPipeBlockEntity))
                throw new AssertionError("Native optical route would overwrite a structure at " + pos);
            level.setBlockAndUpdate(pos, GTBlocks.OPTICAL_PIPES[0].get().defaultBlockState());
        }
        for (int i = 1; i < path.size() - 1; i++) {
            var pipe = (OpticalPipeBlockEntity) level.getBlockEntity(path.get(i));
            for (var side : Direction.values()) pipe.setConnection(side, false, false);
        }
        for (int i = 1; i < path.size() - 1; i++) {
            var pipe = (OpticalPipeBlockEntity) level.getBlockEntity(path.get(i));
            pipe.setConnection(direction(path.get(i), path.get(i - 1)), true, false);
            pipe.setConnection(direction(path.get(i), path.get(i + 1)), true, false);
        }
    }

    private static int manhattan(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ());
    }

    private static Direction direction(BlockPos from, BlockPos to) {
        var delta = to.subtract(from);
        if (Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ()) != 1)
            throw new AssertionError("Native optical route is not continuous");
        return Direction.fromDelta(delta.getX(), delta.getY(), delta.getZ());
    }
}
