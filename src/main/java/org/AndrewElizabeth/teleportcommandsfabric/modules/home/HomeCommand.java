package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

public final class HomeCommand {
	private HomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(HomeNodeFactory.buildSetNode("sethome", false));
		dispatcher.register(HomeNodeFactory.buildSetNode("tmphome", true));
		dispatcher.register(HomeNodeFactory.buildUpdateNode());
		dispatcher.register(HomeNodeFactory.buildTeleportNode());
		dispatcher.register(HomeNodeFactory.buildDeleteNode());
		dispatcher.register(HomeNodeFactory.buildRenameNode());
		dispatcher.register(HomeNodeFactory.buildDefaultNode());
		dispatcher.register(HomeNodeFactory.buildListNode("homes", false));
		dispatcher.register(HomeNodeFactory.buildListNode("teleportcommandsfabric:homespages", true));
		dispatcher.register(HomeNodeFactory.buildMapVisibilityNode("maphome", false));
		dispatcher.register(HomeNodeFactory.buildMapVisibilityNode("teleportcommandsfabric:maphome", true));
	}
}
