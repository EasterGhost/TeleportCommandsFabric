package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportOptions;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.GlobalWarpSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRows;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.*;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class WarpCommand {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;
	private static final WarpSuggestionProvider WARP_SUGGESTIONS = new WarpSuggestionProvider();

	private WarpCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildSetNode());
		dispatcher.register(buildUpdateNode());
		dispatcher.register(buildTeleportNode());
		dispatcher.register(buildDeleteNode());
		dispatcher.register(buildRenameNode());
		dispatcher.register(buildListNode("warps", false));
		dispatcher.register(buildListNode("teleportcommandsfabric:warpspages", true));
		dispatcher.register(buildPlayerMapVisibilityNode("mapwarp", false));
		dispatcher.register(buildPlayerMapVisibilityNode("teleportcommandsfabric:mapwarp", true));
		dispatcher.register(buildPublicGlobalMapVisibilityNode());
		dispatcher.register(buildGlobalMapVisibilityNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("setwarp")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> setWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return Commands.literal("updatewarp")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> updateWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("warp")
				.requires(WarpCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> teleportWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), false))
						.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
								.executes(context -> teleportWarp(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, "Disable Safety")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delwarp")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.executes(context -> deleteWarp(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamewarp")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> renameWarp(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										StringArgumentType.getString(context, "newName")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildListNode(String literal, boolean pagePicker) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(WarpCommand::requiresPlayer);
		if (!pagePicker) {
			root.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
					WaypointListQuery.defaultQuery(), false));
			root.then(filterNode(WaypointListQuery.DEFAULT_PAGE, pagePicker));
			root.then(sortNode(WaypointListQuery.DEFAULT_PAGE, ignored -> WaypointFilter.none(), pagePicker));
		}
		root.then(Commands.argument("page", IntegerArgumentType.integer(1))
				.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
						new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null), pagePicker))
				.then(filterNode(contextPage(), pagePicker))
				.then(sortNode(contextPage(), ignored -> WaypointFilter.none(), pagePicker)));
		return root;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildPlayerMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> setPlayerMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.then(Commands.argument("page", IntegerArgumentType.integer(1))
					.executes(context -> setPlayerMapVisibility(context, true,
							new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
					.then(filterNodeForPlayerVisibility(contextPage(), true))
					.then(sortNodeForPlayerVisibility(contextPage(), ignored -> WaypointFilter.none(), true)));
		}
		return Commands.literal(literal)
				.requires(WarpCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(visibleNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildGlobalMapVisibilityNode() {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool())
				.then(Commands.argument("page", IntegerArgumentType.integer(1))
						.executes(context -> setGlobalMapVisibility(context,
								new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
						.then(filterNodeForGlobalVisibility(contextPage()))
						.then(sortNodeForGlobalVisibility(contextPage(), ignored -> WaypointFilter.none())));
		return Commands.literal("teleportcommandsfabric:gmapwarp")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(visibleNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildPublicGlobalMapVisibilityNode() {
		return Commands.literal("gwarpmap")
				.requires(WarpCommand::requiresAdminPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(WARP_SUGGESTIONS)
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> setGlobalMapVisibility(context, null))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNode(int page, boolean pagePicker) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null),
										pagePicker))
								.then(sortNode(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), pagePicker))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
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
				.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection())), pagePicker))
				.then(Commands.literal("asc")
						.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)), pagePicker)))
				.then(Commands.literal("desc")
						.executes(context -> renderWarps(context.getSource(), context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)), pagePicker)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForPlayerVisibility(int page, boolean silent) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> setPlayerMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForPlayerVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), silent))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> setPlayerMapVisibility(context, silent,
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
				.executes(context -> setPlayerMapVisibility(context, silent,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> setPlayerMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> setPlayerMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForGlobalVisibility(int page) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> setGlobalMapVisibility(context,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForGlobalVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix"))))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> setGlobalMapVisibility(context,
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
				.executes(context -> setGlobalMapVisibility(context,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> setGlobalMapVisibility(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> setGlobalMapVisibility(context,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static int setWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.add(player, name, source),
				"commands.teleport_commands.warp.set", "Error while setting a warp.");
		return 0;
	}

	private static int updateWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.warp.update", "Error while updating a warp.");
		return 0;
	}

	private static int deleteWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.delete(name, source),
				"commands.teleport_commands.warp.delete", "Error while deleting a warp.");
		return 0;
	}

	private static int renameWarp(ServerPlayer player, String oldName, String newName) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.warp.rename", "Error while renaming a warp.");
		return 0;
	}

	private static int setPlayerMapVisibility(CommandContext<CommandSourceStack> context, boolean silent,
			WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return 1;
		}
		if (!ensureEnabled(player, silent)) {
			return 1;
		}
		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		updatePlayerMapVisibility(player, name, visible).whenComplete((result, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating warp map visibility.", throwable);
				if (!silent) {
					WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error", ChatFormatting.RED, ChatFormatting.BOLD);
				}
				return;
			}
			if (!silent) {
				sendPlayerVisibilityResult(currentPlayer, result, visible);
				return;
			}
			if (query != null) {
				renderWarps(context.getSource(), currentPlayer, query, false);
			}
		}));
		return 0;
	}

	private static int setGlobalMapVisibility(CommandContext<CommandSourceStack> context, WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return 1;
		}
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		WaypointCrudService.updateVisibility(name, visible, source).whenComplete((result, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating global warp map visibility.", throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.gwarpmap.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (result == WaypointOperationResult.SUCCESS && TeleportCommands.WAYPOINT_PAGES != null) {
				TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
			}
			sendGlobalVisibilityResult(currentPlayer, result, visible);
			if (query != null) {
				renderWarps(context.getSource(), currentPlayer, query, false);
			}
		}));
		return 0;
	}

	private static int teleportWarp(ServerPlayer player, String name, boolean safetyDisabled) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		WarpCommandSettings settings = ConfigManager.query(WarpCommand::settingsFrom);
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER == null || TeleportCommands.TELEPORT_SERVICE == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		resolveWarp(name).whenComplete((location, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while resolving warp.", throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.goError", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
				return;
			}
			executeTeleport(currentPlayer, location.get(), settings, safetyDisabled);
		}));
		return 0;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView warp, WarpCommandSettings settings,
			boolean safetyDisabled) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(warp.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /warp for {}: world {} was not found.",
					player.getName().getString(), warp.getDimensionId());
			WarpMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidWarps()) {
				AsyncWaypointSource source = source();
				if (source != null) {
					WaypointCrudService.delete(warp.getName(), source).whenComplete((ignored, throwable) -> server.execute(() -> {
						if (throwable == null) {
							if (TeleportCommands.WAYPOINT_PAGES != null) {
								TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
							}
							WarpMessages.send(player, "commands.teleport_commands.warp.deletedInvalid", ChatFormatting.YELLOW);
						}
					}));
				}
			}
			return;
		}
		if (player.level().dimension().equals(warp.getDimension()) && player.blockPosition().equals(warp.getBlockPos())) {
			WarpMessages.send(player, "commands.teleport_commands.warp.goSame", ChatFormatting.AQUA);
			return;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(!safetyDisabled)
				.recordPrevious(true)
				.build();
		TeleportTarget target = TeleportTarget.of(world, new Vec3(
				warp.getX() + 0.5D,
				warp.getYPrecise(),
				warp.getZ() + 0.5D));
		TeleportRequest request = TeleportRequest.resolved(target, options);
		String forceCommand = "warp " + CommandArgumentUtils.quote(warp.getName()) + " true";
		try {
			TeleportService service = TeleportCommands.TELEPORT_SERVICE;
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				WarpMessages.sendStatus(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return;
			}
			if (settings.delaySeconds() > 0) {
				WarpMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				WarpMessages.send(player, "commands.teleport_commands.warp.go", ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /warp teleport.", throwable);
					WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.goError", ChatFormatting.RED,
							ChatFormatting.BOLD);
					return;
				}
				WarpMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /warp teleport.", exception);
			WarpMessages.send(player, "commands.teleport_commands.warp.goError", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static int renderWarps(CommandSourceStack source, ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (globalManager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		UUID playerUuid = player.getUUID();
		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<Set<UUID>> hiddenFuture = hiddenWarps(playerUuid);
		warpsFuture.thenCombine(hiddenFuture, WarpPageData::new)
				.whenComplete((data, throwable) -> player.level().getServer().execute(() -> {
					ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(playerUuid);
					if (currentPlayer == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering warps.", throwable);
						WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					List<NamedLocationView> filtered = WaypointRows.filterAndSort(data.warps(), query);
					if (filtered.isEmpty()) {
						if (query.filter() instanceof WaypointFilter.Dimension dimension) {
							WarpMessages.sendNoWarpsInDimension(currentPlayer, dimension.dimensionId());
						} else {
							WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.homeless", ChatFormatting.AQUA);
						}
						return;
					}
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.WARPS, data.warps(), data.hiddenWarpUuids(),
							null, isAdmin(source), query, language(currentPlayer));
					if (pagePicker) {
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderPagePicker(request), false);
						return;
					}
					TeleportCommands.WAYPOINT_PAGES.render(request).whenComplete((component, renderThrowable) -> player.level().getServer().execute(() -> {
						ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(playerUuid);
						if (target == null) {
							return;
						}
						if (renderThrowable != null) {
							ModConstants.LOGGER.error("Error while rendering warps page.", renderThrowable);
							WarpMessages.send(target, "commands.teleport_commands.warps.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					}));
				}));
		return 0;
	}

	private static void handleGlobalMutationResult(ServerPlayer player, CompletableFuture<WaypointOperationResult> future,
			String successKey, String logMessage) {
		WarpCommandSettings settings = ConfigManager.query(WarpCommand::settingsFrom);
		future.whenComplete((result, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error(logMessage, throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (result == WaypointOperationResult.SUCCESS && TeleportCommands.WAYPOINT_PAGES != null) {
				TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
			}
			sendMutationResult(currentPlayer, result, successKey, settings.maxWarps());
		}));
	}

	private static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxWarps) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> WarpMessages.send(player, "commands.teleport_commands.warp.updateSame", ChatFormatting.AQUA);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> WarpMessages.send(player, "commands.teleport_commands.warp.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> WarpMessages.sendMaxReached(player, maxWarps);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendPlayerVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, visible
				? "commands.teleport_commands.warp.playerMapShown"
				: "commands.teleport_commands.warp.playerMapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendGlobalVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, visible
				? "commands.teleport_commands.warp.mapShown"
				: "commands.teleport_commands.warp.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static CompletableFuture<Optional<NamedLocationView>> resolveWarp(String name) {
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return manager.query(profile -> profile.getWarpByName(name));
	}

	private static CompletableFuture<WaypointOperationResult> updatePlayerMapVisibility(ServerPlayer player, String warpName,
			boolean visible) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(WaypointOperationResult.INTERNAL_ERROR);
		}
		UUID playerUuid = player.getUUID();
		return globalManager.query(profile -> profile.getWarpByName(warpName))
				.thenCompose(warp -> {
					if (warp.isEmpty()) {
						return CompletableFuture.completedFuture(WaypointOperationResult.NOT_FOUND);
					}
					UUID warpUuid = warp.get().getUuid();
					return playerManager.mutate(playerUuid, profile -> {
						if (visible) {
							profile.showWarp(warpUuid);
						} else {
							profile.hideWarp(warpUuid);
						}
						return WaypointOperationResult.SUCCESS;
					});
				});
	}

	private static CompletableFuture<Set<UUID>> hiddenWarps(UUID playerUuid) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Set.of());
		}
		return manager.query(playerUuid, profile -> profile.getHiddenWarpUuids())
				.exceptionally(throwable -> Set.of());
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		return ensureEnabled(player, false);
	}

	private static boolean ensureEnabled(ServerPlayer player, boolean silent) {
		if (ConfigManager.query(config -> config.getWarp().isEnabled())) {
			return true;
		}
		if (!silent) {
			WarpMessages.send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		}
		return false;
	}

	private static AsyncWaypointSource source() {
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new GlobalWarpSource(manager, () -> ConfigManager.query(config -> config.getWarp().getMaximum()));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}

	private static boolean requiresAdminPlayer(CommandSourceStack source) {
		return requiresPlayer(source) && isAdmin(source);
	}

	private static boolean isAdmin(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	private static String language(ServerPlayer player) {
		return player.clientInformation().language().toLowerCase();
	}

	private static WarpCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new WarpCommandSettings(delaySeconds, delaySeconds * TICKS_PER_SECOND,
				cooldownSeconds, cooldownSeconds * MILLIS_PER_SECOND,
				config.getWarp().getMaximum(), config.getWarp().isDeleteInvalid());
	}

	private static int contextPage() {
		return Integer.MIN_VALUE;
	}

	private static int resolvePage(CommandContext<CommandSourceStack> context, int page) {
		if (page != Integer.MIN_VALUE) {
			return page;
		}
		return IntegerArgumentType.getInteger(context, "page");
	}

	private record WarpCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			int maxWarps, boolean deleteInvalidWarps) {
	}

	private record WarpPageData(List<NamedLocationView> warps, Set<UUID> hiddenWarpUuids) {
	}
}
