package com.gtladd.gtladditions.integration.xei

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack

/** Native NeoForge fluid stacks with an exact, transient AE2/JEI display amount. */
object LongFluidStack {
    @JvmStatic
    @JvmOverloads
    fun create(fluid: Holder<Fluid>, amount: Long, patch: DataComponentPatch = DataComponentPatch.EMPTY): FluidStack {
        require(amount >= 0) { "Negative fluid amount: $amount" }
        if (amount == 0L || fluid.value() === Fluids.EMPTY) return FluidStack.EMPTY
        val stack = FluidStack(fluid, amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), patch)
        (stack as ILongFluidDisplay).`gtladditions$setDisplayAmount`(amount)
        return stack
    }

    @JvmStatic
    fun getAmount(stack: FluidStack): Long =
        (stack as ILongFluidDisplay).`gtladditions$getDisplayAmount`()

    @JvmStatic
    fun copyWithAmount(stack: FluidStack, amount: Long): FluidStack {
        require(amount >= 0) { "Negative fluid amount: $amount" }
        return if (stack.isEmpty) FluidStack.EMPTY else create(stack.fluidHolder, amount, stack.componentsPatch)
    }
}
