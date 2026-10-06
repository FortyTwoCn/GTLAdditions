package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.utils.TagUtil
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient

/** Uses the native material registry without requiring tags to have loaded. */
object TempChemicalHelper {
    fun getMaterialFromFluid(fluid: Fluid): Material? = GTRegistries.MATERIALS.firstOrNull { material ->
        material.getProperty(PropertyKey.FLUID)?.let { property ->
            FluidStorageKey.allKeys().any { property.get(it) === fluid }
        } ?: false
    }

    fun getMaterialFromIngredient(ingredient: SizedFluidIngredient): Material? {
        val predicate = LongFluidIngredient.matchingIngredient(ingredient)
        if (predicate is TagFluidIngredient) {
            return GTRegistries.MATERIALS.firstOrNull { material ->
                material.getProperty(PropertyKey.FLUID)?.let { property ->
                    FluidStorageKey.allKeys().any { key ->
                        property.get(key)?.let { fluid ->
                            predicate.tag() == TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluid).path)
                        } ?: false
                    }
                } ?: false
            }
        }
        return predicate.stacks.firstOrNull()?.fluid?.let(::getMaterialFromFluid)
    }
}
