package com.gtladd.gtladditions.mixin.gtmtings;

import com.gtladd.gtladditions.api.machine.wireless.NativeBigTransferData;
import com.hepdd.gtmthings.api.misc.ITransferData;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.*;
import org.gtlcore.gtlcore.integration.gtmt.NativeWirelessMonitorRows;
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Precise Additions rows on the native shared snapshot, with native highlight actions and team filtering. */
@Mixin(value = NativeWirelessMonitorRows.class, remap = false)
public abstract class WirelessEnergyMonitorMixin {
    @ModifyReturnValue(method = "transferText", at = @At("RETURN"), require = 1)
    private static Component gtladditions$formatPreciseTransfer(Component original, ITransferData data) {
        if (!(data instanceof NativeBigTransferData precise)) return original;
        var amount = precise.preciseThroughput();
        long voltage = amount.abs().min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
        int tier = Math.min(NewGTValues.VNF.length - 1, NumberUtils.getFakeVoltageTier(voltage));
        var machine = data.machine();
        var dimension = machine.getLevel().dimension().location().toString();
        return Component.translatable(machine.getBlockState().getBlock().getDescriptionId())
                .withStyle(original.getStyle())
                .append(amount.signum() > 0 ? " +" : " ")
                .append(NativeWirelessMonitorRows.formatDecimal(new BigDecimal(amount)))
                .append(" EU/t (").append(NewGTValues.VNF[tier]).append(")")
                .append(Component.literal(" [ ] ").withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.CHANGE_PAGE, "gtmthings:highlight:" + dimension + ";" + machine.getBlockPos().asLong()))));
    }
}
