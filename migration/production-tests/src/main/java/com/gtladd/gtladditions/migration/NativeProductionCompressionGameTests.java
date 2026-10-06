package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.gtlcore.gtlcore.api.gui.preview.NativePreviewShapes;
import java.util.ArrayList;

/** A real original Astrograph with an actual blocked export bus and native owned-buffer persistence. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionCompressionGameTests {
    @GameTest(template = "empty", batch = "gtl_production_compression", timeoutTicks = 200)
    public static void collectedAstralArraysAndRolledOutputsSurviveBlockingStoppingReloadAndMachineDrop(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = (MetaMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gtladditions:arcanic_astrograph"));
        var definition = (com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition) block.getDefinition();
        var geometry = NativePreviewShapes.get(definition).getFirst().oriented(Direction.NORTH, Direction.UP, false);
        // Keep physical item entities in the GameTest's entity-ticking chunks, above its other fixtures.
        var origin = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 8, 96,
                helper.absolutePos(BlockPos.ZERO).getZ() + 8);
        var busPos = origin.offset(geometry.states().entrySet().stream()
                .filter(entry -> entry.getValue().is(GTBlocks.HIGH_POWER_CASING.get())).findFirst().orElseThrow().getKey());
        geometry.states().forEach((position, state) -> level.setBlockAndUpdate(origin.offset(position), state));
        level.setBlockAndUpdate(busPos, GTMachines.ITEM_EXPORT_BUS[GTValues.LV].getBlock().defaultBlockState());
        var machine = (ArcanicAstrograph) level.getBlockEntity(origin);
        machine.checkAndFormStructure();
        helper.assertTrue(machine.isFormed(), "Original Astrograph with real export bus did not form");
        machine.setWorkingEnabled(false);
        machine.increaseAstralArrayCount(42441); // Seed the existing 100% probability threshold; no replacement RNG.
        var bus = (ItemBusPartMachine) level.getBlockEntity(busPos);
        helper.assertTrue(machine.getParts().contains(bus) && bus.hasController(origin), "Real Astrograph export bus did not bind");
        for (int slot = 0; slot < bus.getInventory().getSlots(); slot++)
            bus.getInventory().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        var center = origin.getCenter().add(0, 0, 16);
        var entities = new ArrayList<ItemEntity>();
        for (int index = 0; index < 33; index++) {
            var entity = new ItemEntity(level, center.x, center.y, center.z,
                    new ItemStack(GTLAddItems.INSTANCE.getASTRAL_ARRAY().get(), index == 32 ? 23 : 64));
            level.addFreshEntity(entity); entities.add(entity);
        }
        var chunk = level.getChunkAt(origin); chunk.setUnsaved(false);
        var trait = machine.getAstralArrayCompression();
        trait.handleCompressionWorking();
        helper.assertTrue(trait.getCompressionAstralArrayCount() == 2071 && entities.stream().allMatch(ItemEntity::isRemoved)
                && chunk.isUnsaved(), "Actual raw item collection lost ownership or did not mark world dirty: collected="
                + trait.getCompressionAstralArrayCount() + " removed=" + entities.stream().filter(ItemEntity::isRemoved).count()
                + " unsaved=" + chunk.isUnsaved());
        trait.finishCompression();
        helper.assertTrue(trait.getCompressionAstralArrayCount() == 23 && trait.getPendingCompressedAstralArrays() == 2,
                "Blocked actual export discarded compressed products");
        trait.finishCompression(); trait.pauseCompression(); machine.setWorkingEnabled(false);
        helper.assertTrue(trait.getCompressionAstralArrayCount() == 23 && trait.getPendingCompressedAstralArrays() == 2,
                "Retry or stop discarded raw inputs or rerolled blocked products");
        var registries = level.registryAccess();
        var saved = machine.saveWithoutMetadata(registries);
        var restored = (ArcanicAstrograph) definition.getBlockEntityType().create(origin, machine.getBlockState());
        restored.loadAdditional(saved, registries);
        helper.assertTrue(restored.getAstralArrayCompression().getCompressionAstralArrayCount() == 23
                        && restored.getAstralArrayCompression().getPendingCompressedAstralArrays() == 2,
                "Native saved trait data lost owned compression buffers");
        var drops = Block.getDrops(machine.getBlockState(), level, origin, machine);
        var controllerDrop = drops.stream().filter(stack -> stack.is(block.asItem())).findFirst().orElseThrow();
        var itemSaved = ItemStack.parseOptional(registries, (CompoundTag) controllerDrop.save(registries));
        var placedCopy = (ArcanicAstrograph) definition.getBlockEntityType().create(origin, machine.getBlockState());
        placedCopy.applyComponentsFromItemStack(itemSaved);
        helper.assertTrue(placedCopy.getAstralArrayCompression().getCompressionAstralArrayCount() == 23
                        && placedCopy.getAstralArrayCompression().getPendingCompressedAstralArrays() == 2,
                "Actual native controller loot/component restoration discarded owned buffers");
        // A real world replacement must retry the committed product and bind the existing output bus.
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin, block.defaultBlockState());
        level.removeBlockEntity(origin); level.setBlockEntity(restored);
        helper.startSequence().thenIdle(2).thenExecute(() -> {
            restored.checkAndFormStructure();
            helper.assertTrue(restored.isFormed(), "Reloaded original Astrograph did not reform");
            bus.getInventory().setStackInSlot(0, ItemStack.EMPTY);
            helper.assertTrue(restored.getAstralArrayCompression().flushPendingOutputs(), "Unblocked real output did not accept pending products");
            helper.assertTrue(restored.getAstralArrayCompression().getPendingCompressedAstralArrays() == 0
                            && restored.getAstralArrayCompression().getCompressionAstralArrayCount() == 23
                            && bus.getInventory().getStackInSlot(0).is(GTLAddItems.INSTANCE.getCOMPRESSED_ASTRAL_ARRAY().get())
                            && bus.getInventory().getStackInSlot(0).getCount() == 2,
                    "Real output retry lost or duplicated products");
            helper.assertTrue(restored.getAstralArrayCompression().flushPendingOutputs()
                    && bus.getInventory().getStackInSlot(0).getCount() == 2, "Finished output was delivered twice");
            LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_COMPRESSION original_structure=true real_entities=true blocked_output=true no_reroll=true stop=true native_world_reload=true actual_loot_components=true exact_delivery=true");
            level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
            geometry.states().keySet().forEach(position -> level.setBlock(origin.offset(position), Blocks.AIR.defaultBlockState(), 2));
        }).thenSucceed();
    }
}
