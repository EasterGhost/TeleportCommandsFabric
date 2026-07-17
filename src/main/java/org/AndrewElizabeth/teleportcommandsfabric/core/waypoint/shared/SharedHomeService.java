package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SharedHomeService {
	private final Map<SharedHomeKey, Publication> publications = new LinkedHashMap<>();
	private final Map<UUID, LinkedHashMap<SharedHomeKey, Subscription>> subscriptionsByPlayer = new LinkedHashMap<>();
	private final Map<UUID, Long> lastBroadcastTimeByPlayer = new LinkedHashMap<>();

	public PublishResult publishOrBroadcast(SharedHomeKey key, int maximum, long nowMillis, long cooldownMillis) {
		Publication existing = publications.get(key);
		if (existing == null && publicationCount(key.ownerUuid()) >= Math.max(1, maximum)) {
			return new PublishResult(PublishStatus.LIMIT_REACHED, 0L);
		}

		long remainingMillis = remainingCooldown(key.ownerUuid(), nowMillis, cooldownMillis);
		if (remainingMillis > 0L) {
			return new PublishResult(PublishStatus.COOLDOWN, remainingMillis);
		}

		boolean created = existing == null;
		if (created) {
			publications.put(key, new Publication());
		}
		lastBroadcastTimeByPlayer.put(key.ownerUuid(), nowMillis);
		return new PublishResult(created ? PublishStatus.PUBLISHED : PublishStatus.BROADCAST, 0L);
	}

	public SubscriptionStatus subscribe(UUID subscriberUuid, SharedHomeKey key) {
		Publication publication = publications.get(key);
		if (publication == null) {
			return SubscriptionStatus.NOT_FOUND;
		}
		if (key.ownerUuid().equals(subscriberUuid)) {
			return SubscriptionStatus.SELF;
		}

		LinkedHashMap<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.computeIfAbsent(
				subscriberUuid, ignored -> new LinkedHashMap<>());
		if (subscriptions.containsKey(key)) {
			return SubscriptionStatus.ALREADY_SUBSCRIBED;
		}
		subscriptions.put(key, new Subscription(true));
		publication.subscribers.add(subscriberUuid);
		return SubscriptionStatus.SUBSCRIBED;
	}

	public boolean unsubscribe(UUID subscriberUuid, SharedHomeKey key) {
		LinkedHashMap<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		if (subscriptions == null || subscriptions.remove(key) == null) {
			return false;
		}
		if (subscriptions.isEmpty()) {
			subscriptionsByPlayer.remove(subscriberUuid);
		}
		Publication publication = publications.get(key);
		if (publication != null) {
			publication.subscribers.remove(subscriberUuid);
		}
		return true;
	}

	public Set<UUID> withdraw(SharedHomeKey key) {
		Publication publication = publications.remove(key);
		if (publication == null) {
			return Set.of();
		}
		Set<UUID> affected = Set.copyOf(publication.subscribers);
		for (UUID subscriberUuid : affected) {
			LinkedHashMap<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
			if (subscriptions == null) {
				continue;
			}
			subscriptions.remove(key);
			if (subscriptions.isEmpty()) {
				subscriptionsByPlayer.remove(subscriberUuid);
			}
		}
		return affected;
	}

	public Set<UUID> removeMissingPublications(UUID ownerUuid, Collection<UUID> existingHomeUuids) {
		Set<UUID> valid = existingHomeUuids == null ? Set.of() : Set.copyOf(existingHomeUuids);
		List<SharedHomeKey> missing = publications.keySet().stream()
				.filter(key -> key.ownerUuid().equals(ownerUuid) && !valid.contains(key.homeUuid()))
				.toList();
		Set<UUID> affected = new LinkedHashSet<>();
		for (SharedHomeKey key : missing) {
			affected.addAll(withdraw(key));
		}
		return Set.copyOf(affected);
	}

	public boolean setMapVisible(UUID subscriberUuid, SharedHomeKey key, boolean visible) {
		LinkedHashMap<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		if (subscriptions == null || !subscriptions.containsKey(key) || !publications.containsKey(key)) {
			return false;
		}
		subscriptions.put(key, new Subscription(visible));
		return true;
	}

	public boolean isPublished(SharedHomeKey key) {
		return publications.containsKey(key);
	}

	public boolean isSubscribed(UUID subscriberUuid, SharedHomeKey key) {
		Map<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		return subscriptions != null && subscriptions.containsKey(key) && publications.containsKey(key);
	}

	public Set<UUID> publishedHomeUuids(UUID ownerUuid) {
		Set<UUID> result = new LinkedHashSet<>();
		for (SharedHomeKey key : publications.keySet()) {
			if (key.ownerUuid().equals(ownerUuid)) {
				result.add(key.homeUuid());
			}
		}
		return Set.copyOf(result);
	}

	public Set<UUID> subscribersForOwner(UUID ownerUuid) {
		Set<UUID> result = new LinkedHashSet<>();
		for (Map.Entry<SharedHomeKey, Publication> entry : publications.entrySet()) {
			if (entry.getKey().ownerUuid().equals(ownerUuid)) {
				result.addAll(entry.getValue().subscribers);
			}
		}
		return Set.copyOf(result);
	}

	public List<SubscriptionView> subscriptions(UUID subscriberUuid) {
		Map<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		if (subscriptions == null || subscriptions.isEmpty()) {
			return List.of();
		}
		List<SubscriptionView> result = new ArrayList<>(subscriptions.size());
		int sequence = 0;
		for (Map.Entry<SharedHomeKey, Subscription> entry : subscriptions.entrySet()) {
			if (publications.containsKey(entry.getKey())) {
				result.add(new SubscriptionView(entry.getKey(), entry.getValue().mapVisible, sequence++));
			}
		}
		return List.copyOf(result);
	}

	public void clear() {
		publications.clear();
		subscriptionsByPlayer.clear();
		lastBroadcastTimeByPlayer.clear();
	}

	private int publicationCount(UUID ownerUuid) {
		int count = 0;
		for (SharedHomeKey key : publications.keySet()) {
			if (key.ownerUuid().equals(ownerUuid)) {
				count++;
			}
		}
		return count;
	}

	private long remainingCooldown(UUID ownerUuid, long nowMillis, long cooldownMillis) {
		Long lastBroadcast = lastBroadcastTimeByPlayer.get(ownerUuid);
		if (lastBroadcast == null) {
			return 0L;
		}
		return Math.max(0L, Math.max(0L, cooldownMillis) - Math.max(0L, nowMillis - lastBroadcast));
	}

	public enum PublishStatus {
		PUBLISHED,
		BROADCAST,
		LIMIT_REACHED,
		COOLDOWN
	}

	public enum SubscriptionStatus {
		SUBSCRIBED,
		ALREADY_SUBSCRIBED,
		SELF,
		NOT_FOUND
	}

	public record PublishResult(PublishStatus status, long remainingCooldownMillis) {
	}

	public record SubscriptionView(SharedHomeKey key, boolean mapVisible, int sequence) {
	}

	private static final class Publication {
		private final Set<UUID> subscribers = new LinkedHashSet<>();
	}

	private record Subscription(boolean mapVisible) {
	}
}
