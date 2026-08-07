package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;
import java.util.Locale;

final class WaypointDimensionSuggestions implements SuggestionProvider<CommandSourceStack> {
	static final WaypointDimensionSuggestions LOADED_DIMENSIONS = new WaypointDimensionSuggestions();

	private WaypointDimensionSuggestions() {
	}

	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		String remaining = unquote(builder.getRemaining()).toLowerCase(Locale.ROOT);
		context.getSource().getServer().levelKeys().stream()
				.map(key -> key.identifier().toString())
				.filter(id -> id.toLowerCase(Locale.ROOT).startsWith(remaining))
				.map(CommandArgumentUtils::quote)
				.forEach(builder::suggest);
		return builder.buildFuture();
	}

	private String unquote(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.charAt(0) == '"' ? value.substring(1) : value;
	}
}
