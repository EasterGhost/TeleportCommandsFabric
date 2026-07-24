package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportTicketTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
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
	private final Map<TicketKey, SharedTicket> sharedTickets = new HashMap<>();
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

	private boolean isChunkLoaded(TeleportTarget target) {
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
		SharedTicket ticket = acquireTicket(entry.target().world(), chunkPos, radius);
		handles.put(key, new PreloadHandle(entry, ticket, currentTick + TeleportServiceSettings.PRELOAD_TIMEOUT_TICKS));
		return true;
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
		releaseTicket(handle.ticket);
	}

	public void releaseAll() {
		List<Key> keys = new ArrayList<>(handles.keySet());
		for (Key key : keys) {
			release(key.playerUuid, key.pendingSequence);
		}
	}

	public int activeTicketCount() {
		return sharedTickets.size();
	}

	private SharedTicket acquireTicket(ServerLevel world, ChunkPos chunkPos, int radiusChunks) {
		TicketKey key = new TicketKey(world, chunkPos, radiusChunks);
		SharedTicket ticket = sharedTickets.get(key);
		if (ticket != null) {
			ticket.retain();
			return ticket;
		}

		ServerChunkCache chunkSource = world.getChunkSource();
		chunkSource.addTicketWithRadius(TeleportTicketTypes.targetPreload(), chunkPos, radiusChunks);
		SharedTicket created = new SharedTicket(key);
		sharedTickets.put(key, created);
		return created;
	}

	private void releaseTicket(SharedTicket ticket) {
		if (!ticket.release()) {
			return;
		}
		if (!sharedTickets.remove(ticket.key(), ticket)) {
			throw new IllegalStateException("Target preload ticket lease is not registered");
		}
		TicketKey key = ticket.key();
		key.world().getChunkSource().removeTicketWithRadius(
				TeleportTicketTypes.targetPreload(), key.chunkPos(), key.radiusChunks());
	}

	public record PreloadTickResult(List<TargetTeleportExecution> ready, List<TargetTeleportExecution> timedOut) {
	}

	private record Key(UUID playerUuid, long pendingSequence) {
		private static Key from(TargetTeleportExecution entry) {
			return new Key(entry.playerUuid(), entry.pendingSequence());
		}
	}

	private record TicketKey(ServerLevel world, ChunkPos chunkPos, int radiusChunks) {
		private TicketKey {
			Objects.requireNonNull(world, "world");
			Objects.requireNonNull(chunkPos, "chunkPos");
			if (radiusChunks < 0) {
				throw new IllegalArgumentException("radiusChunks cannot be negative");
			}
		}
	}

	private static final class PreloadHandle {
		private final TargetTeleportExecution entry;
		private final SharedTicket ticket;
		private final long timeoutTick;
		private boolean handedOff;

		private PreloadHandle(TargetTeleportExecution entry, SharedTicket ticket, long timeoutTick) {
			this.entry = entry;
			this.ticket = ticket;
			this.timeoutTick = timeoutTick;
			this.handedOff = false;
		}
	}

	private static final class SharedTicket {
		private final TicketKey key;
		private int references = 1;

		private SharedTicket(TicketKey key) {
			this.key = key;
		}

		private TicketKey key() {
			return key;
		}

		private void retain() {
			references++;
		}

		private boolean release() {
			if (references <= 0) {
				throw new IllegalStateException("Target preload ticket released more than acquired");
			}
			return --references == 0;
		}
	}
}
