package com.gtladd.gtladditions.migration;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.gtlcore.gtlcore.integration.jei.JeiMissingIngredientBookmarks;
import java.util.List;

/** Validation-only plugin; uses the actual native JEI runtime and its public bookmark manager. */
@JeiPlugin
public final class NativeProductionJeiProbe implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("gtl_native_probe", "jei"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    static boolean isReady() { return runtime != null && JeiMissingIngredientBookmarks.isAvailable(); }

    static void verifyTranslations() {
        var language = net.minecraft.locale.Language.getInstance();
        var client = net.minecraft.client.Minecraft.getInstance();
        var id = ResourceLocation.fromNamespaceAndPath("gtl_native_probe", "native-legacy-translations.json");
        int legacy = 0, recipeTypes = 0;
        try (var reader = client.getResourceManager().openAsReader(id)) {
            for (var value : com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("keys")) {
                if (!language.has(value.getAsString()))
                    throw new AssertionError("Original Additions translation is missing in the actual client language: " + value.getAsString());
                legacy++;
            }
            for (var field : com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.class.getFields()) {
                if (!com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry.class.isAssignableFrom(field.getType())) continue;
                var entry = (com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry) field.get(null);
                for (var prefix : List.of("recipe_type", "recipe_category")) {
                    var key = entry.get().id.toLanguageKey(prefix);
                    if (!language.has(key)) throw new AssertionError("Native production recipe label is missing: " + key);
                }
                recipeTypes++;
            }
        } catch (java.io.IOException | IllegalAccessException error) {
            throw new AssertionError("Cannot verify original and native client translations", error);
        }
        if (legacy < 500 || recipeTypes != 26) throw new AssertionError("Incomplete actual translation coverage");
        com.mojang.logging.LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_TRANSLATIONS legacy_keys={} recipe_types={} actual_client_language=true", legacy, recipeTypes);
    }

    static void verifyBookmarks() {
        if (runtime == null || !JeiMissingIngredientBookmarks.isAvailable()) throw new AssertionError("Native production JEI runtime is missing");
        var ingredientManager = runtime.getIngredientManager();
        var item = ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.DIAMOND)).orElseThrow();
        var fluid = ingredientManager.createTypedIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(Fluids.WATER, 1)).orElseThrow();
        var bookmarks = runtime.getBookmarkManager();
        bookmarks.remove(item);
        bookmarks.remove(fluid);
        var keys = List.of(AEItemKey.of(Items.DIAMOND), AEFluidKey.of(Fluids.WATER), AEItemKey.of(Items.DIAMOND));
        var first = JeiMissingIngredientBookmarks.add(keys);
        var second = JeiMissingIngredientBookmarks.add(keys);
        if (first.status() != JeiMissingIngredientBookmarks.AddStatus.ADDED || first.added() != 2
                || second.status() != JeiMissingIngredientBookmarks.AddStatus.ALREADY_BOOKMARKED || second.added() != 0
                || !bookmarks.contains(item) || !bookmarks.contains(fluid))
            throw new AssertionError("Native missing-ingredient bookmarks failed: first=" + first + " second=" + second);
        if (!bookmarks.remove(item) || !bookmarks.remove(fluid)) throw new AssertionError("Native bookmark cleanup failed");
        com.mojang.logging.LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_JEI_BOOKMARKS item=true fluid=true deduplication=true public_api=true");
    }
}
