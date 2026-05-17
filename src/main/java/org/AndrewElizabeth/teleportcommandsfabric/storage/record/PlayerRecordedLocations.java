package org.AndrewElizabeth.teleportcommandsfabric.storage.record;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocation;

import java.util.Optional;

public class PlayerRecordedLocations {
	private RecordedLocation deathLocation;
	private RecordedLocation previousTeleportLocation;

	public Optional<RecordedLocation> getDeathLocation() {
		return Optional.ofNullable(deathLocation);
	}

	public void setDeathLocation(RecordedLocation deathLocation) {
		this.deathLocation = deathLocation;
	}

	public Optional<RecordedLocation> getPreviousTeleportLocation() {
		return Optional.ofNullable(previousTeleportLocation);
	}

	public void setPreviousTeleportLocation(RecordedLocation previousTeleportLocation) {
		this.previousTeleportLocation = previousTeleportLocation;
	}

	public void clearDeathLocation() {
		this.deathLocation = null;
	}

	public void clearPreviousTeleportLocation() {
		this.previousTeleportLocation = null;
	}

	public boolean isEmpty() {
		return deathLocation == null && previousTeleportLocation == null;
	}
}
