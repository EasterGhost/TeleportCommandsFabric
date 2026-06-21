package org.AndrewElizabeth.teleportcommandsfabric.integration.journeymap;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.MapWaypointAdapter;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.event.common.WaypointEvent;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class JourneyMapWaypointAdapter implements MapWaypointAdapter {
	private static final int WARP_COLOR = 0x55CCFF;
	private static final int HOME_COLOR = 0x55FF88;
	private final IClientAPI api;
	private boolean applyingSnapshot;
	private boolean hasAppliedSnapshot;
	private int lastSnapshotHash;

	JourneyMapWaypointAdapter(IClientAPI api) {
		this.api = api;
	}

	@Override
	public String id() {
		return "journeymap";
	}

	@Override
	public boolean applySnapshot(MapWaypointSnapshot snapshot) {
		if (api == null) {
			return false;
		}
		if (!hasAppliedSnapshot && snapshot == MapWaypointSnapshot.empty()) {
			return true;
		}
		int snapshotHash = snapshot.hashCode();
		if (hasAppliedSnapshot && snapshotHash == lastSnapshotHash) {
			return true;
		}
		applyingSnapshot = true;
		try {
			applyIncremental(snapshot);
			hasAppliedSnapshot = true;
			lastSnapshotHash = snapshotHash;
			return true;
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Failed to apply JourneyMap waypoint snapshot.", exception);
			return false;
		} finally {
			applyingSnapshot = false;
		}
	}

	private void applyIncremental(MapWaypointSnapshot snapshot) {
		Map<String, SyncedMapWaypoint> desired = new LinkedHashMap<>();
		for (SyncedMapWaypoint waypoint : snapshot.waypoints()) {
			desired.put(JourneyMapWaypointCommandHelper.key(waypoint), waypoint);
		}

		List<? extends Waypoint> existing = api.getWaypoints(ModConstants.MOD_ID);
		for (Waypoint waypoint : existing) {
			String key = JourneyMapWaypointCommandHelper.key(waypoint);
			if (key == null) {
				continue;
			}
			SyncedMapWaypoint desiredWaypoint = desired.get(key);
			if (desiredWaypoint == null || !matches(waypoint, desiredWaypoint)) {
				api.removeWaypoint(ModConstants.MOD_ID, waypoint);
			} else {
				desired.remove(key);
			}
		}

		for (SyncedMapWaypoint waypoint : desired.values()) {
			api.addWaypoint(ModConstants.MOD_ID, createWaypoint(waypoint));
		}
	}

	void onWaypointEvent(WaypointEvent event) {
		if (applyingSnapshot || event.getContext() != WaypointEvent.Context.DELETED) {
			return;
		}
		String command = JourneyMapWaypointCommandHelper.buildHideCommand(event.getWaypoint());
		if (command != null) {
			sendCommand(command);
		}
	}

	private Waypoint createWaypoint(SyncedMapWaypoint synced) {
		Waypoint waypoint = WaypointFactory.createWaypoint(ModConstants.MOD_ID,
				new BlockPos(synced.x(), synced.y(), synced.z()), synced.name(), synced.worldId(), false);
		waypoint.setColor(color(synced.kind()));
		JourneyMapWaypointCommandHelper.tag(waypoint, synced.kind(), synced.name());
		return waypoint;
	}

	private boolean matches(Waypoint waypoint, SyncedMapWaypoint synced) {
		return waypoint.getX() == synced.x()
				&& waypoint.getY() == synced.y()
				&& waypoint.getZ() == synced.z()
				&& Objects.equals(waypoint.getName(), synced.name())
				&& Objects.equals(waypoint.getPrimaryDimension(), synced.worldId());
	}

	private int color(SyncedWaypointKind kind) {
		return kind == SyncedWaypointKind.WARP ? WARP_COLOR : HOME_COLOR;
	}

	private void sendCommand(String command) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			if (client.player == null || client.player.connection == null) {
				return;
			}
			client.player.connection.sendCommand(command);
		});
	}
}
