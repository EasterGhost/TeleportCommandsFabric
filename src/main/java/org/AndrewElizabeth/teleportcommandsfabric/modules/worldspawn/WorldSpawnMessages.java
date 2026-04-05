package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class WorldSpawnMessages {

	private WorldSpawnMessages() {
	}

	public static boolean ensureEnabled(ServerPlayer player, boolean enabled) {
		if (enabled) {
			return true;
		}
		send(player, "commands.teleport_commands.worldspawn.disabled", ChatFormatting.RED);
		return false;
	}

	public static int execute(ServerPlayer player, String errorLogMessage, WorldSpawnAction action) {
		try {
			return action.run();
		} catch (Exception error) {
			ModConstants.LOGGER.error(errorLogMessage, error);
			send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.sendSystemMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	public static void sendSame(ServerPlayer player) {
		send(player, "commands.teleport_commands.worldspawn.same", ChatFormatting.AQUA);
	}

	public static void sendGo(ServerPlayer player) {
		player.sendSystemMessage(getTranslatedText("commands.teleport_commands.worldspawn.go", player), true);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player) {
		player.sendSystemMessage(Component.empty()
				.append(getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
						.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
				.append("\n")
				.append(getTranslatedText("commands.teleport_commands.common.safetyIsForLosers", player)
						.withStyle(ChatFormatting.WHITE))
				.append("\n")
				.append(getTranslatedText("commands.teleport_commands.common.forceTeleport", player)
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("WorldSpawnCommand true"))))
				.append("\n"), false);
	}

	@FunctionalInterface
	public interface WorldSpawnAction {
		int run() throws Exception;
	}
}
