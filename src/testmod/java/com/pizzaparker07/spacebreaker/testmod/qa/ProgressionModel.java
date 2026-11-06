package com.pizzaparker07.spacebreaker.testmod.qa;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitTier;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import com.pizzaparker07.spacebreaker.core.tech.TechRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Whole-game progression model for a fresh survival world. Everything is read from the live registries (recipes of
 * every type, item/fluid tags, loot tables, world generation, dungeons, technologies, rocket parts and the transfer
 * graph); the only hand-written rules are the ones the game implements in code (which machines enforce research,
 * where power comes from, the first-orbit flight records, the sample kit, refuelling).
 *
 * <p>The model is a monotone fixed point over "facts" (items, fluids per world, technologies, sample kinds, worlds
 * that can be visited and returned from). Each fact remembers the step it was first derived in and the facts it was
 * derived from, so the report can print a valid order for any goal.
 */
final class ProgressionModel {
    record Fact(int step, String reason, List<String> deps) {
    }

    record WorldInfo(ResourceKey<Level> dim, Optional<ResourceLocation> body, boolean orbit, Set<Block> blocks, Set<EntityType<?>> entities,
                     Set<ResourceLocation> lootTables, Set<Fluid> fluids, boolean breathable, Fluid gas, Set<ResourceLocation> structures) {
    }

    /** What a loot table can give (recursively through nested tables). */
    static final class Loot {
        final Set<Item> items = new LinkedHashSet<>();
        final Set<ResourceLocation> disks = new LinkedHashSet<>();
        final Set<ResourceLocation> kinds = new LinkedHashSet<>();
        final Map<ResourceLocation, Integer> diskPool = new HashMap<>();
    }

    /** One production rule: a recipe (or code behaviour) at a station. */
    record Rule(String id, Block machine, List<Set<Item>> inputs, List<Set<Fluid>> fluidInputs, List<Item> outputs, List<Fluid> fluidOutputs) {
    }

    final MinecraftServer server;
    final Player probe;
    final SbConfig.Realism realism;
    final Map<String, Fact> facts = new LinkedHashMap<>();
    final Map<ResourceKey<Level>, WorldInfo> worlds = new LinkedHashMap<>();
    final Map<ResourceLocation, Loot> loot = new HashMap<>();
    final List<Rule> rules = new ArrayList<>();
    final Set<String> specialRecipes = new TreeSet<>();
    final Set<String> unmappedRecipeTypes = new TreeSet<>();
    final Set<ResourceLocation> orphanLoot = new TreeSet<>();
    final Map<ResourceLocation, TechNode> techTree;
    final Map<String, RocketSearch.Result> missions = new LinkedHashMap<>();
    final Map<ResourceKey<Level>, String> survival = new LinkedHashMap<>();
    final List<String> notes = new ArrayList<>();
    final Set<ResourceLocation> machinesThatEnforce = new TreeSet<>();
    int step;
    /** "What if" mode: tech disks assumed to be obtainable on Earth (fixes not made yet). */
    final Set<ResourceLocation> assumedDisks = new TreeSet<>();

    private final Map<ResourceKey<? extends Registry<?>>, Codec<?>> codecs = new HashMap<>();
    private final RegistryOps<JsonElement> ops;
    private final Map<String, String> lastRocketKey = new HashMap<>();

    static final Map<String, String> MACHINE_FOR_KIND = Map.ofEntries(
            Map.entry("crushing", "crusher"), Map.entry("arc_smelting", "arc_furnace"), Map.entry("pressing", "press"),
            Map.entry("fabricating", "fabricator"), Map.entry("refining", "refinery"), Map.entry("electrolysis", "electrolyzer"),
            Map.entry("liquefaction", "cryogenic_liquefier"), Map.entry("sabatier", "sabatier_reactor"),
            Map.entry("regolith_processing", "regolith_heater"), Map.entry("melting", "ice_melter"),
            Map.entry("chemical_reaction", "chemical_reactor"));

    ProgressionModel(MinecraftServer server, Player probe, SbConfig.Realism realism) {
        this.server = server;
        this.probe = probe;
        this.realism = realism;
        this.ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (var d : DataPackRegistriesHooks.getDataPackRegistries()) {
            codecs.put(d.key(), d.elementCodec());
        }
        for (var d : RegistryDataLoader.DIMENSION_REGISTRIES) {
            codecs.put(d.key(), d.elementCodec());
        }
        this.techTree = Tech.all(server.registryAccess());
        Tech.setKnownSilently(probe, new HashSet<>());
    }

    // ================================================================== facts

    static String item(Item i) {
        return "item:" + BuiltInRegistries.ITEM.getKey(i);
    }

    static String fluid(ResourceKey<Level> w, Fluid f) {
        return "fluid:" + w.location() + ":" + BuiltInRegistries.FLUID.getKey(f);
    }

    static String tech(ResourceLocation t) {
        return "tech:" + t;
    }

    static String world(ResourceKey<Level> w) {
        return "world:" + w.location();
    }

    static String landed(ResourceKey<Level> w) {
        return "landed:" + w.location();
    }

    static String kind(ResourceLocation k) {
        return "kind:" + k;
    }

    boolean has(String fact) {
        return facts.containsKey(fact);
    }

    boolean add(String fact, String reason, List<String> deps) {
        if (facts.containsKey(fact)) {
            return false;
        }
        facts.put(fact, new Fact(step, reason, List.copyOf(deps)));
        return true;
    }

    boolean hasItem(Item i) {
        return has(item(i));
    }

