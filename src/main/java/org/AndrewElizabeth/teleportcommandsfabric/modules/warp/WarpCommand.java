package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;

import net.minecraft.commands.CommandSourceStack;

public final class WarpCommand {
	private WarpCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(WarpNodeFactory.buildSetNode());
		dispatcher.register(WarpNodeFactory.buildUpdateNode());
		dispatcher.register(WarpNodeFactory.buildTeleportNode());
		dispatcher.register(WarpNodeFactory.buildTeleportNode("teleportcommandsfabric:warp"));
		dispatcher.register(WarpNodeFactory.buildDeleteNode());
		dispatcher.register(WarpNodeFactory.buildRenameNode());
		dispatcher.register(WarpNodeFactory.buildListNode("warps", false));
		dispatcher.register(WarpNodeFactory.buildListNode("teleportcommandsfabric:warpspages", true));
		dispatcher.register(WarpNodeFactory.buildFilterPickerNode("teleportcommandsfabric:warpsprefixfilters",
				WaypointFilterPickerKind.PREFIX));
		dispatcher.register(WarpNodeFactory.buildFilterPickerNode("teleportcommandsfabric:warpsdimensionfilters",
				WaypointFilterPickerKind.DIMENSION));
		dispatcher.register(WarpNodeFactory.buildPlayerMapVisibilityNode("mapwarp", false));
		dispatcher.register(WarpNodeFactory.buildPlayerMapVisibilityNode("teleportcommandsfabric:mapwarp", true));
		dispatcher.register(WarpNodeFactory.buildPublicGlobalMapVisibilityNode());
		dispatcher.register(WarpNodeFactory.buildGlobalMapVisibilityNode());
		dispatcher.register(WarpNodeFactory.buildUiNode());
	}
}
