package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.AutoMaintenanceHatchPartMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IAutoConfigurationMaintenanceHatch;
import org.spongepowered.asm.mixin.*;
import java.util.List;

/** Native slot and duration controls share the authoritative multiplier and survive normal world saves. */
@Mixin(AutoConfigurationMaintenanceHatchPartMachine.class)
public abstract class AutoConfigurationMaintenanceHatchPartMachineMixin extends AutoMaintenanceHatchPartMachine
        implements IAutoConfigurationMaintenanceHatch {
    @Shadow(remap = false) private float gtlDurationMultiplier;
    @Shadow(remap = false) protected abstract Flow durationControls(PanelSyncManager sync);
    @Unique private final NotifiableItemStackHandler gtladditions$mainframe = gtladditions$createStorage();
    public AutoConfigurationMaintenanceHatchPartMachineMixin(BlockEntityCreationInfo holder) { super(holder); }
    @Override public boolean canShared(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller, String substructureName) { return true; }
    /** @author Dragons @reason Clamp against the installed mainframe on the native saved multiplier. */
    @Overwrite public void setDurationMultiplier(float value) {
        if (!Float.isFinite(value)) return;
        gtlDurationMultiplier = Math.clamp(value, gtladditions$minimum(), gtladditions$maximum());
        setChanged(); syncDataHolder.markClientSyncFieldDirty("gtlDurationMultiplier");
    }
    /** @author Dragons @reason Keep the native duration buttons and add the physical upgrade slot. */
    @Overwrite public void buildMainUI(ParentWidget<?> main, PosGuiData data, PanelSyncManager sync, UISettings settings) {
        main.size(170, 100).child(durationControls(sync).left(3).top(3));
        sync.registerSlotGroup(new SlotGroup("maintenanceMainframe", 1));
        main.child(new ItemSlot().name("maintenanceMainframe").right(3).bottom(3)
                .slot(new ModularSlot(gtladditions$mainframe.storage, 0).slotGroup("maintenanceMainframe"))
                .tooltip(tooltip -> gtladditions$tooltips().forEach(tooltip::addLine)));
    }
    @Unique private NotifiableItemStackHandler gtladditions$createStorage() {
        var handler = attachTrait(new NotifiableItemStackHandler(1, IO.NONE, IO.BOTH,
                size -> new CustomItemStackHandler(size) { @Override public int getSlotLimit(int slot) { return 1; } }));
        handler.addChangedListener(() -> setDurationMultiplier(gtlDurationMultiplier));
        return handler;
    }
    @Unique private String gtladditions$mainframeId() {
        var stack = gtladditions$mainframe.getStackInSlot(0);
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    @Unique private float gtladditions$minimum() {
        return switch (gtladditions$mainframeId()) {
            case "kubejs:bioware_mainframe" -> .15f;
            case "kubejs:cosmic_mainframe" -> .1f;
            case "kubejs:suprachronal_mainframe_complex" -> .05f;
            default -> .2f;
        };
    }
    @Unique private float gtladditions$maximum() {
        return switch (gtladditions$mainframeId()) {
            case "kubejs:bioware_mainframe" -> 3f;
            case "kubejs:cosmic_mainframe" -> 7.5f;
            case "kubejs:suprachronal_mainframe_complex" -> 25f;
            default -> 1.2f;
        };
    }
    @Unique private List<Component> gtladditions$tooltips() {
        return List.of(Component.translatable("gtceu.universal.enabled"),
                Component.translatable("gtceu.multiblock.use_different_mainframe"),
                Component.translatable("gtceu.multiblock.use_bioware_mainframe", 3.0, .15),
                Component.translatable("gtceu.multiblock.use_cosmic_mainframe", 7.5, .1),
                Component.translatable("gtceu.multiblock.use_suprachronal_mainframe_complex", 25.0, .05),
                GTLAddMachines.INSTANCE.getGTLAdd_MODIFY());
    }
}
