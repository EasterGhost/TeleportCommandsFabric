package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TeleportBatchDispatcher {
	private final ArrayDeque<TargetTeleportExecution> queue = new ArrayDeque<>();
	private int readyThisTick;
	private DrainResult lastDrainResult = new DrainResult(0, false, 0L);

	public void beginTick() {
		readyThisTick = 0;
	}

	public boolean canUseFastPath() {
		return queue.isEmpty() && readyThisTick < TeleportServiceSettings.FAST_PATH_THRESHOLD;
	}

	public void noteFastPathUse() {
		readyThisTick++;
	}

	public void enqueue(TargetTeleportExecution entry) {
		queue.addLast(Objects.requireNonNull(entry, "entry"));
		readyThisTick++;
	}

	public DrainResult drain(Function<TargetTeleportExecution, TeleportStatus> executor) {
		Objects.requireNonNull(executor, "executor");
		return drainBatch(entries -> {
			for (TargetTeleportExecution entry : entries) {
				executor.apply(entry);
			}
		});
	}

	public DrainResult drainBatch(Consumer<List<TargetTeleportExecution>> executor) {
		return drainBatch(TeleportServiceSettings.TIME_CHECK_INTERVAL, executor);
	}

	public DrainResult drainBatch(int batchEntryLimit, Consumer<List<TargetTeleportExecution>> executor) {
		Objects.requireNonNull(executor, "executor");
		if (batchEntryLimit <= 0) {
			throw new IllegalArgumentException("batchEntryLimit must be positive");
		}

		int processed = 0;
		boolean budgetHit = false;
		long start = System.nanoTime();

		while (!queue.isEmpty() && processed < TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK) {
			int batchSize = Math.min(batchEntryLimit,
					Math.min(queue.size(), TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK - processed));
			List<TargetTeleportExecution> entries = new ArrayList<>(batchSize);
			for (int i = 0; i < batchSize; i++) {
				entries.add(queue.pollFirst());
			}

			executor.accept(entries);
			processed += entries.size();

			if (System.nanoTime() - start >= TeleportServiceSettings.MAX_TELEPORT_BUDGET_NANOS) {
				budgetHit = true;
				break;
			}
		}

		long elapsedNanos = System.nanoTime() - start;
		lastDrainResult = new DrainResult(processed, budgetHit, elapsedNanos);
		return lastDrainResult;
	}

	public int queueSize() {
		return queue.size();
	}

	public DrainResult lastDrainResult() {
		return lastDrainResult;
	}

	public void clear() {
		queue.clear();
		readyThisTick = 0;
		lastDrainResult = new DrainResult(0, false, 0L);
	}

	public record DrainResult(int processed, boolean budgetHit, long elapsedNanos) {
	}
}