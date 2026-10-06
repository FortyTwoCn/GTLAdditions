package com.gtladd.gtladditions.api.registry;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MachineInstanceFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.builder.MachineBuilder;

/** Exposes the actual GT builder without asking Kotlin to infer its recursive self type. */
public final class NativeMachineBuilders {
    private NativeMachineBuilders() {}

    public static <M extends MetaMachine> MachineBuilder<MachineDefinition, M, ?> machine(
            GTRegistrate registrate, String name, MachineInstanceFactory<M> factory) {
        return registrate.machine(name, factory);
    }
}
