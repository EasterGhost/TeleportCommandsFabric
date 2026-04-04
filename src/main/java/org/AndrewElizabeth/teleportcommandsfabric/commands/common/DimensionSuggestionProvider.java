package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class DimensionSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SuggestionCommandSupport.suggest(
				builder,
				"Error getting dimension suggestions! ",
				DimensionFilterCommandSupport::getDimensionSuggestions);
	}
}
