package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.SuggestionCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HomeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SuggestionCommandSupport.suggest(builder, "Error getting home suggestions! ", () -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			Optional<Player> optionalPlayerStorage = StorageManager.STORAGE.getPlayer(player.getStringUUID());
			if (optionalPlayerStorage.isEmpty()) {
				return java.util.List.of();
			}

			Player playerStorage = optionalPlayerStorage.get();
			return playerStorage.getHomes().stream()
					.map(NamedLocation::getName)
					.toList();
		});
	}
}
