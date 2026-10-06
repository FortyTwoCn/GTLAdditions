package com.gtladd.gtladditions.api.gui;

import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.items.IItemHandler;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.value.sync.FluidSlotSyncHandler;
import brachy.modularui.value.sync.InteractionSyncHandler;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ScrollWidget;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.FluidSlot;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import java.util.List;
import java.util.function.Supplier;

/** Native slots and server-authorized controls shared by the dual hatches. */
public final class NativeHatchUI {
    public static ScrollWidget<?> componentList(String name, PanelSyncManager manager,
                                               Supplier<List<Component>> text, int height) {
        // Native scroll content may exceed the viewport. Vertical padding on both ends makes
        // ModularUI's dimension sizer treat that expected overflow as a layout failure.
        return com.hepdd.gtmthings.api.gui.ComponentListUI.create(name, manager, text, height)
                .paddingTop(0).paddingBottom(0);
    }

    public static Flow sharedItems(String name, IItemHandler inventory, PanelSyncManager manager) {
        var group = new SlotGroup(name, inventory.getSlots());
        manager.registerSlotGroup(group);
        var column = Flow.column().coverChildren().childPadding(2);
        column.child(Text.lang("gui.gtmthings.share_inventory.title").asWidget().tooltip(t -> t
                .addLine(Component.translatable("gui.gtmthings.share_inventory.desc.0"))
                .addLine(Component.translatable("gui.gtmthings.share_inventory.desc.1"))
                .addLine(Component.translatable("gui.gtmthings.share_inventory.desc.2"))));
        for (int first = 0; first < inventory.getSlots(); first += 3) {
            var row = Flow.row().coverChildren();
            for (int i = first; i < Math.min(first + 3, inventory.getSlots()); i++)
                row.child(new ItemSlot().slot(new ModularSlot(inventory, i).slotGroup(group)));
            column.child(row);
        }
        return column;
    }

    public static Flow sharedFluids(String name, NotifiableFluidTank inventory) {
        var column = Flow.column().coverChildren().childPadding(2);
        column.child(Text.lang("gui.gtceu.share_tank.title").asWidget().tooltip(t -> t
                .addLine(Component.translatable("gui.gtceu.share_tank.desc.0"))
                .addLine(Component.translatable("gui.gtceu.share_inventory.desc.1"))));
        for (int first = 0; first < inventory.getTanks(); first += 3) {
            var row = Flow.row().coverChildren();
            for (int i = first; i < Math.min(first + 3, inventory.getTanks()); i++)
                row.child(new FluidSlot().name(name + i).syncHandler(new FluidSlotSyncHandler(inventory.getStorages()[i])));
            column.child(row);
        }
        return column;
    }

    public static ButtonWidget<?> refund(Runnable action) {
        return new ButtonWidget<>().widthRel(1).height(18).overlay(Text.lang("gui.gtmthings.refund"))
                .tooltip(t -> t.addLine(Component.translatable("gtmthings.machine.huge_item_bus.tooltip.1")))
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> { if (!mouse.isClient()) action.run(); }));
    }

    public static ScrollWidget<?> scroll(Flow column) {
        return new ScrollWidget<>(new VerticalScrollData()).widthRel(1).height(136).child(column);
    }

    private NativeHatchUI() {}
}
