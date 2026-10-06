package com.gtladd.gtladditions.migration;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine;
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank;
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;
import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable fixture identities; server and client own independent machines in separate JVMs. */
final class NativeProductionDedicatedState {
    static final BlockPos INPUT = new BlockPos(8, 96, 8);
    static final BlockPos MAINTENANCE = new BlockPos(10, 96, 8);
    static final BlockPos AE_ITEM = new BlockPos(7, 96, 8);
    static final BlockPos AE_FLUID = new BlockPos(6, 96, 8);
    static final int AE_SLOT = 10;
    static final BlockPos SPOIL_POSITION = new BlockPos(4, 97, 10);
    static final String SPOIL_NAME = "native dedicated frozen spoilable";
    static final ResourceKey<Level> TRAVEL_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sgjourney:lantea"));
    static final long LARGE = 9_007_199_254_741_019L;
    static boolean reload() { return Boolean.getBoolean("gtl.nativeProductionDedicatedReload"); }
    static AEKey aeKey(boolean fluid) {
        var name = Component.literal("dedicated AE component key");
        if (fluid) {
            var stack = new FluidStack(Fluids.WATER, 1);
            stack.set(DataComponents.CUSTOM_NAME, name);
            return AEFluidKey.of(stack);
        }
        var stack = new ItemStack(Items.IRON_INGOT);
        stack.set(DataComponents.CUSTOM_NAME, name);
        return AEItemKey.of(stack);
    }
    static IConfigurableSlot aeSlot(MetaMachine machine, boolean fluid) {
        return fluid ? machine.getTrait(ExportOnlyAEFluidList.class).getConfigurableSlot(AE_SLOT)
                : machine.getTrait(ExportOnlyAEItemList.class).getConfigurableSlot(AE_SLOT);
    }
    static boolean aeMatches(MetaMachine machine, boolean fluid, long amount) {
        var slot = aeSlot(machine, fluid);
        var config = slot.getConfig();
        var stock = slot.getStock();
        return config != null && stock != null && aeKey(fluid).equals(config.what())
                && aeKey(fluid).equals(stock.what()) && config.amount() == amount && stock.amount() == Long.MAX_VALUE;
    }
    static ItemStack namedItem() {
        var stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("dedicated exact named item"));
        return stack;
    }
    static FluidStack namedFluid() {
        var stack = new FluidStack(Fluids.WATER, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("dedicated exact named fluid"));
        return stack;
    }
    static ItemStack changedItem() {
        var stack = namedItem();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("incremental native menu item"));
        return stack;
    }
    static FluidStack changedFluid() {
        var stack = namedFluid();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("incremental native menu fluid"));
        return stack;
    }
    static FastNotifiableInputItemStack items(InfinityDualHatchPartMachine machine) {
        return machine.getTraits(FastNotifiableInputItemStack.class).getFirst();
    }
    static FastNotifiableInputFluidTank fluids(InfinityDualHatchPartMachine machine) {
        return machine.getTraits(FastNotifiableInputFluidTank.class).getFirst();
    }
    static void insert(InfinityDualHatchPartMachine machine, long itemAmount, long fluidAmount) {
        if (machine.insert(AEItemKey.of(namedItem()), itemAmount, Actionable.MODULATE, IActionSource.empty()) != itemAmount
                || machine.insert(AEFluidKey.of(namedFluid()), fluidAmount, Actionable.MODULATE, IActionSource.empty()) != fluidAmount)
            throw new AssertionError("Real dedicated machine rejected exact component quantities");
    }
    static boolean balancesMatch(InfinityDualHatchPartMachine machine, long itemAmount, long fluidAmount) {
        return items(machine).getItemStorage().getLong(namedItem()) == itemAmount
                && fluids(machine).getFluidStorage().getLong(namedFluid()) == fluidAmount
                && items(machine).getItemStorage().getLong(new ItemStack(Items.DIAMOND)) == 31
                && fluids(machine).getFluidStorage().getLong(new FluidStack(Fluids.WATER, 1)) == 37;
    }
    private NativeProductionDedicatedState() {}
}
