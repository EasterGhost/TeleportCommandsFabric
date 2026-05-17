package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileExecutors;
import net.minecraft.util.Util;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageFutures;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerProfileManager {
	private static final Duration DEFAULT_SAVE_INTERVAL = Duration.ofMinutes(5);
	private static final Duration DEFAULT_UNLOAD_DELAY = Duration.ofMinutes(30);

	private final Map<UUID, PlayerProfileEntry> profiles = new ConcurrentHashMap<>();
	private final ExecutorService ioExecutor;
	private final ScheduledExecutorService scheduler;
	private final PlayerProfileIO io;
	private volatile Duration saveInterval;
	private volatile boolean deleteInvalidHomes;
	private final Duration unloadDelay;
	private ScheduledFuture<?> autoSaveTask;

	public PlayerProfileManager() {
		this(new PlayerProfileIO(), ProfileExecutors.createVirtualIoExecutor("tpc-player-profile-io-"),
				ProfileExecutors.createSingleThreadScheduler("tpc-player-profile-scheduler"),
				DEFAULT_SAVE_INTERVAL, DEFAULT_UNLOAD_DELAY);
	}

	public PlayerProfileManager(PlayerProfileIO io, ExecutorService ioExecutor, ScheduledExecutorService scheduler,
			Duration saveInterval, Duration unloadDelay) {
		this.io = io;
		this.ioExecutor = ioExecutor;
		this.scheduler = scheduler;
		this.saveInterval = saveInterval;
		this.unloadDelay = unloadDelay;
		scheduleAutoSave(saveInterval);
	}

	public <T> CompletableFuture<T> query(UUID uuid, Function<PlayerProfileView, T> action) {
		return submitInternal(uuid, entry -> action.apply(readViewForQuery(entry)), false);
	}

	public <T> CompletableFuture<T> mutate(UUID uuid, Function<PlayerProfile, T> action) {
		return submitInternal(uuid, entry -> action.apply(loadIfNeeded(entry)), true);
	}

	public CompletableFuture<Void> mutateVoid(UUID uuid, Consumer<PlayerProfile> action) {
		return submitInternal(uuid, entry -> {
			action.accept(loadIfNeeded(entry));
			return null;
		}, true);
	}

	public CompletableFuture<Void> save(UUID uuid) {
		return submitInternal(uuid, PlayerProfileEntry::captureSaveSnapshot, false)
				.thenCompose(pendingSave -> {
					if (pendingSave == null) {
						return CompletableFuture.completedFuture(null);
					}
					return CompletableFuture.runAsync(() -> saveSnapshot(pendingSave.snapshot()), ioExecutor)
							.handle((ignored, throwable) -> throwable)
							.thenCompose(throwable -> finishSnapshotSave(uuid, pendingSave, throwable));
				});
	}

	public CompletableFuture<Void> unload(UUID uuid) {
		return submitInternal(uuid, entry -> {
			if (entry.saveInProgress) {
				return entry.saveCompletion;
			}

			if (entry.online) {
				return null;
			}

			if (!entry.loaded || entry.profile == null) {
				profiles.remove(uuid, entry);
				return null;
			}

			if (entry.dirty || entry.profile.isEmpty()) {
				PlayerProfileLifecycle.flush(io, uuid, entry.profile);
				entry.dirty = false;
			}

			entry.profile = null;
			entry.loaded = false;
			entry.cancelUnloadTask();
			profiles.remove(uuid, entry);
			return null;
		}, false).thenCompose(saveCompletion -> {
			if (saveCompletion == null) {
				return CompletableFuture.completedFuture(null);
			}
			return saveCompletion
					.handle((ignored, throwable) -> null)
					.thenCompose(ignored -> unload(uuid));
		});
	}

	public void onPlayerJoin(UUID uuid) {
		if (uuid == null) {
			return;
		}

		submitInternal(uuid, entry -> {
			entry.online = true;
			entry.cancelUnloadTask();
			loadIfNeeded(entry);
			return null;
		}, false);
	}

	public void onPlayerQuit(UUID uuid) {
		if (uuid == null) {
			return;
		}

		submitInternal(uuid, entry -> {
			entry.online = false;
			entry.cancelUnloadTask();
			entry.unloadTask = scheduler.schedule(() -> queueUnloadIfIdle(uuid, entry), unloadDelay.toMillis(), TimeUnit.MILLISECONDS);
			return null;
		}, false).thenCompose(ignored -> save(uuid));
	}

	public CompletableFuture<Void> shutdown() {
		CompletableFuture<?>[] saves = profiles.keySet().stream()
				.map(this::flushProfile)
				.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(saves)
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

	public void setDeleteInvalidHomes(boolean deleteInvalidHomes) {
		this.deleteInvalidHomes = deleteInvalidHomes;
	}

	private CompletableFuture<Void> flushProfile(UUID uuid) {
		return submitInternal(uuid, entry -> {
			if (entry.saveInProgress) {
				return entry.saveCompletion;
			}

			if (!entry.loaded || entry.profile == null) {
				profiles.remove(uuid, entry);
				return null;
			}

			PlayerProfile profile = entry.profile;
			if (!entry.dirty && !profile.isEmpty()) {
				return null;
			}

			if (!PlayerProfileLifecycle.flush(io, uuid, profile)) {
				entry.profile = null;
				entry.loaded = false;
				entry.dirty = false;
				entry.saveInProgress = false;
				entry.saveCompletion = CompletableFuture.completedFuture(null);
				return null;
			}

			entry.dirty = false;
			return null;
		}, false).thenCompose(saveCompletion -> {
			if (saveCompletion == null) {
				return CompletableFuture.completedFuture(null);
			}
			return saveCompletion
					.handle((ignored, throwable) -> null)
					.thenCompose(ignored -> flushProfile(uuid));
		});
	}

	private void saveSnapshot(PlayerProfile snapshot) {
		try {
			io.save(snapshot);
		} catch (IOException exception) {
			throw new CompletionException(exception);
		}
	}

	private CompletableFuture<Void> finishSnapshotSave(UUID uuid, PlayerProfileEntry.PendingSave pendingSave, Throwable throwable) {
		return this.<Void>submitInternal(uuid, entry -> {
			if (throwable != null) {
				entry.failSave();
				Throwable cause = StorageFutures.unwrapCompletionException(throwable);
				if (cause instanceof IOException ioException) {
					throw ioException;
				}
				throw new CompletionException(cause);
			}
			entry.finishSave();
			return null;
		}, false).whenComplete((ignored, finishThrowable) -> {
			if (finishThrowable != null) {
				pendingSave.completion().completeExceptionally(finishThrowable);
			} else {
				pendingSave.completion().complete(null);
			}
		});
	}

	private void flushDirtyProfilesSafely() {
		try {
			for (UUID uuid : profiles.keySet()) {
				PlayerProfileEntry entry = profiles.get(uuid);
				if (entry == null || !entry.dirty) {
					continue;
				}
				save(uuid);
			}
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to flush dirty player profiles", exception);
		}
	}

	private synchronized void scheduleAutoSave(Duration saveInterval) {
		if (saveInterval == null || saveInterval.isZero() || saveInterval.isNegative()) {
			throw new IllegalArgumentException("save interval must be positive");
		}

		cancelAutoSaveTask();
		this.saveInterval = saveInterval;
		this.autoSaveTask = scheduler.scheduleAtFixedRate(this::flushDirtyProfilesSafely,
				this.saveInterval.toMillis(), this.saveInterval.toMillis(), TimeUnit.MILLISECONDS);
	}

	private synchronized void cancelAutoSaveTask() {
		if (autoSaveTask != null) {
			autoSaveTask.cancel(false);
			autoSaveTask = null;
		}
	}

	private void queueUnloadIfIdle(UUID uuid, PlayerProfileEntry expectedEntry) {
		if (!isCurrentEntry(uuid, expectedEntry)) {
			return;
		}

		submitOnEntry(expectedEntry, queuedEntry -> {
			if (!isCurrentEntry(uuid, queuedEntry)) {
				return null;
			}

			if (queuedEntry.online) {
				return null;
			}

			long idleMillis = Util.getMillis() - queuedEntry.lastAccessTime;
			long remainingDelay = unloadDelay.toMillis() - idleMillis;
			if (remainingDelay > 0) {
				queuedEntry.unloadTask = scheduler.schedule(() -> queueUnloadIfIdle(uuid, queuedEntry), remainingDelay, TimeUnit.MILLISECONDS);
				return null;
			}

			unload(uuid);
			return null;
		}, false);
	}

	private <T> CompletableFuture<T> submitInternal(UUID uuid, PlayerProfileEntry.Task<T> task, boolean markDirty) {
		if (uuid == null) {
			return CompletableFuture.failedFuture(new IllegalArgumentException("player uuid cannot be null"));
		}

		PlayerProfileEntry entry = profiles.computeIfAbsent(uuid, PlayerProfileEntry::new);
		return submitOnEntry(entry, task, markDirty);
	}

	private <T> CompletableFuture<T> submitOnEntry(PlayerProfileEntry entry, PlayerProfileEntry.Task<T> task, boolean markDirty) {
		return entry.submit(task, ioExecutor, markDirty);
	}

	private PlayerProfile loadIfNeeded(PlayerProfileEntry entry) throws IOException {
		if (!entry.loaded || entry.profile == null) {
			PlayerProfileLifecycle.LoadResult loadedProfile = PlayerProfileLifecycle.loadOrCreate(io, entry.playerUuid, deleteInvalidHomes);
			entry.profile = loadedProfile.profile();
			entry.loaded = true;
			entry.dirty |= loadedProfile.changed();
		}
		return entry.profile;
	}

	private PlayerProfileView readViewForQuery(PlayerProfileEntry entry) throws IOException {
		PlayerProfile profile = loadIfNeeded(entry);
		boolean changed = profile.refreshHomeState();
		if (changed) {
			profile.rebuildHomeNameIndex();
			entry.dirty = true;
		}
		return new PlayerProfileSnapshot(profile);
	}

	private boolean isCurrentEntry(UUID uuid, PlayerProfileEntry expectedEntry) {
		return expectedEntry != null && profiles.get(uuid) == expectedEntry;
	}

}
