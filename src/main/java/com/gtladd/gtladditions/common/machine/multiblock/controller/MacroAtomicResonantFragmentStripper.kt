package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.base.Predicate
import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STAR_CORE_STRIPPER
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.ELEMENT_COPYING_RECIPES
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

class MacroAtomicResonantFragmentStripper(holder: BlockEntityCreationInfo) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder, MacroAtomicResonantFragmentStripperLogic()),
    IAstralArrayInteractionMachine {

    override fun modifyDrops(drops: MutableList<net.minecraft.world.item.ItemStack>) {
        super.modifyDrops(drops)
        addAstralArrayDrops(drops)
    }

    @field:SaveField
    override var astralArrayCount: Int = 0

    @field:SaveField
    private var parallelAmount: Long = 1

    fun getRealParallel(): Long = parallelAmount

    override fun needConfirmMEStock(): Boolean = true

    override fun getMaxParallel(): Int = Ints.saturatedCast(getRealParallel())


    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        parallelAmount = calculateParallelAmount(astralArrayCount, this.coilType.coilTemperature)
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("MacroAtomicResonantFragmentStripperStatus", syncManager, {
            mutableListOf<Component>().also(::collectMacroAtomicResonantFragmentStripperStatusText)
        }, 130)
    )

    private fun collectMacroAtomicResonantFragmentStripperStatusText(textList: MutableList<Component>) {
        
        if (isFormed) {
            textList.add(
                "tooltip.gtladditions.astral_array_count".toComponent("$astralArrayCount / $MAX_ASTRAL_ARRAY_COUNT".literal.withStyle(ChatFormatting.GOLD))
            )
        }
    }

    override fun addParallelDisplay(textList: MutableList<Component>) {
        if (parallelAmount > 1) {
            textList.add(
                "gtceu.multiblock.parallel".toComponent(FormattingUtil.formatNumbers(parallelAmount).literal.withStyle(ChatFormatting.DARK_PURPLE)).withStyle(ChatFormatting.GRAY)
            )
        }
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    .withStyle(ChatFormatting.GOLD)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
            parallelAmount = calculateParallelAmount(astralArrayCount, this.coilType.coilTemperature)
        }
        return actualIncrease
    }

    companion object {
        const val MAX_ASTRAL_ARRAY_COUNT = 256

        val FAIL_NO_ASTRAL_ARRAY: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.no_astral_array".toComponent
        )

        val FRAGMENT_STRIPPER = Predicate { machine: IRecipeLogicMachine ->
            if (machine !is MacroAtomicResonantFragmentStripper) return@Predicate false

            when (machine.recipeType) {
                STAR_CORE_STRIPPER -> {
                    (machine.coilType.coilTemperature >= 21600).also { passed ->
                        if (!passed) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                    }
                }
                ELEMENT_COPYING_RECIPES -> {
                    val hasEternityCoil = machine.coilType.coilTemperature >= 96000
                    val hasEnoughAstralArrays = machine.astralArrayCount >= MAX_ASTRAL_ARRAY_COUNT
                    when {
                        !hasEternityCoil -> {
                            RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                            false
                        }
                        !hasEnoughAstralArrays -> {
                            RecipeResult.of(machine, FAIL_NO_ASTRAL_ARRAY)
                            false
                        }
                        else -> true
                    }
                }
                else -> false
            }
        }

        /**
         * Formula: parallelMultiplier = 2^(6 + 10*((astralArrayCount - 1)/184)^2)
         */
        fun calculateParallelAmount(count: Int, temperature: Int): Long {
            val base = (1536L + max(temperature - 21600, 0) / 1200 * 300)
            if (count == 0) return base
            val normalized = (count - 1) / 184.0
            val exponent = 6 + 10 * normalized * normalized
            return (base * 2.0.pow(exponent)).roundToLong()
        }

        class MacroAtomicResonantFragmentStripperLogic() : GTLAddMultipleRecipesLogic(FRAGMENT_STRIPPER) {
            init {
                this.setReduction(4.0, 1.0)
            }

            override fun getMachine(): MacroAtomicResonantFragmentStripper = super.getMachine() as MacroAtomicResonantFragmentStripper

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                val getParallelLimitForRecipe: (GTRecipe) -> Long =
                    if (getMachine().recipeType == STAR_CORE_STRIPPER.get()) {
                        { getMachine().getRealParallel() }
                    } else {
                        { Long.MAX_VALUE }
                    }

                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe,
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}