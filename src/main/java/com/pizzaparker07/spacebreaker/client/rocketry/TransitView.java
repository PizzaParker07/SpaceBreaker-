package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The transit view: a full-screen animation (GUI layer, so it stays on top of any shader pack) that carries
 * the crew between dimensions. It fades in while the rocket climbs out or coasts, fills the screen while the
 * next dimension loads ({@link TransitScreen}) and then fades into the new world. Timing comes from
 * {@link Transit}.
 */
public final class TransitView {
    private static final Map<String, Integer> AIR = Map.of(
            "earth", 0x6E9FD8, "mars", 0xC98A5E, "venus", 0xD9C07A, "titan", 0xC9A25A);
    /** Ground colour seen from high up, for planets too big on screen to draw from their small disc picture. */
    private static final Map<String, Integer> GROUND = Map.ofEntries(
            Map.entry("earth", 0x2F5F8E), Map.entry("moon", 0x8E8D88), Map.entry("mars", 0xB2643C), Map.entry("venus", 0xD9C48A),
            Map.entry("mercury", 0x8C847A), Map.entry("phobos", 0x6E6258), Map.entry("deimos", 0x7A6E62), Map.entry("io", 0xD8C25A),
            Map.entry("europa", 0xCDBFA8), Map.entry("ganymede", 0x8F877C), Map.entry("callisto", 0x6A6158), Map.entry("titan", 0xC08A3E),
            Map.entry("ceres", 0x77736C), Map.entry("vesta", 0x8A8478));

    @Nullable
    private static Transit.Kind kind;
    @Nullable
    private static Location from;
    @Nullable
    private static Location to;
    private static long started = -1;
    private static long arrived = -1;
    private static float lead;
    private static boolean holding;
    private static boolean pinned;

    private TransitView() {
    }

    /** Called every client tick while the player rides a rocket. */
    static void update(RocketEntity rocket) {
        if (pinned) {
            return;
        }
        FlightState s = rocket.state();
        float l = Transit.leadIn(s, rocket.timer(), rocket.ascentProgress());
        if (arrived >= 0) {
            return; // fading out into the new world
        }
        if (l > 0 && kind == null) {
            Location here = rocket.location();
            Location dest = s == FlightState.ASCENT ? RocketEntity.launchTarget(here) : rocket.destination();
            if (dest != null) {
                begin(Transit.kind(here, dest), here, dest);
            }
        }
        lead = l;
        if (l <= 0 && !holding) {
            reset(); // aborted (failed ascent, cancelled travel)
        }
    }

    static void begin(Transit.Kind k, @Nullable Location here, @Nullable Location dest) {
        kind = k;
        from = here;
        to = dest;
        started = System.nanoTime();
        arrived = -1;
    }

    /** The level-loading screen is showing the view: hold it at full strength. */
    static void hold() {
        holding = true;
    }

    /** The new dimension is there: start fading out. */
    static void arrived() {
        if (kind != null && arrived < 0) {
            arrived = System.nanoTime();
            holding = false;
        }
    }

    static boolean holding() {
        return holding;
    }

    static boolean active() {
        return kind != null;
    }

    /** Shows (or with a null kind, clears) a view that stays on screen until cleared: for screenshots and previews. */
    public static void showcase(@Nullable Transit.Kind k, @Nullable Location here, @Nullable Location dest) {
        reset();
        if (k != null) {
            begin(k, here, dest);
            pinned = true;
        }
    }

    private static void reset() {
        pinned = false;
        kind = null;
        from = null;
        to = null;
        started = -1;
        arrived = -1;
        lead = 0;
        holding = false;
    }

    private static float seconds(long since) {
        return (System.nanoTime() - since) / 1e9F;
    }

    /** Current strength of the view (0 when it is not shown). */
    static float alpha() {
        if (kind == null) {
            return 0;
        }
        if (arrived >= 0) {
            float a = Transit.fadeOut(seconds(arrived));
            if (a <= 0) {
                reset();
            }
            return a;
        }
        return holding || pinned ? 1F : lead;
    }

    @Nullable
    static Location destination() {
        return to;
    }

    // ------------------------------------------------------------------ drawing

    static void render(GuiGraphics g, int w, int h, float alpha) {
        if (kind == null || alpha <= 0.003F) {
            return;
        }
        float time = seconds(started);
        switch (kind) {
            case ASCENT -> ascent(g, w, h, alpha, Transit.progress(time, 5F), time);
            case TRANSFER -> transfer(g, w, h, alpha, Transit.progress(time, 7F), time);
            case LANDING -> landing(g, w, h, alpha, Transit.progress(time, 7F), time);
        }
    }

