package com.gtladd.gtladditions.events

import appeng.api.AECapabilities
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gtladd.gtladditions.integration.ae2.MEStorageCapabilityProvider
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

@Suppress("unused")
@EventBusSubscriber(modid = "gtladditions", bus = EventBusSubscriber.Bus.MOD)
object MECapabilityHandler {
    @SubscribeEvent
    @JvmStatic
    fun registerMEStorageCapability(event: RegisterCapabilitiesEvent) {
        val id = ResourceLocation.fromNamespaceAndPath("gtladditions", "infinity_input_dual_hatch")
        val definition = GTRegistries.MACHINES.get(id)
            ?: error("Required machine $id was not registered before its AE storage capability")
        event.registerBlockEntity(AECapabilities.ME_STORAGE, definition.blockEntityType, MEStorageCapabilityProvider())
    }
}
