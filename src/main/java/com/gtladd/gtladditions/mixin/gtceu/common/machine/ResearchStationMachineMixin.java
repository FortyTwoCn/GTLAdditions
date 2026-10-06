package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gtladd.gtladditions.api.machine.ICloudTeamBindable;
import com.gtladd.gtladditions.utils.CloudTeamUtil;
import com.gtladd.gtladditions.api.gui.NativeHatchUI;
import com.hepdd.gtmthings.utils.TeamUtil;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mixin(ResearchStationMachine.class)
public abstract class ResearchStationMachineMixin extends WorkableElectricMultiblockMachine
        implements IDataStickInteractable, ICloudTeamBindable {
    @Unique @SaveField private @Nullable UUID gtladditions$teamId;
    protected ResearchStationMachineMixin(BlockEntityCreationInfo holder) { super(holder); }
    @Override public @Nullable UUID getUUID() { return gtladditions$teamId; }
    @Override public void setUUID(@Nullable UUID uuid) {
        UUID normalized = CloudTeamUtil.INSTANCE.normalize(uuid);
        if (!isRemote() && !Objects.equals(gtladditions$teamId, normalized)) {
            gtladditions$teamId = normalized; setChanged();
        }
    }
    @Override public InteractionResult onDataStickUse(Player player, ItemStack stack) {
        return CloudTeamUtil.INSTANCE.bindFromDataStick(this, player);
    }
    @Override public InteractionResult onDataStickShiftUse(Player player, ItemStack stack) {
        return CloudTeamUtil.INSTANCE.unbindFromDataStick(this, player);
    }
    @Override public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        if (context.getPlayer() != null && context.getItemInHand().is(GTItems.TOOL_DATA_STICK.asItem()))
            return context.getPlayer().isShiftKeyDown() ? onDataStickShiftUse(context.getPlayer(), context.getItemInHand())
                    : onDataStickUse(context.getPlayer(), context.getItemInHand());
        return super.onUseWithItem(context);
    }
    @Override public boolean onLeftClick(Player player, InteractionHand hand, @Nullable Direction face) {
        if (player.getItemInHand(hand).is(GTItems.TOOL_DATA_STICK.asItem()))
            return onDataStickShiftUse(player, player.getItemInHand(hand)).consumesAction();
        return super.onLeftClick(player, hand, face);
    }
    @Inject(method = "getWidgetsForDisplay", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$cloudStatus(PanelSyncManager sync, CallbackInfoReturnable<List<IWidget>> cir) {
        cir.getReturnValue().add(NativeHatchUI.componentList("researchCloudBinding", sync, () -> {
            if (!isFormed()) return List.of();
            return List.of(gtladditions$teamId != null && TeamUtil.hasOwner(getLevel(), gtladditions$teamId)
                    ? Component.translatable("gui.gtladditions.cloud.bind_success", TeamUtil.GetName(getLevel(), gtladditions$teamId))
                    : Component.translatable("gui.gtladditions.cloud.not_bound"));
        }, 45));
    }
}
