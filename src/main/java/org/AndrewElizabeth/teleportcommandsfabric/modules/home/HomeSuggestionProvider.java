package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointSuggestionSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

final class HomeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	private final Predicate<NamedLocationView> filter;

	HomeSuggestionProvider() {
		this(home -> true);
	}

	HomeSuggestionProvider(Predicate<NamedLocationView> filter) {
		this.filter = filter == null ? home -> true : filter;
	}

	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		if (TeleportCommands.PLAYER_PROFILE_MANAGER == null) {
			return builder.buildFuture();
		}

		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception ignored) {
			return builder.buildFuture();
		}

		return TeleportCommands.PLAYER_PROFILE_MANAGER.query(player.getUUID(), profile -> profile.getHomes().stream()
				.filter(filter).map(NamedLocationView::getName).toList()).handle((names, throwable) ->
						WaypointSuggestionSupport.build(builder, throwable == null ? names : List.of()));
	}
}
