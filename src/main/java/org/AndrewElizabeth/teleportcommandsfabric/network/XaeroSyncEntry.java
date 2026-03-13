package org.AndrewElizabeth.teleportcommandsfabric.network;

import java.util.UUID;

public record XaeroSyncEntry(UUID uuid, String name, String worldId, int x, int y, int z) {
}
