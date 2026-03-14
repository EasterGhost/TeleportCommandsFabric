package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver;
import org.AndrewElizabeth.teleportcommandsfabric.services.NamedLocationTeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

public class warp {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildSetNode());
		commandDispatcher.register(buildTeleportNode());
		commandDispatcher.register(buildDeleteNode());
		commandDispatcher.register(buildRenameNode());
		commandDispatcher.register(buildListNode());
		commandDispatcher.register(buildMapVisibilityNode());
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
									() -> setWarp(player, name));
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
									() -> goToWarp(player, name));
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
									() -> deleteWarp(player, name));
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
											() -> renameWarp(player, name, newName));
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
							() -> printWarps(context.getSource(), player));
				});
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

									if (!WarpMessages.ensureEnabled(player)) {
										return 1;
									}

									return WarpMessages.execute(
											player,
											"Error while updating warp Xaero visibility!",
											"commands.teleport_commands.warps.error",
											() -> setWarpXaeroVisibility(player, warpName, visible));
								})));
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
							() -> printAdminWarpMap(player));
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
											() -> setGlobalWarpXaeroVisibility(player, warpName, visible));
								})));
	}

	private static void setWarp(ServerPlayer player, String warpName) throws Exception {
		warpName = LocationResolver.normalizeName(warpName);
		String worldString = WorldResolver.getDimensionId(player.level().dimension());

		NamedLocation warp = NamedLocation.create(
				warpName,
				player.getBlockX(),
				player.getY(),
				player.getBlockZ(),
				worldString);

		int maxWarps = org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.warp.getMaximum();
		boolean warpAlreadyExists = LocationResolver.resolveWarp(warpName).isPresent();
		if (!warpAlreadyExists && maxWarps > 0 && STORAGE.getWarps().size() >= maxWarps) {
			WarpMessages.sendMaxReached(player, maxWarps);
			return;
		}

		boolean warpExists = STORAGE.addWarp(warp);
		if (warpExists) {
			WarpMessages.send(player, "commands.teleport_commands.warp.exists", ChatFormatting.RED);
		} else {
			WarpMessages.send(player, "commands.teleport_commands.warp.set");
		}
	}

	private static void goToWarp(ServerPlayer player, String warpName) throws Exception {
		Optional<NamedLocation> optionalWarp = resolveWarpForCommand(warpName, player);
		if (optionalWarp.isEmpty()) {
			return;
		}

		goToWarp(player, optionalWarp.get());
	}

	private static void goToWarp(ServerPlayer player, NamedLocation warp) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(warp);

		if (optionalWorld.isEmpty()) {
			Constants.LOGGER.warn(
					"({}) Error while going to the warp \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					warp.getName(),
					warp.getWorldString());

			WarpMessages.sendWorldNotFound(player);

			if (org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getWarp().isDeleteInvalid()) {
				STORAGE.removeWarp(warp);
				Constants.LOGGER.info("Deleted invalid warp '{}'", warp.getName());
				WarpMessages.sendDeletedInvalid(player);
			}
			return;
		}

		ServerLevel warpWorld = optionalWorld.get();
		if (NamedLocationTeleportService.isAlreadyAtDestination(player, warpWorld, warp)) {
			WarpMessages.send(player, "commands.teleport_commands.warp.goSame", ChatFormatting.AQUA);
		} else {
			if (NamedLocationTeleportService.teleportToNamedLocation(player, warpWorld, warp)) {
				WarpMessages.send(player, "commands.teleport_commands.warp.go");
			}
		}
	}

	private static void deleteWarp(ServerPlayer player, String warpName) throws Exception {
		Optional<NamedLocation> optionalWarp = resolveWarpForCommand(warpName, player);
		if (optionalWarp.isPresent()) {
			STORAGE.removeWarp(optionalWarp.get());
			WarpMessages.send(player, "commands.teleport_commands.warp.delete");
		}
	}

	private static void renameWarp(ServerPlayer player, String warpName, String newWarpName) throws Exception {
		newWarpName = LocationResolver.normalizeName(newWarpName);

		if (LocationResolver.resolveWarp(newWarpName).isPresent()) {
			WarpMessages.sendNameExists(player);
			return;
		}

		Optional<NamedLocation> warpToRename = resolveWarpForCommand(warpName, player);
		if (warpToRename.isPresent()) {
			warpToRename.get().setName(newWarpName);
			WarpMessages.send(player, "commands.teleport_commands.warp.rename");
		}
	}

	private static void printWarps(CommandSourceStack source, ServerPlayer player) throws Exception {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(WarpFormatter.buildWarpList(source, player, warps), false);
	}

	private static void printAdminWarpMap(ServerPlayer player) {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(WarpAdminFormatter.buildWarpMapList(player, warps), false);
	}

	private static void setWarpXaeroVisibility(ServerPlayer player, String warpName, boolean visible) throws Exception {
		Optional<NamedLocation> optionalWarp = resolveWarpForCommand(warpName, player);
		if (optionalWarp.isEmpty()) {
			return;
		}

		Player playerData = STORAGE.addPlayer(player.getStringUUID());
		NamedLocation warp = optionalWarp.get();
		boolean currentlyVisible = !playerData.isWarpHidden(warp.getUuid());
		if (currentlyVisible == visible) {
			WarpMessages.sendPlayerMapVisibilityAlready(player, visible);
			return;
		}

		if (visible) {
			playerData.showWarp(warp.getUuid());
		} else {
			playerData.hideWarp(warp.getUuid());
		}
		WarpMessages.sendPlayerMapVisibilityChanged(player, visible);
		printWarps(player.createCommandSourceStack(), player);
	}

	private static void setGlobalWarpXaeroVisibility(ServerPlayer player, String warpName, boolean visible)
			throws Exception {
		Optional<NamedLocation> optionalWarp = resolveWarpForCommand(warpName, player);
		if (optionalWarp.isEmpty()) {
			return;
		}

		NamedLocation warp = optionalWarp.get();
		if (warp.isXaeroVisible() == visible) {
			WarpMessages.sendMapVisibilityAlready(player, visible);
			return;
		}

		warp.setXaeroVisible(visible);
		WarpMessages.sendMapVisibilityChanged(player, visible);
		printAdminWarpMap(player);
	}

	private static Optional<NamedLocation> resolveWarpForCommand(String warpName, ServerPlayer player) {
		Optional<NamedLocation> optionalWarp = LocationResolver.resolveWarp(warpName);
		if (optionalWarp.isEmpty()) {
			WarpMessages.sendNotFound(player);
		}
		return optionalWarp;
	}
}
