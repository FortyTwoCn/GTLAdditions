package com.gtladd.gtladditions.migration;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.common.mui.GTByteBufAdapters;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait;
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine;
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank;
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;
import java.util.ArrayList;
import java.util.List;

/** Nondefault production state, exact quantities, native recipe consumption and real world reload callbacks. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionStateGameTests {
    private static final long LARGE = 9_007_199_254_741_019L;
    private static ItemStack namedItem() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("production exact native item"));
        return stack;
    }
    private static FluidStack namedFluid() {
        var stack = new FluidStack(Fluids.WATER, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("production exact native fluid"));
        return stack;
    }
    private static FastNotifiableInputItemStack items(InfinityDualHatchPartMachine machine) {
        return machine.getTraits(FastNotifiableInputItemStack.class).getFirst();
    }
    private static FastNotifiableInputFluidTank fluids(InfinityDualHatchPartMachine machine) {
        return machine.getTraits(FastNotifiableInputFluidTank.class).getFirst();
    }
    private static void assertBalances(GameTestHelper helper, InfinityDualHatchPartMachine machine, long itemAmount, long fluidAmount) {
        helper.assertTrue(items(machine).getItemStorage().getLong(namedItem()) == itemAmount
                && fluids(machine).getFluidStorage().getLong(namedFluid()) == fluidAmount,
                "Production exact component quantities changed");
        helper.assertTrue(items(machine).getItemStorage().getLong(new ItemStack(Items.DIAMOND)) == 31
                && fluids(machine).getFluidStorage().getLong(new FluidStack(Fluids.WATER, 1)) == 37,
                "Production quantities from different component identities were merged");
    }

    @GameTest(template = "empty", batch = "gtl_production_nondefault_state", timeoutTicks = 200)
    public static void populatedInfinityHatchPreservesExactComponentsRecipesSyncDistinctAndSharedCatalysts(GameTestHelper helper) {
        var block = (MetaMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gtladditions", "infinity_input_dual_hatch"));
        var position = new BlockPos(3, 4, 3);
        helper.setBlock(position, block);
        var machine = (InfinityDualHatchPartMachine) helper.getBlockEntity(position);
        machine.setWorkingEnabled(false);
        var chunk = helper.getLevel().getChunkAt(machine.getBlockPos());
        chunk.setUnsaved(false);
        var source = IActionSource.empty();
        helper.assertTrue(machine.insert(AEItemKey.of(namedItem()), LARGE, Actionable.SIMULATE, source) == LARGE
                && machine.insert(AEFluidKey.of(namedFluid()), LARGE + 2, Actionable.SIMULATE, source) == LARGE + 2,
                "Production long AE insertion simulation failed");
        helper.assertTrue(items(machine).isEmpty() && fluids(machine).isEmpty() && !chunk.isUnsaved(),
                "Production insertion simulation changed inventory or world persistence");
        helper.assertTrue(machine.insert(AEItemKey.of(namedItem()), LARGE, Actionable.MODULATE, source) == LARGE
                && machine.insert(AEFluidKey.of(namedFluid()), LARGE + 2, Actionable.MODULATE, source) == LARGE + 2
                && chunk.isUnsaved(), "Production long insertion lost quantities or did not mark its chunk dirty");
        machine.insert(AEItemKey.of(Items.DIAMOND), 31, Actionable.MODULATE, source);
        machine.insert(AEFluidKey.of(Fluids.WATER), 37, Actionable.MODULATE, source);
        assertBalances(helper, machine, LARGE, LARGE + 2);
        helper.assertTrue(items(machine).getStackInSlot(0).getCount() == Integer.MAX_VALUE
                && fluids(machine).getFluidInTank(0).getAmount() == Integer.MAX_VALUE,
                "Production physical capabilities overflowed their native integer view");

        var sharedItems = machine.getTraits(CatalystItemStackHandler.class).getFirst();
        var sharedFluids = machine.getTraits(CatalystFluidStackHandler.class).getFirst();
        sharedItems.storage.setStackInSlot(8, new ItemStack(Items.EMERALD, 19));
        sharedFluids.getStorages()[8].setFluid(new FluidStack(Fluids.LAVA, 13_000));
        machine.getTraits(ProgrammableCircuitSlotTrait.class).getFirst().setCurrentCircuit(17);
        machine.setDistinct(true);
        machine.setPaintingColor(0x128AC4);
        var groups = machine.getRecipeHandlers();
        helper.assertTrue(groups.size() == 2 && groups.getFirst().isDistinct() && !groups.getLast().isDistinct()
                && groups.getFirst().getColor() == 0x128AC4 && groups.getLast().getColor() == 0x128AC4,
                "Production native recipe groups did not separate distinct inputs and shared catalysts");

        var registries = helper.getLevel().registryAccess();
        var saved = machine.saveWithoutMetadata(registries);
        var restored = (InfinityDualHatchPartMachine) block.getDefinition().getBlockEntityType().create(machine.getBlockPos(), machine.getBlockState());
        // Native controllers can request these groups before a block entity's saved data arrives.
        restored.getRecipeHandlers();
        restored.loadAdditional(saved, registries);
        helper.assertTrue(saved.equals(restored.saveWithoutMetadata(registries)), "Populated production block entity NBT changed on reload");
        assertBalances(helper, restored, LARGE, LARGE + 2);
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);

        helper.startSequence().thenIdle(2).thenExecute(() -> {
            helper.assertTrue(restored.isDistinct() && restored.getRecipeHandlers().getFirst().isDistinct()
                    && !restored.getRecipeHandlers().getLast().isDistinct()
                    && restored.getRecipeHandlers().stream().allMatch(group -> group.getColor() == 0x128AC4),
                    "Production world reload left stale native distinct or painting recipe groups");
            helper.assertTrue(restored.getTraits(ProgrammableCircuitSlotTrait.class).getFirst().getCurrentCircuit() == 17
                    && restored.getTraits(CatalystItemStackHandler.class).getFirst().getStackInSlot(8).getCount() == 19
                    && restored.getTraits(CatalystFluidStackHandler.class).getFirst().getFluidInTank(8).getAmount() == 13_000,
                    "Populated production world reload lost circuit or shared catalysts");

            var clientCopy = (InfinityDualHatchPartMachine) block.getDefinition().getBlockEntityType().create(restored.getBlockPos(), restored.getBlockState());
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                items(restored).getSyncDataHolder().resyncAllFields();
                items(restored).getSyncDataHolder().writeClientPacket(registries, buffer);
                items(clientCopy).getSyncDataHolder().readClientPacket(registries, buffer);
                helper.assertTrue(!buffer.isReadable(), "Native item sync left unread packet data");
                buffer.clear();
                fluids(restored).getSyncDataHolder().resyncAllFields();
                fluids(restored).getSyncDataHolder().writeClientPacket(registries, buffer);
                fluids(clientCopy).getSyncDataHolder().readClientPacket(registries, buffer);
                helper.assertTrue(!buffer.isReadable(), "Native fluid sync left unread packet data");
                assertBalances(helper, clientCopy, LARGE, LARGE + 2);
            } finally { buffer.release(); }

            var inputItem = LongIngredient.create(namedItem(), LARGE - 17);
            var inputFluid = LongFluidIngredient.create(namedFluid(), LARGE - 21);
            helper.assertTrue(items(restored).handleRecipeInner(IO.IN, null, new ArrayList<>(List.of(inputItem)), true).isEmpty()
                    && fluids(restored).handleRecipeInner(IO.IN, null, new ArrayList<>(List.of(inputFluid)), true).isEmpty(),
                    "Production exact recipe simulation failed");
            assertBalances(helper, restored, LARGE, LARGE + 2);
            helper.assertTrue(items(restored).handleRecipeInner(IO.IN, null, new ArrayList<>(List.of(inputItem)), false).isEmpty()
                    && fluids(restored).handleRecipeInner(IO.IN, null, new ArrayList<>(List.of(inputFluid)), false).isEmpty(),
                    "Production exact recipe consumption failed");
            assertBalances(helper, restored, 17, 23);
            var recipe = GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder("native_production_shared_catalysts")
                    .notConsumable(new ItemStack(Items.EMERALD, 19)).notConsumableFluid(new FluidStack(Fluids.LAVA, 13_000))
                    .duration(1).build();
            var catalystItems = restored.getTraits(CatalystItemStackHandler.class).getFirst();
            var catalystFluids = restored.getTraits(CatalystFluidStackHandler.class).getFirst();
            // Feed the handlers the recipe's own native ingredients, as the real runner does.
            // A newly created long wrapper is a different ingredient type from this int-sized recipe.
            helper.assertTrue(catalystItems.handleRecipeInner(IO.IN, recipe,
                    new ArrayList<>(recipe.getInputContents(ItemRecipeCapability.CAP).stream()
                            .map(content -> ItemRecipeCapability.CAP.of(content.content())).toList()), true).isEmpty()
                    && catalystFluids.handleRecipeInner(IO.IN, recipe,
                    new ArrayList<>(recipe.getInputContents(FluidRecipeCapability.CAP).stream()
                            .map(content -> FluidRecipeCapability.CAP.of(content.content())).toList()), true).isEmpty(),
                    "Production shared catalyst recipe did not match");
            // The native runner consumes only consumable contents. Shared catalyst handlers deliberately
            // leave an execution request untouched; matching happens during simulation.
            catalystItems.handleRecipeInner(IO.IN, recipe, new ArrayList<>(List.of(LongIngredient.create(new ItemStack(Items.EMERALD), 19))), false);
            catalystFluids.handleRecipeInner(IO.IN, recipe, new ArrayList<>(List.of(LongFluidIngredient.create(Fluids.LAVA, 13_000))), false);
            helper.assertTrue(catalystItems.getStackInSlot(8).getCount() == 19
                    && catalystFluids.getFluidInTank(8).getAmount() == 13_000, "Production shared catalyst recipe consumed reusable inputs");
            helper.assertTrue(GTByteBufAdapters.COMPONENT.areEqual(Component.literal("0"), Component.literal("0"))
                    && !GTByteBufAdapters.COMPONENT.areEqual(Component.literal("0"), Component.literal("1")),
                    "Production GT component adapter rejected a native literal chat component");
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_STATE exact_long=true components=true chunk_dirty=true world_reload=true native_sync=true distinct=true shared_catalysts=true");
        }).thenSucceed();
    }
}
