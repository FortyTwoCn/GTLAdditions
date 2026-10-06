package com.gtladd.gtladditions.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Optional integrations are selected by installed mod IDs; required API mismatches still fail at load. */
public final class GTLAddMixinPlugin implements IMixinConfigPlugin {
    private static boolean loaded(String mod) { return FMLLoader.getLoadingModList().getModFileById(mod) != null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.contains(".oculus.")) return loaded("iris");
        if (mixin.endsWith(".FluidIngredientConverterMixin")) return loaded("ae2jeiintegration") && loaded("jei");
        if (mixin.endsWith(".FluidHelperMixin") || mixin.endsWith(".FluidTankRendererMixin")) return loaded("jei");
        if (mixin.endsWith(".FluidEmiStackMixin") || mixin.endsWith(".FluidUnitMixin") || mixin.endsWith(".ItemEmiStackMixin")) return loaded("emi");
        if (mixin.contains(".gtceu.integration.") || mixin.endsWith(".RecipeLogicProviderMixin")) return loaded("jade");
        if (mixin.endsWith(".GTRecipeJSMixin")) return loaded("kubejs");
        return true;
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
