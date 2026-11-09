package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Micrometeorite showers on airless worlds: with no air to burn them up, grains of dust and sand hit the ground at
 * many kilometres per second. Now and then a shower passes: a warning a minute ahead on the action bar, bright
 * streaks across the sky, puffs of dust where grains strike, and a player standing in the open takes small hits that
 * also wear their armour (at most three hearts per shower, half as much in a sealed suit). Any roof overhead (a pressurised module, a hut, a cave or lava tube) keeps you safe. Rare,
 * short and never locking you in; can be switched off in the server config.
 */
public final class MeteorShowers {
    public static final ResourceKey<DamageType> MICROMETEORITE = ResourceKey.create(Registries.DAMAGE_TYPE, Spacebreaker.id("micrometeorite"));
    /** Warning time before a shower. */
    public static final int WARNING = 1200;
    /** Damage per hit (half a heart) and suit wear per hit. */
    public static final float DAMAGE = 1F;
    public static final int WEAR = 3;
    /** Chance per tick that an exposed player is hit during a shower. */
    public static final float HIT_CHANCE = 1F / 70F;
    /** A sealed suit takes the edge off: damage per hit with helmet and suit on. */
    public static final float SUITED_DAMAGE = 0.5F;
    /** Most health one shower can take from one player (3 hearts), so it never becomes a death trap. */
    public static final float MAX_PER_SHOWER = 6F;
    /** Showers wear an EVA suit piece at most to this share of its durability: it never leaks from them. */
    public static final double SUIT_WEAR_LIMIT = 0.75;
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();

    /** Per-level schedule: the next shower starts at {@code start} and lasts {@code duration}. */
    static final class State {
        long start = Long.MIN_VALUE;
        int duration;
        boolean warned;
        boolean running;
        boolean manual;
        final Map<java.util.UUID, Float> dealt = new java.util.HashMap<>();
    }

    private MeteorShowers() {
    }

