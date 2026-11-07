package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;

/**
 * The planet engine: a data-driven chunk generator and biome source for rocky worlds, terrain profiles,
 * shared worldgen features and per-body solar time. Used by the Moon now, by Mars and procedural worlds later.
 */
public final class WorldgenModule implements SbModule {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Spacebreaker.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<PlanetChunkGenerator>> PLANET_GENERATOR =
            CHUNK_GENERATORS.register("planet", () -> PlanetChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<PlanetBiomeSource>> PLANET_BIOMES =
            BIOME_SOURCES.register("planet", () -> PlanetBiomeSource.CODEC);
    public static final DeferredHolder<Feature<?>, BoulderFeature> BOULDER = FEATURES.register("boulder", BoulderFeature::new);

    @Override
    public String id() {
        return "worldgen";
    }

    @Override
    public void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
        ColdTraps.register();
        Subsurface.register();
        MeteorShowers.register();
        CometNotice.register();
        EclipseState.register();
        BIOME_SOURCES.register(modBus);
        FEATURES.register(modBus);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, WorldgenConfig.SPEC, "spacebreaker-worlds-server.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.pizzaparker07.spacebreaker.client.sky.SkyClient.init();
        }
        modBus.addListener(DataPackRegistryEvent.NewRegistry.class,
                e -> e.dataPackRegistry(TerrainProfile.REGISTRY, TerrainProfile.CODEC, TerrainProfile.CODEC));
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToClient(PlanetTimePayload.TYPE,
                PlanetTimePayload.STREAM_CODEC, (payload, ctx) -> PlanetTime.receive(payload)));
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").playToClient(MeteorShowerPayload.TYPE,
                MeteorShowerPayload.STREAM_CODEC, (payload, ctx) -> ShowerClientState.receive(payload)));

        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                PlanetTime.onLevelLoad(level);
                ColdTraps.bind(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                PlanetTime.onLevelUnload(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                PlanetTime.syncTo(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                PlanetTime.syncTo(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> registerCommands(e));
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("spacebreaker").then(Commands.literal("meteorshower")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    if (!MeteorShowers.eligible(ctx.getSource().getLevel())) {
                        ctx.getSource().sendFailure(Component.translatable("commands.spacebreaker.meteorshower.none"));
                        return 0;
                    }
                    MeteorShowers.start(ctx.getSource().getLevel(), MeteorShowers.duration());
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.spacebreaker.meteorshower.started"), true);
                    return 1;
                })));
        event.getDispatcher().register(Commands.literal("spacebreaker").then(Commands.literal("eclipse").executes(ctx -> {
            double secs = EclipseState.secondsToNext(ctx.getSource().getLevel());
            if (Double.isNaN(secs)) {
                ctx.getSource().sendFailure(Component.translatable("commands.spacebreaker.eclipse.none"));
                return 0;
            }
            if (EclipseState.coverage(ctx.getSource().getLevel()) > 0) {
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.spacebreaker.eclipse.now"), false);
                return 1;
            }
            long s = Math.round(secs);
            String when = s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                    : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.spacebreaker.eclipse.next", when), false);
            return 1;
        })));
        event.getDispatcher().register(Commands.literal("spacebreaker").then(Commands.literal("localtime")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("query").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    double f = PlanetTime.dayFraction(src.getLevel());
                    src.sendSuccess(() -> Component.translatable("commands.spacebreaker.localtime.query",
                            String.format(Locale.ROOT, "%.3f", f), clockText(f)), false);
                    return (int) (f * 1000);
                }))
                .then(setter("sunrise", 0.0)).then(setter("noon", 0.25)).then(setter("sunset", 0.5)).then(setter("midnight", 0.75))
                .then(Commands.literal("set").then(Commands.argument("fraction", DoubleArgumentType.doubleArg(0, 1))
                        .executes(ctx -> set(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "fraction")))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> setter(String name, double fraction) {
        return Commands.literal(name).executes(ctx -> set(ctx.getSource(), fraction));
    }

    private static int set(CommandSourceStack src, double fraction) {
        if (PlanetTime.clock(src.getLevel()).isEmpty()) {
            src.sendFailure(Component.translatable("commands.spacebreaker.localtime.none"));
            return 0;
        }
        PlanetTime.setDayFraction(src.getLevel(), fraction);
        src.sendSuccess(() -> Component.translatable("commands.spacebreaker.localtime.set", clockText(fraction)), true);
        return 1;
    }

    /** "13:45" style local solar time (0 = sunrise = 06:00). */
    static String clockText(double fraction) {
        double hours = (fraction * 24 + 6) % 24;
        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("commands.spacebreaker.localtime.query", "Local day fraction %s (solar time %s)", "Yerel gün kesri %s (güneş saati %s)");
        lang.add("commands.spacebreaker.localtime.set", "Local solar time set to %s", "Yerel güneş saati %s olarak ayarlandı");
        lang.add("message.spacebreaker.cold_trap.enter", "Permanently shadowed crater: %s °C, the sun never reaches here",
                "Kalıcı gölgeli krater: %s °C, buraya hiç güneş ulaşmaz");
        lang.add("message.spacebreaker.cold_trap.leave", "Out of the cold trap", "Soğuk tuzaktan çıktın");
        lang.add("message.spacebreaker.meteor_shower.warning", "Micrometeorite shower in one minute: get under a roof",
                "Bir dakika içinde mikrometeorit yağmuru: bir çatının altına gir");
        lang.add("message.spacebreaker.meteor_shower.start", "Micrometeorite shower! Grains strike the open ground",
                "Mikrometeorit yağmuru! Açık alana tanecikler çarpıyor");
        lang.add("message.spacebreaker.meteor_shower.end", "The micrometeorite shower has passed", "Mikrometeorit yağmuru geçti");
        lang.add("death.attack.spacebreaker.micrometeorite", "%1$s was caught in a micrometeorite shower", "%1$s mikrometeorit yağmuruna yakalandı");
        lang.add("death.attack.spacebreaker.micrometeorite.player", "%1$s was caught in a micrometeorite shower while fighting %2$s",
                "%1$s, %2$s ile dövüşürken mikrometeorit yağmuruna yakalandı");
        lang.add("commands.spacebreaker.meteorshower.started", "A micrometeorite shower begins", "Mikrometeorit yağmuru başlıyor");
        lang.add("commands.spacebreaker.meteorshower.none", "Only airless planet surfaces get micrometeorite showers",
                "Mikrometeorit yağmuru yalnız havasız gezegen yüzeylerinde olur");
        lang.add("message.spacebreaker.great_comet", "Mission control: great comet %s is in your sky! Look low in the sky after dusk or before dawn",
                "Görev kontrol: büyük kuyruklu yıldız %s gökyüzünde! Akşam karanlığından sonra ya da şafaktan önce ufka yakın bak");
        lang.add("message.spacebreaker.eclipse.soon", "Mission control: in about a minute %s will pass in front of the Sun. Watch its ring of sunsets",
                "Görev kontrol: yaklaşık bir dakika sonra %s Güneş'in önünden geçecek. Gün batımlarından oluşan halkasını izle");
        lang.add("message.spacebreaker.eclipse.now", "Eclipse: %s hides the Sun; solar panels go dark", "Tutulma: %s Güneş'i örtüyor; güneş panelleri kararıyor");
        lang.add("commands.spacebreaker.eclipse.next", "Next eclipse of the Sun in %s (real time)", "Sıradaki Güneş tutulması %s sonra (gerçek zaman)");
        lang.add("commands.spacebreaker.eclipse.now", "An eclipse is happening now", "Şu anda tutulma var");
        lang.add("commands.spacebreaker.eclipse.none", "No eclipse of the Sun from here within a year", "Buradan bir yıl içinde Güneş tutulması yok");
        lang.add("commands.spacebreaker.localtime.none", "This world follows Earth's clock", "Bu dünya Dünya'nın saatini kullanıyor");
    }
}
