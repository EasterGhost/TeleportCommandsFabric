package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared;

import java.util.Objects;
import java.util.UUID;

public record SharedHomeKey(UUID ownerUuid, UUID homeUuid) {
	public SharedHomeKey {
		Objects.requireNonNull(ownerUuid, "ownerUuid");
		Objects.requireNonNull(homeUuid, "homeUuid");
	}
}
