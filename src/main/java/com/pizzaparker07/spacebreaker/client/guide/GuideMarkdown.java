package com.pizzaparker07.spacebreaker.client.guide;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Markdown dialect of the field guide (side-neutral: no client classes, so GameTests can validate chapters).
 *
 * <pre>
 * ---
 * title: First Flight
 * icon: spacebreaker:rocket
 * items: [spacebreaker:rocket, spacebreaker:launch_pad]      # clicking these items anywhere opens this chapter
 * research: spacebreaker:basic_rocketry                       # shows "Research needed: ..." while unknown (never locks)
 * ---
 * # Heading 1 / ## Heading 2 / ### Heading 3
 * Paragraph text with **bold**, *italic*, `code`, {color #D8673A}coloured{/color} or {color gold}named{/color} text,
 * {item spacebreaker:rocket} (name, hover = tooltip, click = its chapter), {item spacebreaker:rocket the rocket},
 * {key key.jump} (the player's real key), {tech spacebreaker:basic_rocketry} and [chapter links](spacebreaker:first_flight).
 * - bullets (two spaces per indent level) · 1. numbered lists
 * > [!TIP] blue hint box        > [!WARNING] orange warning box        > [!NOTE] neutral box
 * | tables | with | cells |  (the |---| line marks the header)
 * {page}                         page break
 * {recipe ns:id}                 a recipe from the game (crafting, furnace, smithing, our machines)
 * {items ns:a ns:b ...}          a row of item slots
 * {entity ns:id}                 a turning entity model
 * {structure ns:name}            a turning 3D structure (assets/ns/guide/structures/name.nbt)
 * {image ns:textures/... w h}    a picture
 * </pre>
 */
public final class GuideMarkdown {
    public static final String LINK_ITEM = "item:", LINK_CHAPTER = "chapter:";

    public sealed interface Block permits Heading, Para, ListItem, Callout, Table, PageBreak, Recipe, Items, EntityView, StructureView, Image {
    }

    public record Heading(int level, Component text) implements Block {
    }

    public record Para(Component text) implements Block {
    }

    public record ListItem(Component marker, Component text, int indent) implements Block {
    }

    /** kind: tip, warning, note */
    public record Callout(String kind, List<Component> paras) implements Block {
    }

    public record Table(List<List<Component>> rows, boolean header) implements Block {
    }

    public record PageBreak() implements Block {
    }

    public record Recipe(ResourceLocation id) implements Block {
    }

    public record Items(List<ResourceLocation> ids) implements Block {
    }

    public record EntityView(ResourceLocation id) implements Block {
    }

    public record StructureView(ResourceLocation id) implements Block {
    }

    public record Image(ResourceLocation texture, int width, int height) implements Block {
    }

    /** Everything a chapter points at, for validation. */
    public record References(List<ResourceLocation> items, List<ResourceLocation> recipes, List<ResourceLocation> entities,
                             List<ResourceLocation> structures, List<ResourceLocation> chapters, List<ResourceLocation> techs,
                             List<ResourceLocation> images, List<String> keys, List<String> problems) {
        static References empty() {
            return new References(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    public record Chapter(ResourceLocation id, String title, ResourceLocation icon, List<ResourceLocation> items,
                          Optional<ResourceLocation> research, List<Block> blocks, References refs, String searchText) {
    }

    // ================================================================== parsing

    public static Chapter parse(ResourceLocation id, String source) {
        References refs = References.empty();
        List<String> lines = new ArrayList<>(List.of(source.replace("\r", "").split("\n", -1)));
        String title = id.getPath();
        ResourceLocation icon = ResourceLocation.withDefaultNamespace("book");
        List<ResourceLocation> items = new ArrayList<>();
        ResourceLocation research = null;
        int start = 0;
        if (!lines.isEmpty() && lines.get(0).trim().equals("---")) {
            int end = 1;
            while (end < lines.size() && !lines.get(end).trim().equals("---")) {
                String l = lines.get(end);
                int c = l.indexOf(':');
                if (c > 0) {
                    String key = l.substring(0, c).trim();
                    String value = l.substring(c + 1).trim();
                    switch (key) {
                        case "title" -> title = value;
                        case "icon" -> icon = rl(value, refs);
                        case "research" -> research = rl(value, refs);
                        case "items" -> {
                            for (String s : value.replace("[", "").replace("]", "").split(",")) {
                                if (!s.isBlank()) {
                                    ResourceLocation r = rl(s.trim(), refs);
                                    items.add(r);
                                    refs.items().add(r);
                                }
                            }
                        }
                        default -> refs.problems().add("unknown front matter key " + key);
                    }
                }
                end++;
            }
            start = end + 1;
        }
        refs.items().add(icon);
        if (research != null) {
            refs.techs().add(research);
        }
        List<Block> blocks = new ArrayList<>();
        StringBuilder search = new StringBuilder(title.toLowerCase(Locale.ROOT));
        StringBuilder para = new StringBuilder();
        int number = 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.trim();
            if (line.isEmpty()) {
                flush(para, blocks, refs);
                number = 0;
                continue;
            }
            search.append(' ').append(line.toLowerCase(Locale.ROOT));
            if (line.startsWith("#")) {
                flush(para, blocks, refs);
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }
                blocks.add(new Heading(Math.min(3, level), inline(line.substring(level).trim(), refs)));
            } else if (line.startsWith(">")) {
                flush(para, blocks, refs);
                String kind = "note";
                List<Component> paras = new ArrayList<>();
                StringBuilder cur = new StringBuilder();
                int j = i;
                for (; j < lines.size() && lines.get(j).trim().startsWith(">"); j++) {
                    String l = lines.get(j).trim().substring(1).trim();
                    if (j == i && l.startsWith("[!")) {
                        int close = l.indexOf(']');
                        kind = l.substring(2, Math.max(2, close)).toLowerCase(Locale.ROOT);
                        l = l.substring(close + 1).trim();
                    }
                    if (l.isEmpty()) {
                        if (!cur.isEmpty()) {
                            paras.add(inline(cur.toString(), refs));
                            cur.setLength(0);
                        }
                    } else {
                        if (!cur.isEmpty()) {
                            cur.append(' ');
                        }
                        cur.append(l);
                        search.append(' ').append(l.toLowerCase(Locale.ROOT));
                    }
                }
                if (!cur.isEmpty()) {
                    paras.add(inline(cur.toString(), refs));
                }
                blocks.add(new Callout(kind, paras));
                i = j - 1;
            } else if (line.startsWith("|")) {
                flush(para, blocks, refs);
                List<List<Component>> rows = new ArrayList<>();
                boolean header = false;
                int j = i;
                for (; j < lines.size() && lines.get(j).trim().startsWith("|"); j++) {
                    String l = lines.get(j).trim();
                    if (l.matches("\\|[\\s:|-]+\\|?")) {
                        header = rows.size() == 1;
                        continue;
                    }
                    List<Component> cells = new ArrayList<>();
                    String body = l.substring(1, l.endsWith("|") ? l.length() - 1 : l.length());
                    for (String cell : body.split("\\|", -1)) {
                        cells.add(inline(cell.trim(), refs));
                    }
                    rows.add(cells);
                }
                blocks.add(new Table(rows, header));
                i = j - 1;
            } else if (line.matches("[-*] .*") || line.matches("\\d+[.)] .*")) {
                flush(para, blocks, refs);
                int indent = (raw.length() - raw.stripLeading().length()) / 2;
                boolean numbered = Character.isDigit(line.charAt(0));
                String text = line.substring(line.indexOf(' ') + 1);
                Component marker = numbered ? Component.literal((++number) + ".") : Component.literal(indent == 0 ? "•" : "◦");
                blocks.add(new ListItem(marker, inline(text, refs), indent));
            } else if (line.startsWith("{") && line.endsWith("}") && isBlockTag(line)) {
                flush(para, blocks, refs);
                Block b = blockTag(line.substring(1, line.length() - 1).trim(), refs);
                if (b != null) {
                    blocks.add(b);
                }
            } else {
                if (!para.isEmpty()) {
                    para.append(' ');
                }
                para.append(line);
            }
        }
        flush(para, blocks, refs);
        return new Chapter(id, title, icon, List.copyOf(items), Optional.ofNullable(research), List.copyOf(blocks), refs, search.toString());
    }

    private static boolean isBlockTag(String line) {
        String inner = line.substring(1, line.length() - 1).trim();
        String head = inner.split("\\s+", 2)[0];
        return switch (head) {
            case "page", "recipe", "items", "entity", "structure", "image" -> inner.indexOf('}') < 0;
            default -> false;
        };
    }

    private static @Nullable Block blockTag(String tag, References refs) {
        String[] parts = tag.split("\\s+");
        switch (parts[0]) {
            case "page":
                return new PageBreak();
            case "recipe":
                if (parts.length > 1) {
                    ResourceLocation r = rl(parts[1], refs);
                    refs.recipes().add(r);
                    return new Recipe(r);
                }
                break;
            case "items": {
                List<ResourceLocation> ids = new ArrayList<>();
                for (int k = 1; k < parts.length; k++) {
                    ResourceLocation r = rl(parts[k], refs);
                    ids.add(r);
                    refs.items().add(r);
                }
                return new Items(ids);
            }
            case "entity":
                if (parts.length > 1) {
                    ResourceLocation r = rl(parts[1], refs);
                    refs.entities().add(r);
                    return new EntityView(r);
                }
                break;
            case "structure":
                if (parts.length > 1) {
                    ResourceLocation r = rl(parts[1], refs);
                    refs.structures().add(r);
                    return new StructureView(r);
                }
                break;
            case "image":
                if (parts.length > 1) {
                    ResourceLocation r = rl(parts[1], refs);
                    refs.images().add(r);
                    int w = parts.length > 2 ? Integer.parseInt(parts[2]) : 112;
                    int h = parts.length > 3 ? Integer.parseInt(parts[3]) : 64;
                    return new Image(r, w, h);
                }
                break;
            default:
                break;
        }
        refs.problems().add("bad block tag {" + tag + "}");
        return null;
    }

    private static void flush(StringBuilder para, List<Block> blocks, References refs) {
        if (!para.isEmpty()) {
            blocks.add(new Para(inline(para.toString(), refs)));
            para.setLength(0);
        }
    }

    private static ResourceLocation rl(String s, References refs) {
        ResourceLocation r = ResourceLocation.tryParse(s.contains(":") ? s : "spacebreaker:" + s);
        if (r == null) {
            refs.problems().add("bad id " + s);
            return ResourceLocation.withDefaultNamespace("air");
        }
        return r;
    }

    // ================================================================== inline

    public static final int LINK_COLOR = 0x1E5A9A, CODE_COLOR = 0x6A3A8A, KEY_COLOR = 0x8A5A10, TECH_COLOR = 0xB0561E;

    /** Parses inline markup into a styled component. */
    public static MutableComponent inline(String text, References refs) {
        MutableComponent out = Component.empty();
        parseInline(text, out, Style.EMPTY, refs);
        return out;
    }

    private static void parseInline(String s, MutableComponent out, Style base, References refs) {
        StringBuilder buf = new StringBuilder();
        Style style = base;
        boolean bold = false, italic = false;
        java.util.ArrayDeque<Style> colors = new java.util.ArrayDeque<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                buf.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (s.startsWith("**", i)) {
                emit(buf, out, style);
                bold = !bold;
                style = style.withBold(bold);
                i += 2;
                continue;
            }
            if (c == '*') {
                emit(buf, out, style);
                italic = !italic;
                style = style.withItalic(italic);
                i++;
                continue;
            }
            if (c == '`') {
                int end = s.indexOf('`', i + 1);
                if (end > 0) {
                    emit(buf, out, style);
                    out.append(Component.literal(s.substring(i + 1, end)).withStyle(style.withColor(CODE_COLOR)));
                    i = end + 1;
                    continue;
                }
            }
            if (c == '[') {
                int close = s.indexOf("](", i);
                int end = close < 0 ? -1 : s.indexOf(')', close);
                if (close > 0 && end > 0) {
                    emit(buf, out, style);
                    String label = s.substring(i + 1, close);
                    ResourceLocation target = rl(s.substring(close + 2, end), refs);
                    refs.chapters().add(target);
                    out.append(Component.literal(label).withStyle(style.withColor(LINK_COLOR).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, LINK_CHAPTER + target))));
                    i = end + 1;
                    continue;
                }
            }
            if (c == '{') {
                int end = s.indexOf('}', i);
                if (end > 0) {
                    String tag = s.substring(i + 1, end).trim();
                    String[] parts = tag.split("\\s+", 3);
                    switch (parts[0]) {
                        case "item" -> {
                            emit(buf, out, style);
                            ResourceLocation id = rl(parts.length > 1 ? parts[1] : "air", refs);
                            refs.items().add(id);
                            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(net.minecraft.world.item.Items.AIR);
                            Component name = parts.length > 2 ? Component.literal(parts[2]) : Component.translatable(item.getDescriptionId());
                            out.append(name.copy().withStyle(style.withColor(LINK_COLOR)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(new ItemStack(item))))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, LINK_ITEM + id))));
                            i = end + 1;
                            continue;
                        }
                        case "key" -> {
                            emit(buf, out, style);
                            String key = parts.length > 1 ? parts[1] : "key.jump";
                            refs.keys().add(key);
                            out.append(Component.literal("[").withStyle(style.withColor(KEY_COLOR).withBold(true))
                                    .append(Component.keybind(key))
                                    .append(Component.literal("]")));
                            i = end + 1;
                            continue;
                        }
                        case "tech" -> {
                            emit(buf, out, style);
                            ResourceLocation id = rl(parts.length > 1 ? parts[1] : "air", refs);
                            refs.techs().add(id);
                            out.append(Component.translatable("tech." + id.getNamespace() + "." + id.getPath())
                                    .withStyle(style.withColor(TECH_COLOR)));
                            i = end + 1;
                            continue;
                        }
                        case "color" -> {
                            emit(buf, out, style);
                            colors.push(style);
                            style = style.withColor(color(parts.length > 1 ? parts[1] : "black"));
                            i = end + 1;
                            continue;
                        }
                        case "/color" -> {
                            emit(buf, out, style);
                            style = colors.isEmpty() ? base.withBold(bold).withItalic(italic) : colors.pop();
                            i = end + 1;
                            continue;
                        }
                        default -> {
                        }
                    }
                }
            }
            buf.append(c);
            i++;
        }
        emit(buf, out, style);
    }

    private static TextColor color(String name) {
        if (name.startsWith("#")) {
            TextColor c = TextColor.parseColor(name).result().orElse(null);
            if (c != null) {
                return c;
            }
        }
        ChatFormatting f = ChatFormatting.getByName(name);
        if (f != null && f.isColor()) {
            return TextColor.fromLegacyFormat(f);
        }
        return TextColor.fromRgb(0x3B2A1A);
    }

    private static void emit(StringBuilder buf, MutableComponent out, Style style) {
        if (!buf.isEmpty()) {
            out.append(Component.literal(buf.toString()).withStyle(style));
            buf.setLength(0);
        }
    }

    private GuideMarkdown() {
    }
}
