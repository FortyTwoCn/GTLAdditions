package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.recipe.GTLRecipeContents

/** Keep the legacy per-tier chance boost distinct from native item counts and chance denominators. */
fun GTRecipeBuilder.tierChancedOutput(stack: ItemStack, chance: Int, boost: Int): GTRecipeBuilder =
    GTLRecipeContents.chancedOutput(this, stack, chance, boost)

fun GTRecipeBuilder.tierChancedOutput(prefix: TagPrefix, material: Material, chance: Int, boost: Int): GTRecipeBuilder =
    GTLRecipeContents.chancedOutput(this, prefix, material, chance, boost)

fun GTRecipeBuilder.tierChancedOutput(prefix: TagPrefix, material: Material, count: Int, chance: Int, boost: Int): GTRecipeBuilder =
    GTLRecipeContents.chancedOutput(this, prefix, material, count, chance, boost)

fun GTRecipeBuilder.tierChancedInput(stack: ItemStack, chance: Int, boost: Int): GTRecipeBuilder =
    if (chance == 0 && boost == 0) notConsumable(stack) else GTLRecipeContents.chancedInput(this, stack, chance, boost)
