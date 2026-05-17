package org.AndrewElizabeth.teleportcommandsfabric.core.record;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTargetResult;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RecordedLocationTeleportTargets {
	private RecordedLocationTeleportTargets() {
	}

	public static CompletableFuture<TeleportTargetResult> resolveDeath(UUID playerUuid, AsyncRecordedLocationSource source,
			MinecraftServer server) {
		return source.getDeathLocation(playerUuid)
				.thenCompose(location -> server.submit(() -> toTargetResult(location, server)))
				.exceptionally(throwable -> TeleportTargetResult.failed(TeleportStatus.FAILED));
	}

	public static CompletableFuture<TeleportTargetResult> resolvePrevious(UUID playerUuid, AsyncRecordedLocationSource source,
			MinecraftServer server) {
		return source.getPreviousTeleportLocation(playerUuid)
				.thenCompose(location -> server.submit(() -> toTargetResult(location, server)))
				.exceptionally(throwable -> TeleportTargetResult.failed(TeleportStatus.FAILED));
	}

	public static TeleportTargetResult toTargetResult(Optional<RecordedLocationView> location, MinecraftServer server) {
		if (location.isEmpty()) {
			return TeleportTargetResult.failed(TeleportStatus.TARGET_UNAVAILABLE);
		}

		RecordedLocationView recordedLocation = location.get();
		ServerLevel world = server.getLevel(recordedLocation.getDimension());
		if (world == null) {
			return TeleportTargetResult.failed(TeleportStatus.TARGET_UNAVAILABLE);
		}

		return TeleportTargetResult.resolved(TeleportTarget.centered(world, recordedLocation.getBlockPos()));
	}
}
