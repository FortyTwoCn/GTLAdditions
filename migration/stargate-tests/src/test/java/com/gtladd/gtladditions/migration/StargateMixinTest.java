package com.gtladd.gtladditions.migration;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;
import net.povstalec.sgjourney.common.init.FluidInit;
import net.povstalec.sgjourney.common.init.VillagerInit;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.Dialing;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class StargateMixinTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static boolean staticFlag(Class<?> type, String name) throws ReflectiveOperationException {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(null);
    }

    @Test
    void allEnergyChecksAreDisabledOnTransformedNativeClasses() throws ReflectiveOperationException {
        assertFalse(staticFlag(AbstractDHDEntity.class, "REQUIRE_ENERGY"));
        assertFalse(staticFlag(AbstractStargateEntity.class, "REQUIRE_ENERGY"));
        assertFalse(staticFlag(StargateNetwork.class, "REQUIRE_ENERGY"));
        assertFalse(staticFlag(StargateConnection.class, "REQUIRE_ENERGY"));
        assertTrue(staticFlag(StargateConnection.class, "ENERGY_BYPASS_ENABLED"));
    }

    @Test
    void everyProductionServerStargateTargetTransformsWithRequiredInjections() throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/stargate-mixin-targets.txt")))) {
            var targets = reader.lines().toList();
            assertEquals(19, targets.size());
            for (String target : targets) {
                assertDoesNotThrow(() -> Class.forName(target, false, getClass().getClassLoader()), target);
            }
        }
    }

    private record Travel(boolean incoming, StargateInfo.WormholeTravel allowed) {}

    // Test collaborators implement the real native Stargate interface. Production
    // StargateConnection/StargateNetwork code runs unchanged through the Mixin launcher.
    private static Stargate gate(List<Travel> travel) {
        return gate(travel, false);
    }

    private static Stargate gate(List<Travel> travel, boolean connected) {
        return (Stargate) Proxy.newProxyInstance(Stargate.class.getClassLoader(),
                new Class<?>[] {Stargate.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "checkValidity" -> true;
                    case "isConnected" -> connected;
                    case "getDimension" -> Level.OVERWORLD;
                    case "getDialedStargates" -> List.of(proxy);
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestStargate";
                    case "doWormhole" -> {
                        travel.add(new Travel((boolean) args[1], (StargateInfo.WormholeTravel) args[2]));
                        yield null;
                    }
                    case "canExtract", "extractEnergy", "shouldAutoclose", "connectStargate", "resetStargate" ->
                            throw new AssertionError("Unexpected energy, autoclose or simulation side effect: " + method);
                    default -> {
                        if (method.getReturnType() == boolean.class) yield false;
                        if (method.getReturnType() == int.class) yield 0;
                        if (method.getReturnType() == long.class) yield 0L;
                        yield null;
                    }
                });
    }

    @Test
    void connectionTickAllowsBothIncomingDestinationsWithoutPowerOrAutoclose() {
        assertNotEquals(StargateInfo.WormholeTravel.ENABLED, CommonStargateConfig.two_way_wormholes.get());
        var outgoing = new ArrayList<Travel>();
        var incomingA = new ArrayList<Travel>();
        var incomingB = new ArrayList<Travel>();
        var connection = StargateConnection.create(StargateConnection.Type.INTERGALACTIC,
                gate(outgoing, true), List.of(gate(incomingA, true), gate(incomingB, true)), false, Dialing.Action.SIMULATE);
        assertNotNull(connection);
        connection.tick(null);
        assertEquals(List.of(new Travel(false, StargateInfo.WormholeTravel.ENABLED)), outgoing);
        assertEquals(List.of(new Travel(true, StargateInfo.WormholeTravel.ENABLED)), incomingA);
        assertEquals(List.of(new Travel(true, StargateInfo.WormholeTravel.ENABLED)), incomingB);
    }

    @Test
    void connectionSimulationKeepsSelfDialValidationAndDoesNotAddLiveConnection() throws Exception {
        var network = new StargateNetwork(null);
        var dialing = gate(new ArrayList<>());
        assertEquals(StargateInfo.Feedback.SELF_DIAL,
                network.createConnection(dialing, dialing, Address.Type.ADDRESS_9_CHEVRON,
                        false, Dialing.Action.SIMULATE).feedback());
        assertEquals(StargateInfo.Feedback.CONNECTION_ESTABLISHED_SYSTEM_WIDE,
                network.createConnection(dialing, gate(new ArrayList<>()), Address.Type.ADDRESS_9_CHEVRON,
                        false, Dialing.Action.SIMULATE).feedback());
        var connections = StargateNetwork.class.getDeclaredField("connections");
        connections.setAccessible(true);
        assertTrue(((Map<?, ?>) connections.get(network)).isEmpty());
    }

    @Test
    void nativeFluidRegistrationKeepsGtlDisabledFluidNames() {
        assertEquals("liquid_naquadah_ban", FluidInit.LIQUID_NAQUADAH_SOURCE.getId().getPath());
        assertEquals("flowing_liquid_naquadah_ban", FluidInit.LIQUID_NAQUADAH_FLOWING.getId().getPath());
        assertEquals("heavy_liquid_naquadah_ban", FluidInit.HEAVY_LIQUID_NAQUADAH_SOURCE.getId().getPath());
        assertEquals("flowing_heavy_liquid_naquadah_ban", FluidInit.HEAVY_LIQUID_NAQUADAH_FLOWING.getId().getPath());
    }

    @Test
    void villageTagsKeepBoundPoiWithoutAddingArcheologistProfession() {
        assertTrue(BuiltInRegistries.POINT_OF_INTEREST_TYPE.containsKey(VillagerInit.ARCHEOLOGIST_POI.getId()));
        assertFalse(BuiltInRegistries.VILLAGER_PROFESSION.containsKey(VillagerInit.ARCHEOLOGIST.getId()));
    }

    @Test
    void nativeJaffaLootItemStacksDecodeFluidComponentsWithPreservedGtlIds() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/data/sgjourney/loot_table/chests/jaffa_house.json"))) {
            var ops = RegistryOps.create(JsonOps.INSTANCE,
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var stacks = new ArrayList<JsonElement>();
            collectItemStacks(JsonParser.parseReader(reader), stacks);
            assertFalse(stacks.isEmpty());
            for (JsonElement stack : stacks) {
                var decoded = ItemStack.CODEC.parse(ops, stack);
                assertTrue(decoded.isSuccess(), decoded.error().map(Object::toString).orElse("Item component decode failed"));
            }
        }
    }

    private static void collectItemStacks(JsonElement node, List<JsonElement> result) {
        if (node.isJsonObject()) {
            var object = node.getAsJsonObject();
            if (object.has("id") && object.has("count")) result.add(object);
            object.entrySet().forEach(entry -> collectItemStacks(entry.getValue(), result));
        } else if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(child -> collectItemStacks(child, result));
        }
    }
}
