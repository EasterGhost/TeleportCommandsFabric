package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.DimensionSuggestionProvider;
import org.AndrewElizabeth.teleportcommandsfabric.core.command.PagedNodeCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.core.command.WaypointNodeBuilder;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

final class HomeNodeFactory {
	private static final DimensionSuggestionProvider DIMENSION_SUGGESTIONS = new DimensionSuggestionProvider();
	private static final Predicate<CommandSourceStack> REQUIRE_PLAYER = source -> source.getPlayer() != null;
	private static final String DISABLED_KEY = "commands.teleport_commands.home.disabled";

	private HomeNodeFactory() {
	}

	private static PlayerHomeSource getSource(ServerPlayer player) {
		return new PlayerHomeSource(StorageManager.STORAGE.addPlayer(player.getStringUUID()));
	}

	private static TemporaryHomeSource getTemporarySource(ServerPlayer player) {
		return new TemporaryHomeSource(StorageManager.STORAGE.addPlayer(player.getStringUUID()));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return WaypointNodeBuilder.buildActionNode(
				"sethome", REQUIRE_PLAYER, new HomeSuggestionProvider(), HomeNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.set(player, name, source, "Error while setting a home!",
						"commands.teleport_commands.home.setError", "commands.teleport_commands.home.set",
						"commands.teleport_commands.home.exists", "commands.teleport_commands.home.max"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTemporarySetNode() {
		return WaypointNodeBuilder.buildActionNode(
				"tmphome", REQUIRE_PLAYER, new HomeSuggestionProvider(), HomeNodeFactory::getTemporarySource,
				DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.set(player, name, source,
						"Error while setting a temporary home!", "commands.teleport_commands.home.tempSetError",
						"commands.teleport_commands.home.tempSet", "commands.teleport_commands.home.exists",
						"commands.teleport_commands.home.max"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return WaypointNodeBuilder.buildActionNode(
				"delhome", REQUIRE_PLAYER, new HomeSuggestionProvider(), HomeNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.delete(player, name, source, "Error while deleting a home!",
						"commands.teleport_commands.home.deleteError", "commands.teleport_commands.home.delete",
						"commands.teleport_commands.home.notFound"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDefaultNode() {
		return WaypointNodeBuilder.buildActionNode(
				"defaulthome", REQUIRE_PLAYER, new HomeSuggestionProvider(home -> !home.isTemporary()),
				HomeNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> HomeMutationActions.setDefaultHome(player, name, (PlayerHomeSource) source));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return WaypointNodeBuilder.buildActionNode(
				"updatehome", REQUIRE_PLAYER, new HomeSuggestionProvider(), HomeNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.update(player, name, source, "Error while updating a home location!",
						"commands.teleport_commands.home.updateError", "commands.teleport_commands.home.update",
						"commands.teleport_commands.home.notFound", "commands.teleport_commands.home.updateSame"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("home")
				.requires(REQUIRE_PLAYER)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();
					if (!HomeMessages.ensureEnabled(player)) {
						return 1;
					}
					return HomeMessages.execute(player, "Error while going home! => ", "commands.teleport_commands.home.goError",
							() -> HomeTeleportActions.goHome(player, ""));
				})
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();
							if (!HomeMessages.ensureEnabled(player)) {
								return 1;
							}
							return HomeMessages.execute(player, "Error while going to a specific home! => ",
									"commands.teleport_commands.home.goError", () -> HomeTeleportActions.goHome(player, name));
						}));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return WaypointNodeBuilder.buildRenameNode(
				"renamehome", REQUIRE_PLAYER, new HomeSuggestionProvider(), HomeNodeFactory::getSource, DISABLED_KEY,
				(player, name, newName, source) -> WaypointCrudService.rename(player, name, newName, source,
						"Error while renaming a home!", "commands.teleport_commands.home.renameError",
						"commands.teleport_commands.home.rename", "commands.teleport_commands.home.notFound",
						"commands.teleport_commands.common.nameExists"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("homes")
				.requires(REQUIRE_PLAYER)
				.executes(context -> HomeCommand.executeList(context, 1, null))
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, HomeCommand::executeList))
				.then(PagedNodeCommandSupport.dimensionOnly(1, DIMENSION_SUGGESTIONS, HomeCommand::executeList));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:homespages")
				.requires(REQUIRE_PLAYER)
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, HomeCommand::executePagePicker));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("maphome")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new HomeSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String homeName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return HomeCommand.handleMapVisibility(player, homeName, visible);
								})));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:maphome")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> HomeCommand.executeSilentMapVisibility(context, null, null))
								.then(PagedNodeCommandSupport.pageWithOptionalDimension(null, HomeCommand::executeSilentMapVisibility))));
	}
}
