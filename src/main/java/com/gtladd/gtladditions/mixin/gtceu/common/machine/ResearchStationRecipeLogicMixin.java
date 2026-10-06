package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;
import com.gtladd.gtladditions.api.machine.ICloudTeamBindable;
import com.gtladd.gtladditions.utils.CloudNetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Upload only after the native object holder has accepted the finished research item. */
@Mixin(ResearchStationMachine.ResearchStationRecipeLogic.class)
public abstract class ResearchStationRecipeLogicMixin {
    @Shadow(remap = false) public abstract ResearchStationMachine getMachine();
    @Inject(method = "handleRecipeIO", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$uploadResearchData(GTRecipe recipe, IO io, CallbackInfoReturnable<ActionResult> cir) {
        if (io != IO.OUT || !cir.getReturnValue().isSuccess()) return;
        var machine = getMachine();
        if (!(machine instanceof ICloudTeamBindable bindable) || bindable.getUUID() == null || machine.isRemote()) return;
        var holder = machine.getObjectHolder();
        if (holder == null) return;
        var stack = holder.getDataItem(false);
        if (!stack.isEmpty() && CloudNetworkManager.INSTANCE.tryUploadResearchData(stack, bindable.getUUID()))
            holder.setDataItem(net.minecraft.world.item.ItemStack.EMPTY);
    }
}
