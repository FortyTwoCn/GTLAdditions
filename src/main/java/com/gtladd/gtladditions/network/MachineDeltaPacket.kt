package com.gtladd.gtladditions.network

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class MachineDeltaPacket(
    val dimensionKey: ResourceKey<Level>,
    val posLong: Long,
    val facing: Direction,
    val add: Boolean
) : CustomPacketPayload {
    constructor(dimensionKey: ResourceKey<Level>, pos: BlockPos, facing: Direction, add: Boolean) :
        this(dimensionKey, pos.asLong(), facing, add)

    override fun type(): CustomPacketPayload.Type<MachineDeltaPacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<MachineDeltaPacket>(
            ResourceLocation.fromNamespaceAndPath("gtladditions", "machine_delta")
        )
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MachineDeltaPacket> =
            StreamCodec.of({ buffer, packet -> encode(packet, buffer) }, ::decode)

        fun encode(packet: MachineDeltaPacket, buffer: RegistryFriendlyByteBuf) {
            buffer.writeResourceLocation(packet.dimensionKey.location())
            buffer.writeLong(packet.posLong)
            buffer.writeEnum(packet.facing)
            buffer.writeBoolean(packet.add)
        }

        fun decode(buffer: RegistryFriendlyByteBuf): MachineDeltaPacket = MachineDeltaPacket(
            ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()),
            buffer.readLong(), buffer.readEnum(Direction::class.java), buffer.readBoolean()
        )
    }
}
