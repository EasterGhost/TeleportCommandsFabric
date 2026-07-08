package org.AndrewElizabeth.teleportcommandsfabric.modules.teleport;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class TeleportCancelCommand {
	private TeleportCancelCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tpcancel")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> execute(context.getSource().getPlayerOrException())));
	}

	private static int execute(ServerPlayer player) {
		TeleportService service = TeleportCommands.TELEPORT_SERVICE;
		if (service == null) {
			MessageSupport.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		boolean cancelled = service.cancelCurrent(player.getUUID(), TeleportStatus.CANCELLED).isPresent();
		if (cancelled) {
			MessageSupport.send(player, "commands.teleport_commands.tpcancel.cancelled", ChatFormatting.AQUA);
		} else {
			MessageSupport.send(player, "commands.teleport_commands.tpcancel.none", ChatFormatting.YELLOW);
		}
		return 0;
	}
}
