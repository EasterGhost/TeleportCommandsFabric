package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortKey;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.function.Function;

public final class WaypointQueryNodes {
	public static final int CONTEXT_PAGE = Integer.MIN_VALUE;

	private static final String ARG_PAGE = "page";
	private static final String ARG_PREFIX = "prefix";
	private static final String ARG_DIMENSION = "dimension";

	private WaypointQueryNodes() {
	}

	public static RequiredArgumentBuilder<CommandSourceStack, Integer> pageArgument(QueryExecutor executor) {
		return Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
				.executes(context -> executor.run(context,
						new WaypointListQuery(IntegerArgumentType.getInteger(context, ARG_PAGE), null, null)))
				.then(filterNode(CONTEXT_PAGE, executor))
				.then(sortNode(CONTEXT_PAGE, ignored -> WaypointFilter.none(), executor));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> filterNode(int page, QueryExecutor executor) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument(ARG_PREFIX, StringArgumentType.string())
								.executes(context -> executor.run(context,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, ARG_PREFIX)), null)))
								.then(sortNode(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, ARG_PREFIX)),
										executor))))
				.then(Commands.literal("dimension")
						.then(Commands.argument(ARG_DIMENSION, StringArgumentType.string())
								.executes(context -> executor.run(context,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.dimension(StringArgumentType.getString(context, ARG_DIMENSION)), null)))
								.then(sortNode(page,
										context -> WaypointFilter.dimension(StringArgumentType.getString(context, ARG_DIMENSION)),
										executor))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> sortNode(int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, QueryExecutor executor) {
		return Commands.literal("sort")
				.then(sortKeyNode("name", SortKey.NAME, page, filter, executor))
				.then(sortKeyNode("sequence", SortKey.SEQUENCE, page, filter, executor));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> sortKeyNode(String literal, SortKey key, int page,
			Function<CommandContext<CommandSourceStack>, WaypointFilter> filter, QueryExecutor executor) {
		return Commands.literal(literal)
				.executes(context -> executor.run(context,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> executor.run(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> executor.run(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static int resolvePage(CommandContext<CommandSourceStack> context, int page) {
		if (page != CONTEXT_PAGE) {
			return page;
		}
		return IntegerArgumentType.getInteger(context, ARG_PAGE);
	}

	@FunctionalInterface
	public interface QueryExecutor {
		int run(CommandContext<CommandSourceStack> context, WaypointListQuery query) throws CommandSyntaxException;
	}
}
