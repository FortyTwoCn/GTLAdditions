package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.sound.SoundEntry
import net.neoforged.neoforge.registries.DeferredHolder
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.Companion.REGISTRATE

object GTLAddSoundEntries {
    val FORGE_OF_THE_ANTICHRIST: DeferredHolder<SoundEntry, SoundEntry>
    val QUANTUM_OSCILLATION: DeferredHolder<SoundEntry, SoundEntry>
    val GENESIS_ENGINE: DeferredHolder<SoundEntry, SoundEntry>
    val INTER_STELLAR: DeferredHolder<SoundEntry, SoundEntry>

    fun init() {
    }

    private fun register(name: String, attenuationDistance: Int): DeferredHolder<SoundEntry, SoundEntry> = REGISTRATE.sound(name).attenuationDistance(attenuationDistance).register()

    init {
        FORGE_OF_THE_ANTICHRIST = register("forgeofantichrist", 64)
        QUANTUM_OSCILLATION = register("quantumoscillation", 48)
        GENESIS_ENGINE = register("genesisengine", 48)
        INTER_STELLAR = register("interstellar", 96)
    }
}