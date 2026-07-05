package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TeleportPreloadManager {
	private final Map<Key, PreloadHandle> handles = new HashMap<>();
	private static final PreloadTickResult EMPTY_TICK_RESULT = new PreloadTickResult(List.of(), List.of());
	private volatile boolean enabled;
	private volatile int radiusChunks = TeleportServiceSettings.PRELOAD_RADIUS_CHUNKS;
	private volatile boolean releaseAllOnNextTick;

	public void configure(boolean enabled, int radiusChunks) {
		this.enabled = enabled;
		this.radiusChunks = Math.max(0, radiusChunks);
		if (!enabled) {
			releaseAllOnNextTick = true;
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isChunkLoaded(TeleportTarget target) {
		Objects.requireNonNull(target, "target");
		Vec3 position = target.position();
		return target.world().isLoaded(BlockPos.containing(position));
	}

	public boolean shouldPreload(TeleportTarget target) {
		return enabled && !isChunkLoaded(target);
	}

	public boolean preload(TargetTeleportExecution entry, long currentTick) {
		if (!shouldPreload(entry.target())) {
			return false;
		}
		Key key = Key.from(entry);
		PreloadHandle existing = handles.get(key);
		if (existing != null) {
			return true;
		}

		BlockPos blockPos = BlockPos.containing(entry.target().position());
		ChunkPos chunkPos = new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
		int radius = radiusChunks;
		ServerChunkCache chunkSource = entry.target().world().getChunkSource();
		chunkSource.addTicketWithRadius(TicketType.UNKNOWN, chunkPos, radius);
		handles.put(key, new PreloadHandle(entry, chunkPos, radius, currentTick + TeleportServiceSettings.PRELOAD_TIMEOUT_TICKS));
		return true;
	}

	public boolean isReady(TargetTeleportExecution entry) {
		return isChunkLoaded(entry.target());
	}

	public PreloadTickResult tick(long currentTick) {
		if (releaseAllOnNextTick) {
			releaseAllOnNextTick = false;
			releaseAll();
		}
		if (handles.isEmpty()) {
			return EMPTY_TICK_RESULT;
		}

		List<TargetTeleportExecution> ready = null;
		List<TargetTeleportExecution> timedOut = null;

		for (PreloadHandle handle : handles.values()) {
			if (!handle.handedOff && isChunkLoaded(handle.entry.target())) {
				handle.handedOff = true;
				if (ready == null) {
					ready = new ArrayList<>();
				}
				ready.add(handle.entry);
			} else if (!handle.handedOff && currentTick >= handle.timeoutTick) {
				handle.handedOff = true;
				if (timedOut == null) {
					timedOut = new ArrayList<>();
				}
				timedOut.add(handle.entry);
			}
		}

		return ready == null && timedOut == null
				? EMPTY_TICK_RESULT
				: new PreloadTickResult(ready == null ? List.of() : ready, timedOut == null ? List.of() : timedOut);
	}

	public void release(UUID playerUuid, long pendingSequence) {
		Key key = new Key(playerUuid, pendingSequence);
		PreloadHandle handle = handles.remove(key);
		if (handle == null) {
			return;
		}
		handle.entry.target().world().getChunkSource()
				.removeTicketWithRadius(TicketType.UNKNOWN, handle.chunkPos, handle.radiusChunks);
	}

	public void releaseAll() {
		List<Key> keys = new ArrayList<>(handles.keySet());
		for (Key key : keys) {
			release(key.playerUuid, key.pendingSequence);
		}
	}

	public int activeTicketCount() {
		return handles.size();
	}

	public record PreloadTickResult(List<TargetTeleportExecution> ready, List<TargetTeleportExecution> timedOut) {
	}

	private record Key(UUID playerUuid, long pendingSequence) {
		private static Key from(TargetTeleportExecution entry) {
			return new Key(entry.playerUuid(), entry.pendingSequence());
		}
	}

	private static final class PreloadHandle {
		private final TargetTeleportExecution entry;
		private final ChunkPos chunkPos;
		private final int radiusChunks;
		private final long timeoutTick;
		private boolean handedOff;

		private PreloadHandle(TargetTeleportExecution entry, ChunkPos chunkPos, int radiusChunks, long timeoutTick) {
			this.entry = entry;
			this.chunkPos = chunkPos;
			this.radiusChunks = radiusChunks;
			this.timeoutTick = timeoutTick;
			this.handedOff = false;
		}
	}
}
