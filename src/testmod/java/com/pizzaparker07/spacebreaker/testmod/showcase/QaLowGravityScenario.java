package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * QA (2026-11-08): the suit team's low-gravity changes, tried with real key presses (player movement is worked out on
 * the client, so the keys are held down there). On the Moon and, for comparison, on Earth: a sprint jump (distance,
 * air time) and the same jump with the back key held in the air (how much the air steering takes off); on Mars and
 * on Earth an elytra glide from 60 blocks up at 15° down (distance covered while dropping 60 blocks), and on the Moon
 * whether the wings open at all; a 3-block drop on the Moon and on Earth with the landing's particles and sounds.
 * Every result is a log line "[showcase] lowg: …" with tick numbers.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class QaLowGravityScenario implements ShowcaseScenario {
    private static final int Y = 200;
    private static final int HIGH = 300;

    private enum Kind { JUMP, JUMP_BACK, GLIDE, DROP }

    private static Kind kind;
    private static String label = "";
    private static int t = -1;
    private static boolean airborne;
    private static Vec3 takeoff;
    private static double takeoffSpeed;
    private static boolean sprintAtTakeoff;
    private static int takeoffTick;
    private static boolean done = true;
    private static final List<String> sounds = new ArrayList<>();
    private static int maxParticles;

    /** Server side: does the fall event fire for the landing, and with what distance? */
    @EventBusSubscriber(modid = Spacebreaker.MOD_ID)
    static final class ServerSide {
        @SubscribeEvent
        static void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
            if (event.getEntity() instanceof ServerPlayer p) {
                ShowcaseDriver.log(String.format(Locale.ROOT, "lowg: server fall event in %s: distance %.2f, multiplier %.2f, tick %d",
                        p.level().dimension().location(), event.getDistance(), event.getDamageMultiplier(), p.level().getGameTime()));
            }
        }

        private ServerSide() {
        }
    }

    @Override
    public void build(ServerLevel level) {
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<Shot> s = new ArrayList<>();
        for (var w : List.of(new Object[]{"earth", Level.OVERWORLD}, new Object[]{"moon", Location.MOON_SURFACE.dimension()})) {
            String name = (String) w[0];
            @SuppressWarnings("unchecked") ResourceKey<Level> dim = (ResourceKey<Level>) w[1];
            s.add(Shot.stay("jump_" + name, 6000, true, fp, 260, p -> runway(p, dim, false), () -> start(Kind.JUMP, name)));
            s.add(Shot.stay("jump_back_" + name, 6000, true, fp, 260, p -> runway(p, dim, false), () -> start(Kind.JUMP_BACK, name)));
            s.add(Shot.stay("drop_" + name, 6000, true, fp, 140, p -> runway(p, dim, true), () -> start(Kind.DROP, name)));
        }
        for (var w : List.of(new Object[]{"earth", Level.OVERWORLD}, new Object[]{"mars", MarsModule.MARS},
                new Object[]{"moon", Location.MOON_SURFACE.dimension()})) {
            String name = (String) w[0];
            @SuppressWarnings("unchecked") ResourceKey<Level> dim = (ResourceKey<Level>) w[1];
            s.add(Shot.stay("glide_" + name, 6000, true, fp, 400, p -> high(p, dim), () -> start(Kind.GLIDE, name)));
        }
        return s;
    }

    /** A 3-wide, 60-long stone strip at y 200 with air above, the player at its start (or 3 blocks above it). */
    private static void runway(ServerPlayer p, ResourceKey<Level> dim, boolean above) {
        ServerLevel level = p.server.getLevel(dim);
        if (level == null) {
            ShowcaseDriver.log("lowg: no world " + dim.location());
            return;
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 60; z++) {
                level.setBlock(new BlockPos(x, Y, z), Math.abs(x) <= 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = Y + 1; y <= Y + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        prepare(p, level, 0.5, Y + 1 + (above ? 3 : 0), 0.5);
    }

    private static void high(ServerPlayer p, ResourceKey<Level> dim) {
        ServerLevel level = p.server.getLevel(dim);
        if (level == null) {
            ShowcaseDriver.log("lowg: no world " + dim.location());
            return;
        }
        level.setBlock(new BlockPos(0, HIGH - 1, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        prepare(p, level, 0.5, HIGH, 0.5);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
    }

    private static void prepare(ServerPlayer p, ServerLevel level, double x, double y, double z) {
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.setNoGravity(false);
        p.getFoodData().setFoodLevel(20);
        p.getInventory().clearContent();
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        p.teleportTo(level, x, y, z, 0F, 0F); // facing south, along the strip
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    private static void start(Kind k, String name) {
        release();
        kind = k;
        label = k.name().toLowerCase(Locale.ROOT) + " " + name;
        t = 0;
        airborne = false;
        done = false;
        sounds.clear();
    }

    private static void release() {
        var o = Minecraft.getInstance().options;
        for (KeyMapping key : new KeyMapping[]{o.keyUp, o.keyDown, o.keySprint, o.keyJump, o.keyLeft, o.keyRight}) {
            key.setDown(false);
        }
    }

    @SubscribeEvent
    static void onSound(PlaySoundEvent event) {
        if (done || kind != Kind.DROP || event.getSound() == null) {
            return;
        }
        sounds.add(String.format(Locale.ROOT, "%s vol %.2f pitch %.2f", event.getSound().getLocation(),
                field(event.getSound(), "volume"), field(event.getSound(), "pitch")));
    }

    private static float field(Object sound, String name) {
        try {
            Field f = AbstractSoundInstance.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getFloat(sound);
        } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException e) {
            return Float.NaN;
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var pl = mc.player;
        if (done || pl == null || mc.level == null) {
            return;
        }
        var o = mc.options;
        t++;
        // settle: chunks in, standing on the strip (or hanging in the air for the glide, which starts falling at once)
        if (t < 40 && kind != Kind.DROP) {
            return;
        }
        int tick = (int) mc.level.getGameTime();
        switch (kind) {
            case JUMP, JUMP_BACK -> {
                if (t == 40) {
                    pl.setYRot(0F);
                    o.keyUp.setDown(true);
                    o.keySprint.setDown(true);
                }
                if (t == 70) {
                    o.keyJump.setDown(true);
                }
                if (t == 72) {
                    o.keyJump.setDown(false);
                }
                if (!airborne && t > 70 && !pl.onGround()) {
                    airborne = true;
                    takeoff = pl.position();
                    takeoffSpeed = pl.getDeltaMovement().horizontalDistance();
                    sprintAtTakeoff = pl.isSprinting();
                    takeoffTick = tick;
                    if (kind == Kind.JUMP_BACK) {
                        o.keyUp.setDown(false);
                        o.keySprint.setDown(false);
                        o.keyDown.setDown(true);
                    }
                } else if (airborne && pl.onGround()) {
                    Vec3 land = pl.position();
                    double dist = Math.hypot(land.x - takeoff.x, land.z - takeoff.z);
                    int air = tick - takeoffTick;
                    ShowcaseDriver.log(String.format(Locale.ROOT, "lowg: %s: took off at tick %d at %.2f b/t, landed at tick %d: %.2f blocks in %d ticks, "
                                    + "landing speed %.2f b/t (%s), sprinting at take-off %s, at landing %s", label, takeoffTick, takeoffSpeed, tick, dist, air,
                            pl.getDeltaMovement().horizontalDistance(), kind == Kind.JUMP_BACK ? "back key held in the air" : "forward held",
                            sprintAtTakeoff, pl.isSprinting()));
                    finish();
                } else if (t > 240) {
                    ShowcaseDriver.log("lowg: " + label + ": no jump/landing seen (airborne " + airborne + ", on ground " + pl.onGround() + ")");
                    finish();
                }
            }
            case DROP -> {
                if (!airborne && !pl.onGround()) {
                    airborne = true;
                    takeoff = pl.position();
                    takeoffTick = tick;
                } else if (airborne && pl.onGround() && takeoff != null) {
                    if (t < 1000) {
                        maxParticles = 0;
                        ShowcaseDriver.log(String.format(Locale.ROOT, "lowg: %s: fell %.2f blocks, landed at tick %d after %d ticks, particles now %s",
                                label, takeoff.y - pl.getY(), tick, tick - takeoffTick, mc.particleEngine.countParticles()));
                        t = 1000; // listen to the landing's sounds for a moment
                    }
                } else if (t > 130) {
                    ShowcaseDriver.log("lowg: " + label + ": no landing seen");
                    finish();
                }
                if (t >= 1000) {
                    try {
                        maxParticles = Math.max(maxParticles, Integer.parseInt(mc.particleEngine.countParticles()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (t >= 1010) {
                    ShowcaseDriver.log("lowg: " + label + ": most particles in the 10 ticks after landing: " + maxParticles);
                    ShowcaseDriver.log("lowg: " + label + ": sounds around the landing: " + (sounds.isEmpty() ? "none" : String.join("; ", sounds)));
                    finish();
                }
            }
            case GLIDE -> {
                // walk off the one-block pillar, then open the wings with a fresh jump press a few ticks into the fall
                if (t == 40) {
                    pl.setYRot(0F);
                    o.keyUp.setDown(true);
                }
                if (!airborne && t > 40 && !pl.onGround()) {
                    airborne = true;
                    takeoff = pl.position();
                    takeoffTick = tick;
                    o.keyUp.setDown(false);
                }
                if (!airborne) {
                    if (t > 200) {
                        ShowcaseDriver.log("lowg: " + label + ": never left the pillar");
                        finish();
                    }
                    return;
                }
                int since = tick - takeoffTick;
                if (since == 5) {
                    o.keyJump.setDown(true);
                }
                if (since == 7) {
                    o.keyJump.setDown(false);
                }
                if (since == 15) {
                    ShowcaseDriver.log("lowg: " + label + ": wings open " + pl.isFallFlying() + " at tick " + tick);
                }
                if (since >= 5) {
                    pl.setXRot(15F);
                }
                if (since > 7 && (pl.getY() <= takeoff.y - 60 || pl.onGround() || t > 380)) {
                    ShowcaseDriver.log(String.format(Locale.ROOT, "lowg: %s: dropped %.1f blocks and covered %.1f blocks in %d ticks (wings open %s)",
                            label, takeoff.y - pl.getY(), Math.hypot(pl.getX() - takeoff.x, pl.getZ() - takeoff.z), since, pl.isFallFlying()));
                    finish();
                }
            }
            default -> {
            }
        }
    }

    private static void finish() {
        release();
        done = true;
    }
}
