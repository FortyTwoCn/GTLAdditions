package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.machine.heart.HeartDeferredRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType
import com.mojang.serialization.MapCodec
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil

class HeartOfTheUniverseRenderer : DynamicRender<HeartOfTheUniverse, HeartOfTheUniverseRenderer>() {
    override fun getType(): DynamicRenderType<HeartOfTheUniverse, HeartOfTheUniverseRenderer> = TYPE

    companion object {
        @JvmField val CODEC: MapCodec<HeartOfTheUniverseRenderer> = MapCodec.unit { HeartOfTheUniverseRenderer() }
        @JvmField val TYPE = DynamicRenderType(CODEC)
    }


    @OnlyIn(Dist.CLIENT)
    override fun render(
        machine: HeartOfTheUniverse,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val blockEntity = machine
        run {
            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val starPos = getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, 0.0, 36.0, -39.0)
                val profile = HeartBlackHoleRenderProfile.create(tick, machine.frontFacing, starPos)

                HeartDeferredRenderer.enqueue(blockEntity, profile)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun isBlockEntityRenderer(): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun shouldRenderOffScreen(machine: HeartOfTheUniverse): Boolean = true

    override fun getRenderBoundingBox(machine: HeartOfTheUniverse): AABB = AABB(machine.blockPos).inflate(viewDistance.toDouble())

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}