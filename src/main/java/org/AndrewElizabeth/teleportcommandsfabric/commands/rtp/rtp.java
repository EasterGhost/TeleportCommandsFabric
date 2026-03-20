package org.AndrewElizabeth.teleportcommandsfabric.commands.rtp;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.services.RtpService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class rtp {

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

					if (!ConfigManager.CONFIG.getRtp().isEnabled()) {
						RtpMessages.send(player, "commands.teleport_commands.rtp.disabled", ChatFormatting.RED);
						return 1;
					}

					int radius = ConfigManager.CONFIG.getRtp().getRadius();
					return RtpMessages.execute(player, "Error while executing /rtp!",
							() -> RtpService.enqueueRandomTeleport(player, radius));
				});
	}
}
