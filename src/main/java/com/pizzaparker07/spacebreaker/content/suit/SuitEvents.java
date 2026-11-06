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
        if (player.tickCount % 20 == 0 || player.tickCount < 2) {
            data.doseRateMsvDay = Environment.radiation(player.level(), BlockPos.containing(player.getEyePosition()));
            cachedRate(sp, data.doseRateMsvDay);
        }
        LifeSupport.tick(player, LifeSupport.Env.at(player, lastRate(sp)));
        if (player.tickCount % 5 == 0) {
            sendStatus(sp, data);
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

    static void sendStatus(ServerPlayer player, SurvivalData data) {
        ItemStack torso = SuitTanks.wornTorso(player);
        int flags = 0;
        if (SpaceSuitItem.isSealed(player)) {
            flags |= SuitStatusPayload.SEALED;
        }
        if (data.breathable) {
            flags |= SuitStatusPayload.BREATHABLE;
        }
        if (LifeSupport.hazards()) {
            flags |= SuitStatusPayload.HAZARDS;
        }
        ItemStack mmu = torso.isEmpty() ? ItemStack.EMPTY : SuitTanks.maneuveringUnit(torso);
        if (!mmu.isEmpty()) {
            flags |= SuitStatusPayload.MMU;
        }
        PacketDistributor.sendToPlayer(player, new SuitStatusPayload(SuitTanks.oxygen(torso), SuitTanks.capacity(torso), data.breath,
                (float) data.bodyTempC, (float) data.ambientC, (float) data.doseMsv, (float) data.doseRateMsvDay,
                (float) data.pressureKpa, (float) Gravity.factorFor(player), mmu.isEmpty() ? 0 : ManeuveringUnitItem.fuel(mmu),
                integrity(player), flags));
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
