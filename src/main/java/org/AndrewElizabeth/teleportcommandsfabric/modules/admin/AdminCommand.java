package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.HomeConfig;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.RtpConfig;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.StorageConfig;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.TeleportingConfig;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.WarpConfig;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminStatusRenderer;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

public final class AdminCommand {
	private static final String PRIMARY_COMMAND = "tpc";
	private static final List<String> CONFIG_MODULES = List.of(
			"teleporting", "back", "home", "tpa", "warp", "worldspawn", "rtp", "xaero", "storage");
	private static final AdminHelpRenderer HELP_RENDERER = new AdminHelpRenderer();
	private static final AdminStatusRenderer STATUS_RENDERER = new AdminStatusRenderer();
	private static final SuggestionProvider<CommandSourceStack> ENABLED_SUGGESTER = (context,
			builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.enabledNames(), builder);
	private static final SuggestionProvider<CommandSourceStack> DISABLED_SUGGESTER = (context,
			builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.disabledNames(), builder);
	private static final SuggestionProvider<CommandSourceStack> CONFIG_MODULE_SUGGESTER = (context,
			builder) -> SharedSuggestionProvider.suggest(CONFIG_MODULES, builder);

	private AdminCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildRootCommand(PRIMARY_COMMAND));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRootCommand(String literal) {
		return Commands.literal(literal)
				.then(buildConfigNode())
				.then(buildDebugNode())
				.then(buildStatusNode())
				.then(buildReloadNode())
				.then(buildDisableNode())
				.then(buildEnableNode())
				.then(buildHelpNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildConfigNode() {
		return Commands.literal("config")
				.requires(AdminCommand::isOpOrConsole)
				.then(buildTeleportingConfigNode())
				.then(buildBackConfigNode())
				.then(buildHomeConfigNode())
				.then(buildTpaConfigNode())
				.then(buildWarpConfigNode())
				.then(buildWorldSpawnConfigNode())
				.then(buildRtpConfigNode())
				.then(buildXaeroConfigNode())
				.then(buildStorageConfigNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportingConfigNode() {
		return Commands.literal("teleporting")
				.then(AdminConfigNodeFactory.intNode("delay", "seconds", 0,
						config -> config.getTeleporting().getDelay(),
						(config, value) -> config.getTeleporting().setDelay(value),
						"commands.teleport_commands.admin.config.teleporting.delay"))
				.then(AdminConfigNodeFactory.intNode("cooldown", "seconds", 0,
						config -> config.getTeleporting().getCooldown(),
						(config, value) -> config.getTeleporting().setCooldown(value),
						"commands.teleport_commands.admin.config.teleporting.cooldown"))
				.then(AdminConfigNodeFactory.boolNode("effects",
						config -> config.getTeleporting().isTeleportEffects(),
						(config, value) -> config.getTeleporting().setTeleportEffects(value),
						"commands.teleport_commands.admin.config.teleporting.effects"))
				.then(AdminConfigNodeFactory.boolNode("restoreRotation",
						config -> config.getTeleporting().isRestoreRotation(),
						(config, value) -> config.getTeleporting().setRestoreRotation(value),
						"commands.teleport_commands.admin.config.teleporting.restoreRotation"))
				.then(AdminConfigNodeFactory.boolNode("preload",
						config -> config.getTeleporting().isPreloadEnabled(),
						(config, value) -> config.getTeleporting().setPreloadEnabled(value),
						"commands.teleport_commands.admin.config.teleporting.preload"))
				.then(AdminConfigNodeFactory.intNode("preloadRadius", "chunks",
						TeleportingConfig.MIN_PRELOAD_RADIUS_CHUNKS,
						config -> config.getTeleporting().getPreloadRadiusChunks(),
						(config, value) -> config.getTeleporting().setPreloadRadiusChunks(value),
						"commands.teleport_commands.admin.config.teleporting.preloadRadius"))
				.then(AdminConfigNodeFactory.boolNode("defaultSafetyCheck",
						config -> config.getTeleporting().isDefaultSafetyCheck(),
						(config, value) -> config.getTeleporting().setDefaultSafetyCheck(value),
						"commands.teleport_commands.admin.config.teleporting.defaultSafetyCheck"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackConfigNode() {
		return Commands.literal("back")
				.then(AdminConfigNodeFactory.boolNode("deleteAfterTeleport",
						config -> config.getBack().isDeleteAfterTeleport(),
						(config, value) -> config.getBack().setDeleteAfterTeleport(value),
						"commands.teleport_commands.admin.config.back.deleteAfterTeleport"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHomeConfigNode() {
		return Commands.literal("home")
				.then(AdminConfigNodeFactory.intNode("max", "count", HomeConfig.MIN_PLAYER_MAXIMUM,
						config -> config.getHome().getPlayerMaximum(),
						(config, value) -> config.getHome().setPlayerMaximum(value),
						"commands.teleport_commands.admin.config.home.max"))
				.then(AdminConfigNodeFactory.boolNode("deleteInvalid",
						config -> config.getHome().isDeleteInvalid(),
						(config, value) -> config.getHome().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.home.deleteInvalid"))
				.then(AdminConfigNodeFactory.intNode("temporaryHomeTtl", "seconds", HomeConfig.MIN_TEMPORARY_HOME_TTL_SECONDS,
						config -> config.getHome().getTemporaryHomeTtlSeconds(),
						(config, value) -> config.getHome().setTemporaryHomeTtlSeconds(value),
						"commands.teleport_commands.admin.config.home.temporaryHomeTtl"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTpaConfigNode() {
		return Commands.literal("tpa")
				.then(AdminConfigNodeFactory.durationSecondsNode("expireTime", "seconds", 0,
						config -> config.getTpa().getRequestExpireTime(),
						(config, value) -> config.getTpa().setRequestExpireTime(value),
						"commands.teleport_commands.admin.config.tpa.expireTime"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWarpConfigNode() {
		return Commands.literal("warp")
				.then(AdminConfigNodeFactory.intNode("max", "count", WarpConfig.MIN_MAXIMUM,
						config -> config.getWarp().getMaximum(),
						(config, value) -> config.getWarp().setMaximum(value),
						"commands.teleport_commands.admin.config.warp.max"))
				.then(AdminConfigNodeFactory.boolNode("deleteInvalid",
						config -> config.getWarp().isDeleteInvalid(),
						(config, value) -> config.getWarp().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.warp.deleteInvalid"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnConfigNode() {
		return Commands.literal("worldspawn")
				.then(AdminConfigNodeFactory.stringNode("world", "worldId",
						config -> config.getWorldSpawn().getWorld_id(),
						(config, value) -> config.getWorldSpawn().setWorld_id(value),
						"commands.teleport_commands.admin.config.worldspawn.world"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRtpConfigNode() {
		return Commands.literal("rtp")
				.then(AdminConfigNodeFactory.intNode("maxRadius", "blocks", RtpConfig.MIN_RADIUS, RtpConfig.MAX_RADIUS,
						config -> config.getRtp().getMaxRadius(),
						(config, value) -> config.getRtp().setMaxRadius(value),
						"commands.teleport_commands.admin.config.rtp.maxRadius"))
				.then(AdminConfigNodeFactory.intNode("minRadius", "blocks", RtpConfig.MIN_MIN_RADIUS,
						RtpConfig.MAX_RADIUS,
						config -> config.getRtp().getMinRadius(),
						(config, value) -> config.getRtp().setMinRadius(value),
						"commands.teleport_commands.admin.config.rtp.minRadius"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildXaeroConfigNode() {
		return Commands.literal("xaero")
				.then(AdminConfigNodeFactory.intNode("syncIntervalSeconds", "seconds", 1,
						config -> config.getXaero().getSyncIntervalSeconds(),
						(config, value) -> config.getXaero().setSyncIntervalSeconds(value),
						"commands.teleport_commands.admin.config.xaero.syncIntervalSeconds"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildStorageConfigNode() {
		return Commands.literal("storage")
				.then(AdminConfigNodeFactory.intNode("autoSaveIntervalSeconds", "seconds",
						StorageConfig.MIN_AUTO_SAVE_INTERVAL, StorageConfig.MAX_AUTO_SAVE_INTERVAL,
						config -> config.getStorage().getAutoSaveIntervalSeconds(),
						(config, value) -> config.getStorage().setAutoSaveIntervalSeconds(value),
						"commands.teleport_commands.admin.config.storage.autoSaveIntervalSeconds"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildReloadNode() {
		return Commands.literal("reload")
				.requires(AdminCommand::isOpOrConsole)
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					ConfigManager.reload().whenComplete((ignored, throwable) -> source.getServer().execute(() -> {
						if (throwable != null) {
							ModConstants.LOGGER.error("Failed to reload config.", throwable);
							AdminMessages.sendError(source, AdminMessages.t(source,
									"commands.teleport_commands.admin.reload.error",
									Component.literal(String.valueOf(throwable.getMessage()))));
						} else {
							AdminMessages.sendSuccess(source,
									AdminMessages.t(source, "commands.teleport_commands.admin.reload.success"), true);
						}
					}));
					return 0;
				});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDebugNode() {
		return Commands.literal("debug")
				.requires(AdminCommand::isOpOrConsole)
				.executes(context -> AdminMessages.sendCurrentValue(context.getSource(), "debug",
						enabledText(context.getSource(), ConfigManager.query(Config::isDebugEnabled))))
				.then(Commands.literal("true")
						.executes(context -> setDebug(context, true)))
				.then(Commands.literal("false")
						.executes(context -> setDebug(context, false)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildStatusNode() {
		return Commands.literal("status")
				.requires(AdminCommand::isOpOrConsole)
				.executes(context -> sendStatus(context.getSource()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDisableNode() {
		return Commands.literal("disable")
				.requires(AdminCommand::isOpOrConsole)
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(ENABLED_SUGGESTER)
						.executes(context -> toggleModule(context, false)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildEnableNode() {
		return Commands.literal("enable")
				.requires(AdminCommand::isOpOrConsole)
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(DISABLED_SUGGESTER)
						.executes(context -> toggleModule(context, true)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHelpNode() {
		return Commands.literal("help")
				.executes(context -> sendHelp(context.getSource(),
						AdminHelpRequest.overview(AdminMessages.language(context.getSource()), ModConstants.VERSION)))
				.then(Commands.literal("admin")
						.executes(context -> sendHelp(context.getSource(),
								AdminHelpRequest.admin(AdminMessages.language(context.getSource()), ModConstants.VERSION))))
				.then(Commands.literal("config")
						.executes(context -> sendHelp(context.getSource(),
								AdminHelpRequest.config(AdminMessages.language(context.getSource()), ModConstants.VERSION)))
						.then(Commands.argument("module", StringArgumentType.word())
								.suggests(CONFIG_MODULE_SUGGESTER)
								.executes(context -> sendHelp(context.getSource(),
										AdminHelpRequest.configModule(
												StringArgumentType.getString(context, "module"),
												AdminMessages.language(context.getSource()), ModConstants.VERSION)))));
	}

	private static int toggleModule(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String moduleName = StringArgumentType.getString(context, "command");
		AdminModuleRegistry.ModuleToggle toggle = AdminModuleRegistry.get(moduleName);

		if (toggle == null) {
			throw new SimpleCommandExceptionType(AdminMessages.t(source,
					"commands.teleport_commands.admin.module.unavailable", Component.literal(moduleName))
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD)).create();
		}

		try {
			AdminModuleRegistry.setEnabled(moduleName, enabled);
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to toggle admin module: {}", moduleName, exception);
			throw new SimpleCommandExceptionType(AdminMessages.t(source,
					"commands.teleport_commands.admin.save.error",
					Component.literal(String.valueOf(exception.getMessage()))).withStyle(ChatFormatting.RED))
							.create();
		}

		AdminMessages.sendSuccess(source, AdminMessages.t(source,
				"commands.teleport_commands.admin.module.status",
				AdminMessages.t(source, toggle.labelKey()),
				AdminMessages.t(source, enabled
						? "commands.teleport_commands.admin.stat.enabled"
						: "commands.teleport_commands.admin.stat.disabled")), true);
		source.sendSuccess(STATUS_RENDERER::renderRefreshDivider, false);
		return sendStatus(source);
	}

	private static int setDebug(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			ConfigManager.mutate(config -> config.setDebugEnabled(enabled));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to update debug logging.", exception);
			throw new SimpleCommandExceptionType(AdminMessages.t(source,
					"commands.teleport_commands.admin.save.error",
					Component.literal(String.valueOf(exception.getMessage()))).withStyle(ChatFormatting.RED))
							.create();
		}

		AdminMessages.sendSuccess(source, AdminMessages.t(source,
				"commands.teleport_commands.admin.debug",
				enabledText(source, enabled)), true);
		return 0;
	}

	private static int sendStatus(CommandSourceStack source) {
		source.sendSuccess(() -> STATUS_RENDERER.render(AdminModuleRegistry.statuses(), AdminMessages.language(source)),
				false);
		return 0;
	}

	private static int sendHelp(CommandSourceStack source, AdminHelpRequest request) {
		source.sendSuccess(() -> HELP_RENDERER.render(request), false);
		return 0;
	}

	private static MutableComponent enabledText(CommandSourceStack source, boolean enabled) {
		return AdminMessages.t(source, enabled
				? "commands.teleport_commands.admin.stat.enabled"
				: "commands.teleport_commands.admin.stat.disabled");
	}

	private static boolean isOpOrConsole(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}
}
