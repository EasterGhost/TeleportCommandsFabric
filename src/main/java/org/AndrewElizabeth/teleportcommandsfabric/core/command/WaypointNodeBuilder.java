package org.AndrewElizabeth.teleportcommandsfabric.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointSource;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public class WaypointNodeBuilder {

	private WaypointNodeBuilder() {
	}

	@FunctionalInterface
	public interface SourceProvider {
		WaypointSource get(ServerPlayer player);
	}

	@FunctionalInterface
	public interface CrudAction {
		int execute(ServerPlayer player, String name, WaypointSource source);
	}

	@FunctionalInterface
	public interface RenameAction {
		int execute(ServerPlayer player, String name, String newName, WaypointSource source);
	}

	public static LiteralArgumentBuilder<CommandSourceStack> buildActionNode(String literal,
			Predicate<CommandSourceStack> permission, SuggestionProvider<CommandSourceStack> suggestions,
			SourceProvider sourceProvider, String disabledKey, CrudAction action) {

		return Commands.literal(literal)
				.requires(permission)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(suggestions)
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							WaypointSource source = sourceProvider.get(player);

							if (!source.isEnabled()) {
								CommandExecutionSupport.send(player, disabledKey, ChatFormatting.RED);
								return 1;
							}

							return action.execute(player, StringArgumentType.getString(context, "name"), source);
						}));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode(String literal,
			Predicate<CommandSourceStack> permission, SuggestionProvider<CommandSourceStack> suggestions,
			SourceProvider sourceProvider, String disabledKey, RenameAction action) {

		return Commands.literal(literal)
				.requires(permission)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(suggestions)
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> {
									ServerPlayer player = context.getSource().getPlayerOrException();
									WaypointSource source = sourceProvider.get(player);

									if (!source.isEnabled()) {
										CommandExecutionSupport.send(player, disabledKey, ChatFormatting.RED);
										return 1;
									}

									return action.execute(player, StringArgumentType.getString(context, "name"),
											StringArgumentType.getString(context, "newName"), source);
								})));
	}
}
