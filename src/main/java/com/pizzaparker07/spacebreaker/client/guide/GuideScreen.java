package com.pizzaparker07.spacebreaker.client.guide;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.ResearchClientData;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The illustrated field guide: a two-page book. The index spread lists chapters (left) and the entries of the selected
 * chapter or the search results (right); an entry flows over as many pages as it needs. Entries tied to a technology
 * stay locked until it is known.
 */
public class GuideScreen extends Screen {
    private static final ResourceLocation BOOK = Spacebreaker.id("textures/gui/guide/book.png");
    private static final ResourceLocation SLOT = Spacebreaker.id("guide/slot");
    private static final ResourceLocation RECIPE = Spacebreaker.id("guide/recipe");
    private static final ResourceLocation LOCK = Spacebreaker.id("guide/lock");
    private static final ResourceLocation BACK = Spacebreaker.id("guide/back");
    public static final int BOOK_W = 300, BOOK_H = 188, TEX_W = 512, TEX_H = 256;
    private static final int PAGE_W = 118, PAGE_TOP = 16, PAGE_BOTTOM = 164;
    private static final int LEFT_X = 22, RIGHT_X = 160;
    private static final int CHAPTER_Y = 30, CHAPTER_ROW = 15;
    private static final int INK = 0x3B2A1A, INK_SOFT = 0x6E5A44, INK_LINK = 0x1E4E7A;

    /** Remembered between openings (like vanilla's lectern page). */
    private static @Nullable ResourceLocation lastEntry;
    private static String lastChapter = "";

    private int bx, by;
    private EditBox search;
    private PageButton next, prev;
    private final List<GuideData.Entry> entries = new ArrayList<>();
    private String chapter = lastChapter;
    private @Nullable GuideData.Entry open;
    private List<List<Laid>> pages = List.of();
    private int spread;
    private int listPage;

    /** One element placed on a page. */
    private record Laid(GuideData.Element el, int y, int h, List<FormattedCharSequence> lines) {
    }

    public GuideScreen() {
        super(Component.translatable("item.spacebreaker.guide_book"));
    }

    @Override
    protected void init() {
        bx = (width - BOOK_W) / 2;
        by = (height - BOOK_H) / 2;
        entries.clear();
        entries.addAll(GuideData.INSTANCE.entries());
        if (chapter.isEmpty() && !GuideData.INSTANCE.chapters().isEmpty()) {
            chapter = GuideData.INSTANCE.chapters().get(0).id();
        }
        String text = search == null ? "" : search.getValue();
        search = new EditBox(font, bx + LEFT_X + 2, by + PAGE_TOP + 12, PAGE_W - 4, 12, Component.translatable("gui.spacebreaker.guide.search"));
        search.setHint(Component.translatable("gui.spacebreaker.guide.search").withStyle(ChatFormatting.GRAY));
        search.setBordered(true);
        search.setMaxLength(40);
        search.setValue(text);
        search.setResponder(s -> listPage = 0);
        addRenderableWidget(search);
        next = addRenderableWidget(new PageButton(bx + BOOK_W - 44, by + BOOK_H - 22, true, b -> turn(1), true));
        prev = addRenderableWidget(new PageButton(bx + 21, by + BOOK_H - 22, false, b -> turn(-1), true));
        if (open == null && lastEntry != null) {
            entries.stream().filter(e -> e.id().equals(lastEntry) && unlocked(e)).findFirst().ifPresent(this::openEntry);
        } else if (open != null) {
            openEntry(open);
        }
        updateWidgets();
    }

    private void updateWidgets() {
        search.visible = open == null;
        if (open != null) {
            prev.visible = true;
            next.visible = (spread + 1) * 2 < pages.size();
        } else {
            prev.visible = listPage > 0;
            next.visible = (listPage + 1) * listCapacity() < visibleEntries().size();
        }
    }

    private void turn(int dir) {
        if (open != null) {
            if (dir < 0 && spread == 0) {
                closeEntry();
                return;
            }
            spread = Mth.clamp(spread + dir, 0, Math.max(0, (pages.size() - 1) / 2));
        } else {
            listPage = Math.max(0, listPage + dir);
        }
        updateWidgets();
    }

    // ------------------------------------------------------------------ navigation

    public void openEntry(GuideData.Entry entry) {
        open = entry;
        lastEntry = entry.id();
        spread = 0;
        pages = paginate(entry);
        if (search != null) {
            search.setFocused(false);
        }
        if (next != null) {
            updateWidgets();
        }
    }

    private void closeEntry() {
        open = null;
        lastEntry = null;
        updateWidgets();
    }

