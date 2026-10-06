package com.gtladd.gtladditions.mixin.gtlcore.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.gtladd.gtladditions.api.gui.NativeHatchUI;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.Widget;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;
import java.util.List;

@Mixin(WorkableElectricMultiblockMachine.class)
@Implements(@Interface(iface = IWirelessElectricMultiblockMachine.class, prefix = "gtladditions$"))
public abstract class WorkableElectricMultiblockMachineMixin {
    @Unique private @Nullable IWirelessNetworkEnergyHandler gtladditions$wirelessNetworkEnergyHandler;
    @Inject(method = "invalidateStructure", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditionsInternal$clearWireless(String name, CallbackInfo ci) { gtladditions$wirelessNetworkEnergyHandler = null; }
    @Inject(method = "onPartUnload", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditionsInternal$unloadWireless(CallbackInfo ci) { gtladditions$wirelessNetworkEnergyHandler = null; }
    @Unique public void gtladditions$setWirelessNetworkEnergyHandler(IWirelessNetworkEnergyHandler trait) {
        gtladditions$wirelessNetworkEnergyHandler = trait;
    }
    @Unique public @Nullable IWirelessNetworkEnergyHandler gtladditions$getWirelessNetworkEnergyHandler() {
        return gtladditions$wirelessNetworkEnergyHandler;
    }
    @Unique private boolean gtladditionsInternal$usesWirelessDisplay(WorkableElectricMultiblockMachine machine) {
        var handler = gtladditions$wirelessNetworkEnergyHandler;
        var logic = machine.getRecipeLogic();
        return machine.isFormed() && handler != null && handler.isOnline() &&
                (logic instanceof MultipleRecipesLogic || logic instanceof MutableRecipesLogic<?> mutable && mutable.isMultipleRecipeMode());
    }
    @Inject(method = "getWidgetsForDisplay", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditionsInternal$wirelessDisplay(PanelSyncManager sync, CallbackInfoReturnable<List<IWidget>> cir) {
        var machine = (WorkableElectricMultiblockMachine) (Object) this;
        var wireless = new BooleanSyncValue(() -> gtladditionsInternal$usesWirelessDisplay(machine));
        sync.syncValue("gtladditions:wirelessEnergyDisplay", wireless);
        var formed = sync.getOrCreateSyncHandler("isFormed", BooleanSyncValue.class, () -> new BooleanSyncValue(machine::isFormed));
        var active = sync.getOrCreateSyncHandler("isActive", BooleanSyncValue.class, () -> new BooleanSyncValue(() -> machine.getRecipeLogic().isActive()));
        var widgets = cir.getReturnValue();
        // Native GT's first two rows are tier and voltage; only replace the latter while wireless routing is active.
        if (widgets.size() > 1 && widgets.get(1) instanceof Widget<?> voltage)
            voltage.setEnabledIf(widget -> formed.getBoolValue() && active.getBoolValue() && !wireless.getBoolValue());
        widgets.add(NativeHatchUI.componentList("gtladditions:wirelessEnergyBudget", sync, () -> {
            if (!gtladditionsInternal$usesWirelessDisplay(machine)) return List.of();
            var total = gtladditions$wirelessNetworkEnergyHandler.getMaxAvailableEnergy();
            long value = NumberUtils.getLongValue(total);
            int tier = Math.min(NewGTValues.VNF.length - 1, NumberUtils.getFakeVoltageTier(value));
            return List.of(Component.translatable("gtceu.multiblock.max_energy_per_tick",
                    CommonUtils.formatBigIntegerFixed(total), Component.literal(NewGTValues.VNF[tier]))
                    .withStyle(ChatFormatting.GRAY).withStyle(style -> style.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, Component.translatable("gtceu.multiblock.max_energy_per_tick_hover")))));
        }, 45).setEnabledIf(widget -> wireless.getBoolValue()));
    }
}
