package com.pizzaparker07.spacebreaker.client.guide;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.research.ResearchClientData;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;

/**
 * The field guide: a large two-page book scaled to the screen. A bar above the book has back / forward / contents
 * buttons and a search box; coloured bookmark ribbons on the right jump to each category. Chapters are Markdown
 * ({@link GuideMarkdown}) laid out over as many pages as they need; arrows, Page Up/Down and the mouse wheel turn
 * the pages. Nothing is locked: chapters tied to a technology only show a small "Related research" note.
 */
public class GuideScreen extends Screen {
    private static final ResourceLocation COVER = Spacebreaker.id("guide/cover"), PAGE_L = Spacebreaker.id("guide/page_left"),
            PAGE_R = Spacebreaker.id("guide/page_right");
    private static final ResourceLocation SLOT = Spacebreaker.id("guide/slot");
    /*
     * The book is sized to the screen (GUI pixels): bookH = min(available, 290); pageH = bookH - 12;
     * pageW = min((screenW - 12) / 2, min(240, max(round(pageH * 0.8), 180))); bookW = 2 * pageW + 12.
     * The cover and pages are nine-slice sprites, so the frame stays sharp at any size.
     * Body text is drawn at TS (a step below the GUI scale on large screens, still on whole pixels) so a line holds more
     * words; layout works in these content units (PAGE_W × PAGE_H).
     */
    private static int W = 360, H = 220, PAGE = 174, BOOK_PAGE_W = 150, TOP = 19, LEFT_X = 19, RIGHT_X = 191;
    private static float TS = 1F;
    private static int PAGE_W = 150, PAGE_H = 170;
    static final int INK = 0x2E2419, INK_SOFT = 0x6E5F4B, INK_FAINT = 0x9C8C72, TITLE = 0x1C4F8F, SUBTITLE = 0xA4520F, LINK = GuideMarkdown.LINK_COLOR;

    private enum Mode { CONTENTS, CHAPTER, SEARCH }

    private record Place(Mode mode, @Nullable ResourceLocation chapter, int spread, String query) {
    }

    /** Remembered between openings. */
    private static Place last = new Place(Mode.CONTENTS, null, 0, "");

    private final Deque<Place> back = new ArrayDeque<>();
    private final Deque<Place> forward = new ArrayDeque<>();
    private Place place = last;
    private List<List<Placed>> pages = List.of();
    private Map<String, Integer> anchors = Map.of();
    private float scale = 1F;
    private int ox, oy;
    private EditBox search;
    private Button backButton, forwardButton;
    private final Map<ResourceLocation, Entity> entities = new HashMap<>();

    public GuideScreen() {
        super(Component.translatable("item.spacebreaker.guide_book"));
        GuideStructures.clear(); // multiblocks come with the world's data
    }

    // ================================================================== setup

