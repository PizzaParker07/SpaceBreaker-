package com.pizzaparker07.spacebreaker.content.vehicles;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Surface vehicles: the planetary rover. */
public final class VehiclesModule implements SbModule {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RoverEntity>> ROVER = ENTITIES.register("rover",
            () -> EntityType.Builder.<RoverEntity>of(RoverEntity::new, MobCategory.MISC).sized(1.9F, 1.3F).clientTrackingRange(10)
                    .build("rover"));
    public static final DeferredItem<RoverItem> ROVER_ITEM = ITEMS.registerItem("rover", RoverItem::new, new Item.Properties().stacksTo(1));
    /** Light, fast, quiet rover (an upgrade of the standard one). */
    public static final DeferredHolder<EntityType<?>, EntityType<RoverEntity>> SPRINT_ROVER = ENTITIES.register("sprint_rover",
            () -> EntityType.Builder.<RoverEntity>of(RoverEntity::new, MobCategory.MISC).sized(1.7F, 1.3F).clientTrackingRange(10)
                    .build("sprint_rover"));
    public static final DeferredItem<RoverItem> SPRINT_ROVER_ITEM = ITEMS.registerItem("sprint_rover",
            p -> new RoverItem(p, SPRINT_ROVER), new Item.Properties().stacksTo(1));

    /** The stats behind an entity type (the standard rover for anything unknown). */
    public static RoverType typeOf(EntityType<?> type) {
        return type == SPRINT_ROVER.get() ? RoverType.SPRINT : RoverType.STANDARD;
    }

    public static Item itemOf(RoverType type) {
        return type == RoverType.SPRINT ? SPRINT_ROVER_ITEM.get() : ROVER_ITEM.get();
    }

    @Override
    public String id() {
        return "vehicles";
    }

    @Override
    public void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        com.pizzaparker07.spacebreaker.core.SbTabs.SPACEFLIGHT.add(ROVER_ITEM);
        com.pizzaparker07.spacebreaker.core.SbTabs.SPACEFLIGHT.add(SPRINT_ROVER_ITEM);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.item(ROVER_ITEM, "Planetary Rover", "Gezegen Gezicisi");
        lang.add("entity.spacebreaker.rover", "Planetary Rover", "Gezegen Gezicisi");
        lang.add("container.spacebreaker.rover", "Rover Cargo", "Gezici Kargosu");
        lang.item(SPRINT_ROVER_ITEM, "Sprint Rover", "Hızlı Gezici");
        lang.add("entity.spacebreaker.sprint_rover", "Sprint Rover", "Hızlı Gezici");
        lang.add("tooltip.spacebreaker.sprint_rover", "Light and quiet: %s m/s, a smaller battery and cargo bay, skids sooner on loose ground",
                "Hafif ve sessiz: %s m/s, daha küçük akü ve kargo, gevşek zeminde daha çabuk kayar");
        lang.add("tooltip.spacebreaker.rover", "Two seats, battery-electric. Sneak-use: cargo. Use with a solar panel, an RTG or a pellet to fit it",
                "İki kişilik, akülü elektrikli. Eğilip tıkla: kargo. Güneş paneli, RTG ya da peletle tıklayıp tak");
        lang.add("tooltip.spacebreaker.rover.charge", "Charges when parked next to a battery bank or cable",
                "Batarya bankası ya da kablonun yanına park edince şarj olur");
        lang.add("hud.spacebreaker.rover.battery", "Battery %s%%", "Akü %%%s");
        lang.add("hud.spacebreaker.rover.speed", "%s m/s", "%s m/s");
        lang.add("hud.spacebreaker.rover.solar", "Solar", "Güneş");
        lang.add("message.spacebreaker.rover.whirl_cleaned", "A dust whirl swept the roof panel clean", "Bir toz hortumu tavan panelini temizledi");
        lang.add("hud.spacebreaker.rover.solar_dusty", "Solar (dust %s%%)", "Güneş (toz %%%s)");
        lang.add("hud.spacebreaker.rover.rtg", "RTG", "RTG");
        lang.add("hud.spacebreaker.rover.empty", "Battery empty", "Akü boş");
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ROVER_ITEM.get())
                .pattern("AAA").pattern("MCM").pattern("MBM")
                .define('A', EarthModule.AL_LI.plateTag).define('M', EarthModule.ELECTRIC_MOTOR.get())
                .define('C', EarthModule.ADVANCED_CIRCUIT.get()).define('B', MachinesModule.BATTERY_BANK.item().get())
                .unlockedBy("has_battery_bank", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
                        MachinesModule.BATTERY_BANK.item().get()))
                .save(out);
        // the sprint rover: the standard one rebuilt on an aluminium-lithium frame with twin motors per axle
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, SPRINT_ROVER_ITEM.get())
                .pattern("AMA").pattern("ARA").pattern("ACA")
                .define('A', EarthModule.AL_LI.plateTag).define('M', EarthModule.ELECTRIC_MOTOR.get())
                .define('R', ROVER_ITEM.get()).define('C', EarthModule.ADVANCED_CIRCUIT.get())
                .unlockedBy("has_rover", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ROVER_ITEM.get()))
                .save(out);
    }
}
