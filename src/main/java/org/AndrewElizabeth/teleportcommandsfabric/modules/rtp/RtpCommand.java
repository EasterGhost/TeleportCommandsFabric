package org.AndrewElizabeth.teleportcommandsfabric.modules.rtp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class RtpCommand {
	private RtpCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(RtpNodeFactory.buildRtpCommand("rtp"));
	}
}
