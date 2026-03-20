package org.AndrewElizabeth.teleportcommandsfabric.commands.tpa;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.services.TpaService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TpaSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();

			List<TpaService.Request> playerTpaList = TpaService.getRequests().stream()
					.filter(tpa -> Objects.equals(player.getStringUUID(), tpa.recPlayer))
					.toList();
			PlayerList playerList = context.getSource().getServer().getPlayerList();

			for (TpaService.Request tpaEntry : playerTpaList) {
				ServerPlayer requestingPlayer = playerList.getPlayer(UUID.fromString(tpaEntry.initPlayer));
				if (requestingPlayer == null) {
					continue;
				}

				Optional<String> recPlayerName = Optional.ofNullable(requestingPlayer.getName().getString());

				if (recPlayerName.isPresent()) {
					builder.suggest(recPlayerName.get());
				}
			}

			return builder.buildFuture();
		} catch (Exception e) {
			Constants.LOGGER.error("Error getting tpa suggestions! ", e);
			return builder.buildFuture();
		}
	}
}
