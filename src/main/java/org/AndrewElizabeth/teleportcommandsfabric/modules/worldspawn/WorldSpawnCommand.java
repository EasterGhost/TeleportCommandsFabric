package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class WorldSpawnCommand {
	private WorldSpawnCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(WorldSpawnNodeFactory.buildWorldSpawnNode());
	}
}
