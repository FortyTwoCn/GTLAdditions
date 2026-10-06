package com.gtladd.gtladditions.mixin.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.common.item.behavior.DataItemBehavior;
import com.gregtechceu.gtceu.common.data.item.GTDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;

/** Native research components supply their own tooltip; the item behavior adds proxy coordinates. */
@Mixin(DataItemBehavior.class)
public abstract class DataItemBehaviorMixin implements IAddInformation {
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(GTDataComponents.RESEARCH_ITEM)) return;
        var ordinary = stack.get(GTDataComponents.DATA_COPY_POS);
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (ordinary != null) {
            tooltip.add(Component.translatable("gtceu.tooltip.proxy_bind", Component.literal(Integer.toString(ordinary.getX())).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(Integer.toString(ordinary.getY())).withStyle(ChatFormatting.LIGHT_PURPLE), Component.literal(Integer.toString(ordinary.getZ())).withStyle(ChatFormatting.LIGHT_PURPLE)));
        } else if (tag.contains("superPos", Tag.TAG_INT_ARRAY)) {
            var pos = tag.getIntArray("superPos");
            if (pos.length != 3) return;
            tooltip.add(Component.translatable("gtceu.tooltip.super_proxy_bind", Component.literal(Integer.toString(pos[0])).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(Integer.toString(pos[1])).withStyle(ChatFormatting.LIGHT_PURPLE), Component.literal(Integer.toString(pos[2])).withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }
}
