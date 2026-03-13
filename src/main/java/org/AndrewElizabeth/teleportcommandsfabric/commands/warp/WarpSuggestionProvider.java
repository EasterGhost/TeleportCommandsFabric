package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WarpSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		try {
			List<NamedLocation> warpStorage = StorageManager.STORAGE.getWarps();
			for (NamedLocation currentWarp : warpStorage) {
				builder.suggest(currentWarp.getName());
			}

			return builder.buildFuture();
		} catch (Exception e) {
			Constants.LOGGER.error("Error getting warp suggestions! ", e);
			return builder.buildFuture();
		}
	}
}
