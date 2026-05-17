package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class GlobalProfile {
	private final LinkedHashMap<UUID, NamedLocation> warps = new LinkedHashMap<>();
	private final HashMap<String, UUID> warpNameIndex = new HashMap<>();

	public List<NamedLocation> getWarps() {
		return List.copyOf(warps.values());
	}

	public int getWarpCount() {
		return warps.size();
	}

	public GlobalProfile snapshotForSave() {
		GlobalProfile snapshot = new GlobalProfile();
		for (NamedLocation warp : warps.values()) {
			snapshot.addWarp(NamedLocation.copyOf(warp));
		}
		return snapshot;
	}

	public Optional<NamedLocation> getWarpByName(String name) {
		UUID uuid = warpNameIndex.get(normalizeName(name));
		return getWarp(uuid);
	}

	public Optional<NamedLocation> getWarp(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}

		NamedLocation warp = warps.get(uuid);
		if (warp == null || warp.isExpired()) {
			return Optional.empty();
		}
		return Optional.of(warp);
	}

	public boolean addWarp(NamedLocation warp) {
		if (warp == null || warps.containsKey(warp.getUuid()) || warpNameIndex.containsKey(normalizeName(warp.getName()))) {
			return false;
		}

		assignSequenceIfNeeded(warp);
		warps.put(warp.getUuid(), warp);
		warpNameIndex.put(normalizeName(warp.getName()), warp.getUuid());
		return true;
	}

	public void deleteWarp(NamedLocation warp) {
		if (warp == null) {
			return;
		}

		NamedLocation removed = warps.remove(warp.getUuid());
		if (removed != null) {
			warpNameIndex.remove(normalizeName(removed.getName()));
		}
	}

	public boolean updateWarp(NamedLocation newWarp) {
		removeExpiredWarps();
		if (newWarp == null || !warps.containsKey(newWarp.getUuid())) {
			return false;
		}

		String normalizedNewName = normalizeName(newWarp.getName());
		UUID existingUuid = warpNameIndex.get(normalizedNewName);
		if (existingUuid != null && !existingUuid.equals(newWarp.getUuid())) {
			return false;
		}

		NamedLocation oldWarp = warps.get(newWarp.getUuid());
		if (newWarp.getSequence() < 0) {
			newWarp.setSequence(oldWarp.getSequence());
		}

		if (!oldWarp.getName().equalsIgnoreCase(newWarp.getName())) {
			warpNameIndex.remove(normalizeName(oldWarp.getName()));
			warpNameIndex.put(normalizedNewName, newWarp.getUuid());
		}

		warps.put(newWarp.getUuid(), newWarp);
		return true;
	}

	public boolean removeExpiredWarps() {
		boolean changed = false;
		for (var iterator = warps.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<UUID, NamedLocation> entry = iterator.next();
			NamedLocation warp = entry.getValue();
			if (!warp.isExpired()) {
				continue;
			}

			warpNameIndex.remove(normalizeName(warp.getName()));
			iterator.remove();
			changed = true;
		}
		return changed;
	}

	public boolean removeInvalidWarps(Predicate<NamedLocation> isInvalidWarp) {
		boolean changed = false;
		for (var iterator = warps.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<UUID, NamedLocation> entry = iterator.next();
			NamedLocation warp = entry.getValue();
			if (!isInvalidWarp.test(warp)) {
				continue;
			}

			warpNameIndex.remove(normalizeName(warp.getName()));
			iterator.remove();
			changed = true;
		}
		return changed;
	}

	public boolean refreshWarpState() {
		return removeExpiredWarps();
	}

	public boolean isEmpty() {
		return warps.isEmpty();
	}

	public void rebuildWarpNameIndex() {
		warpNameIndex.clear();
		for (NamedLocation warp : warps.values()) {
			if (!warp.isExpired()) {
				warpNameIndex.put(normalizeName(warp.getName()), warp.getUuid());
			}
		}
	}

	private static String normalizeName(String name) {
		return name == null ? "" : name.toLowerCase(Locale.ROOT);
	}

	private void assignSequenceIfNeeded(NamedLocation warp) {
		if (warp.getSequence() >= 0) {
			return;
		}
		warp.setSequence(nextSequence());
	}

	private int nextSequence() {
		return warps.values().stream()
				.mapToInt(NamedLocation::getSequence)
				.max()
				.orElse(-1) + 1;
	}
}
