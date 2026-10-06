package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.registry.registrate.builder.MachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.data.model.builder.MachineModelBuilder;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import com.tterrag.registrate.providers.DataGenContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;

/** Native multipart model: painted sided hull plus the original overlay geometry. */
public final class OverlayHullMachineModel implements MachineBuilder.ModelInitializer {
    private final ResourceLocation bottom, top, side, overlay;

    public OverlayHullMachineModel(ResourceLocation bottom, ResourceLocation top,
                                   ResourceLocation side, ResourceLocation overlay) {
        this.bottom = bottom;
        this.top = top;
        this.side = side;
        this.overlay = overlay;
    }

    public OverlayHullMachineModel(ResourceLocation casing, ResourceLocation overlay) {
        this(casing, casing, casing, overlay);
    }

    /** Native color-ring hatch geometry with Additions' original casing and the current GT overlay texture. */
    public static MachineBuilder.ModelInitializer withTexture(ResourceLocation casing, ResourceLocation overlayTexture) {
        return (context, provider, builder) -> {
            builder.forAllStatesModelsExcept(state -> GTMachineModels.colorOverlayHullModel(
                    overlayTexture, null, null, state, provider.models())
                    .texture("bottom", casing).texture("top", casing).texture("side", casing).texture("particle", casing), IS_FORMED);
            builder.addReplaceableTextures("bottom", "top", "side");
        };
    }

    @Override
    public void configureModel(DataGenContext<Block, ? extends Block> context,
                               GTBlockstateProvider provider, MachineModelBuilder<BlockModelBuilder> builder) {
        var hull = provider.models().nested()
                .parent(provider.models().getExistingFile(GTCEu.id("block/cube/tinted/bottom_top")))
                .texture("bottom", bottom).texture("top", top).texture("side", side)
                .texture("particle", side);
        builder.part(hull);
        builder.part(provider.models().getExistingFile(overlay));
        builder.addReplaceableTextures("bottom", "top", "side");
    }
}
