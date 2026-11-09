package com.pizzaparker07.spacebreaker.testmod.qa;

import com.mojang.authlib.GameProfile;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * QA (servers with mixed clients): a player whose connection has agreed none of the mod's network channels (as a
 * client without Spacebreaker, or with a version that lacks a payload) logs in, wears a full suit (the suit HUD is
 * synced every tick), learns a technology (the known-tech sync) and stays for 100 ticks. Nothing may throw and the
 * player must stay connected. {@code tools/qa/channel_audit.py} checks the same rule in the source: every server to
 * client send is guarded by hasChannel.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NoModClientTests {
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aClientWithoutTheModsChannelsStaysConnected(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("qa_no_channels".getBytes()), "qa_no_channels");
        ServerPlayer p = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        try {
            server.getPlayerList().placeNewPlayer(connection, p, CommonListenerCookie.createInitial(profile, false));
        } catch (RuntimeException e) {
            h.fail("logging in a client without the mod's channels threw: " + e);
            return;
        }
        h.assertTrue(p.connection != null, "a play connection");
        h.assertFalse(p.connection.hasChannel(com.pizzaparker07.spacebreaker.content.research.net.KnownTechPayload.TYPE),
                "the test client really has no mod channels");
        Vec3 at = h.absoluteVec(new Vec3(4, 1, 4));
        p.teleportTo(level, at.x, at.y, at.z, 0F, 0F);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        p.setItemSlot(EquipmentSlot.CHEST, torso);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
        try {
            Tech.learn(p, ResearchTree.id("basic_rocketry"));
        } catch (RuntimeException e) {
            h.fail("learning a technology threw for a client without the channels: " + e);
            return;
        }
        h.runAfterDelay(100, () -> {
            h.assertTrue(connection.isConnected(), "still connected after 100 ticks in a suit");
            h.assertTrue(server.getPlayerList().getPlayer(p.getUUID()) == p, "still on the server");
            server.getPlayerList().remove(p);
            h.succeed();
        });
    }

    private NoModClientTests() {
    }
}
