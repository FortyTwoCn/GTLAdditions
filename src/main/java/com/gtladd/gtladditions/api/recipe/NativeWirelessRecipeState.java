package com.gtladd.gtladditions.api.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import net.minecraft.nbt.Tag;
import java.math.BigInteger;

public final class NativeWirelessRecipeState {
    public static final String KEY = "gtladditions:wireless_eut";
    public static final String TOTAL_KEY = "gtladditions:wireless_total_eu";
    public static final String PAID_KEY = "gtladditions:wireless_paid_eu";

    private NativeWirelessRecipeState() {}

    public static GTRecipe restore(GTRecipe recipe) {
        var amount = read(recipe, KEY);
        var total = read(recipe, TOTAL_KEY);
        if ((amount == null || amount.signum() == 0) && (total == null || total.signum() == 0)) return recipe;
        if (amount == null) amount = BigInteger.ZERO;
        if (recipe instanceof IWirelessGTRecipe wireless && amount.equals(wireless.getWirelessEuTickInputs()))
            return recipe;
        return new WirelessGTRecipe(recipe, amount);
    }

    public static BigInteger read(GTRecipe recipe, String key) {
        if (!recipe.data.contains(key, Tag.TAG_BYTE_ARRAY)) return null;
        byte[] bytes = recipe.data.getByteArray(key);
        if (bytes.length == 0) throw new IllegalArgumentException("Empty wireless energy integer: " + key);
        return new BigInteger(bytes);
    }

    public static BigInteger tickAmount(GTRecipe recipe, int progress, int duration) {
        var total = read(recipe, TOTAL_KEY);
        if (total == null) return recipe instanceof IWirelessGTRecipe wireless ? wireless.getWirelessEuTickInputs() : null;
        if (duration <= 0 || progress < 0 || progress >= duration) throw new IllegalArgumentException("Invalid wireless batch tick");
        var divisor = BigInteger.valueOf(duration);
        var amount = total.divide(divisor);
        return progress == duration - 1 ? amount.add(total.remainder(divisor)) : amount;
    }

    /** Virtual batch costs are fixed; native progress regression must not bill an already paid prefix again. */
    public static BigInteger paymentAmount(GTRecipe recipe, int progress, int duration) {
        var total = read(recipe, TOTAL_KEY);
        if (total == null) return tickAmount(recipe, progress, duration);
        tickAmount(recipe, progress, duration); // validate native progress and duration
        var paid = read(recipe, PAID_KEY);
        if (paid == null) paid = BigInteger.ZERO;
        if ((paid.signum() != 0 && paid.signum() != total.signum()) || paid.abs().compareTo(total.abs()) > 0)
            throw new IllegalArgumentException("Invalid paid wireless batch cost");
        var target = progress == duration - 1 ? total :
                total.divide(BigInteger.valueOf(duration)).multiply(BigInteger.valueOf((long) progress + 1));
        return target.abs().compareTo(paid.abs()) > 0 ? target.subtract(paid) : BigInteger.ZERO;
    }

    public static void recordPayment(GTRecipe recipe, BigInteger amount) {
        if (!recipe.data.contains(TOTAL_KEY, Tag.TAG_BYTE_ARRAY) || amount.signum() == 0) return;
        var paid = read(recipe, PAID_KEY);
        recipe.data.putByteArray(PAID_KEY, (paid == null ? amount : paid.add(amount)).toByteArray());
    }
}
