package com.gtladd.gtladditions.common.data

import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level

data class DataModuleBindingRecord(
    val sourcePos: BlockPos,
    val sourceDimension: String,
    val bindingType: ResourceLocation
) {
    fun getSourceName(): Component = bindingType.toLanguageKey("block").toComponent

    companion object {
        private const val NBT_SOURCE_POS = "BindingSourcePos"
        private const val NBT_SOURCE_DIMENSION = "BindingSourceDimension"
        private const val NBT_BINDING_TYPE = "BindingType"

        fun read(stack: ItemStack): DataModuleBindingRecord? {
            val tag = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return null
            if (!tag.contains(NBT_SOURCE_POS, Tag.TAG_LONG.toInt())) return null
            val dimension = tag.getString(NBT_SOURCE_DIMENSION).takeIf { it.isNotBlank() } ?: return null
            val type = ResourceLocation.tryParse(tag.getString(NBT_BINDING_TYPE)) ?: return null
            return DataModuleBindingRecord(
                BlockPos.of(tag.getLong(NBT_SOURCE_POS)),
                dimension,
                type
            )
        }

        fun write(stack: ItemStack, source: IWirelessBindableSource<*>, level: Level) {
            val pos = source.getBlockPos()
            CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                tag.putLong(NBT_SOURCE_POS, pos.asLong())
                tag.putString(NBT_SOURCE_DIMENSION, level.dimension().location().toString())
                tag.putString(NBT_BINDING_TYPE, source.bindingType.toString())
                tag.remove("BindingName")
            }
        }

        fun clear(stack: ItemStack) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                tag.remove(NBT_SOURCE_POS)
                tag.remove(NBT_SOURCE_DIMENSION)
                tag.remove(NBT_BINDING_TYPE)
                tag.remove("BindingName")
            }
        }

        fun hasRecord(stack: ItemStack): Boolean = read(stack) != null
    }
}
