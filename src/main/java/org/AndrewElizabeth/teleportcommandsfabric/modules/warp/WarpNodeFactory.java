package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointQueryNodes;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

final class WarpNodeFactory {
	private static final String ARG_DISABLE_SAFETY = "disableSafety";
	private static final WarpSuggestionProvider WARP_SUGGESTIONS = new WarpSuggestionProvider();

	private WarpNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("setwarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.executes(context -> WarpMutationHandler.setWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return Commands.literal("updatewarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> WarpMutationHandler.updateWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return buildTeleportNode("warp");
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode(String literal) {
		return Commands.literal(literal)
				.requires(WarpNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> WarpTeleportHandler.teleportWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), null))
						.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
								.executes(context -> WarpTeleportHandler.teleportWarp(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delwarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> WarpMutationHandler.deleteWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamewarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> WarpMutationHandler.renameWarp(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										StringArgumentType.getString(context, "newName")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode(String literal, boolean pagePicker) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(WarpNodeFactory::requiresPlayer);
		if (!pagePicker) {
			root.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
					WaypointListQuery.defaultQuery(), false));
			root.then(WaypointQueryNodes.filterNode(WaypointListQuery.DEFAULT_PAGE,
					(context, query) -> WarpListHandler.renderWarps(context.getSource(),
							context.getSource().getPlayerOrException(), query, pagePicker)));
			root.then(WaypointQueryNodes.sortNode(WaypointListQuery.DEFAULT_PAGE,
					ignored -> WaypointFilter.none(),
					(context, query) -> WarpListHandler.renderWarps(context.getSource(),
							context.getSource().getPlayerOrException(), query, pagePicker)));
		}
		root.then(WaypointQueryNodes.pageArgument(
				(context, query) -> WarpListHandler.renderWarps(context.getSource(),
						context.getSource().getPlayerOrException(), query, pagePicker)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildFilterPickerNode(String literal,
			WaypointFilterPickerKind pickerKind) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(WarpNodeFactory::requiresPlayer)
				.executes(context -> WarpListHandler.renderWarpFilterPicker(context.getSource(),
						context.getSource().getPlayerOrException(), WaypointListQuery.defaultQuery(), pickerKind));
		root.then(WaypointQueryNodes.pageArgument(
				(context, query) -> WarpListHandler.renderWarpFilterPicker(context.getSource(),
						context.getSource().getPlayerOrException(), query, pickerKind)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPlayerMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, true, null));
			visibleNode.then(WaypointQueryNodes.pageArgument(
					(context, query) -> WarpMutationHandler.setPlayerMapVisibility(context, true, query)));
		}
		return Commands.literal(literal)
				.requires(WarpNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(visibleNode));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildGlobalMapVisibilityNode() {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool())
				.then(WaypointQueryNodes.pageArgument(WarpMutationHandler::setGlobalMapVisibility));
		return Commands.literal("teleportcommandsfabric:gmapwarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(visibleNode));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPublicGlobalMapVisibilityNode() {
		return Commands.literal("gwarpmap")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context, null))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUiNode() {
		return Commands.literal("teleportcommandsfabric:warpui")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(uiActionNode("manage", (context, waypointUuid, query) ->
						WarpListHandler.renderWarpManage(context.getSource(), context.getSource().getPlayerOrException(),
								waypointUuid, query, false)))
				.then(uiActionNode("update", (context, waypointUuid, query) ->
						WarpMutationHandler.updateWarpFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)))
				.then(uiActionNode("delete", (context, waypointUuid, query) ->
						WarpListHandler.renderWarpManage(context.getSource(), context.getSource().getPlayerOrException(),
								waypointUuid, query, true)))
				.then(uiActionNode("confirmdelete", (context, waypointUuid, query) ->
						WarpMutationHandler.deleteWarpFromManage(context.getSource().getPlayerOrException(), waypointUuid, query)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> uiActionNode(String action,
			WaypointQueryNodes.WaypointQueryExecutor executor) {
		return Commands.literal(action).then(WaypointQueryNodes.waypointUuidArgument(executor));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}

	private static boolean requiresAdminPlayer(CommandSourceStack source) {
		return requiresPlayer(source) && source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}
}
