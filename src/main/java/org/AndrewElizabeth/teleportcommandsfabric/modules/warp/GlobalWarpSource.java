package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public class GlobalWarpSource implements WaypointSource {

	@Override
	public List<NamedLocation> getAll() {
		return StorageManager.STORAGE.getWarps();
	}

	@Override
	public Optional<NamedLocation> getByName(String name) {
		return StorageManager.STORAGE.getWarp(name);
	}

	@Override
	public void add(NamedLocation location) throws Exception {
		StorageManager.STORAGE.addWarp(location);
	}

	@Override
	public void remove(NamedLocation location) throws Exception {
		StorageManager.STORAGE.removeWarp(location);
	}

	@Override
	public int getMaxLimit() {
		return CONFIG.getWarp().getMaximum();
	}

	@Override
	public boolean isEnabled() {
		return CONFIG.getWarp().isEnabled();
	}
}

