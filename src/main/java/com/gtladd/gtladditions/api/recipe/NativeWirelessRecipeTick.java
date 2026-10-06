package com.gtladd.gtladditions.api.recipe;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.wireless.NativeWirelessEnergyAccount;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import java.util.function.Supplier;

/** Reserve signed wireless EU for this native tick and refund it if native IO/conditions reject the tick. */
public final class NativeWirelessRecipeTick {
    private NativeWirelessRecipeTick() {}

    public static ActionResult run(Object machine, GTRecipe recipe, Supplier<ActionResult> nativeTick) {
        if (!(recipe instanceof IWirelessGTRecipe wireless)) return nativeTick.get();
        var logic = ((com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine) machine).getRecipeLogic();
        var amount = NativeWirelessRecipeState.paymentAmount(recipe, logic.getProgress(),
                logic.getDuration() > 0 ? logic.getDuration() : recipe.duration);
        if (amount == null || amount.signum() == 0) return nativeTick.get();
        if (!(machine instanceof IWirelessElectricMultiblockMachine owner)) return noEnergy();
        try (var reports = NativeWirelessEnergyAccount.deferTransferReports((MetaMachine) machine)) {
            return payTick(owner, recipe, amount, logic, nativeTick);
        }
    }

    private static ActionResult payTick(IWirelessElectricMultiblockMachine owner, GTRecipe recipe,
            java.math.BigInteger amount, com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic logic,
            Supplier<ActionResult> nativeTick) {
        var account = owner.getWirelessNetworkEnergyHandler();
        if (account == null || !account.isOnline() || !account.consumeEnergy(amount)) return noEnergy();
        boolean accepted = false;
        try {
            var result = nativeTick.get();
            accepted = result.isSuccess();
            if (accepted) {
                NativeWirelessRecipeState.recordPayment(recipe, amount);
                if (logic instanceof org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic multiple)
                    multiple.syncPendingRecipeData(recipe);
                logic.markAsChanged();
            }
            return result;
        } finally {
            if (!accepted && !account.consumeEnergy(amount.negate())) {
                throw new IllegalStateException("Wireless account refused to refund a rejected native recipe tick");
            }
        }
    }

    private static ActionResult noEnergy() {
        return ActionResult.fail(RecipeResult.FAIL_NO_ENOUGH_EU_IN.reason(), EURecipeCapability.CAP, IO.IN);
    }
}
