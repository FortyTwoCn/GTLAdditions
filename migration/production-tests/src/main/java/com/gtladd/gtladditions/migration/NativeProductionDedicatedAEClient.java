package com.gtladd.gtladditions.migration;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import brachy.modularui.api.IMuiScreen;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.integration.recipeviewer.handlers.IngredientProvider;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.textfield.TextFieldWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.AEConfigSyncHandler;
import com.gregtechceu.gtceu.integration.ae2.gui.AEConfigWidget;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.gtladd.gtladditions.migration.NativeProductionDedicatedState.*;

/** Drives the genuine native AE menus without access to a server, shared fixture fields or test packets. */
final class NativeProductionDedicatedAEClient {
    private static int machine, phase, ticks;
    private static boolean opened, capturing;

    static boolean tick(Minecraft client) {
        if (phase == 6) return true;
        if (capturing) return false;
        boolean fluid = machine == 1;
        if (!opened) {
            if (client.screen != null) return false;
            var position = fluid ? AE_FLUID : AE_ITEM;
            if (client.level.getBlockEntity(position) == null) return false;
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(position.getCenter(), Direction.NORTH, position, false));
            opened = true; ticks = 0; return false;
        }
        if (!(client.screen instanceof IMuiScreen ui) || !ui.screen().getMainPanel().isValid()
                || ui.screen().getContext().getGraphics() == null || ++ticks < 20) return false;
        var main = ui.screen().getMainPanel();
        var widget = (AEConfigWidget) all(main).stream().filter(AEConfigWidget.class::isInstance).findFirst().orElseThrow();
        var sync = ui.screen().getSyncManager().getPanelSyncManager(main.getName())
                .findSyncHandler("ae_config_display", AEConfigSyncHandler.class);
        var config = sync.getClientConfig(AE_SLOT);
        var stock = sync.getClientStock(AE_SLOT);
        if (config == null || stock == null) return false;
        if (!aeKey(fluid).equals(config.what()) || !aeKey(fluid).equals(stock.what())
                || stock.amount() != Long.MAX_VALUE || sync.isAllowC2S())
            throw new AssertionError("Separate client's native AE S2C changed exact stock, component key or server ownership");
        double x = widget.getArea().x + 2 * 18 + 9, y = widget.getArea().y + 38 + 9;
        if (phase == 0) {
            long expected = reload() ? Long.MAX_VALUE - 1 : LARGE;
            if (config.amount() != expected) throw new AssertionError("Separate client's initial native AE amount is inexact: " + config.amount());
            hover(client, ui, x, y);
            next(reload() ? 5 : 1);
        } else if (phase == 1) {
            checkIngredient(widget, aeKey(fluid));
            click(client, ui, x, y); next(2);
        } else if (phase == 2) {
            var popup = ui.screen().getPanelManager().getOpenPanels().stream()
                    .filter(panel -> panel.getName().equals("ae_amount_editor")).findFirst().orElse(null);
            if (popup == null) return false;
            var area = popup.getArea();
            if (area.x < 0 || area.y < 0 || area.x + area.width > client.screen.width || area.y + area.height > client.screen.height)
                throw new AssertionError("Native AE amount editor exceeds the physical client window");
            var field = (TextFieldWidget) all(popup).stream().filter(TextFieldWidget.class::isInstance).findFirst().orElseThrow();
            click(client, ui, field.getArea().x + 5, field.getArea().y + 9);
            if (!field.isFocused()) throw new AssertionError("Native AE field did not receive physical focus");
            for (int i = 0; i < 32; i++) key(client, GLFW.GLFW_KEY_RIGHT);
            for (int i = 0; i < 32; i++) key(client, GLFW.GLFW_KEY_BACKSPACE);
            for (char digit : Long.toString(Long.MAX_VALUE).toCharArray()) {
                if (!ClientHooks.onScreenCharTypedPre(client.screen, digit, 0) && !client.screen.charTyped(digit, 0))
                    ClientHooks.onScreenCharTypedPost(client.screen, digit, 0);
            }
            if (!field.getText().equals(Long.toString(Long.MAX_VALUE)))
                throw new AssertionError("Actual AE text input lost long digits: " + field.getText());
            var confirm = all(popup).stream().filter(ButtonWidget.class::isInstance).toList().getLast();
            click(client, ui, confirm.getArea().x + 9, confirm.getArea().y + 9); next(3);
        } else if (phase == 3) {
            if (config.amount() != Long.MAX_VALUE) return false;
            hover(client, ui, x, y);
            boolean handled = NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Pre(client.screen, x, y, 0, -1)).isCanceled();
            if (!handled) handled = client.screen.mouseScrolled(x, y, 0, -1);
            if (!handled) throw new AssertionError("Native AE menu rejected the physical long decrement wheel event");
            next(4);
        } else if (phase == 4) {
            if (config.amount() != Long.MAX_VALUE - 1) return false;
            next(5);
        } else if (phase == 5) {
            if (config.amount() != Long.MAX_VALUE - 1) throw new AssertionError("Native AE final exact quantity changed");
            hover(client, ui, x, y + 18);
            // Let native rendering update the hovered stock used by its JEI ingredient provider.
            if (ticks < 30) return false;
            checkIngredient(widget, aeKey(fluid));
            capturing = true;
            String filename = "production-dedicated-ae-" + (fluid ? "fluid" : "item") + (reload() ? "-reload" : "") + ".png";
            Screenshot.grab(client.gameDirectory, filename, client.getMainRenderTarget(), message -> client.execute(() -> {
                LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_AE_CLIENT fluid={} exact_long_s2c=true component_key=true native_popup_and_wheel={} stock_maximum=true jei_hover=true reload={}",
                        fluid, !reload(), reload());
                capturing = false;
                if (++machine == 2) phase = 6;
                else { client.player.closeContainer(); opened = false; phase = ticks = 0; }
            }));
        }
        return false;
    }

    private static void next(int value) {
        phase = value; ticks = 0;
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_DEDICATED_AE_CLIENT_STAGE fluid={} phase={} reload={}", machine == 1, phase, reload());
    }
    private static List<IWidget> all(IWidget parent) {
        var result = new ArrayList<IWidget>(); result.add(parent);
        for (var child : parent.getChildren()) result.addAll(all(child));
        return result;
    }
    private static void checkIngredient(AEConfigWidget widget, AEKey expected) {
        if (!(widget instanceof IngredientProvider<?> provider) || provider.getIngredients().isEmpty())
            throw new AssertionError("Native AE widget has no hovered recipe ingredient");
        var stack = provider.getIngredients().getStacks().getFirst();
        AEKey key = stack instanceof ItemStack item ? AEItemKey.of(item) : AEFluidKey.of((FluidStack) stack);
        if (!expected.equals(key) || !(provider.ingredientOverride() instanceof mezz.jei.api.runtime.IClickableIngredient<?>))
            throw new AssertionError("Separate client's native JEI ingredient lost item/fluid components");
    }
    private static void hover(Minecraft client, IMuiScreen ui, double x, double y) {
        try {
            var move = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            move.setAccessible(true);
            move.invoke(client.mouseHandler, client.getWindow().getWindow(),
                    x * client.getWindow().getScreenWidth() / client.screen.width,
                    y * client.getWindow().getScreenHeight() / client.screen.height);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        ui.screen().getContext().updateState((int) x, (int) y, 0); ui.screen().onFrameUpdate();
    }
    private static void click(Minecraft client, IMuiScreen ui, double x, double y) {
        hover(client, ui, x, y); client.screen.afterMouseAction();
        boolean handled = ClientHooks.onScreenMouseClickedPre(client.screen, x, y, 0);
        if (!handled) handled = ClientHooks.onScreenMouseClickedPost(client.screen, x, y, 0, client.screen.mouseClicked(x, y, 0));
        if (!handled) throw new AssertionError("Native AE physical mouse click was rejected");
        if (!ClientHooks.onScreenMouseReleasedPre(client.screen, x, y, 0))
            ClientHooks.onScreenMouseReleasedPost(client.screen, x, y, 0, client.screen.mouseReleased(x, y, 0));
    }
    private static void key(Minecraft client, int code) {
        if (!ClientHooks.onScreenKeyPressedPre(client.screen, code, 0, 0) && !client.screen.keyPressed(code, 0, 0))
            ClientHooks.onScreenKeyPressedPost(client.screen, code, 0, 0);
    }
    private NativeProductionDedicatedAEClient() {}
}
