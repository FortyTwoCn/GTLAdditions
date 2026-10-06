package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gtladd.gtladditions.api.machine.trait.CloudOpticalComputationContainer
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.gtladd.gtladditions.api.machine.ICloudTeamBindable
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import java.util.UUID

class CloudOpticalComputationHatchMachine(
    holder: BlockEntityCreationInfo,
    private val transmitter: Boolean
) : MultiblockPartMachine(holder),
    IDataStickInteractable,
    ICloudTeamBindable, IMuiMachine {

    val computationContainer = attachTrait(CloudOpticalComputationContainer(
        if (transmitter) IO.OUT else IO.IN,
        transmitter
    ))

    @field:SaveField
    @field:SyncToClient
    var teamId: UUID? = null
        private set

    fun isTransmitter(): Boolean = transmitter

    override fun shouldOpenUI(player: Player?, hand: InteractionHand?, hit: BlockHitResult?): Boolean = false

    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickShiftUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun onMachineDestroyed() {
        if (!isRemote) CloudNetworkManager.unregisterComputationHatch(this)
        super.onMachineDestroyed()
    }

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        if (!isRemote) setUUID(uuid)
    }

    override fun onLoad() {
        super<MultiblockPartMachine>.onLoad()
        if (!isRemote) CloudNetworkManager.registerComputationHatch(this)
    }

    override fun onUnload() {
        if (!isRemote) CloudNetworkManager.unregisterComputationHatch(this)
        super.onUnload()
    }

    override fun addedToController(controller: MultiblockControllerMachine, substructureName: String) {
        super.addedToController(controller, substructureName)
        if (!isRemote) CloudNetworkManager.invalidateComputationTopology()
    }

    override fun removedFromController(controller: MultiblockControllerMachine) {
        super.removedFromController(controller)
        if (!isRemote) CloudNetworkManager.invalidateComputationTopology()
    }

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        if (teamId == uuid) return
        teamId = uuid
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("teamId")
        CloudNetworkManager.invalidateComputationTopology()
    }

}