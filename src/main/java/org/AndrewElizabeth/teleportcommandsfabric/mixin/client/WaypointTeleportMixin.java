package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaero.hud.minimap.world.MinimapWorld;

import java.util.Locale;

@Mixin(WaypointTeleport.class)
public class WaypointTeleportMixin {
	private static final String WARP_TAG_PREFIX = "TPC-W ";
	private static final String HOME_TAG_PREFIX = "TPC-H ";

	@Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendCommand(Ljava/lang/String;)V"))
	private void tpc$redirectTeleportSendCommand(
			ClientPacketListener connection,
			String originalCommand,
			Waypoint waypoint,
			MinimapWorld world,
			Screen parent,
			boolean safeCheck) {
		String replacement = tpc$buildTeleportCommand(waypoint);
		connection.sendCommand(replacement != null ? replacement : originalCommand);
	}

	@Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"))
	private void tpc$redirectTeleportSendChat(
			ClientPacketListener connection,
			String originalMessage,
			Waypoint waypoint,
			MinimapWorld world,
			Screen parent,
			boolean safeCheck) {
		String replacement = tpc$buildTeleportCommand(waypoint);
		if (replacement != null) {
			connection.sendCommand(replacement);
			return;
		}
		connection.sendChat(originalMessage);
	}

	private static String tpc$buildTeleportCommand(Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		String name = waypoint.getName();
		String symbol = waypoint.getInitials();
		if (name == null) {
			return null;
		}

		String normalizedName = tpc$stripTaggedPrefix(name);
		if (normalizedName.isBlank()) {
			return null;
		}

		String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
		if ("W".equals(normalizedSymbol) || name.startsWith(WARP_TAG_PREFIX)) {
			return "warp " + normalizedName;
		}
		if ("H".equals(normalizedSymbol) || name.startsWith(HOME_TAG_PREFIX)) {
			return "home " + normalizedName;
		}
		return null;
	}

	private static String tpc$stripTaggedPrefix(String name) {
		if (name.startsWith(WARP_TAG_PREFIX)) {
			return name.substring(WARP_TAG_PREFIX.length()).trim();
		}
		if (name.startsWith(HOME_TAG_PREFIX)) {
			return name.substring(HOME_TAG_PREFIX.length()).trim();
		}
		return name.trim();
	}
}
