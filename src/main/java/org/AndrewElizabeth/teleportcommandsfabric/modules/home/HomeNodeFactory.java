package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointQueryNodes;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
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
		return buildTeleportNode("home");
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode(String literal) {
		return Commands.literal(literal)
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

	static LiteralArgumentBuilder<CommandSourceStack> buildFilterPickerNode(String literal,
			WaypointFilterPickerKind pickerKind) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.executes(context -> HomeListHandler.renderHomeFilterPicker(context.getSource().getPlayerOrException(),
						WaypointListQuery.defaultQuery(), pickerKind));
		root.then(WaypointQueryNodes.pageArgument(
				(context, query) -> HomeListHandler.renderHomeFilterPicker(context.getSource().getPlayerOrException(),
						query, pickerKind)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> HomeMapVisibilityHandler.setVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.executes(context -> HomeMapVisibilityHandler.setVisibility(context, true, null));
			visibleNode.then(WaypointQueryNodes.pageArgument(
					(context, query) -> HomeMapVisibilityHandler.setVisibility(context, true, query)));
		}
		return Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(visibleNode));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUiNode() {
		return Commands.literal("teleportcommandsfabric:homeui")
				.requires(HomeNodeFactory::requiresPlayer)
				.then(uiActionNode("manage", (context, waypointUuid, query) ->
						HomeListHandler.renderHomeManage(context.getSource().getPlayerOrException(), waypointUuid, query, false)))
				.then(uiActionNode("update", (context, waypointUuid, query) ->
						HomeMutationHandler.updateHomeFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)))
				.then(uiActionNode("default", (context, waypointUuid, query) ->
						HomeMutationHandler.setDefaultHomeFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)))
				.then(uiActionNode("share", (context, waypointUuid, query) ->
						SharedHomePublicationHandler.shareFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)))
				.then(uiActionNode("withdraw", (context, waypointUuid, query) ->
						SharedHomePublicationHandler.withdrawFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)))
				.then(uiActionNode("delete", (context, waypointUuid, query) ->
						HomeListHandler.renderHomeManage(context.getSource().getPlayerOrException(), waypointUuid, query, true)))
				.then(uiActionNode("confirmdelete", (context, waypointUuid, query) ->
						HomeMutationHandler.deleteHomeFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> uiActionNode(String action,
			WaypointQueryNodes.WaypointQueryExecutor executor) {
		return Commands.literal(action).then(WaypointQueryNodes.waypointUuidArgument(executor));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}
}
