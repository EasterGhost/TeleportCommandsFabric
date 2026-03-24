package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import java.util.concurrent.CompletableFuture;

public final class SuggestionCommandSupport {
	@FunctionalInterface
	public interface SuggestionSupplier {
		Iterable<String> get() throws Exception;
	}

	private SuggestionCommandSupport() {
	}

	public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, String errorLogMessage,
			SuggestionSupplier supplier) {
		try {
			for (String suggestion : supplier.get()) {
				if (suggestion == null || suggestion.isBlank()) {
					continue;
				}
				builder.suggest(suggestion);
			}
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
		}
		return builder.buildFuture();
	}
}
