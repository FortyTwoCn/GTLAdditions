package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate

/** The native registrate owns machine block entities, entries, and their registry lifecycle. */
class GTLAddRegistration private constructor() {
    companion object {
        @JvmField val REGISTRATE: GTRegistrate = GTRegistrate.create("gtladditions", false)
    }
}
