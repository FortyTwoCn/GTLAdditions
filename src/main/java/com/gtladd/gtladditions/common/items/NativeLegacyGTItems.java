package com.gtladd.gtladditions.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Retains the original plain GT credit used by Additions after GT 8 removed its registration. */
public final class NativeLegacyGTItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("gtceu");
    public static final DeferredItem<Item> NEUTRONIUM_CREDIT = ITEMS.registerSimpleItem("neutronium_credit",
            new Item.Properties().rarity(Rarity.EPIC));

    private NativeLegacyGTItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
