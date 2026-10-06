package com.gtladd.gtladditions.migration;

import earth.terrarium.adastra.common.blocks.LaunchPadBlock;
import earth.terrarium.adastra.common.blocks.properties.LaunchPadPartProperty;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.items.vehicles.RocketItem;
import earth.terrarium.adastra.common.registry.ModBlocks;
import earth.terrarium.common_storage_lib.context.impl.ModifyOnlyContext;
import earth.terrarium.common_storage_lib.fluid.FluidApi;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.mojang.logging.LogUtils;

/** Uses the real item lookup, native component codec, placement handler and all seven registered entities. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionRocketGameTests {
    static void assertCreativeRocketFuels(net.minecraft.world.flag.FeatureFlagSet features,
                                          net.minecraft.core.HolderLookup.Provider registries) {
        CreativeModeTabs.tryRebuildTabContents(features, true, registries);
        var stacks = BuiltInRegistries.CREATIVE_MODE_TAB.stream()
                .flatMap(tab -> tab.getDisplayItems().stream()).filter(stack -> stack.getItem() instanceof RocketItem).toList();
        for (int tier = 1; tier <= 7; tier++) {
            var expected = FluidResource.of(org.gtlcore.gtlcore.common.data.GTLRocketFuels.forTier(tier));
            String itemId = (tier <= 4 ? "ad_astra" : "ad_astra_rocketed") + ":tier_" + tier + "_rocket";
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (stacks.stream().filter(stack -> stack.is(item)).noneMatch(stack -> {
                var storage = new ModifyOnlyContext(stack.copy()).find(FluidApi.ITEM);
                return storage != null && storage.getAmount(0) == 16_000 && storage.getResource(0).equals(expected);
            })) throw new AssertionError("Actual native creative tabs lack a correctly fueled rocket: " + itemId);
        }
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET_CREATIVE tiers=7 native_tabs=true correct_fuel=true capacity=16000");
    }

    @GameTest(template = "empty", batch = "gtl_production_rockets", timeoutTicks = 200)
    public static void sevenTiersFillPlaceSaveReloadAndConsumeExactFuel(GameTestHelper helper) {
        String[] fuelIds = {
                "gtceu:rocket_fuel", "gtceu:rocket_fuel_rp_1", "gtceu:dense_hydrazine_fuel_mixture",
                "gtceu:rocket_fuel_cn3h7o3", "gtceu:rocket_fuel_h8n4c2o4", "ad_astra:cryo_fuel",
                "gtceu:stellar_energy_rocket_fuel"
        };
        var level = helper.getLevel();
        var registries = level.registryAccess();
        assertCreativeRocketFuels(level.enabledFeatures(), registries);
        var player = FakePlayerFactory.getMinecraft(level);
        var previous = player.getMainHandItem();
        var relative = new BlockPos(8, 3, 8);
        helper.setBlock(relative, ModBlocks.LAUNCH_PAD.get().defaultBlockState()
                .setValue(LaunchPadBlock.PART, LaunchPadPartProperty.CENTER));
        var position = helper.absolutePos(relative);
        player.setPos(position.getCenter().add(0, 1, 3));
        try {
            for (int tier = 1; tier <= 7; tier++) {
                String itemId = (tier <= 4 ? "ad_astra" : "ad_astra_rocketed") + ":tier_" + tier + "_rocket";
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                helper.assertTrue(item instanceof RocketItem, "Missing native rocket item: " + itemId);
                var rocketItem = (RocketItem) item;
                var expectedFuel = FluidResource.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(fuelIds[tier - 1])));
                helper.assertTrue(!expectedFuel.isBlank(), "Missing native rocket fuel: " + fuelIds[tier - 1]);
                var stack = new ItemStack(item);
                var storage = new ModifyOnlyContext(stack).find(FluidApi.ITEM);
                helper.assertTrue(storage != null && storage.getLimit(0, expectedFuel) == 16_000,
                        "Invalid rocket item storage capacity: " + itemId);
                helper.assertTrue(storage.insert(FluidResource.of(Fluids.WATER), 16_000, false) == 0,
                        "Rocket item accepted water: " + itemId);
                var wrongFuel = FluidResource.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(fuelIds[tier % 7])));
                helper.assertTrue(storage.insert(wrongFuel, 16_000, false) == 0, "Rocket item accepted another tier's fuel: " + itemId);
                helper.assertTrue(storage.insert(expectedFuel, 20_000, true) == 16_000 && storage.getAmount(0) == 0,
                        "Simulated item fill changed component storage: " + itemId);
                helper.assertTrue(storage.insert(expectedFuel, 20_000, false) == 16_000, "Native item fill failed: " + itemId);
                stack = ItemStack.parseOptional(registries, (CompoundTag) stack.save(registries));
                storage = new ModifyOnlyContext(stack).find(FluidApi.ITEM);
                helper.assertTrue(storage != null && storage.getAmount(0) == 16_000 && storage.getResource(0).equals(expectedFuel),
                        "Rocket item component save/load lost fuel: " + itemId);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var hit = new BlockHitResult(position.getCenter(), Direction.UP, position, false);
                helper.assertTrue(rocketItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                        "Native rocket placement failed: " + itemId);
                helper.assertTrue(player.getMainHandItem().isEmpty(), "Placement did not consume the rocket item: " + itemId);
                var entities = level.getEntitiesOfClass(Rocket.class, new AABB(position).inflate(2));
                helper.assertTrue(entities.size() == 1, "Placement did not create exactly one native rocket: " + itemId);
                var rocket = entities.getFirst();
                helper.assertTrue(rocket.getType() == rocketItem.type() && rocket.tier() == tier,
                        "Placement created the wrong rocket entity: " + itemId);
                helper.assertTrue(rocket.fluidContainer().getAmount(0) == 16_000
                        && rocket.fluidContainer().getResource(0).equals(expectedFuel), "Placement lost the item's fuel: " + itemId);
                var saved = rocket.saveWithoutId(new CompoundTag());
                saved.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(rocket.getType()).toString());
                var loaded = EntityType.loadEntityRecursive(saved, level, entity -> entity);
                helper.assertTrue(loaded instanceof Rocket, "Rocket entity save/load failed: " + itemId);
                var restored = (Rocket) loaded;
                helper.assertTrue(restored.tier() == tier && restored.fluidContainer().getAmount(0) == 16_000
                        && restored.fluidContainer().getResource(0).equals(expectedFuel), "Entity persistence lost rocket fuel: " + itemId);
                helper.assertTrue(restored.consumeFuel(true) && restored.fluidContainer().getAmount(0) == 16_000,
                        "Simulated launch consumed fuel: " + itemId);
                helper.assertTrue(restored.consumeFuel(false) && restored.fluidContainer().getAmount(0) == 0,
                        "Launch did not consume exactly 16,000 mB: " + itemId);
                helper.assertTrue(restored.fluidContainer().insert(wrongFuel, 16_000, false) == 0,
                        "Rocket entity accepted another tier's fuel: " + itemId);
                helper.assertTrue(restored.fluidContainer().insert(expectedFuel, 15_999, false) == 15_999,
                        "Rocket entity rejected its tier fuel: " + itemId);
                helper.assertTrue(!restored.hasEnoughFuel() && restored.fluidContainer().getAmount(0) == 15_999,
                        "Underfilled rocket passed launch check or lost fuel: " + itemId);
                rocket.discard();
                restored.discard();
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_ROCKET tier={} item={} fuel={}", tier, itemId, fuelIds[tier - 1]);
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
        helper.succeed();
    }
}
