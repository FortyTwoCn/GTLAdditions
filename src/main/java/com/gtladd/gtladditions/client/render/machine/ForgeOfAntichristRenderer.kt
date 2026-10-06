package com.gtladd.gtladditions.client.render.machine

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristDeferredRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType
import com.mojang.serialization.MapCodec
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.vertex.PoseStack
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil

class ForgeOfAntichristRenderer : DynamicRender<ForgeOfTheAntichrist, ForgeOfAntichristRenderer>() {
    override fun getType(): DynamicRenderType<ForgeOfTheAntichrist, ForgeOfAntichristRenderer> = TYPE

    companion object {
        @JvmField val CODEC: MapCodec<ForgeOfAntichristRenderer> = MapCodec.unit { ForgeOfAntichristRenderer() }
        @JvmField val TYPE = DynamicRenderType(CODEC)
    }


    @OnlyIn(Dist.CLIENT)
    override fun render(
        machine: ForgeOfTheAntichrist,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val blockEntity = machine
        val isWorking = machine.isActiveOrStasisAnchored()
        if (!machine.isFormed && !isWorking) return

        val tick = if (isWorking) RenderUtil.getSmoothTick(machine, partialTicks) else 0f
        val profile = AntichristRenderProfile.create(machine, tick, isWorking)

        if (machine.isFormed) {
            AntichristDeferredRenderer.enqueueRing(blockEntity, profile)
        }

        if (isWorking) {
            AntichristDeferredRenderer.enqueue(blockEntity, profile)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun isBlockEntityRenderer(): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun shouldRenderOffScreen(machine: ForgeOfTheAntichrist): Boolean = true

    override fun getRenderBoundingBox(machine: ForgeOfTheAntichrist): AABB = AABB(machine.blockPos).inflate(viewDistance.toDouble())

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}