package com.gtladd.gtladditions.mixin.gtceu.integration;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.integration.jade.provider.ParallelProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine;
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.common.machine.multiblock.controller.*;
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.ForgeOfTheAntichristModuleBase;
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.HelioFusionExoticizer;
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.LightHunterSpaceStationModuleBase;
import com.gtladd.gtladditions.common.machine.multiblock.part.SuperParallelHatchPartMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Objects;

import static com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponent;
import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.ELEMENT_COPYING_RECIPES;

@Mixin(ParallelProvider.class)
public abstract class ParallelProviderMixin {

    @Unique
    private static final long INFINITY_FEATURE = -114514;

    @Inject(method = "addTooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$parallelTooltip(CompoundTag data, ITooltip iTooltip, Player player,
            BlockAccessor accessor, BlockEntity blockEntity, IPluginConfig config, CallbackInfo ci) {
        if (!data.getBoolean("gtladditions:parallel_display")) return;
        ci.cancel();
        if (data.contains("parallel")) {
            long parallel = data.getLong("parallel");
            if (parallel > 0) {
                iTooltip.add(Component.translatable(
                        "gtceu.multiblock.parallel",
                        Component.literal(parallel + "").withStyle(ChatFormatting.DARK_PURPLE)));
            } else if (parallel == INFINITY_FEATURE) {
                iTooltip.add(Component.translatable(
                        "gtceu.multiblock.parallel",
                        createLanguageRainbowComponent(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel"))));
            }
        }
        if (data.contains("threads")) {
            int threads = data.getInt("threads");
            if (threads > 0) {
                iTooltip.add(Component.translatable(
                        "gtladditions.multiblock.threads",
                        Component.literal(String.valueOf(threads)).withStyle(ChatFormatting.GOLD)));
            } else if (threads == INFINITY_FEATURE) {
                iTooltip.add(Component.translatable(
                        "gtladditions.multiblock.threads",
                        createLanguageRainbowComponent(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel"))));

            }
        }
    }

    @Inject(method = "write(Lcom/gregtechceu/gtceu/api/machine/MetaMachine;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$parallelData(MetaMachine machine, CallbackInfoReturnable<CompoundTag> cir) {
        var compoundTag = new CompoundTag();
            if (machine instanceof SuperParallelHatchPartMachine parallelHatch) {
                compoundTag.putLong("parallel", parallelHatch.getCurrentParallel());
            } else if (machine instanceof WorkableMultiblockMachine workableElectricMultiblockMachine && workableElectricMultiblockMachine.isFormed()) {
                if (workableElectricMultiblockMachine instanceof GTLAddWorkableElectricMultipleRecipesMachine addMachine) {
                    if (workableElectricMultiblockMachine instanceof ForgeOfTheAntichrist) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof ForgeOfTheAntichristModuleBase) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        if (!(workableElectricMultiblockMachine instanceof HelioFusionExoticizer))
                            compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof MacroAtomicResonantFragmentStripper atomic) {
                        compoundTag.putLong("parallel", atomic.getRecipeType() == ELEMENT_COPYING_RECIPES.get() ? INFINITY_FEATURE : atomic.getRealParallel());
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof SpaceInfinityIntegratedOreProcessor) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof LightHunterSpaceStationModuleBase lightHunterSpaceStationModuleBase && lightHunterSpaceStationModuleBase.isConnectedToHost() && Objects.requireNonNull(lightHunterSpaceStationModuleBase.getHost()).unlockParadoxical()) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof SpaceScalingInstrument) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else {
                        compoundTag.putLong("parallel", addMachine.getMaxParallel());
                        compoundTag.putLong("threads", addMachine.getRecipeLogic().getMultipleThreads());
                    }
                } else {
                    var logic = workableElectricMultiblockMachine.getRecipeLogic();
                    if (logic instanceof MultipleRecipesLogic) {
                        compoundTag.putLong("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        compoundTag.putLong("threads", Ints.saturatedCast(64L + ((IThreadModifierMachine) workableElectricMultiblockMachine).getAdditionalThread()));
                    } else if (logic instanceof MutableRecipesLogic<?> mutableRecipesLogic) {
                        if (workableElectricMultiblockMachine instanceof TaixuTurbidArray taixu) {
                            compoundTag.putLong("parallel", taixu.getRealParallel());
                        } else compoundTag.putLong("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        if (mutableRecipesLogic.isMultipleRecipeMode() && mutableRecipesLogic.getMultipleThreads() > 1)
                            compoundTag.putLong("threads", mutableRecipesLogic.getMultipleThreads());
                    } else if (machine instanceof ParallelMachine controller
                            && (machine.getClass().getName().startsWith("org.gtlcore.")
                                || machine.getClass().getName().startsWith("com.gtladd."))) {
                        compoundTag.putLong("parallel", controller.getMaxParallel());
                    } else if (workableElectricMultiblockMachine instanceof ConversationMachine conversationMachine) {
                        compoundTag.putLong("parallel", conversationMachine.getParallel());
                    } else if (workableElectricMultiblockMachine instanceof BasicOreProcessorMachine basicOreProcessorMachine) {
                        compoundTag.putLong("parallel", basicOreProcessorMachine.getParallel());
                    }
                }
            }
        if (compoundTag.contains("parallel") || compoundTag.contains("threads")) {
            compoundTag.putBoolean("gtladditions:parallel_display", true);
            cir.getReturnValue().merge(compoundTag);
        }
    }
}
