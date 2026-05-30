package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

final class WarpSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER == null) {
			return builder.buildFuture();
		}
		return TeleportCommands.GLOBAL_PROFILE_MANAGER.query(profile -> profile.getWarps().stream()
				.map(NamedLocationView::getName)
				.toList()).handle((names, throwable) -> {
					if (throwable == null) {
						for (String name : names) {
							builder.suggest(CommandArgumentUtils.quote(name));
						}
					}
					return null;
				}).thenCompose(ignored -> builder.buildFuture());
	}
}
