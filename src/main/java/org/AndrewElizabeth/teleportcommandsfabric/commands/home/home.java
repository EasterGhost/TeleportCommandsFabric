package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.utils.UnicodeStringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class home {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildSetNode());
		commandDispatcher.register(buildUpdateNode());
		commandDispatcher.register(buildTeleportNode());
		commandDispatcher.register(buildDeleteNode());
		commandDispatcher.register(buildRenameNode());
		commandDispatcher.register(buildDefaultNode());
		commandDispatcher.register(buildListNode());
		commandDispatcher.register(buildPagePickerNode());
		commandDispatcher.register(buildMapVisibilityNode());
		commandDispatcher.register(buildSilentMapVisibilityNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return Commands.literal("sethome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.executes(context -> {
							final String name = UnicodeStringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while setting a home! => ",
									"commands.teleport_commands.home.setError",
									() -> HomeMutationActions.setHome(player, name));
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
							() -> HomeTeleportActions.goHome(player, ""));
				})
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = UnicodeStringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while going to a specific home! => ",
									"commands.teleport_commands.home.goError",
									() -> HomeTeleportActions.goHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return Commands.literal("delhome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = UnicodeStringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while deleting a home! => ",
									"commands.teleport_commands.home.deleteError",
									() -> HomeMutationActions.deleteHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamehome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("newName", UnicodeStringArgumentType.unicodeString())
								.executes(context -> {
									final String name = UnicodeStringArgumentType.getString(context, "name");
									final String newName = UnicodeStringArgumentType.getString(context, "newName");
									final ServerPlayer player = context.getSource().getPlayerOrException();

									if (!HomeMessages.ensureEnabled(player)) {
										return 1;
									}

									return HomeMessages.execute(
											player,
											"Error while renaming a home! => ",
											"commands.teleport_commands.home.renameError",
											() -> HomeMutationActions.renameHome(player, name, newName));
								})));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return Commands.literal("defaulthome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = UnicodeStringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while setting the default home! => ",
									"commands.teleport_commands.home.defaultError",
									() -> HomeMutationActions.setDefaultHome(player, name));
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
							() -> HomeCommandSupport.withPlayerStorage(player,
									playerStorage -> HomeCommandSupport.printHomes(player, playerStorage)));
				})
				.then(Commands.argument("page", IntegerArgumentType.integer())
						.executes(context -> {
							final ServerPlayer player = context.getSource().getPlayerOrException();
							final int page = IntegerArgumentType.getInteger(context, "page");

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while printing the homes! => ",
									"commands.teleport_commands.homes.error",
									() -> HomeCommandSupport.withPlayerStorage(player,
											playerStorage -> HomeCommandSupport.printHomes(player, playerStorage, page)));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return Commands.literal("updatehome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = UnicodeStringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while updating a home location! => ",
									"commands.teleport_commands.home.updateError",
									() -> HomeMutationActions.updateHome(player, name));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:homespages")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("page", IntegerArgumentType.integer())
						.executes(context -> {
							final ServerPlayer player = context.getSource().getPlayerOrException();
							final int page = IntegerArgumentType.getInteger(context, "page");

							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}

							return HomeMessages.execute(
									player,
									"Error while printing the home page picker! => ",
									"commands.teleport_commands.homes.error",
									() -> HomeCommandSupport.withPlayerStorage(player,
											playerStorage -> HomeCommandSupport.printHomePagePicker(player, playerStorage,
													page)));
						}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("maphome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String homeName = UnicodeStringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return handleMapVisibility(player, homeName, visible);
								})));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:maphome")
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("name", UnicodeStringArgumentType.unicodeString())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String homeName = UnicodeStringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return handleSilentMapVisibility(player, homeName, visible);
								})
								.then(Commands.argument("page", IntegerArgumentType.integer())
										.executes(context -> {
											final ServerPlayer player = context.getSource().getPlayerOrException();
											final String homeName = UnicodeStringArgumentType.getString(context, "name");
											final boolean visible = BoolArgumentType.getBool(context, "visible");
											final int page = IntegerArgumentType.getInteger(context, "page");

											return handleSilentMapVisibility(player, homeName, visible, page);
										}))));
	}

	private static int handleMapVisibility(ServerPlayer player, String homeName, boolean visible) {
		if (!HomeMessages.ensureEnabled(player)) {
			return 1;
		}

		return HomeMessages.execute(
				player,
				"Error while updating home Xaero visibility! => ",
				"commands.teleport_commands.homes.error",
				() -> HomeVisibilityActions.setVisibility(player, homeName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String homeName, boolean visible) {
		if (!org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getHome().isEnabled()) {
			return 1;
		}

		return HomeMessages.executeSilently(
				"Error while updating home Xaero visibility! => ",
				() -> HomeVisibilityActions.setVisibilitySilently(player, homeName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String homeName, boolean visible, int page) {
		if (!org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getHome().isEnabled()) {
			return 1;
		}

		return HomeMessages.executeSilently(
				"Error while updating home Xaero visibility! => ",
				() -> HomeVisibilityActions.setVisibilitySilentlyAndShowPage(player, homeName, visible, page));
	}

}
