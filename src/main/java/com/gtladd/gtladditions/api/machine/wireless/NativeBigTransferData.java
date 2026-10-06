package com.gtladd.gtladditions.api.machine.wireless;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import org.gtlcore.gtlcore.integration.gtmt.IPreciseWirelessTransferData;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

/** Keep the native snapshot contract while retaining throughput beyond the long-based capability view. */
public record NativeBigTransferData(UUID UUID, BigInteger preciseThroughput, MetaMachine machine)
        implements IPreciseWirelessTransferData {
    public NativeBigTransferData {
        Objects.requireNonNull(UUID); Objects.requireNonNull(preciseThroughput); Objects.requireNonNull(machine);
    }
    @Override public long Throughput() {
        return preciseThroughput.max(BigInteger.valueOf(Long.MIN_VALUE)).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
