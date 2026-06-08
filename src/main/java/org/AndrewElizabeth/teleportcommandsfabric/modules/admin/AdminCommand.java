package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

public final class AdminCommand {
	private static final String PRIMARY_COMMAND = "tpc";

	private AdminCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(AdminNodeFactory.buildRootNode(PRIMARY_COMMAND));
	}
}
