package org.AndrewElizabeth.teleportcommandsfabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.lang.Deprecated;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.getTranslatedText;

@Deprecated(since = "1.3", forRemoval = true)
public class Admin {

	private static final Map<String, ModuleToggle> MODULE_TOGGLES = new LinkedHashMap<>();

	static {
		MODULE_TOGGLES.put("back", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getBack().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getBack().isEnabled(),
				"commands.teleport_commands.admin.module.back"));
		MODULE_TOGGLES.put("home", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getHome().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getHome().isEnabled(),
				"commands.teleport_commands.admin.module.home"));
		MODULE_TOGGLES.put("tpa", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getTpa().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getTpa().isEnabled(),
				"commands.teleport_commands.admin.module.tpa"));
		MODULE_TOGGLES.put("warp", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getWarp().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getWarp().isEnabled(),
				"commands.teleport_commands.admin.module.warp"));
		MODULE_TOGGLES.put("worldspawn", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getWorldSpawn().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getWorldSpawn().isEnabled(),
				"commands.teleport_commands.admin.module.worldspawn"));
		MODULE_TOGGLES.put("rtp", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getRtp().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getRtp().isEnabled(),
				"commands.teleport_commands.admin.module.rtp"));
		MODULE_TOGGLES.put("xaero", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getXaero().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getXaero().isEnabled(),
				"commands.teleport_commands.admin.module.xaero"));
	}

	private static final SuggestionProvider<CommandSourceStack> disabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(getModuleNamesByState(false), builder);
	private static final SuggestionProvider<CommandSourceStack> enabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(getModuleNamesByState(true), builder);
		@Deprecated(since = "1.3", forRemoval = true)
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("teleportcommands")
						// Admin subcommand config
						.then(Commands.literal("config")
								.requires(Admin::isOpOrConsole)
								// Config teleporting delay / cooldown
								.then(Commands.literal("teleporting")
										.then(Commands.literal("delay")
												.then(Commands.argument("seconds", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getTeleporting()
																		.setDelay(IntegerArgumentType
																				.getInteger(context, "seconds")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.teleporting.delay",
																		argInt(context, "seconds"))))))
										.then(Commands.literal("cooldown")
												.then(Commands
														.argument("seconds", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getTeleporting()
																		.setCooldown(
																				IntegerArgumentType.getInteger(
																						context, "seconds")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.teleporting.cooldown",
																		argInt(context, "seconds")))))))
								// Config back deleteAfterTeleport (true/false)
								.then(Commands.literal("back")
										.then(Commands.literal("deleteAfterTeleport")
												.then(Commands.literal("true")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getBack()
																		.setDeleteAfterTeleport(true),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.back.deleteAfterTeleport",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.enabled")))))
												.then(Commands.literal("false")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getBack()
																		.setDeleteAfterTeleport(false),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.back.deleteAfterTeleport",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.disabled")))))))
								// Config home max / deleteInvalid (true/false)
								.then(Commands.literal("home")
										.then(Commands.literal("max")
												.then(Commands.argument("count", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getHome()
																		.setPlayerMaximum(IntegerArgumentType
																				.getInteger(context, "count")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.home.max",
																		argInt(context, "count"))))))
										.then(Commands.literal("deleteInvalid")
												.then(Commands.literal("true")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getHome()
																		.setDeleteInvalid(true),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.home.deleteInvalid",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.enabled")))))
												.then(Commands.literal("false")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getHome()
																		.setDeleteInvalid(false),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.home.deleteInvalid",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.disabled")))))))
								// Config tpa expireTime
								.then(Commands.literal("tpa")
										.then(Commands.literal("expireTime")
												.then(Commands
														.argument("seconds", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getTpa()
																		.setRequestExpireTime(
																				IntegerArgumentType.getInteger(
																						context, "seconds")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.tpa.expireTime",
																		argInt(context, "seconds")))))))
								// Config warp max / deleteInvalid (true/false)
								.then(Commands.literal("warp")
										.then(Commands.literal("max")
												.then(Commands.argument("count", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getWarp()
																		.setMaximum(IntegerArgumentType
																				.getInteger(context, "count")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.warp.max",
																		argInt(context, "count"))))))
										.then(Commands.literal("deleteInvalid")
												.then(Commands.literal("true")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getWarp()
																		.setDeleteInvalid(true),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.warp.deleteInvalid",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.enabled")))))
												.then(Commands.literal("false")
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getWarp()
																		.setDeleteInvalid(false),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.warp.deleteInvalid",
																		translate(context.getSource(),
																				"commands.teleport_commands.admin.stat.disabled")))))))
								// Config worldspawn world
								.then(Commands.literal("worldspawn")
										.then(Commands.literal("world")
												.then(Commands.argument("worldId", StringArgumentType.string())
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getWorldSpawn()
																		.setWorld_id(StringArgumentType
																				.getString(context, "worldId")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.worldspawn.world",
																		Component.literal(StringArgumentType
																				.getString(context,
																						"worldId"))))))))
								// Config rtp radius
								.then(Commands.literal("rtp")
										.then(Commands.literal("radius")
												.then(Commands
														.argument("blocks", IntegerArgumentType.integer(1))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getRtp()
																		.setRadius(IntegerArgumentType
																				.getInteger(context, "blocks")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.rtp.radius",
																		argInt(context, "blocks")))))))
								// Config xaero syncIntervalSeconds
								.then(Commands.literal("xaero")
										.then(Commands.literal("syncIntervalSeconds")
												.then(Commands
														.argument("seconds", IntegerArgumentType.integer(0))
														.executes(context -> setAndSave(
																context,
																() -> ConfigManager.CONFIG.getXaero()
																		.setSyncIntervalSeconds(IntegerArgumentType
																				.getInteger(context, "seconds")),
																translate(context.getSource(),
																		"commands.teleport_commands.admin.config.xaero.syncIntervalSeconds",
																		argInt(context, "seconds"))))))))
					// Admin subcommands reload / enable / disable / help
						.then(Commands.literal("reload")
								.requires(Admin::isOpOrConsole)
								.executes(context -> {
									try {
										ConfigManager.ConfigLoader();
									} catch (Exception e) {
										Constants.LOGGER.error("Failed to reload config!", e);
										throw new SimpleCommandExceptionType(
												translate(context.getSource(),
														"commands.teleport_commands.admin.reload.error",
														Component.literal(e.toString())))
												.create();
									}
									context.getSource().sendSuccess(
											() -> translate(context.getSource(),
													"commands.teleport_commands.admin.reload.success"),
											true);
									return 0;
								}))
						.then(Commands.literal("disable")
								.then(Commands.argument("command", StringArgumentType.word())
										.suggests(enabled_commands_suggester)
										.requires(Admin::isOpOrConsole)
										.executes(context -> toggleModule(context, false))))
						.then(Commands.literal("enable")
								.then(Commands.argument("command", StringArgumentType.word())
										.suggests(disabled_commands_suggester)
										.requires(Admin::isOpOrConsole)
										.executes(context -> toggleModule(context, true))))
						.then(Commands.literal("help")
								.executes(context -> {
									context.getSource().sendSuccess(() -> printCommands(context.getSource()), false);
									return 0;
								})));
	}

	private static int setAndSave(CommandContext<CommandSourceStack> context, Runnable setter,
			MutableComponent message) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		try {
			setter.run();
			ConfigManager.saveConfigChanges();
			context.getSource().sendSuccess(() -> message.copy().withStyle(ChatFormatting.GREEN), true);
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to save config!", e);
			throw new SimpleCommandExceptionType(
					translate(context.getSource(),
							"commands.teleport_commands.admin.save.error",
							Component.literal(e.getMessage()))
							.withStyle(ChatFormatting.RED))
					.create();
		}
	}

	private static int toggleModule(CommandContext<CommandSourceStack> context, boolean enabled)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		final String moduleName = StringArgumentType.getString(context, "command");
		final ModuleToggle module = MODULE_TOGGLES.get(moduleName);

		if (module == null) {
			throw new SimpleCommandExceptionType(
					translate(context.getSource(),
							"commands.teleport_commands.admin.module.unavailable",
							Component.literal(moduleName))
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
					.create();
		}

		try {
			return setAndSave(
					context,
					() -> module.setter().accept(enabled),
					translate(context.getSource(),
							"commands.teleport_commands.admin.module.status",
							translate(context.getSource(), module.labelKey()),
							translate(context.getSource(), enabled
									? "commands.teleport_commands.admin.stat.enabled"
									: "commands.teleport_commands.admin.stat.disabled")));
		} catch (Exception e) {
			Constants.LOGGER.error("Error while {} a command! => ", enabled ? "enabling" : "disabling", e);
			throw new SimpleCommandExceptionType(
					translate(context.getSource(),
							"commands.teleport_commands.admin.module.error",
							Component.literal(e.getMessage()))
							.withStyle(ChatFormatting.RED))
					.create();
		}
	}

	private static Iterable<String> getModuleNamesByState(boolean enabled) {
		return MODULE_TOGGLES.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isEnabled().getAsBoolean() == enabled)
				.map(Map.Entry::getKey)
				.toList();
	}

	private static MutableComponent printCommands(CommandSourceStack source) {
		MutableComponent message = Component.empty();

		appendHelpLine(message,
				translate(source, "commands.teleport_commands.admin.help.title",
						Component.literal(Constants.VERSION)),
				ChatFormatting.AQUA, true);
		appendHelpLine(message,
				translate(source, "commands.teleport_commands.admin.help.section.admin"),
				ChatFormatting.GOLD, true);
		appendHelpLine(message, Component.literal("/teleportcommands help"), ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands reload"), ChatFormatting.YELLOW, false);
		appendHelpLine(message,
				Component.literal("/teleportcommands enable <back|home|tpa|warp|worldspawn|rtp|xaero>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message,
				Component.literal("/teleportcommands disable <back|home|tpa|warp|worldspawn|rtp|xaero>"),
				ChatFormatting.YELLOW, false);

		appendHelpLine(message, Component.literal(""), ChatFormatting.WHITE, false);
		appendHelpLine(message,
				translate(source, "commands.teleport_commands.admin.help.section.config"),
				ChatFormatting.GOLD, true);
		appendHelpLine(message, Component.literal("/teleportcommands config teleporting delay <seconds>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config teleporting cooldown <seconds>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config back deleteAfterTeleport <true|false>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config home max <count>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config home deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config tpa expireTime <seconds>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config warp max <count>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config warp deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config worldspawn world <worldId>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config rtp radius <blocks>"),
				ChatFormatting.YELLOW, false);
		appendHelpLine(message, Component.literal("/teleportcommands config xaero syncIntervalSeconds <seconds>"),
				ChatFormatting.YELLOW, false);

		return message;
	}

	private static void appendHelpLine(MutableComponent message, MutableComponent text, ChatFormatting color,
			boolean bold) {
		MutableComponent line = text.copy().append("\n");
		message.append(bold ? line.copy().withStyle(color, ChatFormatting.BOLD) : line.copy().withStyle(color));
	}

	private static MutableComponent translate(CommandSourceStack source, String key, MutableComponent... args) {
		ServerPlayer player = source.getPlayer();
		return player != null ? getTranslatedText(key, player, args) : getTranslatedText(key, "en_us", args);
	}

	private static MutableComponent argInt(CommandContext<CommandSourceStack> context, String argumentName) {
		return Component.literal(String.valueOf(IntegerArgumentType.getInteger(context, argumentName)));
	}

	private static boolean isOpOrConsole(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	private record ModuleToggle(Consumer<Boolean> setter, BooleanSupplier isEnabled, String labelKey) {
	}
}
