package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.SuggestionCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class WarpSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SuggestionCommandSupport.suggest(builder, "Error getting WarpCommand suggestions! ",
				() -> StorageManager.STORAGE.getWarps().stream()
						.map(NamedLocation::getName)
						.toList());
	}
}
