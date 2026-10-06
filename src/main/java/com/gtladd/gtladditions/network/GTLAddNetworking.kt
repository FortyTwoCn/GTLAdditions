package com.gtladd.gtladditions.network

import com.gtladd.gtladditions.client.network.ClientMachinePayloadHandler
import net.minecraft.core.registries.Registries
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object GTLAddNetworking {
    val DIMENSION_REGISTRY_KEY = Registries.DIMENSION

    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("2")
        // These snapshots are server-authoritative; clients cannot send them to mutate server state.
        registrar.playToClient(SyncDimensionMachinesPacket.TYPE, SyncDimensionMachinesPacket.STREAM_CODEC) { packet, context ->
            context.enqueueWork { ClientMachinePayloadHandler.handle(packet) }
        }
        registrar.playToClient(MachineDeltaPacket.TYPE, MachineDeltaPacket.STREAM_CODEC) { packet, context ->
            context.enqueueWork { ClientMachinePayloadHandler.handle(packet) }
        }
    }
}
