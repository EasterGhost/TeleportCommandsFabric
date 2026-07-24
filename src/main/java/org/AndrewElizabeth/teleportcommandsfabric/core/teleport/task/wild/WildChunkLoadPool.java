package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportTicketTypes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

final class WildChunkLoadPool<C> {
	private static final int MAX_ACTIVE_CHUNKS = 32;
	private static final long LOAD_TIMEOUT_TICKS = 600L;
	private static final int TICKET_RADIUS = 0;

	private final Map<ChunkKey, ChunkLoad<C>> chunkLoads = new HashMap<>();
	private final ConcurrentLinkedQueue<LoadCompletion> completionQueue = new ConcurrentLinkedQueue<>();
	private long nextLoadSequence;
	private int activeTicketCount;

	boolean canAcquire(ResourceKey<Level> dimension, List<ChunkPos> chunks) {
		long newChunkCount = chunks.stream()
				.map(chunkPos -> new ChunkKey(dimension, chunkPos))
				.filter(key -> !chunkLoads.containsKey(key))
				.count();
		return activeTicketCount + newChunkCount <= MAX_ACTIVE_CHUNKS;
	}

	LoadState acquire(ServerLevel level, ChunkKey key, C consumer, long currentTick) {
		ChunkLoad<C> load = chunkLoads.get(key);
		if (load == null) {
			load = startLoad(level, key, currentTick);
			chunkLoads.put(key, load);
		}
		load.consumers().add(consumer);
		return load.state();
	}

	List<LoadResult<C>> advance(long currentTick) {
		List<LoadResult<C>> results = new ArrayList<>();
		consumeCompletions(results);
		expireLoads(currentTick, results);
		return List.copyOf(results);
	}

	void releaseConsumer(C owner, ChunkKey chunkKey) {
		ChunkLoad<C> load = chunkLoads.get(chunkKey);
		if (load == null) {
			return;
		}
		load.consumers().remove(owner);
		if (!load.consumers().isEmpty()) {
			return;
		}
		releaseTicket(load);
		chunkLoads.remove(chunkKey, load);
	}

	void clear() {
		for (ChunkLoad<C> load : List.copyOf(chunkLoads.values())) {
			releaseTicket(load);
		}
		chunkLoads.clear();
		completionQueue.clear();
		activeTicketCount = 0;
	}

	static boolean isSuccessfulLoad(Object result, Throwable throwable) {
		return throwable == null && result instanceof ChunkResult<?> chunkResult && chunkResult.isSuccess();
	}

	private ChunkLoad<C> startLoad(ServerLevel level, ChunkKey key, long currentTick) {
		long loadSequence = ++nextLoadSequence;
		ChunkLoad<C> load = new ChunkLoad<>(key, level, loadSequence, currentTick);
		load.markTicketHeld();
		activeTicketCount++;
		try {
			level.getChunkSource().addTicketAndLoadWithRadius(TeleportTicketTypes.wild(), key.chunkPos(), TICKET_RADIUS)
					.whenComplete((result, throwable) -> completionQueue.add(
							new LoadCompletion(key, loadSequence, isSuccessfulLoad(result, throwable))));
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.warn("Failed to start Wild chunk load for {} in {}", key.chunkPos(),
					key.dimension().identifier(), exception);
			completeLoad(load, LoadState.FAILED, null);
		}
		return load;
	}

	private void consumeCompletions(List<LoadResult<C>> results) {
		LoadCompletion completion;
		while ((completion = completionQueue.poll()) != null) {
			ChunkLoad<C> load = chunkLoads.get(completion.key());
			if (load == null || load.loadSequence() != completion.loadSequence()
					|| load.state() != LoadState.PENDING) {
				continue;
			}
			completeLoad(load, completion.success() ? LoadState.SUCCESS : LoadState.FAILED, results);
		}
	}

	private void expireLoads(long currentTick, List<LoadResult<C>> results) {
		for (ChunkLoad<C> load : List.copyOf(chunkLoads.values())) {
			if (load.state() == LoadState.PENDING && currentTick - load.startTick() >= LOAD_TIMEOUT_TICKS) {
				completeLoad(load, LoadState.FAILED, results);
			}
		}
	}

	private void completeLoad(ChunkLoad<C> load, LoadState state, List<LoadResult<C>> results) {
		if (load.state() != LoadState.PENDING) {
			return;
		}
		load.setState(state);
		if (state == LoadState.FAILED) {
			releaseTicket(load);
		}
		if (results != null) {
			results.add(new LoadResult<>(load.key(), state, List.copyOf(load.consumers())));
		}
	}

	private void releaseTicket(ChunkLoad<C> load) {
		if (!load.ticketHeld()) {
			return;
		}
		load.clearTicketHeld();
		activeTicketCount--;
		try {
			load.level().getChunkSource().removeTicketWithRadius(TeleportTicketTypes.wild(), load.key().chunkPos(),
					TICKET_RADIUS);
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.warn("Failed to release Wild chunk ticket for {} in {}", load.key().chunkPos(),
					load.key().dimension().identifier(), exception);
		}
	}

	record ChunkKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
		ChunkKey {
			Objects.requireNonNull(dimension, "dimension");
			Objects.requireNonNull(chunkPos, "chunkPos");
		}
	}

	record LoadResult<C>(ChunkKey key, LoadState state, List<C> consumers) {
		LoadResult {
			consumers = List.copyOf(consumers);
		}
	}

	enum LoadState {
		PENDING,
		SUCCESS,
		FAILED
	}

	private record LoadCompletion(ChunkKey key, long loadSequence, boolean success) {
	}

	private static final class ChunkLoad<C> {
		private final ChunkKey key;
		private final ServerLevel level;
		private final long loadSequence;
		private final long startTick;
		private final Set<C> consumers = new HashSet<>();
		private LoadState state = LoadState.PENDING;
		private boolean ticketHeld;

		private ChunkLoad(ChunkKey key, ServerLevel level, long loadSequence, long startTick) {
			this.key = key;
			this.level = level;
			this.loadSequence = loadSequence;
			this.startTick = startTick;
		}

		private ChunkKey key() {
			return key;
		}

		private ServerLevel level() {
			return level;
		}

		private long loadSequence() {
			return loadSequence;
		}

		private long startTick() {
			return startTick;
		}

		private Set<C> consumers() {
			return consumers;
		}

		private LoadState state() {
			return state;
		}

		private void setState(LoadState state) {
			this.state = state;
		}

		private boolean ticketHeld() {
			return ticketHeld;
		}

		private void markTicketHeld() {
			ticketHeld = true;
		}

		private void clearTicketHeld() {
			ticketHeld = false;
		}
	}
}
