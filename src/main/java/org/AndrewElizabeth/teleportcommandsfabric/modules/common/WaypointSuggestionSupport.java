package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import java.util.Locale;

public final class WaypointSuggestionSupport {
	private WaypointSuggestionSupport() {
	}

	public static Suggestions build(SuggestionsBuilder builder, Iterable<String> names) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		String unquotedRemaining = remaining.startsWith("\"") ? remaining.substring(1) : remaining;

		for (String name : names) {
			if (name == null || name.isBlank()) {
				continue;
			}
			String suggestion = CommandArgumentUtils.quote(name);
			if (matches(name, suggestion, remaining, unquotedRemaining)) {
				builder.suggest(suggestion);
			}
		}
		return builder.build();
	}

	private static boolean matches(String name, String suggestion, String remaining, String unquotedRemaining) {
		return suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)
				|| name.toLowerCase(Locale.ROOT).startsWith(unquotedRemaining);
	}
}
