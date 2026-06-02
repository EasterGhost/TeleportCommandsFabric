package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.*;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.function.Function;

final class HomeNodeFactory {
	private static final int CONTEXT_PAGE = Integer.MIN_VALUE;
	private static final String ARG_DISABLE_SAFETY = "disableSafety";
	private static final HomeSuggestionProvider HOME_SUGGESTIONS = new HomeSuggestionProvider();
	private static final HomeSuggestionProvider DEFAULT_HOME_SUGGESTIONS = new HomeSuggestionProvider(home -> !home.isTemporary());

	private HomeNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode(String literal, boolean temporary) {
		return Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
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
				.executes(context -> HomeTeleportHandler.teleportHome(context.getSource().getPlayerOrException(), null, false))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> HomeTeleportHandler.teleportHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), false))
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
			root.then(filterNode(WaypointListQuery.DEFAULT_PAGE, pagePicker));
			root.then(sortNode(WaypointListQuery.DEFAULT_PAGE, ignored -> WaypointFilter.none(), pagePicker));
		}
		root.then(Commands.argument("page", IntegerArgumentType.integer(1))
				.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
						new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null), pagePicker))
				.then(filterNode(CONTEXT_PAGE, pagePicker))
				.then(sortNode(CONTEXT_PAGE, ignored -> WaypointFilter.none(), pagePicker)));
		return root;
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> HomeMutationHandler.setMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.then(Commands.argument("page", IntegerArgumentType.integer(1))
					.executes(context -> HomeMutationHandler.setMapVisibility(context, true,
							new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
					.then(filterNodeForVisibility(CONTEXT_PAGE, true))
					.then(sortNodeForVisibility(CONTEXT_PAGE, ignored -> WaypointFilter.none(), true)));
		}
		return Commands.literal(literal)
				.requires(HomeNodeFactory::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(visibleNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNode(int page, boolean pagePicker) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null),
										pagePicker))
								.then(sortNode(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), pagePicker))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
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
				.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection())), pagePicker))
				.then(Commands.literal("asc")
						.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)), pagePicker)))
				.then(Commands.literal("desc")
						.executes(context -> HomeListHandler.renderHomes(context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)), pagePicker)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForVisibility(int page, boolean silent) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> HomeMutationHandler.setMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), silent))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> HomeMutationHandler.setMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), null)))
								.then(sortNodeForVisibility(page,
										context -> WaypointFilter.dimension(StringArgumentType.getString(context, "dimension")), silent))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortNodeForVisibility(int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean silent) {
		return Commands.literal("sort")
				.then(sortKeyNodeForVisibility("name", SortKey.NAME, page, filter, silent))
				.then(sortKeyNodeForVisibility("sequence", SortKey.SEQUENCE, page, filter, silent));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortKeyNodeForVisibility(String literal, SortKey key, int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, boolean silent) {
		return Commands.literal(literal)
				.executes(context -> HomeMutationHandler.setMapVisibility(context, silent,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> HomeMutationHandler.setMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> HomeMutationHandler.setMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}

	private static int resolvePage(CommandContext<CommandSourceStack> context, int page) {
		if (page != CONTEXT_PAGE) {
			return page;
		}
		return IntegerArgumentType.getInteger(context, "page");
	}
}
