package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HomeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			Optional<Player> optionalPlayerStorage = StorageManager.STORAGE.getPlayer(player.getStringUUID());

			if (optionalPlayerStorage.isPresent()) {
				Player playerStorage = optionalPlayerStorage.get();
				for (NamedLocation currentHome : playerStorage.getHomes()) {
					builder.suggest(currentHome.getName());
				}
			}
			return builder.buildFuture();
		} catch (Exception e) {
			Constants.LOGGER.error("Error getting home suggestions! ", e);
			return builder.buildFuture();
		}
	}
}
