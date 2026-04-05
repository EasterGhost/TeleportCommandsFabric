package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.mods.SupportXaeroMinimap;

import org.AndrewElizabeth.teleportcommandsfabric.client.xaero.XaeroWaypointCommandHelper;

import net.minecraft.client.Minecraft;

@Mixin(SupportXaeroMinimap.class)
public class MapWaypointDeleteMixin {
	@Inject(method = "deleteWaypoint", at = @At("HEAD"))
	private void tpc$hideWaypointOnDelete(xaero.map.mods.gui.Waypoint waypoint, CallbackInfo ci) {
		String command = XaeroWaypointCommandHelper.buildHideCommand(waypoint);
		if (command == null) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.connection == null) {
			return;
		}

		mc.player.connection.sendCommand(command);
	}
}
