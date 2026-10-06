package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalDataHatchMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.part.WirelessOpticalDataHatchMachine;
import org.spongepowered.asm.mixin.Mixin;

/** Sharing uses the native controller-and-substructure contract. */
@Mixin(WirelessOpticalDataHatchMachine.class)
public abstract class WirelessOpticalDataHatchMachineMixin extends OpticalDataHatchMachine {
    protected WirelessOpticalDataHatchMachineMixin(BlockEntityCreationInfo info, boolean transmitter) { super(info, transmitter); }
    @Override public boolean canShared(MultiblockControllerMachine controller, String substructureName) { return true; }
}
