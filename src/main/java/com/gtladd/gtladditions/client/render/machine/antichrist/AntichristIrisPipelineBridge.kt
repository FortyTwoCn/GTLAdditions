package com.gtladd.gtladditions.client.render.machine.antichrist

import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
interface AntichristIrisPipelineBridge {
    fun beginAntichristFallbackTarget()

    fun endAntichristFallbackTarget()
}