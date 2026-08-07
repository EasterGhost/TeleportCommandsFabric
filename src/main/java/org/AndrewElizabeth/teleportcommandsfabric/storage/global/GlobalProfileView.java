package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlobalProfileView {
	List<NamedLocationView> getWarps();

	Optional<NamedLocationView> getWarpByName(String name);

	Optional<NamedLocationView> getWarp(UUID uuid);
}
