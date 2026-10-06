package com.gtladd.gtladditions.migration;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.leodreamer.wildcard_pattern.WildcardItems;
import org.leodreamer.wildcard_pattern.gui.GenericGTTag;
import org.leodreamer.wildcard_pattern.wildcard.WildcardPatternLogic;
import org.leodreamer.wildcard_pattern.wildcard.impl.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_PLATE;

/** Actual item-use/open, native mouse callbacks and packets, exact long wheel/save, dynamic slots and small screen. */
public final class NativeProductionWildcardClientProbe {
    private static final long LARGE = 9_007_199_254_741_019L;
    private static int stage, ticks, initialSlots, addedTags;
    private static CompletableFuture<?> pending;

    private static ModularUIGuiContainer screen(Minecraft client) {
        if (!(client.screen instanceof ModularUIGuiContainer ui)) return null;
        if (ui.modularUI.getWidgetsById("wildcard_tab_input").isEmpty()) return null;
        return ui;
    }
    private static Widget widget(ModularUIGuiContainer screen, String id) {
        var matches = screen.modularUI.getWidgetsById(id);
        if (matches.size() != 1) throw new AssertionError("Missing/ambiguous genuine wildcard widget: " + id);
        return matches.getFirst();
    }
    private static void click(Minecraft client, Widget widget) {
        double x = widget.getPositionX() + widget.getSizeWidth() / 2.0;
        double y = widget.getPositionY() + widget.getSizeHeight() / 2.0;
        client.screen.afterMouseAction();
        boolean handled = net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPre(client.screen, x, y, 0);
        if (!handled) handled = net.neoforged.neoforge.client.ClientHooks.onScreenMouseClickedPost(
                client.screen, x, y, 0, client.screen.mouseClicked(x, y, 0));
        if (!handled) throw new AssertionError("Native wildcard UI did not handle actual mouse click");
        if (!net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPre(client.screen, x, y, 0))
            net.neoforged.neoforge.client.ClientHooks.onScreenMouseReleasedPost(
                    client.screen, x, y, 0, client.screen.mouseReleased(x, y, 0));
    }
    private static void bounds(ModularUIGuiContainer screen) {
        var ui = screen.modularUI;
        if (ui.getGuiLeft() < 0 || ui.getGuiTop() < 0 || ui.getGuiLeft() + ui.getWidth() > screen.width
                || ui.getGuiTop() + ui.getHeight() > screen.height)
            throw new AssertionError("Genuine wildcard editor exceeds the scaled screen");
    }
    public static boolean tick(Minecraft client) {
        var server = client.getSingleplayerServer();
        var id = client.player.getUUID();
        if (stage == 0) {
            client.player.closeContainer();
            pending = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var stack = WildcardItems.WILDCARD_PATTERN.asStack();
                var named = new ItemStack(Items.DIAMOND);
                named.set(DataComponents.CUSTOM_NAME, Component.literal("native wildcard editor exact component"));
                var logic = WildcardPatternLogic.on(stack, player.level().registryAccess());
                logic.setIOComponents(WildcardPatternLogic.IO.IN, List.of(new SimpleIOComponent(new GenericStack(AEItemKey.of(named), LARGE))));
                logic.setIOComponents(WildcardPatternLogic.IO.OUT, List.of(new TagIOComponent(GenericGTTag.item(TagPrefix.dust), 1)));
                logic.setFilterComponents(List.of(new SimpleFilterComponent(GTMaterials.Copper, true),
                        new FlagFilterComponent(GENERATE_PLATE, GTMaterials.Copper, true),
                        new PropertyFilterComponent(PropertyKey.INGOT, GTMaterials.Copper, true)));
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.inventoryMenu.broadcastChanges();
            });
            stage = 1;
            return false;
        }
        if (stage == 1) {
            if (!pending.isDone() || !client.player.getMainHandItem().is(WildcardItems.WILDCARD_PATTERN.get())) return false;
            pending.join();
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            stage = 2;
            ticks = 0;
            return false;
        }
        if (stage == 15) return true;
        if (stage == 10) {
            if (++ticks < 15) return false;
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            stage = 11; ticks = 0;
            return false;
        }
        var ui = screen(client);
        if (ui == null || ++ticks < 12) return false;
        bounds(ui);
        if (stage == 2) {
            initialSlots = ui.modularUI.getSlotMap().size();
            if (initialSlots != 41) throw new AssertionError("Wildcard native slots do not match its genuine inventories: " + initialSlots);
            click(client, widget(ui, "wildcard_tab_input"));
            stage = 3; ticks = 0;
        } else if (stage == 3) {
            var quantity = (TextFieldWidget) widget(ui, "wildcard_simple_quantity");
            if (!quantity.getCurrentString().equals(Long.toString(LARGE))) throw new AssertionError("Wildcard GUI truncated its initial native long amount");
            click(client, quantity);
            double x = quantity.getPositionX() + 4, y = quantity.getPositionY() + 4;
            if (!client.screen.mouseScrolled(x, y, 0, 1)) throw new AssertionError("Native wildcard numeric wheel did not handle input");
            if (!quantity.getCurrentString().equals(Long.toString(LARGE + 1))) throw new AssertionError("Native wildcard numeric wheel rounded a long amount");
            click(client, widget(ui, "wildcard_save_in"));
            stage = 4; ticks = 0; pending = null;
        } else if (stage == 4) {
            if (pending == null) pending = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var logic = WildcardPatternLogic.on(player.getMainHandItem(), player.level().registryAccess());
                return logic.getIOStacks(WildcardPatternLogic.IO.IN, GTMaterials.Copper)[0].amount();
            });
            if (!pending.isDone()) return false;
            if (!pending.join().equals(LARGE + 1)) throw new AssertionError("Actual wildcard save C2S did not persist the exact long wheel edit");
            click(client, widget(ui, "wildcard_add_tag_in"));
            stage = 5; ticks = 0;
        } else if (stage == 5) {
            if (ui.modularUI.getSlotMap().size() != initialSlots + 1) throw new AssertionError("Wildcard dynamic native ghost slot did not register");
            var quantity = (TextFieldWidget) widget(ui, "wildcard_simple_quantity");
            if (!quantity.getCurrentString().equals(Long.toString(LARGE + 1))) throw new AssertionError("Adding a wildcard component discarded the existing edit");
            click(client, widget(ui, "wildcard_save_in"));
            click(client, widget(ui, "wildcard_tab_preview"));
            stage = 6; ticks = 0;
        } else if (stage == 6) {
            if (ticks < 45) return false;
            if (ui.modularUI.getSlotMap().size() != initialSlots + 1) throw new AssertionError("Wildcard preview cycling mutated synchronized menu slots");
            client.getWindow().setWindowed(960, 480);
            stage = 7; ticks = 0;
        } else if (stage == 7) {
            click(client, widget(ui, "wildcard_tab_filter"));
            stage = 8; ticks = 0;
        } else if (stage == 8) {
            for (int index = 0; index < 3; index++) click(client, widget(ui, "wildcard_toggle_filter_" + index));
            click(client, widget(ui, "wildcard_save_filter"));
            stage = 9; ticks = 0;
        } else if (stage == 9) {
            Screenshot.grab(client.gameDirectory, "production-wildcard-editor-small-screen.png", client.getMainRenderTarget(), message -> {});
            client.player.closeContainer();
            client.getWindow().setWindowed(1280, 900);
            stage = 10; ticks = 0;
        } else if (stage == 11) {
            if (ui.modularUI.getSlotMap().size() != initialSlots + 1) throw new AssertionError("Reopening wildcard editor lost its saved components");
            var quantity = (TextFieldWidget) widget(ui, "wildcard_simple_quantity");
            if (!quantity.getCurrentString().equals(Long.toString(LARGE + 1))) throw new AssertionError("Reopening wildcard editor lost its saved long amount");
            var logic = WildcardPatternLogic.on(client.player.getMainHandItem(), client.level.registryAccess());
            if (logic.getFilterComponents().size() != 3 || logic.getFilterComponents().stream().anyMatch(component -> component.isWhitelist()))
                throw new AssertionError("Actual filter toggle C2S/S2C did not survive closing and reopening");
            click(client, widget(ui, "wildcard_tab_input"));
            stage = 12; ticks = 0;
        } else if (stage == 12) {
            if (addedTags < 4) {
                click(client, widget(ui, "wildcard_add_tag_in"));
                addedTags++; ticks = 0;
                return false;
            }
            if (ui.modularUI.getSlotMap().size() != initialSlots + 5) throw new AssertionError("Six wildcard components did not own stable native ghost slots");
            var scroll = (com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup) widget(ui, "wildcard_scroll_in");
            double x = scroll.getPositionX() + 60, y = scroll.getPositionY() + 60;
            // LDLib clamps each real wheel event to one step, irrespective of the event's magnitude.
            for (int step = 0; step < 8; step++) if (!client.screen.mouseScrolled(x, y, 0, -1))
                throw new AssertionError("Real wheel events cannot scroll the six-component editor");
            if (scroll.getScrollYOffset() == 0) throw new AssertionError("Wildcard native scroll offset did not change");
            var lastInput = ui.modularUI.getWidgetsById("wildcard_tag_quantity").get(4);
            if (lastInput.getPositionY() < scroll.getPositionY() || lastInput.getPositionY() + lastInput.getSizeHeight() > scroll.getPositionY() + scroll.getSizeHeight())
                throw new AssertionError("Sixth wildcard component cannot be reached in its real scroll viewport");
            click(client, widget(ui, "wildcard_save_in"));
            stage = 13; ticks = 0; pending = null;
        } else if (stage == 13) {
            if (pending == null) pending = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var logic = WildcardPatternLogic.on(player.getMainHandItem(), player.level().registryAccess());
                if (logic.getIOComponents(WildcardPatternLogic.IO.IN).size() != 6
                        // Newly added tag rows are intentionally unconfigured and cannot expand yet.
                        || logic.getIOComponents(WildcardPatternLogic.IO.IN).getFirst().apply(GTMaterials.Copper).amount() != LARGE + 1
                        || logic.getFilterComponents().stream().anyMatch(component -> component.isWhitelist()))
                    throw new AssertionError("Actual six-component wildcard C2S save changed existing values");
            });
            if (!pending.isDone()) return false;
            pending.join();
            stage = 14; ticks = 0;
        } else if (stage == 14) {
            Screenshot.grab(client.gameDirectory, "production-wildcard-editor-six-components.png", client.getMainRenderTarget(), message -> {});
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WILDCARD_CLIENT native_item_use=true native_editor=true exact_long_wheel=true save_c2s=true dynamic_slots=true preview_slot_stability=true small_screen=true reopen=true three_filter_toggles=true six_components_scroll=true");
            client.player.closeContainer();
            stage = 15;
            return true;
        }
        return false;
    }
}
