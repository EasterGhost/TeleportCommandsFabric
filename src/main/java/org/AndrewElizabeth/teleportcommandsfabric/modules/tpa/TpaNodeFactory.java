package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustDecision;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Arrays;
import java.util.Collection;

final class TpaNodeFactory {
	private static final TpaSuggestionProvider REQUEST_SUGGESTIONS = new TpaSuggestionProvider();
	private static final SuggestionProvider<CommandSourceStack> DECISION_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(TpaTrustDecision.values())
					.map(TpaTrustDecision::serializedName), builder);
	private static final SimpleCommandExceptionType INVALID_DECISION = new SimpleCommandExceptionType(
			Component.literal("Invalid trust decision."));
	private static final SimpleCommandExceptionType EXPECTED_SINGLE_PLAYER = new SimpleCommandExceptionType(
			Component.literal("Expected a single player."));

	private TpaNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRequestNode(String literal, Tpa.Type type) {
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> TpaRequestHandler.sendRequest(context.getSource().getPlayerOrException(),
								EntityArgument.getPlayer(context, "player"), type)));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildResponseNode(String literal, boolean accept) {
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player())
						.suggests(REQUEST_SUGGESTIONS)
						.executes(context -> TpaRequestHandler.handleResponse(context.getSource().getPlayerOrException(),
								EntityArgument.getPlayer(context, "player"), null, accept))
						.then(Commands.argument("requestId", StringArgumentType.word())
								.executes(context -> TpaRequestHandler.handleResponseWithRequestId(context.getSource().getPlayerOrException(),
										EntityArgument.getPlayer(context, "player"),
										StringArgumentType.getString(context, "requestId"), accept))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTrustNode() {
		return Commands.literal("trust")
				.requires(source -> source.getPlayer() != null)
				.then(withTrustRuleNodes(Commands.literal("all"), context -> TpaTrustTarget.allPlayers()))
				.then(Commands.literal("player")
						.then(withTrustRuleNodes(Commands.argument("player", GameProfileArgument.gameProfile()),
								context -> TpaTrustTarget.player(singleProfile(context, "player")))))
				.then(withTrustRuleNodes(Commands.argument("player", GameProfileArgument.gameProfile()),
						context -> TpaTrustTarget.player(singleProfile(context, "player"))));
	}

	private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTrustRuleNodes(T root,
			TrustTargetResolver targetResolver) {
		root.executes(context -> TpaTrustHandler.showTrust(context.getSource().getPlayerOrException(),
				targetResolver.resolve(context)));
		root.then(trustTypeNode("tpa", Tpa.Type.TPA, "tpahere", targetResolver));
		root.then(trustTypeNode("tpahere", Tpa.Type.TPAHERE, "tpa", targetResolver));
		return root;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> trustTypeNode(String literal, Tpa.Type type,
			String pairedLiteral, TrustTargetResolver targetResolver) {
		return Commands.literal(literal)
				.then(Commands.argument("decision", StringArgumentType.word())
						.suggests(DECISION_SUGGESTIONS)
						.executes(context -> TpaTrustHandler.setTrust(context.getSource().getPlayerOrException(),
								targetResolver.resolve(context), type, decision(context, "decision")))
						.then(Commands.literal(pairedLiteral)
								.then(Commands.argument("pairedDecision", StringArgumentType.word())
										.suggests(DECISION_SUGGESTIONS)
										.executes(context -> {
											ServerPlayer owner = context.getSource().getPlayerOrException();
											TpaTrustTarget target = targetResolver.resolve(context);
											TpaTrustDecision first = decision(context, "decision");
											TpaTrustDecision second = decision(context, "pairedDecision");
											if (type == Tpa.Type.TPA) {
												return TpaTrustHandler.setTrust(owner, target, first, second);
											}
											return TpaTrustHandler.setTrust(owner, target, second, first);
										}))));
	}

	private static TpaTrustDecision decision(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			String argumentName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		return TpaTrustDecision.parseSerialized(StringArgumentType.getString(context, argumentName))
				.orElseThrow(INVALID_DECISION::create);
	}

	private static NameAndId singleProfile(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			String argumentName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, argumentName);
		if (profiles.size() != 1) {
			throw EXPECTED_SINGLE_PLAYER.create();
		}
		return profiles.iterator().next();
	}

	private interface TrustTargetResolver {
		TpaTrustTarget resolve(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context)
				throws com.mojang.brigadier.exceptions.CommandSyntaxException;
	}
}
