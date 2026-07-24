package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointQueryNodes;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;

final class SharedHomeNodeFactory {
	private static final String ARG_DISABLE_SAFETY = "disableSafety";
	private static final HomeSuggestionProvider SHARE_SUGGESTIONS = new HomeSuggestionProvider(home -> !home.isTemporary());
	private static final SharedHomeSuggestionProvider SHARED_HOME_SUGGESTIONS = new SharedHomeSuggestionProvider();

	private SharedHomeNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildShareNode() {
		return Commands.literal("sharehome")
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(SHARE_SUGGESTIONS)
						.executes(context -> SharedHomePublicationHandler.shareByName(
								context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("sharedhome")
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(SHARED_HOME_SUGGESTIONS)
						.executes(context -> SharedHomeTeleportHandler.teleportByName(
								context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"), null))
						.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
								.executes(context -> SharedHomeTeleportHandler.teleportByName(
										context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildInternalTeleportNode() {
		return Commands.literal("teleportcommandsfabric:sharedhome")
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.then(Commands.argument("ownerUuid", UuidArgument.uuid())
						.then(Commands.argument("homeUuid", UuidArgument.uuid())
								.executes(context -> SharedHomeTeleportHandler.teleportByKey(
										context.getSource().getPlayerOrException(), key(context), null))
								.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
										.executes(context -> SharedHomeTeleportHandler.teleportByKey(
												context.getSource().getPlayerOrException(), key(context),
												BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY))))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode(String literal, boolean pagePicker) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(SharedHomeNodeFactory::requiresPlayer);
		if (!pagePicker) {
			root.executes(context -> SharedHomeListHandler.renderSharedHomes(context.getSource().getPlayerOrException(),
					WaypointListQuery.defaultQuery(), false));
			root.then(WaypointQueryNodes.filterNode(WaypointListQuery.DEFAULT_PAGE,
					(context, query) -> SharedHomeListHandler.renderSharedHomes(
							context.getSource().getPlayerOrException(), query, false)));
			root.then(WaypointQueryNodes.sortNode(WaypointListQuery.DEFAULT_PAGE, ignored -> WaypointFilter.none(),
					(context, query) -> SharedHomeListHandler.renderSharedHomes(
							context.getSource().getPlayerOrException(), query, false)));
		}
		root.then(WaypointQueryNodes.pageArgument((context, query) -> SharedHomeListHandler.renderSharedHomes(
				context.getSource().getPlayerOrException(), query, pagePicker)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildFilterPickerNode(String literal,
			WaypointFilterPickerKind pickerKind) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.executes(context -> SharedHomeListHandler.renderFilterPicker(context.getSource().getPlayerOrException(),
						WaypointListQuery.defaultQuery(), pickerKind));
		root.then(WaypointQueryNodes.pageArgument((context, query) -> SharedHomeListHandler.renderFilterPicker(
				context.getSource().getPlayerOrException(), query, pickerKind)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUiNode() {
		return Commands.literal("teleportcommandsfabric:sharedhomeui")
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.then(buildSubscribeAction())
				.then(buildUnsubscribeAction())
				.then(buildMapAction());
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:mapsharedhome")
				.requires(SharedHomeNodeFactory::requiresPlayer)
				.then(Commands.argument("ownerUuid", UuidArgument.uuid())
						.then(Commands.argument("homeUuid", UuidArgument.uuid())
								.then(Commands.argument("visible", BoolArgumentType.bool())
										.executes(context -> SharedHomeSubscriptionHandler.setMapVisible(
												context.getSource().getPlayerOrException(), key(context),
												BoolArgumentType.getBool(context, "visible"))))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSubscribeAction() {
		return Commands.literal("subscribe")
				.then(Commands.argument("ownerUuid", UuidArgument.uuid())
						.then(Commands.argument("homeUuid", UuidArgument.uuid())
								.executes(context -> SharedHomeSubscriptionHandler.subscribe(
										context.getSource().getPlayerOrException(), key(context)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildUnsubscribeAction() {
		return Commands.literal("unsubscribe")
				.then(Commands.argument("ownerUuid", UuidArgument.uuid())
						.then(Commands.argument("homeUuid", UuidArgument.uuid())
								.then(WaypointQueryNodes.pageArgument((context, query) ->
										SharedHomeSubscriptionHandler.unsubscribe(
												context.getSource().getPlayerOrException(), key(context), query)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMapAction() {
		return Commands.literal("map")
				.then(Commands.argument("ownerUuid", UuidArgument.uuid())
						.then(Commands.argument("homeUuid", UuidArgument.uuid())
								.then(Commands.argument("visible", BoolArgumentType.bool())
										.then(WaypointQueryNodes.pageArgument((context, query) ->
												SharedHomeSubscriptionHandler.setMapVisible(
														context.getSource().getPlayerOrException(), key(context),
														BoolArgumentType.getBool(context, "visible"), query))))));
	}

	private static SharedHomeKey key(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
		return new SharedHomeKey(UuidArgument.getUuid(context, "ownerUuid"), UuidArgument.getUuid(context, "homeUuid"));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}
}
