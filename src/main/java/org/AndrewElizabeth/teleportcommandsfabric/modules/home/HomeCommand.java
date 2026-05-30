package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

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
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class HomeCommand {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;
	private static final long TEMP_HOME_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
	private static final HomeSuggestionProvider HOME_SUGGESTIONS = new HomeSuggestionProvider();
	private static final HomeSuggestionProvider DEFAULT_HOME_SUGGESTIONS = new HomeSuggestionProvider(home -> !home.isTemporary());

	private HomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildSetNode("sethome", false));
		dispatcher.register(buildSetNode("tmphome", true));
		dispatcher.register(buildUpdateNode());
		dispatcher.register(buildTeleportNode());
		dispatcher.register(buildDeleteNode());
		dispatcher.register(buildRenameNode());
		dispatcher.register(buildDefaultNode());
		dispatcher.register(buildListNode("homes", false));
		dispatcher.register(buildListNode("teleportcommandsfabric:homespages", true));
		dispatcher.register(buildMapVisibilityNode("maphome", false));
		dispatcher.register(buildMapVisibilityNode("teleportcommandsfabric:maphome", true));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSetNode(String literal, boolean temporary) {
		return Commands.literal(literal)
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> setHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), temporary)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return Commands.literal("updatehome")
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> updateHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("home")
				.requires(HomeCommand::requiresPlayer)
				.executes(context -> teleportHome(context.getSource().getPlayerOrException(), null, false))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> teleportHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"), false))
						.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
								.executes(context -> teleportHome(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										BoolArgumentType.getBool(context, "Disable Safety")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delhome")
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.executes(context -> deleteHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamehome")
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> renameHome(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "name"),
										StringArgumentType.getString(context, "newName")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return Commands.literal("defaulthome")
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(DEFAULT_HOME_SUGGESTIONS)
						.executes(context -> setDefaultHome(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "name"))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildListNode(String literal, boolean pagePicker) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
				.requires(HomeCommand::requiresPlayer);
		if (!pagePicker) {
			root.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
					WaypointListQuery.defaultQuery(), false));
			root.then(filterNode(WaypointListQuery.DEFAULT_PAGE, pagePicker));
			root.then(sortNode(WaypointListQuery.DEFAULT_PAGE, ignored -> WaypointFilter.none(), pagePicker));
		}
		root.then(Commands.argument("page", IntegerArgumentType.integer(1))
				.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
						new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null), pagePicker))
				.then(filterNode(contextPage(), pagePicker))
				.then(sortNode(contextPage(), ignored -> WaypointFilter.none(), pagePicker)));
		return root;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode(String literal, boolean silent) {
		var visibleNode = Commands.argument("visible", BoolArgumentType.bool());
		if (!silent) {
			visibleNode.executes(context -> setMapVisibility(context, false, null));
		}
		if (silent) {
			visibleNode.then(Commands.argument("page", IntegerArgumentType.integer(1))
					.executes(context -> setMapVisibility(context, true,
							new WaypointListQuery(IntegerArgumentType.getInteger(context, "page"), null, null)))
					.then(filterNodeForVisibility(contextPage(), true))
					.then(sortNodeForVisibility(contextPage(), ignored -> WaypointFilter.none(), true)));
		}
		return Commands.literal(literal)
				.requires(HomeCommand::requiresPlayer)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(HOME_SUGGESTIONS)
						.then(visibleNode));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNode(int page, boolean pagePicker) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null),
										pagePicker))
								.then(sortNode(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), pagePicker))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
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
				.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection())), pagePicker))
				.then(Commands.literal("asc")
						.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)), pagePicker)))
				.then(Commands.literal("desc")
						.executes(context -> renderHomes(context.getSource().getPlayerOrException(),
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)), pagePicker)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterNodeForVisibility(int page, boolean silent) {
		return Commands.literal("filter")
				.then(Commands.literal("prefix")
						.then(Commands.argument("prefix", StringArgumentType.string())
								.executes(context -> setMapVisibility(context, silent,
										new WaypointListQuery(resolvePage(context, page),
												WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), null)))
								.then(sortNodeForVisibility(page,
										context -> WaypointFilter.prefix(StringArgumentType.getString(context, "prefix")), silent))))
				.then(Commands.literal("dimension")
						.then(Commands.argument("dimension", StringArgumentType.string())
								.executes(context -> setMapVisibility(context, silent,
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
				.executes(context -> setMapVisibility(context, silent,
						new WaypointListQuery(resolvePage(context, page), filter.apply(context),
								new WaypointSort(key, SortDirection.defaultDirection()))))
				.then(Commands.literal("asc")
						.executes(context -> setMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.ASC)))))
				.then(Commands.literal("desc")
						.executes(context -> setMapVisibility(context, silent,
								new WaypointListQuery(resolvePage(context, page), filter.apply(context),
										new WaypointSort(key, SortDirection.DESC)))));
	}

	private static int setHome(ServerPlayer player, String name, boolean temporary) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		CompletableFuture<WaypointOperationResult> result = temporary
				? WaypointCrudService.addTemporary(player, name, System.currentTimeMillis() + TEMP_HOME_TTL_MS, source)
				: WaypointCrudService.add(player, name, source);
		handleMutationResult(player, result, temporary ? "commands.teleport_commands.home.tempSet" : "commands.teleport_commands.home.set",
				temporary ? "Error while setting a temporary home." : "Error while setting a home.");
		return 0;
	}

	private static int updateHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleMutationResult(player, WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.home.update", "Error while updating a home.");
		return 0;
	}

	private static int deleteHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleMutationResult(player, WaypointCrudService.delete(name, source),
				"commands.teleport_commands.home.delete", "Error while deleting a home.");
		return 0;
	}

	private static int renameHome(ServerPlayer player, String oldName, String newName) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleMutationResult(player, WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.home.rename", "Error while renaming a home.");
		return 0;
	}

	private static int setDefaultHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleMutationResult(player, WaypointCrudService.setDefault(name, source),
				"commands.teleport_commands.home.default", "Error while setting default home.");
		return 0;
	}

	private static int setMapVisibility(CommandContext<CommandSourceStack> context, boolean silent, WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return 1;
		}
		if (!ensureEnabled(player, silent)) {
			return 1;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			if (!silent) {
				HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			}
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
				ModConstants.LOGGER.error("Error while updating home map visibility.", throwable);
				if (!silent) {
					HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error", ChatFormatting.RED, ChatFormatting.BOLD);
				}
				return;
			}
			if (!silent) {
				sendVisibilityResult(currentPlayer, result, visible);
				return;
			}
			if (query != null) {
				renderHomes(currentPlayer, query, false);
			}
		}));
		return 0;
	}

	private static int teleportHome(ServerPlayer player, String name, boolean safetyDisabled) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		HomeCommandSettings settings = ConfigManager.query(HomeCommand::settingsFrom);
		AsyncWaypointSource source = source(player);
		if (source == null || TeleportCommands.TELEPORT_SERVICE == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		resolveHome(player, name).whenComplete((location, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while resolving home.", throwable);
				HomeMessages.send(currentPlayer, "commands.teleport_commands.home.goError", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				HomeMessages.send(currentPlayer,
						name == null || name.isBlank() ? "commands.teleport_commands.home.defaultNone" : "commands.teleport_commands.home.notFound",
						ChatFormatting.AQUA);
				return;
			}
			executeTeleport(currentPlayer, location.get(), source, settings, safetyDisabled);
		}));
		return 0;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView home, AsyncWaypointSource source,
			HomeCommandSettings settings, boolean safetyDisabled) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(home.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /home for {}: world {} was not found.",
					player.getName().getString(), home.getDimensionId());
			HomeMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidHomes()) {
				WaypointCrudService.delete(home.getName(), source).whenComplete((ignored, throwable) -> server.execute(() -> {
					if (throwable == null) {
						HomeMessages.send(player, "commands.teleport_commands.home.deletedInvalid", ChatFormatting.YELLOW);
					}
				}));
			}
			return;
		}
		if (player.level().dimension().equals(home.getDimension()) && player.blockPosition().equals(home.getBlockPos())) {
			HomeMessages.send(player, "commands.teleport_commands.home.goSame", ChatFormatting.AQUA);
			return;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(!safetyDisabled)
				.recordPrevious(true)
				.build();
		TeleportTarget target = TeleportTarget.of(world, new Vec3(
				home.getX() + 0.5D,
				home.getYPrecise(),
				home.getZ() + 0.5D));
		TeleportRequest request = TeleportRequest.resolved(target, options);
		String forceCommand = "home " + CommandArgumentUtils.quote(home.getName()) + " true";
		try {
			TeleportService service = TeleportCommands.TELEPORT_SERVICE;
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				HomeMessages.sendStatus(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return;
			}
			if (settings.delaySeconds() > 0) {
				HomeMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				HomeMessages.send(player, "commands.teleport_commands.home.go", ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /home teleport.", throwable);
					HomeMessages.send(currentPlayer, "commands.teleport_commands.home.goError", ChatFormatting.RED,
							ChatFormatting.BOLD);
					return;
				}
				HomeMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /home teleport.", exception);
			HomeMessages.send(player, "commands.teleport_commands.home.goError", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static int renderHomes(ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		UUID playerUuid = player.getUUID();
		manager.query(playerUuid, profile -> new HomePageData(profile.getHomes(), profile.getDefaultHomeUuid()))
				.whenComplete((data, throwable) -> player.level().getServer().execute(() -> {
					ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(playerUuid);
					if (currentPlayer == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering homes.", throwable);
						HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					List<NamedLocationView> filtered = WaypointRows.filterAndSort(data.homes(), query);
					if (filtered.isEmpty()) {
						if (query.filter() instanceof WaypointFilter.Dimension dimension) {
							HomeMessages.sendNoHomesInDimension(currentPlayer, dimension.dimensionId());
						} else {
							HomeMessages.send(currentPlayer, "commands.teleport_commands.home.homeless", ChatFormatting.AQUA);
						}
						return;
					}
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.HOMES, data.homes(), Set.of(),
							data.defaultHomeUuid(), true, query, language(currentPlayer));
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
							ModConstants.LOGGER.error("Error while rendering homes page.", renderThrowable);
							HomeMessages.send(target, "commands.teleport_commands.homes.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					}));
				}));
		return 0;
	}

	private static void handleMutationResult(ServerPlayer player, CompletableFuture<WaypointOperationResult> future,
			String successKey, String logMessage) {
		HomeCommandSettings settings = ConfigManager.query(HomeCommand::settingsFrom);
		future.whenComplete((result, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error(logMessage, throwable);
				HomeMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			sendMutationResult(currentPlayer, result, successKey, settings.maxHomes());
		}));
	}

	private static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxHomes) {
		switch (result) {
		case SUCCESS -> HomeMessages.send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> HomeMessages.send(player, "commands.teleport_commands.home.updateSame", ChatFormatting.AQUA);
		case SAME_DEFAULT -> HomeMessages.send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
		case NOT_FOUND -> HomeMessages.send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> HomeMessages.send(player, "commands.teleport_commands.home.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> HomeMessages.sendMaxReached(player, maxHomes);
		case TEMP_HOME_EXISTS -> HomeMessages.send(player, "commands.teleport_commands.home.tempExists", ChatFormatting.RED);
		case CANNOT_BE_DEFAULT -> HomeMessages.send(player, "commands.teleport_commands.home.defaultTemporary", ChatFormatting.RED);
		default -> HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> HomeMessages.send(player, visible
				? "commands.teleport_commands.home.mapShown"
				: "commands.teleport_commands.home.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> HomeMessages.send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		default -> HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static CompletableFuture<Optional<NamedLocationView>> resolveHome(ServerPlayer player, String name) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		UUID playerUuid = player.getUUID();
		if (name != null && !name.isBlank()) {
			return manager.query(playerUuid, profile -> profile.getHomeByName(name).map(home -> (NamedLocationView) home));
		}
		return manager.query(playerUuid, profile -> profile.getDefaultHomeLocation().map(home -> (NamedLocationView) home));
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		return ensureEnabled(player, false);
	}

	private static boolean ensureEnabled(ServerPlayer player, boolean silent) {
		if (ConfigManager.query(config -> config.getHome().isEnabled())) {
			return true;
		}
		if (!silent) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
		}
		return false;
	}

	private static AsyncWaypointSource source(ServerPlayer player) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new PlayerHomeSource(player.getUUID(), manager, () -> ConfigManager.query(config -> config.getHome().getPlayerMaximum()));
	}

	private static boolean requiresPlayer(CommandSourceStack source) {
		return source.getPlayer() != null;
	}

	private static String language(ServerPlayer player) {
		return player.clientInformation().language().toLowerCase();
	}

	private static HomeCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new HomeCommandSettings(delaySeconds, delaySeconds * TICKS_PER_SECOND,
				cooldownSeconds, cooldownSeconds * MILLIS_PER_SECOND,
				config.getHome().getPlayerMaximum(), config.getHome().isDeleteInvalid());
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

	private record HomeCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			int maxHomes, boolean deleteInvalidHomes) {
	}

	private record HomePageData(List<NamedLocationView> homes, UUID defaultHomeUuid) {
	}

}
