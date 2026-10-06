package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GTLAddDynamicRenders {
    public static void register(IEventBus bus) {
        DynamicRenderManager.register(id("arcanic_astrograph"), ArcanicAstrographRender.TYPE);
        DynamicRenderManager.register(id("forge_of_the_antichrist"), ForgeOfAntichristRenderer.TYPE);
        DynamicRenderManager.register(id("heart_of_the_universe"), HeartOfTheUniverseRenderer.TYPE);
        DynamicRenderManager.register(id("light_hunter_space_station"), LightHunterSpaceStationRenderer.TYPE);
        DynamicRenderManager.register(id("time_space_distorter"), TimeSpaceDistorterRenderer.TYPE);
        DynamicRenderManager.register(id("space_elevator_mkii"), SpaceElevatorMKIIRenderer.TYPE);
        bus.addListener(GTLAddDynamicRenders::additionalModels);
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("gtladditions", name); }
    private static void additionalModels(ModelEvent.RegisterAdditional event) {
        java.util.function.Consumer<ResourceLocation> registry = id -> event.register(ModelResourceLocation.standalone(id));
        new ArcanicAstrographRender().onAdditionalModel(registry);
        new LightHunterSpaceStationRenderer().onAdditionalModel(registry);
        new SpaceElevatorMKIIRenderer().onAdditionalModel(registry);
    }
    private GTLAddDynamicRenders() {}
}
