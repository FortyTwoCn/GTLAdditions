package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class HelioFusionExoticizer(holder: BlockEntityCreationInfo, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(holder, HelioFusionExoticizerLogic()) {


    override fun getRecipeLogic(): HelioFusionExoticizerLogic = super<ForgeOfTheAntichristModuleBase>.getRecipeLogic() as HelioFusionExoticizerLogic

    override fun addParallelDisplay(textList: MutableList<Component>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class HelioFusionExoticizerLogic(
            
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(BEFORE_WORKING) {
            init {
                this.setReduction(0.5, 1.0)
            }

            override fun getMachine(): HelioFusionExoticizer = super.getMachine() as HelioFusionExoticizer

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                if (recipes.isEmpty()) return null

                recipes.first().let { recipe ->
                    val parallel = getMaxParallel(recipe, Long.MAX_VALUE)
                    if (parallel > 0) {
                        return ParallelData(
                            listOf(
                                copyAndModifyRecipe(
                                    recipe,
                                    ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)
                                )
                            ),
                            longArrayOf(parallel)
                        )
                    }
                }

                return null
            }

            override fun lookupRecipeSet(): Set<GTRecipe> {
                lockRecipe?.let {
                    return if (matchesSourceRecipe(it)) setOf(it) else setOf()
                }

                return machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe).asSequence().firstOrNull()
                    ?.also {
                        isLock = true
                        lockRecipe = it
                    }
                    ?.let { setOf(it) } ?: setOf()
            }

            override fun enableModify(recipeType: GTRecipeType): Boolean = true
        }
    }
}