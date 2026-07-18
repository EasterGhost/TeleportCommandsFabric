package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageFutures;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SharedHomeResolver {
	private SharedHomeResolver() {
	}

	public static CompletableFuture<List<SharedHomeView>> resolveSubscriptions(UUID subscriberUuid,
			SharedHomeService service, PlayerProfileManager profileManager, MinecraftServer server) {
		List<SharedHomeService.SubscriptionView> subscriptions = service.subscriptions(subscriberUuid);
		if (subscriptions.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		}

		Map<UUID, String> ownerNames = new LinkedHashMap<>();
		Map<UUID, CompletableFuture<Map<UUID, NamedLocationView>>> homesByOwner = new LinkedHashMap<>();
		for (SharedHomeService.SubscriptionView subscription : subscriptions) {
			UUID ownerUuid = subscription.key().ownerUuid();
			ownerNames.computeIfAbsent(ownerUuid, uuid -> resolveOwnerName(server, uuid));
			homesByOwner.computeIfAbsent(ownerUuid, uuid -> loadOwnerHomes(uuid, profileManager));
		}

		CompletableFuture<?>[] loads = homesByOwner.values().toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(loads).thenApply(ignored -> subscriptions.stream()
				.map(subscription -> resolve(subscription, ownerNames, homesByOwner))
				.flatMap(Optional::stream)
				.map(SharedHomeView.class::cast)
				.toList());
	}

	public static CompletableFuture<Optional<NamedLocationView>> resolve(SharedHomeKey key, PlayerProfileManager profileManager) {
		return profileManager.query(key.ownerUuid(), profile -> profile.getHome(key.homeUuid()));
	}

	private static CompletableFuture<Map<UUID, NamedLocationView>> loadOwnerHomes(UUID ownerUuid, PlayerProfileManager profileManager) {
		return profileManager.query(ownerUuid, profile -> {
			Map<UUID, NamedLocationView> homes = new LinkedHashMap<>();
			for (NamedLocationView home : profile.getHomes()) {
				homes.put(home.getUuid(), home);
			}
			return Map.copyOf(homes);
		}).exceptionally(throwable -> {
			ModConstants.LOGGER.error("Failed to resolve shared homes for owner {}.", ownerUuid,
					StorageFutures.unwrapCompletionException(throwable));
			return Map.of();
		});
	}

	private static Optional<SharedHomeView> resolve(SharedHomeService.SubscriptionView subscription,
			Map<UUID, String> ownerNames,
			Map<UUID, CompletableFuture<Map<UUID, NamedLocationView>>> homesByOwner) {
		SharedHomeKey key = subscription.key();
		NamedLocationView location = homesByOwner.get(key.ownerUuid()).join().get(key.homeUuid());
		if (location == null || location.isTemporary() || location.isExpired()) {
			return Optional.empty();
		}
		return Optional.of(new SharedHomeView(key, ownerNames.get(key.ownerUuid()),
				NamedLocationSnapshot.from(location), subscription.mapVisible(), subscription.sequence()));
	}

	private static String resolveOwnerName(MinecraftServer server, UUID ownerUuid) {
		ServerPlayer online = server.getPlayerList().getPlayer(ownerUuid);
		if (online != null) {
			return online.getName().getString();
		}
		return server.services().nameToIdCache().get(ownerUuid)
				.map(profile -> profile.name())
				.filter(name -> !name.isBlank())
				.orElse(ownerUuid.toString());
	}
}
