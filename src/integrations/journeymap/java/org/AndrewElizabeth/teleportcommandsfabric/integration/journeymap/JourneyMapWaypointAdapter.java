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
import journeymap.api.v2.common.waypoint.WaypointGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class JourneyMapWaypointAdapter implements MapWaypointAdapter {
	private static final int WARP_COLOR = 0x55CCFF;
	private static final int HOME_COLOR = 0x55FF88;
	private static final String WARP_GROUP_NAME = "TPC Warps";
	private static final String HOME_GROUP_NAME = "TPC Homes";
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
		if (!hasAppliedSnapshot && snapshot.equals(MapWaypointSnapshot.empty())) {
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
		boolean persistent = snapshot.persistWaypointSets();
		Map<SyncedWaypointKind, WaypointGroup> groups = groups(persistent);

		List<? extends Waypoint> existing = api.getWaypoints(ModConstants.MOD_ID);
		for (Waypoint waypoint : existing) {
			String key = JourneyMapWaypointCommandHelper.key(waypoint);
			if (key == null) {
				continue;
			}
			SyncedMapWaypoint desiredWaypoint = desired.get(key);
			if (desiredWaypoint == null
					|| !matches(waypoint, desiredWaypoint, groups.get(desiredWaypoint.kind()), persistent)) {
				api.removeWaypoint(ModConstants.MOD_ID, waypoint);
			} else {
				desired.remove(key);
			}
		}

		for (SyncedMapWaypoint waypoint : desired.values()) {
			api.addWaypoint(ModConstants.MOD_ID,
					createWaypoint(waypoint, groups.get(waypoint.kind()), persistent));
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

	private Waypoint createWaypoint(SyncedMapWaypoint synced, WaypointGroup group, boolean persistent) {
		Waypoint waypoint = WaypointFactory.createWaypoint(ModConstants.MOD_ID,
				new BlockPos(synced.x(), synced.y(), synced.z()), synced.name(), synced.worldId(), persistent);
		waypoint.setColor(color(synced.kind()));
		JourneyMapWaypointCommandHelper.tag(waypoint, synced.kind(), synced.name());
		group.addWaypoint(waypoint);
		return waypoint;
	}

	private boolean matches(Waypoint waypoint, SyncedMapWaypoint synced, WaypointGroup group, boolean persistent) {
		return waypoint.getX() == synced.x()
				&& waypoint.getY() == synced.y()
				&& waypoint.getZ() == synced.z()
				&& Objects.equals(waypoint.getName(), synced.name())
				&& Objects.equals(waypoint.getPrimaryDimension(), synced.worldId())
				&& Objects.equals(waypoint.getGroupId(), group.getGuid())
				&& waypoint.isPersistent() == persistent;
	}

	private Map<SyncedWaypointKind, WaypointGroup> groups(boolean persistent) {
		Map<SyncedWaypointKind, WaypointGroup> groups = new LinkedHashMap<>();
		groups.put(SyncedWaypointKind.WARP, group(WARP_GROUP_NAME, persistent));
		groups.put(SyncedWaypointKind.HOME, group(HOME_GROUP_NAME, persistent));
		return groups;
	}

	private WaypointGroup group(String name, boolean persistent) {
		WaypointGroup group = api.getWaypointGroupByName(ModConstants.MOD_ID, name);
		if (group == null) {
			group = WaypointFactory.createWaypointGroup(ModConstants.MOD_ID, name);
			api.addWaypointGroup(group);
		}
		group.setPersistent(persistent);
		return group;
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
