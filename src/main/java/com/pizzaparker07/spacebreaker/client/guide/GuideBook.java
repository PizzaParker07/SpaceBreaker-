package com.pizzaparker07.spacebreaker.client.guide;

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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The field guide's content, loaded from resource packs (any namespace may add to it):
 * <ul>
 *   <li>{@code assets/<ns>/guide/book.json}: categories in order, each with a colour, an icon and its chapters,
 *       plus {@code "machines": {"<recipe type>": "<item>"}} for the machine shown on recipe pages.</li>
 *   <li>{@code assets/<ns>/guide/<lang>/book.json}: {@code {"title": "...", "categories": {"<id>": "name"}}}.</li>
 *   <li>{@code assets/<ns>/guide/<lang>/<chapter>.md}: one chapter in Markdown (see {@link GuideMarkdown}).
 *       Missing languages fall back to {@code en_us}.</li>
 * </ul>
 */
public final class GuideBook extends SimplePreparableReloadListener<GuideBook.Data> {
    public static final GuideBook INSTANCE = new GuideBook();
    public static final String FALLBACK = "en_us";

    public record Category(String id, int color, ResourceLocation icon, List<ResourceLocation> chapters) {
    }

    public record LangBook(String title, String subtitle, String intro, Map<String, String> categories) {
    }

    public record Data(List<Category> categories, Map<String, LangBook> langs, Map<String, Map<ResourceLocation, String>> sources,
                       Map<ResourceLocation, ResourceLocation> machines) {
    }

    private Data data = new Data(List.of(), Map.of(), Map.of(), Map.of());
    private final Map<String, Map<ResourceLocation, GuideMarkdown.Chapter>> parsed = new HashMap<>();
    private final Map<String, Map<ResourceLocation, ResourceLocation>> itemIndex = new HashMap<>();

    private GuideBook() {
    }

    // ------------------------------------------------------------------ queries

    public static String lang() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public List<Category> categories() {
        return data.categories();
    }

    public String title() {
        LangBook b = data.langs().getOrDefault(lang(), data.langs().get(FALLBACK));
        return b == null ? "Field Guide" : b.title();
    }

    public String subtitle() {
        LangBook b = data.langs().getOrDefault(lang(), data.langs().get(FALLBACK));
        return b == null ? "" : b.subtitle();
    }

    public String intro() {
        LangBook b = data.langs().getOrDefault(lang(), data.langs().get(FALLBACK));
        return b == null ? "" : b.intro();
    }

    public String categoryName(String id) {
        LangBook b = data.langs().get(lang());
        if (b != null && b.categories().containsKey(id)) {
            return b.categories().get(id);
        }
        LangBook en = data.langs().get(FALLBACK);
        return en != null ? en.categories().getOrDefault(id, id) : id;
    }

    public @Nullable Category categoryOf(ResourceLocation chapter) {
        for (Category c : data.categories()) {
            if (c.chapters().contains(chapter)) {
                return c;
            }
        }
        return null;
    }

    /** Every chapter in reading order. */
    public List<ResourceLocation> order() {
        List<ResourceLocation> out = new ArrayList<>();
        data.categories().forEach(c -> out.addAll(c.chapters()));
        return out;
    }

    /** A chapter in the current language (falling back to English), or null if it exists in neither. */
    public @Nullable GuideMarkdown.Chapter chapter(ResourceLocation id) {
        return chapter(id, lang());
    }

    public @Nullable GuideMarkdown.Chapter chapter(ResourceLocation id, String lang) {
        GuideMarkdown.Chapter c = parse(lang, id);
        return c != null ? c : parse(FALLBACK, id);
    }

    private @Nullable GuideMarkdown.Chapter parse(String lang, ResourceLocation id) {
        Map<ResourceLocation, GuideMarkdown.Chapter> cache = parsed.computeIfAbsent(lang, k -> new HashMap<>());
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        String src = data.sources().getOrDefault(lang, Map.of()).get(id);
        GuideMarkdown.Chapter c = src == null ? null : GuideMarkdown.parse(id, src);
        cache.put(id, c);
        return c;
    }

    /** The chapter that describes an item (listed in its front matter {@code items:}), if any. */
    public @Nullable ResourceLocation chapterForItem(ResourceLocation item) {
        Map<ResourceLocation, ResourceLocation> index = itemIndex.computeIfAbsent(lang(), l -> {
            Map<ResourceLocation, ResourceLocation> m = new HashMap<>();
            for (ResourceLocation ch : order()) {
                GuideMarkdown.Chapter c = chapter(ch, l);
                if (c != null) {
                    c.items().forEach(i -> m.putIfAbsent(i, ch));
                }
            }
            return m;
        });
        return index.get(item);
    }

