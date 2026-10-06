package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.machine.MachineInstanceFactory
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry

fun MachineEntry<MultiblockMachineDefinition>.replaceQueuedFactory(
    factory: MachineInstanceFactory<out MultiblockControllerMachine>
) = NativeQueuedMachines.replaceFactory(this, factory)
