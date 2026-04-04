package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.DimensionSuggestionProvider;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PagedNodeCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PlayerCommandExecutionSupport;

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

	private WarpNodeFactory() {
	}

	@FunctionalInterface
	private interface WarpMutationAction {
		void run(ServerPlayer player, String name) throws Exception;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildActionNode(
			String literal,
			Predicate<CommandSourceStack> permission,
			String logPrefix,
			String transKey,
			WarpMutationAction action) {
		return Commands.literal(literal)
				.requires(permission)
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(new WarpSuggestionProvider())
						.executes(context -> PlayerCommandExecutionSupport.executeWithEnabledPlayer(
								context,
								WarpMessages::ensureEnabled,
								logPrefix,
								transKey,
								player -> action.run(player, StringArgumentType.getString(context, "name")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSetNode() {
		return buildActionNode(
				"setwarp",
				REQUIRE_ADMIN,
				"Error while setting the warp!",
				"commands.teleport_commands.warp.setError",
				WarpMutationActions::setWarp);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildDeleteNode() {
		return buildActionNode(
				"delwarp",
				REQUIRE_ADMIN,
				"Error while deleting the warp!",
				"commands.teleport_commands.warp.deleteError",
				WarpMutationActions::deleteWarp);
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildUpdateNode() {
		return buildActionNode(
				"updatewarp",
				REQUIRE_ADMIN,
				"Error while updating the warp location!",
				"commands.teleport_commands.warp.updateError",
				WarpMutationActions::updateWarp);
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

							return WarpMessages.execute(
									player,
									"Error while going to the warp!",
									"commands.teleport_commands.warp.goError",
									() -> WarpTeleportActions.goToWarp(player, name));
						}));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRenameNode() {
		return Commands.literal("renamewarp")
				.requires(REQUIRE_ADMIN)
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

	static LiteralArgumentBuilder<CommandSourceStack> buildListNode() {
		return Commands.literal("warps")
				.requires(REQUIRE_PLAYER)
				.executes(context -> warp.executeList(context, 1, null))
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(DIMENSION_SUGGESTIONS, warp::executeList))
				.then(PagedNodeCommandSupport.dimensionOnly(1, DIMENSION_SUGGESTIONS, warp::executeList));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildPagePickerNode() {
		return Commands.literal("teleportcommandsfabric:warpspages")
				.requires(REQUIRE_PLAYER)
				.then(PagedNodeCommandSupport.pageWithOptionalDimension(
						DIMENSION_SUGGESTIONS,
						warp::executePagePicker));
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

									return warp.handleMapVisibility(player, warpName, visible);
								})));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildSilentMapVisibilityNode() {
		return Commands.literal("teleportcommandsfabric:mapwarp")
				.requires(REQUIRE_PLAYER)
				.then(Commands.argument("name", StringArgumentType.string())
						.then(Commands.argument("visible", BoolArgumentType.bool())
								.executes(context -> warp.executeSilentMapVisibility(context, null, null))
								.then(PagedNodeCommandSupport.pageWithOptionalDimension(null,
										warp::executeSilentMapVisibility))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildAdminMapListNode() {
		return Commands.literal("gwarpmap")
				.requires(source -> REQUIRE_PLAYER.test(source) && REQUIRE_ADMIN.test(source))
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
}
