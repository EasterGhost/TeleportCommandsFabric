package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.PlayerCommandExecutionSupport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public class HomeCommand {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(HomeNodeFactory.buildSetNode());
		commandDispatcher.register(HomeNodeFactory.buildUpdateNode());
		commandDispatcher.register(HomeNodeFactory.buildTeleportNode());
		commandDispatcher.register(HomeNodeFactory.buildDeleteNode());
		commandDispatcher.register(HomeNodeFactory.buildRenameNode());
		commandDispatcher.register(HomeNodeFactory.buildDefaultNode());
		commandDispatcher.register(HomeNodeFactory.buildListNode());
		commandDispatcher.register(HomeNodeFactory.buildPagePickerNode());
		commandDispatcher.register(HomeNodeFactory.buildMapVisibilityNode());
		commandDispatcher.register(HomeNodeFactory.buildSilentMapVisibilityNode());
	}

	static int handleMapVisibility(ServerPlayer player, String homeName, boolean visible) {
		if (!HomeMessages.ensureEnabled(player)) {
			return 1;
		}

		return HomeMessages.execute(player, "Error while updating home Xaero visibility! => ",
				"commands.teleport_commands.homes.error", () -> HomeVisibilityActions.setVisibility(player, homeName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String homeName, boolean visible) {
		if (!CONFIG.getHome().isEnabled()) {
			return 1;
		}

		return HomeMessages.executeSilently("Error while updating home Xaero visibility! => ",
				() -> HomeVisibilityActions.setVisibilitySilently(player, homeName, visible));
	}

	private static int handleSilentMapVisibility(ServerPlayer player, String homeName, boolean visible, int page,
			String dimensionFilter) {
		if (!CONFIG.getHome().isEnabled()) {
			return 1;
		}

		return HomeMessages.executeSilently("Error while updating home Xaero visibility! => ",
				() -> HomeVisibilityActions.setVisibilitySilentlyAndShowPage(player, homeName, visible, page, dimensionFilter));
	}

	static int executeList(CommandContext<CommandSourceStack> context, int page, String dimension)
			throws CommandSyntaxException {
		return PlayerCommandExecutionSupport.executeWithEnabledPlayer(context, HomeMessages::ensureEnabled,
				"Error while printing the homes! => ", "commands.teleport_commands.homes.error",
				player -> HomeCommandSupport.withPlayerStorage(player,
						playerStorage -> HomeCommandSupport.printHomes(player, playerStorage, page, dimension)));
	}

	static int executePagePicker(CommandContext<CommandSourceStack> context, int page, String dimension)
			throws CommandSyntaxException {
		return PlayerCommandExecutionSupport.executeWithEnabledPlayer(context, HomeMessages::ensureEnabled,
				"Error while printing the home page picker! => ", "commands.teleport_commands.homes.error",
				player -> HomeCommandSupport.withPlayerStorage(player,
						playerStorage -> HomeCommandSupport.printHomePagePicker(player, playerStorage, page, dimension)));
	}

	static int executeSilentMapVisibility(CommandContext<CommandSourceStack> context, Integer page, String dimension)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String homeName = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");

		if (page == null) {
			return handleSilentMapVisibility(player, homeName, visible);
		}
		return handleSilentMapVisibility(player, homeName, visible, page, dimension);
	}
}

