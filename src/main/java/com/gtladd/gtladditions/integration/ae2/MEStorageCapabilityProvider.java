package com.gtladd.gtladditions.integration.ae2;

import appeng.api.storage.MEStorage;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

/** Query the current native machine directly; only its front exposes AE insertion. */
public final class MEStorageCapabilityProvider implements ICapabilityProvider<BlockEntity, @Nullable Direction, MEStorage> {
    @Override
    public @Nullable MEStorage getCapability(BlockEntity blockEntity, @Nullable Direction direction) {
        if (blockEntity instanceof MetaMachine machine && direction != null && direction == machine.getFrontFacing() &&
                machine instanceof MEStorage storage) return storage;
        return null;
    }
}
