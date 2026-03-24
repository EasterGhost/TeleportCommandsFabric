package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class warp {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildSetNode());
		commandDispatcher.register(buildTeleportNode());
		commandDispatcher.register(buildDeleteNode());
		commandDispatcher.register(buildRenameNode());
		commandDispatcher.register(buildListNode());
		commandDispatcher.register(buildPagePickerNode());
		commandDispatcher.register(buildMapVisibilityNode());
		commandDispatcher.register(buildSilentMapVisibilityNode());
		commandDispatcher.register(buildAdminMapListNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("setwarp")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
				.then(Commands.argument("name", StringArgumentType.string())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(
									player,
									"Error while setting the warp!",
									"commands.teleport_commands.warp.setError",
									() -> WarpMutationActions.setWarp(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("warp")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(
									player,
									"Error while going to the warp!",
									"commands.teleport_commands.warp.goError",
									() -> WarpTeleportActions.goToWarp(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delwarp")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(
									player,
									"Error while deleting the warp!",
									"commands.teleport_commands.warp.deleteError",
									() -> WarpMutationActions.deleteWarp(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamewarp")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> {
									final String name = StringArgumentType.getString(context, "name");
									final String newName = StringArgumentType.getString(context, "newName");
									final ServerPlayer player = context.getSource().getPlayerOrException();

									if (!WarpMessages.ensureEnabled(player)) {
										return 1;
									}

									return WarpMessages.execute(
											player,
											"Error while renaming the warp!",
											"commands.teleport_commands.warp.renameError",
											() -> WarpMutationActions.renameWarp(player, name, newName));
								})));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("warps")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!WarpMessages.ensureEnabled(player)) {
						return 1;
					}

					return WarpMessages.execute(
							player,
							"Error while printing warps!",
							"commands.teleport_commands.warps.error",
							() -> WarpCommandSupport.printWarps(context.getSource(), player));
				})
				.then(Commands.argument("page", IntegerArgumentType.integer())
						.executes(context -> {
							final ServerPlayer player = context.getSource().getPlayerOrException();
							final int page = IntegerArgumentType.getInteger(context, "page");

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(
									player,
									"Error while printing warps!",
									"commands.teleport_commands.warps.error",
									() -> WarpCommandSupport.printWarps(context.getSource(), player, page));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:warpspages")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("page", IntegerArgumentType.integer())
						.executes(context -> {
							final ServerPlayer player = context.getSource().getPlayerOrException();
							final int page = IntegerArgumentType.getInteger(context, "page");

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(
									player,
									"Error while printing the warp page picker!",
									"commands.teleport_commands.warps.error",
									() -> WarpCommandSupport.printWarpPagePicker(player, page));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("mapwarp")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String warpName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return handleMapVisibility(player, warpName, visible);
								})));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:mapwarp")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String warpName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return handleSilentMapVisibility(player, warpName, visible);
								})
								.then(Commands.argument("page", IntegerArgumentType.integer())
										.executes(context -> {
											final ServerPlayer player = context.getSource().getPlayerOrException();
											final String warpName = StringArgumentType.getString(context, "name");
											final boolean visible = BoolArgumentType.getBool(context, "visible");
											final int page = IntegerArgumentType.getInteger(context, "page");

											return handleSilentMapVisibility(player, warpName, visible, page);
										}))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildAdminMapListNode() {
		return Commands.literal("gwarpmap")
				.requires(source -> source.getPlayer() != null
						&& source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!WarpMessages.ensureEnabled(player)) {
						return 1;
					}

					return WarpMessages.execute(
							player,
							"Error while printing warp map visibility list!",
							"commands.teleport_commands.gwarpmap.error",
							() -> WarpCommandSupport.printAdminWarpMap(player));
				})
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String warpName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									if (!WarpMessages.ensureEnabled(player)) {
										return 1;
									}

									return WarpMessages.execute(
											player,
											"Error while updating global warp Xaero visibility!",
											"commands.teleport_commands.gwarpmap.error",
											() -> WarpVisibilityActions.setGlobalVisibility(player, warpName, visible));
								})));
	}

	private static int handleMapVisibility(ServerPlayer player, String warpName, boolean visible) {
		if (!WarpMessages.ensureEnabled(player)) {
			return 1;
		}

		return WarpMessages.execute(
				player,
				"Error while updating warp Xaero visibility!",
				"commands.teleport_commands.warps.error",
				() -> WarpVisibilityActions.setPlayerVisibility(player, warpName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String warpName, boolean visible) {
		if (!org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getWarp().isEnabled()) {
			return 1;
		}

		return WarpMessages.executeSilently(
				"Error while updating warp Xaero visibility!",
				() -> WarpVisibilityActions.setPlayerVisibilitySilently(player, warpName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String warpName, boolean visible, int page) {
		if (!org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getWarp().isEnabled()) {
			return 1;
		}

		return WarpMessages.executeSilently(
				"Error while updating warp Xaero visibility!",
				() -> WarpVisibilityActions.setPlayerVisibilitySilentlyAndShowPage(player, warpName, visible, page));
	}
}
