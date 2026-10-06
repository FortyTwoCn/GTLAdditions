package com.gtladd.gtladditions.mixin.gtceu.registry;

import com.gtladd.gtladditions.api.registry.NativeQueuedMachines;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorkableMultiblockMachine.class)
public abstract class DefaultRecipeLogicMixin {
    @ModifyArg(method = "<init>(Lcom/gregtechceu/gtceu/api/blockentity/BlockEntityCreationInfo;)V",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/multiblock/WorkableMultiblockMachine;<init>(Lcom/gregtechceu/gtceu/api/blockentity/BlockEntityCreationInfo;Lcom/gregtechceu/gtceu/api/machine/trait/recipe/RecipeLogic;)V"),
            index = 1, remap = false, require = 1)
    private static RecipeLogic gtladd$constructConfiguredDefaultLogic(BlockEntityCreationInfo info, RecipeLogic original) {
        return NativeQueuedMachines.defaultRecipeLogic(info, original);
    }
}
