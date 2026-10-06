package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import org.gtlcore.gtlcore.api.recipe.NativeRecipeState
import java.math.BigInteger

/** Native GT recipe fields and codecs; the signed wireless amount lives in recipe data. */
open class WirelessGTRecipe(gtRecipe: GTRecipe, @JvmField val wirelessEuTickInputs: BigInteger?) :
    GTRecipe(
        gtRecipe.recipeType, gtRecipe.id,
        gtRecipe.inputs, gtRecipe.outputs, gtRecipe.tickInputs, gtRecipe.tickOutputs,
        gtRecipe.inputChanceLogics, gtRecipe.outputChanceLogics,
        gtRecipe.tickInputChanceLogics, gtRecipe.tickOutputChanceLogics,
        gtRecipe.conditions, gtRecipe.ingredientActions,
        gtRecipe.data.copy().apply {
            if (wirelessEuTickInputs == null || wirelessEuTickInputs.signum() == 0) {
                remove(NativeWirelessRecipeState.KEY)
            } else {
                putByteArray(NativeWirelessRecipeState.KEY, wirelessEuTickInputs.toByteArray())
            }
        },
        gtRecipe.duration, gtRecipe.parallels, gtRecipe.subtickParallels, gtRecipe.batchParallels,
        gtRecipe.recipeCategory, gtRecipe.groupColor, gtRecipe.keepSpoilingProgress
    ), IWirelessGTRecipe {

    init {
        ocLevel = gtRecipe.ocLevel
        outputModifier = gtRecipe.outputModifier
    }

    override fun getWirelessEuTickInputs(): BigInteger? = wirelessEuTickInputs

    override fun hasTick(): Boolean = NativeRecipeState.tickOverride(this)
        ?: (wirelessEuTickInputs?.let { it.signum() != 0 } == true ||
            NativeWirelessRecipeState.read(this, NativeWirelessRecipeState.TOTAL_KEY)?.signum()?.let { it != 0 } == true || super.hasTick())

    override fun copy(modifier: ContentModifier, modifyDuration: Boolean): WirelessGTRecipe =
        WirelessGTRecipe(super.copy(modifier, modifyDuration), wirelessEuTickInputs)

    override fun copyWithoutTicks(): WirelessGTRecipe {
        val copy = super.copyWithoutTicks()
        copy.data.remove(NativeWirelessRecipeState.TOTAL_KEY)
        copy.data.remove(NativeWirelessRecipeState.PAID_KEY)
        return WirelessGTRecipe(copy, null)
    }
}
