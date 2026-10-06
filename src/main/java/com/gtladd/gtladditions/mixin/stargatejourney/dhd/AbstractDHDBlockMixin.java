package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(AbstractDHDBlock.class)
public abstract class AbstractDHDBlockMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"), require = 1)
    private void describeGtlDialing(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        tooltip.removeIf(line -> line.getContents() instanceof TranslatableContents text &&
                (text.getKey().equals("tooltip.sgjourney.energy_buffer") ||
                 text.getKey().equals("tooltip.sgjourney.dhd.crystal_menu")));
        tooltip.add(Component.translatable("tooltip.gtladditions.dhd")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (int i = 0; i < 4; i++) {
            tooltip.add(Component.translatable("tooltip.gtladditions.dialog." + i));
        }
    }
}
