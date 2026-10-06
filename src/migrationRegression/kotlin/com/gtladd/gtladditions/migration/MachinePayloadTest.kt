package com.gtladd.gtladditions.migration

import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.network.MachineDeltaPacket
import com.gtladd.gtladditions.network.SyncDimensionMachinesPacket
import io.netty.buffer.Unpooled
import io.netty.handler.codec.DecoderException
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.level.Level
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MachinePayloadTest {
    private fun buffer() = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)

    @Test
    fun deltaPreservesDimensionNegativePositionFacingAndRemoval() {
        val original = MachineDeltaPacket(Level.NETHER, BlockPos(-12, -32, 2048), Direction.SOUTH, false)
        val buf = buffer()
        try {
            MachineDeltaPacket.STREAM_CODEC.encode(buf, original)
            val restored = MachineDeltaPacket.STREAM_CODEC.decode(buf)
            assertEquals(Level.NETHER, restored.dimensionKey)
            assertEquals(original.posLong, restored.posLong)
            assertEquals(Direction.SOUTH, restored.facing)
            assertFalse(restored.add)
            assertEquals(0, buf.readableBytes())
        } finally { buf.release() }
    }

    @Test
    fun snapshotOwnsItsArrayAndEmptySnapshotCanClearClientState() {
        val expected = MachineInfo(BlockPos(1, 70, 3).asLong(), Direction.WEST)
        val input = arrayOf(expected)
        val original = SyncDimensionMachinesPacket(Level.OVERWORLD, input)
        input[0] = MachineInfo(0, Direction.UP)
        for (packet in listOf(original, SyncDimensionMachinesPacket(Level.OVERWORLD, emptyArray()))) {
            val buf = buffer()
            try {
                SyncDimensionMachinesPacket.STREAM_CODEC.encode(buf, packet)
                val restored = SyncDimensionMachinesPacket.STREAM_CODEC.decode(buf)
                assertArrayEquals(packet.machines, restored.machines)
                assertEquals(Level.OVERWORLD, restored.dimensionKey)
                assertEquals(0, buf.readableBytes())
            } finally { buf.release() }
        }
        assertEquals(expected, original.machines[0])
    }

    @Test
    fun rejectsNegativeAndTruncatedSnapshotCountsBeforeAllocation() {
        for (count in listOf(-1, Int.MAX_VALUE, 1)) {
            val buf = buffer()
            try {
                buf.writeResourceLocation(Level.OVERWORLD.location())
                buf.writeVarInt(count)
                assertThrows(DecoderException::class.java) { SyncDimensionMachinesPacket.STREAM_CODEC.decode(buf) }
            } finally { buf.release() }
        }
    }
}
