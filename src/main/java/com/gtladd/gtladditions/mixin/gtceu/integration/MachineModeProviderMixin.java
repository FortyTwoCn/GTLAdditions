package com.gtladd.gtladditions.mixin.gtceu.integration;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.integration.jade.provider.MachineModeProvider;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MachineModeProvider.class)
public abstract class MachineModeProviderMixin {
    @Inject(method = "write(Lcom/gregtechceu/gtceu/api/machine/MetaMachine;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$crossRecipeType(MetaMachine machine, CallbackInfoReturnable<CompoundTag> cir) {
        if (!(machine instanceof IRecipeLogicMachine logic) || !GTLAddRecipesTypes.isMultiRecipeType(logic.getRecipeType())) return;
        var types = new ListTag(); types.add(StringTag.valueOf(logic.getRecipeType().id.toString()));
        cir.getReturnValue().put("RecipeTypes", types);
        cir.getReturnValue().putInt("CurrentRecipeType", 0);
    }
}
