package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.WorkableElectricMultipleRecipesMachine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Implements(@Interface(
                       iface = IWirelessThreadModifierParallelMachine.class,
                       prefix = "gtladditions$"))
@Mixin(WorkableElectricMultipleRecipesMachine.class)
public abstract class WorkableElectricMultipleRecipesMachineMixin extends WorkableElectricMultiblockMachine {

    @Unique
    protected @Nullable IThreadModifierPart gtladditions$threadPartMachine = null;

    public WorkableElectricMultipleRecipesMachineMixin(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Unique
    public int gtladditions$getAdditionalThread() {
        return gtladditions$threadPartMachine == null ? 0 : gtladditions$threadPartMachine.getThreadCount();
    }

    @Unique
    public void gtladditions$setThreadPartMachine(@Nullable IThreadModifierPart threadPartMachine) {
        this.gtladditions$threadPartMachine = threadPartMachine;
    }

    @Unique
    public @Nullable IThreadModifierPart gtladditions$getThreadPartMachine() {
        return gtladditions$threadPartMachine;
    }

    @Override
    public void invalidateStructure(String name) {
        super.invalidateStructure(name);
        this.gtladditions$threadPartMachine = null;
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        this.gtladditions$threadPartMachine = null;
    }
}