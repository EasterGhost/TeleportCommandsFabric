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
	private final Map<UUID, LinkedHashMap<UUID, Publication>> publicationsByOwner = new LinkedHashMap<>();
	private final Map<UUID, LinkedHashMap<SharedHomeKey, Subscription>> subscriptionsByPlayer = new LinkedHashMap<>();
	private final Map<UUID, Long> lastBroadcastTimeByPlayer = new LinkedHashMap<>();

	public PublishResult publishOrBroadcast(SharedHomeKey key, int maximum, long nowMillis, long cooldownMillis) {
		Map<UUID, Publication> ownerPublications = publicationsByOwner.get(key.ownerUuid());
		Publication existing = ownerPublications == null ? null : ownerPublications.get(key.homeUuid());
		if (existing == null && ownerPublications != null && ownerPublications.size() >= Math.max(1, maximum)) {
			return new PublishResult(PublishStatus.LIMIT_REACHED, 0L);
		}

		long remainingMillis = remainingCooldown(key.ownerUuid(), nowMillis, cooldownMillis);
		if (remainingMillis > 0L) {
			return new PublishResult(PublishStatus.COOLDOWN, remainingMillis);
		}

		boolean created = existing == null;
		if (created) {
			publicationsByOwner.computeIfAbsent(key.ownerUuid(), ignored -> new LinkedHashMap<>())
					.put(key.homeUuid(), new Publication());
		}
		lastBroadcastTimeByPlayer.put(key.ownerUuid(), nowMillis);
		return new PublishResult(created ? PublishStatus.PUBLISHED : PublishStatus.BROADCAST, 0L);
	}

	public SubscriptionStatus subscribe(UUID subscriberUuid, SharedHomeKey key) {
		Publication publication = publication(key);
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
		Publication publication = publication(key);
		if (publication != null) {
			publication.subscribers.remove(subscriberUuid);
		}
		return true;
	}

	public Set<UUID> withdraw(SharedHomeKey key) {
		LinkedHashMap<UUID, Publication> ownerPublications = publicationsByOwner.get(key.ownerUuid());
		Publication publication = ownerPublications == null ? null : ownerPublications.remove(key.homeUuid());
		if (publication == null) {
			return Set.of();
		}
		if (ownerPublications.isEmpty()) {
			publicationsByOwner.remove(key.ownerUuid());
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
		Map<UUID, Publication> ownerPublications = publicationsByOwner.get(ownerUuid);
		if (ownerPublications == null || ownerPublications.isEmpty()) {
			return Set.of();
		}
		Set<UUID> valid = existingHomeUuids == null ? Set.of() : Set.copyOf(existingHomeUuids);
		List<SharedHomeKey> missing = ownerPublications.keySet().stream()
				.filter(homeUuid -> !valid.contains(homeUuid))
				.map(homeUuid -> new SharedHomeKey(ownerUuid, homeUuid))
				.toList();
		Set<UUID> affected = new LinkedHashSet<>();
		for (SharedHomeKey key : missing) {
			affected.addAll(withdraw(key));
		}
		return Set.copyOf(affected);
	}

	public boolean setMapVisible(UUID subscriberUuid, SharedHomeKey key, boolean visible) {
		LinkedHashMap<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		if (subscriptions == null || !subscriptions.containsKey(key) || !isPublished(key)) {
			return false;
		}
		subscriptions.put(key, new Subscription(visible));
		return true;
	}

	public boolean isPublished(SharedHomeKey key) {
		return publication(key) != null;
	}

	public boolean hasPublications(UUID ownerUuid) {
		return publicationsByOwner.containsKey(ownerUuid);
	}

	public boolean isSubscribed(UUID subscriberUuid, SharedHomeKey key) {
		Map<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		return subscriptions != null && subscriptions.containsKey(key) && isPublished(key);
	}

	public Set<UUID> publishedHomeUuids(UUID ownerUuid) {
		Map<UUID, Publication> ownerPublications = publicationsByOwner.get(ownerUuid);
		return ownerPublications == null ? Set.of() : Set.copyOf(ownerPublications.keySet());
	}

	public Set<UUID> subscribersForOwner(UUID ownerUuid) {
		Map<UUID, Publication> ownerPublications = publicationsByOwner.get(ownerUuid);
		if (ownerPublications == null) {
			return Set.of();
		}
		Set<UUID> result = new LinkedHashSet<>();
		for (Publication publication : ownerPublications.values()) {
			result.addAll(publication.subscribers);
		}
		return Set.copyOf(result);
	}

	List<SubscriptionView> subscriptions(UUID subscriberUuid) {
		Map<SharedHomeKey, Subscription> subscriptions = subscriptionsByPlayer.get(subscriberUuid);
		if (subscriptions == null || subscriptions.isEmpty()) {
			return List.of();
		}
		List<SubscriptionView> result = new ArrayList<>(subscriptions.size());
		int sequence = 0;
		for (Map.Entry<SharedHomeKey, Subscription> entry : subscriptions.entrySet()) {
			if (isPublished(entry.getKey())) {
				result.add(new SubscriptionView(entry.getKey(), entry.getValue().mapVisible, sequence++));
			}
		}
		return List.copyOf(result);
	}

	public void clear() {
		publicationsByOwner.clear();
		subscriptionsByPlayer.clear();
		lastBroadcastTimeByPlayer.clear();
	}

	private Publication publication(SharedHomeKey key) {
		Map<UUID, Publication> ownerPublications = publicationsByOwner.get(key.ownerUuid());
		return ownerPublications == null ? null : ownerPublications.get(key.homeUuid());
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

	record SubscriptionView(SharedHomeKey key, boolean mapVisible, int sequence) {
	}

	private static final class Publication {
		private final Set<UUID> subscribers = new LinkedHashSet<>();
	}

	private record Subscription(boolean mapVisible) {
	}
}