    private static int argb(float a, int rgb) {
        return ((int) (Mth.clamp(a, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerpRgb(float t, int a, int b) {
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
        int gg = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
        int bb = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | gg << 8 | bb;
    }

    static int airColour(@Nullable Location l) {
        if (l == null || !l.atmosphere()) {
            return 0x000000;
        }
        return AIR.getOrDefault(l.body().getPath(), 0x9AA6B8);
    }

    private static void stars(GuiGraphics g, int w, int h, float alpha, float twinkle) {
        RandomSource rng = RandomSource.create(7331);
        for (int i = 0; i < 220; i++) {
            int x = rng.nextInt(Math.max(1, w));
            int y = rng.nextInt(Math.max(1, h));
            float b = 0.35F + rng.nextFloat() * 0.65F;
            b *= 0.85F + 0.15F * Mth.sin(twinkle * (1 + rng.nextFloat() * 2) + i);
            int size = rng.nextFloat() < 0.08F ? 2 : 1;
            int v = (int) (b * 255);
            g.fill(x, y, x + size, y + size, argb(alpha * b, v << 16 | v << 8 | v));
        }
    }

    private static void disc(GuiGraphics g, ResourceLocation body, float cx, float cy, float size, float alpha) {
        ResourceLocation tex = Location.MOON.equals(body) ? OrbitSkyEffects.MOON_DISC : OrbitSkyEffects.disc(body);
        int s = Math.max(1, (int) size);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, Mth.clamp(alpha, 0, 1));
        g.blit(tex, (int) (cx - s / 2F), (int) (cy - s / 2F), s, s, 0F, 0F, 1, 1, 1, 1);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    static int groundColour(@Nullable Location l) {
        return l == null ? 0x6A6A70 : GROUND.getOrDefault(l.body().getPath(), 0x7A7670);
    }

    /**
     * A planet too big to draw from its disc picture: its surface map wrapped row by row (rows near the limb
     * are squeezed like on a sphere), clouds on Earth, darker towards the bottom, with a halo of air where it
     * has some. Only the rows on screen are drawn.
     *
     * @param u0    longitude at the centre, as a share of the map width
     * @param v0    map row (share of the height) at the top of the disc
     * @param vSpan share of the map height the whole disc covers
     */
    private static void bigPlanet(GuiGraphics g, int w, int h, float cx, float cy, float r, Location body, float u0, float v0,
                                  float vSpan, float alpha) {
        int air = airColour(body);
        if ((air & 0xFFFFFF) != 0) {
            for (int i = 3; i >= 1; i--) {
                fillCircle(g, w, h, cx, cy, r + i * 5, argb(alpha * 0.22F, lerpRgb(0.25F, air, 0xFFFFFF)));
            }
        }
        fillCircle(g, w, h, cx, cy, r, argb(alpha, groundColour(body)));
        mapRows(g, w, h, cx, cy, r, OrbitSkyEffects.surface(body.body()), u0, v0, vSpan, alpha);
        if (body.isEarth()) {
            mapRows(g, w, h, cx, cy, r, OrbitSkyEffects.EARTH_CLOUDS, u0 + 0.13F, v0, vSpan, alpha * 0.75F);
        }
        // night side towards the bottom, a little haze at the limb
        int y0 = Math.max(0, (int) (cy - r)), y1 = Math.min(h, (int) (cy + r) + 1);
        for (int y = y0; y < y1; y++) {
            float dy = y + 0.5F - cy;
            float half = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            float depth = Mth.clamp((dy + r) / Math.min(2 * r, h * 1.2F), 0, 1);
            int x0 = Math.max(0, (int) (cx - half)), x1 = Math.min(w, (int) (cx + half) + 1);
            if (depth > 0.25F) {
                g.fill(x0, y, x1, y + 1, argb(alpha * (depth - 0.25F) * 0.6F, 0x000000));
            }
            if ((air & 0xFFFFFF) != 0 && depth < 0.08F) {
                g.fill(x0, y, x1, y + 1, argb(alpha * (0.08F - depth) * 4F, lerpRgb(0.4F, air, 0xFFFFFF)));
            }
        }
    }

    private static final int MAP_W = 4096, MAP_H = 2048;

    private static void mapRows(GuiGraphics g, int w, int h, float cx, float cy, float r, ResourceLocation tex, float u0, float v0,
                                float vSpan, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, Mth.clamp(alpha, 0, 1));
        int y0 = Math.max(0, (int) (cy - r)), y1 = Math.min(h, (int) (cy + r) + 1);
        for (int y = y0; y < y1; y++) {
            float dy = y + 0.5F - cy;
            float s = Mth.clamp(dy / r, -1, 1);
            float half = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            if (half < 1) {
                continue;
            }
            // latitude from the sphere, so rows bunch up towards the limb
            float lat = (float) ((Math.asin(s) + Math.PI / 2) / Math.PI);
            int v = (int) ((v0 + lat * vSpan) * MAP_H) % MAP_H;
            int x0 = Math.max(0, (int) (cx - half)), x1 = Math.min(w, (int) (cx + half) + 1);
            // the visible chord covers half the globe's longitudes at the equator, less near the poles
            float uSpan = 0.5F * MAP_W * (half / r);
            float uLeft = u0 * MAP_W - uSpan / 2 + (x0 - (cx - half)) / (2 * half) * uSpan;
            int uw = Math.max(1, Math.round(uSpan * (x1 - x0) / (2 * half)));
            int u = Math.floorMod((int) uLeft, MAP_W - uw);
            g.blit(tex, x0, y, x1 - x0, 1, u, v, uw, 1, MAP_W, MAP_H);
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void fillCircle(GuiGraphics g, int w, int h, float cx, float cy, float r, int colour) {
        int y0 = Math.max(0, (int) (cy - r)), y1 = Math.min(h, (int) (cy + r) + 1);
        for (int y = y0; y < y1; y++) {
            float dy = y + 0.5F - cy;
            float half = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            if (half > 0) {
                g.fill(Math.max(0, (int) (cx - half)), y, Math.min(w, (int) (cx + half) + 1), y + 1, colour);
            }
        }
    }

    private static void ascent(GuiGraphics g, int w, int h, float alpha, float p, float time) {
        int air = airColour(from);
        int top = lerpRgb(Mth.clamp(p * 1.6F, 0, 1), lerpRgb(0.35F, air, 0x000000), 0x000000);
        int bottom = lerpRgb(Mth.clamp(p * 1.1F, 0, 1), air, 0x02040A);
        g.fillGradient(0, 0, w, h, argb(alpha, top), argb(alpha, bottom));
        stars(g, w, h, alpha * Mth.clamp((p - 0.2F) / 0.5F, 0, 1), time);
        if (from != null) {
            // the planet curves away below: a huge body whose rim bends more and sinks as the rocket climbs
            float r = Mth.lerp(p, w * 3.5F, w * 0.9F);
            float topEdge = Mth.lerp(p, h * 0.6F, h * 0.7F);
            bigPlanet(g, w, h, w / 2F, topEdge + r, r, from, 0.32F + time * 0.004F, 0.18F, 0.7F, alpha);
        }
    }

    private static void transfer(GuiGraphics g, int w, int h, float alpha, float p, float time) {
        g.fill(0, 0, w, h, argb(alpha, 0x01020A));
        // stars stream outwards from the point ahead
        RandomSource rng = RandomSource.create(4242);
        float cx = w / 2F, cy = h / 2F;
        float maxR = (float) Math.hypot(cx, cy);
        for (int i = 0; i < 180; i++) {
            float angle = rng.nextFloat() * Mth.TWO_PI;
            float phase = rng.nextFloat();
            float speed = 0.12F + rng.nextFloat() * 0.18F;
            float f = (phase + time * speed) % 1F;
            float r = f * f * maxR;
            float len = 2 + f * f * 26;
            float b = Mth.clamp(f * 1.6F, 0, 1) * (0.5F + rng.nextFloat() * 0.5F);
            float dx = Mth.cos(angle), dy = Mth.sin(angle);
            int steps = Math.max(1, (int) (len / 2));
            for (int k = 0; k < steps; k++) {
                float rr = r + k * 2;
                int x = (int) (cx + dx * rr), y = (int) (cy + dy * rr);
                int v = (int) (b * (1 - k / (float) steps) * 255);
                g.fill(x, y, x + 1, y + 1, argb(alpha, v << 16 | v << 8 | v));
            }
        }
        if (to != null) {
            float size = Mth.lerp(p * p, 6F, h * 0.75F);
            disc(g, to.body(), cx, cy, size, alpha);
        }
    }

    private static void landing(GuiGraphics g, int w, int h, float alpha, float p, float time) {
        g.fill(0, 0, w, h, argb(alpha, 0x01020A));
        stars(g, w, h, alpha * (1 - p), time);
        if (to == null) {
            return;
        }
        float size = Mth.lerp(p * p * p, h * 0.55F, w * 4.5F);
        float cy = h / 2F + size * 0.32F;
        // the picture while it is small, a smooth body once it would turn blocky
        float swap = Mth.clamp((size - h * 0.9F) / (h * 0.5F), 0, 1);
        if (swap < 1) {
            disc(g, to.body(), w / 2F, cy, size, alpha * (1 - swap));
        }
        if (swap > 0) {
            bigPlanet(g, w, h, w / 2F, cy, size / 2F, to, 0.55F, 0.0F, 1.0F, alpha * swap);
        }
        if (to.atmosphere()) {
            int air = airColour(to);
            float haze = Mth.clamp((p - 0.6F) / 0.35F, 0, 1);
            g.fill(0, 0, w, h, argb(alpha * haze * 0.45F, air));
            float heat = Mth.clamp((p - 0.62F) / 0.25F, 0, 1) * 0.7F;
            if (heat > 0) {
                RocketHud.plasma(g, w, h, heat * alpha, time * 20F);
            }
        }
    }

    /** Caption under the view: where the rocket is going. */
    static void caption(GuiGraphics g, int w, int h, float alpha) {
        if (to == null || alpha < 0.3F) {
            return;
        }
        Component text = Component.translatable("hud.spacebreaker.coasting", to.name());
        g.drawCenteredString(Minecraft.getInstance().font, text, w / 2, h - 34, argb(alpha * 0.9F, 0xDDE6F0));
    }
}
