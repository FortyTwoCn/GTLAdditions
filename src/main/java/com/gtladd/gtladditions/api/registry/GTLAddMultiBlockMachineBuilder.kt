package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.registry.registrate.builder.MultiblockMachineBuilder
import com.gregtechceu.gtceu.utils.FormattingUtil
import brachy.modularui.api.drawable.Text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.util.function.Supplier

/** Helpers on the native builder; GT owns its block, item and block-entity factories. */
object GTLAddMultiBlockMachineBuilder {
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.dynamicCasingModel(
        casing: net.minecraft.resources.ResourceLocation,
        overlay: net.minecraft.resources.ResourceLocation,
        render: Supplier<com.gregtechceu.gtceu.client.renderer.machine.DynamicRender<*, *>>
    ): MultiblockMachineBuilder<M> {
        workableCasingModel(casing, overlay)
        return model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(casing, overlay)
            .andThen(java.util.function.Consumer<com.gregtechceu.gtceu.data.model.builder.MachineModelBuilder<net.neoforged.neoforge.client.model.generators.BlockModelBuilder>> {
                it.addDynamicRenderer(render)
            }))
    }

    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.allRotation(): MultiblockMachineBuilder<M> =
        rotationState(RotationState.ALL)
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.nonYAxisRotation(): MultiblockMachineBuilder<M> =
        rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false)
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.noneRotation(): MultiblockMachineBuilder<M> =
        rotationState(RotationState.NONE).allowExtendedFacing(false).allowFlip(false)
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextKey(vararg text: Component): MultiblockMachineBuilder<M> =
        tooltips(*text)
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextMaxParallels(parallel: Any): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.multiblock.max_parallel", parallel))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextRecipeTypes(vararg types: Supplier<GTRecipeType>): MultiblockMachineBuilder<M> =
        tooltips(Supplier {
            val names = types.map { Component.translatable(it.get().id.toLanguageKey("recipe_type")) }.toTypedArray()
            Component.translatable("gtceu.machine.available_recipe_map_${types.size}.tooltip", *names)
        })
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextCoilParallel(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.multiblock.coil_parallel"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextLaser(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.multiblock.laser.tooltip"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipOnlyTextLaser(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.multiblock.only.laser.tooltip"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextMultiRecipes(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.machine.multiple_recipes.tooltip"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextParallelHatch(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.multiblock.parallelizable.tooltip"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.tooltipTextPerfectOverclock(): MultiblockMachineBuilder<M> =
        tooltips(Component.translatable("gtceu.machine.perfect_oc"))
    fun <M : MultiblockControllerMachine> MultiblockMachineBuilder<M>.coilParallelDisplay(): MultiblockMachineBuilder<M> =
        additionalDisplay { controller, _ ->
            if (controller is CoilWorkableElectricMultiblockMachine && controller.isFormed) {
                val parallels = Math.pow(2.0, controller.coilType.coilTemperature.toDouble() / 900).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
                listOf(Text.of(Component.translatable("gtceu.multiblock.parallel",
                    Component.literal(FormattingUtil.formatNumbers(parallels)).withStyle(ChatFormatting.DARK_PURPLE))
                    .withStyle(ChatFormatting.GRAY)).asWidget())
            } else emptyList()
        }
}
