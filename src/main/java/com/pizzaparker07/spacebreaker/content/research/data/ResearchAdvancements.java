package com.pizzaparker07.spacebreaker.content.research.data;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.DungeonDiscoveredTrigger;
import com.pizzaparker07.spacebreaker.content.dungeons.DungeonsModule;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.research.Milestones;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.research.TechLearnedTrigger;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The main progression tree ("Spacebreaker" tab). Rocketry and Moon milestones are reported by other areas through
 * {@link Milestones#award}; entering the orbit / Moon dimensions also counts.
 */
public final class ResearchAdvancements implements AdvancementSubProvider {
    public static final ResourceLocation BACKGROUND = Spacebreaker.id("textures/gui/advancements/backgrounds/space.png");
    public static final ResourceKey<Level> EARTH_ORBIT = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("earth_orbit"));
    public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon"));

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> out) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(new ItemStack(SuitModule.EVA_HELMET.get()), title("root"), desc("root"), BACKGROUND, AdvancementType.TASK, false, false, false)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .save(out, id("root"));

        // materials & power
        AdvancementHolder ore = task(out, root, "first_ore", new ItemStack(EarthModule.RAW_BAUXITE.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(EarthModule.RAW_BAUXITE.get()));
        AdvancementHolder ingot = task(out, ore, "first_ingot", new ItemStack(EarthModule.ALUMINUM_INGOT.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(EarthModule.ALUMINUM_INGOT.get()));
        task(out, ingot, "solar_power", new ItemStack(MachinesModule.SOLAR_PANEL_ITEM.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(MachinesModule.SOLAR_PANEL_ITEM.get()));

        // research
        task(out, root, "read_guide", new ItemStack(ResearchModule.GUIDE_BOOK.get()), milestone(Milestones.OPEN_GUIDE));
        AdvancementHolder sample = task(out, root, "field_sample", Research.sample(ResearchTree.EARTH),
                InventoryChangeTrigger.TriggerInstance.hasItems(ResearchModule.FIELD_SAMPLE.get()));
        AdvancementHolder lab = task(out, sample, "research_lab", new ItemStack(ResearchModule.RESEARCH_LAB_ITEM.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(ResearchModule.RESEARCH_LAB_ITEM.get()));
        AdvancementHolder first = task(out, lab, "first_tech", Research.disk(ResearchTree.id("electronics")),
                techLearned(Optional.empty(), Optional.empty()));
        AdvancementHolder ten = build(out, first, "ten_techs", new ItemStack(Items.WRITABLE_BOOK), AdvancementType.GOAL,
                "known", techLearned(Optional.empty(), Optional.of(10)));
        build(out, ten, "thirty_techs", new ItemStack(Items.ENCHANTED_BOOK), AdvancementType.CHALLENGE,
                "known", techLearned(Optional.empty(), Optional.of(30)));

        // dungeons and the road to the Moon
        AdvancementHolder dungeon = task(out, root, "dungeon_found", Research.disk(ResearchTree.id("basic_rocketry")),
                DungeonsModule.DISCOVERED_TRIGGER.get().createCriterion(new DungeonDiscoveredTrigger.Instance(Optional.empty(), Optional.empty(), Optional.empty())));
        AdvancementHolder rocketry = task(out, dungeon, "basic_rocketry", new ItemStack(Items.FIREWORK_ROCKET),
                techLearned(Optional.of(ResearchTree.id("basic_rocketry")), Optional.empty()));
        AdvancementHolder rocket = task(out, rocketry, "first_rocket", new ItemStack(Items.FIREWORK_STAR), milestone(Milestones.FIRST_ROCKET));
        AdvancementHolder launch = task(out, rocket, "launch", new ItemStack(Items.FIRE_CHARGE), milestone(Milestones.LAUNCH));
        AdvancementHolder orbit = Advancement.Builder.advancement().parent(launch)
                .display(new ItemStack(Items.ENDER_EYE), title("orbit"), desc("orbit"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("milestone", milestone(Milestones.ORBIT))
                .addCriterion("dimension", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(EARTH_ORBIT))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(out, id("orbit"));
        AdvancementHolder moon = Advancement.Builder.advancement().parent(orbit)
                .display(new ItemStack(MoonModule.LUNAR_REGOLITH_ITEM.get()), title("moon_landing"), desc("moon_landing"), null,
                        AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("milestone", milestone(Milestones.MOON_LANDING))
                .addCriterion("dimension", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(MOON))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(out, id("moon_landing"));
        build(out, moon, "safe_return", new ItemStack(Items.GRASS_BLOCK), AdvancementType.CHALLENGE, "milestone", milestone(Milestones.SAFE_RETURN));

        // suit
        AdvancementHolder suit = task(out, root, "suit_up", new ItemStack(SuitModule.EVA_HELMET.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(SuitModule.EVA_HELMET.get()));
        task(out, suit, "deep_breath", new ItemStack(SuitModule.OXYGEN_TANK.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(SuitModule.OXYGEN_TANK.get()));
    }

    private static Criterion<?> milestone(String id) {
        return ResearchModule.MILESTONE.get().createCriterion(new Milestones.Trigger.Instance(Optional.empty(), id));
    }

    private static Criterion<?> techLearned(Optional<ResourceLocation> tech, Optional<Integer> minKnown) {
        return ResearchModule.TECH_LEARNED.get().createCriterion(new TechLearnedTrigger.Instance(Optional.empty(), tech, Optional.empty(), minKnown));
    }

    private static AdvancementHolder task(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemStack icon, Criterion<?> c) {
        return build(out, parent, name, icon, AdvancementType.TASK, "done", c);
    }

    private static AdvancementHolder build(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemStack icon,
                                           AdvancementType type, String key, Criterion<?> c) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, false)
                .addCriterion(key, c)
                .save(out, id(name));
    }

    private static String id(String name) {
        return Spacebreaker.id("main/" + name).toString();
    }

    private static Component title(String name) {
        return Component.translatable("advancements.spacebreaker." + name + ".title");
    }

    private static Component desc(String name) {
        return Component.translatable("advancements.spacebreaker." + name + ".description");
    }

    /** Titles and descriptions, English + Turkish. */
    public static void addTranslations(Lang l) {
        a(l, "root", "Spacebreaker", "Reach for the stars, one technology at a time",
                "Spacebreaker", "Yıldızlara uzan, her seferinde bir teknolojiyle");
        a(l, "first_ore", "Red Earth", "Mine bauxite, the ore of aluminium", "Kızıl Toprak", "Alüminyumun cevheri boksiti kaz");
        a(l, "first_ingot", "Lighter than Steel", "Make an aluminium ingot", "Çelikten Hafif", "Bir alüminyum külçe yap");
        a(l, "solar_power", "Catching Rays", "Build a solar tracker", "Işık Avcısı", "Bir güneş takipli panel yap");
        a(l, "read_guide", "Read the Manual", "Open the Spacebreaker field guide", "Kılavuzu Oku", "Spacebreaker saha rehberini aç");
        a(l, "field_sample", "Rock Collector", "Take a field sample with a sample kit", "Taş Koleksiyoncusu", "Numune çantasıyla bir saha numunesi al");
        a(l, "research_lab", "Laboratory", "Build a Research Lab", "Laboratuvar", "Bir Araştırma Laboratuvarı kur");
        a(l, "first_tech", "Eureka!", "Learn your first technology", "Buldum!", "İlk teknolojini öğren");
        a(l, "ten_techs", "Scholar", "Know 10 technologies", "Bilgin", "10 teknoloji bil");
        a(l, "thirty_techs", "Rocket Scientist", "Know 30 technologies", "Roket Bilimcisi", "30 teknoloji bil");
        a(l, "dungeon_found", "Ruins of the Space Age", "Discover a lost research site", "Uzay Çağının Kalıntıları", "Kayıp bir araştırma alanı keşfet");
        a(l, "basic_rocketry", "Action and Reaction", "Learn Basic Rocketry", "Etki ve Tepki", "Temel Roketçiliği öğren");
        a(l, "first_rocket", "Some Assembly Required", "Assemble your first rocket", "Biraz Montaj Gerekir", "İlk roketini birleştir");
        a(l, "launch", "Lift-off!", "Launch a rocket", "Kalkış!", "Bir roket fırlat");
        a(l, "orbit", "Falling Around the World", "Reach orbit", "Dünyanın Etrafına Düşmek", "Yörüngeye ulaş");
        a(l, "moon_landing", "One Small Step", "Land on the Moon", "Küçük Bir Adım", "Ay'a in");
        a(l, "safe_return", "Welcome Home", "Come back to Earth safely", "Eve Hoş Geldin", "Dünya'ya sağ salim dön");
        a(l, "suit_up", "Suit Up", "Get an EVA helmet", "Giyin Kuşan", "Bir EVA kaskı edin");
        a(l, "deep_breath", "Deep Breath", "Get an oxygen tank", "Derin Nefes", "Bir oksijen tüpü edin");
    }

    private static void a(Lang l, String name, String en, String enDesc, String tr, String trDesc) {
        l.add("advancements.spacebreaker." + name + ".title", en, tr);
        l.add("advancements.spacebreaker." + name + ".description", enDesc, trDesc);
    }
}
