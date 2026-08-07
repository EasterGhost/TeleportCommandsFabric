package org.AndrewElizabeth.teleportcommandsfabric.modules.rtp;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class RtpNodeFactory {
	private RtpNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildRtpCommand(String commandName) {
		return Commands.literal(commandName)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> RtpHandler.execute(context.getSource().getPlayerOrException()));
	}
}
