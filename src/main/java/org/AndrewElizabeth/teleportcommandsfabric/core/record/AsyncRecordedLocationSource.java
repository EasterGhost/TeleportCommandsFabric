package org.AndrewElizabeth.teleportcommandsfabric.core.record;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AsyncRecordedLocationSource {
	CompletableFuture<Optional<RecordedLocationView>> getDeathLocation(UUID playerUuid);

	CompletableFuture<Optional<RecordedLocationView>> getPreviousTeleportLocation(UUID playerUuid);

	CompletableFuture<Void> recordDeathLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension);

	CompletableFuture<Void> recordPreviousTeleportLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension);

	CompletableFuture<Void> removeDeathLocation(UUID playerUuid);

	CompletableFuture<Void> removePreviousTeleportLocation(UUID playerUuid);

	CompletableFuture<Void> removeRecord(UUID playerUuid);
}
