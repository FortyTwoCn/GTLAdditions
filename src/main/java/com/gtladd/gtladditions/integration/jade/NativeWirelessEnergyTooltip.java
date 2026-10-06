package com.gtladd.gtladditions.integration.jade;

import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;
import org.gtlcore.gtlcore.utils.TextUtil;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Keeps arbitrary precision wireless EU out of the native provider's signed long field. */
public final class NativeWirelessEnergyTooltip {
    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q" };

    private NativeWirelessEnergyTooltip() {}

    public static Component energyLine(BigInteger amount) {
        var absolute = amount.abs();
        if (absolute.signum() == 0) throw new IllegalArgumentException("Wireless energy must be nonzero");
        long voltage = NumberUtils.getLongValue(absolute);
        int tier = voltage == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(voltage);
        tier = Math.min(tier, Math.min(GTValues.VEX.length, NewGTValues.VNF.length) - 1);
        var amps = FormattingUtil.DECIMAL_FORMAT_2F.format(new BigDecimal(absolute)
                .divide(BigDecimal.valueOf(GTValues.VEX[tier]), 3, RoundingMode.DOWN));
        Component value = Component.literal(format(absolute)).withStyle(ChatFormatting.RED)
                .append(Component.literal(" EU/t").withStyle(ChatFormatting.RESET))
                .append(Component.literal(" (").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("gtceu.top.electricity", amps, NewGTValues.VNF[tier])
                        .withStyle(TextUtil.GTL_CORE$VC[Math.min(tier, TextUtil.GTL_CORE$VC.length - 1)]))
                .append(Component.literal(")").withStyle(ChatFormatting.GREEN));
        return Component.translatable(amount.signum() < 0 ? "gtceu.top.energy_consumption" :
                "gtceu.top.energy_production").append(" ").append(value);
    }

    private static String format(BigInteger absolute) {
        int exponent = ((absolute.toString().length() - 1) / 3) * 3;
        var scaled = new BigDecimal(absolute).movePointLeft(exponent);
        var suffix = exponent / 3 < UNITS.length ? UNITS[exponent / 3] : "e" + exponent;
        return new DecimalFormat("#.##").format(scaled) + suffix;
    }
}
