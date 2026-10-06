package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.gtlcore.gtlcore.api.recipe.NativeContentState;
import org.gtlcore.gtlcore.utils.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Legacy chance boosts are metadata on native contents, including the decreasing glacio-spirit input chance. */
@Mixin(GTRecipeSchema.GTKubeRecipe.class)
public abstract class GTRecipeJSMixin {
    @Shadow(remap = false) public boolean perTick;
    @Shadow(remap = false) public abstract GTRecipeSchema.GTKubeRecipe chancedInput(SizedIngredient stack, int chance);

    public GTRecipeSchema.GTKubeRecipe chancedInput(SizedIngredient stack, int chance, int boost) {
        var self = (GTRecipeSchema.GTKubeRecipe) (Object) this;
        var items = stack.ingredient().getItems();
        if (stack.count() == 1 && items.length > 0 && "kubejs:glacio_spirit".equals(Registries.getItemId(items[0]))
                && chance == 8000 && boost == 100) boost = -boost;
        chancedInput(stack, chance);
        var map = self.getValue(perTick ? GTRecipeSchema.ALL_TICK_INPUTS : GTRecipeSchema.ALL_INPUTS);
        var contents = map.get(ItemRecipeCapability.CAP);
        NativeContentState.of(contents.getLast()).gtlcore$setTierChanceBoost(boost);
        self.save();
        return self;
    }
}
