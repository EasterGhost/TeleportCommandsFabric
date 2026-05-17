package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTargetResult;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class WaypointTeleportTargets {
	private WaypointTeleportTargets() {
	}

	public static CompletableFuture<TeleportTargetResult> resolveByName(String name, AsyncWaypointSource source, MinecraftServer server) {
		return WaypointCrudService.getByName(name, source)
				.thenCompose(location -> server.submit(() -> toTargetResult(location, server)))
				.exceptionally(throwable -> TeleportTargetResult.failed(TeleportStatus.FAILED));
	}

	public static TeleportTargetResult toTargetResult(Optional<NamedLocationView> location, MinecraftServer server) {
		if (location.isEmpty()) {
			return TeleportTargetResult.failed(TeleportStatus.TARGET_UNAVAILABLE);
		}

		NamedLocationView namedLocation = location.get();
		ServerLevel world = server.getLevel(namedLocation.getDimension());
		if (world == null) {
			return TeleportTargetResult.failed(TeleportStatus.TARGET_UNAVAILABLE);
		}

		return TeleportTargetResult.resolved(TeleportTarget.of(world, new Vec3(
				namedLocation.getX() + 0.5D,
				namedLocation.getYPrecise(),
				namedLocation.getZ() + 0.5D)));
	}
}
