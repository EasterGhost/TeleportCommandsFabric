package org.AndrewElizabeth.teleportcommandsfabric.mixin.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroWaypointCommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaero.hud.minimap.world.MinimapWorld;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(WaypointTeleport.class)
public class WaypointTeleportMixin {
	@Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendCommand(Ljava/lang/String;)V"))
	private void tpc$redirectTeleportSendCommand(ClientPacketListener connection, String originalCommand,
			Waypoint waypoint, MinimapWorld world, Screen parent, boolean safeCheck) {
		String replacement = XaeroWaypointCommandHelper.buildTeleportCommand(waypoint);
		connection.sendCommand(replacement != null ? replacement : originalCommand);
	}

	@Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"))
	private void tpc$redirectTeleportSendChat(ClientPacketListener connection, String originalMessage, Waypoint waypoint,
			MinimapWorld world, Screen parent, boolean safeCheck) {
		String replacement = XaeroWaypointCommandHelper.buildTeleportCommand(waypoint);
		if (replacement != null) {
			connection.sendCommand(replacement);
			return;
		}
		connection.sendChat(originalMessage);
	}
}
