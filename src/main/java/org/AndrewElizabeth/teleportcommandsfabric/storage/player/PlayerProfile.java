package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class PlayerProfile {
	private final UUID playerUuid;
	private UUID defaultHomeUuid;
	private final LinkedHashMap<UUID, NamedLocation> homes = new LinkedHashMap<>();
	private final HashMap<String, UUID> homeNameIndex = new HashMap<>();
	private final HashSet<UUID> hiddenWarpUuids = new HashSet<>();

	public PlayerProfile(UUID uuid) {
		this.playerUuid = uuid;
	}

	public UUID getPlayerUuid() {
		return playerUuid;
	}

	public UUID getDefaultHomeUuid() {
		return defaultHomeUuid;
	}

	public String getDefaultHomeName() {
		return getDefaultHomeLocation().map(NamedLocation::getName).orElse("");
	}

	public List<NamedLocation> getHomes() {
		return List.copyOf(homes.values());
	}

	public int getHomeCount() {
		return homes.size();
	}

	public Set<UUID> getHiddenWarpUuids() {
		return Set.copyOf(hiddenWarpUuids);
	}

	public PlayerProfile snapshotForSave() {
		PlayerProfile snapshot = new PlayerProfile(playerUuid);
		for (NamedLocation home : homes.values()) {
			snapshot.addHome(NamedLocation.copyOf(home));
		}
		snapshot.defaultHomeUuid = defaultHomeUuid;
		snapshot.hiddenWarpUuids.addAll(hiddenWarpUuids);
		return snapshot;
	}

	public Optional<NamedLocation> getHomeByName(String name) {
		UUID uuid = homeNameIndex.get(normalizeName(name));
		return getHome(uuid);
	}

	public Optional<NamedLocation> getHome(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}

		NamedLocation home = homes.get(uuid);
		if (home == null || home.isExpired()) {
			return Optional.empty();
		}
		return Optional.of(home);
	}

	public Optional<NamedLocation> getDefaultHomeLocation() {
		return getHome(defaultHomeUuid).filter(this::isEligibleDefaultHome);
	}

	public Optional<NamedLocation> getTemporaryHomeLocation() {
		return homes.values().stream()
				.filter(home -> !home.isExpired())
				.filter(NamedLocation::isTemporary)
				.findFirst();
	}

	public boolean hasTemporaryHome() {
		return getTemporaryHomeLocation().isPresent();
	}

	public boolean isWarpHidden(UUID warpUuid) {
		return hiddenWarpUuids.contains(warpUuid);
	}

	public boolean setDefaultHomeByName(String defaultHome) {
		Optional<NamedLocation> home = getHomeByName(defaultHome).filter(this::isEligibleDefaultHome);

		if (home.isEmpty()) {
			return false;
		}

		this.defaultHomeUuid = home.get().getUuid();
		return true;
	}

	public boolean setDefaultHome(UUID defaultHomeUuid) {
		Optional<NamedLocation> home = getHome(defaultHomeUuid).filter(this::isEligibleDefaultHome);

		if (home.isEmpty()) {
			return false;
		}

		this.defaultHomeUuid = home.get().getUuid();
		return true;
	}

	public boolean addHome(NamedLocation home) {
		if (home == null || homes.containsKey(home.getUuid()) || homeNameIndex.containsKey(normalizeName(home.getName()))) {
			return false;
		}

		assignSequenceIfNeeded(home);
		homes.put(home.getUuid(), home);
		homeNameIndex.put(normalizeName(home.getName()), home.getUuid());
		return true;
	}

	public void deleteHome(NamedLocation home) {
		if (home == null) {
			return;
		}

		NamedLocation removed = homes.remove(home.getUuid());
		if (removed != null) {
			homeNameIndex.remove(normalizeName(removed.getName()));
		}
		if (Objects.equals(defaultHomeUuid, home.getUuid())) {
			defaultHomeUuid = null;
		}
	}

	public boolean updateHome(NamedLocation newHome) {
		removeExpiredHomes();
		if (newHome == null || !homes.containsKey(newHome.getUuid())) {
			return false;
		}

		String normalizedNewName = normalizeName(newHome.getName());
		UUID existingUuid = homeNameIndex.get(normalizedNewName);
		if (existingUuid != null && !Objects.equals(existingUuid, newHome.getUuid())) {
			return false;
		}

		NamedLocation oldHome = homes.get(newHome.getUuid());
		if (newHome.getSequence() < 0) {
			newHome.setSequence(oldHome.getSequence());
		}

		if (!oldHome.getName().equalsIgnoreCase(newHome.getName())) {
			homeNameIndex.remove(normalizeName(oldHome.getName()));
			homeNameIndex.put(normalizedNewName, newHome.getUuid());
		}

		homes.put(newHome.getUuid(), newHome);
		return true;
	}

	public void hideWarp(UUID warpUuid) {
		if (warpUuid != null) {
			hiddenWarpUuids.add(warpUuid);
		}
	}

	public void showWarp(UUID warpUuid) {
		if (warpUuid != null) {
			hiddenWarpUuids.remove(warpUuid);
		}
	}

	public boolean cleanupHiddenWarpUuids(Set<UUID> existingWarpUuids) {
		return hiddenWarpUuids.removeIf(uuid -> !existingWarpUuids.contains(uuid));
	}

	public boolean ensureDefaultHomeUuid() {
		if (defaultHomeUuid == null) {
			return false;
		}
		if (getDefaultHomeLocation().isPresent()) {
			return false;
		}
		defaultHomeUuid = null;
		return true;
	}

	public boolean removeExpiredHomes() {
		boolean changed = false;
		for (var iterator = homes.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<UUID, NamedLocation> entry = iterator.next();
			NamedLocation home = entry.getValue();
			if (!home.isExpired()) {
				continue;
			}
			if (Objects.equals(defaultHomeUuid, home.getUuid())) {
				defaultHomeUuid = null;
			}
			homeNameIndex.remove(normalizeName(home.getName()));
			iterator.remove();
			changed = true;
		}
		return changed;
	}

	public boolean removeInvalidHomes(Predicate<NamedLocation> isInvalidHome) {
		boolean changed = false;
		for (var iterator = homes.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<UUID, NamedLocation> entry = iterator.next();
			NamedLocation home = entry.getValue();
			if (!isInvalidHome.test(home)) {
				continue;
			}
			if (Objects.equals(defaultHomeUuid, home.getUuid())) {
				defaultHomeUuid = null;
			}
			homeNameIndex.remove(normalizeName(home.getName()));
			iterator.remove();
			changed = true;
		}
		return changed;
	}

	public boolean refreshHomeState() {
		boolean changed = removeExpiredHomes();
		changed |= ensureDefaultHomeUuid();
		return changed;
	}

	public boolean isEmpty() {
		return homes.isEmpty() && defaultHomeUuid == null && hiddenWarpUuids.isEmpty();
	}

	public void rebuildHomeNameIndex() {
		homeNameIndex.clear();
		for (NamedLocation home : homes.values()) {
			if (!home.isExpired()) {
				homeNameIndex.put(normalizeName(home.getName()), home.getUuid());
			}
		}
	}

	public boolean isEligibleDefaultHome(NamedLocation home) {
		return home != null && !home.isTemporary() && !home.isExpired();
	}

	private static String normalizeName(String name) {
		return name == null ? "" : name.toLowerCase(Locale.ROOT);
	}

	public void clearDefaultHome() {
		this.defaultHomeUuid = null;
	}

	private void assignSequenceIfNeeded(NamedLocation home) {
		if (home.getSequence() >= 0) {
			return;
		}
		home.setSequence(nextSequence());
	}

	private int nextSequence() {
		return homes.values().stream().mapToInt(NamedLocation::getSequence).max().orElse(-1) + 1;
	}
}
