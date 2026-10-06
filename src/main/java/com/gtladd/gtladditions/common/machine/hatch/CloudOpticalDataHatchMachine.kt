package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch
import com.gregtechceu.gtceu.api.capability.IOpticalDataAccessHatch
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.DataBankMachine
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition
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
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.UUID

class CloudOpticalDataHatchMachine(holder: BlockEntityCreationInfo) :
    MultiblockPartMachine(holder),
    IOpticalDataAccessHatch,
    IDataStickInteractable,
    ICloudTeamBindable, IMuiMachine {

    @field:SaveField
    @field:SyncToClient
    var teamId: UUID? = null
        private set

    override fun setOwnerUUID(uuid: UUID) {
        super.setOwnerUUID(uuid)
        if (!isRemote) setUUID(uuid)
    }

    override fun onDataStickUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickShiftUse(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun isRecipeAvailable(recipe: GTRecipe, seen: MutableCollection<IDataAccessHatch>): Boolean {
        if (!seen.add(this)) return false
        return CloudNetworkManager.isRecipeAvailableInCloud(recipe, teamId)
    }

    override fun modifyRecipe(recipe: GTRecipe): GTRecipe? {
        if (recipe.conditions.none { it is ResearchCondition }) return recipe
        if (CloudNetworkManager.isRecipeAvailableInCloud(recipe, teamId)) return recipe
        for (controller in controllers) {
            if (controller is DataBankMachine) continue
            if (controller is IRecipeLogicMachine) {
                RecipeResult.of(controller, RecipeResult.FAIL_NO_FIND_RESEARCHED)
            }
        }
        return null
    }

    override fun isCreative(): Boolean = false

    override fun isTransmitter(): Boolean = false

    override fun shouldOpenUI(player: Player?, hand: InteractionHand?, hit: BlockHitResult?): Boolean = false

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        if (teamId == uuid) return
        teamId = uuid
        markAsChanged()
        syncDataHolder.markClientSyncFieldDirty("teamId")
    }

}