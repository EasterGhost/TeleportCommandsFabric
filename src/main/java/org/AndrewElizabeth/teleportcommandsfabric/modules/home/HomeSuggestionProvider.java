package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.SuggestionCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.models.PlayerData;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class HomeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	private final Predicate<NamedLocation> filter;

	public HomeSuggestionProvider() {
		this(home -> true);
	}

	public HomeSuggestionProvider(Predicate<NamedLocation> filter) {
		this.filter = filter;
	}

	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SuggestionCommandSupport.suggest(builder, "Error getting HomeCommand suggestions! ", () -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			Optional<PlayerData> optionalPlayerStorage = StorageManager.STORAGE.getPlayer(player.getStringUUID());
			if (optionalPlayerStorage.isEmpty()) {
				return java.util.List.of();
			}

			PlayerData playerStorage = optionalPlayerStorage.get();
			playerStorage.refreshHomeState();
			return playerStorage.getHomes().stream()
					.filter(filter)
					.map(NamedLocation::getName)
					.toList();
		});
	}
}