    static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent.class,
                e -> sync(e.getEntity()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class,
                e -> sync(e.getEntity()));
    }

    /** Worlds that get showers: airless planet surfaces (not orbits). */
    public static boolean eligible(Level level) {
        return level instanceof ServerLevel server && server.getChunkSource().getGenerator() instanceof PlanetChunkGenerator
                && Environment.isVacuum(level) && !Bodies.isOrbit(level);
    }

    /** True while a shower is falling on this level. */
    public static boolean active(ServerLevel level) {
        State s = STATES.get(level);
        return s != null && s.running;
    }

    /** Starts a shower right away (command, showcase), lasting {@code duration} ticks. */
    public static void start(ServerLevel level, int duration) {
        State s = STATES.computeIfAbsent(level, l -> new State());
        s.start = level.getGameTime();
        s.duration = duration;
        s.warned = true;
        s.running = false;
        s.manual = true;
        s.dealt.clear();
    }

    /** True if a player is out in the open where grains can reach them. */
    public static boolean exposed(LivingEntity entity) {
        if (entity instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        Level level = entity.level();
        BlockPos eye = BlockPos.containing(entity.getEyePosition());
        // any solid roof overhead (heightmap, so a freshly built roof counts at once)
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, eye.getX(), eye.getZ()) <= eye.getY();
    }

    /** Damage of one hit for this wearer: less in a sealed suit. */
    public static float damageFor(LivingEntity entity) {
        return com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem.isSealed(entity) ? SUITED_DAMAGE : DAMAGE;
    }

    /** One micrometeorite hit: a little damage and wear on one worn armour piece. Returns the damage dealt. */
    public static float strike(LivingEntity entity, RandomSource random) {
        float amount = damageFor(entity);
        DamageSource source = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MICROMETEORITE));
        entity.hurt(source, amount);
        EquipmentSlot[] armour = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = armour[(random.nextInt(4) + i) % 4];
            ItemStack piece = entity.getItemBySlot(slot);
            if (piece.isEmpty() || !piece.isDamageableItem()) {
                continue;
            }
            if (piece.getItem() instanceof com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem) {
                // EVA suits wear through the suit's own model, never far enough to leak from a shower
                com.pizzaparker07.spacebreaker.content.suit.SuitWear.wearPiece(entity, slot, WEAR, SUIT_WEAR_LIMIT);
            } else {
                piece.hurtAndBreak(WEAR, entity, slot);
            }
            break;
        }
        if (entity.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getEyeY(), entity.getZ(), 6, 0.2, 0.2, 0.2, 0.3);
        }
        return amount;
    }

    /** Mean ticks between showers and their length, from the server config. */
    static int interval() {
        return WorldgenConfig.SPEC.isLoaded() ? WorldgenConfig.METEOR_INTERVAL_MINUTES.get() * 1200 : 60 * 1200;
    }

    static int duration() {
        return WorldgenConfig.SPEC.isLoaded() ? WorldgenConfig.METEOR_DURATION_SECONDS.get() * 20 : 40 * 20;
    }

    static boolean enabled() {
        return !WorldgenConfig.SPEC.isLoaded() || WorldgenConfig.METEOR_SHOWERS.get();
    }

    static void tick(ServerLevel level) {
        if (!eligible(level)) {
            return;
        }
        State s = STATES.computeIfAbsent(level, l -> new State());
        long now = level.getGameTime();
        if (s.start == Long.MIN_VALUE) {
            schedule(s, now, level.random);
        }
        if (!enabled() && !s.manual) {
            if (s.running) {
                s.running = false;
                broadcast(level, 0);
            }
            return;
        }
        if (!s.warned && now >= s.start - WARNING) {
            s.warned = true;
            tell(level, Component.translatable("message.spacebreaker.meteor_shower.warning"));
        }
        if (!s.running && now >= s.start && now < s.start + s.duration) {
            s.running = true;
            tell(level, Component.translatable("message.spacebreaker.meteor_shower.start"));
            broadcast(level, (int) (s.start + s.duration - now));
        }
        if (s.running) {
            if (now >= s.start + s.duration) {
                s.running = false;
                tell(level, Component.translatable("message.spacebreaker.meteor_shower.end"));
                broadcast(level, 0);
                schedule(s, now, level.random);
                return;
            }
            for (ServerPlayer p : level.players()) {
                if (exposed(p)) {
                    float dealt = s.dealt.getOrDefault(p.getUUID(), 0F);
                    if (level.random.nextFloat() < HIT_CHANCE && dealt + damageFor(p) <= MAX_PER_SHOWER) {
                        s.dealt.put(p.getUUID(), dealt + strike(p, level.random));
                    }
                    if (level.random.nextInt(4) == 0) {
                        impact(level, p);
                    }
                }
            }
        }
    }

    /** A puff of dust where a grain hits the ground near a player. */
    private static void impact(ServerLevel level, ServerPlayer p) {
        RandomSource r = level.random;
        int x = (int) Math.floor(p.getX()) + r.nextInt(33) - 16;
        int z = (int) Math.floor(p.getZ()) + r.nextInt(33) - 16;
        if (!level.isLoaded(new BlockPos(x, 0, z))) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos ground = new BlockPos(x, y - 1, z);
        var state = level.getBlockState(ground);
        if (state.isAir()) {
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x + 0.5, y + 0.1, z + 0.5, 8, 0.15, 0.05, 0.15, 0.12);
        level.sendParticles(ParticleTypes.SMALL_FLAME, x + 0.5, y + 0.1, z + 0.5, 1, 0.05, 0.05, 0.05, 0.01);
        if (r.nextInt(6) == 0) {
            level.playSound(null, ground.above(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.AMBIENT, 0.3F, 1.6F + r.nextFloat() * 0.3F);
        }
    }

    private static void schedule(State s, long now, RandomSource r) {
        s.start = now + Math.max(WARNING + 200, (long) (interval() * (0.5 + r.nextDouble())));
        s.duration = duration();
        s.warned = false;
        s.running = false;
        s.manual = false;
        s.dealt.clear();
    }

    private static void tell(ServerLevel level, Component msg) {
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(msg, true);
        }
    }

    private static void broadcast(ServerLevel level, int ticksLeft) {
        MeteorShowerPayload payload = new MeteorShowerPayload(level.dimension(), ticksLeft);
        for (ServerPlayer p : level.players()) {
            if (p.connection != null && p.connection.hasChannel(MeteorShowerPayload.TYPE)) {
                PacketDistributor.sendToPlayer(p, payload);
            }
        }
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level && eligible(level)
                && sp.connection.hasChannel(MeteorShowerPayload.TYPE)) {
            State s = STATES.get(level);
            int left = s != null && s.running ? (int) Math.max(0, s.start + s.duration - level.getGameTime()) : 0;
            PacketDistributor.sendToPlayer(sp, new MeteorShowerPayload(level.dimension(), left));
        }
    }
}
