package org.AndrewElizabeth.teleportcommandsfabric.core.record;

import org.AndrewElizabeth.teleportcommandsfabric.storage.record.PlayerRecordedLocationManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRecordedLocationSource implements AsyncRecordedLocationSource {
	private final PlayerRecordedLocationManager recordManager;

	public PlayerRecordedLocationSource(PlayerRecordedLocationManager recordManager) {
		this.recordManager = Objects.requireNonNull(recordManager, "recordManager");
	}

	@Override
	public CompletableFuture<Optional<RecordedLocationView>> getDeathLocation(UUID playerUuid) {
		return CompletableFuture.completedFuture(RecordedLocationSnapshot.optional(recordManager.getDeathLocation(playerUuid)));
	}

	@Override
	public CompletableFuture<Optional<RecordedLocationView>> getPreviousTeleportLocation(UUID playerUuid) {
		return CompletableFuture.completedFuture(RecordedLocationSnapshot.optional(recordManager.getPreviousTeleportLocation(playerUuid)));
	}

	@Override
	public CompletableFuture<Void> recordDeathLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
		recordManager.recordDeathLocation(playerUuid, pos, dimension);
		WaypointMapSyncEvents.markPlayerDirty(playerUuid);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> recordDeathLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension, float yRot, float xRot) {
		recordManager.recordDeathLocation(playerUuid, pos, dimension, yRot, xRot);
		WaypointMapSyncEvents.markPlayerDirty(playerUuid);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> recordPreviousTeleportLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
		recordManager.recordPreviousTeleportLocation(playerUuid, pos, dimension);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> recordPreviousTeleportLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension, float yRot, float xRot) {
		recordManager.recordPreviousTeleportLocation(playerUuid, pos, dimension, yRot, xRot);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> removeDeathLocation(UUID playerUuid) {
		recordManager.removeDeathLocation(playerUuid);
		WaypointMapSyncEvents.markPlayerDirty(playerUuid);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> removePreviousTeleportLocation(UUID playerUuid) {
		recordManager.removePreviousTeleportLocation(playerUuid);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> removeRecord(UUID playerUuid) {
		recordManager.removeRecord(playerUuid);
		return CompletableFuture.completedFuture(null);
	}
}
