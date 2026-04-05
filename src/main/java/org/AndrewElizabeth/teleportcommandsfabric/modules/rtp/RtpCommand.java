package org.AndrewElizabeth.teleportcommandsfabric.modules.rtp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.RtpService;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public class RtpCommand {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		RtpService.initialize();
		commandDispatcher.register(buildRtpCommand("rtp"));
		commandDispatcher.register(buildRtpCommand("wild"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRtpCommand(String commandName) {
		return Commands.literal(commandName)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!CONFIG.getRtp().isEnabled()) {
						RtpMessages.send(player, "commands.teleport_commands.rtp.disabled", ChatFormatting.RED);
						return 1;
					}

					int radius = CONFIG.getRtp().getRadius();
					return RtpMessages.execute(player, "Error while executing /rtp!",
							() -> RtpService.enqueueRandomTeleport(player, radius));
				});
	}
}

