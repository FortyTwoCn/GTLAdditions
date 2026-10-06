package com.gtladd.gtladditions.api.registry;

import com.gregtechceu.gtceu.api.machine.MachineInstanceFactory;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.registry.registrate.builder.MachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.builder.MultiblockMachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Edit the actual queued native builders before their immutable definitions are created. */
public final class NativeQueuedMachines {
    private static final Map<ResourceLocation, MultiblockMachineBuilder<?>> BUILDERS = new HashMap<>();
    private static final Set<ResourceLocation> SEALED = new HashSet<>();
    private static final Map<ResourceLocation, Supplier<? extends RecipeLogic>> DEFAULT_LOGICS = new HashMap<>();

    private NativeQueuedMachines() {}

    public static synchronized void capture(MachineBuilder<?, ?, ?> builder) {
        if (builder instanceof MultiblockMachineBuilder<?> multiblock) {
            var id = builder.getOwner().makeResourceLocation(builder.getName());
            var existing = BUILDERS.putIfAbsent(id, multiblock);
            if (existing != null && existing != builder) {
                throw new IllegalStateException("Duplicate queued multiblock builder " + id);
            }
        }
    }

    public static synchronized void seal(MachineBuilder<?, ?, ?> builder) {
        if (builder instanceof MultiblockMachineBuilder<?>) {
            SEALED.add(builder.getOwner().makeResourceLocation(builder.getName()));
        }
    }

    public static synchronized MultiblockMachineBuilder<?> builder(MachineEntry<MultiblockMachineDefinition> entry) {
        var id = entry.getId();
        var builder = BUILDERS.get(id);
        if (builder == null || SEALED.contains(id)) {
            throw new IllegalStateException("Multiblock " + id + " must be modified before native registry binding");
        }
        return builder;
    }

    public static void replaceFactory(MachineEntry<MultiblockMachineDefinition> entry,
                                      MachineInstanceFactory<? extends MultiblockControllerMachine> factory) {
        // Every native multiblock factory returns a controller; this is the same erased registration
        // boundary used by GT's machine modification API, without mutating a bound definition.
        @SuppressWarnings("unchecked")
        var builder = (MultiblockMachineBuilder<MultiblockControllerMachine>) builder(entry);
        builder.instanceFactory(factory::buildMachine);
    }

    /** Native parents without a logic constructor use this only at their default trait construction site. */
    public static synchronized void defaultRecipeLogic(MachineEntry<MultiblockMachineDefinition> entry,
                                                       Supplier<? extends RecipeLogic> factory) {
        builder(entry);
        if (DEFAULT_LOGICS.putIfAbsent(entry.getId(), factory) != null) {
            throw new IllegalStateException("Duplicate default recipe logic for " + entry.getId());
        }
    }

    public static synchronized RecipeLogic defaultRecipeLogic(BlockEntityCreationInfo info, RecipeLogic original) {
        if (info.state().getBlock() instanceof MetaMachineBlock block) {
            var factory = DEFAULT_LOGICS.get(block.getDefinition().getId());
            if (factory != null) return java.util.Objects.requireNonNull(factory.get());
        }
        return original;
    }
}
