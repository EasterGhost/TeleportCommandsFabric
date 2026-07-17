package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeResolver;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

final class SharedHomeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		if (TeleportCommands.SHARED_HOME_SERVICE == null || TeleportCommands.PLAYER_PROFILE_MANAGER == null) {
			return builder.buildFuture();
		}
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception ignored) {
			return builder.buildFuture();
		}
		return SharedHomeResolver.resolveSubscriptions(player.getUUID(), TeleportCommands.SHARED_HOME_SERVICE,
				TeleportCommands.PLAYER_PROFILE_MANAGER, player.level().getServer()).handle((homes, throwable) -> {
			if (throwable == null) {
				homes.stream().map(home -> home.getName()).distinct()
						.forEach(name -> builder.suggest(CommandArgumentUtils.quote(name)));
			}
			return null;
		}).thenCompose(ignored -> builder.buildFuture());
	}
}
