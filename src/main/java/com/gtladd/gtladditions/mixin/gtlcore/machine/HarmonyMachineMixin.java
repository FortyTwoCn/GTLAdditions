package com.gtladd.gtladditions.mixin.gtlcore.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gtladd.gtladditions.api.machine.IHarmonyMachineAccessor;
import com.gtladd.gtladditions.api.machine.wireless.NativeWirelessEnergyAccount;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.api.recipe.NativeContentState;
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;
import org.gtlcore.gtlcore.utils.MachineIO;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.UUID;

@Mixin(HarmonyMachine.class)
@Implements(@Interface(iface = IHarmonyMachineAccessor.class, prefix = "gtladditions$"))
public abstract class HarmonyMachineMixin extends NoEnergyMultiblockMachine {
    @Shadow(remap = false) private int oc;
    @Shadow(remap = false) private long hydrogen;
    @Shadow(remap = false) private long helium;
    @Shadow(remap = false) public abstract long getStartupEnergy();
    @Shadow(remap = false) public abstract UUID getWirelessOwner();
    @Unique private final NotifiableItemStackHandler gtladditions$machineStorage = attachTrait(
            new NotifiableItemStackHandler(1, IO.NONE, IO.BOTH, size -> new CustomItemStackHandler(size) {
                @Override public int getSlotLimit(int slot) { return 1; }
            }));
    public HarmonyMachineMixin(BlockEntityCreationInfo info) { super(info); }

    @Unique public boolean gtladditions$consumeCosmosStartup() {
        if (oc <= 0 || hydrogen < HarmonyMachine.STARTUP_GAS || helium < HarmonyMachine.STARTUP_GAS ||
                !NativeWirelessEnergyAccount.transfer(getWirelessOwner(), -getStartupEnergy(), this)) return false;
        hydrogen -= HarmonyMachine.STARTUP_GAS; helium -= HarmonyMachine.STARTUP_GAS; setChanged(); return true;
    }
    @Unique public boolean gtladditions$consumeAstralStartup() {
        if (hydrogen < HarmonyMachine.STARTUP_GAS || helium < HarmonyMachine.STARTUP_GAS ||
                !NativeWirelessEnergyAccount.transfer(getWirelessOwner(), -Long.MAX_VALUE, this)) return false;
        hydrogen -= HarmonyMachine.STARTUP_GAS; helium -= HarmonyMachine.STARTUP_GAS; setChanged(); return true;
    }
    @Unique public int gtladditions$getHarmonyDuration() { return 4800 >> oc; }
    @Unique public GTRecipe gtladditions$applyCreateDataOutput(GTRecipe source) {
        var copy = source.copy(); RecipeExtensionCopier.copy(source, copy);
        var stack = gtladditions$machineStorage.getStackInSlot(0);
        if (stack.isEmpty() || !"gtladditions:create_data".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return copy;
        copy.outputs.clear();
        copy.outputs.put(FluidRecipeCapability.CAP, List.of(NativeContentState.create(
                LongFluidIngredient.create(GTLMaterials.RawStarMatter.getFluid(FluidStorageKeys.PLASMA), 15_728_640L),
                10000, 10000, 0, null, null)));
        return copy;
    }
    @ModifyReturnValue(method = "recipeModifier", at = @At("RETURN"), remap = false, require = 1)
    private static ModifierFunction gtladditionsInternal$createDataOutput(ModifierFunction original, MetaMachine machine, GTRecipe source) {
        return original.andThen(recipe -> ((IHarmonyMachineAccessor) machine).applyCreateDataOutput(recipe));
    }
    @Override public void buildMainUI(ParentWidget<?> main, PosGuiData data, PanelSyncManager sync, UISettings settings) {
        super.buildMainUI(main, data, sync, settings);
        sync.registerSlotGroup(new SlotGroup("harmonyCreateData", 1));
        main.child(new ItemSlot().name("harmonyCreateData").right(3).bottom(3)
                .slot(new ModularSlot(gtladditions$machineStorage.storage, 0).slotGroup("harmonyCreateData")));
    }
    /** @author Dragons @reason Keep Additions' ten-billion-millibucket buffer cap on native gas transfers. */
    @Overwrite protected void StartupUpdate() {
        if (getOffsetTimer() % 20 != 0) return;
        oc = 0;
        if (hydrogen < 10_000_000_000L && MachineIO.inputFluid(this, GTMaterials.Hydrogen.getFluid(100_000_000))) hydrogen += 100_000_000L;
        if (helium < 10_000_000_000L && MachineIO.inputFluid(this, GTMaterials.Helium.getFluid(100_000_000))) helium += 100_000_000L;
        for (int circuit = 4; circuit >= 1; circuit--) if (MachineIO.notConsumableCircuit(this, circuit)) oc = circuit;
        setChanged();
    }
}
