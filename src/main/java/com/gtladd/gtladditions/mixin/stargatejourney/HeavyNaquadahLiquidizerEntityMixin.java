package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.common.block_entities.tech.AbstractLiquidizerEntity;
import net.povstalec.sgjourney.common.block_entities.tech.HeavyNaquadahLiquidizerEntity;
import net.povstalec.sgjourney.common.recipe.LiquidizingRecipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HeavyNaquadahLiquidizerEntity.class)
public abstract class HeavyNaquadahLiquidizerEntityMixin
        extends AbstractLiquidizerEntity<LiquidizingRecipe.HeavyNaquadahLiquidizer> {
    protected HeavyNaquadahLiquidizerEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Native liquidizers no longer have hasMaterial(); disable the inherited processing entry point.
    @Override
    public void doProgress() {
        resetProgress();
    }
}
