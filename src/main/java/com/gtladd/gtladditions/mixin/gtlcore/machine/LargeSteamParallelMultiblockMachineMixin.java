package com.gtladd.gtladditions.mixin.gtlcore.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;
import com.gtladd.gtladditions.common.machine.hatch.HugeSteamHatchPartMachine;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import com.gtladd.gtladditions.api.gui.NativeHatchUI;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.gtlcore.gtlcore.api.machine.trait.ExactSteamEnergyRecipeHandler;
import org.gtlcore.gtlcore.api.machine.trait.SteamEnergyConversion;
import org.gtlcore.gtlcore.api.recipe.CoreRecipeModifiers;
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;
import org.gtlcore.gtlcore.common.machine.multiblock.part.LargeSteamHatchPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.steam.LargeSteamParallelMultiblockMachine;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/** Huge steam keeps its 125 EU/mB ratio and one-tick recipes using the native, exact handler. */
@Mixin(LargeSteamParallelMultiblockMachine.class)
public abstract class LargeSteamParallelMultiblockMachineMixin extends WorkableMultiblockMachine {
    @Shadow(remap = false) private boolean isOC;
    @Shadow(remap = false) private int amountOC;
    @Shadow(remap = false) @Final private int max_parallels;
    @Unique @SyncToClient private boolean gtladditions$isHuge;
    public LargeSteamParallelMultiblockMachineMixin(BlockEntityCreationInfo info) { super(info); }

    @Inject(method = "formStructure", at = @At("HEAD"), remap = false, require = 1)
    private void gtladditions$resetHatch(String name, CallbackInfo ci) { gtladditions$isHuge = false; }
    @Inject(method = "formStructure", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$syncHatch(String name, CallbackInfo ci) { syncDataHolder.markClientSyncFieldDirty("gtladditions$isHuge"); }
    @Inject(method = "invalidateStructure", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$invalidateHugeHatch(String name, CallbackInfo ci) {
        gtladditions$isHuge = false;
        syncDataHolder.markClientSyncFieldDirty("gtladditions$isHuge");
    }
    /** @author Dragons @reason Discover the additional native huge steam part through Core's extension point. */
    @Overwrite protected boolean isSteamInputPart(MetaMachine part) {
        boolean matches = part instanceof SteamHatchPartMachine || part instanceof LargeSteamHatchPartMachine || part instanceof HugeSteamHatchPartMachine;
        if (matches) gtladditions$isHuge = part instanceof HugeSteamHatchPartMachine;
        return matches;
    }
    /** @author Dragons @reason Use exact rational accounting for huge steam. */
    @Overwrite protected ExactSteamEnergyRecipeHandler createSteamEnergyHandler(NotifiableFluidTank tank) {
        return gtladditions$isHuge ? new ExactSteamEnergyRecipeHandler(tank, () -> new SteamEnergyConversion.Rate(BigInteger.ONE, BigInteger.valueOf(125)))
                : new ExactSteamEnergyRecipeHandler(tank, () -> isOC ? amountOC : 0);
    }
    /** @author Dragons @reason Preserve the public EU per mB conversion rate. */
    @Overwrite public double getConversionRate() { return gtladditions$isHuge ? 125 : .5 * (isOC ? Math.pow(3, amountOC) : 1); }

    /** @author Dragons @reason Keep huge hatch parallel limits, duration and oven energy reduction. */
    @Overwrite public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe, double durationMultiplier) {
        if (!(machine instanceof LargeSteamParallelMultiblockMachineMixin steam)) return ModifierFunction.NULL;
        if (recipe.getInputEUt().getTotalEU() > (steam.gtladditions$isHuge ? 512 : steam.isOC ? 128 : 32)) return ModifierFunction.NULL;
        final double multiplier = steam.gtladditions$isHuge ? 0 : durationMultiplier / (steam.isOC ? Math.pow(2, steam.amountOC) : 1);
        return input -> {
            var result = CoreRecipeModifiers.accurateParallel(machine, input, steam.max_parallels, false).getFirst();
            if (result == null) return null;
            result = result.copy(); result.data = result.data.copy();
            result.duration = (int) Math.max(1, input.duration * multiplier);
            result.data.putInt(ExactSteamEnergyRecipeHandler.RECIPE_OC, steam.isOC ? steam.amountOC : 0);
            if (machine.getDefinition() == MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_STEAM_OVEN.get()) {
                long energy = Math.max(1, BigDecimal.valueOf(result.getInputEUt().getTotalEU()).multiply(new BigDecimal("0.01")).longValue());
                EURecipeCapability.putEUContent(result.tickInputs, new EnergyStack(energy));
            }
            return result;
        };
    }
    @Override public Reference2IntMap<RecipeCapability<?>> getOutputLimits() {
        var limits = new Reference2IntOpenHashMap<>(super.getOutputLimits());
        if (getDefinition() == MultiBlockMachineA.LARGE_STEAM_MACERATOR.get()) limits.put(ItemRecipeCapability.CAP, gtladditions$isHuge ? 3 : 1);
        return limits;
    }
    @Inject(method = "buildMainUI", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$hugeStatus(ParentWidget<?> main, PosGuiData data, PanelSyncManager sync, UISettings settings, CallbackInfo ci) {
        main.child(NativeHatchUI.componentList("hugeSteamStatus", sync, () -> gtladditions$isHuge
                ? List.of(Component.translatable("gtceu.multiblock.steam_duration_modify")) : List.of(), 25).bottom(0));
    }
}
