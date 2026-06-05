package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointQueryNodes;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class HomeNodeFactory {
	private static final String ARG_DISABLE_SAFETY = "disableSafety";
	private static final HomeSuggestionProvider HOME_SUGGESTIONS = new HomeSuggestionProvider();
	private static final HomeSuggestionProvider DEFAULT_HOME_SUGGESTIONS = new HomeSuggestionProvider(home -> !home.isTemporary());

	private HomeNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode(String literal, boolean temporary) {
		return Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.executes(context -> HomeMutationHandler.setHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), temporary)));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return Commands.literal("updatehome")
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> HomeMutationHandler.updateHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("home")
				.requires(HomeNodeFactory::requiresPlayer)
				.executes(context -> HomeTeleportHandler.teleportHome(context.getSource().getPlayerOrException(), null, null))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> HomeTeleportHandler.teleportHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), null))
						.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
								.executes(context -> HomeTeleportHandler.teleportHome(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delhome")
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> HomeMutationHandler.deleteHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamehome")
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> HomeMutationHandler.renameHome(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										StringArgumentType.getString(context, "newName")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return Commands.literal("defaulthome")
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(DEFAULT_HOME_SUGGESTIONS)
						.executes(context -> HomeMutationHandler.setDefaultHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode(String literal, boolean pagePicker) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer);
		if (!pagePicker) {
			root.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
					WaypointListQuery.defaultQuery(), false));
			root.then(WaypointQueryNodes.filterNode(WaypointListQuery.DEFAULT_PAGE,
					(context, query) -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(), query, pagePicker)));
			root.then(WaypointQueryNodes.sortNode(WaypointListQuery.DEFAULT_PAGE,
					ignored -> WaypointFilter.none(),
					(context, query) -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(), query, pagePicker)));
		}
		root.then(WaypointQueryNodes.pageArgument(
				(context, query) -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(), query, pagePicker)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> HomeMutationHandler.setMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.then(WaypointQueryNodes.pageArgument(
					(context, query) -> HomeMutationHandler.setMapVisibility(context, true, query)));
		}
		return Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(visibleNode));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}
}
