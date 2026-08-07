package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedHomeServiceTest {
	private static final long COOLDOWN_MILLIS = 60_000L;

	@Test
	void publicationLimitOnlyBlocksNewHomes() {
		SharedHomeService service = new SharedHomeService();
		UUID owner = UUID.randomUUID();
		SharedHomeKey first = key(owner);
		SharedHomeKey second = key(owner);

		assertEquals(SharedHomeService.PublishStatus.PUBLISHED,
				service.publishOrBroadcast(first, 1, 1_000L, COOLDOWN_MILLIS).status());
		assertEquals(SharedHomeService.PublishStatus.BROADCAST,
				service.publishOrBroadcast(first, 1, 61_000L, COOLDOWN_MILLIS).status());
		assertEquals(SharedHomeService.PublishStatus.LIMIT_REACHED,
				service.publishOrBroadcast(second, 1, 121_000L, COOLDOWN_MILLIS).status());
		assertTrue(service.hasPublications(owner));
		assertEquals(Set.of(first.homeUuid()), service.publishedHomeUuids(owner));
	}

	@Test
	void increasedLimitAllowsMultiplePublications() {
		SharedHomeService service = new SharedHomeService();
		UUID owner = UUID.randomUUID();
		SharedHomeKey first = key(owner);
		SharedHomeKey second = key(owner);

		assertEquals(SharedHomeService.PublishStatus.PUBLISHED,
				service.publishOrBroadcast(first, 2, 1_000L, 0L).status());
		assertEquals(SharedHomeService.PublishStatus.PUBLISHED,
				service.publishOrBroadcast(second, 2, 1_000L, 0L).status());
		assertEquals(Set.of(first.homeUuid(), second.homeUuid()), service.publishedHomeUuids(owner));
	}

	@Test
	void broadcastCooldownIsSharedAcrossOwnersHomes() {
		SharedHomeService service = new SharedHomeService();
		UUID owner = UUID.randomUUID();
		SharedHomeKey first = key(owner);
		SharedHomeKey second = key(owner);

		service.publishOrBroadcast(first, 2, 1_000L, COOLDOWN_MILLIS);
		SharedHomeService.PublishResult blocked = service.publishOrBroadcast(second, 2, 11_000L, COOLDOWN_MILLIS);

		assertEquals(SharedHomeService.PublishStatus.COOLDOWN, blocked.status());
		assertEquals(50_000L, blocked.remainingCooldownMillis());
		assertFalse(service.isPublished(second));
	}

	@Test
	void subscriptionsAreUnlimitedAndKeepInsertionOrder() {
		SharedHomeService service = new SharedHomeService();
		UUID subscriber = UUID.randomUUID();
		SharedHomeKey first = publish(service, UUID.randomUUID(), 1_000L);
		SharedHomeKey second = publish(service, UUID.randomUUID(), 1_000L);

		assertEquals(SharedHomeService.SubscriptionStatus.SUBSCRIBED, service.subscribe(subscriber, first));
		assertEquals(SharedHomeService.SubscriptionStatus.SUBSCRIBED, service.subscribe(subscriber, second));
		assertEquals(List.of(first, second), service.subscriptions(subscriber).stream()
				.map(SharedHomeService.SubscriptionView::key).toList());
		assertEquals(List.of(0, 1), service.subscriptions(subscriber).stream()
				.map(SharedHomeService.SubscriptionView::sequence).toList());
	}

	@Test
	void withdrawalRemovesSubscriptionsAndReportsAffectedPlayers() {
		SharedHomeService service = new SharedHomeService();
		SharedHomeKey key = publish(service, UUID.randomUUID(), 1_000L);
		UUID firstSubscriber = UUID.randomUUID();
		UUID secondSubscriber = UUID.randomUUID();
		service.subscribe(firstSubscriber, key);
		service.subscribe(secondSubscriber, key);

		assertEquals(Set.of(firstSubscriber, secondSubscriber), service.withdraw(key));
		assertFalse(service.hasPublications(key.ownerUuid()));
		assertFalse(service.isPublished(key));
		assertFalse(service.isSubscribed(firstSubscriber, key));
		assertTrue(service.subscriptions(secondSubscriber).isEmpty());
	}

	@Test
	void mapVisibilityBelongsToEachSubscription() {
		SharedHomeService service = new SharedHomeService();
		SharedHomeKey key = publish(service, UUID.randomUUID(), 1_000L);
		UUID subscriber = UUID.randomUUID();
		service.subscribe(subscriber, key);

		assertTrue(service.subscriptions(subscriber).getFirst().mapVisible());
		assertTrue(service.setMapVisible(subscriber, key, false));
		assertFalse(service.subscriptions(subscriber).getFirst().mapVisible());
	}

	@Test
	void missingOwnerHomesWithdrawOnlyMissingPublications() {
		SharedHomeService service = new SharedHomeService();
		UUID owner = UUID.randomUUID();
		SharedHomeKey kept = key(owner);
		SharedHomeKey removed = key(owner);
		service.publishOrBroadcast(kept, 2, 1_000L, 0L);
		service.publishOrBroadcast(removed, 2, 1_000L, 0L);
		UUID subscriber = UUID.randomUUID();
		service.subscribe(subscriber, removed);

		assertEquals(Set.of(subscriber), service.removeMissingPublications(owner, Set.of(kept.homeUuid())));
		assertTrue(service.isPublished(kept));
		assertFalse(service.isPublished(removed));
	}

	@Test
	void ownerIndexesRemainIndependentAtScale() {
		SharedHomeService service = new SharedHomeService();
		List<SharedHomeKey> keys = java.util.stream.IntStream.range(0, 10_000)
				.mapToObj(ignored -> key(UUID.randomUUID()))
				.toList();
		for (SharedHomeKey key : keys) {
			service.publishOrBroadcast(key, 1, 1_000L, 0L);
		}

		SharedHomeKey target = keys.get(5_000);
		assertEquals(Set.of(target.homeUuid()), service.publishedHomeUuids(target.ownerUuid()));
		assertEquals(Set.of(), service.removeMissingPublications(target.ownerUuid(), Set.of(target.homeUuid())));
		assertTrue(service.isPublished(target));
		assertTrue(service.isPublished(keys.getFirst()));
		assertTrue(service.isPublished(keys.getLast()));
	}

	private static SharedHomeKey publish(SharedHomeService service, UUID owner, long now) {
		SharedHomeKey key = key(owner);
		service.publishOrBroadcast(key, 1, now, 0L);
		return key;
	}

	private static SharedHomeKey key(UUID owner) {
		return new SharedHomeKey(owner, UUID.randomUUID());
	}
}
