package com.pizzaparker07.spacebreaker.testmod.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.guide.GuideMarkdown;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The field guide is client data, so it is read straight from the mod file: every chapter listed in book.json exists in
 * English and Turkish, parses cleanly and points only at things that exist (items, recipes, entities, structures, chapters,
 * techs, images and key bindings).
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuideGameTests {
    private static final String EMPTY = "empty";
    private static final List<String> LANGS = List.of("en_us", "tr_tr");

    private static Path asset(String path) {
        return ModList.get().getModFileById(Spacebreaker.MOD_ID).getFile().findResource("assets", Spacebreaker.MOD_ID, path);
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @GameTest(template = EMPTY)
    public static void guideChaptersExistInBothLanguagesAndResolve(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        try {
            JsonObject book = JsonParser.parseString(read(asset("guide/book.json"))).getAsJsonObject();
            Set<String> chapters = new LinkedHashSet<>();
            for (JsonElement cat : book.getAsJsonArray("categories")) {
                JsonObject c = cat.getAsJsonObject();
                String id = c.get("id").getAsString();
                if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(c.get("icon").getAsString()))) {
                    errors.add("category " + id + ": icon " + c.get("icon").getAsString());
                }
                for (JsonElement ch : c.getAsJsonArray("chapters")) {
                    if (!chapters.add(ch.getAsString())) {
                        errors.add("chapter listed twice: " + ch.getAsString());
                    }
                }
            }
            for (String lang : LANGS) {
                JsonObject langBook = JsonParser.parseString(read(asset("guide/" + lang + "/book.json"))).getAsJsonObject();
                for (JsonElement cat : book.getAsJsonArray("categories")) {
                    String id = cat.getAsJsonObject().get("id").getAsString();
                    if (!langBook.getAsJsonObject("categories").has(id)) {
                        errors.add(lang + ": no name for category " + id);
                    }
                }
                for (String chapter : chapters) {
                    Path p = asset("guide/" + lang + "/" + chapter + ".md");
                    if (!Files.exists(p)) {
                        errors.add(lang + "/" + chapter + ".md is missing");
                        continue;
                    }
                    GuideMarkdown.Chapter parsed = GuideMarkdown.parse(Spacebreaker.id(chapter), read(p));
                    check(helper, lang + "/" + chapter, parsed, chapters, errors);
                    machineFor(helper, lang + "/" + chapter, parsed, book, errors);
                }
            }
        } catch (IOException | RuntimeException e) {
            errors.add("could not read the guide: " + e);
        }
        if (!errors.isEmpty()) {
            helper.fail(errors.size() + " guide problems: " + String.join("; ", errors.subList(0, Math.min(12, errors.size()))));
        }
        helper.succeed();
    }

    /** Recipe types made by a crafting grid or the smithing table rather than a machine. */
    private static final Set<String> NO_MACHINE = Set.of("spacebreaker:acid_coating", "spacebreaker:suit_upgrade", "spacebreaker:tool_shapeless");

    /** Every machine recipe the guide shows names its machine in book.json, or the page would label it a crafting table. */
    private static void machineFor(GameTestHelper helper, String where, GuideMarkdown.Chapter chapter, JsonObject book, List<String> errors) {
        JsonObject machines = book.has("machines") ? book.getAsJsonObject("machines") : new JsonObject();
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (ResourceLocation id : chapter.refs().recipes()) {
            recipes.byKey(id).ifPresent(holder -> {
                ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
                if (type != null && type.getNamespace().equals(Spacebreaker.MOD_ID) && !NO_MACHINE.contains(type.toString())
                        && !machines.has(type.toString())) {
                    errors.add(where + ": no machine in book.json for recipe type " + type + " (" + id + ")");
                }
            });
        }
    }

    private static void check(GameTestHelper helper, String where, GuideMarkdown.Chapter chapter, Set<String> chapters,
                              List<String> errors) {
        GuideMarkdown.References refs = chapter.refs();
        refs.problems().forEach(p -> errors.add(where + ": " + p));
        if (chapter.title().isBlank() || chapter.title().equals(chapter.id().getPath())) {
            errors.add(where + ": no title");
        }
        if (!BuiltInRegistries.ITEM.containsKey(chapter.icon())) {
            errors.add(where + ": icon " + chapter.icon());
        }
        for (ResourceLocation id : refs.items()) {
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                errors.add(where + ": item " + id);
            }
        }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (ResourceLocation id : refs.recipes()) {
            if (recipes.byKey(id).isEmpty()) {
                errors.add(where + ": recipe " + id);
            }
        }
        for (ResourceLocation id : refs.entities()) {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                errors.add(where + ": entity " + id);
            }
        }
        for (ResourceLocation id : refs.structures()) {
            boolean multiblock = com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(helper.getLevel().registryAccess(), id).isPresent();
            if (!multiblock && (!id.getNamespace().equals(Spacebreaker.MOD_ID) || !Files.exists(asset("guide/structures/" + id.getPath() + ".nbt")))) {
                errors.add(where + ": structure " + id);
            }
        }
        for (ResourceLocation id : refs.chapters()) {
            if (!chapters.contains(id.getPath())) {
                errors.add(where + ": link to " + id);
            }
        }
        for (ResourceLocation id : refs.techs()) {
            if (Tech.get(helper.getLevel().registryAccess(), id).isEmpty()) {
                errors.add(where + ": tech " + id);
            }
        }
        for (ResourceLocation id : refs.images()) {
            if (!id.getNamespace().equals(Spacebreaker.MOD_ID) || !Files.exists(asset(id.getPath()))) {
                errors.add(where + ": image " + id);
            }
        }
        for (String opt : refs.optional()) {
            String[] kv = opt.split(" ", 2);
            ResourceLocation id = ResourceLocation.parse(kv[1]);
            boolean exists = switch (kv[0]) {
                case "recipe" -> helper.getLevel().getServer().getRecipeManager().byKey(id).isPresent();
                case "entity" -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
                default -> com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(helper.getLevel().registryAccess(), id).isPresent()
                        || Files.exists(asset("guide/structures/" + id.getPath() + ".nbt"));
            };
            if (exists) {
                Spacebreaker.LOGGER.info("Guide {}: optional {} now exists, the ? can go", where, opt);
            }
        }
        for (String key : refs.keys()) {
            if (!Language.getInstance().has(key)) {
                errors.add(where + ": key " + key);
            }
        }
    }
}
