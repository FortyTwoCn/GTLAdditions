package com.gtladd.gtladditions

import com.gtladd.gtladditions.api.registry.GTLAddRegistration
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.material.MaterialModify
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.config.ConfigHolder
import com.gtladd.gtladditions.network.GTLAddNetworking
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(GTLAdditions.MOD_ID)
class GTLAdditions(modEventBus: net.neoforged.bus.api.IEventBus) {
    init {
        GTLAddCreativeModeTabs.init()
        ConfigHolder.init()
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.gtladd.gtladditions.client.render.machine.GTLAddDynamicRenders.register(modEventBus)
        }
        modEventBus.addListener(GTLAddNetworking::register)
        com.gtladd.gtladditions.common.items.NativeLegacyGTItems.register(modEventBus)

        GTLAddRegistration.REGISTRATE.registerEventListeners(modEventBus)

        val addon = GTLAdditionsGTAddon()
        addon.registerElements()
        MaterialModify.init()
        GTLAddMaterial.init()
        GTLAddRecipesTypes.init()
        addon.registerSounds()
        addon.initializeAddon()
        GTLAddMachines.init()
    }

    companion object {
        const val MOD_ID = "gtladditions"
        const val NAME = "GTLAdditions"
        val LOGGER: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        fun id(name: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, name)
    }
}
