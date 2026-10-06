package com.gtladd.gtladditions.mixin.ae2;

import net.neoforged.neoforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import tamaized.ae2jeiintegration.integration.modules.jei.converters.FluidIngredientConverter;
import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FluidIngredientConverter.class)
public abstract class FluidIngredientConverterMixin {

    /**
     * @author Dragons
     * @reason Support Long
     */
    @Overwrite(remap = false)
    public @Nullable FluidStack getIngredientFromStack(GenericStack stack) {
        AEKey var3 = stack.what();
        if (var3 instanceof AEFluidKey fluidKey) {
            return LongFluidStack.copyWithAmount(fluidKey.toStack(1), Math.max(1L, stack.amount()));
        } else {
            return null;
        }
    }

    /**
     * @author Dragons
     * @reason Support Long
     */
    @Overwrite(remap = false)
    public @Nullable GenericStack getStackFromIngredient(FluidStack ingredient) {
        AEFluidKey key = AEFluidKey.of(ingredient);
        return key == null ? null : new GenericStack(key, LongFluidStack.getAmount(ingredient));
    }
}