package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Player {
	private final String UUID;
	private UUID DefaultHomeUuid;
	private final ArrayList<NamedLocation> Homes = new ArrayList<>();
	private HashSet<UUID> HiddenWarpUuids = new HashSet<>();

	public Player(String uuid) {
		this.UUID = uuid;
	}

	public String getUUID() {
		return UUID;
	}

	public UUID getDefaultHomeUuid() {
		return DefaultHomeUuid;
	}

	public String getDefaultHome() {
		return getHomeByUuid(DefaultHomeUuid)
				.map(NamedLocation::getName)
				.orElse("");
	}

	public List<NamedLocation> getHomes() {
		return Collections.unmodifiableList(Homes);
	}

	public Set<UUID> getHiddenWarpUuids() {
		return Set.copyOf(getOrCreateHiddenWarpUuids());
	}

	public Optional<NamedLocation> getHome(String name) {
		return Homes.stream()
				.filter(home -> Objects.equals(home.getName(), name))
				.findFirst();
	}

	public Optional<NamedLocation> getHomeByUuid(UUID uuid) {
		return Homes.stream()
				.filter(home -> Objects.equals(home.getUuid(), uuid))
				.findFirst();
	}

	public boolean isWarpHidden(UUID warpUuid) {
		return getOrCreateHiddenWarpUuids().contains(warpUuid);
	}

	public void setDefaultHome(String defaultHome) throws Exception {
		setDefaultHomeByNameNoSave(defaultHome);
		StorageManager.markDirty();
	}

	public void setDefaultHomeByNameNoSave(String defaultHome) {
		if (defaultHome == null || defaultHome.isBlank()) {
			this.DefaultHomeUuid = null;
			return;
		}
		this.DefaultHomeUuid = getHome(defaultHome)
				.map(NamedLocation::getUuid)
				.orElse(null);
	}

	public void setDefaultHomeNoSave(String defaultHome) {
		setDefaultHomeByNameNoSave(defaultHome);
	}

	public void setDefaultHomeByUuid(UUID defaultHomeUuid) throws Exception {
		setDefaultHomeByUuidNoSave(defaultHomeUuid);
		StorageManager.markDirty();
	}

	public void setDefaultHomeByUuidNoSave(UUID defaultHomeUuid) {
		this.DefaultHomeUuid = defaultHomeUuid;
	}

	public boolean addHome(NamedLocation home) throws Exception {
		if (getHome(home.getName()).isPresent()) {
			return true;

		} else {
			Homes.add(home);
			StorageManager.markDirty();
			return false;
		}
	}

	public void deleteHome(NamedLocation home) throws Exception {
		deleteHomeNoSave(home);
		StorageManager.markDirty();
	}

	public void deleteHomeNoSave(NamedLocation home) {
		Homes.removeIf(existing -> Objects.equals(existing.getUuid(), home.getUuid()));
		if (Objects.equals(DefaultHomeUuid, home.getUuid())) {
			DefaultHomeUuid = null;
		}
	}

	public void hideWarp(UUID warpUuid) throws Exception {
		hideWarpNoSave(warpUuid);
		StorageManager.markDirty();
	}

	public void hideWarpNoSave(UUID warpUuid) {
		if (warpUuid != null) {
			getOrCreateHiddenWarpUuids().add(warpUuid);
		}
	}

	public void showWarp(UUID warpUuid) throws Exception {
		showWarpNoSave(warpUuid);
		StorageManager.markDirty();
	}

	public void showWarpNoSave(UUID warpUuid) {
		if (warpUuid != null) {
			getOrCreateHiddenWarpUuids().remove(warpUuid);
		}
	}

	public boolean cleanupHiddenWarpUuids(Set<UUID> existingWarpUuids) {
		return getOrCreateHiddenWarpUuids().removeIf(uuid -> !existingWarpUuids.contains(uuid));
	}

	public boolean ensureDefaultHomeUuid() {
		if (DefaultHomeUuid == null) {
			return false;
		}
		if (getHomeByUuid(DefaultHomeUuid).isPresent()) {
			return false;
		}
		DefaultHomeUuid = null;
		return true;
	}

	public boolean ensureHiddenWarpUuids() {
		if (HiddenWarpUuids != null) {
			return false;
		}
		HiddenWarpUuids = new HashSet<>();
		return true;
	}

	public boolean removeExpiredHomes() {
		return Homes.removeIf(NamedLocation::isExpired);
	}

	public boolean isEmpty() {
		return Homes.isEmpty()
				&& DefaultHomeUuid == null
				&& getOrCreateHiddenWarpUuids().isEmpty();
	}

	private HashSet<UUID> getOrCreateHiddenWarpUuids() {
		if (HiddenWarpUuids == null) {
			HiddenWarpUuids = new HashSet<>();
		}
		return HiddenWarpUuids;
	}
}
