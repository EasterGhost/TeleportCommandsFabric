package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

final class PlayerProfileEntry {
	final UUID playerUuid;
	volatile PlayerProfile profile;
	volatile boolean loaded;
	volatile boolean dirty;
	volatile boolean saveInProgress;
	volatile boolean online;
	volatile long lastAccessTime;
	volatile ScheduledFuture<?> unloadTask;
	CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
	CompletableFuture<Void> saveCompletion = CompletableFuture.completedFuture(null);

	PlayerProfileEntry(UUID playerUuid) {
		this.playerUuid = playerUuid;
		this.lastAccessTime = Util.getMillis();
	}

	<T> CompletableFuture<T> submit(Task<T> task, Executor executor, boolean markDirty) {
		synchronized (this) {
			CompletableFuture<T> nextTask = tail
					.handle((ignored, throwable) -> null)
					.thenApplyAsync(ignored -> executeTask(task, markDirty), executor);
			tail = nextTask.handle((ignored, throwable) -> null);
			return nextTask;
		}
	}

	PendingSave captureSaveSnapshot() {
		if (!loaded || profile == null || !dirty || saveInProgress) {
			return null;
		}

		CompletableFuture<Void> completion = new CompletableFuture<>();
		PendingSave pendingSave = new PendingSave(profile.snapshotForSave(), completion);
		dirty = false;
		saveInProgress = true;
		saveCompletion = completion;
		return pendingSave;
	}

	void finishSave() {
		saveInProgress = false;
		saveCompletion = CompletableFuture.completedFuture(null);
	}

	void failSave() {
		finishSave();
		dirty = true;
	}

	void cancelUnloadTask() {
		if (unloadTask != null) {
			unloadTask.cancel(false);
			unloadTask = null;
		}
	}

	private <T> T executeTask(Task<T> task, boolean markDirty) {
		try {
			lastAccessTime = Util.getMillis();
			T result = task.run(this);
			if (markDirty && loaded && profile != null) {
				dirty = true;
			}
			return result;
		} catch (IOException exception) {
			ModConstants.LOGGER.error("Failed to process player profile task for {}", playerUuid, exception);
			throw new CompletionException(exception);
		}
	}

	@FunctionalInterface
	interface Task<T> {
		T run(PlayerProfileEntry entry) throws IOException;
	}

	record PendingSave(PlayerProfile snapshot, CompletableFuture<Void> completion) {
	}
}
