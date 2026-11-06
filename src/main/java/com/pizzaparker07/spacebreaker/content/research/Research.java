package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public research API for other areas (server side unless noted).
 * <pre>
 *   Research.disk(id)                     // a tech disk item stack (loot, quest rewards)
 *   Research.sample(Spacebreaker.id("moon"))      // a field sample from a world
 *   Research.experimentData(Spacebreaker.id("orbital_flight")) // flight / probe records
 *   Research.teach(player, id, true)      // learn now (and share with the player's team)
 * </pre>
 * Sample kinds that technologies react to are listed in their {@code samples} field (see {@link ResearchTree}).
 */
public final class Research {
    public static ItemStack disk(ResourceLocation tech) {
        ItemStack stack = new ItemStack(ResearchModule.TECH_DISK.get());
        stack.set(ResearchModule.TECH.get(), tech);
        return stack;
    }

    public static ItemStack sample(ResourceLocation kind) {
        ItemStack stack = new ItemStack(ResearchModule.FIELD_SAMPLE.get());
        stack.set(ResearchModule.SAMPLE.get(), kind);
        return stack;
    }

    public static ItemStack experimentData(ResourceLocation kind) {
        ItemStack stack = new ItemStack(ResearchModule.EXPERIMENT_DATA.get());
        stack.set(ResearchModule.SAMPLE.get(), kind);
        return stack;
    }

    /**
     * Teaches a technology. With {@code shareWithTeam} every member of the player's vanilla scoreboard team learns it too
     * (offline members on their next login).
     *
     * @return true if the player (not counting team mates) newly learned it
     */
    public static boolean teach(ServerPlayer player, ResourceLocation tech, boolean shareWithTeam) {
        boolean learned = Tech.learn(player, tech);
        if (shareWithTeam) {
            PlayerTeam team = player.getTeam();
            if (team != null) {
                ResearchSavedData.get(player.server).addTeamTech(team.getName(), tech);
                for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                    if (other != player && team.getPlayers().contains(other.getScoreboardName())) {
                        Tech.learn(other, tech);
                    }
                }
            }
        }
        return learned;
    }

    /** Technologies a sample of {@code kind} could reveal to the player right now (prerequisites met, not yet known). */
    public static List<ResourceLocation> revealableBy(Player player, ResourceLocation kind) {
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, TechNode> e : Tech.all(player.level().registryAccess()).entrySet()) {
            TechNode node = e.getValue();
            if (node.samples().contains(kind) && !Tech.knows(player, e.getKey()) && Tech.prerequisitesMet(player, node)) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** Picks what analysing a sample teaches: the lowest-tier revealable technology, ties broken at random. */
    public static Optional<ResourceLocation> pickRevealed(Player player, ResourceLocation kind, RandomSource random) {
        List<ResourceLocation> options = revealableBy(player, kind);
        if (options.isEmpty()) {
            return Optional.empty();
        }
        var all = Tech.all(player.level().registryAccess());
        int minTier = options.stream().mapToInt(id -> all.get(id).tier()).min().orElse(1);
        List<ResourceLocation> lowest = options.stream().filter(id -> all.get(id).tier() == minTier).toList();
        return Optional.of(lowest.get(random.nextInt(lowest.size())));
    }

    private Research() {
    }
}
