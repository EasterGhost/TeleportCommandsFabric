package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileExecutors;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageFutures;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class GlobalProfileManager {
	private static final Duration DEFAULT_SAVE_INTERVAL = Duration.ofMinutes(5);

	private final ExecutorService ioExecutor;
	private final ScheduledExecutorService scheduler;
	private final GlobalProfileIO io;
	private volatile Duration saveInterval;
	private final Object monitor = new Object();
	private GlobalProfile profile;
	private volatile GlobalProfileSnapshot snapshot;
	private boolean dirty;
	private boolean saveInProgress;
	private volatile boolean deleteInvalidWarps;
	private ScheduledFuture<?> autoSaveTask;
	private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
	private CompletableFuture<Void> saveCompletion = CompletableFuture.completedFuture(null);

	public GlobalProfileManager() {
		this(new GlobalProfileIO(), ProfileExecutors.createVirtualIoExecutor("tpc-global-profile-io-"),
				ProfileExecutors.createSingleThreadScheduler("tpc-global-profile-scheduler"), DEFAULT_SAVE_INTERVAL);
	}

	public GlobalProfileManager(GlobalProfileIO io, ExecutorService ioExecutor, ScheduledExecutorService scheduler,
			Duration saveInterval) {
		this.io = io;
		this.ioExecutor = ioExecutor;
		this.scheduler = scheduler;
		this.saveInterval = saveInterval;
		this.profile = new GlobalProfile();
		publishSnapshot();
		scheduleAutoSave(saveInterval);
	}

	public CompletableFuture<GlobalProfile> load() {
		synchronized (monitor) {
			CompletableFuture<GlobalProfile> nextTask = tail.handle((ignored, throwable) -> null).thenApplyAsync(ignored -> loadProfile(), ioExecutor);
			tail = nextTask.handle((ignored, throwable) -> null);
			return nextTask;
		}
	}

	public <T> CompletableFuture<T> query(Function<GlobalProfileView, T> action) {
		try {
			return CompletableFuture.completedFuture(action.apply(snapshot));
		} catch (Exception exception) {
			return CompletableFuture.failedFuture(exception);
		}
	}

	public <T> CompletableFuture<T> mutate(Function<GlobalProfile, T> action) {
		return submitInternal(action::apply, true);
	}

	public CompletableFuture<Void> mutateVoid(Consumer<GlobalProfile> action) {
		return submitInternal(currentProfile -> {
			action.accept(currentProfile);
			return null;
		}, true);
	}

	public CompletableFuture<Void> save() {
		return submitInternal(this::captureSaveSnapshot, false)
				.thenCompose(pendingSave -> {
					if (pendingSave == null) {
						return CompletableFuture.completedFuture(null);
					}
					return CompletableFuture.runAsync(() -> saveSnapshot(pendingSave.snapshot()), ioExecutor).handle((ignored, throwable) -> throwable)
							.thenCompose(throwable -> finishSnapshotSave(pendingSave, throwable));
				});
	}

	public CompletableFuture<Void> shutdown() {
		return flushProfile()
				.handle((ignored, throwable) -> {
					cancelAutoSaveTask();
					scheduler.shutdown();
					ioExecutor.shutdown();
					if (throwable != null) {
						throw new CompletionException(throwable);
					}
					return null;
				});
	}

	public synchronized void setSaveInterval(Duration saveInterval) {
		scheduleAutoSave(saveInterval);
	}

	public void setDeleteInvalidWarps(boolean deleteInvalidWarps) {
		this.deleteInvalidWarps = deleteInvalidWarps;
	}

	private CompletableFuture<Void> flushProfile() {
		return submitInternal(currentProfile -> {
			if (saveInProgress) {
				return saveCompletion;
			}

			if (!GlobalProfileLifecycle.flush(io, currentProfile)) {
				profile = new GlobalProfile();
				dirty = false;
				saveInProgress = false;
				publishSnapshot();
				return null;
			}

			dirty = false;
			publishSnapshot();
			return null;
		}, false).thenCompose(currentSaveCompletion -> {
			if (currentSaveCompletion == null) {
				return CompletableFuture.completedFuture(null);
			}
			return currentSaveCompletion
					.handle((ignored, throwable) -> null)
					.thenCompose(ignored -> flushProfile());
		});
	}

	private PendingSave captureSaveSnapshot(GlobalProfile currentProfile) {
		if (refreshWarpState(currentProfile)) {
			dirty = true;
			publishSnapshot();
		}

		if (!dirty || saveInProgress) {
			return null;
		}

		GlobalProfile snapshot = currentProfile.snapshotForSave();
		CompletableFuture<Void> currentSaveCompletion = new CompletableFuture<>();
		dirty = false;
		saveInProgress = true;
		saveCompletion = currentSaveCompletion;
		return new PendingSave(snapshot, currentSaveCompletion);
	}

	private void saveSnapshot(GlobalProfile snapshot) {
		try {
			io.save(snapshot);
		} catch (IOException exception) {
			throw new CompletionException(exception);
		}
	}

	private CompletableFuture<Void> finishSnapshotSave(PendingSave pendingSave, Throwable throwable) {
		return this.<Void>submitInternal(currentProfile -> {
			saveInProgress = false;
			saveCompletion = CompletableFuture.completedFuture(null);
			if (throwable != null) {
				dirty = true;
				Throwable cause = StorageFutures.unwrapCompletionException(throwable);
				if (cause instanceof IOException ioException) {
					throw ioException;
				}
				throw new CompletionException(cause);
			}
			return null;
		}, false).whenComplete((ignored, finishThrowable) -> {
			if (finishThrowable != null) {
				pendingSave.completion().completeExceptionally(finishThrowable);
			} else {
				pendingSave.completion().complete(null);
			}
		});
	}

	private void flushDirtyProfileSafely() {
		try {
			save();
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to flush dirty global profile", exception);
		}
	}

	private synchronized void scheduleAutoSave(Duration saveInterval) {
		if (saveInterval == null || saveInterval.isZero() || saveInterval.isNegative()) {
			throw new IllegalArgumentException("save interval must be positive");
		}

		cancelAutoSaveTask();
		this.saveInterval = saveInterval;
		this.autoSaveTask = scheduler.scheduleAtFixedRate(this::flushDirtyProfileSafely, this.saveInterval.toMillis(), this.saveInterval.toMillis(),
				TimeUnit.MILLISECONDS);
	}

	private synchronized void cancelAutoSaveTask() {
		if (autoSaveTask != null) {
			autoSaveTask.cancel(false);
			autoSaveTask = null;
		}
	}

	private <T> CompletableFuture<T> submitInternal(GlobalTask<T> task, boolean markDirty) {
		synchronized (monitor) {
			CompletableFuture<T> nextTask = tail.handle((ignored, throwable) -> null).thenApplyAsync(ignored -> executeTask(task, markDirty), ioExecutor);
			tail = nextTask.handle((ignored, throwable) -> null);
			return nextTask;
		}
	}

	private <T> T executeTask(GlobalTask<T> task, boolean markDirty) {
		try {
			if (markDirty && refreshWarpState(profile)) {
				dirty = true;
				publishSnapshot();
			}
			T result = task.run(profile);
			if (markDirty) {
				dirty = true;
				publishSnapshot();
			}
			return result;
		} catch (IOException exception) {
			ModConstants.LOGGER.error("Failed to process global profile task", exception);
			throw new CompletionException(exception);
		}
	}

	private GlobalProfile loadProfile() {
		try {
			GlobalProfileLifecycle.LoadResult loadedProfile = GlobalProfileLifecycle.loadOrCreate(io, deleteInvalidWarps);
			profile = loadedProfile.profile();
			dirty = loadedProfile.changed();
			publishSnapshot();
			return profile;
		} catch (IOException exception) {
			throw new CompletionException(new IllegalStateException("Failed to load global profile", exception));
		}
	}

	private boolean refreshWarpState(GlobalProfile currentProfile) {
		boolean changed = currentProfile.refreshWarpState();
		if (changed) {
			currentProfile.rebuildWarpNameIndex();
		}
		return changed;
	}

	private void publishSnapshot() {
		snapshot = GlobalProfileSnapshot.from(profile);
	}

	private record PendingSave(GlobalProfile snapshot, CompletableFuture<Void> completion) {
	}

	@FunctionalInterface
	private interface GlobalTask<T> {
		T run(GlobalProfile profile) throws IOException;
	}
}