    public @Nullable ResourceLocation machineFor(ResourceLocation recipeType) {
        return data.machines().get(recipeType);
    }

    /** Raw sources (for the in-game check and tests). */
    public Map<String, Map<ResourceLocation, String>> sources() {
        return data.sources();
    }

    // ------------------------------------------------------------------ loading

    @Override
    protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
        return load(manager);
    }

    public static Data load(ResourceManager manager) {
        Map<String, Category> categories = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> machines = new HashMap<>();
        Map<String, LangBook> langs = new HashMap<>();
        Map<String, Map<ResourceLocation, String>> sources = new HashMap<>();
        List<String> namespaces = new ArrayList<>(manager.getNamespaces());
        namespaces.sort((a, b) -> a.equals(Spacebreaker.MOD_ID) ? -1 : b.equals(Spacebreaker.MOD_ID) ? 1 : a.compareTo(b));
        for (String ns : namespaces) {
            for (Resource res : manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(ns, "guide/book.json"))) {
                try (Reader r = res.openAsReader()) {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    for (JsonElement el : GsonHelper.getAsJsonArray(json, "categories", new com.google.gson.JsonArray())) {
                        JsonObject o = el.getAsJsonObject();
                        String id = GsonHelper.getAsString(o, "id");
                        List<ResourceLocation> chapters = new ArrayList<>();
                        for (JsonElement ch : GsonHelper.getAsJsonArray(o, "chapters", new com.google.gson.JsonArray())) {
                            String s = ch.getAsString();
                            chapters.add(s.contains(":") ? ResourceLocation.parse(s) : ResourceLocation.fromNamespaceAndPath(ns, s));
                        }
                        Category old = categories.get(id);
                        if (old != null) {
                            List<ResourceLocation> merged = new ArrayList<>(old.chapters());
                            chapters.stream().filter(c -> !merged.contains(c)).forEach(merged::add);
                            categories.put(id, new Category(id, old.color(), old.icon(), merged));
                        } else {
                            int color = Integer.parseInt(GsonHelper.getAsString(o, "color", "#5A4A3A").replace("#", ""), 16);
                            categories.put(id, new Category(id, color, ResourceLocation.parse(GsonHelper.getAsString(o, "icon", "minecraft:book")), chapters));
                        }
                    }
                    JsonObject m = GsonHelper.getAsJsonObject(json, "machines", new JsonObject());
                    m.entrySet().forEach(e -> machines.put(ResourceLocation.parse(e.getKey()), ResourceLocation.parse(e.getValue().getAsString())));
                } catch (Exception e) {
                    Spacebreaker.LOGGER.warn("Bad guide book.json in {}: {}", ns, e.toString());
                }
            }
        }
        manager.listResources("guide", rl -> rl.getPath().endsWith(".md") || rl.getPath().endsWith("/book.json")).forEach((rl, res) -> {
            String[] parts = rl.getPath().split("/");
            if (parts.length != 3) {
                return; // guide/<lang>/<file>
            }
            String lang = parts[1];
            try (BufferedReader r = res.openAsReader()) {
                if (parts[2].equals("book.json")) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    Map<String, String> names = new HashMap<>();
                    GsonHelper.getAsJsonObject(o, "categories", new JsonObject()).entrySet()
                            .forEach(e -> names.put(e.getKey(), e.getValue().getAsString()));
                    LangBook old = langs.get(lang);
                    if (old != null) {
                        names.putAll(old.categories());
                    }
                    langs.put(lang, new LangBook(old != null ? old.title() : GsonHelper.getAsString(o, "title", "Field Guide"),
                            old != null && !old.subtitle().isEmpty() ? old.subtitle() : GsonHelper.getAsString(o, "subtitle", ""),
                            old != null && !old.intro().isEmpty() ? old.intro() : GsonHelper.getAsString(o, "intro", ""), names));
                } else {
                    String name = parts[2].substring(0, parts[2].length() - 3);
                    sources.computeIfAbsent(lang, k -> new HashMap<>())
                            .put(ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), name), r.lines().collect(Collectors.joining("\n")));
                }
            } catch (Exception e) {
                Spacebreaker.LOGGER.warn("Bad guide file {}: {}", rl, e.toString());
            }
        });
        return new Data(List.copyOf(categories.values()), langs, sources, machines);
    }

    @Override
    protected void apply(Data loaded, ResourceManager manager, ProfilerFiller profiler) {
        this.data = loaded;
        parsed.clear();
        itemIndex.clear();
        GuideStructures.clear();
    }
}
