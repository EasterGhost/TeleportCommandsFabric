package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import java.util.List;
import java.util.Optional;

public interface WaypointSource {

	List<NamedLocation> getAll();

	Optional<NamedLocation> getByName(String name);

	void add(NamedLocation location) throws Exception;

	void remove(NamedLocation location) throws Exception;

	int getMaxLimit();

	boolean isEnabled();

	default void setDefault(NamedLocation location) throws Exception {
	}

	default boolean isDefault(NamedLocation location) {
		return false;
	}

	default void onAdded(NamedLocation location) {
	}

	default void onRemoved(NamedLocation location) {
	}

	default void onRenamed(NamedLocation location, String oldName) {
	}
}
