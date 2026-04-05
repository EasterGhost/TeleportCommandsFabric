package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.DimensionSuggestionProvider;
import org.AndrewElizabeth.teleportcommandsfabric.core.command.PagedNodeCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.core.command.WaypointNodeBuilder;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Predicate;

final class WarpNodeFactory {
	private static final DimensionSuggestionProvider DIMENSION_SUGGESTIONS = new DimensionSuggestionProvider();

	private static final Predicate<CommandSourceStack> REQUIRE_ADMIN = source -> source.permissions()
			.hasPermission(Permissions.COMMANDS_ADMIN);
	private static final Predicate<CommandSourceStack> REQUIRE_PLAYER = source -> source.getPlayer() != null;
	private static final String DISABLED_KEY = "commands.teleport_commands.warp.disabled";

	private WarpNodeFactory() {
	}

	private static GlobalWarpSource getSource(ServerPlayer player) {
		return new GlobalWarpSource();
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return WaypointNodeBuilder.buildActionNode(
				"setwarp", REQUIRE_ADMIN, new WarpSuggestionProvider(), WarpNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.set(player, name, source, "Error while setting the warp!",
						"commands.teleport_commands.warp.setError", "commands.teleport_commands.warp.set",
						"commands.teleport_commands.warp.exists", "commands.teleport_commands.warp.max"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return WaypointNodeBuilder.buildActionNode(
				"delwarp", REQUIRE_ADMIN, new WarpSuggestionProvider(), WarpNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.delete(player, name, source, "Error while deleting the warp!",
						"commands.teleport_commands.warp.deleteError", "commands.teleport_commands.warp.delete",
						"commands.teleport_commands.warp.notFound"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return WaypointNodeBuilder.buildActionNode(
				"updatewarp", REQUIRE_ADMIN, new WarpSuggestionProvider(), WarpNodeFactory::getSource, DISABLED_KEY,
				(player, name, source) -> WaypointCrudService.update(player, name, source,
						"Error while updating the warp location!", "commands.teleport_commands.warp.updateError",
						"commands.teleport_commands.warp.update", "commands.teleport_commands.warp.notFound",
						"commands.teleport_commands.warp.updateSame"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildTeleportNode() {
		return Commands.literal("warp")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.executes(context -> {
							final String name = StringArgumentType.getString(context, "name");
							final ServerPlayer player = context.getSource().getPlayerOrException();

							if (!WarpMessages.ensureEnabled(player)) {
								return 1;
							}

							return WarpMessages.execute(player, "Error while going to the warp!", "commands.teleport_commands.warp.goError",
									() -> WarpTeleportActions.goToWarp(player, name));
						}));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return WaypointNodeBuilder.buildRenameNode(
				"renamewarp", REQUIRE_ADMIN, new WarpSuggestionProvider(), WarpNodeFactory::getSource, DISABLED_KEY,
				(player, name, newName, source) -> WaypointCrudService.rename(player, name, newName, source,
						"Error while renaming the warp!", "commands.teleport_commands.warp.renameError",
						"commands.teleport_commands.warp.rename", "commands.teleport_commands.warp.notFound",
						"commands.teleport_commands.common.nameExists"));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("warps")
				.requires(REQUIRE_PLAYER)
				.executes(context -> WarpCommand.executeList(context, 1, null))
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, WarpCommand::executeList))
				.then(PagedNodeCommandSupport.dimensionOnly(1, DIMENSION_SUGGESTIONS, WarpCommand::executeList));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:warpspages")
				.requires(REQUIRE_PLAYER)
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, WarpCommand::executePagePicker));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildMapVisibilityNode() {
		return Commands.literal("mapwarp")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> {
									final ServerPlayer player = context.getSource().getPlayerOrException();
									final String warpName = StringArgumentType.getString(context, "name");
									final boolean visible = BoolArgumentType.getBool(context, "visible");

									return WarpCommand.handleMapVisibility(player, warpName, visible);
								})));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:mapwarp")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> WarpCommand.executeSilentMapVisibility(context, null, null))
								.then(PagedNodeCommandSupport.pageWithOptionalDimension(null, WarpCommand::executeSilentMapVisibility))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildAdminMapListNode() {
		return Commands.literal("gwarpmap")
				.requires(source -> REQUIRE_PLAYER.test(source) && REQUIRE_ADMIN.test(source))
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!WarpMessages.ensureEnabled(player)) {
						return 1;
					}

					return WarpMessages.execute(player, "Error while printing warp map visibility list!",
							"commands.teleport_commands.gwarpmap.error", () -> WarpCommandSupport.printAdminWarpMap(player));
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

									return WarpMessages.execute(player, "Error while updating global warp Xaero visibility!",
											"commands.teleport_commands.gwarpmap.error",
											() -> WarpVisibilityActions.setGlobalVisibility(player, warpName, visible));
								})));
	}
}
