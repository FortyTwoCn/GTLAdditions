package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.registry.registrate.builder.MachineBuilder
import com.gtladd.gtladditions.client.render.machine.OverlayHullMachineModel
import net.minecraft.resources.ResourceLocation

object MachineBuilderExtensions {
    fun <D : MachineDefinition, M : MetaMachine> MachineBuilder<D, M, *>.overlayHullTextureModel(
        casing: ResourceLocation,
        overlayTexture: ResourceLocation
    ): MachineBuilder<D, M, *> {
        model(OverlayHullMachineModel.withTexture(casing, overlayTexture))
        return this
    }

    fun <D : MachineDefinition, M : MetaMachine> MachineBuilder<D, M, *>.overlayHullModel(
        bottom: ResourceLocation,
        top: ResourceLocation,
        side: ResourceLocation,
        overlayModel: ResourceLocation
    ): MachineBuilder<D, M, *> {
        model(OverlayHullMachineModel(bottom, top, side, overlayModel))
        return this
    }

    fun <D : MachineDefinition, M : MetaMachine> MachineBuilder<D, M, *>.overlayHullModel(
        res: ResourceLocation,
        overlayModel: ResourceLocation
    ): MachineBuilder<D, M, *> {
        model(OverlayHullMachineModel(res, overlayModel))
        return this
    }
}
