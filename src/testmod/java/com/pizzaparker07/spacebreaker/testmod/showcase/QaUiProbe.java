package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * QA screen probe (showcase runs whose shot prefix starts with {@code qa_ui}): before a shot with a screen open is
 * taken, the screen is drawn again with the mouse on a grid over the whole screen and every tooltip that shows is
 * recorded (text, where it showed, width). Then the real mouse is put on each tooltip in turn and a screenshot taken
 * ({@code <prefix>_<shot>_tipN.png}). A prefix containing {@code _tr} switches the game to Turkish first. Results go to
 * {@code run-showcase/qa_ui/<prefix>_<shot>.tsv}; raw translation keys and tooltips wider than the screen are flagged.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class QaUiProbe {
    private static final String PREFIX = System.getProperty("spacebreaker.shotPrefix", "");
    private static final int STEP = 8;
    private static final int MAX_TIPS = 8;
    private static final Pattern RAW_KEY = Pattern.compile("^[a-z0-9_]+(\\.[a-z0-9_]+){2,}$");

    private enum State { SWEEP, HOVER, REST, DONE }

    private static String current = "";
    private static State state = State.DONE;
    private static int row;
    private static int wait;
    /** Frames drawn since the mouse moved: ticks can run several in a row without a frame between them. */
    private static int frames;
    private static int tip;
    private static boolean languageAsked;
    private static boolean sweeping;
    private static int sweepX, sweepY;
    private static final Map<String, Found> found = new LinkedHashMap<>();
    private static List<Found> tips = List.of();

    private record Point(int x, int y) {
    }

    private static final class Found {
        final String text;
        final int width;
        final int screenWidth;
        final List<Point> at = new ArrayList<>();

        Found(String text, int width, int screenWidth) {
            this.text = text;
            this.width = width;
            this.screenWidth = screenWidth;
        }

        Point middle() {
            return at.get(at.size() / 2);
        }

        String flags() {
            List<String> f = new ArrayList<>();
            for (String line : text.split(" \\| ")) {
                if (RAW_KEY.matcher(line.trim()).matches()) {
                    f.add("RAW_KEY");
                }
            }
            if (width > screenWidth - 8) {
                f.add("WIDER_THAN_SCREEN");
            }
            return String.join(",", f);
        }
    }

    private QaUiProbe() {
    }

    static boolean active() {
        return PREFIX.startsWith("qa_ui");
    }

    /** False while the probe still works on this shot; the driver waits and takes its own shot afterwards. */
    static boolean finished(Minecraft mc, String shot) {
        if (!active()) {
            return true;
        }
        if (PREFIX.contains("_tr") && !languageAsked) {
            languageAsked = true;
            if (!"tr_tr".equals(mc.getLanguageManager().getSelected())) {
                mc.getLanguageManager().setSelected("tr_tr");
                mc.options.languageCode = "tr_tr";
                mc.reloadResourcePacks();
                return false;
            }
        }
        if (mc.getOverlay() != null) {
            return false;
        }
        if (mc.screen == null) {
            return true;
        }
        if (!shot.equals(current)) {
            current = shot;
            state = State.SWEEP;
            row = 0;
            found.clear();
            return false;
        }
        switch (state) {
            case SWEEP -> {
                return false;
            }
            case HOVER -> {
                if (tip >= tips.size()) {
                    write(mc, shot);
                    mouse(mc, 1, 1);
                    state = State.REST;
                    wait = 0;
                    return false;
                }
                if (wait == 0) {
                    Point p = tips.get(tip).middle();
                    mouse(mc, p.x(), p.y());
                    frames = 0;
                }
                if (++wait > 4 && frames >= 3) {
                    String file = PREFIX + "_" + shot + "_tip" + (tip + 1) + ".png";
                    Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> ShowcaseDriver.log("saved " + file));
                    ShowcaseDriver.log("qa_ui " + shot + " tip" + (tip + 1) + ": " + tips.get(tip).text);
                    tip++;
                    wait = 0;
                }
                return false;
            }
            case REST -> {
                if (wait++ == 0) {
                    frames = 0;
                }
                if (frames >= 3) {
                    state = State.DONE;
                }
                return state == State.DONE;
            }
            default -> {
                return true;
            }
        }
    }

    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        frames++;
        if (!active() || state != State.SWEEP || sweeping) {
            return;
        }
        var screen = event.getScreen();
        var g = event.getGuiGraphics();
        long until = System.nanoTime() + 150_000_000L;
        sweeping = true;
        try {
            while (System.nanoTime() < until && row * STEP < screen.height) {
                sweepY = row * STEP + STEP / 2;
                for (int x = STEP / 2; x < screen.width; x += STEP) {
                    sweepX = x;
                    screen.renderWithTooltip(g, x, sweepY, event.getPartialTick());
                }
                row++;
            }
        } finally {
            sweeping = false;
        }
        if (row * STEP >= screen.height) {
            // tooltips that contain numbers first: the numbers moved off the screens should be found there
            List<Found> all = new ArrayList<>(found.values());
            all.sort((a, b) -> Boolean.compare(!a.text.matches(".*\\d.*"), !b.text.matches(".*\\d.*")));
            tips = all.subList(0, Math.min(MAX_TIPS, all.size()));
            tip = 0;
            wait = 0;
            state = State.HOVER;
            ShowcaseDriver.log("qa_ui " + current + ": " + found.size() + " different tooltips on a " + STEP + " px grid");
        }
    }

    @SubscribeEvent
    static void onTooltip(RenderTooltipEvent.Pre event) {
        if (!sweeping) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int width = 0;
        for (ClientTooltipComponent c : event.getComponents()) {
            width = Math.max(width, c.getWidth(event.getFont()));
            if (c instanceof ClientTextTooltip t) {
                if (!sb.isEmpty()) {
                    sb.append(" | ");
                }
                sb.append(text(t));
            }
        }
        String key = sb.toString().strip();
        if (!key.isEmpty()) {
            final int w = width;
            // tooltips that only differ in their numbers (an energy bar that drains) count as one
            found.computeIfAbsent(key.replaceAll("\\d+", "#"), k -> new Found(key, w, event.getScreenWidth())).at.add(new Point(sweepX, sweepY));
        }
        event.setCanceled(true);
    }

    private static Field textField;

    private static String text(ClientTextTooltip t) {
        try {
            if (textField == null) {
                for (Field f : ClientTextTooltip.class.getDeclaredFields()) {
                    if (f.getType() == FormattedCharSequence.class) {
                        f.setAccessible(true);
                        textField = f;
                    }
                }
            }
            FormattedCharSequence seq = (FormattedCharSequence) textField.get(t);
            StringBuilder sb = new StringBuilder();
            seq.accept((i, style, cp) -> {
                sb.appendCodePoint(cp);
                return true;
            });
            return sb.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "?";
        }
    }

    private static Field mx, my;

    /** Puts the mouse at a GUI position (what the screen receives as mouseX/mouseY on the next frames). */
    private static void mouse(Minecraft mc, int guiX, int guiY) {
        try {
            if (mx == null) {
                mx = MouseHandler.class.getDeclaredField("xpos");
                my = MouseHandler.class.getDeclaredField("ypos");
                mx.setAccessible(true);
                my.setAccessible(true);
            }
            var w = mc.getWindow();
            mx.setDouble(mc.mouseHandler, (guiX + 0.5) * w.getScreenWidth() / w.getGuiScaledWidth());
            my.setDouble(mc.mouseHandler, (guiY + 0.5) * w.getScreenHeight() / w.getGuiScaledHeight());
        } catch (ReflectiveOperationException e) {
            ShowcaseDriver.log("qa_ui: cannot move the mouse: " + e);
        }
    }

    private static void write(Minecraft mc, String shot) {
        StringBuilder sb = new StringBuilder("tooltip\tshown_at_points\tfirst_x\tfirst_y\twidth\tscreen_width\tflags\n");
        for (Found f : found.values()) {
            sb.append(String.format(Locale.ROOT, "%s\t%d\t%d\t%d\t%d\t%d\t%s%n", f.text.replace('\t', ' '), f.at.size(),
                    f.at.get(0).x(), f.at.get(0).y(), f.width, f.screenWidth, f.flags()));
        }
        try {
            Path dir = mc.gameDirectory.toPath().resolve("qa_ui");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(PREFIX + "_" + shot + ".tsv"), sb.toString());
        } catch (IOException e) {
            ShowcaseDriver.log("qa_ui: cannot write the tooltip list: " + e);
        }
    }
}
