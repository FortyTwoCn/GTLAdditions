package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.common.data.GTItems
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.api.machine.wireless.NativeWirelessEnergyAccount
import com.hepdd.gtmthings.utils.TeamUtil
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext
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
import org.gtlcore.gtlcore.utils.NumberUtils
import java.math.BigInteger
import java.util.*

open class GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder: BlockEntityCreationInfo, logic: GTLAddMultipleWirelessRecipesLogic = GTLAddMultipleWirelessRecipesLogic()) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, logic) {
    @field:SaveField
    var uuid: UUID? = null
        protected set

    val selfWirelessNetworkTrait = attachTrait(SelfWirelessNetworkHandler())


    override fun getRecipeLogic(): GTLAddMultipleWirelessRecipesLogic = super.getRecipeLogic() as GTLAddMultipleWirelessRecipesLogic

    override fun onUse(context: ExtendedUseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val hand = context.hand
        if (player.getItemInHand(hand).`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!isRemote) {
                this.uuid = player.uuid
                markAsChanged()
                player.sendSystemMessage(
                    "gtmthings.machine.wireless_energy_hatch.tooltip.bind".toComponent(TeamUtil.GetName(player))
                )
                if (isFormed) refreshTier()
                getRecipeLogic().updateTickSubscription()
            }
            return InteractionResult.sidedSuccess(isRemote)
        }
        return super.onUse(context)
    }

    override fun onLeftClick(
        player: Player,
        hand: InteractionHand,
        direction: Direction?
    ): Boolean {
        val itemStack = player.getItemInHand(hand)
        if (itemStack.isEmpty) return false
        if (itemStack.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!isRemote) {
                this.uuid = null
                markAsChanged()
                getRecipeLogic().updateTickSubscription()
                player.sendSystemMessage(
                    "gtmthings.machine.wireless_energy_hatch.tooltip.unbind".toComponent
                )
            }
            return true
        }
        return false
    }

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        this.uuid = uuid
        markAsChanged()
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        this.refreshTier()
    }

    override fun getMaxVoltage(): Long = 0

    override fun getWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler = selfWirelessNetworkTrait

    fun refreshTier() {
        uuid?.let {
            val totalEu = NativeWirelessEnergyAccount.balance(it)
            val longEu = NumberUtils.getLongValue(totalEu)
            this.tier = if (longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)
        }
    }

    inner class SelfWirelessNetworkHandler :
        MachineTrait(),
        IWirelessNetworkEnergyHandler {

        override fun consumeEnergy(energy: Int): Boolean = uuid != null &&
            NativeWirelessEnergyAccount.transfer(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )

        override fun consumeEnergy(energy: Long): Boolean = uuid != null &&
            NativeWirelessEnergyAccount.transfer(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )

        override fun consumeEnergy(energy: BigInteger): Boolean = uuid != null &&
            NativeWirelessEnergyAccount.transfer(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )

        override val maxAvailableEnergy: BigInteger
            get() = if (uuid != null) NativeWirelessEnergyAccount.balance(uuid) else BigInteger.ZERO

        override val isOnline: Boolean
            get() = uuid != null && NativeWirelessEnergyAccount.balance(uuid).signum() > 0
    }
}
