package com.gtladd.gtladditions.mixin.gtceu.xei;

import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import mezz.jei.neoforge.platform.FluidHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = FluidHelper.class, priority = 2000, remap = false)
public abstract class FluidHelperMixin {
    /** @author Dragons @reason Preserve exact AE2/JEI fluid amounts on the native NeoForge API. */
    @Overwrite
    public FluidStack create(Holder<Fluid> fluid, long amount) {
        return LongFluidStack.create(fluid, amount);
    }

    /** @author Dragons @reason Preserve native data components as well as the long display amount. */
    @Overwrite
    public FluidStack create(Holder<Fluid> fluid, long amount, DataComponentPatch patch) {
        return LongFluidStack.create(fluid, amount, patch);
    }

    /** @author Dragons @reason Read the exact AE2/JEI fluid amount. */
    @Overwrite
    public long getAmount(FluidStack ingredient) {
        return LongFluidStack.getAmount(ingredient);
    }

    /** @author Dragons @reason Copy long amounts without changing the fluid identity. */
    @Overwrite
    public FluidStack copyWithAmount(FluidStack ingredient, long amount) {
        return LongFluidStack.copyWithAmount(ingredient, amount);
    }
}
