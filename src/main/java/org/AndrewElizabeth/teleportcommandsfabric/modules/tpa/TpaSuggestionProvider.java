package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class TpaSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		TpaService service = TeleportCommands.TPA_SERVICE;
		if (service == null) {
			return builder.buildFuture();
		}

		ServerPlayer recipient;
		try {
			recipient = context.getSource().getPlayerOrException();
		} catch (Exception ignored) {
			return builder.buildFuture();
		}

		List<String> senderNames = service.getIncoming(recipient.getUUID()).stream()
				.map(session -> context.getSource().getServer().getPlayerList().getPlayer(session.sender()))
				.filter(sender -> sender != null)
				.map(sender -> sender.getName().getString())
				.toList();
		return SharedSuggestionProvider.suggest(senderNames, builder);
	}
}
