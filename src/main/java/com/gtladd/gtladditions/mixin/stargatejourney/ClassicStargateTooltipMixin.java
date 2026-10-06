package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.povstalec.sgjourney.common.blocks.stargate.ClassicStargateBaseBlock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;
import static com.gtladd.gtladditions.integration.sgjourney.StargateIntegration.rainbow;

/** Client-only tooltip: never resolve Screen from the dedicated-server Mixin. */
@Mixin(ClassicStargateBaseBlock.class)
public abstract class ClassicStargateTooltipMixin extends HorizontalDirectionalBlock {
    protected ClassicStargateTooltipMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(rainbow(
                Component.translatable("tooltip.gtladditions.classic_stargate_base_block").getString()));
        if (Screen.hasShiftDown()) {
            tooltip.addAll(
                    List.of(
                            Component.translatable("tooltip.gtladditions.stargate_structure.1"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.2"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.3"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.4"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.5"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.6"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.7"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.8"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.9"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.10")));
        } else {
            tooltip.add(Component.translatable("tooltip.gtladditions.stargate_structure.0")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

}
