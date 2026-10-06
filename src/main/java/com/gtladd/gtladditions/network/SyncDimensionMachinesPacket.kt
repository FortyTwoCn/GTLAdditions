package com.gtladd.gtladditions.network

import com.gtladd.gtladditions.common.data.MachineInfo
import io.netty.handler.codec.DecoderException
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class SyncDimensionMachinesPacket(
    val dimensionKey: ResourceKey<Level>,
    machines: Array<MachineInfo>
) : CustomPacketPayload {
    // Snapshot mutable server collections before encoding can occur on a different thread.
    val machines: Array<MachineInfo> = machines.copyOf()

    override fun type(): CustomPacketPayload.Type<SyncDimensionMachinesPacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<SyncDimensionMachinesPacket>(
            ResourceLocation.fromNamespaceAndPath("gtladditions", "sync_dimension_machines")
        )
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncDimensionMachinesPacket> =
            StreamCodec.of({ buffer, packet -> encode(packet, buffer) }, ::decode)

        fun encode(packet: SyncDimensionMachinesPacket, buffer: RegistryFriendlyByteBuf) {
            buffer.writeResourceLocation(packet.dimensionKey.location())
            buffer.writeVarInt(packet.machines.size)
            for (machine in packet.machines) {
                buffer.writeLong(machine.posLong)
                buffer.writeEnum(machine.facing)
            }
        }

        fun decode(buffer: RegistryFriendlyByteBuf): SyncDimensionMachinesPacket {
            val dimensionKey = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation())
            val count = buffer.readVarInt()
            // Each entry needs at least a long position and one byte for the direction.
            // Bound allocation by the received bytes before creating the array.
            if (count < 0 || count > buffer.readableBytes() / 9) {
                throw DecoderException("Invalid machine snapshot count: $count")
            }
            val machines = Array(count) {
                MachineInfo(buffer.readLong(), buffer.readEnum(Direction::class.java))
            }
            return SyncDimensionMachinesPacket(dimensionKey, machines)
        }
    }
}
