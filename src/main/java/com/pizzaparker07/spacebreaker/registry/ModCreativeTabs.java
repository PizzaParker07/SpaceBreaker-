package com.pizzaparker07.spacebreaker.registry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.item.OxygenTankItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Spacebreaker.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.spacebreaker.main"))
            .icon(() -> ModItems.EVA_HELMET.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(ModItems.BAUXITE_ORE.get());
                out.accept(ModItems.DEEPSLATE_BAUXITE_ORE.get());
                out.accept(ModItems.RAW_BAUXITE.get());
                out.accept(ModItems.RAW_BAUXITE_BLOCK.get());
                out.accept(ModItems.ALUMINUM_INGOT.get());
                out.accept(ModItems.ALUMINUM_PLATE.get());
                out.accept(ModItems.LUNAR_REGOLITH.get());
                out.accept(ModItems.MARE_BASALT.get());
                out.accept(ModItems.ILMENITE_ORE.get());
                out.accept(ModItems.SOLAR_PANEL.get());
                out.accept(OxygenTankItem.filled(ModItems.OXYGEN_TANK.get()));
                out.accept(ModItems.EVA_HELMET.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
