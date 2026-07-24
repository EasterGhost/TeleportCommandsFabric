package org.AndrewElizabeth.teleportcommandsfabric.storage.record;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocation;

import java.util.Optional;

final class PlayerRecordedLocations {
	private RecordedLocation deathLocation;
	private RecordedLocation previousTeleportLocation;

	Optional<RecordedLocation> getDeathLocation() {
		return Optional.ofNullable(deathLocation);
	}

	void setDeathLocation(RecordedLocation deathLocation) {
		this.deathLocation = deathLocation;
	}

	Optional<RecordedLocation> getPreviousTeleportLocation() {
		return Optional.ofNullable(previousTeleportLocation);
	}

	void setPreviousTeleportLocation(RecordedLocation previousTeleportLocation) {
		this.previousTeleportLocation = previousTeleportLocation;
	}

	void clearDeathLocation() {
		this.deathLocation = null;
	}

	void clearPreviousTeleportLocation() {
		this.previousTeleportLocation = null;
	}

	boolean isEmpty() {
		return deathLocation == null && previousTeleportLocation == null;
	}
}
