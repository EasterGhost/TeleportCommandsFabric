package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeResolver;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeView;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class MapWaypointSnapshotBuilder {
	private MapWaypointSnapshotBuilder() {
	}

	static SyncedDeathLocation deathLocation(UUID playerUuid) {
		if (TeleportCommands.RECORDED_LOCATION_MANAGER == null) {
			return SyncedDeathLocation.NONE;
		}
		return TeleportCommands.RECORDED_LOCATION_MANAGER.getDeathLocation(playerUuid)
				.map(MapWaypointSnapshotBuilder::syncedDeathLocation)
				.orElse(SyncedDeathLocation.NONE);
	}

	static CompletableFuture<Result> build(UUID playerUuid, SyncedDeathLocation deathLocation, Options options) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(new Result(new MapWaypointSnapshot(List.of(),
					options.persistWaypointSets(), options.warpGroupName(), options.homeGroupName(),
					deathLocation), 0L));
		}

		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<PlayerWaypointData> playerFuture = playerManager.query(playerUuid,
				profile -> new PlayerWaypointData(profile.getHomes(), profile.getHiddenWarpUuids()));
		SharedHomeService sharedHomeService = TeleportCommands.SHARED_HOME_SERVICE;
		boolean sharedHomesEnabled = ConfigManager.query(config -> config.getHome().isEnabled());
		CompletableFuture<List<SharedHomeView>> sharedHomesFuture = sharedHomeService == null || !sharedHomesEnabled
				? CompletableFuture.completedFuture(List.of())
				: SharedHomeResolver.resolveSubscriptions(playerUuid, sharedHomeService, playerManager,
						TeleportCommands.SERVER);

		return warpsFuture.thenCombine(playerFuture, CombinedWaypointData::new)
				.thenCombine(sharedHomesFuture, (combined, sharedHomes) -> {
			List<SyncedMapWaypoint> waypoints = new ArrayList<>();
			addWarps(waypoints, combined.warps(), combined.playerData().hiddenWarpUuids());
			addHomes(waypoints, combined.playerData().homes());
			addSharedHomes(waypoints, sharedHomes);
			MapWaypointSnapshot snapshot = new MapWaypointSnapshot(waypoints, options.persistWaypointSets(), options.warpGroupName(), options.homeGroupName(),
					deathLocation);
			return new Result(snapshot, nextHomeExpiryMillis(combined.playerData().homes()));
		});
	}

	private static SyncedDeathLocation syncedDeathLocation(RecordedLocationView location) {
		return new SyncedDeathLocation(location.getDimensionId(), location.getBlockPos().getX(), location.getBlockPos().getY(), location.getBlockPos().getZ());
	}

	private static void addWarps(List<SyncedMapWaypoint> waypoints, List<NamedLocationView> warps,
			Set<UUID> hiddenWarpUuids) {
		for (NamedLocationView warp : warps) {
			if (!warp.isVisible() || hiddenWarpUuids.contains(warp.getUuid())) {
				continue;
			}
			addWaypoint(waypoints, SyncedWaypointKind.WARP, warp);
		}
	}

	private static void addHomes(List<SyncedMapWaypoint> waypoints, List<NamedLocationView> homes) {
		for (NamedLocationView home : homes) {
			if (!home.isVisible() || home.isExpired()) {
				continue;
			}
			addWaypoint(waypoints, SyncedWaypointKind.HOME, home);
		}
	}

	private static void addSharedHomes(List<SyncedMapWaypoint> waypoints, List<SharedHomeView> homes) {
		for (SharedHomeView home : homes) {
			if (!home.mapVisible()) {
				continue;
			}
			String displayName = home.ownerName() + " / " + home.getName();
			String commandTarget = home.key().ownerUuid() + " " + home.key().homeUuid();
			addWaypoint(waypoints, SyncedWaypointKind.SHARED_HOME, displayName, commandTarget, home);
		}
	}

	private static long nextHomeExpiryMillis(List<NamedLocationView> homes) {
		long nextExpiry = 0L;
		for (NamedLocationView home : homes) {
			if (!home.isVisible() || !home.isTemporary() || home.isExpired()) {
				continue;
			}
			long expiredTime = home.getExpiredTime();
			if (nextExpiry == 0L || expiredTime < nextExpiry) {
				nextExpiry = expiredTime;
			}
		}
		return nextExpiry;
	}

	private static void addWaypoint(List<SyncedMapWaypoint> waypoints, SyncedWaypointKind kind,
			NamedLocationView location) {
		addWaypoint(waypoints, kind, location.getName(), location.getName(), location);
	}

	private static void addWaypoint(List<SyncedMapWaypoint> waypoints, SyncedWaypointKind kind,
			String name, String commandTarget, NamedLocationView location) {
		String worldId = location.getDimensionId();
		if (worldId == null || worldId.isBlank()) {
			return;
		}
		waypoints.add(new SyncedMapWaypoint(kind, name, commandTarget, worldId,
				location.getX(), location.getY(), location.getZ()));
	}

	record Options(boolean persistWaypointSets, String warpGroupName, String homeGroupName) {
	}

	record Result(MapWaypointSnapshot snapshot, long nextHomeExpiryMillis) {
	}

	private record PlayerWaypointData(List<NamedLocationView> homes, Set<UUID> hiddenWarpUuids) {
	}

	private record CombinedWaypointData(List<NamedLocationView> warps, PlayerWaypointData playerData) {
	}
}
