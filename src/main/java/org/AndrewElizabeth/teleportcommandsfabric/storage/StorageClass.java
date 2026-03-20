package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.unmodifiableList;

public class StorageClass {
	private final int version = Constants.STORAGE_VERSION;
	private final ArrayList<NamedLocation> Warps = new ArrayList<>();
	private final ArrayList<Player> Players = new ArrayList<>();
	private transient final java.util.Map<String, Player> playerCache = new java.util.concurrent.ConcurrentHashMap<>();

	public void cleanup() throws Exception {
		boolean changed = false;
		playerCache.clear();
		
		for (Iterator<Player> iterator = Players.iterator(); iterator.hasNext();) {
			Player player = iterator.next();

			if (player == null) {
				iterator.remove();
				continue;
			}

			String uuid = player.getUUID();
			if (uuid == null || uuid.isBlank()) {
				iterator.remove();
				continue;
			}

			if (ConfigManager.CONFIG.home.isDeleteInvalid()) {
				List<NamedLocation> homesSnapshot = new ArrayList<>(player.getHomes());
				for (NamedLocation home : homesSnapshot) {
					changed |= home.ensureUuid();
					if (home.getWorld().isEmpty()) {
						player.deleteHomeNoSave(home);
						changed = true;
					}
				}

				changed |= player.ensureDefaultHomeUuid();
			}

			if (!ConfigManager.CONFIG.home.isDeleteInvalid()) {
				for (NamedLocation home : player.getHomes()) {
					changed |= home.ensureUuid();
				}
				changed |= player.ensureDefaultHomeUuid();
			}
			changed |= player.ensureHiddenWarpUuids();

			if (player.isEmpty()) {
				iterator.remove();
				changed = true;
			} else {
				playerCache.put(uuid, player);
			}
		}

		for (NamedLocation warp : Warps) {
			changed |= warp.ensureUuid();
		}
		if (ConfigManager.CONFIG.warp.isDeleteInvalid()) {
			boolean removed = Warps.removeIf(warp -> warp.getWorld().isEmpty());
			changed |= removed;
		}

		Set<UUID> existingWarpUuids = new HashSet<>();
		for (NamedLocation warp : Warps) {
			existingWarpUuids.add(warp.getUuid());
		}
		for (Player player : Players) {
			changed |= player.cleanupHiddenWarpUuids(existingWarpUuids);
		}

		if (changed) {
			StorageManager.StorageSaver();
		}
	}

	public int getVersion() {
		return version;
	}

	public List<NamedLocation> getWarps() {
		return unmodifiableList(Warps);
	}

	public Optional<NamedLocation> getWarp(String name) {
		return Warps.stream()
				.filter(warp -> Objects.equals(warp.getName(), name))
				.findFirst();
	}

	public Optional<NamedLocation> getWarpByUuid(UUID uuid) {
		return Warps.stream()
				.filter(warp -> Objects.equals(warp.getUuid(), uuid))
				.findFirst();
	}

	public Optional<Player> getPlayer(String uuid) {
		return Optional.ofNullable(playerCache.get(uuid));
	}

	public boolean addWarp(NamedLocation warp) throws Exception {
		if (getWarp(warp.getName()).isPresent()) {
			return true;

		} else {
			Warps.add(warp);
			StorageManager.StorageSaver();
			return false;
		}
	}

	public Player addPlayer(String uuid) {
		return playerCache.computeIfAbsent(uuid, k -> {
			Player player = new Player(k);
			Players.add(player);
			return player;
		});
	}

	public void removeWarp(NamedLocation warp) throws Exception {
		Warps.remove(warp);
		StorageManager.StorageSaver();
	}
}