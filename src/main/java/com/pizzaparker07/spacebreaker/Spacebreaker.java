package com.pizzaparker07.spacebreaker;

import com.mojang.logging.LogUtils;
import com.pizzaparker07.spacebreaker.content.blockentity.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.registry.ModArmorMaterials;
import com.pizzaparker07.spacebreaker.registry.ModBlockEntities;
import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModCreativeTabs;
import com.pizzaparker07.spacebreaker.registry.ModDataComponents;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(Spacebreaker.MOD_ID)
public final class Spacebreaker {
    public static final String MOD_ID = "spacebreaker";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Spacebreaker(IEventBus modBus, ModContainer container) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModArmorMaterials.MATERIALS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(Spacebreaker::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SOLAR_PANEL.get(),
                (be, side) -> side == null || side != net.minecraft.core.Direction.UP ? be.getEnergy() : null);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
