package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.PlayerCommandExecutionSupport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public class WarpCommand {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(WarpNodeFactory.buildSetNode());
		commandDispatcher.register(WarpNodeFactory.buildUpdateNode());
		commandDispatcher.register(WarpNodeFactory.buildTeleportNode());
		commandDispatcher.register(WarpNodeFactory.buildDeleteNode());
		commandDispatcher.register(WarpNodeFactory.buildRenameNode());
		commandDispatcher.register(WarpNodeFactory.buildListNode());
		commandDispatcher.register(WarpNodeFactory.buildPagePickerNode());
		commandDispatcher.register(WarpNodeFactory.buildMapVisibilityNode());
		commandDispatcher.register(WarpNodeFactory.buildSilentMapVisibilityNode());
		commandDispatcher.register(WarpNodeFactory.buildAdminMapListNode());
	}

	static int handleMapVisibility(ServerPlayer player, String warpName, boolean visible) {
		if (!WarpMessages.ensureEnabled(player)) {
			return 1;
		}

		return WarpMessages.execute(player, "Error while updating warp Xaero visibility!",
				"commands.teleport_commands.warps.error",
				() -> WarpVisibilityActions.setPlayerVisibility(player, warpName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String warpName, boolean visible) {
		if (!CONFIG.getWarp().isEnabled()) {
			return 1;
		}

		return WarpMessages.executeSilently("Error while updating warp Xaero visibility!",
				() -> WarpVisibilityActions.setPlayerVisibilitySilently(player, warpName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String warpName, boolean visible, int page,
			String dimensionFilter) {
		if (!CONFIG.getWarp().isEnabled()) {
			return 1;
		}

		return WarpMessages.executeSilently("Error while updating warp Xaero visibility!",
				() -> WarpVisibilityActions.setPlayerVisibilitySilentlyAndShowPage(player, warpName, visible, page,
						dimensionFilter));
	}

	static int executeList(CommandContext<CommandSourceStack> context, int page, String dimension)
			throws CommandSyntaxException {
		return PlayerCommandExecutionSupport.executeWithEnabledPlayer(context, WarpMessages::ensureEnabled,
				"Error while printing warps!", "commands.teleport_commands.warps.error",
				player -> WarpCommandSupport.printWarps(context.getSource(), player, page, dimension));
	}

	static int executePagePicker(CommandContext<CommandSourceStack> context, int page, String dimension)
			throws CommandSyntaxException {
		return PlayerCommandExecutionSupport.executeWithEnabledPlayer(context, WarpMessages::ensureEnabled,
				"Error while printing the warp page picker!", "commands.teleport_commands.warps.error",
				player -> WarpCommandSupport.printWarpPagePicker(player, page, dimension));
	}

	static int executeSilentMapVisibility(CommandContext<CommandSourceStack> context, Integer page, String dimension)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String warpName = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");

		if (page == null) {
			return handleSilentMapVisibility(player, warpName, visible);
		}
		return handleSilentMapVisibility(player, warpName, visible, page, dimension);
	}
}

