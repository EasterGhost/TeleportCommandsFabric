package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TpaCommand {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildRequestNode("tpa", false));
		commandDispatcher.register(buildRequestNode("tpahere", true));
		commandDispatcher.register(buildResponseNode("tpaaccept", true));
		commandDispatcher.register(buildResponseNode("tpadeny", false));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRequestNode(String literal, boolean here) {
		String errorLogMessage = here
				? "Error while sending a tpahere request! => "
				: "Error while sending a tpa request! => ";
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> {
							final ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!TpaMessages.ensureEnabled(player)) {
								return 1;
							}

							return TpaMessages.execute(errorLogMessage, () -> TpaService.sendRequest(player, targetPlayer, here));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildResponseNode(String literal, boolean accept) {
		String errorLogMessage = accept
				? "Error while accepting a tpa(here) request! => "
				: "Error while denying a tpa(here) request! => ";
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player()).suggests(new TpaSuggestionProvider())
						.executes(context -> {
							final ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!TpaMessages.ensureEnabled(player)) {
								return 1;
							}

							return TpaMessages.execute(errorLogMessage, () -> handleResponse(player, targetPlayer, null, accept));
						})
						.then(Commands.argument("requestId", StringArgumentType.word())
								.executes(context -> {
									final ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final UUID requestId = parseRequestId(StringArgumentType.getString(context, "requestId"));

									if (!TpaMessages.ensureEnabled(player)) {
										return 1;
									}

									return TpaMessages.execute(errorLogMessage, () -> handleResponse(player, targetPlayer, requestId, accept));
								})));
	}

	private static void handleResponse(ServerPlayer player, ServerPlayer targetPlayer, UUID requestId, boolean accept) {
		if (accept) {
			TpaService.acceptRequest(player, targetPlayer, requestId);
			return;
		}
		TpaService.denyRequest(player, targetPlayer, requestId);
	}

	private static UUID parseRequestId(String requestId) {
		try {
			return UUID.fromString(requestId);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
