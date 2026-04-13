package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.LocationResolver;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.models.PlayerData;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public class PlayerHomeSource implements WaypointSource {
	private final PlayerData player;

	public PlayerHomeSource(PlayerData player) {
		this.player = player;
	}

	protected PlayerData player() {
		return player;
	}

	@Override
	public List<NamedLocation> getAll() {
		player.refreshHomeState();
		return player.getHomes();
	}

	@Override
	public Optional<NamedLocation> getByName(String name) {
		player.refreshHomeState();
		return player.getHome(LocationResolver.normalizeName(name));
	}

	@Override
	public void add(NamedLocation location) throws Exception {
		player.addHome(location);
	}

	@Override
	public void remove(NamedLocation location) throws Exception {
		player.deleteHome(location);
	}

	@Override
	public int getMaxLimit() {
		return CONFIG.getHome().getPlayerMaximum();
	}

	@Override
	public boolean isEnabled() {
		return CONFIG.getHome().isEnabled();
	}

	@Override
	public void setDefault(NamedLocation location) throws Exception {
		player.setDefaultHomeByUuid(location.getUuid());
	}

	@Override
	public boolean isDefault(NamedLocation location) {
		return location.getUuid().equals(player.getDefaultHomeUuid());
	}

	@Override
	public void onAdded(NamedLocation location) {
		if (player.getDefaultHomeUuid() == null && player.isEligibleDefaultHome(location)) {
			try {
				player.setDefaultHomeByUuid(location.getUuid());
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public void onRenamed(NamedLocation location, String oldName) {
		if (location.getUuid().equals(player.getDefaultHomeUuid())) {
			try {
				player.setDefaultHomeByUuid(location.getUuid());
			} catch (Exception ignored) {
			}
		}
	}

	boolean hasTemporaryHome() {
		player.refreshHomeState();
		return player.hasTemporaryHome();
	}

	boolean canBeDefault(NamedLocation location) {
		return player.isEligibleDefaultHome(location);
	}
}

