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

import java.util.Arrays;

public class main {

	private static final String[] available_commands = {
			"back",
			"home",
			"tpa",
			"warp",
			"worldspawn",
			"wild"
	};

	// sum lists
	private static final String[] enabled_commands = available_commands;
	private static final String[] disabled_commands = available_commands;

	// Create sum suggestion providers
	private static final SuggestionProvider<CommandSourceStack> disabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(disabled_commands, builder);
	private static final SuggestionProvider<CommandSourceStack> enabled_commands_suggester = (context,
			builder) -> SharedSuggestionProvider.suggest(enabled_commands, builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("teleportcommands")
				.then(Commands.literal("config")
						.requires(main::isOpOrConsole)
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
						.then(Commands.literal("wild")
								.then(Commands.literal("radius")
										.then(Commands.argument("blocks", IntegerArgumentType.integer(1))
												.executes(context -> setAndSave(
														context,
														() -> ConfigManager.CONFIG
																.getWild()
																.setRadius(IntegerArgumentType
																		.getInteger(context, "blocks")),
														"Wild radius set to "
																+ IntegerArgumentType
																		.getInteger(context, "blocks"))))))
						.then(Commands.literal("reload")
								.requires(main::isOpOrConsole)
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
										.requires(main::isOpOrConsole)
										.executes(context -> {
											final String string = StringArgumentType
													.getString(context, "command");

											if (!Arrays.asList(enabled_commands)
													.contains(string)) {
												throw new SimpleCommandExceptionType(
														Component.literal("\""
																+ string
																+ "\" is not available as a command!")
																.withStyle(ChatFormatting.RED,
																		ChatFormatting.BOLD))
														.create();
											}

											try {
												return switch (string) {
													case "back" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getBack()
																	.setEnabled(false),
															"Back command: disabled");
													case "home" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getHome()
																	.setEnabled(false),
															"Home command: disabled");
													case "tpa" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getTpa()
																	.setEnabled(false),
															"TPA command: disabled");
													case "warp" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWarp()
																	.setEnabled(false),
															"Warp command: disabled");
													case "worldspawn" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWorldSpawn()
																	.setEnabled(false),
															"WorldSpawn command: disabled");
													case "wild" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWild()
																	.setEnabled(false),
															"Wild command: disabled");
													default ->
														throw new SimpleCommandExceptionType(
																Component.literal(
																		"Unknown command: "
																				+ string))
																.create();
												};
											} catch (Exception e) {
												Constants.LOGGER.error(
														"Error while disabling a command! => ",
														e);
												throw new SimpleCommandExceptionType(
														Component.literal(
																"Error: " + e.getMessage())
																.withStyle(ChatFormatting.RED))
														.create();
											}
										})))
						.then(Commands.literal("enable")
								.then(Commands.argument("command", StringArgumentType.word())
										.suggests(disabled_commands_suggester)
										.requires(main::isOpOrConsole)
										.executes(context -> {
											final String string = StringArgumentType
													.getString(context, "command");

											if (!Arrays.asList(disabled_commands)
													.contains(string)) {
												throw new SimpleCommandExceptionType(
														Component.literal("\""
																+ string
																+ "\" is not available as a command!")
																.withStyle(ChatFormatting.RED,
																		ChatFormatting.BOLD))
														.create();
											}

											try {
												return switch (string) {
													case "back" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getBack()
																	.setEnabled(true),
															"Back command: enabled");
													case "home" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getHome()
																	.setEnabled(true),
															"Home command: enabled");
													case "tpa" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getTpa()
																	.setEnabled(true),
															"TPA command: enabled");
													case "warp" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWarp()
																	.setEnabled(true),
															"Warp command: enabled");
													case "worldspawn" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWorldSpawn()
																	.setEnabled(true),
															"WorldSpawn command: enabled");
													case "wild" -> setAndSave(
															context,
															() -> ConfigManager.CONFIG
																	.getWild()
																	.setEnabled(true),
															"Wild command: enabled");
													default ->
														throw new SimpleCommandExceptionType(
																Component.literal(
																		"Unknown command: "
																				+ string))
																.create();
												};
											} catch (Exception e) {
												Constants.LOGGER.error(
														"Error while enabling a command! => ",
														e);
												throw new SimpleCommandExceptionType(
														Component.literal(
																"Error: " + e.getMessage())
																.withStyle(ChatFormatting.RED))
														.create();
											}
										})))
						.then(Commands.literal("help")
								.executes(context -> {
									context.getSource().sendSuccess(main::printCommands, false);
									return 0;
								}))));
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
}
