package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;
import com.gtladd.gtladditions.integration.ae2.SuperPatternDataStick;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MEPatternBufferPartMachineBase.class)
public abstract class MEPatternBufferPartMachineBaseMixin {
    @Inject(method = "onDataStickUse", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$clearSuperBinding(Player player, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && !player.level().isClientSide) SuperPatternDataStick.clear(stack);
    }
}
