package com.gtladd.gtladditions.mixin.gtceu.xei;

import mezz.jei.library.render.FluidTankRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Recipe fluid icons fill the slot while keeping JEI's native sprite, color and tiling. */
@Mixin(FluidTankRenderer.class)
public abstract class FluidTankRendererMixin {
    @Shadow(remap = false) @Final private int height;
    @ModifyArg(method = "lambda$render$0", at = @At(value = "INVOKE", target =
            "Lmezz/jei/library/render/FluidTankRenderer;drawTiledSprite(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lmezz/jei/api/gui/drawable/TilingDirection;II)V"),
            index = 4, remap = false, require = 1)
    private int gtladditions$fullFluidIcon(int scaledAmount) { return height; }
}
