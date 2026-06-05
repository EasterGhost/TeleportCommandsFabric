package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

public final class BackCommand {
	private BackCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(BackNodeFactory.buildBackNode());
	}
}
