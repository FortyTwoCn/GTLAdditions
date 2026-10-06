package com.gtladd.gtladditions.mixin.neoforge;

import com.gtladd.gtladditions.integration.xei.ILongFluidDisplay;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class, remap = false)
public abstract class FluidStackDisplayMixin implements ILongFluidDisplay {
    @Unique private long gtladditions$displayAmount = -1;

    @Shadow public abstract boolean isEmpty();
    @Shadow public abstract int getAmount();

    @Override
    public long gtladditions$getDisplayAmount() {
        return isEmpty() ? 0 : gtladditions$displayAmount >= 0 ? gtladditions$displayAmount : getAmount();
    }

    @Override
    public void gtladditions$setDisplayAmount(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative display amount");
        gtladditions$displayAmount = amount;
    }

    @Override
    public boolean gtladditions$hasDisplayAmount() {
        return gtladditions$displayAmount >= 0;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void gtladditions$copyDisplayAmount(CallbackInfoReturnable<FluidStack> cir) {
        FluidStack copy = cir.getReturnValue();
        if (gtladditions$hasDisplayAmount() && !copy.isEmpty()) {
            ((ILongFluidDisplay) (Object) copy).gtladditions$setDisplayAmount(gtladditions$displayAmount);
        }
    }

    @Inject(method = "setAmount", at = @At("TAIL"))
    private void gtladditions$resetDisplayAmount(int amount, CallbackInfo ci) {
        // Physical tank operations and copyWithAmount(int) must use their native quantity.
        gtladditions$displayAmount = -1;
    }

    @Inject(method = "containsFluid", at = @At("HEAD"), cancellable = true)
    private void gtladditions$containsExactAmount(FluidStack other, CallbackInfoReturnable<Boolean> cir) {
        ILongFluidDisplay otherDisplay = (ILongFluidDisplay) (Object) other;
        if (gtladditions$hasDisplayAmount() || otherDisplay.gtladditions$hasDisplayAmount()) {
            cir.setReturnValue(FluidStack.isSameFluidSameComponents((FluidStack) (Object) this, other)
                    && gtladditions$getDisplayAmount() >= otherDisplay.gtladditions$getDisplayAmount());
        }
    }

    @Inject(method = "isFluidStackIdentical", at = @At("HEAD"), cancellable = true)
    private void gtladditions$compareExactAmount(FluidStack other, CallbackInfoReturnable<Boolean> cir) {
        ILongFluidDisplay otherDisplay = (ILongFluidDisplay) (Object) other;
        if (gtladditions$hasDisplayAmount() || otherDisplay.gtladditions$hasDisplayAmount()) {
            cir.setReturnValue(FluidStack.isSameFluidSameComponents((FluidStack) (Object) this, other)
                    && gtladditions$getDisplayAmount() == otherDisplay.gtladditions$getDisplayAmount());
        }
    }
}
