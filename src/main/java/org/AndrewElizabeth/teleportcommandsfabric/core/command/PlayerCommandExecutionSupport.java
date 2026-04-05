package org.AndrewElizabeth.teleportcommandsfabric.core.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public final class PlayerCommandExecutionSupport {
	@FunctionalInterface
	public interface PlayerAction {
		void run(ServerPlayer player) throws Exception;
	}

	private PlayerCommandExecutionSupport() {
	}

	public static int executeWithEnabledPlayer(CommandContext<CommandSourceStack> context,
			Predicate<ServerPlayer> enabledCheck, String errorLogMessage, String errorTranslationKey,
			PlayerAction action) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (!enabledCheck.test(player)) {
			return 1;
		}

		return CommandExecutionSupport.execute(player, errorLogMessage, errorTranslationKey, () -> action.run(player));
	}
}
