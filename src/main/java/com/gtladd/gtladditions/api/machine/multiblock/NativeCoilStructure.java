package com.gtladd.gtladditions.api.machine.multiblock;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.error.CoilMatchingError;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Read coils from the native accepted pattern cache and reject mixed coil types. */
public final class NativeCoilStructure {
    private NativeCoilStructure() {}

    public static @Nullable ICoilType match(MultiblockControllerMachine machine, String structure) {
        ICoilType matched = null;
        var state = machine.getPatternState(structure);
        for (var entry : state.getCache().long2ObjectEntrySet()) {
            if (entry.getValue().getBlockState().getBlock() instanceof CoilBlock coil &&
                    GTCEuAPI.HEATING_COILS.containsKey(coil.coilType)) {
                if (matched == null) matched = coil.coilType;
                else if (matched != coil.coilType) {
                    state.setError(new CoilMatchingError(BlockPos.of(entry.getLongKey()), matched, coil.coilType));
                    machine.invalidateStructure(structure);
                    return null;
                }
            }
        }
        return matched == null ? CoilBlock.CoilType.CUPRONICKEL : matched;
    }
}
