package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import com.gtladd.gtladditions.integration.jade.NativeWirelessEnergyTooltip;
import org.gtlcore.gtlcore.integration.jade.NativeRecipeLogicTooltip;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.integration.jade.provider.RecipeLogicProvider;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Add the wireless line to Core's native status provider without replacing its tooltip hook. */
@Mixin(value = RecipeLogicProvider.class, priority = 1500)
public abstract class RecipeLogicProviderMixin {
    @Inject(method = "write(Lcom/gregtechceu/gtceu/api/machine/trait/recipe/RecipeLogic;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$wirelessEnergy(RecipeLogic logic, CallbackInfoReturnable<CompoundTag> cir) {
        var active = logic.getLastUnrolledRecipe();
        var recipe = active instanceof IWirelessGTRecipe wireless ? wireless :
                logic.getLastRecipe() instanceof IWirelessGTRecipe wireless ? wireless : null;
        if (recipe == null) return;
        var amount = recipe.getWirelessEuTickInputs();
        if (amount == null || amount.signum() == 0) return;
        var data = cir.getReturnValue();
        data.putByteArray("wirelessTickInputs", amount.toByteArray());
        NativeRecipeLogicTooltip.writeAlternateEnergy(data, NativeWirelessEnergyTooltip.energyLine(amount),
                logic.getMachine().getLevel().registryAccess());
    }
}
