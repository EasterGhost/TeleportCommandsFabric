package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportXaeroMinimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Temporary fallback path for map waypoint teleport.
 * Primary behavior stays in GuiMapMixin (right-click option replacement).
 * This backup keeps tagged WarpCommand/HomeCommand teleport working when Xaero UI callback signatures drift.
 */
@Mixin(SupportXaeroMinimap.class)
public class MapWaypointTeleportFallbackMixin {
	private static final String WARP_TAG_PREFIX = "TPC-W ";
	private static final String HOME_TAG_PREFIX = "TPC-H ";

	@Inject(method = "teleportToWaypoint", at = @At("HEAD"), cancellable = true)
	private void tpc$fallbackTeleportToTaggedWaypoint(Screen parent, xaero.map.mods.gui.Waypoint waypoint,
			MinimapWorld world, CallbackInfo ci) {
		String replacement = tpc$buildTeleportCommand(waypoint);
		if (replacement == null) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.connection == null) {
			return;
		}

		mc.player.connection.sendCommand(replacement);
		ci.cancel();
	}

	private static String tpc$buildTeleportCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		String name = waypoint.getName();
		if (name == null) {
			return null;
		}

		String normalizedName = tpc$stripTaggedPrefix(name);
		if (normalizedName.isBlank()) {
			return null;
		}

		if (name.startsWith(WARP_TAG_PREFIX)) {
			return "warp " + CommandArgumentUtils.quote(normalizedName);
		}
		if (name.startsWith(HOME_TAG_PREFIX)) {
			return "home " + CommandArgumentUtils.quote(normalizedName);
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
