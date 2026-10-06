package com.gtladd.gtladditions.api.machine.wireless;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.hepdd.gtmthings.api.misc.WirelessEnergyContainer;
import com.hepdd.gtmthings.config.ConfigHolder;
import com.hepdd.gtmthings.data.WirelessEnergySavaedData;
import com.gtladd.gtladditions.utils.WirelessEnergyManagerData;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Exact, all-or-nothing recipe payments into GTMThings' native team-owned, saved accounts. */
public final class NativeWirelessEnergyAccount {
    private static final ThreadLocal<TransferReports> PENDING_REPORTS = new ThreadLocal<>();
    private NativeWirelessEnergyAccount() {}

    /** Report the net transfer after a tick's reservation and possible refund have completed. */
    public static TransferReports deferTransferReports(MetaMachine machine) {
        var scope = new TransferReports(machine, PENDING_REPORTS.get());
        PENDING_REPORTS.set(scope);
        return scope;
    }

    public static final class TransferReports implements AutoCloseable {
        private final MetaMachine machine;
        private final @Nullable TransferReports parent;
        private final Map<WirelessEnergyContainer, BigInteger> changes = new LinkedHashMap<>();
        private boolean closed;
        private TransferReports(MetaMachine machine, @Nullable TransferReports parent) {
            this.machine = machine; this.parent = parent;
        }
        @Override public void close() {
            if (closed) return;
            if (PENDING_REPORTS.get() != this) throw new IllegalStateException("Wireless reporting scopes closed out of order");
            closed = true;
            if (parent == null) PENDING_REPORTS.remove(); else PENDING_REPORTS.set(parent);
            changes.forEach((account, amount) -> {
                if (amount.signum() != 0) report(account, amount, machine);
            });
        }
    }

    private static void report(WirelessEnergyContainer account, BigInteger amount, MetaMachine machine) {
        var scope = PENDING_REPORTS.get();
        if (scope != null && scope.machine == machine) {
            scope.changes.merge(account, amount, BigInteger::add);
            return;
        }
        account.getEnergyStat().update(amount, WirelessEnergyContainer.server.getTickCount());
        WirelessEnergyContainer.TRANSFER_DATA.put(machine, new NativeBigTransferData(account.getUuid(), amount, machine));
        WirelessEnergyManagerData.BIG_MACHINE_DATA.put(machine, Pair.of(account.getUuid(), amount));
    }

    public static @Nullable WirelessEnergyContainer find(@Nullable UUID user) {
        if (user == null || WirelessEnergySavaedData.INSTANCE == null || WirelessEnergyContainer.server == null) return null;
        return WirelessEnergyContainer.getOrCreateContainer(user);
    }

    public static BigInteger balance(@Nullable UUID user) {
        var account = find(user);
        return account == null ? BigInteger.ZERO : account.getStorage();
    }

    /** The native default Long.MAX_VALUE denotes unlimited throughput for Additions' precise payments. */
    public static @Nullable BigInteger transferLimit(WirelessEnergyContainer account) {
        return ConfigHolder.INSTANCE.isWirelessRateEnable && account.getRate() != Long.MAX_VALUE ?
                BigInteger.valueOf(Math.max(0, account.getRate())) : null;
    }

    public static boolean transfer(@Nullable UUID user, long amount, @Nullable MetaMachine machine) {
        return transfer(user, BigInteger.valueOf(amount), machine);
    }

    public static boolean transfer(@Nullable UUID user, int amount, @Nullable MetaMachine machine) {
        return transfer(user, (long) amount, machine);
    }

    public static boolean transfer(@Nullable UUID user, BigInteger amount, @Nullable MetaMachine machine) {
        var account = find(user);
        if (account == null || (machine != null && machine.hasLevel() && machine.isRemote())) return false;
        var limit = transferLimit(account);
        if (limit != null && amount.abs().compareTo(limit) > 0) return false;
        synchronized (account) {
            var updated = account.getStorage().add(amount);
            if (updated.signum() < 0) return false;
            account.setStorage(updated);
            if (machine != null && amount.signum() != 0) {
                report(account, amount, machine);
            }
        }
        return true;
    }
}
