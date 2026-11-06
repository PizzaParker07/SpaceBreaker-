package com.pizzaparker07.spacebreaker.content.research;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;

/**
 * {@code /spacebreaker research learn|forget <targets> <tech|*>} and {@code /spacebreaker research list [target]}
 * (operators only).
 */
final class ResearchCommands {
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spacebreaker").then(Commands.literal("research")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("learn").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal("*").executes(c -> all(c, true)))
                        .then(Commands.argument("tech", ResourceLocationArgument.id()).suggests((c, b) ->
                                        SharedSuggestionProvider.suggestResource(Tech.all(c.getSource().registryAccess()).keySet(), b))
                                .executes(c -> one(c, true)))))
                .then(Commands.literal("forget").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal("*").executes(c -> all(c, false)))
                        .then(Commands.argument("tech", ResourceLocationArgument.id()).suggests((c, b) ->
                                        SharedSuggestionProvider.suggestResource(Tech.all(c.getSource().registryAccess()).keySet(), b))
                                .executes(c -> one(c, false)))))
                .then(Commands.literal("list").then(Commands.argument("target", EntityArgument.player()).executes(c ->
                        list(c.getSource(), EntityArgument.getPlayer(c, "target")))))));
    }

    private static int one(CommandContext<CommandSourceStack> c, boolean learn) throws CommandSyntaxException {
        ResourceLocation tech = ResourceLocationArgument.getId(c, "tech");
        if (Tech.get(c.getSource().registryAccess(), tech).isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.spacebreaker.research.unknown", tech.toString()));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        int changed = 0;
        for (ServerPlayer p : targets) {
            if (learn ? Tech.learn(p, tech) : Tech.forget(p, tech)) {
                changed++;
            }
        }
        int n = changed;
        c.getSource().sendSuccess(() -> Component.translatable(learn ? "commands.spacebreaker.research.learned" : "commands.spacebreaker.research.forgot",
                Component.translatable(TechNode.nameKey(tech)), n), true);
        return changed;
    }

    private static int all(CommandContext<CommandSourceStack> c, boolean learn) throws CommandSyntaxException {
        Set<ResourceLocation> techs = Tech.all(c.getSource().registryAccess()).keySet();
        int changed = 0;
        for (ServerPlayer p : EntityArgument.getPlayers(c, "targets")) {
            for (ResourceLocation t : techs) {
                if (learn ? Tech.learn(p, t) : Tech.forget(p, t)) {
                    changed++;
                }
            }
        }
        int n = changed;
        c.getSource().sendSuccess(() -> Component.translatable(learn ? "commands.spacebreaker.research.learned_all"
                : "commands.spacebreaker.research.forgot_all", n), true);
        return changed;
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        Set<ResourceLocation> known = Tech.known(player);
        source.sendSuccess(() -> Component.translatable("commands.spacebreaker.research.list", player.getDisplayName(), known.size(),
                Tech.all(source.registryAccess()).size()), false);
        known.stream().sorted().forEach(id -> source.sendSuccess(() -> Component.literal(" - ")
                .append(Component.translatable(TechNode.nameKey(id))), false));
        return known.size();
    }

    private ResearchCommands() {
    }
}
