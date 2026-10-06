package com.gtladd.gtladditions.client.network

import com.gtladd.gtladditions.network.MachineDeltaPacket
import com.gtladd.gtladditions.network.SyncDimensionMachinesPacket
import com.gtladd.gtladditions.utils.antichrist.ClientRingBlockHelper
import net.minecraft.client.Minecraft

/** Loaded by clientbound handlers only; payload classes are safe to load on dedicated servers. */
object ClientMachinePayloadHandler {
    fun handle(packet: SyncDimensionMachinesPacket) {
        val level = Minecraft.getInstance().level ?: return
        if (level.dimension() == packet.dimensionKey) {
            ClientRingBlockHelper.syncDimensionMachines(level, packet.machines)
        }
    }

    fun handle(packet: MachineDeltaPacket) {
        val level = Minecraft.getInstance().level ?: return
        if (level.dimension() != packet.dimensionKey) return
        if (packet.add) {
            ClientRingBlockHelper.hideRingsAtPosition(level, packet.posLong, packet.facing)
        } else {
            ClientRingBlockHelper.restoreRingsAtPosition(level, packet.posLong, packet.facing)
        }
    }
}
