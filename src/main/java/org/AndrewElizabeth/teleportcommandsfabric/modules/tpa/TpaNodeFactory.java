package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

import java.util.UUID;

final class TpaNodeFactory {
	private static final TpaSuggestionProvider REQUEST_SUGGESTIONS = new TpaSuggestionProvider();

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
								.executes(context -> TpaRequestHandler.handleResponse(context.getSource().getPlayerOrException(),
										EntityArgument.getPlayer(context, "player"),
										parseRequestId(StringArgumentType.getString(context, "requestId")), accept))));
	}

	private static UUID parseRequestId(String requestId) {
		try {
			return UUID.fromString(requestId);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
