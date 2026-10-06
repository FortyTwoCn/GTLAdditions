package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType
import com.mojang.serialization.MapCodec
import net.minecraft.world.phys.AABB
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.roundToInt

class TimeSpaceDistorterRenderer : DynamicRender<TimeSpaceDistorter, TimeSpaceDistorterRenderer>() {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        machine: TimeSpaceDistorter,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val blockEntity = machine
        if (!machine.shouldRenderMagicCircleEffect()) return

        val tick = RenderUtil.getSmoothTick(machine, partialTicks)
        val center = getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, 0.0, -9.0, -5.0)

        renderMagicCircle(tick, center, machine.renderCausalityDistortionLevel, poseStack, buffer)
    }

    @OnlyIn(Dist.CLIENT)
    override fun isBlockEntityRenderer(): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun shouldRenderOffScreen(machine: TimeSpaceDistorter): Boolean = true

    override fun getRenderBoundingBox(machine: TimeSpaceDistorter): AABB = AABB(machine.blockPos).inflate(viewDistance.toDouble())

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    override fun getType(): DynamicRenderType<TimeSpaceDistorter, TimeSpaceDistorterRenderer> = TYPE

    companion object {
        @JvmField val CODEC: MapCodec<TimeSpaceDistorterRenderer> = MapCodec.unit { TimeSpaceDistorterRenderer() }
        @JvmField val TYPE = DynamicRenderType(CODEC)

        private const val MAGIC_CIRCLE_RADIUS = 22.0f

        private val LEVEL_TEXTURES = Array(4) { index -> magicCircleTexture("magic_circle_level_${index + 1}") }
        private val LEVEL_RENDER_TYPES = Array(4) { index -> RenderType.entityCutoutNoCull(LEVEL_TEXTURES[index]) }
        private val LEVEL_RED = IntArray(4) { index -> levelColorComponent(index, 0.34f, 0.55f) }
        private val LEVEL_GREEN = IntArray(4) { index -> levelColorComponent(index, 0.78f, 0.4f) }
        private val LEVEL_BLUE = IntArray(4) { 255 }

        private fun renderMagicCircle(
            tick: Float,
            center: Vec3,
            distortionLevel: Int,
            poseStack: PoseStack,
            buffer: MultiBufferSource
        ) {
            val levelIndex = distortionLevel.coerceIn(1, 4) - 1
            val levelProgress = levelIndex / 3.0f
            val rotationDegrees = tick * (0.62f + 0.08f * levelProgress)

            poseStack.withPose {
                translate(center.x, center.y, center.z)
                mulPose(Axis.YP.rotationDegrees(rotationDegrees))
                scale(MAGIC_CIRCLE_RADIUS, 1.0f, MAGIC_CIRCLE_RADIUS)

                val pose = last()
                addQuad(
                    buffer.getBuffer(LEVEL_RENDER_TYPES[levelIndex]),
                    pose,
                    LEVEL_RED[levelIndex],
                    LEVEL_GREEN[levelIndex],
                    LEVEL_BLUE[levelIndex],
                    255
                )
            }
        }

        @Suppress("SameParameterValue")
        private fun addQuad(
            consumer: VertexConsumer,
            pose: PoseStack.Pose,
            red: Int,
            green: Int,
            blue: Int,
            alpha: Int
        ) {
            addVertex(consumer, pose, -1.0f, 0.0f, -1.0f, 0.0f, 0.0f, red, green, blue, alpha)
            addVertex(consumer, pose, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, red, green, blue, alpha)
            addVertex(consumer, pose, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, red, green, blue, alpha)
            addVertex(consumer, pose, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f, red, green, blue, alpha)
        }

        @Suppress("SameParameterValue")
        private fun addVertex(
            consumer: VertexConsumer,
            pose: PoseStack.Pose,
            x: Float,
            y: Float,
            z: Float,
            u: Float,
            v: Float,
            red: Int,
            green: Int,
            blue: Int,
            alpha: Int
        ) {
            consumer.addVertex(pose.pose(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0f, 1.0f, 0.0f)
        }

        private fun levelColorComponent(levelIndex: Int, base: Float, boostScale: Float): Int {
            val levelProgress = levelIndex / 3.0f
            val colorBoost = 0.06f + 0.12f * levelProgress
            return colorComponent(base + colorBoost * boostScale)
        }

        private fun colorComponent(value: Float): Int =
            (value.coerceIn(0.0f, 1.0f) * 255.0f).roundToInt().coerceIn(0, 255)

        private fun magicCircleTexture(name: String): ResourceLocation =
            GTLAdditions.id("textures/block/multiblock/time_space_distorter/$name.png")
    }
}