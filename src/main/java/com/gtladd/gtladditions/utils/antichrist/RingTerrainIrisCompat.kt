package com.gtladd.gtladditions.utils.antichrist

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder
import net.irisshaders.iris.vertices.ImmediateState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState

/** Loaded only when Iris is installed; vanilla rendering has no Iris class dependencies. */
internal object RingTerrainIrisCompat {
    fun createBuffer(allocator: ByteBufferBuilder, shaderMode: Boolean): BufferBuilder {
        val previous = ImmediateState.skipExtension.get()
        ImmediateState.skipExtension.set(!shaderMode)
        try {
            return BufferBuilder(allocator, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
        } finally {
            ImmediateState.skipExtension.set(previous)
        }
    }

    fun beginBlock(builder: BufferBuilder, state: BlockState, x: Int, y: Int, z: Int) {
        val sensitive = builder as BlockSensitiveBufferBuilder
        val id = WorldRenderingSettings.INSTANCE.blockStateIds?.getOrDefault(state, -1) ?: -1
        val emission = state.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toByte()
        sensitive.beginBlock(id, (-1).toByte(), emission, x, y, z)
    }

    fun endBlock(builder: BufferBuilder) = (builder as BlockSensitiveBufferBuilder).endBlock()
}