    /** For the showcase: open the first entry of a chapter, or an entry by id. */
    public void showEntry(ResourceLocation id, int spreadIndex) {
        entries.stream().filter(e -> e.id().equals(id)).findFirst().ifPresent(e -> {
            chapter = e.chapter();
            openEntry(e);
            spread = Mth.clamp(spreadIndex, 0, Math.max(0, (pages.size() - 1) / 2));
            updateWidgets();
        });
    }

    public void showChapter(String id) {
        chapter = id;
        open = null;
        listPage = 0;
        updateWidgets();
    }

    public void setSearch(String text) {
        open = null;
        lastEntry = null;
        search.setValue(text);
        updateWidgets();
    }

    private static boolean unlocked(GuideData.Entry e) {
        return e.requiresTech().map(ResearchClientData::isKnownOnClient).orElse(true)
                || Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
    }

    private List<GuideData.Entry> visibleEntries() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            return entries.stream().filter(e -> e.searchText().contains(q)).toList();
        }
        return entries.stream().filter(e -> e.chapter().equals(chapter)).toList();
    }

    private int listCapacity() {
        return (PAGE_BOTTOM - PAGE_TOP - 16) / 18;
    }

    // ------------------------------------------------------------------ pagination

    private List<List<Laid>> paginate(GuideData.Entry entry) {
        List<List<Laid>> out = new ArrayList<>();
        List<Laid> page = new ArrayList<>();
        int y = 22; // first page starts below the title
        int max = PAGE_BOTTOM - PAGE_TOP;
        for (GuideData.Element el : entry.elements()) {
            switch (el.type()) {
                case "page_break" -> {
                    out.add(page);
                    page = new ArrayList<>();
                    y = 0;
                }
                case "text", "heading" -> {
                    Component c = el.type().equals("heading") ? Component.literal(el.text()).withStyle(ChatFormatting.BOLD)
                            : Component.literal(el.text());
                    List<FormattedCharSequence> lines = new ArrayList<>(font.split(c, PAGE_W));
                    while (!lines.isEmpty()) {
                        int fit = Math.max(0, (max - y) / 9);
                        if (fit == 0) {
                            out.add(page);
                            page = new ArrayList<>();
                            y = 0;
                            continue;
                        }
                        List<FormattedCharSequence> part = new ArrayList<>(lines.subList(0, Math.min(fit, lines.size())));
                        lines = new ArrayList<>(lines.subList(part.size(), lines.size()));
                        page.add(new Laid(el, y, part.size() * 9, part));
                        y += part.size() * 9 + (el.type().equals("heading") ? 3 : 5);
                    }
                }
                default -> {
                    List<FormattedCharSequence> caption = el.text().isEmpty() ? List.of()
                            : font.split(Component.literal(el.text()).withStyle(ChatFormatting.ITALIC), PAGE_W);
                    int h = switch (el.type()) {
                        case "items" -> ((el.items().size() + 5) / 6) * 20;
                        case "image" -> Math.min(el.height() * PAGE_W / Math.max(1, el.width()), el.height()) + 2;
                        case "recipe" -> 60;
                        default -> 0;
                    } + caption.size() * 9;
                    if (y + h > max && !page.isEmpty()) {
                        out.add(page);
                        page = new ArrayList<>();
                        y = 0;
                    }
                    page.add(new Laid(el, y, h, caption));
                    y += h + 6;
                }
            }
        }
        if (!page.isEmpty() || out.isEmpty()) {
            out.add(page);
        }
        return out;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(BOOK, bx, by, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ItemStack hoverStack = ItemStack.EMPTY;
        List<Component> hoverText = null;
        if (open == null) {
            hoverText = renderIndex(g, mouseX, mouseY);
        } else {
            for (int side = 0; side < 2; side++) {
                int idx = spread * 2 + side;
                if (idx >= pages.size()) {
                    break;
                }
                int px = bx + (side == 0 ? LEFT_X : RIGHT_X), py = by + PAGE_TOP;
                if (idx == 0) {
                    renderTitle(g, open, px, py);
                }
                for (Laid l : pages.get(idx)) {
                    ItemStack s = renderElement(g, l, px, py + l.y(), mouseX, mouseY);
                    if (!s.isEmpty()) {
                        hoverStack = s;
                    }
                }
                Component num = Component.literal(String.valueOf(idx + 1));
                g.drawString(font, num, px + (PAGE_W - font.width(num)) / 2, by + BOOK_H - 18, INK_SOFT, false);
            }
            g.blitSprite(BACK, bx + LEFT_X - 4, by + 4, 16, 10);
            if (inside(mouseX, mouseY, bx + LEFT_X - 4, by + 4, 16, 10)) {
                hoverText = List.of(Component.translatable("gui.spacebreaker.guide.back"));
            }
        }
        if (!hoverStack.isEmpty()) {
            g.renderTooltip(font, hoverStack, mouseX, mouseY);
        } else if (hoverText != null) {
            g.renderComponentTooltip(font, hoverText, mouseX, mouseY);
        }
    }

    private void renderTitle(GuiGraphics g, GuideData.Entry e, int px, int py) {
        renderIcon(g, e.icon(), px, py);
        List<FormattedCharSequence> lines = font.split(Component.literal(e.title()).withStyle(ChatFormatting.BOLD), PAGE_W - 20);
        int ty = py + (lines.size() > 1 ? 0 : 4);
        for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
            g.drawString(font, line, px + 20, ty, INK, false);
            ty += 9;
        }
        g.fill(px, py + 18, px + PAGE_W, py + 19, 0x40000000 | INK_SOFT);
    }

    private @Nullable List<Component> renderIndex(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> hover = null;
        int lx = bx + LEFT_X, rx = bx + RIGHT_X, py = by + PAGE_TOP;
        Component title = Component.translatable("gui.spacebreaker.guide.title").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, lx + (PAGE_W - font.width(title)) / 2, py + 1, INK, false);
        int y = py + CHAPTER_Y;
        for (GuideData.Chapter c : GuideData.INSTANCE.chapters()) {
            if (y > by + BOOK_H - 26) {
                break;
            }
            boolean sel = c.id().equals(chapter) && search.getValue().isBlank();
            boolean hot = inside(mouseX, mouseY, lx, y - 1, PAGE_W, CHAPTER_ROW);
            if (sel) {
                g.fill(lx - 2, y - 1, lx + PAGE_W, y + CHAPTER_ROW - 1, 0x30000000 | INK_SOFT);
            }
            renderIcon(g, c.icon(), lx, y - 1);
            Component name = Component.translatable("guide.spacebreaker.chapter." + c.id());
            g.drawString(font, name, lx + 20, y + 3, hot ? INK_LINK : INK, false);
            y += CHAPTER_ROW;
        }
        // right page: entries
        boolean searching = !search.getValue().isBlank();
        Component header = searching ? Component.translatable("gui.spacebreaker.guide.results")
                : Component.translatable("guide.spacebreaker.chapter." + chapter);
        g.drawString(font, header.copy().withStyle(ChatFormatting.BOLD), rx + (PAGE_W - font.width(header) - 6) / 2, py + 1, INK, false);
        List<GuideData.Entry> list = visibleEntries();
        int cap = listCapacity();
        int start = listPage * cap;
        y = py + 16;
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("gui.spacebreaker.guide.none"), rx, y + 4, INK_SOFT, false);
        }
        for (int i = start; i < Math.min(list.size(), start + cap); i++) {
            GuideData.Entry e = list.get(i);
            boolean unlocked = unlocked(e);
            boolean hot = inside(mouseX, mouseY, rx, y - 1, PAGE_W, 18);
            if (unlocked) {
                renderIcon(g, e.icon(), rx, y);
            } else {
                g.blitSprite(LOCK, rx + 3, y + 2, 10, 12);
            }
            String t = unlocked ? e.title() : "???";
            List<FormattedCharSequence> lines = font.split(Component.literal(t), PAGE_W - 22);
            int color = !unlocked ? INK_SOFT : hot ? INK_LINK : INK;
            if (lines.size() == 1) {
                g.drawString(font, lines.get(0), rx + 20, y + 5, color, false);
            } else {
                g.drawString(font, lines.get(0), rx + 20, y, color, false);
                g.drawString(font, lines.get(1), rx + 20, y + 9, color, false);
            }
            if (hot && !unlocked && e.requiresTech().isPresent()) {
                hover = List.of(Component.translatable("gui.spacebreaker.guide.locked",
                        Component.translatable(TechNode.nameKey(e.requiresTech().get()))));
            }
            y += 18;
        }
        return hover;
    }

    private void renderIcon(GuiGraphics g, ResourceLocation item, int x, int y) {
        var it = BuiltInRegistries.ITEM.getOptional(item).orElse(Items.BOOK);
        g.renderFakeItem(new ItemStack(it), x, y);
    }

    private ItemStack renderElement(GuiGraphics g, Laid l, int x, int y, int mouseX, int mouseY) {
        GuideData.Element el = l.el();
        ItemStack hovered = ItemStack.EMPTY;
        switch (el.type()) {
            case "text", "heading" -> {
                int ly = y;
                for (FormattedCharSequence line : l.lines()) {
                    g.drawString(font, line, x, ly, INK, false);
                    ly += 9;
                }
            }
            case "items" -> {
                int n = el.items().size();
                for (int i = 0; i < n; i++) {
                    int row = i / 6, col = i % 6;
                    int inRow = Math.min(6, n - row * 6);
                    int sx = x + (PAGE_W - inRow * 20) / 2 + col * 20;
                    int sy = y + row * 20;
                    g.blitSprite(SLOT, sx, sy, 18, 18);
                    var item = BuiltInRegistries.ITEM.getOptional(el.items().get(i));
                    if (item.isPresent()) {
                        ItemStack s = new ItemStack(item.get());
                        g.renderItem(s, sx + 1, sy + 1);
                        if (inside(mouseX, mouseY, sx, sy, 18, 18)) {
                            hovered = s;
                        }
                    }
                }
                caption(g, l, x, y + ((n + 5) / 6) * 20);
            }
            case "image" -> {
                int w = Math.min(el.width(), PAGE_W);
                int h = el.height() * w / Math.max(1, el.width());
                int ix = x + (PAGE_W - w) / 2;
                g.blit(el.image(), ix, y, w, h, 0, 0, el.width(), el.height(), el.width(), el.height());
                caption(g, l, x, y + h + 2);
            }
            case "recipe" -> {
                hovered = renderRecipe(g, el.items().get(0), x + (PAGE_W - 100) / 2, y, mouseX, mouseY);
                caption(g, l, x, y + 60);
            }
            default -> {
            }
        }
        return hovered;
    }

    private void caption(GuiGraphics g, Laid l, int x, int y) {
        for (FormattedCharSequence line : l.lines()) {
            g.drawString(font, line, x + (PAGE_W - font.width(line)) / 2, y, INK_SOFT, false);
            y += 9;
        }
    }

    private ItemStack renderRecipe(GuiGraphics g, ResourceLocation id, int x, int y, int mouseX, int mouseY) {
        g.blitSprite(RECIPE, x, y, 100, 56);
        if (minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<?>> holder = minecraft.level.getRecipeManager().byKey(id);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
            g.drawString(font, "?", x + 24, y + 24, INK_SOFT, false);
            return ItemStack.EMPTY;
        }
        ItemStack hovered = ItemStack.EMPTY;
        int w = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<Ingredient> ings = recipe.getIngredients();
        long tick = minecraft.level.getGameTime() / 20;
        for (int i = 0; i < ings.size() && i < 9; i++) {
            ItemStack[] options = ings.get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack s = options[(int) (tick % options.length)];
            int sx = x + 2 + (i % w) * 18, sy = y + 2 + (i / w) * 18;
            g.renderItem(s, sx + 1, sy + 1);
            if (inside(mouseX, mouseY, sx, sy, 18, 18)) {
                hovered = s;
            }
        }
        ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
        int ox = x + 79, oy = y + 20;
        g.renderItem(result, ox, oy);
        g.renderItemDecorations(font, result, ox, oy);
        if (inside(mouseX, mouseY, ox - 1, oy - 1, 18, 18)) {
            hovered = result;
        }
        return hovered;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (open != null) {
            if (inside(mx, my, bx + LEFT_X - 4, by + 4, 16, 10)) {
                closeEntry();
                return true;
            }
            return false;
        }
        int lx = bx + LEFT_X, rx = bx + RIGHT_X, py = by + PAGE_TOP;
        int y = py + CHAPTER_Y;
        for (GuideData.Chapter c : GuideData.INSTANCE.chapters()) {
            if (inside(mx, my, lx, y - 1, PAGE_W, CHAPTER_ROW)) {
                chapter = c.id();
                lastChapter = chapter;
                search.setValue("");
                listPage = 0;
                updateWidgets();
                return true;
            }
            y += CHAPTER_ROW;
        }
        List<GuideData.Entry> list = visibleEntries();
        int cap = listCapacity();
        y = py + 16;
        for (int i = listPage * cap; i < Math.min(list.size(), listPage * cap + cap); i++) {
            if (inside(mx, my, rx, y - 1, PAGE_W, 18)) {
                GuideData.Entry e = list.get(i);
                if (unlocked(e)) {
                    openEntry(e);
                }
                return true;
            }
            y += 18;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (open != null && (key == 259 || key == 263)) { // backspace / left
            turn(-1);
            return true;
        }
        if (open != null && key == 262) { // right
            turn(1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void tick() {
        updateWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
