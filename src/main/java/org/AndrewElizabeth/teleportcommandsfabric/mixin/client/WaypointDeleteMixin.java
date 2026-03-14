package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.AndrewElizabeth.teleportcommandsfabric.client.XaeroWaypointCommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.WaypointDeleter;
import xaero.hud.minimap.world.MinimapWorld;

@Mixin(WaypointDeleter.class)
public class WaypointDeleteMixin {
	@Shadow
	private List<Waypoint> toDeleteList;

	@Inject(method = "deleteCollected", at = @At("HEAD"))
	private void tpc$hideWaypointOnDelete(MinimapSession session, MinimapWorld world, boolean save, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.connection == null || world == null || toDeleteList == null
				|| toDeleteList.isEmpty()) {
			return;
		}

		String currentSetName = world.getCurrentWaypointSet() == null ? null : world.getCurrentWaypointSet().getName();
		Set<String> commands = new LinkedHashSet<>();
		for (Waypoint waypoint : List.copyOf(toDeleteList)) {
			String command = XaeroWaypointCommandHelper.buildHideCommand(waypoint, currentSetName);
			if (command != null) {
				commands.add(command);
			}
		}

		for (String command : commands) {
			mc.player.connection.sendCommand(command);
		}
	}
}
