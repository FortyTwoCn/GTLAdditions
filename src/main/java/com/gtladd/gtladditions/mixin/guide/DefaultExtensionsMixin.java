package com.gtladd.gtladditions.mixin.guide;

import com.gtladd.gtladditions.api.guide.CleanItemLinkCompiler;
import com.gtladd.gtladditions.api.guide.FluidLinkCompiler;
import com.gtladd.gtladditions.api.guide.LatexCompiler;
import guideme.compiler.TagCompiler;
import guideme.compiler.tags.ItemLinkCompiler;
import guideme.internal.extensions.DefaultExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(DefaultExtensions.class)
public abstract class DefaultExtensionsMixin {
    @Inject(method = "tagCompilers", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private static void gtladditions$guideTags(CallbackInfoReturnable<List<TagCompiler>> cir) {
        var compilers = new ArrayList<TagCompiler>();
        for (var compiler : cir.getReturnValue()) compilers.add(compiler instanceof ItemLinkCompiler ? new CleanItemLinkCompiler() : compiler);
        compilers.add(new FluidLinkCompiler()); compilers.add(new LatexCompiler());
        cir.setReturnValue(compilers);
    }
}
