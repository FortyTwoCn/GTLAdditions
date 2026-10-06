package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.antichrist.ServerMachineManager
import net.minecraft.server.level.ServerLevel
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.common.EventBusSubscriber

@Suppress("unused")
@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object ServerLifecycleHandler {

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        (event.level as? ServerLevel)?.let {
            ServerMachineManager.clearDimension(it.dimension())
            CloudNetworkManager.clearLevel(it.dimension())
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onServerStopping(event: ServerStoppingEvent) {
        ServerMachineManager.clearAll()
        CloudNetworkManager.clearAll()
    }
}