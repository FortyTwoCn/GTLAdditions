package com.gtladd.gtladditions.migration;

import committee.nova.mods.avaritia.api.common.crafting.TierInput;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;

@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionRecipeGameTests {
    @GameTest(template = "empty", batch = "gtl_production_recipes", timeoutTicks = 200)
    public static void allFiveStargateRecipesLoadMatchAndCraftOnTheNativeExtremeTable(GameTestHelper helper) {
        var supplyId = ResourceLocation.fromNamespaceAndPath("gtlcore", "virtual_ingredient_supply_machine");
        var supply = helper.getLevel().getRecipeManager().byKey(supplyId)
                .orElseThrow(() -> new AssertionError("Original virtual ingredient supply recipe ID is missing: " + supplyId));
        helper.assertTrue(supply.value() instanceof com.gregtechceu.gtceu.api.recipe.GTRecipe,
                "Virtual ingredient supply recipe no longer uses the native GT serializer");
        var aliasId = supplyId.withPrefix("assembler/");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(aliasId).isEmpty(),
                "Native parent regeneration created a duplicate assembler alias for the original recipe ID");
        var nativeRecipe = (com.gregtechceu.gtceu.api.recipe.GTRecipe) supply.value();
        var inputs = new java.util.HashMap<com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability<?>, java.util.List<Object>>();
        nativeRecipe.inputs.forEach((capability, contents) -> inputs.put(capability,
                contents.stream().map(com.gregtechceu.gtceu.api.recipe.content.Content::content).toList()));
        com.gregtechceu.gtceu.api.recipe.GTRecipe found;
        try {
            // Read the real already-populated private lookup; do not create a fixture database.
            var field = com.gregtechceu.gtceu.api.recipe.GTRecipeType.class.getDeclaredField("db");
            field.setAccessible(true);
            var database = (com.gregtechceu.gtceu.api.recipe.lookup.RecipeDB) field.get(nativeRecipe.recipeType);
            found = database.find(inputs, recipe -> recipe.getId().equals(supplyId));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Cannot read the real native machine recipe lookup", error);
        }
        helper.assertTrue(found != null && found.getId().equals(supplyId),
                "Original recipe ID is loaded but unavailable to real native machine lookup");
        var regenerated = new java.util.HashMap<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>>();
        var output = new net.minecraft.data.recipes.RecipeOutput() {
            @Override public net.minecraft.advancements.Advancement.Builder advancement() {
                return net.minecraft.advancements.Advancement.Builder.recipeAdvancement();
            }
            @Override public void accept(ResourceLocation id, net.minecraft.world.item.crafting.Recipe<?> recipe,
                                          net.minecraft.advancements.AdvancementHolder advancement,
                                          net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                regenerated.put(id, recipe);
            }
        };
        new com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder(nativeRecipe, nativeRecipe.recipeType)
                .id(supplyId).onSave((builder, recipes) ->
                        com.gregtechceu.gtceu.common.data.GTRecipeTypes.MIXER_RECIPES.copyFrom(builder).save(recipes))
                .save(output);
        var childId = supplyId.withPrefix("mixer/");
        helper.assertTrue(regenerated.size() == 2 && regenerated.containsKey(supplyId) && regenerated.containsKey(childId)
                        && ((com.gregtechceu.gtceu.api.recipe.GTRecipe) regenerated.get(supplyId)).getId().equals(supplyId)
                        && ((com.gregtechceu.gtceu.api.recipe.GTRecipe) regenerated.get(childId)).getId().equals(childId),
                "Preserving a loaded parent's ID changed its real native child-recipe callback");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_RECIPE_ID id={} original_id_preserved=true native_serializer=true native_machine_lookup=true duplicate_alias=false native_child_recipe=true", supplyId);
        for (String name : new String[]{"classic_stargate_chevron_block", "classic_stargate_base_block",
                "classic_stargate_ring_block", "stargate_chevron_upgrade", "pegasus_dhd"}) {
            var id = ResourceLocation.fromNamespaceAndPath("gtladditions", name);
            var holder = helper.getLevel().getRecipeManager().byKey(id).orElseThrow(() -> new AssertionError("Missing production recipe: " + id));
            helper.assertTrue(holder.value() instanceof ShapedTableCraftingRecipe, "Incorrect native recipe serializer for " + id);
            var recipe = (ShapedTableCraftingRecipe) holder.value();
            helper.assertTrue(recipe.getTier() == 4 && recipe.getWidth() <= 9 && recipe.getHeight() <= 9,
                    "Extreme crafting geometry or tier changed: " + id);
            var items = new ArrayList<ItemStack>();
            // The native pattern codec trims empty outside rows/columns. Exercise the actual 9x9 table.
            for (int slot = 0; slot < 81; slot++) items.add(ItemStack.EMPTY);
            for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
                var ingredient = recipe.getIngredients().get(slot);
                if (ingredient.isEmpty()) continue;
                else {
                    helper.assertTrue(ingredient.getItems().length > 0, "Unresolved production recipe ingredient: " + id);
                    int x = slot % recipe.getWidth(), y = slot / recipe.getWidth();
                    items.set(y * 9 + x, ingredient.getItems()[0].copy());
                }
            }
            var input = TierInput.of(9, 9, items, 4);
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Native extreme crafting does not match its ingredients: " + id);
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            var expectedId = ResourceLocation.fromNamespaceAndPath(name.equals("stargate_chevron_upgrade") ? "gtladditions" : "sgjourney", name);
            helper.assertTrue(!result.isEmpty() && BuiltInRegistries.ITEM.getKey(result.getItem()).equals(expectedId)
                    && result.getCount() == 1, "Incorrect extreme crafting result for " + id);
            helper.assertTrue(!recipe.matches(TierInput.of(9, 9, items, 3), helper.getLevel()), "Extreme recipe accepted a lower tier table: " + id);
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isEmpty()) { items.set(i, ItemStack.EMPTY); break; }
            }
            helper.assertTrue(!recipe.matches(TierInput.of(9, 9, items, 4), helper.getLevel()), "Extreme recipe accepted a missing ingredient: " + id);
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_RECIPE id={} native_match=true native_craft=true", id);
        }
        helper.succeed();
    }
}
