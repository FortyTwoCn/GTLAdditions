package com.gtladd.gtladditions.integration.xei;

/** Transient display amount; this never changes a fluid's data components or AE2 key. */
public interface ILongFluidDisplay {
    long gtladditions$getDisplayAmount();
    void gtladditions$setDisplayAmount(long amount);
    boolean gtladditions$hasDisplayAmount();
}
