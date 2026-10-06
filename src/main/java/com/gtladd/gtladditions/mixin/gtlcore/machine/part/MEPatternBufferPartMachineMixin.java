package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine;
import com.gtladd.gtladditions.integration.ae2.MEBufferPatternHelperExtensions;
import appeng.api.crafting.IPatternDetails;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MEPatternBufferPartMachine.class)
public abstract class MEPatternBufferPartMachineMixin extends MEPatternBufferPartMachineBase {
    @Shadow(remap = false) @Final protected InternalSlot[] internalInventory;
    @Shadow(remap = false) protected boolean keepByProduct;
    protected MEPatternBufferPartMachineMixin(BlockEntityCreationInfo info, IO io) { super(info, io); }
    @Inject(method = "getRealPattern", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$publishFOAPattern(int index, ItemStack stack, CallbackInfoReturnable<IPatternDetails> cir) {
        if (!stack.isEmpty() && (Object) this instanceof MESuperPatternBufferPartMachine machine && machine.isFOAModeEnabled()) {
            cir.setReturnValue(MEBufferPatternHelperExtensions.processForgeOfTheAntichristPattern(realPatternHelper, stack,
                    internalInventory[index].getCacheManager()::setCircuitCache, getLevel(), keepByProduct, machine.getFOAPatternOutputMultiplier()));
        }
    }
}