    Set<ResourceLocation> knownTech() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String f : facts.keySet()) {
            if (f.startsWith("tech:")) {
                out.add(ResourceLocation.parse(f.substring(5)));
            }
        }
        return out;
    }

    // ================================================================== world scan

    void scan() {
        // recipes
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            rule(holder);
        }
        // loot tables
        for (ResourceLocation id : server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
            loot(id, new HashSet<>());
        }
        // dungeons: world + chests
        Map<ResourceLocation, Set<ResourceLocation>> dungeonLoot = new HashMap<>();
        Set<ResourceLocation> mapped = new HashSet<>();
        server.registryAccess().registry(Dungeons.REGISTRY).ifPresent(reg -> reg.entrySet().forEach(e -> {
            var def = e.getValue();
            dungeonLoot.computeIfAbsent(def.world(), k -> new HashSet<>()).addAll(def.lootTables());
            mapped.addAll(def.lootTables());
            for (var g : def.guardians()) {
                dungeonLoot.computeIfAbsent(def.world(), k -> new HashSet<>()).add(
                        BuiltInRegistries.ENTITY_TYPE.get(g.entity()).getDefaultLootTable().location());
            }
        }));
        for (var e : stems().entrySet()) {
            worlds.put(e.getKey(), scanWorld(e.getKey(), e.getValue(), dungeonLoot));
        }
        for (WorldInfo w : worlds.values()) {
            mapped.addAll(w.lootTables());
        }
        for (ResourceLocation id : server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
            if (id.getNamespace().equals(Spacebreaker.MOD_ID) && !mapped.contains(id) && !id.getPath().startsWith("blocks/")
                    && !id.getPath().startsWith("entities/")) {
                Loot l = loot.get(id);
                if (l != null && (!l.disks.isEmpty() || !l.kinds.isEmpty() || l.items.stream().anyMatch(i -> !isVanilla(i)))) {
                    orphanLoot.add(id);
                }
            }
        }
        // which machines enforce their research (MachineBlockEntity locks itself until the owner knows it)
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals(Spacebreaker.MOD_ID) || !(b instanceof EntityBlock eb)) {
                continue;
            }
            try {
                var be = eb.newBlockEntity(BlockPos.ZERO, b.defaultBlockState());
                if (be instanceof MachineBlockEntity && !TechRequirement.forUnlock(server.registryAccess(), id).isEmpty()) {
                    machinesThatEnforce.add(id);
                }
            } catch (RuntimeException ignored) {
                // some block entities need a level; they are not machines
            }
        }
    }

    static boolean isVanilla(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("minecraft");
    }

    /**
     * The dimensions of a normal survival world: vanilla ones from the default world preset (the GameTest server
     * itself runs on a flat preset) and every data-pack dimension of the mod, read from its JSON.
     */
    Map<ResourceKey<Level>, LevelStem> stems() {
        Map<ResourceKey<Level>, LevelStem> out = new LinkedHashMap<>();
        server.registryAccess().registry(Registries.WORLD_PRESET).flatMap(r -> r.getOptional(WorldPresets.NORMAL)).ifPresent(preset -> {
            var dims = preset.createWorldDimensions().dimensions();
            for (var key : List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) {
                LevelStem stem = dims.get(key);
                if (stem != null) {
                    out.put(ResourceKey.create(Registries.DIMENSION, key.location()), stem);
                }
            }
        });
        @SuppressWarnings("unchecked")
        Codec<LevelStem> codec = (Codec<LevelStem>) codecs.get(Registries.LEVEL_STEM);
        var resources = server.getResourceManager().listResources("dimension", rl -> rl.getPath().endsWith(".json"));
        for (var e : new TreeMap<>(resources).entrySet()) {
            ResourceLocation file = e.getKey();
            if (!file.getNamespace().equals(Spacebreaker.MOD_ID)) {
                continue;
            }
            String path = file.getPath().substring("dimension/".length(), file.getPath().length() - ".json".length());
            try (var reader = e.getValue().openAsReader()) {
                JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                codec.parse(ops, json).result().ifPresentOrElse(
                        stem -> out.put(ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(path)), stem),
                        () -> notes.add("dimension " + file + " does not parse as a level stem"));
            } catch (java.io.IOException ex) {
                notes.add("dimension " + file + " unreadable: " + ex.getMessage());
            }
        }
        return out;
    }

    Optional<CelestialBody> bodyOf(ResourceKey<Level> dim) {
        return Bodies.forDimension(server.registryAccess(), dim);
    }

    boolean isOrbit(ResourceKey<Level> dim) {
        return bodyOf(dim).flatMap(CelestialBody::orbitDimension).filter(dim::equals).isPresent()
                || dim.location().getPath().endsWith("orbit");
    }

    private static final Map<String, String> GASES = Map.of("CO2", "carbon_dioxide", "O2", "oxygen", "CH4", "methane", "H2", "hydrogen");

    /** Mirrors {@code AtmosphereProcessorBlockEntity.yieldAt}: the most abundant separable gas, nothing in vacuum or orbit. */
    Fluid gasOf(ResourceKey<Level> dim) {
        if (isOrbit(dim)) {
            return null;
        }
        var body = bodyOf(dim);
        if (body.isEmpty() || body.get().atmosphere().pressureKpa() <= 0.01) {
            return body.isEmpty() && dim == Level.OVERWORLD ? BuiltInRegistries.FLUID.get(Spacebreaker.id("oxygen")) : null;
        }
        String best = null;
        double fraction = 0;
        for (var e : body.get().atmosphere().composition().entrySet()) {
            if (GASES.containsKey(e.getKey()) && e.getValue() > fraction) {
                best = GASES.get(e.getKey());
                fraction = e.getValue();
            }
        }
        return best == null ? null : BuiltInRegistries.FLUID.get(Spacebreaker.id(best));
    }

    double gravityOf(ResourceKey<Level> dim) {
        return bodyOf(dim).map(b -> b.physical().surfaceGravity()).orElse(CelestialBody.EARTH_GRAVITY);
    }

    private WorldInfo scanWorld(ResourceKey<Level> dim, LevelStem stemValue, Map<ResourceLocation, Set<ResourceLocation>> dungeonLoot) {
        Optional<ResourceLocation> body = Bodies.registry(server.registryAccess())
                .flatMap(reg -> bodyOf(dim).map(reg::getKey));
        boolean orbit = isOrbit(dim);
        Set<Block> blocks = new LinkedHashSet<>();
        Set<EntityType<?>> entities = new LinkedHashSet<>();
        Set<Fluid> fluids = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        List<JsonElement> todo = new ArrayList<>();
        todo.add(QaScan.encode(codecs.get(Registries.LEVEL_STEM), stemValue, ops));
        Set<ResourceKey<Biome>> biomes = new HashSet<>();
        stemValue.generator().getBiomeSource().possibleBiomes().forEach(h -> h.unwrapKey().ifPresent(biomes::add));
        for (ResourceKey<Biome> b : biomes) {
            todo.add(new com.google.gson.JsonPrimitive(b.location().toString()));
            Biome biome = server.registryAccess().registryOrThrow(Registries.BIOME).get(b);
            if (biome == null) {
                continue;
            }
            for (var step : biome.getGenerationSettings().features()) {
                for (var pf : step) {
                    pf.unwrapKey().ifPresent(k -> todo.add(new com.google.gson.JsonPrimitive(k.location().toString())));
                    if (pf.unwrapKey().isEmpty()) {
                        todo.add(QaScan.encode(PlacedFeature.DIRECT_CODEC, pf.value(), ops));
                    }
                }
            }
            for (MobCategory cat : MobCategory.values()) {
                biome.getMobSettings().getMobs(cat).unwrap().forEach(s -> entities.add(s.type));
            }
        }
        // biome modifiers (data-driven ore and oil generation, extra spawns)
        server.registryAccess().registry(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS).ifPresent(reg -> reg.forEach(mod -> {
            if (mod instanceof net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier add
                    && add.biomes().stream().anyMatch(h -> h.unwrapKey().map(biomes::contains).orElse(false))) {
                add.features().forEach(pf -> {
                    pf.unwrapKey().ifPresent(k -> todo.add(new com.google.gson.JsonPrimitive(k.location().toString())));
                    // code-placed features with no block list in their config
                    String feature = String.valueOf(BuiltInRegistries.FEATURE.getKey(pf.value().feature().value().feature()));
                    if (feature.equals("spacebreaker:oil_deposit")) {
                        todo.add(new com.google.gson.JsonPrimitive("spacebreaker:crude_oil"));
                        todo.add(new com.google.gson.JsonPrimitive("spacebreaker:oil_seep"));
                    }
                });
            } else if (mod instanceof net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier spawns
                    && spawns.biomes().stream().anyMatch(h -> h.unwrapKey().map(biomes::contains).orElse(false))) {
                spawns.spawners().forEach(sp -> entities.add(sp.type));
            }
        }));
        body.ifPresent(id -> todo.add(new com.google.gson.JsonPrimitive(id.toString())));
        List<ResourceKey<? extends Registry<?>>> follow = List.of(Registries.BIOME, Registries.PLACED_FEATURE, Registries.CONFIGURED_FEATURE,
                Registries.NOISE_SETTINGS, TerrainProfile.REGISTRY);
        while (!todo.isEmpty()) {
            JsonElement e = todo.remove(todo.size() - 1);
            for (String s : QaScan.ids(e)) {
                boolean tag = s.startsWith("#");
                ResourceLocation id = ResourceLocation.parse(tag ? s.substring(1) : s);
                if (tag) {
                    BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, id)).ifPresent(set -> set.forEach(h -> addBlock(h.value(), blocks, fluids)));
                    continue;
                }
                if (BuiltInRegistries.BLOCK.containsKey(id) && !id.getPath().equals("air")) {
                    addBlock(BuiltInRegistries.BLOCK.get(id), blocks, fluids);
                }
                if (BuiltInRegistries.FLUID.containsKey(id) && !id.getPath().equals("empty")) {
                    fluids.add(source(BuiltInRegistries.FLUID.get(id)));
                }
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(id) && !id.getPath().equals("pig")) {
                    // "pig" is the registry default; only count real references
                    entities.add(BuiltInRegistries.ENTITY_TYPE.get(id));
                }
                for (var reg : follow) {
                    String key = reg.location() + "|" + id;
                    if (seen.contains(key)) {
                        continue;
                    }
                    var r = server.registryAccess().registry(reg).orElse(null);
                    if (r == null || !r.containsKey(id)) {
                        continue;
                    }
                    seen.add(key);
                    Codec<?> codec = codecs.get(reg);
                    if (codec != null) {
                        todo.add(QaScan.encode(codec, r.get(id), ops));
                    }
                }
            }
        }
        Set<ResourceLocation> tables = new LinkedHashSet<>();
        String world = dim.location().getPath();
        if (dim.location().getNamespace().equals(Spacebreaker.MOD_ID) || dim == Level.OVERWORLD) {
            String w = dim == Level.OVERWORLD ? "earth" : world;
            for (ResourceLocation id : server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
                if (id.getNamespace().equals(Spacebreaker.MOD_ID)
                        && (id.getPath().startsWith("dungeons/" + w + "/") || id.getPath().startsWith("chests/" + w + "/"))) {
                    tables.add(id);
                }
            }
        }
        tables.addAll(dungeonLoot.getOrDefault(dim.location(), Set.of()));
        if (dim == Level.OVERWORLD) {
            tables.addAll(dungeonLoot.getOrDefault(Spacebreaker.id("earth"), Set.of()));
        }
        Set<ResourceLocation> structures = new TreeSet<>();
        var structureReg = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        structureReg.entrySet().forEach(e -> {
            if (e.getValue().biomes().stream().anyMatch(h -> h.unwrapKey().map(biomes::contains).orElse(false))) {
                structures.add(e.getKey().location());
            }
        });
        boolean breathable = bodyOf(dim).map(b -> b.atmosphere().breathable() && !orbit).orElse(dim == Level.OVERWORLD);
        Fluid gas = gasOf(dim);
        return new WorldInfo(dim, body, orbit, blocks, entities, tables, fluids, breathable, gas, structures);
    }

    private static void addBlock(Block b, Set<Block> blocks, Set<Fluid> fluids) {
        blocks.add(b);
        if (b instanceof LiquidBlock lb) {
            fluids.add(source(lb.fluid));
        }
    }

    static Fluid source(Fluid f) {
        return f instanceof FlowingFluid ff ? ff.getSource() : f;
    }

    // ------------------------------------------------------------------ loot

    Loot loot(ResourceLocation id, Set<ResourceLocation> stack) {
        Loot cached = loot.get(id);
        if (cached != null) {
            return cached;
        }
        Loot out = new Loot();
        if (!stack.add(id)) {
            return out;
        }
        LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        if (table != LootTable.EMPTY) {
            JsonElement json = QaScan.encode(LootTable.DIRECT_CODEC, table, ops);
            if (json.isJsonObject()) {
                for (JsonElement pool : QaScan.arr(json.getAsJsonObject(), "pools")) {
                    if (pool.isJsonObject()) {
                        for (JsonElement entry : QaScan.arr(pool.getAsJsonObject(), "entries")) {
                            entry(entry, out, stack);
                        }
                    }
                }
            }
        }
        loot.put(id, out);
        return out;
    }

    private void entry(JsonElement e, Loot out, Set<ResourceLocation> stack) {
        if (!e.isJsonObject()) {
            return;
        }
        JsonObject o = e.getAsJsonObject();
        String type = QaScan.str(o, "type");
        for (JsonElement c : QaScan.arr(o, "children")) {
            entry(c, out, stack);
        }
        if (type == null) {
            return;
        }
        switch (type) {
            case "minecraft:item" -> {
                ResourceLocation name = ResourceLocation.tryParse(String.valueOf(QaScan.str(o, "name")));
                if (name == null || !BuiltInRegistries.ITEM.containsKey(name)) {
                    return;
                }
                Item item = BuiltInRegistries.ITEM.get(name);
                JsonElement functions = o.get("functions");
                if (name.equals(Spacebreaker.id("tech_disk"))) {
                    disks(functions, out);
                } else if (name.equals(Spacebreaker.id("field_sample")) || name.equals(Spacebreaker.id("experiment_data"))) {
                    for (String s : QaScan.ids(functions)) {
                        ResourceLocation k = ResourceLocation.tryParse(s);
                        if (k != null && !s.startsWith("minecraft:") && !s.equals("spacebreaker:sample")) {
                            out.kinds.add(k);
                        }
                    }
                } else {
                    out.items.add(item);
                }
            }
            case "minecraft:tag" -> {
                ResourceLocation name = ResourceLocation.tryParse(String.valueOf(QaScan.str(o, "name")));
                if (name != null) {
                    BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, name)).ifPresent(set -> set.forEach(h -> out.items.add(h.value())));
                }
            }
            case "minecraft:loot_table" -> {
                JsonElement v = o.get("value");
                if (v != null && v.isJsonPrimitive()) {
                    Loot nested = loot(ResourceLocation.parse(v.getAsString()), stack);
                    out.items.addAll(nested.items);
                    out.disks.addAll(nested.disks);
                    out.kinds.addAll(nested.kinds);
                } else if (v != null && v.isJsonObject()) {
                    for (JsonElement pool : QaScan.arr(v.getAsJsonObject(), "pools")) {
                        if (pool.isJsonObject()) {
                            for (JsonElement en : QaScan.arr(pool.getAsJsonObject(), "entries")) {
                                entry(en, out, stack);
                            }
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    /** Mirrors {@code TechDiskLootFunction.candidates}. */
    private void disks(JsonElement functions, Loot out) {
        if (functions == null || !functions.isJsonArray()) {
            return;
        }
        for (JsonElement f : functions.getAsJsonArray()) {
            if (!f.isJsonObject() || !"spacebreaker:random_tech_disk".equals(QaScan.str(f.getAsJsonObject(), "function"))) {
                continue;
            }
            JsonObject o = f.getAsJsonObject();
            List<String> techs = QaScan.arr(o, "techs").asList().stream().map(JsonElement::getAsString).toList();
            List<String> cats = QaScan.arr(o, "categories").asList().stream().map(JsonElement::getAsString).toList();
            List<String> samples = QaScan.arr(o, "samples").asList().stream().map(JsonElement::getAsString).toList();
            int min = o.has("min_tier") ? o.get("min_tier").getAsInt() : 1;
            int max = o.has("max_tier") ? o.get("max_tier").getAsInt() : 99;
            List<ResourceLocation> pool = new ArrayList<>();
            techTree.forEach((id, n) -> {
                if (!techs.isEmpty() && !techs.contains(id.toString())) {
                    return;
                }
                if (!cats.isEmpty() && !cats.contains(n.category())) {
                    return;
                }
                if (n.tier() < min || n.tier() > max) {
                    return;
                }
                if (!samples.isEmpty() && n.samples().stream().noneMatch(s -> samples.contains(s.toString()))) {
                    return;
                }
                pool.add(id);
            });
            if (pool.isEmpty()) {
                techTree.forEach((id, n) -> {
                    if (n.tier() <= 1) {
                        pool.add(id);
                    }
                });
            }
            for (ResourceLocation t : pool) {
                out.disks.add(t);
                out.diskPool.merge(t, pool.size(), Math::min);
            }
        }
    }

    // ------------------------------------------------------------------ recipes

    private void rule(RecipeHolder<?> holder) {
        Recipe<?> r = holder.value();
        String id = holder.id().toString();
        if (r instanceof ProcessingRecipe pr) {
            String m = MACHINE_FOR_KIND.get(pr.kind().name());
            if (m == null) {
                unmappedRecipeTypes.add(pr.kind().name());
                return;
            }
            Block machine = BuiltInRegistries.BLOCK.get(Spacebreaker.id(m));
            List<Set<Item>> in = new ArrayList<>();
            pr.ingredients.forEach(s -> in.add(items(s.ingredient())));
            pr.tool.ifPresent(t -> in.add(items(t)));
            List<Set<Fluid>> fin = new ArrayList<>();
            pr.fluidInput.ifPresent(f -> fin.add(fluids(f)));
            pr.fluidInput2.ifPresent(f -> fin.add(fluids(f)));
            List<Item> outItems = new ArrayList<>();
            pr.results.forEach(s -> outItems.add(s.getItem()));
            pr.bonus.ifPresent(s -> outItems.add(s.getItem()));
            List<Fluid> outFluids = new ArrayList<>();
            pr.fluidResults.forEach(s -> outFluids.add(source(s.getFluid())));
            rules.add(new Rule(id, machine, in, fin, outItems, outFluids));
            return;
        }
        ItemStack result;
        try {
            result = r.getResultItem(server.registryAccess());
        } catch (RuntimeException e) {
            result = ItemStack.EMPTY;
        }
        List<Ingredient> ingredients = new ArrayList<>(r.getIngredients());
        if (ingredients.isEmpty()) {
            ingredients.addAll(reflectIngredients(r));
        }
        if (result.isEmpty() || ingredients.isEmpty()) {
            specialRecipes.add(id + " (" + BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.getSerializer()) + ")");
            return;
        }
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
        if (type != null && !type.getNamespace().equals("minecraft")) {
            unmappedRecipeTypes.add(type.toString());
            return;
        }
        List<Set<Item>> in = new ArrayList<>();
        for (Ingredient i : ingredients) {
            if (!i.isEmpty()) {
                in.add(items(i));
            }
        }
        rules.add(new Rule(id, null, in, List.of(), List.of(result.getItem()), List.of()));
    }

    private static List<Ingredient> reflectIngredients(Recipe<?> r) {
        List<Ingredient> out = new ArrayList<>();
        for (Class<?> c = r.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == Ingredient.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(r);
                        if (v instanceof Ingredient i) {
                            out.add(i);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // inaccessible field: the recipe stays "special"
                    }
                }
            }
        }
        return out;
    }

    private static Set<Item> items(Ingredient i) {
        Set<Item> out = new LinkedHashSet<>();
        for (ItemStack s : i.getItems()) {
            out.add(s.getItem());
        }
        return out;
    }

    private static Set<Fluid> fluids(net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient f) {
        Set<Fluid> out = new LinkedHashSet<>();
        for (var s : f.getFluids()) {
            out.add(source(s.getFluid()));
        }
        return out;
    }

    // ================================================================== fixed point

    void run() {
        start();
        boolean changed = true;
        while (changed) {
            step++;
            changed = false;
            changed |= worldsGive();
            changed |= produce();
            changed |= learn();
            if (!changed) {
                changed = fly();
            }
        }
    }

    private void start() {
        List<String> none = List.of();
        for (Item i : BuiltInRegistries.ITEM) {
            if (isVanilla(i) && i != Items.AIR) {
                add(item(i), "vanilla", none);
            }
        }
        add(world(Level.OVERWORLD), "start", none);
        for (ResourceLocation t : assumedDisks) {
            add("disk:" + t, "ASSUMED: a disk on Earth (proposed fix)", List.of(world(Level.OVERWORLD)));
        }
        add(landed(Level.OVERWORLD), "start", none);
        // vanilla dimensions are reached through vanilla portals
        add(world(Level.NETHER), "nether portal (vanilla)", none);
        add(landed(Level.NETHER), "nether portal (vanilla)", none);
        add(world(Level.END), "end portal (vanilla)", none);
        add(landed(Level.END), "end portal (vanilla)", none);
        for (Item i : List.of(Items.WATER_BUCKET, Items.LAVA_BUCKET)) {
            add(item(i), "vanilla", none);
        }
        add(fluid(Level.OVERWORLD, Fluids.WATER), "water source", none);
        add(fluid(Level.OVERWORLD, Fluids.LAVA), "lava source", none);
    }

    /** Worlds the player can visit and come back from hand over their blocks, creatures, chests and samples. */
    private boolean worldsGive() {
        boolean changed = false;
        Item kit = BuiltInRegistries.ITEM.get(Spacebreaker.id("sample_kit"));
        for (WorldInfo w : worlds.values()) {
            String wf = world(w.dim());
            if (!has(wf)) {
                continue;
            }
            List<String> dep = List.of(wf);
            for (Block b : w.blocks()) {
                for (Item i : blockDrops(b)) {
                    changed |= add(item(i), "mined in " + w.dim().location() + " (" + BuiltInRegistries.BLOCK.getKey(b) + ")", dep);
                }
            }
            for (EntityType<?> e : w.entities()) {
                Loot l = loot(e.getDefaultLootTable().location(), new HashSet<>());
                for (Item i : l.items) {
                    changed |= add(item(i), "dropped by " + BuiltInRegistries.ENTITY_TYPE.getKey(e) + " in " + w.dim().location(), dep);
                }
            }
            for (ResourceLocation t : w.lootTables()) {
                Loot l = loot(t, new HashSet<>());
                for (Item i : l.items) {
                    changed |= add(item(i), "loot " + t, dep);
                }
                for (ResourceLocation d : l.disks) {
                    changed |= add("disk:" + d, "loot " + t + (l.diskPool.getOrDefault(d, 1) > 1 ? " (random, 1 of " + l.diskPool.get(d) + ")" : ""), dep);
                }
                for (ResourceLocation k : l.kinds) {
                    changed |= add(kind(k), "loot " + t, dep);
                }
            }
            for (Fluid f : w.fluids()) {
                if (f == Fluids.WATER || f == Fluids.LAVA) {
                    changed |= add(fluid(w.dim(), f), "natural source", dep);
                }
            }
            if (w.orbit()) {
                changed |= add(kind(Spacebreaker.id("orbital_flight")), "first arrival in an orbit (" + w.dim().location() + ")", dep);
            }
            if (hasItem(kit) && w.body().isPresent() && !w.orbit()) {
                changed |= add(kind(w.body().get()), "sample kit used in " + w.dim().location(), List.of(wf, item(kit)));
            }
        }
        return changed;
    }

    private final Map<Block, Set<Item>> drops = new HashMap<>();

    Set<Item> blockDrops(Block b) {
        return drops.computeIfAbsent(b, block -> {
            Set<Item> out = new LinkedHashSet<>(loot(block.getLootTable().location(), new HashSet<>()).items);
            if (block instanceof LiquidBlock lb) {
                Item bucket = lb.fluid.getBucket();
                if (bucket != null && bucket != Items.AIR) {
                    out.add(bucket);
                }
            }
            return out;
        });
    }

    /** Machines usable in world {@code w}: built, unlocked when the machine enforces it, and powered there. */
    List<String> machineDeps(Block machine, ResourceKey<Level> w) {
        Item it = machine.asItem();
        if (!hasItem(it) || !has(landed(w))) {
            return null;
        }
        List<String> deps = new ArrayList<>();
        deps.add(item(it));
        deps.add(landed(w));
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(machine);
        if (machinesThatEnforce.contains(id)) {
            String known = null;
            for (ResourceLocation t : TechRequirement.forUnlock(server.registryAccess(), id).techs()) {
                if (has(tech(t))) {
                    known = tech(t);
                    break;
                }
            }
            if (known == null) {
                return null;
            }
            deps.add(known);
        }
        String power = power(w);
        if (power == null) {
            return null;
        }
        deps.add(power);
        return deps;
    }

    /** A power source in {@code w}: combustion where the air burns, solar panels anywhere with sunlight. */
    String power(ResourceKey<Level> w) {
        WorldInfo info = worlds.get(w);
        Item gen = BuiltInRegistries.ITEM.get(Spacebreaker.id("combustion_generator"));
        if (info != null && info.breathable() && hasItem(gen)) {
            add("power:" + w.location(), "combustion generator (breathable air)", List.of(item(gen), landed(w)));
            return "power:" + w.location();
        }
        Item solar = BuiltInRegistries.ITEM.get(Spacebreaker.id("solar_panel"));
        if (hasItem(solar)) {
            add("power:" + w.location(), "solar panels", List.of(item(solar), landed(w)));
            return "power:" + w.location();
        }
        return null;
    }

    private boolean produce() {
        boolean changed = false;
        for (Rule r : rules) {
            if (r.machine() == null) {
                changed |= apply(r, Level.OVERWORLD, List.of());
                continue;
            }
            for (WorldInfo w : worlds.values()) {
                if (!has(landed(w.dim()))) {
                    continue;
                }
                List<String> m = machineDeps(r.machine(), w.dim());
                if (m != null) {
                    changed |= apply(r, w.dim(), m);
                }
            }
        }
        // code-defined production
        Block pumpjack = BuiltInRegistries.BLOCK.get(Spacebreaker.id("pumpjack"));
        Block processor = BuiltInRegistries.BLOCK.get(Spacebreaker.id("atmosphere_processor"));
        for (WorldInfo w : worlds.values()) {
            if (!has(landed(w.dim()))) {
                continue;
            }
            List<String> pj = machineDeps(pumpjack, w.dim());
            if (pj != null) {
                for (Fluid f : w.fluids()) {
                    if (BuiltInRegistries.FLUID.getKey(f).getPath().equals("crude_oil")) {
                        changed |= add(fluid(w.dim(), f), "pumpjack over an oil reservoir", pj);
                    }
                }
            }
            List<String> ap = machineDeps(processor, w.dim());
            if (ap != null && w.gas() != null) {
                changed |= add(fluid(w.dim(), w.gas()), "atmosphere processor", ap);
            }
            // a fluid made here can be bucketed (an empty bucket and a tank); buckets are not carried between worlds
            // by the model, so off-world fuel has to be made there (or the rocket flies a round trip)
            for (Item i : BuiltInRegistries.ITEM) {
                if (i instanceof BucketItem b && b.content != Fluids.EMPTY && !hasItem(i) && hasItem(Items.BUCKET)
                        && has(fluid(w.dim(), source(b.content)))) {
                    changed |= add(item(i), "bucket filled in " + w.dim().location(), List.of(item(Items.BUCKET), fluid(w.dim(), source(b.content))));
                }
            }
        }
        return changed;
    }

    private boolean apply(Rule r, ResourceKey<Level> w, List<String> machine) {
        boolean missingOut = false;
        for (Item o : r.outputs()) {
            missingOut |= !hasItem(o);
        }
        for (Fluid f : r.fluidOutputs()) {
            missingOut |= !has(fluid(w, f));
        }
        if (!missingOut) {
            return false;
        }
        List<String> deps = new ArrayList<>(machine);
        for (Set<Item> slot : r.inputs()) {
            String pick = null;
            for (Item i : slot) {
                if (hasItem(i)) {
                    pick = item(i);
                    break;
                }
            }
            if (pick == null) {
                return false;
            }
            deps.add(pick);
        }
        for (Set<Fluid> slot : r.fluidInputs()) {
            String pick = null;
            for (Fluid f : slot) {
                if (has(fluid(w, f))) {
                    pick = fluid(w, f);
                    break;
                }
            }
            if (pick == null) {
                return false;
            }
            deps.add(pick);
        }
        boolean changed = false;
        String where = r.machine() == null ? "" : " in " + w.location();
        for (Item o : r.outputs()) {
            changed |= add(item(o), "recipe " + r.id() + where, deps);
        }
        for (Fluid f : r.fluidOutputs()) {
            changed |= add(fluid(w, f), "recipe " + r.id() + where, deps);
        }
        return changed;
    }

    private boolean learn() {
        Item lab = BuiltInRegistries.ITEM.get(Spacebreaker.id("research_lab"));
        String power = hasItem(lab) ? power(Level.OVERWORLD) : null;
        if (power == null) {
            return false;
        }
        boolean changed = false;
        for (var e : techTree.entrySet()) {
            ResourceLocation id = e.getKey();
            if (has(tech(id))) {
                continue;
            }
            TechNode n = e.getValue();
            List<String> deps = new ArrayList<>(List.of(item(lab), power));
            boolean prereqs = true;
            for (ResourceLocation r : n.requires()) {
                prereqs &= has(tech(r));
                deps.add(tech(r));
            }
            if (!prereqs) {
                continue;
            }
            String source = null;
            if (has("disk:" + id)) {
                source = "disk:" + id;
            } else {
                for (ResourceLocation k : n.samples()) {
                    if (has(kind(k))) {
                        source = kind(k);
                        break;
                    }
                }
            }
            if (source == null) {
                continue;
            }
            deps.add(source);
            changed |= add(tech(id), source.startsWith("disk:") ? "tech disk" : "analysed " + source.substring(5), deps);
        }
        if (changed) {
            Tech.setKnownSilently(probe, knownTech());
        }
        // research items exist once any disk / sample / record does
        for (String f : List.copyOf(facts.keySet())) {
            if (f.startsWith("disk:")) {
                changed |= add("item:spacebreaker:tech_disk", "any tech disk", List.of(f));
            } else if (f.startsWith("kind:")) {
                String k = f.substring(5);
                boolean flight = k.endsWith("_flight");
                changed |= add(flight ? "item:spacebreaker:experiment_data" : "item:spacebreaker:field_sample", "any " + (flight ? "flight record" : "field sample"), List.of(f));
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ rockets

    private final Map<String, List<RocketSearch.Candidate>> designCache = new HashMap<>();
    /** The designs buildable at the end of the run (for the "missions not possible" table). */
    List<RocketSearch.Candidate> best = List.of();

    /** Tries to reach every location with the best rocket the player can build right now. */
    private boolean fly() {
        Item assembler = BuiltInRegistries.ITEM.get(Spacebreaker.id("rocket_assembler"));
        Item pad = BuiltInRegistries.ITEM.get(Spacebreaker.id("launch_pad"));
        if (!hasItem(assembler) || !hasItem(pad)) {
            return false;
        }
        var lookup = RocketParts.lookup(server.registryAccess());
        Set<ResourceLocation> parts = new TreeSet<>();
        List<String> partDeps = new ArrayList<>(List.of(item(assembler), item(pad)));
        server.registryAccess().registry(RocketParts.REGISTRY).ifPresent(reg -> reg.keySet().forEach(id -> {
            Item it = BuiltInRegistries.ITEM.get(id);
            if (it != Items.AIR && hasItem(it) && RocketTech.unlocked(probe, new ItemStack(it))) {
                parts.add(id);
            }
        }));
        for (ResourceLocation p : parts) {
            partDeps.add(item(BuiltInRegistries.ITEM.get(p)));
            ResourceLocation t = RocketTech.techFor(probe, new ItemStack(BuiltInRegistries.ITEM.get(p)));
            if (t != null) {
                partDeps.add(tech(t));
            }
        }
        Set<Propellant> earthFuels = fuels(Level.OVERWORLD, partDeps);
        String key = parts + "|" + earthFuels;
        List<RocketSearch.Candidate> designs = designCache.computeIfAbsent(key, k -> RocketSearch.designs(lookup, parts, earthFuels));
        if (designs.isEmpty()) {
            return false;
        }
        boolean changed = add("item:spacebreaker:rocket", "assembled in the rocket assembler", partDeps);
        best = designs;
        double gEarth = gravityOf(Level.OVERWORLD);
        for (Location target : Locations.all()) {
            ResourceKey<Level> dim = target.dimension();
            if (dim == null || !worlds.containsKey(dim) || missions.containsKey(target.id())) {
                continue;
            }
            double gThere = gravityOf(dim);
            List<String> deps = new ArrayList<>(partDeps);
            Set<Propellant> there = target.orbit() ? Set.of() : fuels(dim, deps);
            String sig = key + "|" + there;
            if (sig.equals(lastRocketKey.get(target.id()))) {
                continue;
            }
            lastRocketKey.put(target.id(), sig);
            // one-way: can it get there at all? (lets local industry make fuel for the way home)
            List<Location> out = RocketSearch.route(Location.EARTH_SURFACE, target, realism);
            if (!out.isEmpty() && !has(landed(dim))) {
                for (RocketSearch.Candidate c : designs) {
                    if (com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath.twr(c.layout(), 0, c.layout().fullFuel(), gEarth) >= 1.05
                            && com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner.fly(c.layout(), 0, c.layout().fullFuel(), out, realism).possible()) {
                        changed |= add(landed(dim), "one-way flight to " + target.id() + " with " + describe(c), deps);
                        break;
                    }
                }
            }
            RocketSearch.Result r = RocketSearch.mission(designs, target, realism, gEarth, gThere, there);
            if (r != null) {
                missions.put(target.id(), r);
                changed |= add(landed(dim), "flight to " + target.id(), deps);
                changed |= add(world(dim), (r.mode().equals("round_trip") ? "round trip to " : "flight with refuelling at ") + target.id()
                        + " with " + describe(r.rocket()), deps);
                survival.put(dim, survivalAt(worlds.get(dim)));
            }
        }
        return changed;
    }

    /** Propellants that can be loaded in a world: fuel and liquid oxygen made (or carried) there. */
    Set<Propellant> fuels(ResourceKey<Level> w, List<String> deps) {
        Set<Propellant> out = new LinkedHashSet<>();
        Item pipe = BuiltInRegistries.ITEM.get(Spacebreaker.id("fluid_pipe"));
        Item umbilical = BuiltInRegistries.ITEM.get(Spacebreaker.id("fuel_umbilical"));
        String feed = hasItem(umbilical) ? item(umbilical) : hasItem(pipe) ? item(pipe) : null;
        if (feed == null) {
            return out;
        }
        String lox = tagged(w, Propellant.LOX_TAG);
        if (lox == null) {
            return out;
        }
        for (Propellant p : Propellant.values()) {
            String f = tagged(w, p.fuelTag());
            if (f != null) {
                out.add(p);
                deps.add(f);
            }
        }
        if (!out.isEmpty()) {
            deps.add(lox);
            deps.add(feed);
        }
        return out;
    }

    private String tagged(ResourceKey<Level> w, TagKey<Fluid> tag) {
        for (var h : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
            Fluid f = source(h.value());
            if (has(fluid(w, f))) {
                return fluid(w, f);
            }
        }
        return null;
    }

    static String describe(RocketSearch.Candidate c) {
        List<String> groups = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation id : c.design().parts()) {
            String p = id.getPath();
            if (p.equals("decoupler") || p.equals("interstage")) {
                groups.add(group(counts) + " / " + p);
                counts.clear();
            } else {
                counts.merge(p, 1, Integer::sum);
            }
        }
        groups.add(group(counts));
        return c.layout().stages().size() + "-stage [" + String.join(" / ", groups) + "], " + Math.round(c.totalDv()) + " m/s vacuum Δv";
    }

    private static String group(Map<String, Integer> counts) {
        List<String> parts = new ArrayList<>();
        counts.forEach((k, v) -> parts.add(v > 1 ? v + "× " + k : k));
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ survival

    String survivalAt(WorldInfo w) {
        Optional<CelestialBody> body = w.body().flatMap(id -> Bodies.get(server.registryAccess(), Bodies.key(id.getPath())));
        List<String> parts = new ArrayList<>();
        if (w.orbit()) {
            parts.add("in orbit (vacuum, inside the capsule)");
        } else if (body.isPresent()) {
            CelestialBody b = body.get();
            double night = b.climate().nightTempC();
            double day = b.climate().dayTempC();
            parts.add(String.format(java.util.Locale.ROOT, "%.0f…%.0f °C, %.1f kPa%s, radiation %.2f", night, day,
                    b.atmosphere().pressureKpa(), w.breathable() ? " breathable" : "", b.radiation()));
            List<String> ok = new ArrayList<>();
            for (SuitTier t : SuitTier.ALL) {
                boolean have = true;
                for (Item i : BuiltInRegistries.ITEM) {
                    if (i instanceof SpaceSuitItem s && s.tier() == t) {
                        have &= hasItem(i);
                    }
                }
                if (have && (t.tech() == null || has(tech(t.tech()))) && t.minTempC() <= night && t.maxTempC() >= day) {
                    ok.add(t.id());
                }
            }
            parts.add(ok.isEmpty() ? "NO SUIT covers the temperature range yet" : "suits: " + String.join("/", ok));
        }
        if (!w.breathable()) {
            Item tank = BuiltInRegistries.ITEM.get(Spacebreaker.id("oxygen_tank"));
            Item filler = BuiltInRegistries.ITEM.get(Spacebreaker.id("oxygen_filler"));
            boolean oxygenHome = facts.keySet().stream().anyMatch(f -> f.startsWith("fluid:minecraft:overworld:") && f.endsWith(":oxygen"));
            boolean oxygenThere = facts.keySet().stream().anyMatch(f -> f.startsWith("fluid:" + w.dim().location() + ":") && f.endsWith(":oxygen"));
            parts.add("oxygen tank " + yes(hasItem(tank)) + ", filler " + yes(hasItem(filler)) + ", O₂ at home " + yes(oxygenHome)
                    + ", O₂ made there " + yes(oxygenThere));
        }
        parts.add("power there " + yes(power(w.dim()) != null));
        return String.join("; ", parts);
    }

    private static String yes(boolean b) {
        return b ? "yes" : "NO";
    }

    // ================================================================== queries for the report

    /** Every fact {@code goal} needs, in the order the model derived them. */
    List<Map.Entry<String, Fact>> path(String goal) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> todo = new ArrayList<>(List.of(goal));
        while (!todo.isEmpty()) {
            String f = todo.remove(todo.size() - 1);
            if (!seen.add(f)) {
                continue;
            }
            Fact fact = facts.get(f);
            if (fact != null) {
                todo.addAll(fact.deps());
            }
        }
        List<Map.Entry<String, Fact>> out = new ArrayList<>();
        for (String f : seen) {
            if (facts.containsKey(f)) {
                out.add(Map.entry(f, facts.get(f)));
            }
        }
        out.sort((a, b) -> Integer.compare(a.getValue().step(), b.getValue().step()));
        return out;
    }

    /** Why a technology is not learnable: which prerequisites and sources are missing. */
    String whyNot(ResourceLocation id) {
        TechNode n = techTree.get(id);
        List<String> why = new ArrayList<>();
        for (ResourceLocation r : n.requires()) {
            if (!has(tech(r))) {
                why.add("needs " + r.getPath());
            }
        }
        boolean source = has("disk:" + id) || n.samples().stream().anyMatch(k -> has(kind(k)));
        if (!source) {
            List<String> s = new ArrayList<>();
            for (ResourceLocation k : n.samples()) {
                s.add(k.getPath());
            }
            List<String> tables = new ArrayList<>();
            for (WorldInfo w : worlds.values()) {
                for (ResourceLocation t : w.lootTables()) {
                    if (loot.containsKey(t) && loot.get(t).disks.contains(id)) {
                        tables.add(t.getPath());
                    }
                }
            }
            why.add("no source reached (samples: " + (s.isEmpty() ? "none" : String.join(", ", s)) + "; disk in loot: "
                    + (tables.isEmpty() ? "nowhere" : String.join(", ", tables.stream().distinct().limit(4).toList())
                    + (tables.size() > 4 ? " …" : "")) + ")");
        }
        return String.join("; ", why);
    }

    /** First missing input of each recipe that would make the item. */
    String whyNotItem(Item item) {
        List<String> out = new ArrayList<>();
        for (Rule r : rules) {
            if (!r.outputs().contains(item)) {
                continue;
            }
            List<String> miss = new ArrayList<>();
            if (r.machine() != null) {
                ResourceLocation mid = BuiltInRegistries.BLOCK.getKey(r.machine());
                if (!hasItem(r.machine().asItem())) {
                    miss.add("machine " + mid.getPath() + " not buildable");
                } else if (machinesThatEnforce.contains(mid)
                        && TechRequirement.forUnlock(server.registryAccess(), mid).techs().stream().noneMatch(t -> has(tech(t)))) {
                    miss.add("machine " + mid.getPath() + " locked by " + TechRequirement.forUnlock(server.registryAccess(), mid).techs());
                }
            }
            for (Set<Item> slot : r.inputs()) {
                if (slot.stream().noneMatch(this::hasItem)) {
                    miss.add(slot.isEmpty() ? "an empty ingredient (tag with no items)"
                            : BuiltInRegistries.ITEM.getKey(slot.iterator().next()).toString() + (slot.size() > 1 ? " (or " + (slot.size() - 1) + " alternatives)" : ""));
                }
            }
            for (Set<Fluid> slot : r.fluidInputs()) {
                if (slot.isEmpty()) {
                    miss.add("an empty fluid ingredient");
                } else if (worlds.keySet().stream().noneMatch(w -> slot.stream().anyMatch(f -> has(fluid(w, f))))) {
                    miss.add("fluid " + BuiltInRegistries.FLUID.getKey(slot.iterator().next()));
                }
            }
            out.add(r.id() + ": " + (miss.isEmpty() ? "inputs exist but not together in one powered world" : String.join(", ", miss)));
        }
        return out.isEmpty() ? "no recipe, loot, drop or world source" : String.join(" | ", out);
    }

    /** Technologies whose prerequisites are known but that have no sample kind and no disk in any reached world. */
    List<ResourceLocation> sourcelessFrontier() {
        List<ResourceLocation> out = new ArrayList<>();
        techTree.forEach((id, n) -> {
            if (!has(tech(id)) && n.samples().isEmpty() && n.requires().stream().allMatch(r -> has(tech(r))) && !has("disk:" + id)) {
                out.add(id);
            }
        });
        return out;
    }

    Collection<Item> modItems() {
        List<Item> out = new ArrayList<>();
        for (Item i : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(Spacebreaker.MOD_ID)) {
                out.add(i);
            }
        }
        return out;
    }

    Map<String, Integer> stepsOf(String prefix) {
        Map<String, Integer> out = new TreeMap<>();
        facts.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                out.put(k.substring(prefix.length()), v.step());
            }
        });
        return out;
    }
}
