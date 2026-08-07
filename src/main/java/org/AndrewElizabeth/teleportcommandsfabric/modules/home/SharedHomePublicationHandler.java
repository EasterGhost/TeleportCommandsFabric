package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeService;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class SharedHomePublicationHandler {
	private SharedHomePublicationHandler() {
	}

	static int shareByName(ServerPlayer player, String name) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		return resolveAndPublish(player, profileManager -> profileManager.query(player.getUUID(),
				profile -> profile.getHomeByName(name)), null);
	}

	static int shareFromManage(ServerPlayer player, UUID homeUuid, WaypointListQuery query) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		return resolveAndPublish(player, profileManager -> profileManager.query(player.getUUID(),
				profile -> profile.getHome(homeUuid)),
				currentPlayer -> HomeListHandler.renderHomeManage(currentPlayer, homeUuid, query, false));
	}

	static int withdrawFromManage(ServerPlayer player, UUID homeUuid, WaypointListQuery query) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		SharedHomeKey key = new SharedHomeKey(player.getUUID(), homeUuid);
		if (!TeleportCommands.SHARED_HOME_SERVICE.isPublished(key)) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.notPublished", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		Set<UUID> affected = TeleportCommands.SHARED_HOME_SERVICE.withdraw(key);
		markDirty(affected);
		SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.withdrawn", ChatFormatting.GREEN);
		HomeListHandler.renderHomeManage(player, homeUuid, query, false);
		return CommandReturns.COMPLETED_SYNC;
	}

	static void onOwnerHomesChanged(MinecraftServer server, UUID ownerUuid) {
		SharedHomeService service = TeleportCommands.SHARED_HOME_SERVICE;
		PlayerProfileManager profileManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (service == null || profileManager == null || !service.hasPublications(ownerUuid)) {
			return;
		}
		profileManager.query(ownerUuid, profile -> profile.getHomes().stream()
				.map(NamedLocationView::getUuid)
				.toList()).whenComplete((homeUuids, throwable) -> server.execute(() -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to reconcile shared homes for {}.", ownerUuid, throwable);
				return;
			}
			Set<UUID> affected = service.subscribersForOwner(ownerUuid);
			affected = union(affected, service.removeMissingPublications(ownerUuid, homeUuids));
			markDirty(affected);
		}));
	}

	static void invalidateMissing(SharedHomeKey key) {
		SharedHomeService service = TeleportCommands.SHARED_HOME_SERVICE;
		if (service != null) {
			markDirty(service.withdraw(key));
		}
	}

	private static int resolveAndPublish(ServerPlayer player, HomeResolver resolver,
			java.util.function.Consumer<ServerPlayer> completionAction) {
		PlayerProfileManager profileManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid, resolver.resolve(profileManager),
				(currentPlayer, home, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to resolve a home for sharing.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
						ChatFormatting.BOLD);
				return;
			}
			if (home == null || home.isEmpty()) {
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
				return;
			}
			publish(currentPlayer, home.get());
			if (completionAction != null) {
				completionAction.accept(currentPlayer);
			}
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void publish(ServerPlayer player, NamedLocationView home) {
		if (home.isTemporary()) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.temporary", ChatFormatting.RED);
			return;
		}
		SharedHomeSettings settings = ConfigManager.query(config -> new SharedHomeSettings(
				config.getHome().getSharedHomeMaximum(),
				TimeUtils.secondsToMillis(config.getHome().getSharedHomeBroadcastCooldownSeconds())));
		SharedHomeKey key = new SharedHomeKey(player.getUUID(), home.getUuid());
		SharedHomeService.PublishResult result = TeleportCommands.SHARED_HOME_SERVICE.publishOrBroadcast(key,
				settings.maximum(), Util.getMillis(), settings.broadcastCooldownMillis());
		switch (result.status()) {
		case LIMIT_REACHED -> SharedHomeMessages.sendLimitReached(player, settings.maximum());
		case COOLDOWN -> SharedHomeMessages.sendBroadcastCooldown(player,
				Math.max(1L, (result.remainingCooldownMillis() + 999L) / 1000L));
		case PUBLISHED, BROADCAST -> {
			broadcast(player, home.getName(), key);
			SharedHomeMessages.send(player, result.status() == SharedHomeService.PublishStatus.PUBLISHED
					? "commands.teleport_commands.sharedhome.published"
					: "commands.teleport_commands.sharedhome.broadcasted", ChatFormatting.GREEN);
		}
		}
	}

	private static void broadcast(ServerPlayer owner, String homeName, SharedHomeKey key) {
		TeleportCommands.SHARED_HOME_BROADCAST_DISPATCHER.enqueue(owner.level().getServer(), owner.getUUID(),
				owner.getName().getString(), homeName, key);
	}

	private static boolean ensureAvailable(ServerPlayer player) {
		if (!ConfigManager.query(config -> config.getHome().isEnabled())) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
			return false;
		}
		if (TeleportCommands.SHARED_HOME_SERVICE == null || TeleportCommands.SHARED_HOME_BROADCAST_DISPATCHER == null
				|| TeleportCommands.PLAYER_PROFILE_MANAGER == null) {
			SharedHomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}
		return true;
	}

	private static void markDirty(Collection<UUID> playerUuids) {
		for (UUID playerUuid : playerUuids) {
			WaypointMapSyncEvents.markPlayerDirty(playerUuid);
		}
	}

	private static Set<UUID> union(Set<UUID> first, Set<UUID> second) {
		if (first.isEmpty()) {
			return second;
		}
		if (second.isEmpty()) {
			return first;
		}
		java.util.LinkedHashSet<UUID> result = new java.util.LinkedHashSet<>(first);
		result.addAll(second);
		return Set.copyOf(result);
	}

	@FunctionalInterface
	private interface HomeResolver {
		java.util.concurrent.CompletableFuture<Optional<NamedLocationView>> resolve(PlayerProfileManager profileManager);
	}

	private record SharedHomeSettings(int maximum, long broadcastCooldownMillis) {
	}
}
