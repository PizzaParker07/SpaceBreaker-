package com.pizzaparker07.spacebreaker.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven guide content (resource packs / other areas can add to it):
 * <ul>
 *   <li>{@code assets/<ns>/guide/chapters.json}: {@code [{"id": "research", "icon": "spacebreaker:research_lab", "order": 30}]};
 *       the chapter title is the language key {@code guide.<ns>.chapter.<id>}</li>
 *   <li>{@code assets/<ns>/guide/<lang>/<entry>.json}: one entry with its pages, see {@link Entry}. Missing languages fall
 *       back to {@code en_us}.</li>
 * </ul>
 * Page element types: {@code {"text": "..."}}, {@code {"heading": "..."}}, {@code {"items": ["id", ...], "text": "caption"}},
 * {@code {"image": "ns:textures/gui/guide/x.png", "width": 112, "height": 64, "text": "caption"}},
 * {@code {"recipe": "ns:recipe_id"}}, {@code {"page_break": true}}.
 * An entry with {@code "requires_tech"} stays locked until the player knows that technology.
 */
public final class GuideData extends SimplePreparableReloadListener<GuideData.Loaded> {
    public static final GuideData INSTANCE = new GuideData();

    public record Chapter(String id, ResourceLocation icon, int order) {
    }

    public record Element(String type, String text, List<ResourceLocation> items, @Nullable ResourceLocation image, int width, int height) {
    }

    /**
     * @param id           entry id (namespace + file name)
     * @param chapter      chapter id
     * @param title        title shown in lists and on the first page
     * @param icon         item icon
     * @param order        sort order within the chapter
     * @param requiresTech technology needed to read it (optional)
     * @param elements     content
     */
    public record Entry(ResourceLocation id, String chapter, String title, ResourceLocation icon, int order,
                        Optional<ResourceLocation> requiresTech, List<Element> elements) {
        public String searchText() {
            StringBuilder b = new StringBuilder(title.toLowerCase(Locale.ROOT));
            for (Element e : elements) {
                b.append(' ').append(e.text().toLowerCase(Locale.ROOT));
                e.items().forEach(i -> b.append(' ').append(i.getPath()));
            }
            return b.toString();
        }
    }

    public record Loaded(List<Chapter> chapters, Map<String, Map<ResourceLocation, Entry>> byLang) {
    }

    private Loaded data = new Loaded(List.of(), Map.of());

    private GuideData() {
    }

    public List<Chapter> chapters() {
        return data.chapters();
    }

    /** Entries in the current language (falling back to English entry by entry), sorted by chapter order then entry order. */
    public List<Entry> entries() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<ResourceLocation, Entry> merged = new HashMap<>(data.byLang().getOrDefault("en_us", Map.of()));
        merged.putAll(data.byLang().getOrDefault(lang, Map.of()));
        Map<String, Integer> chapterOrder = new HashMap<>();
        for (Chapter c : data.chapters()) {
            chapterOrder.put(c.id(), c.order());
        }
        List<Entry> out = new ArrayList<>(merged.values());
        out.sort(Comparator.comparingInt((Entry e) -> chapterOrder.getOrDefault(e.chapter(), 999))
                .thenComparingInt(Entry::order).thenComparing(e -> e.id().toString()));
        return out;
    }

    @Override
    protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<Chapter> chapters = new ArrayList<>();
        for (String ns : manager.getNamespaces()) {
            for (Resource res : manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(ns, "guide/chapters.json"))) {
                readChapters(res, chapters);
            }
        }
        Map<String, Chapter> unique = new HashMap<>();
        chapters.forEach(c -> unique.put(c.id(), c));
        List<Chapter> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparingInt(Chapter::order).thenComparing(Chapter::id));

        Map<String, Map<ResourceLocation, Entry>> byLang = new HashMap<>();
        Map<ResourceLocation, Resource> files = manager.listResources("guide", rl -> rl.getPath().endsWith(".json") && !rl.getPath().endsWith("chapters.json"));
        files.forEach((rl, res) -> {
            // guide/<lang>/<name>.json
            String[] parts = rl.getPath().split("/");
            if (parts.length < 3) {
                return;
            }
            String lang = parts[1];
            String name = rl.getPath().substring(("guide/" + lang + "/").length(), rl.getPath().length() - 5);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), name);
            try (Reader reader = res.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                byLang.computeIfAbsent(lang, k -> new HashMap<>()).put(id, parseEntry(id, json));
            } catch (Exception e) {
                Spacebreaker.LOGGER.warn("Bad guide entry {}: {}", rl, e.toString());
            }
        });
        return new Loaded(List.copyOf(sorted), byLang);
    }

    private static void readChapters(Resource res, List<Chapter> out) {
        try (Reader reader = res.openAsReader()) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                out.add(new Chapter(GsonHelper.getAsString(o, "id"), ResourceLocation.parse(GsonHelper.getAsString(o, "icon", "minecraft:book")),
                        GsonHelper.getAsInt(o, "order", 100)));
            }
        } catch (Exception e) {
            Spacebreaker.LOGGER.warn("Bad guide chapters file: {}", e.toString());
        }
    }

    private static Entry parseEntry(ResourceLocation id, JsonObject json) {
        List<Element> elements = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "pages", new JsonArray())) {
            JsonObject o = el.getAsJsonObject();
            String text = GsonHelper.getAsString(o, "text", "");
            if (o.has("heading")) {
                elements.add(new Element("heading", GsonHelper.getAsString(o, "heading"), List.of(), null, 0, 0));
            } else if (o.has("items")) {
                List<ResourceLocation> items = new ArrayList<>();
                for (JsonElement i : o.getAsJsonArray("items")) {
                    items.add(ResourceLocation.parse(i.getAsString()));
                }
                elements.add(new Element("items", text, items, null, 0, 0));
            } else if (o.has("image")) {
                elements.add(new Element("image", text, List.of(), ResourceLocation.parse(GsonHelper.getAsString(o, "image")),
                        GsonHelper.getAsInt(o, "width", 112), GsonHelper.getAsInt(o, "height", 64)));
            } else if (o.has("recipe")) {
                elements.add(new Element("recipe", text, List.of(ResourceLocation.parse(GsonHelper.getAsString(o, "recipe"))), null, 0, 0));
            } else if (GsonHelper.getAsBoolean(o, "page_break", false)) {
                elements.add(new Element("page_break", "", List.of(), null, 0, 0));
            } else {
                elements.add(new Element("text", text, List.of(), null, 0, 0));
            }
        }
        Optional<ResourceLocation> tech = json.has("requires_tech") ? Optional.of(ResourceLocation.parse(GsonHelper.getAsString(json, "requires_tech")))
                : Optional.empty();
        return new Entry(id, GsonHelper.getAsString(json, "chapter", "misc"), GsonHelper.getAsString(json, "title", id.getPath()),
                ResourceLocation.parse(GsonHelper.getAsString(json, "icon", "minecraft:book")), GsonHelper.getAsInt(json, "order", 100),
                tech, List.copyOf(elements));
    }

    @Override
    protected void apply(Loaded loaded, ResourceManager manager, ProfilerFiller profiler) {
        this.data = loaded;
    }
}
