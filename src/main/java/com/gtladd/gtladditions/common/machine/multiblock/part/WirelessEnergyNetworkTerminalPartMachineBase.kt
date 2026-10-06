package com.gtladd.gtladditions.common.machine.multiblock.part

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.common.data.GTItems
import com.gtladd.gtladditions.common.machine.trait.NetworkEnergyContainer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.api.capability.IBindable
import com.hepdd.gtmthings.utils.TeamUtil
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import java.util.*

open class WirelessEnergyNetworkTerminalPartMachineBase(holder: BlockEntityCreationInfo, protected val io: IO) :
    MultiblockPartMachine(holder),
    IMuiMachine,
    IBindable {
    @field:SaveField
    protected var uuid: UUID? = null

    @field:SaveField
    val energyContainer: NetworkEnergyContainer

    init {
        this.energyContainer = attachTrait(createEnergyContainer())
    }

    override fun onUse(context: ExtendedUseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val hand = context.hand
        if (player.getItemInHand(hand).`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!isRemote) {
                setUUID(player.uuid)
                player.sendSystemMessage(
                    "gtmthings.machine.wireless_energy_hatch.tooltip.bind".toComponent(TeamUtil.GetName(player))
                )
            }
            return InteractionResult.sidedSuccess(isRemote)
        }
        return InteractionResult.PASS
    }

    override fun onLeftClick(
        player: Player,
        hand: InteractionHand,
        direction: Direction?
    ): Boolean {
        val `is` = player.getItemInHand(hand)
        if (`is`.isEmpty) return false
        if (`is`.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!isRemote) {
                setUUID(null)
                player.sendSystemMessage("gtmthings.machine.wireless_energy_hatch.tooltip.unbind".toComponent)
            }
            return true
        }
        return false
    }

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        setUUID(uuid)
    }

    protected fun createEnergyContainer(): NetworkEnergyContainer = NetworkEnergyContainer(io)

    override fun getUUID(): UUID? = uuid

    fun setUUID(uuid: UUID?) {
        this.uuid = uuid
        markAsChanged()
        energyContainer.notifyListeners()
        for (controller in controllers) {
            if (controller is IRecipeLogicMachine) {
                controller.recipeLogic.updateTickSubscription()
            }
        }
    }

    override fun shouldOpenUI(player: Player, hand: InteractionHand, hit: BlockHitResult): Boolean = false

    override fun canShared(controller: MultiblockControllerMachine, substructureName: String): Boolean = false

    companion object {

    }
}
