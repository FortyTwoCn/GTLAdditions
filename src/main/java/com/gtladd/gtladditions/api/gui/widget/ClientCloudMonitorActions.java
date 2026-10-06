package com.gtladd.gtladditions.api.gui.widget;

import com.gtladd.gtladditions.common.data.CloudMachineSnapshot;
import com.gtladd.gtladditions.events.ClientCloudHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class ClientCloudMonitorActions {
    static void highlight(CloudComputationMonitorOverviewPage.Row row) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) return;
        for (var machine : row.machines()) {
            if (!machine.getDimensionId().isEmpty()) ClientCloudHighlighter.highlight(machine.getPos(), machine.getDimensionId());
            if (row.kind() == 0 && machine.getDimensionId().equals(level.dimension().location().toString())) {
                var pos = machine.getPos();
                player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
            } else if (!machine.getDimensionId().isEmpty()) showLocation(player, machine);
        }
        minecraft.setScreen(null);
    }
    private static void showLocation(net.minecraft.world.entity.player.Player player, CloudMachineSnapshot location) {
        var target = location.getFrontPos() == null ? location.getPos() : location.getFrontPos();
        var pos = location.getPos();
        var coordinates = Component.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]").withStyle(ChatFormatting.GREEN);
        if (player.hasPermissions(2)) coordinates.withStyle(style -> style.withUnderlined(true).withClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in " + location.getDimensionId() + " run tp @s " +
                        (target.getX() + 0.5) + " " + target.getY() + " " + (target.getZ() + 0.5))));
        player.displayClientMessage(Component.translatable("gui.gtladditions.cloud_monitor.cross_dim",
                Component.literal("[" + location.getDimensionId() + "]").withStyle(ChatFormatting.GREEN), coordinates), false);
    }
    private ClientCloudMonitorActions() {}
}
