package com.gtladd.gtladditions.integration.ae2;

import com.gregtechceu.gtceu.common.data.item.GTDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/** Super and ordinary proxy bindings are mutually exclusive and scoped to their actual dimension. */
public final class SuperPatternDataStick {
    private SuperPatternDataStick() {}
    public static void write(ItemStack stack, BlockPos pos, ResourceLocation dimension) {
        if (stack.has(GTDataComponents.RESEARCH_ITEM)) return;
        stack.remove(GTDataComponents.DATA_COPY_POS);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("pos");
            tag.putIntArray("superPos", new int[] {pos.getX(), pos.getY(), pos.getZ()});
            tag.putString("superDimension", dimension.toString());
        });
    }
    public static @Nullable BlockPos read(ItemStack stack, ResourceLocation dimension) {
        if (stack.has(GTDataComponents.RESEARCH_ITEM)) return null;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("superPos", Tag.TAG_INT_ARRAY) || !tag.contains("superDimension", Tag.TAG_STRING) ||
                !dimension.toString().equals(tag.getString("superDimension"))) return null;
        var coordinates = tag.getIntArray("superPos");
        return coordinates.length == 3 ? new BlockPos(coordinates[0], coordinates[1], coordinates[2]) : null;
    }
    public static void clear(ItemStack stack) {
        if (stack.has(GTDataComponents.RESEARCH_ITEM)) return;
        if (stack.has(DataComponents.CUSTOM_DATA)) CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("superPos"); tag.remove("superDimension");
        });
    }
}
