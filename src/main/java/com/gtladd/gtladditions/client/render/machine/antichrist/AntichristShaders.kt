package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.antichrist.RingStructureVertexBuffer
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.ShaderInstance
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.common.EventBusSubscriber

@OnlyIn(Dist.CLIENT)
object AntichristShaders {
    private val STAR_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_star")
    private val BEAM_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_beam")
    internal val BEAM_VERTEX_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV1", VertexFormatElement.UV1)
        .build()

    var starShader: ShaderInstance? = null
        private set

    var beamShader: ShaderInstance? = null
        private set

    fun register(event: RegisterShadersEvent) {
        RingStructureVertexBuffer.invalidate()
        event.registerShader(
            ShaderInstance(event.resourceProvider, STAR_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> starShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, BEAM_SHADER_ID, BEAM_VERTEX_FORMAT)
        ) { shader -> beamShader = shader }
    }
}

@Suppress("unused")
@EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object AntichristShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        AntichristShaders.register(event)
    }
}
