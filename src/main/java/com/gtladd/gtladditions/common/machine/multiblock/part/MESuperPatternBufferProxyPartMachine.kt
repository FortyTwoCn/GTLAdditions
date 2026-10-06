package com.gtladd.gtladditions.common.machine.multiblock.part

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.integration.ae2.SuperPatternDataStick
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferProxyPartMachine

class MESuperPatternBufferProxyPartMachine(info: BlockEntityCreationInfo) : MEPatternBufferProxyPartMachine(info) {
    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult {
        val world = level ?: return InteractionResult.PASS
        val target = SuperPatternDataStick.read(stack, world.dimension().location()) ?: return InteractionResult.PASS
        if (!isRemote) {
            if (!world.hasChunkAt(target) || MetaMachine.getMachine(world, target) !is MESuperPatternBufferPartMachine)
                return InteractionResult.PASS
            setBuffer(target)
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"))
        }
        return InteractionResult.sidedSuccess(isRemote)
    }
    override fun setBuffer(pos: BlockPos?) {
        val world = level
        if (pos != null && world != null && world.hasChunkAt(pos) && MetaMachine.getMachine(world, pos) !is MESuperPatternBufferPartMachine) return
        super.setBuffer(pos)
    }
}
