package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.DimensionSuggestionProvider;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PagedNodeCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PlayerCommandExecutionSupport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

final class HomeNodeFactory {
	private static final DimensionSuggestionProvider DIMENSION_SUGGESTIONS = new DimensionSuggestionProvider();

	private HomeNodeFactory() {
	}

	@FunctionalInterface
	private interface HomeMutationAction {
		void run(ServerPlayer player, String name) throws Exception;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMutationNode(
			String literal,
			String logPrefix,
			String transKey,
			boolean useSuggestions,
			HomeMutationAction action) {
		var argument = Commands.argument("name", StringArgumentType.string());
		if (useSuggestions) {
			argument.suggests(new HomeSuggestionProvider());
		}

		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(argument.executes(context -> PlayerCommandExecutionSupport.executeWithEnabledPlayer(
						context,
						HomeMessages::ensureEnabled,
						logPrefix,
						transKey,
						player -> action.run(player, StringArgumentType.getString(context, "name")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return buildMutationNode(
				"sethome",
				"Error while setting a home! => ",
				"commands.teleport_commands.home.setError",
				false,
				HomeMutationActions::setHome);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return buildMutationNode(
				"delhome",
				"Error while deleting a home! => ",
				"commands.teleport_commands.home.deleteError",
				true,
				HomeMutationActions::deleteHome);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return buildMutationNode(
				"defaulthome",
				"Error while setting the default home! => ",
				"commands.teleport_commands.home.defaultError",
				true,
				HomeMutationActions::setDefaultHome);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return buildMutationNode(
				"updatehome",
				"Error while updating a home location! => ",
				"commands.teleport_commands.home.updateError",
				true,
				HomeMutationActions::updateHome);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("home")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!HomeMessages.ensureEnabled(player)) {
						return 1;
					}

					return HomeMessages.execute(
							player,
							"Error while going home! => ",
							"commands.teleport_commands.home.goError",
							() -> HomeTeleportActions.goHome(player, ""));
				})
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while going to a specific home! => ",
									"commands.teleport_commands.home.goError",
									() -> HomeTeleportActions.goHome(player, name));
						}));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamehome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> {
									final String name = StringArgumentType.getString(context, "name");
									final String newName = StringArgumentType.getString(context, "newName");
									final ServerPlayer player = context.getSource().getPlayerOrException();

									if (!HomeMessages.ensureEnabled(player)) {
										return 1;
									}

									return HomeMessages.execute(
											player,
											"Error while renaming a home! => ",
											"commands.teleport_commands.home.renameError",
											() -> HomeMutationActions.renameHome(player, name, newName));
								})));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("homes")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> home.executeList(context, 1, null))
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, home::executeList))
				.then(PagedNodeCommandSupport.dimensionOnly(1, DIMENSION_SUGGESTIONS, home::executeList));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:homespages")
				.requires(source -> source.getPlayer() != null)
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(
						DIMENSION_SUGGESTIONS,
						home::executePagePicker));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("maphome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String homeName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return home.handleMapVisibility(player, homeName, visible);
								})));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:maphome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> home.executeSilentMapVisibility(context, null, null))
								.then(PagedNodeCommandSupport.pageWithOptionalDimension(null,
										home::executeSilentMapVisibility))));
	}
}
