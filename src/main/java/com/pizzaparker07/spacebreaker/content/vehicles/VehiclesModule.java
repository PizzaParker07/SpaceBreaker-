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

    /** Closed-cabin rover: no helmet needed aboard. */
    public static final DeferredHolder<EntityType<?>, EntityType<RoverEntity>> PRESSURIZED_ROVER = ENTITIES.register("pressurized_rover",
            () -> EntityType.Builder.<RoverEntity>of(RoverEntity::new, MobCategory.MISC).sized(1.9F, 1.9F).clientTrackingRange(10)
                    .build("pressurized_rover"));
    public static final DeferredItem<RoverItem> PRESSURIZED_ROVER_ITEM = ITEMS.registerItem("pressurized_rover",
            p -> new RoverItem(p, PRESSURIZED_ROVER), new Item.Properties().stacksTo(1));

    /** Pressurised cabin on a floating hull: water and Titan's methane lakes. */
    public static final DeferredHolder<EntityType<?>, EntityType<RoverEntity>> AMPHIBIOUS_ROVER = ENTITIES.register("amphibious_rover",
            () -> EntityType.Builder.<RoverEntity>of(RoverEntity::new, MobCategory.MISC).sized(2.3F, 1.9F).clientTrackingRange(10)
                    .build("amphibious_rover"));
    public static final DeferredItem<RoverItem> AMPHIBIOUS_ROVER_ITEM = ITEMS.registerItem("amphibious_rover",
            p -> new RoverItem(p, AMPHIBIOUS_ROVER), new Item.Properties().stacksTo(1));

    /** The stats behind an entity type (the standard rover for anything unknown). */
    public static RoverType typeOf(EntityType<?> type) {
        if (type == SPRINT_ROVER.get()) {
            return RoverType.SPRINT;
        }
        if (type == AMPHIBIOUS_ROVER.get()) {
            return RoverType.AMPHIBIOUS;
        }
        return type == PRESSURIZED_ROVER.get() ? RoverType.PRESSURIZED : RoverType.STANDARD;
    }

    public static Item itemOf(RoverType type) {
        if (type == RoverType.SPRINT) {
            return SPRINT_ROVER_ITEM.get();
        }
        if (type == RoverType.AMPHIBIOUS) {
            return AMPHIBIOUS_ROVER_ITEM.get();
        }
        return type == RoverType.PRESSURIZED ? PRESSURIZED_ROVER_ITEM.get() : ROVER_ITEM.get();
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
        com.pizzaparker07.spacebreaker.core.SbTabs.SPACEFLIGHT.add(PRESSURIZED_ROVER_ITEM);
        com.pizzaparker07.spacebreaker.core.SbTabs.SPACEFLIGHT.add(AMPHIBIOUS_ROVER_ITEM);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.item(ROVER_ITEM, "Planetary Rover", "Gezegen Gezicisi");
        lang.add("entity.spacebreaker.rover", "Planetary Rover", "Gezegen Gezicisi");
        lang.add("container.spacebreaker.rover", "Rover Cargo", "Gezici Kargosu");
        lang.item(SPRINT_ROVER_ITEM, "Sprint Rover", "Hızlı Gezici");
        lang.add("entity.spacebreaker.sprint_rover", "Sprint Rover", "Hızlı Gezici");
        lang.item(PRESSURIZED_ROVER_ITEM, "Pressurized Rover", "Basınçlı Gezici");
        lang.add("entity.spacebreaker.pressurized_rover", "Pressurized Rover", "Basınçlı Gezici");
        lang.add("tooltip.spacebreaker.pressurized_rover", "Closed cabin: no helmet needed aboard. %s m/s, long range, half the radiation. Fill its oxygen from a tank, a pipe or a canister",
                "Kapalı kabin: içeride kask gerekmez. %s m/s, uzun menzil, radyasyonun yarısı. Oksijenini tanktan, borudan ya da tüple doldur");
        lang.item(AMPHIBIOUS_ROVER_ITEM, "Amphibious Rover", "Amfibi Gezici");
        lang.add("entity.spacebreaker.amphibious_rover", "Amphibious Rover", "Amfibi Gezici");
        lang.add("tooltip.spacebreaker.amphibious_rover", "Pressurized cabin on a floating hull: drives into water or liquid methane and sails on at %s m/s",
                "Yüzen gövde üstünde basınçlı kabin: suya ya da sıvı metana girer, %s m/s ile yüzerek devam eder");
        lang.add("message.spacebreaker.rover.cabin_o2", "Cabin oxygen: %s%%", "Kabin oksijeni: %%%s");
        lang.add("hud.spacebreaker.rover.cabin", "Cabin O₂ %s%%", "Kabin O₂ %%%s");
        lang.add("hud.spacebreaker.rover.cabin_empty", "Cabin unpressurized", "Kabin basınçsız");
        lang.add("tooltip.spacebreaker.sprint_rover", "Light and quiet: %s m/s, a smaller battery and cargo bay, skids sooner on loose ground",
                "Hafif ve sessiz: %s m/s, daha küçük akü ve kargo, gevşek zeminde daha çabuk kayar");
        lang.add("tooltip.spacebreaker.rover", "Two seats, battery-electric. Sneak-use: cargo. Use with a solar panel, an RTG or a pellet to fit it",
                "İki kişilik, akülü elektrikli. Eğilip tıkla: kargo. Güneş paneli, RTG ya da peletle tıklayıp tak");
        lang.add("tooltip.spacebreaker.rover.charge", "Charges when parked next to a battery bank or cable",
                "Batarya bankası ya da kablonun yanına park edince şarj olur");
        lang.add("tooltip.spacebreaker.rover.hand_brake", "%s while driving: hand brake", "Sürerken %s: el freni");
        lang.add("hud.spacebreaker.rover.battery", "Battery %s%%", "Akü %%%s");
        lang.add("hud.spacebreaker.rover.percent", "%s%%", "%%%s");
        lang.add("hud.spacebreaker.rover.speed", "%s m/s", "%s m/s");
        lang.add("hud.spacebreaker.rover.solar", "Solar", "Güneş");
        lang.add("message.spacebreaker.rover.whirl_cleaned", "A dust whirl swept the solar panel clean", "Bir toz hortumu güneş panelini temizledi");
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
        // the pressurised rover: the standard one under a titanium cabin with windows, life support and shielding
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PRESSURIZED_ROVER_ITEM.get())
                .pattern("TGT").pattern("SRS").pattern("TOT")
                .define('T', EarthModule.TITANIUM.plateTag).define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_PANES_COLORLESS)
                .define('S', com.pizzaparker07.spacebreaker.content.machines.IsruRecipes.RADIATION_SHIELDING)
                .define('R', ROVER_ITEM.get()).define('O', com.pizzaparker07.spacebreaker.content.suit.SuitModule.OXYGEN_DISTRIBUTOR_ITEM.get())
                .unlockedBy("has_rover", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ROVER_ITEM.get()))
                .save(out, com.pizzaparker07.spacebreaker.Spacebreaker.id("pressurized_rover"));
        // the amphibious rover: the pressurised one on sealed aluminium floats with a stern propeller
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, AMPHIBIOUS_ROVER_ITEM.get())
                .pattern("AMA").pattern("ARA").pattern("AAA")
                .define('A', EarthModule.ALUMINUM.plateTag).define('M', EarthModule.ELECTRIC_MOTOR.get())
                .define('R', PRESSURIZED_ROVER_ITEM.get())
                .unlockedBy("has_pressurized_rover", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
                        PRESSURIZED_ROVER_ITEM.get()))
                .save(out, com.pizzaparker07.spacebreaker.Spacebreaker.id("amphibious_rover"));
    }
}
