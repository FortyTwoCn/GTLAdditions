package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.RecipeOutput
import java.util.function.Consumer

object InterStellar {
    fun init(provider: RecipeOutput) {
        GTLAddRecipesTypes.INTER_STELLAR.recipeBuilder(GTLAdditions.id("inter_stellar"))
            .circuitMeta(1)
            .duration(800)
            .EUt(GTValues.V[GTValues.UXV])
            .save(provider)
    }
}