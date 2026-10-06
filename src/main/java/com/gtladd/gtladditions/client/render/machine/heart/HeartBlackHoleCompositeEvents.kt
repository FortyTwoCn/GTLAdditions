@file:Suppress("ktlint:standard:filename")

package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.common.EventBusSubscriber

@Suppress("unused")
@EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = EventBusSubscriber.Bus.GAME,
    value = [Dist.CLIENT]
)
internal object HeartBlackHoleGuiCompositeEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRenderGuiPre(event: RenderGuiEvent.Pre) {
        HeartBlackHoleRenderer.renderPendingComposite(
            Minecraft.getInstance().window.width,
            Minecraft.getInstance().window.height
        )
    }

    @SubscribeEvent
    @JvmStatic
    fun onScreenRenderPre(event: ScreenEvent.Render.Pre) {
        val window = Minecraft.getInstance().window
        HeartBlackHoleRenderer.renderPendingComposite(
            window.width,
            window.height
        )
    }
}