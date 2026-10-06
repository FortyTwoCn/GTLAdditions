package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import com.gtladd.gtladditions.api.machine.IThreadModifierMachine;
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.utils.RecipeCalculationHelper;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.google.common.primitives.Ints;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.recipe.*;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import net.minecraft.nbt.CompoundTag;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.function.BiPredicate;

/** Additions policies over Core's native paid-work ledger; never read an owner during logic construction. */
@Mixin(MultipleRecipesLogic.class)
public abstract class MultipleRecipesLogicMixin extends RecipeLogic {
    @Shadow(remap = false) public abstract ParallelMachine getParallel();
    @Shadow(remap = false) public abstract WorkableElectricMultiblockMachine getMachine();
    @Shadow(remap = false) public abstract BiPredicate<CompoundTag, com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine> getDataCheck();
    @Shadow(remap = false) protected abstract double getEuMultiplier();
    @Shadow(remap = false) protected abstract Iterator<GTRecipe> lookupRecipeIterator();
    @Shadow(remap = false) protected abstract int getMultipleThreads();

    @ModifyReturnValue(method = "getMultipleThreads", at = @At("RETURN"), remap = false, require = 1)
    private int gtladditions$threadCount(int original) {
        return getMachine() instanceof IThreadModifierMachine threaded ?
                Math.max(1, Ints.saturatedCast((long) original + threaded.getAdditionalThread())) : original;
    }

    @Inject(method = "collectRecipes", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$wirelessCollection(CallbackInfoReturnable<GTRecipe> result) {
        if (!(getMachine() instanceof IWirelessElectricMultiblockMachine owner)) return;
        var account = owner.getWirelessNetworkEnergyHandler();
        if (account != null) result.setReturnValue(gtladditions$collectPaidWireless(account));
    }

    @Unique
    private GTRecipe gtladditions$collectPaidWireless(IWirelessNetworkEnergyHandler account) {
        if (!account.isOnline()) return null;
        var machine = getRLMachine();
        var itemOutputs = new ObjectArrayList<Content>();
        var fluidOutputs = new ObjectArrayList<Content>();
        long remaining = (long) Math.max(1, getParallel().getMaxParallel()) * Math.max(1, getMultipleThreads());
        BigInteger total = BigInteger.ZERO;
        boolean consumed = false;
        var lock = (ILockRecipe) (Object) this;
        var iterator = lock.isLock() && lock.getLockRecipe() != null ? java.util.Collections.singleton(lock.getLockRecipe()).iterator() : lookupRecipeIterator();
        while (remaining > 0 && iterator.hasNext()) {
            var source = iterator.next();
            if (source == null || !RecipeRunnerHelper.matchRecipe(machine, source) ||
                    !RecipeHelper.checkConditions(source, this).isSuccess() ||
                    getDataCheck() != null && !getDataCheck().test(source.data, machine)) continue;
            long parallel = IParallelLogic.getMaxParallel(machine, source,
                    Math.min(remaining, Long.MAX_VALUE / IGTRecipe.of(source).getRealParallels()));
            if (parallel <= 0) continue;
            var selected = RecipeCalculationHelper.INSTANCE.multipleRecipe(source, parallel);
            var cost = BigDecimal.valueOf(source.duration * getEuMultiplier())
                    .multiply(new BigDecimal(BigInteger.valueOf(source.getInputEUt().getTotalEU()).multiply(BigInteger.valueOf(parallel)))).toBigInteger();
            var next = total.add(cost);
            if (next.compareTo(account.getMaxAvailableEnergy()) > 0) {
                if (!consumed) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                break;
            }
            RecipeChancePreview.output(selected, machine, selected.outputs, getChanceCaches(), false);
            if (!RecipeRunnerHelper.handleRecipeInput(machine, selected)) continue;
            consumed = true; remaining -= parallel; total = next;
            RecipeCalculationHelper.INSTANCE.collectOutputs(IParallelLogic.getRecipeOutputChance(machine, selected), itemOutputs, fluidOutputs);
            lastOriginRecipe = source.copy();
            if (lock.isLock() && lock.getLockRecipe() == null) lock.setLockRecipe(source);
        }
        return consumed ? RecipeCalculationHelper.INSTANCE.buildWirelessRecipe(itemOutputs, fluidOutputs, 20, total, GTRecipeTypes.DUMMY_RECIPES.value()) : null;
    }
}
