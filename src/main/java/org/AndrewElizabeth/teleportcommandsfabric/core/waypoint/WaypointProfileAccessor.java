package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;

import java.util.Optional;
import java.util.UUID;

public interface WaypointProfileAccessor {
	int getCount();

	Optional<NamedLocation> findByName(String name);

	Optional<NamedLocation> findByUuid(UUID uuid);

	boolean put(NamedLocation location);

	void remove(NamedLocation location);

	boolean isDefault(NamedLocation location);

	boolean hasDefault();

	boolean setDefault(NamedLocation location);

	boolean hasTemporary();
}
