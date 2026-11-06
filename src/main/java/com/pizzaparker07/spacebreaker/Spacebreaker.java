package com.pizzaparker07.spacebreaker;

import com.mojang.logging.LogUtils;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.module.Modules;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Spacebreaker.MOD_ID)
public final class Spacebreaker {
    public static final String MOD_ID = "spacebreaker";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Spacebreaker(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SbConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, SbConfig.CLIENT_SPEC);

        modBus.addListener(Bodies::registerRegistry);
        modBus.addListener(Tech::registerRegistry);
        Tech.ATTACHMENTS.register(modBus);

        for (SbModule module : Modules.ALL) {
            module.register(modBus);
        }
        SbTabs.TABS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
