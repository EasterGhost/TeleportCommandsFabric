package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.*;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Function;

final class WarpNodeFactory {
	private static final int CONTEXT_PAGE = Integer.MIN_VALUE;
	private static final WarpSuggestionProvider WARP_SUGGESTIONS = new WarpSuggestionProvider();

	private WarpNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("setwarp")
				.requires(WarpNodeFactory::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
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
		return Commands.literal("warp")
				.requires(WarpNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> WarpTeleportHandler.teleportWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), false))
						.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
								.executes(context -> WarpTeleportHandler.teleportWarp(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, "Disable Safety")))));
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
			root.then(filterNode(WaypointListQuery.DEFAULT_PAGE, pagePicker));
			root.then(sortNode(WaypointListQuery.DEFAULT_PAGE, ignored -> WaypointFilter.none(), pagePicker));
		}
		root.then(Commands.argument("page", IntegerArgumentType.integer(1))
				.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
						new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null), pagePicker))
				.then(filterNode(CONTEXT_PAGE, pagePicker))
				.then(sortNode(CONTEXT_PAGE, ignored -> WaypointFilter.none(), pagePicker)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPlayerMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.then(Commands.argument("page", IntegerArgumentType.integer(1))
					.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, true,
							new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
					.then(filterNodeForPlayerVisibility(CONTEXT_PAGE, true))
					.then(sortNodeForPlayerVisibility(CONTEXT_PAGE, ignored -> WaypointFilter.none(), true)));
		}
		return Commands.literal(literal)
				.requires(WarpNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(visibleNode));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildGlobalMapVisibilityNode() {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool())
				.then(Commands.argument("page", IntegerArgumentType.integer(1))
						.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
								new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
						.then(filterNodeForGlobalVisibility(CONTEXT_PAGE))
						.then(sortNodeForGlobalVisibility(CONTEXT_PAGE, ignored -> WaypointFilter.none())));
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

	private static LiteralArgumentBuilder<CommandSourceStack> filterNode(int page, boolean pagePicker) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null),
										pagePicker))
								.then(sortNode(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), pagePicker))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), null),
										pagePicker))
								.then(sortNode(page,
										context -> WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), pagePicker))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortNode(int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean pagePicker) {
		return Commands.literal("sort")
				.then(sortKeyNode("name", SortKey.NAME, page, filter, pagePicker))
				.then(sortKeyNode("sequence", SortKey.SEQUENCE, page, filter, pagePicker));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortKeyNode(String literal, SortKey key, int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean pagePicker) {
		return Commands.literal(literal)
				.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection())), pagePicker))
				.then(Commands.literal("asc")
						.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)), pagePicker)))
				.then(Commands.literal("desc")
						.executes(context -> WarpListHandler.renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)), pagePicker)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForPlayerVisibility(int page, boolean silent) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForPlayerVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), silent))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), null)))
								.then(sortNodeForPlayerVisibility(page,
										context -> WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), silent))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortNodeForPlayerVisibility(int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean silent) {
		return Commands.literal("sort")
				.then(sortKeyNodeForPlayerVisibility("name", SortKey.NAME, page, filter, silent))
				.then(sortKeyNodeForPlayerVisibility("sequence", SortKey.SEQUENCE, page, filter, silent));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortKeyNodeForPlayerVisibility(String literal, SortKey key,
			int page, Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean silent) {
		return Commands.literal(literal)
				.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, silent,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> WarpMutationHandler.setPlayerMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForGlobalVisibility(int page) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForGlobalVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix"))))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), null)))
								.then(sortNodeForGlobalVisibility(page,
										context -> WaypointFilter.dimension(StringArgumentType.getString(context, "dimension"))))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortNodeForGlobalVisibility(int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter) {
		return Commands.literal("sort")
				.then(sortKeyNodeForGlobalVisibility("name", SortKey.NAME, page, filter))
				.then(sortKeyNodeForGlobalVisibility("sequence", SortKey.SEQUENCE, page, filter));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortKeyNodeForGlobalVisibility(String literal, SortKey key,
			int page, Function<CommandContext<CommandSourceStack>, WaypointFilter> filter) {
		return Commands.literal(literal)
				.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> WarpMutationHandler.setGlobalMapVisibility(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}

	private static boolean requiresAdminPlayer(CommandSourceStack source) {
		return requiresPlayer(source) && source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	private static int resolvePage(CommandContext<CommandSourceStack> context, int page) {
		if (page != CONTEXT_PAGE) {
			return page;
		}
		return IntegerArgumentType.getInteger(context, "page");
	}
}
