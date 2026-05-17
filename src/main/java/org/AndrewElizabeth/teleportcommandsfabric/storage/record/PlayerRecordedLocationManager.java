package org.AndrewElizabeth.teleportcommandsfabric.storage.record;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRecordedLocationManager {
	private final PlayerRecordedLocationIO io;
	private final Map<UUID, PlayerRecordedLocations> records = new ConcurrentHashMap<>();

	public PlayerRecordedLocationManager() {
		this(new PlayerRecordedLocationIO());
	}

	public PlayerRecordedLocationManager(PlayerRecordedLocationIO io) {
		this.io = io;
	}

	public Optional<PlayerRecordedLocations> getRecord(UUID playerUuid) {
		return Optional.ofNullable(records.get(playerUuid));
	}

	public Optional<RecordedLocation> getDeathLocation(UUID playerUuid) {
		return getRecord(playerUuid).flatMap(PlayerRecordedLocations::getDeathLocation);
	}

	public Optional<RecordedLocation> getPreviousTeleportLocation(UUID playerUuid) {
		return getRecord(playerUuid).flatMap(PlayerRecordedLocations::getPreviousTeleportLocation);
	}

	public void recordDeathLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
		recordDeathLocation(playerUuid, new RecordedLocation(pos, dimension));
	}

	public void recordDeathLocation(UUID playerUuid, RecordedLocation location) {
		records.compute(playerUuid, (ignored, existing) -> {
			PlayerRecordedLocations record = existing == null ? new PlayerRecordedLocations() : existing;
			record.setDeathLocation(location);
			return record;
		});
	}

	public void recordPreviousTeleportLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
		recordPreviousTeleportLocation(playerUuid, new RecordedLocation(pos, dimension));
	}

	public void recordPreviousTeleportLocation(UUID playerUuid, RecordedLocation location) {
		records.compute(playerUuid, (ignored, existing) -> {
			PlayerRecordedLocations record = existing == null ? new PlayerRecordedLocations() : existing;
			record.setPreviousTeleportLocation(location);
			return record;
		});
	}

	public void removeDeathLocation(UUID playerUuid) {
		records.computeIfPresent(playerUuid, (ignored, record) -> {
			record.clearDeathLocation();
			return record.isEmpty() ? null : record;
		});
	}

	public void removePreviousTeleportLocation(UUID playerUuid) {
		records.computeIfPresent(playerUuid, (ignored, record) -> {
			record.clearPreviousTeleportLocation();
			return record.isEmpty() ? null : record;
		});
	}

	public void removeRecord(UUID playerUuid) {
		records.remove(playerUuid);
	}

	public void clear() {
		records.clear();
	}

	public void loadRecords() {
		try {
			records.clear();
			records.putAll(io.loadRecords());
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load recorded locations", exception);
		}
	}

	public void saveRecords() {
		try {
			io.saveRecords(records);
		} catch (IOException exception) {
			ModConstants.LOGGER.error("Failed to save recorded locations", exception);
		}
	}
}
