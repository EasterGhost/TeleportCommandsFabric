package org.AndrewElizabeth.teleportcommandsfabric.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

public final class AdminCommands {

	private static final SuggestionProvider<CommandSourceStack> ENABLED_SUGGESTER =
			(context, builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.enabledNames(), builder);
	private static final SuggestionProvider<CommandSourceStack> DISABLED_SUGGESTER =
			(context, builder) -> SharedSuggestionProvider.suggest(AdminModuleRegistry.disabledNames(), builder);

	private AdminCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("teleportcommands")
				.then(buildConfigNode())
				.then(buildReloadNode())
				.then(buildDisableNode())
				.then(buildEnableNode())
				.then(buildHelpNode()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildConfigNode() {
		return Commands.literal("config")
				.requires(AdminCommands::isOpOrConsole)
				.then(buildTeleportingConfigNode())
				.then(buildBackConfigNode())
				.then(buildHomeConfigNode())
				.then(buildTpaConfigNode())
				.then(buildWarpConfigNode())
				.then(buildWorldSpawnConfigNode())
				.then(buildRtpConfigNode())
				.then(buildXaeroConfigNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportingConfigNode() {
		return Commands.literal("teleporting")
				.then(AdminConfigNodeFactory.intNode(
						"delay",
						"seconds",
						0,
						value -> ConfigManager.CONFIG.getTeleporting().setDelay(value),
						"commands.teleport_commands.admin.config.teleporting.delay"))
				.then(AdminConfigNodeFactory.intNode(
						"cooldown",
						"seconds",
						0,
						value -> ConfigManager.CONFIG.getTeleporting().setCooldown(value),
						"commands.teleport_commands.admin.config.teleporting.cooldown"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackConfigNode() {
		return Commands.literal("back")
				.then(AdminConfigNodeFactory.boolNode(
						"deleteAfterTeleport",
						value -> ConfigManager.CONFIG.getBack().setDeleteAfterTeleport(value),
						"commands.teleport_commands.admin.config.back.deleteAfterTeleport"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHomeConfigNode() {
		return Commands.literal("home")
				.then(AdminConfigNodeFactory.intNode(
						"max",
						"count",
						0,
						value -> ConfigManager.CONFIG.getHome().setPlayerMaximum(value),
						"commands.teleport_commands.admin.config.home.max"))
				.then(AdminConfigNodeFactory.boolNode(
						"deleteInvalid",
						value -> ConfigManager.CONFIG.getHome().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.home.deleteInvalid"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTpaConfigNode() {
		return Commands.literal("tpa")
				.then(AdminConfigNodeFactory.intNode(
						"expireTime",
						"seconds",
						0,
						value -> ConfigManager.CONFIG.getTpa().setRequestExpireTime(value),
						"commands.teleport_commands.admin.config.tpa.expireTime"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWarpConfigNode() {
		return Commands.literal("warp")
				.then(AdminConfigNodeFactory.intNode(
						"max",
						"count",
						0,
						value -> ConfigManager.CONFIG.getWarp().setMaximum(value),
						"commands.teleport_commands.admin.config.warp.max"))
				.then(AdminConfigNodeFactory.boolNode(
						"deleteInvalid",
						value -> ConfigManager.CONFIG.getWarp().setDeleteInvalid(value),
						"commands.teleport_commands.admin.config.warp.deleteInvalid"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnConfigNode() {
		return Commands.literal("worldspawn")
				.then(AdminConfigNodeFactory.stringNode(
						"world",
						"worldId",
						value -> ConfigManager.CONFIG.getWorldSpawn().setWorld_id(value),
						"commands.teleport_commands.admin.config.worldspawn.world"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRtpConfigNode() {
		return Commands.literal("rtp")
				.then(AdminConfigNodeFactory.intNode(
						"radius",
						"blocks",
						1,
						value -> ConfigManager.CONFIG.getRtp().setRadius(value),
						"commands.teleport_commands.admin.config.rtp.radius"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildXaeroConfigNode() {
		return Commands.literal("xaero")
				.then(AdminConfigNodeFactory.intNode(
						"syncIntervalSeconds",
						"seconds",
						0,
						value -> ConfigManager.CONFIG.getXaero().setSyncIntervalSeconds(value),
						"commands.teleport_commands.admin.config.xaero.syncIntervalSeconds"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildReloadNode() {
		return Commands.literal("reload")
				.requires(AdminCommands::isOpOrConsole)
				.executes(context -> {
					try {
						ConfigManager.ConfigLoader();
					} catch (Exception e) {
						Constants.LOGGER.error("Failed to reload config!", e);
						throw new SimpleCommandExceptionType(
								AdminMessages.t(context.getSource(),
										"commands.teleport_commands.admin.reload.error",
										Component.literal(e.toString())))
								.create();
					}
					context.getSource().sendSuccess(
							() -> AdminMessages.t(context.getSource(),
									"commands.teleport_commands.admin.reload.success"),
							true);
					return 0;
				});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDisableNode() {
		return Commands.literal("disable")
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(ENABLED_SUGGESTER)
						.requires(AdminCommands::isOpOrConsole)
						.executes(context -> toggleModule(context, false)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildEnableNode() {
		return Commands.literal("enable")
				.then(Commands.argument("command", StringArgumentType.word())
						.suggests(DISABLED_SUGGESTER)
						.requires(AdminCommands::isOpOrConsole)
						.executes(context -> toggleModule(context, true)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildHelpNode() {
		return Commands.literal("help")
				.executes(context -> {
					context.getSource().sendSuccess(
							() -> AdminHelpFormatter.build(context.getSource()),
							false);
					return 0;
				});
	}

	private static int toggleModule(
			com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String moduleName = StringArgumentType.getString(context, "command");
		AdminModuleRegistry.ModuleToggle toggle = AdminModuleRegistry.get(moduleName);

		if (toggle == null) {
			throw new SimpleCommandExceptionType(
					AdminMessages.t(context.getSource(),
							"commands.teleport_commands.admin.module.unavailable",
							Component.literal(moduleName))
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
					.create();
		}

		return AdminMessages.setAndSave(
				context,
				() -> toggle.setter().accept(enabled),
				AdminMessages.t(context.getSource(),
						"commands.teleport_commands.admin.module.status",
						AdminMessages.t(context.getSource(), toggle.labelKey()),
						AdminMessages.t(context.getSource(),
								enabled
										? "commands.teleport_commands.admin.state.enabled"
										: "commands.teleport_commands.admin.state.disabled")));
	}

	private static boolean isOpOrConsole(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}
}
