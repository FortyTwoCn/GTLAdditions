package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.common.EventBusSubscriber

@OnlyIn(Dist.CLIENT)
internal object HeartBlackHoleShaders {
    private val VOLUME_SHADER_ID = GTLAdditions.id("gtladditions_heart_black_hole")
    private val BLUR_SHADER_ID = GTLAdditions.id("gtladditions_heart_blur")
    private val COMPOSITE_SHADER_ID = GTLAdditions.id("gtladditions_heart_composite")

    var volumeShader: ShaderInstance? = null
        private set

    var blurShader: ShaderInstance? = null
        private set

    var compositeShader: ShaderInstance? = null
        private set

    fun register(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(event.resourceProvider, VOLUME_SHADER_ID, DefaultVertexFormat.POSITION)
        ) { shader -> volumeShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, BLUR_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> blurShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, COMPOSITE_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> compositeShader = shader }
    }
}

@Suppress("unused")
@EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
internal object HeartBlackHoleShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        HeartBlackHoleShaders.register(event)
    }
}