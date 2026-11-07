package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game-bus hooks of the suit module: per-player life support, gravity for every living entity. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class SuitEvents {
    private SuitEvents() {
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Gravity.apply(player, Gravity.factorFor(player)); // both sides: movement is predicted on the client
        ZeroG.tickThrust(player);
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        SurvivalData data = LifeSupport.data(player);
        Tether.tick(player);
        if (player.tickCount % 20 == 0 || player.tickCount < 2) {
            data.doseRateMsvDay = Environment.radiation(player.level(), BlockPos.containing(player.getEyePosition()));
            cachedRate(sp, data.doseRateMsvDay);
        }
        LifeSupport.tick(player, LifeSupport.Env.at(player, lastRate(sp)));
        boolean magnetised = Environment.gravityFactor(player.level()) < 1.0 && Gravity.magnetised(player);
        if (magnetised && !data.wasMagnetised) {
            // the boots grip: a short metallic clank
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.7F);
        }
        data.wasMagnetised = magnetised;
        if (player.tickCount % 20 == 0) {
            double g = Environment.gravityFactor(player.level());
            boolean micro = ZeroG.microGravity(g) && !player.isCreative();
            if (micro && !data.wasMicroGravity) {
                ItemStack torso = SuitTanks.wornTorso(player);
                ItemStack mmu = torso.isEmpty() ? ItemStack.EMPTY : SuitTanks.maneuveringUnit(torso);
                boolean boots = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(SuitModule.MAGNETIC_BOOTS.get());
                if (ZeroG.needsMicroGravityHint(g, boots, Tether.anchor(player).isPresent(), !mmu.isEmpty() && ManeuveringUnitItem.fuel(mmu) > 0)) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.suit.micro_g")
                            .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                }
            }
            data.wasMicroGravity = micro;
        }
        if (player.tickCount % 10 == 0) {
            boolean pressurised = pressurisedAt(player);
            if (pressurised && !data.wasPressurised) {
                // stepping into a station module or base: the helmet may come off here
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.suit.pressurised")
                        .withStyle(net.minecraft.ChatFormatting.AQUA), true);
            }
            data.wasPressurised = pressurised;
        }
        if (player.tickCount % 20 == 7 && SpaceSuitItem.wearsHelmet(player) && sp.connection != null
                && sp.connection.hasChannel(com.pizzaparker07.spacebreaker.content.suit.net.NavPayload.TYPE)) {
            var target = Navigation.find(sp.serverLevel(), player, data.navKind);
            PacketDistributor.sendToPlayer(sp, new com.pizzaparker07.spacebreaker.content.suit.net.NavPayload(data.navKind.ordinal(),
                    target.map(t -> t.kind().ordinal()).orElse(0), target.map(Navigation.Target::pos)));
        }
        if (player.tickCount % 5 == 0) {
            sendStatus(sp, data);
        }
        if (player.tickCount % 10 == 0) {
            sealFeedback(sp, data);
        }
    }

    // the raw (unshielded) rate is cached per player between the 1-second updates
    private static final java.util.WeakHashMap<ServerPlayer, Double> RATES = new java.util.WeakHashMap<>();

    private static void cachedRate(ServerPlayer player, double rate) {
        RATES.put(player, rate);
    }

    private static double lastRate(ServerPlayer player) {
        return RATES.getOrDefault(player, 0.0);
    }

    private static final java.util.List<java.util.function.Predicate<Player>> PRESSURE_SOURCES = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Lets other modules say "this player is in a pressurised cabin" (e.g. rocketry: a crew capsule). The suit then
     * reports a pressurised environment (HUD row, entry message, no visor frost). Breathing itself still follows
     * {@code Environment} oxygen sources; register one there too. Server side, called every 10 ticks per player.
     */
    public static void addPressureSource(java.util.function.Predicate<Player> source) {
        PRESSURE_SOURCES.add(source);
    }

    /**
     * Is the player inside a pressurised space: any registered Environment oxygen source at the head (oxygen
     * distributor rooms, a crew capsule, ...) or a registered pressure source?
     */
    public static boolean pressurisedAt(Player player) {
        if (Environment.suppliedOxygen(player.level(), BlockPos.containing(player.getEyePosition()))) {
            return true;
        }
        for (var source : PRESSURE_SOURCES) {
            if (source.test(player)) {
                return true;
            }
        }
        return false;
    }

    /** Tells the wearer when the suit seals (all four pieces on) or opens again. */
    static void sealFeedback(ServerPlayer player, SurvivalData data) {
        boolean sealed = SpaceSuitItem.isSealed(player);
        if (data.wasSealed != null && data.wasSealed != sealed) {
            if (sealed) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.spacebreaker.suit.sealed")
                        .withStyle(net.minecraft.ChatFormatting.AQUA), true);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.25F, 1.8F);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(data.breathable
                        ? "message.spacebreaker.suit.unsealed" : "message.spacebreaker.suit.unsealed_vacuum")
                        .withStyle(data.breathable ? net.minecraft.ChatFormatting.GRAY : net.minecraft.ChatFormatting.RED), true);
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 0.8F);
            }
        }
        data.wasSealed = sealed;
    }

    static void sendStatus(ServerPlayer player, SurvivalData data) {
        ItemStack torso = SuitTanks.wornTorso(player);
        int flags = 0;
        SuitProtection.Rating rating = SuitProtection.rating(player);
        if (rating != null) {
            flags |= SuitStatusPayload.SEALED;
        }
        if (SuitProtection.torsoHas(player, SuitUpgrade.JETPACK_BOOSTER)) {
            flags |= SuitStatusPayload.BOOSTER;
        }
        if (data.breathable) {
            flags |= SuitStatusPayload.BREATHABLE;
        }
        if (LifeSupport.hazards()) {
            flags |= SuitStatusPayload.HAZARDS;
        }
        if (data.wasMagnetised) {
            flags |= SuitStatusPayload.MAGNETISED;
        }
        if (data.wasPressurised) {
            flags |= SuitStatusPayload.PRESSURISED;
        }
        if (data.wasMicroGravity) {
            flags |= SuitStatusPayload.MICRO_G;
        }
        if (com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.inColdTrap(player.level(), player.blockPosition())) {
            flags |= SuitStatusPayload.COLD_TRAP;
        }
        if (Tether.anchor(player).isPresent()) {
            flags |= SuitStatusPayload.TETHERED;
        }
        ItemStack mmu = torso.isEmpty() ? ItemStack.EMPTY : SuitTanks.maneuveringUnit(torso);
        if (!mmu.isEmpty()) {
            flags |= SuitStatusPayload.MMU;
        }
        PacketDistributor.sendToPlayer(player, new SuitStatusPayload(SuitTanks.oxygen(torso), SuitTanks.capacity(torso), data.breath,
                (float) data.bodyTempC, (float) data.ambientC, (float) data.doseMsv, (float) data.doseRateMsvDay,
                (float) data.pressureKpa, (float) Gravity.factorFor(player), mmu.isEmpty() ? 0 : ManeuveringUnitItem.fuel(mmu),
                integrity(player), flags, rating == null ? 0 : (int) Math.round(LifeSupport.ratedRange(rating, data.pressureKpa)[0]),
                rating == null ? 0 : (int) Math.round(LifeSupport.ratedRange(rating, data.pressureKpa)[1]),
                rating == null ? 0 : rating.shieldingPercent(), (float) data.oxygenRate));
    }

    /** Suit integrity in percent: the most worn piece counts. */
    static int integrity(Player player) {
        int worst = 100;
        for (ItemStack s : player.getArmorSlots()) {
            if (s.getItem() instanceof SpaceSuitItem && s.isDamageableItem()) {
                worst = Math.min(worst, Math.round(100F * (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage()));
            }
        }
        return worst;
    }

    @SubscribeEvent
    static void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp && !event.isCanceled()) {
            // after this block goes, the wide-bore drill takes its neighbours (scheduled so this break finishes first)
            BlockPos pos = event.getPos().immutable();
            var face = ElectricDrillItem.wideFace(sp, pos);
            if (face != null) {
                var state = event.getState();
                sp.server.execute(() -> ElectricDrillItem.breakAround(sp, pos, state, face));
            }
        }
    }

    @SubscribeEvent
    static void onStartTracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer tracker
                && tracker.connection.hasChannel(com.pizzaparker07.spacebreaker.content.suit.net.TetherPayload.TYPE)) {
            Tether.anchor(target).ifPresent(a -> PacketDistributor.sendToPlayer(tracker,
                    new com.pizzaparker07.spacebreaker.content.suit.net.TetherPayload(target.getId(), java.util.Optional.of(a))));
        }
    }

    @SubscribeEvent
    static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Tether.sync(event.getEntity());
    }

    @SubscribeEvent
    static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        Tether.sync(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        Tether.release(event.getEntity(), "message.spacebreaker.tether.released");
        Tether.sync(event.getEntity());
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player) && !living.level().isClientSide
                && (living.tickCount + living.getId()) % 20 == 0) {
            Gravity.apply(living, Gravity.factorFor(living));
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !event.getLevel().isClientSide) {
            Gravity.apply(living, Gravity.factorFor(living));
        }
    }
}
