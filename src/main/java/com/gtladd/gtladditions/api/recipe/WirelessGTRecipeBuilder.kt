package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import net.minecraft.resources.ResourceLocation
import java.math.BigInteger

class WirelessGTRecipeBuilder : GTRecipeBuilder {

    @JvmField
    var wirelessEut: BigInteger? = null

    constructor(id: ResourceLocation, recipeType: GTRecipeType) : super(id, recipeType)

    constructor(toCopy: GTRecipe, recipeType: GTRecipeType) : super(toCopy, recipeType)
    {
        wirelessEut = (toCopy as? IWirelessGTRecipe)?.getWirelessEuTickInputs()
    }

    fun setWirelessEut(wirelessEut: BigInteger?): WirelessGTRecipeBuilder {
        this.wirelessEut = wirelessEut
        return this
    }

    fun output(cap: RecipeCapability<*>, contents: List<Content>): WirelessGTRecipeBuilder {
        output[cap] = ArrayList(contents)
        return this
    }

    override fun duration(duration: Int): WirelessGTRecipeBuilder {
        this.duration = duration
        return this
    }

    override fun build(): WirelessGTRecipe = WirelessGTRecipe(super.build(), wirelessEut)

    override fun copy(id: String): WirelessGTRecipeBuilder = copy(GTCEu.id(id))

    override fun copy(id: ResourceLocation): WirelessGTRecipeBuilder {
        val native = super.copy(id)
        val result = WirelessGTRecipeBuilder(native.build(), recipeType)
        result.id = id
        result.chance = native.chance
        result.maxChance = maxChance
        result.perTick = native.perTick
        result.onSave = native.onSave
        result.recipeCategory = native.recipeCategory
        result.keepSpoilingProgress = native.keepSpoilingProgress
        result.wirelessEut = wirelessEut
        return result
    }

    fun copy(): WirelessGTRecipeBuilder = copy(id)

    fun buildRawRecipe(): WirelessGTRecipe = build()

    companion object {
        fun ofRaw(): WirelessGTRecipeBuilder = ofRaw(GTRecipeTypes.DUMMY_RECIPES.value())

        fun ofRaw(recipeType: GTRecipeType): WirelessGTRecipeBuilder = WirelessGTRecipeBuilder(GTCEu.id("raw"), recipeType)
    }
}
