package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine
import snownee.jade.addon.universal.ItemStorageProvider
import snownee.jade.api.Accessor
import snownee.jade.api.view.*

object GTItemStorageProvider :
    IServerExtensionProvider<ItemStack>,
    IClientExtensionProvider<ItemStack, ItemView> {
    override fun getGroups(accessor: Accessor<*>): List<ViewGroup<ItemStack>>? {
        val machine = (accessor as? snownee.jade.api.BlockAccessor)?.blockEntity as? MetaMachine
        return if (machine is InfinityDualHatchPartMachine || machine is MEPatternBufferPartMachine) {
            listOf()
        } else {
            ItemStorageProvider.Extension.INSTANCE.getGroups(accessor)
        }
    }

    override fun getUid(): ResourceLocation = GTLAdditions.id("ban_item_storage")

    override fun getClientGroups(
        accessor: Accessor<*>,
        list: List<ViewGroup<ItemStack>>
    ): List<ClientViewGroup<ItemView>> = ItemStorageProvider.Extension.INSTANCE.getClientGroups(accessor, list)
}