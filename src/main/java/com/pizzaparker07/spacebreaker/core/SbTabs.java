package com.pizzaparker07.spacebreaker.core;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creative tabs. Modules call e.g. {@code SbTabs.MATERIALS.add(MyItems.FOO)} while registering;
 * items appear in the order they were added.
 */
public final class SbTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Spacebreaker.MOD_ID);

    public static final Tab MATERIALS = new Tab("materials");
    public static final Tab PLANETS = new Tab("planets");
    public static final Tab MACHINES = new Tab("machines");
    public static final Tab SPACEFLIGHT = new Tab("spaceflight");
    public static final Tab EQUIPMENT = new Tab("equipment");

    public static final class Tab {
        private final List<Supplier<ItemStack>> entries = new ArrayList<>();
        private Supplier<ItemStack> icon = () -> new ItemStack(Items.BARRIER);
        public final DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

        Tab(String id) {
            this.holder = TABS.register(id, () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Spacebreaker.MOD_ID + "." + id))
                    .icon(() -> icon.get())
                    .displayItems((params, out) -> entries.forEach(e -> out.accept(e.get())))
                    .build());
        }

        public Tab add(Supplier<? extends ItemLike> item) {
            entries.add(() -> new ItemStack(item.get()));
            return this;
        }

        public Tab addStack(Supplier<ItemStack> stack) {
            entries.add(stack);
            return this;
        }

        public Tab icon(Supplier<? extends ItemLike> item) {
            this.icon = () -> new ItemStack(item.get());
            return this;
        }
    }

    private SbTabs() {
    }
}
