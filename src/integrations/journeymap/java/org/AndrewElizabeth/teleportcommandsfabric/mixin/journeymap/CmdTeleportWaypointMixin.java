package org.AndrewElizabeth.teleportcommandsfabric.mixin.journeymap;

import org.AndrewElizabeth.teleportcommandsfabric.integration.journeymap.JourneyMapWaypointCommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import journeymap.api.v2.common.waypoint.Waypoint;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;

@Mixin(targets = "journeymap.client.command.CmdTeleportWaypoint", remap = false)
public class CmdTeleportWaypointMixin {
	@Inject(method = "run", at = @At("HEAD"), cancellable = true, remap = false)
	private void tpc$redirectTpcWaypointTeleport(CallbackInfo info) {
		Object waypoint = tpc$getWaypoint();
		if (!(waypoint instanceof Waypoint journeyMapWaypoint)) {
			return;
		}

		String command = JourneyMapWaypointCommandHelper.buildTeleportCommand(journeyMapWaypoint);
		if (command == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.player.connection == null) {
			return;
		}
		client.player.connection.sendCommand(command);
		info.cancel();
	}

	private Object tpc$getWaypoint() {
		try {
			Field field = ((Object) this).getClass().getDeclaredField("waypoint");
			field.setAccessible(true);
			return field.get(this);
		} catch (ReflectiveOperationException | InaccessibleObjectException | SecurityException exception) {
			return null;
		}
	}
}
