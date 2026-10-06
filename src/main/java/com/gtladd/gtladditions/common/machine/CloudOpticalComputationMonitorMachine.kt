package com.gtladd.gtladditions.common.machine

import brachy.modularui.factory.PosGuiData
import brachy.modularui.screen.UISettings
import brachy.modularui.value.sync.PanelSyncManager
import brachy.modularui.widget.ParentWidget
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.gui.widget.CloudComputationMonitorOverviewPage
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.gtladd.gtladditions.api.machine.ICloudTeamBindable
import com.hepdd.gtmthings.utils.TeamUtil
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.UUID

class CloudOpticalComputationMonitorMachine(holder: BlockEntityCreationInfo) :
    MetaMachine(holder),
    IMuiMachine,
    IDataStickInteractable,
    ICloudTeamBindable {

    @field:SaveField
    @field:SyncToClient
    var teamId: UUID? = null
        private set

    override fun isRemote(): Boolean = super<MetaMachine>.isRemote

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        if (teamId == uuid) return
        teamId = uuid
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("teamId")
    }

    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickShiftUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        if (!isRemote) setUUID(uuid)
    }

    override fun buildMainUI(
        mainWidget: ParentWidget<*>, guiData: PosGuiData,
        syncManager: PanelSyncManager, settings: UISettings
    ) {
        mainWidget.child(CloudComputationMonitorOverviewPage.create(this, syncManager, { teamId }, {
            mutableListOf<Component>().also(::addDisplayText)
        }))
    }

    private fun addDisplayText(textList: MutableList<Component>) {
        if (isRemote) return
        val boundPlayerId = teamId
        if (boundPlayerId == null) {
            textList.add(
                Component.translatable("gui.gtladditions.cloud.not_bound").withStyle(ChatFormatting.RED)
            )
            return
        }
        textList.add(self().blockState.block.name)
        if (TeamUtil.hasOwner(level, boundPlayerId)) {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud.bind_success",
                    TeamUtil.GetName(level, boundPlayerId)
                )
            )
        }
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.provider_count",
                CloudNetworkManager.getProviderCount(boundPlayerId)
            )
        )
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.max_cwu",
                FormattingUtil.formatNumbers(CloudNetworkManager.getMaxCWU(boundPlayerId))
            )
        )
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.requestable_cwu",
                FormattingUtil.formatNumbers(CloudNetworkManager.getRemainingCWU(boundPlayerId))
            )
        )
    }

}
