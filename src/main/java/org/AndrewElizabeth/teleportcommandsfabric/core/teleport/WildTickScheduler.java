package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

final class WildTickScheduler<K> {
	private final Map<K, Long> scheduledTicks = new HashMap<>();
	private final NavigableMap<Long, LinkedHashSet<K>> buckets = new TreeMap<>();

	void schedule(K key, long tick) {
		K safeKey = Objects.requireNonNull(key, "key");
		Long previousTick = scheduledTicks.put(safeKey, tick);
		if (previousTick != null) {
			removeFromBucket(safeKey, previousTick);
		}
		buckets.computeIfAbsent(tick, ignored -> new LinkedHashSet<>()).add(safeKey);
	}

	void cancel(K key) {
		Long tick = scheduledTicks.remove(key);
		if (tick != null) {
			removeFromBucket(key, tick);
		}
	}

	List<K> drainDue(long currentTick) {
		if (buckets.isEmpty() || buckets.firstKey() > currentTick) {
			return List.of();
		}

		List<K> due = new ArrayList<>();
		while (!buckets.isEmpty() && buckets.firstKey() <= currentTick) {
			Map.Entry<Long, LinkedHashSet<K>> entry = buckets.pollFirstEntry();
			for (K key : entry.getValue()) {
				if (scheduledTicks.remove(key, entry.getKey())) {
					due.add(key);
				}
			}
		}
		return List.copyOf(due);
	}

	void clear() {
		scheduledTicks.clear();
		buckets.clear();
	}

	private void removeFromBucket(K key, long tick) {
		LinkedHashSet<K> bucket = buckets.get(tick);
		if (bucket == null) {
			return;
		}
		bucket.remove(key);
		if (bucket.isEmpty()) {
			buckets.remove(tick);
		}
	}
}
