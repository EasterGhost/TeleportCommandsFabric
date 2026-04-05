package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigClass;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public final class AdminCommand {
	private static final String PRIMARY_COMMAND = "tpc";

	private static final SuggestionProvider<CommandSourceStack> ENABLED_SUGGESTER = (context,
			builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.enabledNames(), builder);
	private static final SuggestionProvider<CommandSourceStack> DISABLED_SUGGESTER = (context,
			builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.disabledNames(), builder);

	private AdminCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildRootCommand(PRIMARY_COMMAND));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRootCommand(String literal) {
		return Commands.literal(literal)
				.then(buildConfigNode())
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

	private static LiteralArgumentBuilder<CommandSourceStack> buildStorageConfigNode() {
		return Commands.literal("storage")
				.then(AdminConfigNodeFactory.intNode("autoSaveIntervalSeconds", "seconds",
						ConfigClass.Storage.MIN_AUTO_SAVE_INTERVAL,
						() -> CONFIG.storage.getAutoSaveIntervalSeconds(),
						value -> CONFIG.storage.setAutoSaveIntervalSeconds(value),
						"commands.teleport_commands.admin.config.storage.autoSaveIntervalSeconds"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportingConfigNode() {
		return Commands.literal("teleporting")
				.then(AdminConfigNodeFactory.intNode("delay", "seconds", 0,
						() -> CONFIG.getTeleporting().getDelay(),
						value -> CONFIG.getTeleporting().setDelay(value),
						"commands.teleport_commands.admin.config.teleporting.delay"))
				.then(AdminConfigNodeFactory.intNode("cooldown", "seconds", 0,
						() -> CONFIG.getTeleporting().getCooldown(),
						value -> CONFIG.getTeleporting().setCooldown(value),
						"commands.teleport_commands.admin.config.teleporting.cooldown"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackConfigNode() {
		return Commands.literal("back")
				.then(AdminConfigNodeFactory.boolNode("deleteAfterTeleport",
						() -> CONFIG.getBack().isDeleteAfterTeleport(),
						value -> CONFIG.getBack().setDeleteAfterTeleport(value),
						"commands.teleport_commands.admin.config.back.deleteAfterTeleport"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHomeConfigNode() {
		return Commands.literal("home")
				.then(AdminConfigNodeFactory.intNode("max", "count", 0,
						() -> CONFIG.getHome().getPlayerMaximum(),
						value -> CONFIG.getHome().setPlayerMaximum(value),
						"commands.teleport_commands.admin.config.home.max"))
				.then(AdminConfigNodeFactory.boolNode("deleteInvalid",
						() -> CONFIG.getHome().isDeleteInvalid(),
						value -> CONFIG.getHome().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.home.deleteInvalid"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTpaConfigNode() {
		return Commands.literal("tpa")
				.then(AdminConfigNodeFactory.intNode("expireTime", "seconds", 0,
						() -> CONFIG.getTpa().getRequestExpireTime(),
						value -> CONFIG.getTpa().setRequestExpireTime(value),
						"commands.teleport_commands.admin.config.tpa.expireTime"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWarpConfigNode() {
		return Commands.literal("warp")
				.then(AdminConfigNodeFactory.intNode("max", "count", 0,
						() -> CONFIG.getWarp().getMaximum(),
						value -> CONFIG.getWarp().setMaximum(value),
						"commands.teleport_commands.admin.config.warp.max"))
				.then(AdminConfigNodeFactory.boolNode("deleteInvalid",
						() -> CONFIG.getWarp().isDeleteInvalid(),
						value -> CONFIG.getWarp().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.warp.deleteInvalid"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnConfigNode() {
		return Commands.literal("worldspawn")
				.then(AdminConfigNodeFactory.stringNode("world", "worldId",
						() -> CONFIG.getWorldSpawn().getWorld_id(),
						value -> CONFIG.getWorldSpawn().setWorld_id(value),
						"commands.teleport_commands.admin.config.worldspawn.world"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRtpConfigNode() {
		return Commands.literal("rtp")
				.then(AdminConfigNodeFactory.intNode("radius", "blocks", ConfigClass.Rtp.MIN_RADIUS,
						ConfigClass.Rtp.MAX_RADIUS,
						() -> CONFIG.getRtp().getRadius(),
						value -> CONFIG.getRtp().setRadius(value),
						"commands.teleport_commands.admin.config.rtp.radius"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildXaeroConfigNode() {
		return Commands.literal("xaero")
				.then(AdminConfigNodeFactory.intNode("syncIntervalSeconds", "seconds", 0,
						() -> CONFIG.getXaero().getSyncIntervalSeconds(),
						value -> CONFIG.getXaero().setSyncIntervalSeconds(value),
						"commands.teleport_commands.admin.config.xaero.syncIntervalSeconds"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildReloadNode() {
		return Commands.literal("reload")
				.requires(AdminCommand::isOpOrConsole)
				.executes(context -> {
					try {
						ConfigManager.ConfigLoader();
					} catch (Exception e) {
						ModConstants.LOGGER.error("Failed to reload config!", e);
						throw new SimpleCommandExceptionType(AdminMessages.t(context.getSource(),
								"commands.teleport_commands.admin.reload.error", Component.literal(e.toString())))
										.create();
					}
					context.getSource().sendSuccess(() -> AdminMessages.t(context.getSource(),
							"commands.teleport_commands.admin.reload.success"), true);
					return 0;
				});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildStatusNode() {
		return Commands.literal("status")
				.requires(AdminCommand::isOpOrConsole)
				.executes(context -> {
					context.getSource().sendSuccess(() -> AdminStatusFormatter.build(context.getSource()), false);
					return 0;
				});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDisableNode() {
		return Commands.literal("disable")
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(ENABLED_SUGGESTER)
						.requires(AdminCommand::isOpOrConsole)
						.executes(context -> toggleModule(context, false)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildEnableNode() {
		return Commands.literal("enable")
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(DISABLED_SUGGESTER)
						.requires(AdminCommand::isOpOrConsole)
						.executes(context -> toggleModule(context, true)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHelpNode() {
		return Commands.literal("help")
				.executes(context -> {
					context.getSource().sendSuccess(() -> AdminHelpFormatter.build(context.getSource()), false);
					return 0;
				});
	}

	private static int toggleModule(CommandContext<CommandSourceStack> context,
			boolean enabled) throws CommandSyntaxException {
		String moduleName = StringArgumentType.getString(context, "command");
		AdminModuleRegistry.ModuleToggle toggle = AdminModuleRegistry.get(moduleName);

		if (toggle == null) {
			throw new SimpleCommandExceptionType(AdminMessages.t(context.getSource(),
					"commands.teleport_commands.admin.module.unavailable", Component.literal(moduleName))
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
							.create();
		}

		int result = AdminMessages.setAndSave(context, () -> toggle.setter().accept(enabled),
				AdminMessages.t(context.getSource(), "commands.teleport_commands.admin.module.status",
						AdminMessages.t(context.getSource(), toggle.labelKey()),
						AdminMessages.t(context.getSource(), enabled
								? "commands.teleport_commands.admin.stat.enabled"
								: "commands.teleport_commands.admin.stat.disabled")));

		if (result == 0) {
			context.getSource().sendSuccess(AdminStatusFormatter::refreshDivider, false);
			context.getSource().sendSuccess(() -> AdminStatusFormatter.build(context.getSource()), false);
		}
		return result;
	}

	private static boolean isOpOrConsole(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}
}
