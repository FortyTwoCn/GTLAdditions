package com.gtladd.gtladditions.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.blockstates.Orientation;
import net.povstalec.sgjourney.common.blockstates.StargatePart;
import net.povstalec.sgjourney.common.init.BlockInit;

/** Builds the native original classic ring and invokes Additions' real block interaction to assemble it. */
final class NativeProductionStargates {
    static PegasusStargateEntity assemble(ServerLevel level, BlockPos base, ServerPlayer player) {
        // The isolated Nether fixture can be inside natural terrain. Clear the genuine
        // ring aperture and both approach sides rather than bypass native obstruction checks.
        for (var position : BlockPos.betweenClosed(base.offset(-4, 0, -2), base.offset(4, 7, 2)))
            level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        for (var part : BlockInit.CLASSIC_STARGATE.get().getParts()) {
            var position = part.getRingPos(base, Direction.NORTH, Orientation.REGULAR);
            var block = switch (part) {
                case BASE -> BlockInit.CLASSIC_STARGATE_BASE_BLOCK.get();
                case LEFT, LEFT3_ABOVE, LEFT3_ABOVE4, LEFT2_ABOVE6, ABOVE6,
                        RIGHT2_ABOVE6, RIGHT3_ABOVE4, RIGHT3_ABOVE, RIGHT -> BlockInit.CLASSIC_STARGATE_CHEVRON_BLOCK.get();
                default -> BlockInit.CLASSIC_STARGATE_RING_BLOCK.get();
            };
            var state = block.defaultBlockState();
            if (part == StargatePart.BASE) state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            level.setBlockAndUpdate(position, state);
        }
        var previous = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        try {
            var state = level.getBlockState(base);
            var result = state.useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(base.getCenter(), Direction.NORTH, base, false));
            if (result != ItemInteractionResult.SUCCESS || !(level.getBlockEntity(base) instanceof PegasusStargateEntity gate))
                throw new AssertionError("Original classic ring did not assemble through the native GTL block interaction");
            if (gate.getStargate() == null || !gate.getStargate().checkValidity() || gate.getEnergyCapacity() != 0
                    || gate.getEnergyHandler(Direction.NORTH) != null)
                throw new AssertionError("Assembled native GTL Pegasus gate is invalid or requires external energy");
            return gate;
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, previous); }
    }
    private NativeProductionStargates() {}
}