    @Override
    protected void init() {
        int bookH = Math.max(160, Math.min(height - 20 - 2 * 6 - 4, 290));
        int pageH = bookH - 12;
        PAGE = Math.max(120, Math.min((width - 12 - 2 * 24) / 2, Math.min(240, Math.max(Math.round(pageH * 0.8F), 180))));
        W = 2 * PAGE + 12;
        H = bookH;
        BOOK_PAGE_W = PAGE - 24;
        TOP = 6 + 13;
        LEFT_X = 6 + 13;
        RIGHT_X = 6 + PAGE + 11;
        double gs = minecraft.getWindow().getGuiScale();
        TS = gs >= 3 ? (float) ((gs - 1) / gs) : 1F;
        PAGE_W = (int) (BOOK_PAGE_W / TS);
        PAGE_H = (int) ((pageH - 13 - 15 - 8) / TS);
        scale = 1F;
        ox = (width - W) / 2;
        oy = (height - H) / 2 + 11;
        int barY = oy - 20;
        backButton = addRenderableWidget(Button.builder(Component.literal("◀"), b -> goBack()).bounds(ox, barY, 20, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.spacebreaker.guide.back"))).build());
        forwardButton = addRenderableWidget(Button.builder(Component.literal("▶"), b -> goForward()).bounds(ox + 22, barY, 20, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.spacebreaker.guide.forward"))).build());
        addRenderableWidget(Button.builder(Component.literal("☰"), b -> navigate(new Place(Mode.CONTENTS, null, 0, ""))).bounds(ox + 44, barY, 20, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.spacebreaker.guide.contents"))).build());
        String text = search == null ? place.query() : search.getValue();
        int sw = Math.min(160, (int) (W * scale) - 70);
        search = new EditBox(font, ox + (int) (W * scale) - sw, barY + 1, sw, 16, Component.translatable("gui.spacebreaker.guide.search"));
        search.setHint(Component.translatable("gui.spacebreaker.guide.search").withStyle(ChatFormatting.GRAY));
        search.setMaxLength(48);
        search.setValue(text);
        search.setResponder(this::onSearch);
        addRenderableWidget(search);
        rebuild();
    }

    private void onSearch(String text) {
        if (text.isBlank()) {
            if (place.mode() == Mode.SEARCH) {
                place = new Place(Mode.CONTENTS, null, 0, "");
                rebuild();
            }
            return;
        }
        place = new Place(Mode.SEARCH, null, 0, text);
        rebuild();
    }

    private void navigate(Place to) {
        back.push(place);
        forward.clear();
        place = to;
        if (to.mode() != Mode.SEARCH && search != null && !search.getValue().isEmpty()) {
            search.setResponder(s -> {
            });
            search.setValue("");
            search.setResponder(this::onSearch);
        }
        rebuild();
        playTurn();
    }

    private void goBack() {
        if (!back.isEmpty()) {
            forward.push(place);
            place = back.pop();
            rebuild();
            playTurn();
        }
    }

    private void goForward() {
        if (!forward.isEmpty()) {
            back.push(place);
            place = forward.pop();
            rebuild();
            playTurn();
        }
    }

    private void playTurn() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    /** Opens a chapter (used by item links, other screens and the showcase). */
    public void openChapter(ResourceLocation id, int spread) {
        navigate(new Place(Mode.CHAPTER, id, spread, ""));
    }

    public void showContents() {
        navigate(new Place(Mode.CONTENTS, null, 0, ""));
    }

    public void setSearch(String text) {
        search.setValue(text);
    }

    private void turn(int dir) {
        int spreads = Math.max(1, (pages.size() + 1) / 2);
        int target = place.spread() + dir;
        if (target >= 0 && target < spreads) {
            place = new Place(place.mode(), place.chapter(), target, place.query());
            playTurn();
            return;
        }
        if (place.mode() == Mode.CHAPTER && place.chapter() != null) {
            List<ResourceLocation> order = GuideBook.INSTANCE.order();
            int i = order.indexOf(place.chapter()) + (dir > 0 ? 1 : -1);
            if (i >= 0 && i < order.size()) {
                place = new Place(Mode.CHAPTER, order.get(i), 0, "");
                rebuild();
                if (dir < 0) {
                    place = new Place(Mode.CHAPTER, place.chapter(), Math.max(0, (pages.size() - 1) / 2), "");
                }
                playTurn();
            }
        }
    }

    // ================================================================== layout

    private void rebuild() {
        if (last != null && last.mode() == Mode.CONTENTS && place.mode() != Mode.CONTENTS) {
            introShown = true;
        }
        last = place;
        Layout l = new Layout(font);
        switch (place.mode()) {
            case CONTENTS -> layoutContents(l);
            case SEARCH -> layoutSearch(l, place.query());
            case CHAPTER -> {
                GuideMarkdown.Chapter ch = place.chapter() == null ? null : GuideBook.INSTANCE.chapter(place.chapter());
                if (ch == null) {
                    layoutContents(l);
                } else {
                    layoutChapter(l, ch);
                }
            }
        }
        pages = l.finish();
        anchors = l.anchors;
        int spreads = Math.max(1, (pages.size() + 1) / 2);
        if (place.spread() >= spreads) {
            place = new Place(place.mode(), place.chapter(), spreads - 1, place.query());
        }
    }

    private void layoutContents(Layout l) {
        l.add(new Title(Component.literal(GuideBook.INSTANCE.title()).withColor(TITLE), new ItemStack(BuiltInRegistries.ITEM.get(Spacebreaker.id("guide_book"))), null));
        String sub = GuideBook.INSTANCE.subtitle();
        if (!sub.isEmpty()) {
            l.text(Component.literal(sub).withStyle(Style.EMPTY.withColor(SUBTITLE).withItalic(true)), 0, null, 0);
            l.space(3);
        }
        if (!introShown) {
            String intro = GuideBook.INSTANCE.intro();
            l.text((intro.isEmpty() ? Component.translatable("gui.spacebreaker.guide.intro") : Component.literal(intro))
                    .withStyle(Style.EMPTY.withColor(INK_SOFT)), 0, null, 0);
            l.space(4);
        }
        for (GuideBook.Category c : GuideBook.INSTANCE.categories()) {
            boolean open = OPEN_CATEGORIES.contains(c.id());
            l.add(new CategoryHeader(c, open));
            if (!open) {
                continue;
            }
            for (ResourceLocation id : c.chapters()) {
                GuideMarkdown.Chapter ch = GuideBook.INSTANCE.chapter(id);
                if (ch != null) {
                    l.add(new ChapterLink(id, ch.title(), stack(ch.icon()), null));
                }
            }
            l.space(3);
        }
    }

    private void layoutSearch(Layout l, String query) {
        String q = query.trim().toLowerCase(Locale.forLanguageTag(GuideBook.lang().replace('_', '-')));
        String qr = query.trim().toLowerCase(Locale.ROOT);
        l.add(new Title(Component.translatable("gui.spacebreaker.guide.results"), new ItemStack(Items.SPYGLASS), null));
        int found = 0;
        for (ResourceLocation id : GuideBook.INSTANCE.order()) {
            GuideMarkdown.Chapter ch = GuideBook.INSTANCE.chapter(id);
            if (ch == null) {
                continue;
            }
            String text = ch.searchText();
            int at = text.indexOf(q);
            if (at < 0) {
                at = text.indexOf(qr);
            }
            if (at >= 0) {
                int s = Math.max(0, at - 18);
                String snippet = (s > 0 ? "…" : "") + stripMarkup(text.substring(s, Math.min(text.length(), at + 40))) + "…";
                l.add(new ChapterLink(id, ch.title(), stack(ch.icon()), snippet));
                found++;
            }
        }
        if (found == 0) {
            l.text(Component.translatable("gui.spacebreaker.guide.none").withStyle(Style.EMPTY.withColor(INK_SOFT)), 0, null, 0);
        }
    }

    private static String stripMarkup(String s) {
        return s.replaceAll("\\{[a-z/]+ ?[^}]*}", "").replace("**", "").replace("*", "").replace("`", "");
    }

    private void layoutChapter(Layout l, GuideMarkdown.Chapter ch) {
        Component note = null;
        if (ch.research().isPresent() && !ResearchClientData.isKnownOnClient(ch.research().get())) {
            note = Component.translatable("gui.spacebreaker.guide.research_needed",
                    Component.translatable(TechNode.nameKey(ch.research().get())));
        }
        GuideBook.Category cat = GuideBook.INSTANCE.categoryOf(ch.id());
        if (cat != null) {
            l.add(new TextLine(Component.literal(GuideBook.INSTANCE.categoryName(cat.id()).toUpperCase(Locale.forLanguageTag(GuideBook.lang().replace('_', '-'))))
                    .withStyle(Style.EMPTY.withColor(INK_FAINT)).getVisualOrderText(), 0, 0, 0, true));
        }
        l.add(new Title(Component.literal(ch.title()).withColor(TITLE), stack(ch.icon()), note));
        for (GuideMarkdown.Block b : ch.blocks()) {
            switch (b) {
                case GuideMarkdown.Heading h -> {
                    if (h.anchor() != null) {
                        l.mark(h.anchor());
                    }
                    l.space(h.level() == 1 ? 5 : 3);
                    int color = h.level() == 1 ? categoryColor(ch.id()) : (h.level() == 2 ? INK : INK_SOFT);
                    MutableComponent t = h.text().copy().withStyle(s -> s.withBold(true).withColor(color));
                    List<FormattedCharSequence> lines = font.split(t, PAGE_W);
                    for (int i = 0; i < lines.size(); i++) {
                        l.add(new TextLine(lines.get(i), 0, 0, 0, true));
                    }
                    if (h.level() == 1) {
                        l.add(new Rule(color));
                    } else {
                        l.space(2);
                    }
                }
                case GuideMarkdown.Para p -> {
                    l.text(p.text(), 0, null, 0);
                    l.space(4);
                }
                case GuideMarkdown.ListItem li -> {
                    l.text(li.text(), 8 + li.indent() * 8, li.marker(), 0);
                    l.space(1);
                }
                case GuideMarkdown.Callout c -> {
                    int bar = switch (c.kind()) {
                        case "tip" -> 0x2A8CFF;
                        case "warning" -> 0xE07412;
                        default -> 0x8A7A60;
                    };
                    l.space(2);
                    l.add(new BoxEdge(bar, true));
                    for (int i = 0; i < c.paras().size(); i++) {
                        if (i > 0) {
                            l.add(new BoxSpace(bar, 3));
                        }
                        l.text(c.paras().get(i), 7, null, bar);
                    }
                    l.add(new BoxEdge(bar, false));
                    l.space(4);
                }
                case GuideMarkdown.Table t -> {
                    l.space(2);
                    int cols = t.rows().stream().mapToInt(List::size).max().orElse(1);
                    int[] widths = columnWidths(t, cols);
                    for (int r = 0; r < t.rows().size(); r++) {
                        l.add(new TableRow(font, t.rows().get(r), widths, t.header() && r == 0, r == t.rows().size() - 1));
                    }
                    l.space(4);
                }
                case GuideMarkdown.PageBreak pb -> l.pageBreak();
                case GuideMarkdown.Rule r -> {
                    l.space(2);
                    l.add(new Ornament());
                    l.space(2);
                }
                case GuideMarkdown.Recipe r -> {
                    RecipeView v = new RecipeView(r.id());
                    if (!(v.missing && r.optional())) {
                        l.add(v);
                        l.space(4);
                    }
                }
                case GuideMarkdown.Craft cr -> {
                    l.add(new RecipeView(cr));
                    l.space(4);
                }
                case GuideMarkdown.Items it -> {
                    l.add(new ItemRow(it.ids()));
                    l.space(4);
                }
                case GuideMarkdown.EntityView e -> {
                    if (!(e.optional() && !BuiltInRegistries.ENTITY_TYPE.containsKey(e.id()))) {
                        l.add(new EntityElem(e.id(), e.scale(), e.height()));
                        l.space(4);
                    }
                }
                case GuideMarkdown.StructureView s -> {
                    if (!(s.optional() && !GuideStructures.exists(s.id()))) {
                        l.add(new StructureElem(s.id()));
                        l.space(4);
                    }
                }
                case GuideMarkdown.Image im -> {
                    l.add(new ImageElem(im));
                    l.space(4);
                }
            }
        }
    }

    private static int categoryColor(ResourceLocation chapter) {
        GuideBook.Category c = GuideBook.INSTANCE.categoryOf(chapter);
        return c == null ? INK : darken(c.color());
    }

    /** Category colours are bright for ribbons; text needs a darker tone on cream paper. */
    static int darken(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return ((int) (r * 0.62) << 16) | ((int) (g * 0.62) << 8) | (int) (b * 0.62);
    }

    static ItemStack stack(ResourceLocation id) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BOOK));
    }

    // ================================================================== rendering

    /** Per-frame drawing context in book coordinates. */
    final class Ctx {
        final GuiGraphics g;
        final double mx, my;
        final float time;
        ItemStack hoverStack = ItemStack.EMPTY;
        @Nullable Style hoverStyle;
        @Nullable List<Component> hoverText;

        Ctx(GuiGraphics g, double mx, double my, float time) {
            this.g = g;
            this.mx = mx;
            this.my = my;
            this.time = time;
        }

        void copyHoverTo(Ctx o) {
            if (!hoverStack.isEmpty()) {
                o.hoverStack = hoverStack;
            }
            if (hoverStyle != null) {
                o.hoverStyle = hoverStyle;
            }
            if (hoverText != null) {
                o.hoverText = hoverText;
            }
        }

        boolean over(int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        Font font() {
            return font;
        }
    }

    @Override
    public void removed() {
        introShown = true;
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // one strip along the book's top edge, the width of the book, holding the buttons and the search box
        int x0 = ox, x1 = ox + (int) (W * scale), y0 = oy - 22, y1 = oy;
        g.fill(x0 + 2, y0, x1 - 2, y1, 0xFF0F1830);
        g.fill(x0 + 3, y0 + 1, x1 - 3, y1, 0xFF22336B);
    }

    /** Categories opened on the contents page (the contents list only the category names until one is clicked). */
    private static final java.util.Set<String> OPEN_CATEGORIES = new java.util.HashSet<>();
    /** The "how to use this book" paragraph shows on the first contents page of a play session only. */
    private static boolean introShown;
    private static final String LINK_CATEGORY = "category:";

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        backButton.active = !back.isEmpty();
        forwardButton.active = !forward.isEmpty();
        double mx = (mouseX - ox) / scale, my = (mouseY - oy) / scale;
        float time = (minecraft.level != null ? minecraft.level.getGameTime() : 0) + partialTick;
        Ctx c = new Ctx(g, mx, my, time);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(scale, scale, 1F);
        renderBookmarks(c, true);
        g.blitSprite(COVER, 0, 0, W, H);
        g.blitSprite(PAGE_L, 6, 6, PAGE, H - 12);
        g.blitSprite(PAGE_R, 6 + PAGE, 6, PAGE, H - 12);
        g.fill(5 + PAGE, 4, 7 + PAGE, H - 4, 0xFF0F1830);
        int spread = place.spread();
        for (int side = 0; side < 2; side++) {
            int idx = spread * 2 + side;
            if (idx >= pages.size()) {
                break;
            }
            int px = side == 0 ? LEFT_X : RIGHT_X;
            Ctx pc = new Ctx(g, (mx - px) / TS, (my - TOP) / TS, time);
            g.pose().pushPose();
            g.pose().translate(px, TOP, 0);
            g.pose().scale(TS, TS, 1F);
            for (Placed p : pages.get(idx)) {
                p.elem().render(pc, 0, p.y());
            }
            g.pose().popPose();
            pc.copyHoverTo(c);
            Component num = Component.literal(String.valueOf(idx + 1));
            g.drawString(font, num, px + (BOOK_PAGE_W - font.width(num)) / 2, H - 18, INK_SOFT, false);
        }
        int spreads = Math.max(1, (pages.size() + 1) / 2);
        renderArrow(c, true, spread > 0 || canChainChapter(-1));
        renderArrow(c, false, spread < spreads - 1 || canChainChapter(1));
        renderBookmarks(c, false);
        g.pose().popPose();
        if (!c.hoverStack.isEmpty()) {
            g.renderTooltip(font, c.hoverStack, mouseX, mouseY);
        } else if (c.hoverStyle != null && c.hoverStyle.getHoverEvent() != null) {
            HoverEvent.ItemStackInfo info = c.hoverStyle.getHoverEvent().getValue(HoverEvent.Action.SHOW_ITEM);
            if (info != null) {
                g.renderTooltip(font, info.getItemStack(), mouseX, mouseY);
            }
        } else if (c.hoverText != null) {
            g.renderComponentTooltip(font, c.hoverText, mouseX, mouseY);
        }
    }

    private boolean canChainChapter(int dir) {
        if (place.mode() != Mode.CHAPTER || place.chapter() == null) {
            return false;
        }
        List<ResourceLocation> order = GuideBook.INSTANCE.order();
        int i = order.indexOf(place.chapter()) + dir;
        return i >= 0 && i < order.size();
    }

    private static int arrowY() {
        return H - 21;
    }

    private void renderArrow(Ctx c, boolean left, boolean enabled) {
        if (!enabled) {
            return;
        }
        int x = left ? 14 : W - 32;
        boolean hot = c.over(x, arrowY(), 18, 10);
        String sprite = left ? (hot ? "widget/page_backward_highlighted" : "widget/page_backward")
                : (hot ? "widget/page_forward_highlighted" : "widget/page_forward");
        c.g.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, arrowY(), 18, 10);
    }

    /** Coloured ribbons sticking out of the right edge: one per category (behind the cover; tips drawn on top). */
    private void renderBookmarks(Ctx c, boolean behind) {
        List<GuideBook.Category> cats = GuideBook.INSTANCE.categories();
        int n = cats.size();
        if (n == 0) {
            return;
        }
        int h = Math.min(18, (H - 24) / n);
        GuideBook.Category current = place.chapter() == null ? null : GuideBook.INSTANCE.categoryOf(place.chapter());
        for (int i = 0; i < n; i++) {
            GuideBook.Category cat = cats.get(i);
            int y = 12 + i * h;
            boolean active = cat == current;
            boolean hot = c.over(W - 4, y, 22, h - 2);
            int len = active || hot ? 20 : 15;
            if (behind) {
                int col = 0xFF000000 | cat.color();
                c.g.fill(W - 6, y, W + len, y + h - 2, col);
                c.g.fill(W - 6, y + h - 3, W + len, y + h - 2, 0xFF000000 | darken(cat.color()));
                c.g.fill(W + len - 1, y, W + len, y + h - 2, 0xFF000000 | darken(cat.color()));
            } else {
                c.g.pose().pushPose();
                c.g.pose().translate(W + len - 13, y + (h - 2) / 2F - 5, 0);
                c.g.pose().scale(0.625F, 0.625F, 1F);
                c.g.renderFakeItem(new ItemStack(BuiltInRegistries.ITEM.getOptional(cat.icon()).orElse(Items.BOOK)), 0, 0);
                c.g.pose().popPose();
                if (hot) {
                    c.hoverText = List.of(Component.literal(GuideBook.INSTANCE.categoryName(cat.id())).withColor(cat.color()));
                }
            }
        }
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 3) {
            goBack();
            return true;
        }
        if (button == 4) {
            goForward();
            return true;
        }
        if (button != 0) {
            return false;
        }
        double mx = (mouseX - ox) / scale, my = (mouseY - oy) / scale;
        if (mx >= 14 && mx < 32 && my >= arrowY() && my < arrowY() + 10) {
            turn(-1);
            return true;
        }
        if (mx >= W - 32 && mx < W - 14 && my >= arrowY() && my < arrowY() + 10) {
            turn(1);
            return true;
        }
        List<GuideBook.Category> cats = GuideBook.INSTANCE.categories();
        if (!cats.isEmpty() && mx >= W - 4 && mx < W + 22) {
            int h = Math.min(18, (H - 24) / cats.size());
            int i = (int) ((my - 12) / h);
            if (my >= 12 && i >= 0 && i < cats.size() && !cats.get(i).chapters().isEmpty()) {
                openChapter(cats.get(i).chapters().get(0), 0);
                return true;
            }
        }
        for (int side = 0; side < 2; side++) {
            int idx = place.spread() * 2 + side;
            if (idx >= pages.size()) {
                break;
            }
            int px = side == 0 ? LEFT_X : RIGHT_X;
            for (Placed p : pages.get(idx)) {
                String link = p.elem().click(0, p.y(), (mx - px) / TS, (my - TOP) / TS);
                if (link != null) {
                    follow(link);
                    return true;
                }
            }
        }
        return false;
    }

    private void follow(String link) {
        if (link.startsWith(LINK_CATEGORY)) {
            String id = link.substring(LINK_CATEGORY.length());
            if (!OPEN_CATEGORIES.remove(id)) {
                OPEN_CATEGORIES.add(id);
            }
            rebuild();
            return;
        }
        if (link.startsWith(LINK_HOLOGRAM)) {
            ResourceLocation id = ResourceLocation.tryParse(link.substring(LINK_HOLOGRAM.length()));
            if (id != null) {
                showHologram(id);
            }
        } else if (link.startsWith("https://")) {
            minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen(open -> {
                if (open) {
                    net.minecraft.Util.getPlatform().openUri(link);
                }
                minecraft.setScreen(this);
            }, link, true));
        } else if (link.startsWith(GuideMarkdown.LINK_CHAPTER)) {
            String target = link.substring(GuideMarkdown.LINK_CHAPTER.length());
            int hash = target.indexOf('#');
            String anchor = hash < 0 ? null : target.substring(hash + 1);
            ResourceLocation id = hash == 0 ? place.chapter() : ResourceLocation.tryParse(hash < 0 ? target : target.substring(0, hash));
            if (id != null && GuideBook.INSTANCE.chapter(id) != null) {
                if (!id.equals(place.chapter()) || place.mode() != Mode.CHAPTER) {
                    openChapter(id, 0);
                }
                if (anchor != null && anchors.containsKey(anchor)) {
                    place = new Place(Mode.CHAPTER, id, anchors.get(anchor) / 2, "");
                }
            }
        } else if (link.startsWith(GuideMarkdown.LINK_ITEM)) {
            ResourceLocation item = ResourceLocation.tryParse(link.substring(GuideMarkdown.LINK_ITEM.length()));
            ResourceLocation ch = item == null ? null : GuideBook.INSTANCE.chapterForItem(item);
            if (ch != null && !ch.equals(place.chapter())) {
                openChapter(ch, 0);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY < 0 ? 1 : -1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_DOWN) {
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_D -> {
                turn(1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_A -> {
                turn(-1);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                goBack();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                showContents();
                return true;
            }
            default -> {
            }
        }
        if (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== layout engine

    private record Placed(Elem elem, int y) {
    }

    private final class Layout {
        private final Font font;
        private final List<List<Placed>> pages = new ArrayList<>();
        private List<Placed> page = new ArrayList<>();
        private int y;
        private @Nullable Elem held; // a heading waiting for its first line (kept together)
        final Map<String, Integer> anchors = new HashMap<>();

        /** Remembers the page the next element lands on (headings with {#anchor}). */
        void mark(String anchor) {
            anchors.put(anchor, y > PAGE_H - 30 ? pages.size() + 1 : pages.size());
        }

        Layout(Font font) {
            this.font = font;
        }

        void add(Elem e) {
            int h = e.height();
            if (y + h > PAGE_H && !page.isEmpty()) {
                newPage();
            }
            page.add(new Placed(e, y));
            y += h;
        }

        void space(int h) {
            if (!page.isEmpty()) {
                y += h;
            }
        }

        void pageBreak() {
            if (!page.isEmpty()) {
                newPage();
            }
        }

        private void newPage() {
            // keep a heading together with what follows it
            List<Placed> carry = new ArrayList<>();
            while (!page.isEmpty() && page.get(page.size() - 1).elem().keepWithNext()) {
                carry.add(0, page.remove(page.size() - 1));
            }
            if (page.isEmpty()) {
                page = carry;
                carry = new ArrayList<>();
            }
            pages.add(page);
            page = new ArrayList<>();
            y = 0;
            for (Placed p : carry) {
                page.add(new Placed(p.elem(), y));
                y += p.elem().height();
            }
        }

        /** Wraps text into lines (each its own element so paragraphs flow across pages). */
        void text(Component text, int indent, @Nullable Component marker, int box) {
            int inner = box != 0 ? 14 : 0;
            Component styled = text.copy().withStyle(s -> s.getColor() == null ? s.withColor(INK) : s);
            List<FormattedCharSequence> lines = font.split(styled, PAGE_W - indent - inner);
            for (int i = 0; i < lines.size(); i++) {
                TextLine l = new TextLine(lines.get(i), indent + (box != 0 ? 7 : 0), box, 0, false);
                if (i == 0 && marker != null) {
                    l.marker = marker;
                }
                add(l);
            }
        }

        List<List<Placed>> finish() {
            if (!page.isEmpty() || pages.isEmpty()) {
                pages.add(page);
            }
            return pages;
        }
    }

    // ================================================================== elements

    private abstract static class Elem {
        abstract int height();

        abstract void render(Ctx c, int x, int y);

        /** @return a link target if clicked */
        @Nullable String click(int x, int y, double mx, double my) {
            return null;
        }

        boolean keepWithNext() {
            return false;
        }
    }

    private final class TextLine extends Elem {
        final FormattedCharSequence seq;
        final int indent, box;
        final boolean heading;
        @Nullable Component marker;

        TextLine(FormattedCharSequence seq, int indent, int box, int unused, boolean heading) {
            this.seq = seq;
            this.indent = indent;
            this.box = box;
            this.heading = heading;
        }

        @Override
        int height() {
            return heading ? 11 : 10;
        }

        @Override
        boolean keepWithNext() {
            return heading;
        }

        @Override
        void render(Ctx c, int x, int y) {
            if (box != 0) {
                c.g.fill(x, y - 1, x + PAGE_W, y + 9, 0x22000000 | box);
                c.g.fill(x, y - 1, x + 2, y + 9, 0xFF000000 | box);
            }
            if (marker != null) {
                c.g.drawString(font, marker, x + indent - font.width(marker) - 3, y, box != 0 ? box : INK_SOFT, false);
            }
            c.g.drawString(font, seq, x + indent, y, INK, false);
            if (c.my >= y && c.my < y + 9 && c.mx >= x + indent) {
                Style s = font.getSplitter().componentStyleAtWidth(seq, (int) (c.mx - x - indent));
                if (s != null && (s.getHoverEvent() != null || s.getClickEvent() != null)) {
                    c.hoverStyle = s;
                }
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            if (my >= y && my < y + 9 && mx >= x + indent) {
                Style s = font.getSplitter().componentStyleAtWidth(seq, (int) (mx - x - indent));
                if (s != null && s.getClickEvent() != null && s.getClickEvent().getAction() == ClickEvent.Action.CHANGE_PAGE) {
                    return s.getClickEvent().getValue();
                }
            }
            return null;
        }
    }

    private static final class Spacer extends Elem {
        final int h;

        Spacer(int h) {
            this.h = h;
        }

        @Override
        int height() {
            return h;
        }

        @Override
        void render(Ctx c, int x, int y) {
        }
    }

    private static final class Rule extends Elem {
        final int color;

        Rule(int color) {
            this.color = color;
        }

        @Override
        int height() {
            return 4;
        }

        @Override
        boolean keepWithNext() {
            return true;
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.fill(x, y, x + PAGE_W, y + 1, 0xAA000000 | color);
        }
    }

    /** A decorative divider: a thin line with a small diamond in the middle. */
    private static final class Ornament extends Elem {
        @Override
        int height() {
            return 7;
        }

        @Override
        void render(Ctx c, int x, int y) {
            int mid = x + PAGE_W / 2, col = 0xFF000000 | 0xCDBB94;
            c.g.fill(x + 12, y + 3, mid - 5, y + 4, col);
            c.g.fill(mid + 5, y + 3, x + PAGE_W - 12, y + 4, col);
            for (int i = 0; i < 3; i++) {
                c.g.fill(mid - i, y + 3 - (2 - i), mid + i + 1, y + 4 + (2 - i), 0xFF000000 | SUBTITLE);
            }
        }
    }

    private static final class BoxEdge extends Elem {
        final int color;
        final boolean top;

        BoxEdge(int color, boolean top) {
            this.color = color;
            this.top = top;
        }

        @Override
        int height() {
            return 3;
        }

        @Override
        boolean keepWithNext() {
            return top;
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.fill(x, y, x + PAGE_W, y + 3, 0x22000000 | color);
            c.g.fill(x, y, x + 2, y + 3, 0xFF000000 | color);
        }
    }

    private static final class BoxSpace extends Elem {
        final int color, h;

        BoxSpace(int color, int h) {
            this.color = color;
            this.h = h;
        }

        @Override
        int height() {
            return h;
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.fill(x, y - 1, x + PAGE_W, y + h - 1, 0x22000000 | color);
            c.g.fill(x, y - 1, x + 2, y + h - 1, 0xFF000000 | color);
        }
    }

    /** Titles stay large while body text is drawn smaller. */
    private static final float BIG = 1.25F;

    private final class Title extends Elem {
        final Component text;
        final ItemStack icon;
        final @Nullable Component note;
        final List<FormattedCharSequence> lines;
        final List<FormattedCharSequence> noteLines;

        Title(Component text, ItemStack icon, @Nullable Component note) {
            this.text = text;
            this.icon = icon;
            this.note = note;
            this.lines = font.split(text.copy().withStyle(ChatFormatting.BOLD), (int) ((PAGE_W - 22) / BIG));
            this.noteLines = note == null ? List.of() : font.split(Component.literal("⚑ ").append(note), PAGE_W - 6);
        }

        int noteHeight() {
            return noteLines.isEmpty() ? 0 : noteLines.size() * 10 + 2;
        }

        int head() {
            return Math.max(24, Math.round(lines.size() * 10 * BIG) + 4);
        }

        @Override
        int height() {
            return head() + noteHeight() + 4;
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.pose().pushPose();
            c.g.pose().translate(x, y, 0);
            c.g.pose().scale(BIG, BIG, 1F);
            c.g.renderFakeItem(icon, 0, 0);
            int ty = lines.size() == 1 ? 4 : 0;
            for (FormattedCharSequence l : lines) {
                c.g.drawString(font, l, 20, ty, INK, false);
                ty += 10;
            }
            c.g.pose().popPose();
            int base = y + head();
            c.g.fill(x, base - 2, x + PAGE_W, base - 1, 0x55000000 | INK_SOFT);
            if (!noteLines.isEmpty()) {
                c.g.fill(x, base + 1, x + PAGE_W, base + 1 + noteHeight() - 2, 0x33D07020);
                int ny = base + 2;
                for (FormattedCharSequence l : noteLines) {
                    c.g.drawString(font, l, x + 3, ny, 0xA05010, false);
                    ny += 10;
                }
            }
        }
    }

    private final class CategoryHeader extends Elem {
        final GuideBook.Category cat;

        final boolean open;

        CategoryHeader(GuideBook.Category cat) {
            this(cat, true);
        }

        CategoryHeader(GuideBook.Category cat, boolean open) {
            this.cat = cat;
            this.open = open;
        }

        @Override
        int height() {
            return 15;
        }

        @Override
        boolean keepWithNext() {
            return true;
        }

        @Override
        void render(Ctx c, int x, int y) {
            boolean hot = place.mode() == Mode.CONTENTS && c.over(x, y, PAGE_W, height() - 1);
            String marker = place.mode() == Mode.CONTENTS ? (open ? "▼ " : "▶ ") : "";
            c.g.drawString(font, Component.literal(marker + GuideBook.INSTANCE.categoryName(cat.id())).withStyle(ChatFormatting.BOLD),
                    x, y + 2, hot ? LINK : darken(cat.color()), false);
            if (place.mode() == Mode.CONTENTS && !open) {
                String n = String.valueOf(cat.chapters().size());
                c.g.drawString(font, n, x + PAGE_W - font.width(n), y + 2, INK_SOFT, false);
            }
            c.g.fill(x, y + 12, x + PAGE_W, y + 13, 0xFF000000 | cat.color());
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            return place.mode() == Mode.CONTENTS && mx >= x && mx < x + PAGE_W && my >= y && my < y + height() - 1
                    ? LINK_CATEGORY + cat.id() : null;
        }
    }

    private final class ChapterLink extends Elem {
        final ResourceLocation id;
        final String title;
        final ItemStack icon;
        final @Nullable String snippet;

        ChapterLink(ResourceLocation id, String title, ItemStack icon, @Nullable String snippet) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.snippet = snippet;
        }

        @Override
        int height() {
            return snippet != null ? 22 : 13;
        }

        @Override
        void render(Ctx c, int x, int y) {
            boolean hot = c.over(x, y, PAGE_W, height() - 1);
            if (hot) {
                c.g.fill(x - 1, y - 1, x + PAGE_W, y + height() - 2, 0x22000000 | LINK);
            }
            c.g.pose().pushPose();
            c.g.pose().translate(x + 2, y, 0);
            c.g.pose().scale(0.75F, 0.75F, 1F);
            c.g.renderFakeItem(icon, 0, 0);
            c.g.pose().popPose();
            String t = font.plainSubstrByWidth(title, PAGE_W - 18);
            c.g.drawString(font, t, x + 16, y + 2, hot ? LINK : INK, false);
            if (snippet != null) {
                c.g.drawString(font, font.plainSubstrByWidth(snippet, PAGE_W - 18), x + 16, y + 11, INK_SOFT, false);
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            return mx >= x && mx < x + PAGE_W && my >= y && my < y + height() - 1 ? GuideMarkdown.LINK_CHAPTER + id : null;
        }
    }

    /**
     * Column widths for a table: every column gets at least its longest word (so words are not cut), and the rest of the
     * page width goes to the columns with the longest text.
     */
    private int[] columnWidths(GuideMarkdown.Table t, int cols) {
        int[] min = new int[cols], natural = new int[cols];
        for (int r = 0; r < t.rows().size(); r++) {
            boolean bold = t.header() && r == 0;
            List<Component> row = t.rows().get(r);
            for (int i = 0; i < row.size() && i < cols; i++) {
                Component cell = bold ? row.get(i).copy().withStyle(ChatFormatting.BOLD) : row.get(i);
                natural[i] = Math.max(natural[i], font.width(cell) + 5);
                for (String word : row.get(i).getString().split("\\s+")) {
                    Component w = Component.literal(word);
                    min[i] = Math.max(min[i], font.width(bold ? w.copy().withStyle(ChatFormatting.BOLD) : w) + 5);
                }
            }
        }
        int[] widths = new int[cols];
        int sumMin = 0, sumExtra = 0;
        for (int i = 0; i < cols; i++) {
            sumMin += min[i];
            sumExtra += Math.max(0, natural[i] - min[i]);
        }
        if (sumMin >= PAGE_W) {
            for (int i = 0; i < cols; i++) {
                widths[i] = min[i] * PAGE_W / Math.max(1, sumMin);
            }
        } else {
            int spare = PAGE_W - sumMin;
            for (int i = 0; i < cols; i++) {
                int extra = Math.max(0, natural[i] - min[i]);
                widths[i] = min[i] + (sumExtra == 0 ? spare / cols : Math.min(extra, spare * extra / sumExtra));
            }
            int used = 0;
            for (int w : widths) {
                used += w;
            }
            widths[cols - 1] += PAGE_W - used;
        }
        return widths;
    }

    private final class TableRow extends Elem {
        final List<List<FormattedCharSequence>> cells = new ArrayList<>();
        final int cols, rows;
        final int[] widths, offsets;
        final boolean header, lastRow;

        TableRow(Font font, List<Component> row, int[] widths, boolean header, boolean lastRow) {
            this.cols = widths.length;
            this.widths = widths;
            this.offsets = new int[cols + 1];
            for (int i = 0; i < cols; i++) {
                offsets[i + 1] = offsets[i] + widths[i];
            }
            this.header = header;
            this.lastRow = lastRow;
            int r = 1;
            for (int i = 0; i < cols; i++) {
                Component cell = i < row.size() ? row.get(i) : Component.empty();
                Component styled = cell.copy().withStyle(s -> (s.getColor() == null ? s.withColor(INK) : s).withBold(header || s.isBold()));
                List<FormattedCharSequence> lines = font.split(styled, widths[i] - 4);
                cells.add(lines);
                r = Math.max(r, lines.size());
            }
            this.rows = r;
        }

        @Override
        int height() {
            return rows * 9 + 4;
        }

        @Override
        void render(Ctx c, int x, int y) {
            int h = height();
            if (header) {
                c.g.fill(x, y, x + offsets[cols], y + h, 0x30000000 | INK_SOFT);
            }
            c.g.fill(x, y, x + offsets[cols], y + 1, 0x66000000 | INK_SOFT);
            if (lastRow) {
                c.g.fill(x, y + h, x + offsets[cols], y + h + 1, 0x66000000 | INK_SOFT);
            }
            for (int i = 0; i < cols; i++) {
                int cx = x + offsets[i];
                if (i > 0) {
                    c.g.fill(cx, y, cx + 1, y + h, 0x33000000 | INK_SOFT);
                }
                int ly = y + 3;
                for (FormattedCharSequence l : cells.get(i)) {
                    c.g.drawString(font, l, cx + 2, ly, INK, false);
                    if (c.my >= ly && c.my < ly + 9 && c.mx >= cx + 2 && c.mx < cx + widths[i]) {
                        Style s = font.getSplitter().componentStyleAtWidth(l, (int) (c.mx - cx - 2));
                        if (s != null && s.getHoverEvent() != null) {
                            c.hoverStyle = s;
                        }
                    }
                    ly += 9;
                }
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            for (int i = 0; i < cols; i++) {
                int cx = x + offsets[i], ly = y + 3;
                for (FormattedCharSequence l : cells.get(i)) {
                    if (my >= ly && my < ly + 9 && mx >= cx + 2 && mx < cx + widths[i]) {
                        Style s = font.getSplitter().componentStyleAtWidth(l, (int) (mx - cx - 2));
                        if (s != null && s.getClickEvent() != null) {
                            return s.getClickEvent().getValue();
                        }
                    }
                    ly += 9;
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ item widgets

    /** Draws one slot with a stack; returns true if hovered. */
    private boolean slot(Ctx c, int x, int y, ItemStack s) {
        c.g.blitSprite(SLOT, x, y, 18, 18);
        if (!s.isEmpty()) {
            c.g.renderItem(s, x + 1, y + 1);
            c.g.renderItemDecorations(font, s, x + 1, y + 1);
        }
        boolean hot = c.over(x, y, 18, 18);
        if (hot && !s.isEmpty()) {
            c.hoverStack = s;
        }
        return hot;
    }

    /** Draws one slot with a fluid or gas, its amount in millibuckets and a tooltip. */
    private void fluidSlot(Ctx c, int x, int y, net.neoforged.neoforge.fluids.FluidStack f) {
        c.g.blitSprite(SLOT, x, y, 18, 18);
        var ext = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(f.getFluid());
        var sprite = minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(f));
        int tint = ext.getTintColor(f);
        c.g.blit(x + 1, y + 1, 0, 16, 16, sprite, ((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1F);
        String amount = f.getAmount() >= 1000 && f.getAmount() % 1000 == 0 ? f.getAmount() / 1000 + "B" : String.valueOf(f.getAmount());
        c.g.pose().pushPose();
        c.g.pose().translate(x + 17, y + 12, 200);
        c.g.pose().scale(0.5F, 0.5F, 1F);
        c.g.drawString(font, amount, -font.width(amount), 0, 0xFFFFFF, true);
        c.g.pose().popPose();
        if (c.over(x, y, 18, 18)) {
            c.hoverText = List.of(f.getHoverName(), Component.literal(f.getAmount() + " mB").withStyle(ChatFormatting.GRAY));
        }
    }

    private static @Nullable String itemLink(ItemStack s) {
        return s.isEmpty() ? null : GuideMarkdown.LINK_ITEM + BuiltInRegistries.ITEM.getKey(s.getItem());
    }

    private final class ItemRow extends Elem {
        final List<ItemStack> stacks = new ArrayList<>();

        ItemRow(List<ResourceLocation> ids) {
            ids.forEach(id -> stacks.add(stack(id)));
        }

        int perRow() {
            return Math.max(1, PAGE_W / 20);
        }

        @Override
        int height() {
            return ((stacks.size() + perRow() - 1) / perRow()) * 20;
        }

        private int sx(int x, int i) {
            int row = i / perRow();
            int inRow = Math.min(perRow(), stacks.size() - row * perRow());
            return x + (PAGE_W - inRow * 20) / 2 + (i % perRow()) * 20;
        }

        @Override
        void render(Ctx c, int x, int y) {
            for (int i = 0; i < stacks.size(); i++) {
                slot(c, sx(x, i), y + (i / perRow()) * 20, stacks.get(i));
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            for (int i = 0; i < stacks.size(); i++) {
                int sx = sx(x, i), sy = y + (i / perRow()) * 20;
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    return itemLink(stacks.get(i));
                }
            }
            return null;
        }
    }

    private final class RecipeView extends Elem {
        final ResourceLocation id;
        final List<Ingredient> inputs = new ArrayList<>();
        final List<Integer> counts = new ArrayList<>();
        final List<ItemStack> outputs = new ArrayList<>();
        final java.util.Map<Integer, net.neoforged.neoforge.fluids.FluidStack> inFluid = new java.util.HashMap<>();
        final java.util.Map<Integer, net.neoforged.neoforge.fluids.FluidStack> outFluid = new java.util.HashMap<>();
        ItemStack machine = ItemStack.EMPTY;
        int gridW = 0; // 0 = row of inputs, otherwise crafting grid width
        @Nullable Component info;
        boolean missing;

        /** A hand-written crafting grid from {craft ...}. */
        RecipeView(GuideMarkdown.Craft craft) {
            this.id = craft.result();
            for (ResourceLocation r : craft.grid()) {
                inputs.add(r == null ? Ingredient.EMPTY : Ingredient.of(BuiltInRegistries.ITEM.get(r)));
                counts.add(1);
            }
            gridW = craft.width();
            ItemStack out = stack(craft.result());
            out.setCount(Math.max(1, craft.count()));
            outputs.add(out);
            machine = new ItemStack(Items.CRAFTING_TABLE);
        }

        RecipeView(ResourceLocation id) {
            this.id = id;
            var level = Minecraft.getInstance().level;
            RecipeHolder<?> holder = level == null ? null : level.getRecipeManager().byKey(id).orElse(null);
            if (holder == null) {
                missing = true;
                return;
            }
            Recipe<?> r = holder.value();
            var regs = level.registryAccess();
            if (r instanceof ProcessingRecipe p) {
                p.ingredients.forEach(si -> {
                    inputs.add(si.ingredient());
                    counts.add(si.count());
                });
                java.util.stream.Stream.of(p.fluidInput, p.fluidInput2).flatMap(Optional::stream).forEach(f -> {
                    var fs = f.getFluids();
                    if (fs.length > 0) {
                        inFluid.put(inputs.size(), new net.neoforged.neoforge.fluids.FluidStack(fs[0].getFluid(), f.amount()));
                        inputs.add(Ingredient.EMPTY);
                        counts.add(1);
                    }
                });
                outputs.addAll(p.results);
                p.bonus.ifPresent(outputs::add);
                p.fluidResults.forEach(f -> {
                    outFluid.put(outputs.size(), f);
                    outputs.add(ItemStack.EMPTY);
                });
                info = Component.translatable("gui.spacebreaker.guide.recipe_info", p.energy, Mth.ceil(p.time / 20F));
            } else if (r instanceof CraftingRecipe cr) {
                gridW = cr instanceof ShapedRecipe s ? s.getWidth() : Math.min(3, Math.max(1, (int) Math.ceil(Math.sqrt(cr.getIngredients().size()))));
                inputs.addAll(cr.getIngredients());
                cr.getIngredients().forEach(x -> counts.add(1));
                outputs.add(r.getResultItem(regs));
                machine = new ItemStack(Items.CRAFTING_TABLE);
            } else if (r instanceof AbstractCookingRecipe cook) {
                inputs.addAll(cook.getIngredients());
                cook.getIngredients().forEach(x -> counts.add(1));
                outputs.add(r.getResultItem(regs));
                info = Component.translatable("gui.spacebreaker.guide.recipe_time", Mth.ceil(cook.getCookingTime() / 20F));
            } else {
                for (Ingredient in : r.getIngredients()) {
                    if (!in.isEmpty()) {
                        inputs.add(in);
                        counts.add(1);
                    }
                }
                outputs.add(r.getResultItem(regs));
            }
            if (machine.isEmpty()) {
                ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
                ResourceLocation m = type == null ? null : GuideBook.INSTANCE.machineFor(type);
                machine = m != null ? stack(m) : r.getToastSymbol();
            }
        }

        int cols() {
            return gridW > 0 ? gridW : Math.min(3, Math.max(1, inputs.size()));
        }

        int rows() {
            return Math.max(1, (inputs.size() + cols() - 1) / cols());
        }

        @Override
        int height() {
            return missing ? 12 : Math.max(rows() * 18, 36) + 4 + (info != null ? 10 : 0);
        }

        private ItemStack shown(int i, float time) {
            ItemStack[] opts = inputs.get(i).getItems();
            if (opts.length == 0) {
                return ItemStack.EMPTY;
            }
            ItemStack s = opts[(int) (time / 20) % opts.length].copy();
            s.setCount(Math.max(1, counts.get(i)));
            return s;
        }

        private int gx(int x) {
            int total = cols() * 18 + 34 + 18 * Math.min(3, outputs.size());
            return x + (PAGE_W - total) / 2;
        }

        @Override
        void render(Ctx c, int x, int y) {
            if (missing) {
                c.g.drawString(font, Component.translatable("gui.spacebreaker.guide.no_recipe", id.toString()), x, y, 0xA03020, false);
                return;
            }
            int gx = gx(x), gh = rows() * 18;
            int top = y + Math.max(0, (36 - gh) / 2);
            for (int i = 0; i < cols() * rows(); i++) {
                int sx = gx + (i % cols()) * 18, sy = top + (i / cols()) * 18;
                if (inFluid.containsKey(i)) {
                    fluidSlot(c, sx, sy, inFluid.get(i));
                } else {
                    slot(c, sx, sy, i < inputs.size() ? shown(i, c.time) : ItemStack.EMPTY);
                }
            }
            int ax = gx + cols() * 18 + 4;
            int mid = y + Math.max(gh, 36) / 2;
            // arrow
            for (int i = 0; i < 18; i++) {
                c.g.fill(ax + i, mid + 1, ax + i + 1, mid + 3, 0xFF000000 | INK_SOFT);
            }
            for (int i = 0; i < 4; i++) {
                c.g.fill(ax + 18 + i, mid - 2 + i, ax + 19 + i, mid + 6 - i, 0xFF000000 | INK_SOFT);
            }
            if (!machine.isEmpty()) {
                c.g.pose().pushPose();
                c.g.pose().translate(ax + 5, mid - 14, 0);
                c.g.pose().scale(0.75F, 0.75F, 1F);
                c.g.renderFakeItem(machine, 0, 0);
                c.g.pose().popPose();
                if (c.over(ax + 5, mid - 14, 12, 12)) {
                    c.hoverStack = machine;
                }
            }
            int ox2 = ax + 26;
            for (int i = 0; i < Math.min(3, outputs.size()); i++) {
                if (outFluid.containsKey(i)) {
                    fluidSlot(c, ox2 + i * 18, mid - 8, outFluid.get(i));
                } else {
                    slot(c, ox2 + i * 18, mid - 8, outputs.get(i));
                }
            }
            if (info != null) {
                c.g.drawString(font, info, x + (PAGE_W - font.width(info)) / 2, y + Math.max(gh, 36) + 3, INK_SOFT, false);
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            if (missing) {
                return null;
            }
            int gx = gx(x), gh = rows() * 18;
            int top = y + Math.max(0, (36 - gh) / 2);
            for (int i = 0; i < inputs.size(); i++) {
                int sx = gx + (i % cols()) * 18, sy = top + (i / cols()) * 18;
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    return itemLink(shown(i, minecraft.level == null ? 0 : minecraft.level.getGameTime()));
                }
            }
            int ax = gx + cols() * 18 + 4, mid = y + Math.max(gh, 36) / 2;
            for (int i = 0; i < Math.min(3, outputs.size()); i++) {
                int sx = ax + 26 + i * 18, sy = mid - 8;
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    return itemLink(outputs.get(i));
                }
            }
            return null;
        }
    }

    private final class EntityElem extends Elem {
        final ResourceLocation id;
        final float scale;
        final int h;

        EntityElem(ResourceLocation id, float scale, int h) {
            this.id = id;
            this.scale = scale;
            this.h = h;
        }

        @Override
        int height() {
            return h + 2;
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.fill(x, y, x + PAGE_W, y + h, 0x16000000 | INK);
            Entity e = entities.computeIfAbsent(id, k -> {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(k).orElse(null);
                return type == null || minecraft.level == null ? null : type.create(minecraft.level);
            });
            if (!(e instanceof LivingEntity living)) {
                return;
            }
            float size = Math.max(living.getBbHeight(), living.getBbWidth() * 1.3F);
            float s = Math.min((h - 24F) / size, 40F) * scale;
            Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI).rotateX((float) Math.toRadians(-12))
                    .rotateY((float) Math.toRadians(c.time * 1.2F % 360F));
            living.yBodyRot = 0;
            living.yHeadRot = 0;
            living.setYRot(0);
            living.setXRot(0);
            living.tickCount = (int) c.time;
            InventoryScreen.renderEntityInInventory(c.g, x + PAGE_W / 2F, y + h / 2F + living.getBbHeight() * s / 2F, s,
                    new Vector3f(), rot, null, living);
            if (c.over(x, y, PAGE_W, h)) {
                c.hoverText = List.of(living.getType().getDescription());
            }
        }
    }

    private final class StructureElem extends Elem {
        final ResourceLocation id;

        StructureElem(ResourceLocation id) {
            this.id = id;
        }

        boolean projectable() {
            return GuideStructures.get(id).multiblock();
        }

        @Override
        int height() {
            return projectable() ? 112 : 96;
        }

        private Component label() {
            return Component.translatable("gui.spacebreaker.guide.show_hologram");
        }

        @Override
        void render(Ctx c, int x, int y) {
            c.g.fill(x, y, x + PAGE_W, y + 94, 0x16000000 | INK);
            GuideStructures.render(c.g, GuideStructures.get(id), x + 2, y + 2, PAGE_W - 4, 90, c.time);
            if (projectable()) {
                int w = font.width(label()) + 12, bx = x + (PAGE_W - w) / 2, by = y + 97;
                boolean hot = c.over(bx, by, w, 13);
                c.g.fill(bx, by, bx + w, by + 13, 0xFF000000 | (hot ? 0x2A8CFF : 0x1C4F8F));
                c.g.drawString(font, label(), bx + 6, by + 3, 0xFFFFFF, false);
                if (hot) {
                    c.hoverText = List.of(Component.translatable(holdsProjector() ? "gui.spacebreaker.guide.show_hologram.hint"
                            : "message.spacebreaker.hologram.need_projector").withStyle(ChatFormatting.GRAY));
                }
            }
        }

        @Override
        @Nullable String click(int x, int y, double mx, double my) {
            int w = font.width(label()) + 12, bx = x + (PAGE_W - w) / 2, by = y + 97;
            if (projectable() && mx >= bx && mx < bx + w && my >= by && my < by + 13) {
                return LINK_HOLOGRAM + id;
            }
            return null;
        }
    }

    static final String LINK_HOLOGRAM = "hologram:";

    private boolean holdsProjector() {
        var p = minecraft.player;
        return p != null && (p.getMainHandItem().getItem() instanceof com.pizzaparker07.spacebreaker.content.blueprint.HologramProjectorItem
                || p.getOffhandItem().getItem() instanceof com.pizzaparker07.spacebreaker.content.blueprint.HologramProjectorItem);
    }

    /** Selects the structure on the projector in hand, or opens the projector's chapter when there is none. */
    private void showHologram(ResourceLocation id) {
        if (holdsProjector()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.pizzaparker07.spacebreaker.content.blueprint.BlueprintPayload(
                    com.pizzaparker07.spacebreaker.content.blueprint.BlueprintPayload.SELECT, id));
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable("gui.spacebreaker.guide.hologram_selected",
                        Component.translatable("multiblock." + id.getNamespace() + "." + id.getPath())), true);
            }
            onClose();
            return;
        }
        ResourceLocation ch = GuideBook.INSTANCE.chapterForItem(Spacebreaker.id("hologram_projector"));
        if (ch != null) {
            openChapter(ch, 0);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.spacebreaker.hologram.need_projector"), true);
        }
    }

    private final class ImageElem extends Elem {
        final GuideMarkdown.Image im;

        ImageElem(GuideMarkdown.Image im) {
            this.im = im;
        }

        @Override
        int height() {
            int w = Math.min(im.width(), PAGE_W);
            return im.height() * w / Math.max(1, im.width());
        }

        @Override
        void render(Ctx c, int x, int y) {
            int w = Math.min(im.width(), PAGE_W);
            int h = height();
            c.g.blit(im.texture(), x + (PAGE_W - w) / 2, y, w, h, 0, 0, im.width(), im.height(), im.width(), im.height());
        }
    }
}
