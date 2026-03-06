package org.AndrewElizabeth.teleportcommandsfabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.permissions.Permissions;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class admin {

	private static final Map<String, ModuleToggle> MODULE_TOGGLES = new LinkedHashMap<>();

	static {
		MODULE_TOGGLES.put("back", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getBack().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getBack().isEnabled(),
				"Back command"));
		MODULE_TOGGLES.put("home", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getHome().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getHome().isEnabled(),
				"Home command"));
		MODULE_TOGGLES.put("tpa", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getTpa().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getTpa().isEnabled(),
				"TPA command"));
		MODULE_TOGGLES.put("warp", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getWarp().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getWarp().isEnabled(),
				"Warp command"));
		MODULE_TOGGLES.put("worldspawn", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getWorldSpawn().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getWorldSpawn().isEnabled(),
				"WorldSpawn command"));
		MODULE_TOGGLES.put("rtp", new ModuleToggle(
				enabled -> ConfigManager.CONFIG.getWild().setEnabled(enabled),
				() -> ConfigManager.CONFIG.getWild().isEnabled(),
				"RTP command"));
	}

	private static final SuggestionProvider<CommandSourceStack> disabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(getModuleNamesByState(false), builder);
	private static final SuggestionProvider<CommandSourceStack> enabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(getModuleNamesByState(true), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("teleportcommands")
				.then(Commands.literal("config")
						.requires(admin::isOpOrConsole)
						// --- Teleporting Config ---
						.then(Commands.literal("teleporting")
								.then(Commands.literal("delay")
										.then(Commands.argument("seconds",
												IntegerArgumentType
														.integer(0))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getTeleporting()
																.setDelay(IntegerArgumentType
																		.getInteger(context,
																				"seconds")),
														"Teleport delay set to "
																+ IntegerArgumentType
																		.getInteger(context,
																				"seconds")
																+ "s"))))
								.then(Commands.literal("cooldown")
										.then(Commands.argument("seconds",
												IntegerArgumentType
														.integer(0))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getTeleporting()
																.setCooldown(IntegerArgumentType
																		.getInteger(context,
																				"seconds")),
														"Teleport cooldown set to "
																+ IntegerArgumentType
																		.getInteger(context,
																				"seconds")
																+ "s")))))
						// --- Back Config ---
						.then(Commands.literal("back")
								.then(Commands.literal("deleteAfterTeleport")
										.then(Commands.literal("true").executes(
												context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getBack()
																.setDeleteAfterTeleport(
																		true),
														"Delete last location after teleport: enabled")))
										.then(Commands.literal("false")
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getBack()
																.setDeleteAfterTeleport(
																		false),
														"Delete last location after teleport: disabled")))))
						// --- Home Config ---
						.then(Commands.literal("home")
								.then(Commands.literal("max")
										.then(Commands.argument("count",
												IntegerArgumentType
														.integer(0))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getHome()
																.setPlayerMaximum(
																		IntegerArgumentType
																				.getInteger(context,
																						"count")),
														"Maximum homes per player set to "
																+ IntegerArgumentType
																		.getInteger(context,
																				"count")))))
								.then(Commands.literal("deleteInvalid")
										.then(Commands.literal("true").executes(
												context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getHome()
																.setDeleteInvalid(
																		true),
														"Auto-delete invalid homes: enabled")))
										.then(Commands.literal("false")
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getHome()
																.setDeleteInvalid(
																		false),
														"Auto-delete invalid homes: disabled")))))
						// --- TPA Config ---
						.then(Commands.literal("tpa")
								.then(Commands.literal("expireTime")
										.then(Commands.argument("seconds",
												IntegerArgumentType
														.integer(0))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getTpa()
																.setRequestExpireTime(
																		IntegerArgumentType
																				.getInteger(context,
																						"seconds")),
														"TPA request expiration time set to "
																+ IntegerArgumentType
																		.getInteger(context,
																				"seconds")
																+ "s")))))
						// --- Warp Config ---
						.then(Commands.literal("warp")
								.then(Commands.literal("max")
										.then(Commands.argument("count",
												IntegerArgumentType
														.integer(0))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWarp()
																.setMaximum(IntegerArgumentType
																		.getInteger(context,
																				"count")),
														"Maximum warps set to "
																+ IntegerArgumentType
																		.getInteger(context,
																				"count")))))
								.then(Commands.literal("deleteInvalid")
										.then(Commands.literal("true").executes(
												context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWarp()
																.setDeleteInvalid(
																		true),
														"Auto-delete invalid warps: enabled")))
										.then(Commands.literal("false")
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWarp()
																.setDeleteInvalid(
																		false),
														"Auto-delete invalid warps: disabled")))))
						// --- WorldSpawn Config ---
						.then(Commands.literal("worldspawn")
								.then(Commands.literal("world")
										.then(Commands.argument("worldId", StringArgumentType.string())
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWorldSpawn()
																.setWorld_id(StringArgumentType
																		.getString(context, "worldId")),
														"WorldSpawn world set to "
																+ StringArgumentType
																		.getString(context, "worldId"))))))
						.then(Commands.literal("rtp")
								.then(Commands.literal("radius")
										.then(Commands.argument("blocks", IntegerArgumentType.integer(1))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWild()
																.setRadius(IntegerArgumentType
																		.getInteger(context, "blocks")),
														"RTP radius set to "
																+ IntegerArgumentType
																		.getInteger(context, "blocks")))))))
				.then(Commands.literal("reload")
						.requires(admin::isOpOrConsole)
						.executes(context -> {
							try {
								ConfigManager.ConfigLoader();
							} catch (Exception e) {
								Constants.LOGGER.error("Failed to reload config!", e);
								throw new SimpleCommandExceptionType(
										Component.literal(e.toString()))
										.create();
							}
							context.getSource()
									.sendSuccess(() -> Component.literal(
											"Config reloaded successfully"),
											true);
							return 0;
						}))
				.then(Commands.literal("disable")
						.then(Commands.argument("command", StringArgumentType.word())
								.suggests(enabled_commands_suggester)
								.requires(admin::isOpOrConsole)
								.executes(context -> toggleModule(context, false))))
				.then(Commands.literal("enable")
						.then(Commands.argument("command", StringArgumentType.word())
								.suggests(disabled_commands_suggester)
								.requires(admin::isOpOrConsole)
								.executes(context -> toggleModule(context, true))))
				.then(Commands.literal("help")
						.executes(context -> {
							context.getSource().sendSuccess(admin::printCommands, false);
							return 0;
						})));
	}

	// -----

	/// Utility method to set config and save with error handling
	private static int setAndSave(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			Runnable setter, String message) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		try {
			setter.run();
			ConfigManager.saveConfigChanges();
			context.getSource().sendSuccess(
					() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to save config!", e);
			throw new SimpleCommandExceptionType(
					Component.literal("Failed to save config: " + e.getMessage())
							.withStyle(ChatFormatting.RED))
					.create();
		}
	}

	private static int toggleModule(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
			boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		final String moduleName = StringArgumentType.getString(context, "command");
		final ModuleToggle module = MODULE_TOGGLES.get(moduleName);

		if (module == null) {
			throw new SimpleCommandExceptionType(
					Component.literal("\"" + moduleName + "\" is not available as a command!")
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
					.create();
		}

		try {
			return setAndSave(
					context,
					() -> module.setter().accept(enabled),
					module.label() + ": " + (enabled ? "enabled" : "disabled"));
		} catch (Exception e) {
			Constants.LOGGER.error(
					"Error while {} a command! => ",
					enabled ? "enabling" : "disabling",
					e);
			throw new SimpleCommandExceptionType(
					Component.literal("Error: " + e.getMessage())
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

	private static MutableComponent printCommands() {
		MutableComponent message = Component.empty();

		message.append(Component.literal("Thank you for using Teleport Commands (V" + Constants.VERSION + ")!")
				.withStyle(ChatFormatting.AQUA));
		message.append(Component.literal(
				"Teleport Commands is a server-side mod that adds various teleportation related commands")
				.withStyle(ChatFormatting.AQUA));

		message.append(Component.literal("----").withStyle(ChatFormatting.AQUA));
		message.append(Component.literal("Usage:").withStyle(ChatFormatting.AQUA));

		return message;
	}

	private static boolean isOpOrConsole(CommandSourceStack source) {
		// Use command source permission level (console usually level 4)
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	private record ModuleToggle(Consumer<Boolean> setter, java.util.function.BooleanSupplier isEnabled, String label) {
	}
}
