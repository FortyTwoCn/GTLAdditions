package com.gtladd.gtladditions.mixin.gtmtings;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.hepdd.gtmthings.api.misc.WirelessEnergyContainer;
import com.gtladd.gtladditions.api.machine.wireless.NativeBigTransferData;
import com.gtladd.gtladditions.utils.WirelessEnergyManagerData;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.math.BigInteger;
import java.util.UUID;

/** Observe accepted native transfers; the saved account and rate policy remain owned by GTMThings. */
@Mixin(value = WirelessEnergyContainer.class, remap = false)
public abstract class WirelessEnergyManagerMixin {
    @Shadow @Final private UUID uuid;

    @Inject(method = "addEnergy", at = @At("RETURN"), require = 1)
    private void gtladditions$recordInput(long requested, @Nullable MetaMachine machine, CallbackInfoReturnable<Long> cir) {
        gtladditions$record(machine, cir.getReturnValueJ(), false);
    }

    @Inject(method = "removeEnergy", at = @At("RETURN"), require = 1)
    private void gtladditions$recordOutput(long requested, @Nullable MetaMachine machine, CallbackInfoReturnable<Long> cir) {
        gtladditions$record(machine, cir.getReturnValueJ(), true);
    }

    @Unique
    private void gtladditions$record(@Nullable MetaMachine machine, long accepted, boolean debit) {
        if (machine == null || accepted <= 0) return;
        var amount = BigInteger.valueOf(accepted);
        if (debit) amount = amount.negate();
        WirelessEnergyManagerData.BIG_MACHINE_DATA.put(machine, Pair.of(uuid, amount));
        WirelessEnergyContainer.TRANSFER_DATA.put(machine, new NativeBigTransferData(uuid, amount, machine));
    }
}
