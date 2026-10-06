package com.gtladd.gtladditions.migration;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import org.gtlcore.gtlcore.integration.wildcard.*;
import org.leodreamer.wildcard_pattern.WildcardItems;
import org.leodreamer.wildcard_pattern.gui.GenericGTTag;
import org.leodreamer.wildcard_pattern.gui.WildcardUIFactory;
import org.leodreamer.wildcard_pattern.wildcard.WildcardPatternLogic;
import org.leodreamer.wildcard_pattern.wildcard.impl.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_PLATE;

/** Genuine native external item/serializers, AE2 provider/grid and registered Core integration. */
@GameTestHolder("gtl_native_probe")
@PrefixGameTestTemplate(false)
public final class NativeProductionWildcardGameTests {
    private static Map<AEKey, Long> inputs(IPatternDetails details) {
        var result = new HashMap<AEKey, Long>();
        for (var input : details.getInputs()) {
            var stack = input.getPossibleInputs()[0];
            result.merge(stack.what(), Math.multiplyExact(stack.amount(), input.getMultiplier()), Math::addExact);
        }
        return result;
    }
    private static Map<AEKey, Long> outputs(IPatternDetails details) {
        var result = new HashMap<AEKey, Long>();
        for (var output : details.getOutputs()) result.merge(output.what(), output.amount(), Math::addExact);
        return result;
    }

    @GameTest(template = "empty", batch = "gtl_production_wildcard", timeoutTicks = 300)
    public static void realExternalWildcardItemExpandsInNativeAeProviderAndCoreBufferWithAllFiveSerializers(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var stack = WildcardItems.WILDCARD_PATTERN.asStack();
        var item = new ItemStack(Items.IRON_INGOT);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("wildcard production component item"));
        var fluid = new FluidStack(Fluids.WATER, 1);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("wildcard production component fluid"));
        var logic = WildcardPatternLogic.on(stack, registries);
        logic.setIOComponents(WildcardPatternLogic.IO.IN, List.of(
                new TagIOComponent(GenericGTTag.item(TagPrefix.ingot), 2),
                new SimpleIOComponent(new GenericStack(AEItemKey.of(item), 5))));
        logic.setIOComponents(WildcardPatternLogic.IO.OUT, List.of(
                new SimpleIOComponent(new GenericStack(AEFluidKey.of(fluid), 750)),
                new TagIOComponent(GenericGTTag.item(TagPrefix.dust), 1)));
        logic.setFilterComponents(List.of(new SimpleFilterComponent(GTMaterials.Copper, true),
                new FlagFilterComponent(GENERATE_PLATE, GTMaterials.Copper, true),
                new PropertyFilterComponent(PropertyKey.INGOT, GTMaterials.Copper, true)));
        var expected = logic.generateAllPatterns(level).toList();
        helper.assertTrue(expected.size() == 1, "External wildcard's native material filters did not select exactly copper");
        helper.assertTrue(inputs(expected.getFirst()).equals(Map.of(AEItemKey.of(ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Copper)), 2L,
                AEItemKey.of(item), 5L)) && outputs(expected.getFirst()).equals(Map.of(
                AEItemKey.of(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Copper)), 1L, AEFluidKey.of(fluid), 750L)),
                "External native pattern lost material substitution, component keys or quantities");
        var restored = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag) stack.save(registries));
        helper.assertTrue(ItemStack.isSameItemSameComponents(stack, restored) && PatternDetailsHelper.isEncodedPattern(restored),
                "Real external wildcard item did not persist or enter the native AE pattern inventory");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var received = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(ItemStack.isSameItemSameComponents(restored, received) && !buffer.isReadable(),
                    "Native external wildcard item packet lost custom data or component keys");
        } finally { buffer.release(); }
        var core = NativeWildcardPatternDecoder.expand(restored, level);
        helper.assertTrue(core.size() == 1 && inputs(core.getFirst()).equals(inputs(expected.getFirst()))
                && outputs(core.getFirst()).equals(outputs(expected.getFirst())), "Core and genuine external wildcard protocol disagree");
        var incomplete = restored.copy();
        var incompleteLogic = WildcardPatternLogic.on(incomplete, registries);
        incompleteLogic.setIOComponents(WildcardPatternLogic.IO.IN, List.of(SimpleIOComponent.empty()));
        helper.assertTrue(incompleteLogic.generateAllPatterns(level).findAny().isEmpty()
                && NativeWildcardPatternDecoder.expand(incomplete, level).isEmpty(), "Blank real editor ghost slot created a pattern or crashed Core");

        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, restored);
        var holder = new WildcardUIFactory.Holder(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!holder.isInvalid(), "Native wildcard editor rejected its actual held item");
        holder.createUI(player);
        // The server must reject editing a replaced item even if its type and data are identical.
        player.setItemInHand(InteractionHand.MAIN_HAND, restored.copy());
        helper.assertTrue(holder.isInvalid(), "Native external wildcard editor accepted a replaced held item");

        var providerPos = new BlockPos(3, 4, 3);
        helper.setBlock(providerPos, AEBlocks.PATTERN_PROVIDER.block());
        var provider = (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
        helper.setBlock(providerPos.south(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var entry = WildcardPatternCompatImpl.ME_WILDCARD_PATTERN_BUFFER;
        helper.assertTrue(entry != null, "Complete Core did not register its optional real wildcard buffer");
        helper.setBlock(providerPos.south(2), entry.getBlock());
        var part = (MEWildcardPatternBufferPartMachine) helper.getBlockEntity(providerPos.south(2));
        part.setFrontFacing(Direction.NORTH);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(provider.getMainNode().isActive() && part.isOnline(),
                "Genuine AE provider and Core wildcard buffer did not join their powered native grid"))
                .thenExecute(() -> {
                    provider.getLogic().getPatternInv().setItemDirect(0, restored.copy());
                    provider.getLogic().updatePatterns();
                    part.getWildcardPatternSlot().setStackInSlot(0, restored.copy());
                }).thenWaitUntil(() -> helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1
                        && part.getAvailablePatterns().size() == 1, "Native provider/Core buffer did not expand the real external item"))
                .thenExecute(() -> {
                    var actual = provider.getLogic().getAvailablePatterns().getFirst();
                    helper.assertTrue(inputs(actual).equals(inputs(expected.getFirst())) && outputs(actual).equals(outputs(expected.getFirst())),
                            "Genuine native AE provider advertised changed external wildcard inputs/outputs");
                }).thenWaitUntil(() -> helper.assertTrue(provider.getMainNode().getGrid().getCraftingService()
                        .isCraftable(AEFluidKey.of(fluid)), "Native AE crafting service did not index wildcard component output"))
                .thenExecute(() -> {
                    provider.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                    provider.getLogic().updatePatterns();
                    part.getWildcardPatternSlot().setStackInSlot(0, ItemStack.EMPTY);
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().isEmpty(), "Removing real wildcard left stale native AE patterns");
                }).thenWaitUntil(() -> helper.assertTrue(!provider.getMainNode().getGrid().getCraftingService()
                        .isCraftable(AEFluidKey.of(fluid)), "Removing wildcard providers left stale native AE crafting service output"))
                .thenExecute(() -> {
                    LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_WILDCARD external_item=true five_serializers=true components=true native_packet=true ae_provider=true core_buffer=true powered_grid=true editor_holder=true");
                }).thenSucceed();
    }
}
