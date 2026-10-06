package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient as FluidIngredient
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import net.neoforged.neoforge.fluids.FluidStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeInputNocache
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeInputNocache
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient

object MachineUtil {
    fun inputFluid(machine: WorkableMultiblockMachine, stack: FluidStack): Boolean =
        input(machine).fluid(stack).execute()

    fun inputItem(machine: WorkableMultiblockMachine, stack: ItemStack, amount: Long): Boolean =
        input(machine).item(stack, amount).execute()

    fun inputItem(machine: WorkableMultiblockMachine, ingredient: net.neoforged.neoforge.common.crafting.SizedIngredient, stack: ItemStack): Boolean =
        input(machine).item(ingredient).execute()

    fun input(machine: WorkableMultiblockMachine): InputBuilder = InputBuilder(machine)

    class InputBuilder internal constructor(private val machine: WorkableMultiblockMachine) {
        private val builder = GTRecipeBuilder.ofRaw()
        private var hasInput = false

        fun fluid(stack: FluidStack): InputBuilder {
            if (!stack.isEmpty) {
                builder.inputFluids(FluidIngredient.of(stack))
                hasInput = true
            }
            return this
        }

        fun fluid(ingredient: FluidIngredient): InputBuilder {
            if (!ingredient.ingredient().isEmpty) {
                builder.inputFluids(ingredient)
                hasInput = true
            }
            return this
        }

        fun item(stack: ItemStack): InputBuilder {
            if (!stack.isEmpty) {
                builder.inputItems(stack)
                hasInput = true
            }
            return this
        }

        fun item(stack: ItemStack, amount: Long): InputBuilder =
            if (amount <= 0 || stack.isEmpty) this else item(LongIngredient.create(stack, amount))

        fun item(ingredient: net.neoforged.neoforge.common.crafting.SizedIngredient): InputBuilder {
            if (!ingredient.ingredient().isEmpty) {
                builder.inputItems(ingredient)
                hasInput = true
            }
            return this
        }

        fun execute(): Boolean {
            if (!hasInput) return true
            val recipe = builder.build()
            if (matchRecipeInputNocache(machine, recipe)) {
                return handleRecipeInputNocache(machine, recipe)
            }
            return false
        }
    }

    fun outputItem(machine: WorkableMultiblockMachine, ingredient: net.neoforged.neoforge.common.crafting.SizedIngredient): Boolean {
        if (ingredient.ingredient().isEmpty) return true

        val recipe = GTRecipeBuilder.ofRaw().outputItems(ingredient).build()

        return RecipeRunnerHelper.matchRecipeOutput(machine, recipe) &&
            RecipeRunnerHelper.handleRecipeOutput(machine, recipe)
    }
}