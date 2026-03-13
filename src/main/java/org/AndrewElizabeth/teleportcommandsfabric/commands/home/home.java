package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver;
import org.AndrewElizabeth.teleportcommandsfabric.services.NamedLocationTeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

public class home {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildSetNode());
		commandDispatcher.register(buildTeleportNode());
		commandDispatcher.register(buildDeleteNode());
		commandDispatcher.register(buildRenameNode());
		commandDispatcher.register(buildDefaultNode());
		commandDispatcher.register(buildListNode());
		commandDispatcher.register(buildMapVisibilityNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("sethome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while setting a home! => ",
									"commands.teleport_commands.home.setError",
									() -> setHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("home")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!HomeMessages.ensureEnabled(player)) {
						return 1;
					}

					return HomeMessages.execute(
							player,
							"Error while going home! => ",
							"commands.teleport_commands.home.goError",
							() -> goHome(player, ""));
				})
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while going to a specific home! => ",
									"commands.teleport_commands.home.goError",
									() -> goHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delhome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while deleting a home! => ",
									"commands.teleport_commands.home.deleteError",
									() -> deleteHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamehome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("newName", StringArgumentType.string())
								.executes(context -> {
									final String name = StringArgumentType.getString(context, "name");
									final String newName = StringArgumentType.getString(context, "newName");
									final ServerPlayer player = context.getSource().getPlayerOrException();

									if (!HomeMessages.ensureEnabled(player)) {
										return 1;
									}

									return HomeMessages.execute(
											player,
											"Error while renaming a home! => ",
											"commands.teleport_commands.home.renameError",
											() -> renameHome(player, name, newName));
								})));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return Commands.literal("defaulthome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while setting the default home! => ",
									"commands.teleport_commands.home.defaultError",
									() -> setDefaultHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("homes")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!HomeMessages.ensureEnabled(player)) {
						return 1;
					}

					return HomeMessages.execute(
							player,
							"Error while printing the homes! => ",
							"commands.teleport_commands.homes.error",
							() -> printHomes(player));
				});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("maphome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String homeName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									if (!HomeMessages.ensureEnabled(player)) {
										return 1;
									}

									return HomeMessages.execute(
											player,
											"Error while updating home Xaero visibility! => ",
											"commands.teleport_commands.homes.error",
											() -> setHomeXaeroVisibility(player, homeName, visible));
								})));
	}

	private static void setHome(ServerPlayer player, String homeName) throws Exception {
		homeName = LocationResolver.normalizeName(homeName);
		String worldString = WorldResolver.getDimensionId(player.level().dimension());

		Player playerStorage = STORAGE.addPlayer(player.getStringUUID());

		int maxHomes = org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.home.getPlayerMaximum();
		boolean homeExists = playerStorage.getHome(homeName).isPresent();
		if (!homeExists && maxHomes > 0 && playerStorage.getHomes().size() >= maxHomes) {
			HomeMessages.sendMaxReached(player, maxHomes);
			return;
		}

		NamedLocation home = NamedLocation.create(
				homeName,
				player.getBlockX(),
				player.getY(),
				player.getBlockZ(),
				worldString);

		boolean homeAlreadyExists = playerStorage.addHome(home);

		if (homeAlreadyExists) {
			HomeMessages.send(player, "commands.teleport_commands.home.exists", ChatFormatting.RED);
		} else {
			if (playerStorage.getHomes().size() == 1) {
				playerStorage.setDefaultHomeByUuid(home.getUuid());
			}

			HomeMessages.send(player, "commands.teleport_commands.home.set");
		}
	}

	private static void goHome(ServerPlayer player, String homeName) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Player playerStorage = optionalPlayerStorage.get();

		if (homeName.isEmpty()) {
			String defaultHome = playerStorage.getDefaultHome();

			if (defaultHome.isEmpty()) {
				HomeMessages.send(player, "commands.teleport_commands.home.defaultNone", ChatFormatting.AQUA);
				return;
			}
			homeName = defaultHome;
		}

		Optional<NamedLocation> optionalHome = resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.AQUA);
		if (optionalHome.isEmpty()) {
			return;
		}

		goHome(player, playerStorage, optionalHome.get());
	}

	private static void goHome(ServerPlayer player, Player playerStorage, NamedLocation home) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(home);

		if (optionalWorld.isEmpty()) {
			Constants.LOGGER.warn(
					"({}) Error while going to the home \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					home.getName(),
					home.getWorldString());

			HomeMessages.sendWorldNotFound(player);

			if (org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getHome().isDeleteInvalid()) {
				playerStorage.deleteHomeNoSave(home);
				StorageManager.StorageSaver();
				Constants.LOGGER.info("Deleted invalid home '{}' for player {}", home.getName(),
						player.getName().getString());
				HomeMessages.sendDeletedInvalid(player);
			}
			return;
		}

		ServerLevel homeWorld = optionalWorld.get();
		if (NamedLocationTeleportService.isAlreadyAtDestination(player, homeWorld, home)) {
			HomeMessages.send(player, "commands.teleport_commands.home.goSame", ChatFormatting.AQUA);
		} else {
			if (NamedLocationTeleportService.teleportToNamedLocation(player, homeWorld, home)) {
				HomeMessages.send(player, "commands.teleport_commands.home.go");
			}
		}
	}

	private static void deleteHome(ServerPlayer player, String homeName) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Player playerStorage = optionalPlayerStorage.get();

		Optional<NamedLocation> optionalHome = resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED);
		if (optionalHome.isEmpty()) {
			return;
		}

		playerStorage.deleteHome(optionalHome.get());
		HomeMessages.send(player, "commands.teleport_commands.home.delete");
	}

	private static void renameHome(ServerPlayer player, String homeName, String newHomeName) throws Exception {
		newHomeName = LocationResolver.normalizeName(newHomeName);

		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Player playerStorage = optionalPlayerStorage.get();

		if (LocationResolver.resolveHome(playerStorage, newHomeName).isPresent()) {
			HomeMessages.sendNameExists(player);
			return;
		}

		Optional<NamedLocation> optionalHome = resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED);
		if (optionalHome.isEmpty()) {
			return;
		}

		UUID homeUuid = optionalHome.get().getUuid();
		optionalHome.get().setName(newHomeName);
		if (homeUuid.equals(playerStorage.getDefaultHomeUuid())) {
			playerStorage.setDefaultHomeByUuid(homeUuid);
		}

		HomeMessages.send(player, "commands.teleport_commands.home.rename");
	}

	private static void setDefaultHome(ServerPlayer player, String homeName) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Player playerStorage = optionalPlayerStorage.get();

		Optional<NamedLocation> optionalHome = resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED);
		if (optionalHome.isEmpty()) {
			return;
		}

		if (optionalHome.get().getUuid().equals(playerStorage.getDefaultHomeUuid())) {
			HomeMessages.send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
			return;
		}

		playerStorage.setDefaultHomeByUuid(optionalHome.get().getUuid());
		HomeMessages.send(player, "commands.teleport_commands.home.default");
	}

	private static void printHomes(ServerPlayer player) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Player playerStorage = optionalPlayerStorage.get();
		List<NamedLocation> homes = playerStorage.getHomes();

		if (homes.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(HomeFormatter.buildHomeList(player, playerStorage, homes), false);
	}

	private static void setHomeXaeroVisibility(ServerPlayer player, String homeName, boolean visible) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		Optional<NamedLocation> optionalHome = resolveHomeForCommand(optionalPlayerStorage.get(), homeName, player,
				ChatFormatting.RED);
		if (optionalHome.isEmpty()) {
			return;
		}

		NamedLocation home = optionalHome.get();
		if (home.isXaeroVisible() == visible) {
			HomeMessages.sendMapVisibilityAlready(player, visible);
			return;
		}

		home.setXaeroVisible(visible);
		HomeMessages.sendMapVisibilityChanged(player, visible);
	}

	private static Optional<NamedLocation> resolveHomeForCommand(Player playerStorage, String homeName, ServerPlayer player,
			ChatFormatting notFoundColor) {
		Optional<NamedLocation> optionalHome = LocationResolver.resolveHome(playerStorage, homeName);
		if (optionalHome.isEmpty()) {
			HomeMessages.sendNotFound(player, notFoundColor);
		}
		return optionalHome;
	}
}
