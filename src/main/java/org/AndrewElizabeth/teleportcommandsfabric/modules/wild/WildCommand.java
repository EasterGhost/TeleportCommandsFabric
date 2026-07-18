package org.AndrewElizabeth.teleportcommandsfabric.modules.wild;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

public final class WildCommand {
	private WildCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(WildNodeFactory.build());
	}
}
