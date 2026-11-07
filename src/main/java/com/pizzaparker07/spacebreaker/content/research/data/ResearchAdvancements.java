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
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.StartRidingTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
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
        AdvancementHolder steel = task(out, ingot, "steel", new ItemStack(item("steel_ingot")),
                InventoryChangeTrigger.TriggerInstance.hasItems(item("steel_ingot")));
        task(out, steel, "fabricator", new ItemStack(item("fabricator")), InventoryChangeTrigger.TriggerInstance.hasItems(item("fabricator")));
        AdvancementHolder oil = task(out, steel, "rp1", new ItemStack(item("rp1_bucket")), InventoryChangeTrigger.TriggerInstance.hasItems(item("rp1_bucket")));
        build(out, oil, "liquid_oxygen", new ItemStack(item("liquid_oxygen_bucket")), AdvancementType.GOAL, "done",
                InventoryChangeTrigger.TriggerInstance.hasItems(item("liquid_oxygen_bucket")));

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
        AdvancementHolder rocket = anyOf(out, rocketry, "first_rocket", new ItemStack(item("rocket")), AdvancementType.TASK,
                milestone(Milestones.FIRST_ROCKET), InventoryChangeTrigger.TriggerInstance.hasItems(item("rocket")));
        AdvancementHolder launch = anyOf(out, rocket, "launch", new ItemStack(item("crew_capsule")), AdvancementType.TASK,
                milestone(Milestones.LAUNCH), StartRidingTrigger.TriggerInstance.playerStartsRiding(EntityPredicate.Builder.entity()
                        .vehicle(EntityPredicate.Builder.entity().of(BuiltInRegistries.ENTITY_TYPE.get(Spacebreaker.id("rocket"))))));
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
        // a soft capsule landing on Earth after having been on the Moon (any Earth return alone, from a first orbit or by
        // beacon, does not count)
        Advancement.Builder.advancement().parent(moon)
                .display(new ItemStack(Items.GRASS_BLOCK), title("safe_return"), desc("safe_return"), null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("moon", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(MOON))
                .addCriterion("landed", milestone(Milestones.SAFE_RETURN))
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(out, id("safe_return"));
        task(out, orbit, "moon_orbit", new ItemStack(Items.ENDER_PEARL), ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(
                ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("moon_orbit"))));

        // the outer solar system, one advancement per system (any of its worlds), and one for having been to all of them
        List<List<String>> requirements = new java.util.ArrayList<>();
        Advancement.Builder tour = Advancement.Builder.advancement();
        for (String[] system : SYSTEMS) {
            String name = system[0];
            Advancement.Builder b = Advancement.Builder.advancement().parent(orbit)
                    .display(new ItemStack(item(system[1])), title(name), desc(name), null, AdvancementType.GOAL, true, true, false);
            List<String> any = new java.util.ArrayList<>();
            for (int i = 2; i < system.length; i++) {
                Criterion<?> c = ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(
                        ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(system[i])));
                b.addCriterion(system[i], c);
                tour.addCriterion(system[i], c);
                any.add(system[i]);
            }
            b.requirements(AdvancementRequirements.Strategy.OR).save(out, id(name));
            requirements.add(any);
        }
        tour.parent(orbit)
                .display(new ItemStack(Items.FILLED_MAP), title("grand_tour"), desc("grand_tour"), null, AdvancementType.CHALLENGE, true, true, false)
                .requirements(new AdvancementRequirements(requirements))
                .save(out, id("grand_tour"));
        task(out, moon, "moon_dungeon", Research.disk(ResearchTree.id("lunar_surveying")),
                DungeonsModule.DISCOVERED_TRIGGER.get().createCriterion(new DungeonDiscoveredTrigger.Instance(Optional.empty(), Optional.empty(),
                        Optional.of(ResearchTree.MOON))));
        task(out, moon, "moon_creature", new ItemStack(item("kreep_fragment")), KilledTrigger.TriggerInstance.playerKilledEntity(
                EntityPredicate.Builder.entity().of(BuiltInRegistries.ENTITY_TYPE.get(Spacebreaker.id("regolith_tick")))));

        // suit
        AdvancementHolder suit = task(out, root, "suit_up", new ItemStack(SuitModule.EVA_HELMET.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(SuitModule.EVA_HELMET.get()));
        task(out, suit, "deep_breath", new ItemStack(SuitModule.OXYGEN_TANK.get()),
                InventoryChangeTrigger.TriggerInstance.hasItems(SuitModule.OXYGEN_TANK.get()));
        AdvancementHolder full = build(out, suit, "full_suit", new ItemStack(item("eva_chestplate")), AdvancementType.GOAL, "done",
                InventoryChangeTrigger.TriggerInstance.hasItems(item("eva_helmet"), item("eva_chestplate"), item("eva_leggings"), item("eva_boots")));
        task(out, full, "jetpack", new ItemStack(item("maneuvering_unit")), InventoryChangeTrigger.TriggerInstance.hasItems(item("maneuvering_unit")));
    }

    /** Advancement name, icon item, then the worlds of that system (dimension paths). */
    public static final String[][] SYSTEMS = {
            {"inner_planets", "martian_regolith", "mars", "venus", "mercury", "phobos", "deimos"},
            {"belt", "carbonaceous_chondrite", "ceres", "vesta"},
            {"jupiter_system", "sulfur", "io", "europa", "ganymede", "callisto"},
            {"saturn_system", "tholin", "titan", "enceladus"},
            {"uranus_system", "graben_floor_ice", "miranda", "ariel", "titania", "umbriel", "oberon"},
            {"neptune_system", "nitrogen_geyser", "triton", "proteus", "nereid"},
            {"kuiper_gate", "tholin_sand", "pluto", "charon", "nix", "hydra"},
            {"near_earth", "bennu_boulder", "bennu", "ryugu", "eros", "itokawa"},
            {"comets", "comet_ice", "churyumov_gerasimenko", "encke", "halley", "tempel_1"},
    };

    private static net.minecraft.world.item.Item item(String path) {
        return BuiltInRegistries.ITEM.get(Spacebreaker.id(path));
    }

    private static AdvancementHolder anyOf(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemStack icon,
                                           AdvancementType type, Criterion<?> a, Criterion<?> b) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, false)
                .addCriterion("a", a).addCriterion("b", b)
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(out, id(name));
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
        a(l, "first_ore", "Red Earth", "Mine bauxite, the ore of aluminium", "Kızıl Toprak", "Alüminyum cevheri olan boksiti kaz");
        a(l, "first_ingot", "Lighter than Steel", "Make an aluminium ingot", "Çelikten Hafif", "Bir alüminyum külçe yap");
        a(l, "solar_power", "Catching Rays", "Build a solar tracker", "Işık Avcısı", "Bir güneş paneli yap");
        a(l, "read_guide", "Read the Manual", "Open the Spacebreaker field guide", "Rehberi Oku", "Spacebreaker saha rehberini aç");
        a(l, "field_sample", "Rock Collector", "Take a field sample with a sample kit", "Taş Koleksiyoncusu", "Numune kitiyle bir saha numunesi al");
        a(l, "research_lab", "Laboratory", "Build a Research Lab", "Laboratuvar", "Bir Araştırma Laboratuvarı kur");
        a(l, "first_tech", "Eureka!", "Learn your first technology", "Buldum!", "İlk teknolojini öğren");
        a(l, "ten_techs", "Scholar", "Know 10 technologies", "Bilgin", "10 teknoloji bil");
        a(l, "thirty_techs", "Rocket Scientist", "Know 30 technologies", "Roket Bilimcisi", "30 teknoloji bil");
        a(l, "dungeon_found", "Ruins of the Space Age", "Discover a lost research site", "Uzay Çağının Kalıntıları", "Kayıp bir araştırma alanı keşfet");
        a(l, "basic_rocketry", "Action and Reaction", "Learn Basic Rocketry", "Etki ve Tepki", "Temel Roketçiliği öğren");
        a(l, "first_rocket", "Some Assembly Required", "Assemble your first rocket", "Parça Parça Roket", "İlk roketini birleştir");
        a(l, "launch", "Strapped In", "Climb aboard a rocket on the pad", "Kemerler Bağlandı", "Rampadaki bir rokete bin");
        a(l, "orbit", "Falling Around the World", "Reach orbit", "Yörüngedesin!", "Yörüngeye ulaş: Dünya'nın etrafında düşüyorsun ama yere hiç çarpmıyorsun");
        a(l, "moon_landing", "One Small Step", "Land on the Moon", "Küçük Bir Adım", "Ay'a in");
        a(l, "safe_return", "Welcome Home", "Come back from the Moon and land softly on Earth", "Eve Hoş Geldin", "Ay'dan dön ve Dünya'ya yumuşak iniş yap");
        a(l, "steel", "Forged in the Arc", "Make steel in an electric arc furnace", "Ark Ateşinde Çelik", "Elektrik ark fırınında çelik yap");
        a(l, "fabricator", "Workshop", "Get a fabricator", "Atölye", "Bir Parça Üretim Tezgâhı edin");
        a(l, "rp1", "Black Gold", "Refine crude oil into RP-1 kerosene", "Kara Altın", "Ham petrolü RP-1 kerosenine arıt");
        a(l, "liquid_oxygen", "Colder than Space", "Liquefy oxygen at −183 °C", "Uzaydan da Soğuk", "Oksijeni −183 °C'de sıvılaştır");
        a(l, "moon_orbit", "Around the Moon", "Reach lunar orbit", "Ay'ın Etrafında", "Ay yörüngesine ulaş");
        a(l, "moon_dungeon", "Someone Was Here First", "Find a lost outpost or lava tube on the Moon",
                "Biri Senden Önce Gelmiş", "Ay'da kayıp bir karakol ya da lav tüpü bul");
        a(l, "moon_creature", "Pest Control", "Defeat a regolith tick", "Haşere Kontrolü", "Bir regolit kenesini yen");
        a(l, "full_suit", "Pressurised", "Collect all four pieces of an EVA suit", "Tam Donanım", "Bir EVA giysisinin dört parçasını da topla");
        a(l, "jetpack", "Free Flyer", "Get a maneuvering unit", "Serbest Uçuş", "Bir manevra ünitesi edin");
        a(l, "suit_up", "Suit Up", "Get an EVA helmet", "Giyin Kuşan", "Bir EVA kaskı edin");
        a(l, "inner_planets", "Neighbours of the Sun", "Land on Mars, Venus, Mercury, Phobos or Deimos", "Güneş'in Komşuları", "Mars'a, Venüs'e, Merkür'e, Phobos'a ya da Deimos'a in");
        a(l, "belt", "Welcome to the Belt", "Land on Ceres or Vesta", "Kuşağa Hoş Geldin", "Ceres'e ya da Vesta'ya in");
        a(l, "jupiter_system", "In Jupiter's Shadow", "Land on one of Jupiter's moons", "Jüpiter'in Gölgesinde", "Jüpiter'in uydularından birine in");
        a(l, "saturn_system", "Beneath the Rings", "Land on Titan or Enceladus", "Halkaların Altında", "Titan'a ya da Enceladus'a in");
        a(l, "uranus_system", "The Tilted Giant", "Land on one of Uranus' moons", "Yan Yatmış Dev", "Uranüs'ün uydularından birine in");
        a(l, "neptune_system", "Edge of the Blue Giant", "Land on Triton, Proteus or Nereid", "Mavi Devin Kıyısı", "Triton'a, Proteus'a ya da Nereid'e in");
        a(l, "kuiper_gate", "Gateway to the Kuiper Belt", "Land on Pluto or one of its moons", "Kuiper'in Kapısı", "Plüton'a ya da uydularından birine in");
        a(l, "near_earth", "Close Visitors", "Land on a near-Earth asteroid", "Yakın Ziyaretçiler", "Dünya'ya yakın bir asteroite in");
        a(l, "comets", "Ice Wanderers", "Land on a comet", "Buz Gezginleri", "Bir kuyruklu yıldıza in");
        a(l, "grand_tour", "Solar System Wanderer", "Land on a world in every one of these systems", "Güneş Sistemi Gezgini", "Bu sistemlerin her birinde bir dünyaya in");
        a(l, "deep_breath", "Deep Breath", "Get an oxygen tank", "Derin Nefes", "Bir oksijen tüpü edin");
    }

    private static void a(Lang l, String name, String en, String enDesc, String tr, String trDesc) {
        l.add("advancements.spacebreaker." + name + ".title", en, tr);
        l.add("advancements.spacebreaker." + name + ".description", enDesc, trDesc);
    }
}
