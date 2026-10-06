package com.gtladd.gtladditions.api.gui.widget;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gtladd.gtladditions.common.data.CloudMachineSnapshot;
import com.gtladd.gtladditions.common.data.ComputationTopologySnapshot;
import com.gtladd.gtladditions.utils.CloudNetworkManager;
import com.gtladd.gtladditions.api.gui.NativeHatchUI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.utils.serialization.network.IByteBufAdapter;
import brachy.modularui.value.sync.*;
import brachy.modularui.widget.ScrollWidget;
import brachy.modularui.widget.scroll.VerticalScrollData;
import brachy.modularui.widgets.*;
import brachy.modularui.widgets.dynamic.DynamicWidget;
import brachy.modularui.widgets.layout.Flow;

import java.util.*;
import java.util.function.Supplier;

/** Native menus retain team topology, live CWU values, sorting and local highlighting. */
public final class CloudComputationMonitorOverviewPage {
    public record Row(int kind, boolean provider, List<CloudMachineSnapshot> machines) {
        public Row { machines = List.copyOf(machines); }
    }

    public static final IByteBufAdapter<RegistryFriendlyByteBuf, Row> ROW_CODEC = new IByteBufAdapter<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, Row row) {
            buffer.writeVarInt(row.kind());
            buffer.writeBoolean(row.provider());
            buffer.writeVarInt(row.machines().size());
            for (var machine : row.machines()) {
                buffer.writeUtf(machine.getDimensionId(), 256);
                buffer.writeBlockPos(machine.getPos());
                buffer.writeBoolean(machine.getFrontPos() != null);
                if (machine.getFrontPos() != null) buffer.writeBlockPos(machine.getFrontPos());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, machine.getItem());
                buffer.writeLong(machine.getCurrentCwu());
                buffer.writeLong(machine.getMaxCwu());
                buffer.writeVarInt(machine.getRequestedCwu());
            }
        }
        @Override public Row decode(RegistryFriendlyByteBuf buffer) {
            int kind = buffer.readVarInt();
            boolean provider = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (kind < 0 || kind > 2 || size < 0 || size > 65536 || (kind == 0 && size != 1))
                throw new IllegalArgumentException("Invalid cloud monitor row");
            List<CloudMachineSnapshot> machines = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String dimension = buffer.readUtf(256);
                BlockPos pos = buffer.readBlockPos();
                BlockPos front = buffer.readBoolean() ? buffer.readBlockPos() : null;
                machines.add(new CloudMachineSnapshot(dimension, pos, front,
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readLong(), buffer.readLong(), buffer.readVarInt()));
            }
            return new Row(kind, provider, machines);
        }
        @Override public boolean areEqual(Row a, Row b) {
            if (a.kind() != b.kind() || a.provider() != b.provider() || a.machines().size() != b.machines().size()) return false;
            for (int i = 0; i < a.machines().size(); i++) {
                var x = a.machines().get(i);
                var y = b.machines().get(i);
                if (!x.getDimensionId().equals(y.getDimensionId()) || !x.getPos().equals(y.getPos()) ||
                        !Objects.equals(x.getFrontPos(), y.getFrontPos()) || !ItemStack.matches(x.getItem(), y.getItem()) ||
                        x.getCurrentCwu() != y.getCurrentCwu() || x.getMaxCwu() != y.getMaxCwu() ||
                        x.getRequestedCwu() != y.getRequestedCwu()) return false;
            }
            return true;
        }
    };

    public static Flow create(MetaMachine owner, PanelSyncManager sync, Supplier<UUID> team,
                               Supplier<List<Component>> status) {
        var pages = new PagedWidget.Controller();
        var snapshots = new SnapshotCache(owner, team);
        return Flow.column().width(284).coverChildrenHeight().childPadding(4)
                .child(Flow.row().coverChildren().childPadding(4)
                        .child(new PageButton(0, pages).size(136, 18).overlay(Text.of(owner.getBlockState().getBlock().getName())))
                        .child(new PageButton(1, pages).size(140, 18)
                                .overlay(Text.lang("gui.gtladditions.cloud_monitor.overview"))
                                .setEnabledIf(w -> team.get() != null)))
                .child(new PagedWidget<>().size(284, 180).controller(pages)
                        .addPage(NativeHatchUI.componentList("cloudComputationStatus", sync, status, 180))
                        .addPage(Flow.column().width(284).coverChildrenHeight().childPadding(4)
                                .child(section("cloudProviders", true, sync, snapshots))
                                .child(section("cloudReceivers", false, sync, snapshots))));
    }

    private static Flow section(String name, boolean provider, PanelSyncManager sync, SnapshotCache snapshots) {
        boolean[] descending = { true };
        var sort = new BooleanSyncValue(() -> descending[0], value -> descending[0] = value).allowC2S();
        sync.syncValue(name + "Sort", sort);
        var rows = GenericListSyncHandler.<RegistryFriendlyByteBuf, Row>builder()
                .getter(() -> rows(snapshots.get(), provider, descending[0]))
                .adapter(ROW_CODEC).immutableCopy().build();
        sync.syncValue(name + "Rows", rows);
        var dynamic = new DynamicLinkedSyncHandler<>(rows).widgetProvider((manager, value) -> {
            Flow list = Flow.column().widthRel(1).coverChildrenHeight().childPadding(2);
            for (Row row : value.getValue()) list.child(rowWidget(row));
            return list;
        });
        return Flow.column().widthRel(1).coverChildrenHeight().childPadding(2)
                .child(Flow.row().widthRel(1).height(18)
                        .child(Text.lang(provider ? "gui.gtladditions.cloud_monitor.providers" :
                                "gui.gtladditions.cloud_monitor.requesters").asWidget().width(206))
                        .child(new ToggleButton().value(sort).size(72, 18)
                                .overlay(Text.dynamic(() -> Component.translatable("gui.gtladditions.cloud_monitor.sort_quantity_short")
                                        .append(sort.getBoolValue() ? "▼" : "▲")))
                                .tooltip(t -> t.addLine(Component.translatable("gui.gtladditions.cloud_monitor.sort")))))
                .child(new ScrollWidget<>(new VerticalScrollData()).widthRel(1).height(66).background(GTGuiTextures.DISPLAY)
                        .child(new DynamicWidget<>().widthRel(1).coverChildrenHeight().syncHandler(dynamic)));
    }

    public static List<Row> rows(ComputationTopologySnapshot snapshot, boolean provider, boolean descending) {
        List<CloudMachineSnapshot> machines = new ArrayList<>(provider ? snapshot.getProviders() : snapshot.getReceivers());
        Comparator<CloudMachineSnapshot> order = Comparator.comparingLong(m -> provider ? m.getMaxCwu() : m.getRequestedCwu());
        if (descending) order = order.reversed();
        order = order.thenComparing(CloudMachineSnapshot::getDimensionId).thenComparingLong(m -> m.getPos().asLong());
        machines.sort(order);
        List<Row> rows = new ArrayList<>();
        for (var machine : machines) rows.add(new Row(0, provider, List.of(machine)));
        if (rows.isEmpty()) rows.add(new Row(1, provider, List.of()));
        var unbound = provider ? snapshot.getUnboundProviders() : snapshot.getUnboundReceivers();
        if (!unbound.isEmpty()) rows.add(new Row(2, provider, unbound));
        return List.copyOf(rows);
    }

    private static Flow rowWidget(Row row) {
        var machine = row.kind() == 0 ? row.machines().getFirst() : null;
        Flow result = Flow.row().widthRel(1).height(row.kind() == 0 ? 20 : 28).childPadding(3);
        if (machine != null) result.child(new ItemDrawable(machine.getItem()).asWidget().size(18)
                .tooltip(t -> t.addLine(machine.getItem().getHoverName())));
        Component text = row.kind() == 0 ?
                row.provider() ? Component.translatable("gui.gtladditions.cloud_monitor.provider_info",
                        FormattingUtil.formatNumbers(machine.getCurrentCwu()), FormattingUtil.formatNumbers(machine.getMaxCwu())) :
                        Component.translatable("gui.gtladditions.cloud_monitor.requester_info", FormattingUtil.formatNumbers(machine.getRequestedCwu())) :
                row.kind() == 1 ? Component.translatable(row.provider() ? "gui.gtladditions.cloud_monitor.no_providers" :
                        "gui.gtladditions.cloud_monitor.no_requesters") :
                        Component.translatable(row.provider() ? "gui.gtladditions.cloud_monitor.unbound_providers" :
                                "gui.gtladditions.cloud_monitor.unbound_receivers", row.machines().size());
        result.child(Text.of(text).asWidget().width(machine != null ? 180 : 202));
        if (row.kind() != 1) result.child(new ButtonWidget<>().size(56, 18)
                .overlay(Text.lang("gui.gtladditions.cloud_monitor.highlight"))
                .tooltip(t -> {
                    if (machine == null) t.addLine(Component.translatable("gui.gtladditions.cloud_monitor.highlight_all"));
                    else {
                        t.addLine(Component.translatable("gui.gtladditions.cloud_monitor.tooltip_dim", machine.getDimensionId()));
                        var pos = machine.getPos();
                        t.addLine(Component.translatable("gui.gtladditions.cloud_monitor.tooltip_pos", pos.getX(), pos.getY(), pos.getZ()));
                    }
                })
                .onMousePressed((context, button) -> {
                    if (button != 0) return false;
                    ClientCloudMonitorActions.highlight(row);
                    return true;
                }));
        return result;
    }

    private static final class SnapshotCache {
        private final MetaMachine owner;
        private final Supplier<UUID> team;
        private long tick = Long.MIN_VALUE;
        private UUID previousTeam;
        private ComputationTopologySnapshot snapshot = new ComputationTopologySnapshot(-1, List.of(), List.of(), List.of(), List.of());
        SnapshotCache(MetaMachine owner, Supplier<UUID> team) { this.owner = owner; this.team = team; }
        ComputationTopologySnapshot get() {
            if (owner.getLevel() == null || owner.getLevel().isClientSide) return snapshot;
            long now = owner.getLevel().getGameTime();
            UUID id = team.get();
            if (now == tick || (tick != Long.MIN_VALUE && now >= tick && now - tick < 20 && Objects.equals(id, previousTeam))) return snapshot;
            tick = now;
            previousTeam = id;
            var cloud = CloudNetworkManager.INSTANCE;
            if (snapshot.getTopologyVersion() != cloud.getTopologyVersion() || snapshot.getTopologyVersion() == -1 ||
                    !Objects.equals(id, cachedTeam)) {
                snapshot = cloud.getComputationTopologySnapshot(id);
                cachedTeam = id;
                return snapshot;
            }
            var live = cloud.getComputationLiveSnapshot(id);
            if (live.getProviderCount() != snapshot.getProviders().size() || live.getReceiverCount() != snapshot.getReceivers().size()) {
                snapshot = cloud.getComputationTopologySnapshot(id);
                return snapshot;
            }
            List<CloudMachineSnapshot> providers = new ArrayList<>(), receivers = new ArrayList<>();
            for (int i = 0; i < live.getProviderCount(); i++) {
                var old = snapshot.getProviders().get(i);
                providers.add(new CloudMachineSnapshot(old.getDimensionId(), old.getPos(), old.getFrontPos(), old.getItem(),
                        live.getProviderCurrentCwu(i), live.getProviderMaxCwu(i), old.getRequestedCwu()));
            }
            for (int i = 0; i < live.getReceiverCount(); i++) {
                var old = snapshot.getReceivers().get(i);
                receivers.add(new CloudMachineSnapshot(old.getDimensionId(), old.getPos(), old.getFrontPos(), old.getItem(),
                        old.getCurrentCwu(), old.getMaxCwu(), live.getReceiverRequestedCwu(i)));
            }
            snapshot = new ComputationTopologySnapshot(snapshot.getTopologyVersion(), providers, receivers,
                    snapshot.getUnboundProviders(), snapshot.getUnboundReceivers());
            return snapshot;
        }
        private UUID cachedTeam;
    }
    private CloudComputationMonitorOverviewPage() {}
}
