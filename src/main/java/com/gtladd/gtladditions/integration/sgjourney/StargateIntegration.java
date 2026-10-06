package com.gtladd.gtladditions.integration.sgjourney;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Registry-based Stargate integration shared by world generation and player travel. */
public final class StargateIntegration {
    public static final String VALID_TAG = "gtladditions:stargate_transfer";
    private static final Map<ResourceLocation, ResourceLocation> DISPLAY_ITEMS = Map.of(
            ResourceLocation.parse("sgjourney:lantea"), ResourceLocation.parse("sgjourney:universe_stargate"),
            ResourceLocation.parse("sgjourney:abydos"), ResourceLocation.parse("avaritia:neutron_ring"),
            ResourceLocation.parse("sgjourney:chulak"), ResourceLocation.parse("avaritia:infinity_umbrella"),
            ResourceLocation.parse("sgjourney:cavum_tenebrae"), ResourceLocation.parse("avaritia:infinity_ring"));
    private static final ChatFormatting[] RAINBOW = {
            ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE
    };

    private StargateIntegration() {}

    public static boolean isTargetDimension(ResourceLocation dimension) {
        return DISPLAY_ITEMS.containsKey(dimension);
    }

    public static @Nullable ItemStack selectDisplayItem(ResourceLocation dimension) {
        ResourceLocation id = DISPLAY_ITEMS.get(dimension);
        if (id == null) return null;
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() ->
                new IllegalStateException("Missing Stargate structure display item: " + id)));
    }

    // Preserve GTL's per-character rainbow colors and 80 ms phase used by the assembly tooltip.
    public static Component rainbow(String text) {
        int offset = (int) Math.floor((System.currentTimeMillis() & 0x3FFFL) / 80.0) % RAINBOW.length;
        var result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(RAINBOW[(RAINBOW.length + i - offset) % RAINBOW.length]));
        }
        return result;
    }
}
