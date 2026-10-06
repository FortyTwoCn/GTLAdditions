package com.gtladd.gtladditions.utils

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.machine.MetaMachine
import net.minecraft.resources.ResourceLocation
import org.gtlcore.gtlcore.common.machine.multiblock.electric.ComputationProviderMachine

internal fun IOpticalComputationProvider.canBridgeCloud(): Boolean {
    val canBridge = canBridge(mutableSetOf())
    return if (this is ComputationProviderMachine) isFormed && canBridge else canBridge
}

internal fun IOpticalComputationProvider.requestCloudCWU(cwu: Long, simulate: Boolean): Long {
    if (cwu <= 0L) return 0L
    if (isUnlimitedCloudProvider()) return cwu
    val requested = Ints.saturatedCast(cwu)
    // The native computation provider simulates without drawing energy or allocating CWU.
    return requestCWUt(requested, simulate, mutableSetOf()).toLong()
}

internal fun IOpticalComputationProvider.getMaxCloudCWU(): Long {
    if (this is ComputationProviderMachine && energyContainer.energyStored == 0L) return 0L
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    return getMaxCWUt(mutableSetOf()).toLong().coerceAtLeast(0L)
}

internal fun IOpticalComputationProvider.getRemainingCloudCWU(): Long {
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    return requestCWUt(Int.MAX_VALUE, true, mutableSetOf()).toLong().coerceAtLeast(0L)
}

internal fun IOpticalComputationProvider.isUnlimitedCloudProvider(): Boolean =
    this is MetaMachine && definition.id == ResourceLocation.fromNamespaceAndPath("gtceu", "create_computation")