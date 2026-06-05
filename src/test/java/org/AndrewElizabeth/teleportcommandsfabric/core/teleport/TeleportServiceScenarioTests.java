package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.*;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.AndrewElizabeth.teleportcommandsfabric.testsupport.ScenarioTestSupport.scenario;
import static org.junit.jupiter.api.Assertions.*;

public final class TeleportServiceScenarioTests {
	private static final Unsafe UNSAFE = unsafe();

	TeleportServiceScenarioTests() {
	}

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@TestFactory
	Stream<DynamicTest> scenarios() {
		return Stream.of(
				scenario("Immediate success records previous and applies cooldown",
						"Verify immediate target execution, previous-location recording, and cooldown refresh.",
						TeleportServiceScenarioTests::testImmediateSuccessRecordsPreviousAndAppliesCooldown),
				scenario("Delayed target waits for resolver and delay",
						"Verify unresolved target futures block execution until resolved and delay-ready.",
						TeleportServiceScenarioTests::testDelayedTargetWaitsForResolverAndDelay),
				scenario("Delayed preload starts only inside lead window",
						"Verify delayed requests do not preload too early and execute at the delay deadline.",
						TeleportServiceScenarioTests::testDelayedPreloadStartsOnlyInsideLeadWindow),
				scenario("Replacement cancels old pending and executes latest",
						"Verify a newer request cancels an older pending request for the same player.",
						TeleportServiceScenarioTests::testReplacementCancelsOldPendingAndExecutesLatest),
				scenario("Preload path waits until chunk ready",
						"Verify unloaded targets create a preload ticket and execute once ready.",
						TeleportServiceScenarioTests::testPreloadPathWaitsUntilChunkReady),
				scenario("Death cancels pending before execution",
						"Verify death events cancel pending teleport execution.",
						TeleportServiceScenarioTests::testDeathCancelsPendingBeforeExecution));
	}

	private static void testImmediateSuccessRecordsPreviousAndAppliesCooldown() {
		Harness harness = Harness.create();
		TeleportTarget target = harness.target(new Vec3(20.5D, 70.0D, 20.5D));
		TargetTeleportOptions options = new TargetTeleportOptions(0, 20L, false, true);
		CompletableFuture<TeleportStatus> result = harness.service.request(
				TeleportRequest.resolved(target, options));

		harness.service.tick();

		assertEquals(TeleportStatus.SUCCESS, result.join(), "immediate target should teleport successfully");
		assertEquals(1, harness.runtime.teleportCalls, "teleport should be executed exactly once");
		assertEquals(new Vec3(20.5D, 70.0D, 20.5D), harness.runtime.lastDestination, "destination should preserve resolved position");
		assertEquals(1, harness.recorded.previousRecords.size(), "previous location should be recorded before teleport");
		assertEquals(harness.runtime.playerBlockPos, harness.recorded.previousRecords.get(0).pos(), "previous record should use player position");

		CompletableFuture<TeleportStatus> cooldown = harness.service.request(
				TeleportRequest.resolved(target, options));
		assertEquals(TeleportStatus.COOLDOWN, cooldown.join(), "successful teleport should start cooldown");
	}

	private static void testDelayedTargetWaitsForResolverAndDelay() {
		Harness harness = Harness.create();
		CompletableFuture<TeleportTargetResult> targetFuture = new CompletableFuture<>();
		TargetTeleportOptions options = new TargetTeleportOptions(2, 0L, false, false);
		CompletableFuture<TeleportStatus> result = harness.service.request(
				TeleportRequest.of(targetFuture, options));

		harness.service.tick();
		assertFalse(result.isDone(), "pending should wait while target future is unresolved");
		assertEquals(0, harness.runtime.teleportCalls, "unresolved target should not teleport");

		targetFuture.complete(TeleportTargetResult.resolved(harness.target(new Vec3(30.5D, 75.0D, 30.5D))));
		harness.service.tick();

		assertEquals(TeleportStatus.SUCCESS, result.join(), "target should execute once resolver and delay are ready");
		assertEquals(1, harness.runtime.teleportCalls, "delayed request should teleport once");
	}

	private static void testReplacementCancelsOldPendingAndExecutesLatest() {
		Harness harness = Harness.create();
		TargetTeleportOptions options = new TargetTeleportOptions(0, 0L, false, false);
		CompletableFuture<TeleportStatus> first = harness.service.request(TeleportRequest.resolved(
				harness.target(new Vec3(10.5D, 64.0D, 10.5D)), options));
		CompletableFuture<TeleportStatus> second = harness.service.request(TeleportRequest.resolved(
				harness.target(new Vec3(40.5D, 80.0D, 40.5D)), options));

		assertEquals(TeleportStatus.CANCELLED, first.join(), "new request should cancel replaced pending");

		harness.service.tick();

		assertEquals(TeleportStatus.SUCCESS, second.join(), "latest pending should execute");
		assertEquals(1, harness.runtime.teleportCalls, "only latest pending should teleport");
		assertEquals(new Vec3(40.5D, 80.0D, 40.5D), harness.runtime.lastDestination, "latest target should win replacement");
	}

	private static void testDelayedPreloadStartsOnlyInsideLeadWindow() {
		Harness harness = Harness.create();
		harness.preload.loaded = false;
		int delayTicks = (int) TeleportServiceSettings.PRELOAD_LEAD_TICKS + 2;
		TargetTeleportOptions options = new TargetTeleportOptions(delayTicks, 0L, false, false);
		CompletableFuture<TeleportStatus> result = harness.service.request(TeleportRequest.resolved(
				harness.target(new Vec3(60.5D, 85.0D, 60.5D)), options));

		int ticksBeforeLeadWindow = delayTicks - (int) TeleportServiceSettings.PRELOAD_LEAD_TICKS - 1;
		for (int i = 0; i < ticksBeforeLeadWindow; i++) {
			harness.service.tick();
		}
		assertFalse(result.isDone(), "delayed request should still be pending before lead window");
		assertEquals(0, harness.preload.preloadCalls, "preload should not start before lead window");

		harness.service.tick();
		assertEquals(1, harness.preload.preloadCalls, "preload should start inside lead window");

		harness.preload.loaded = true;
		harness.service.tick();
		assertFalse(result.isDone(), "preload ready before delay deadline should not execute early");
		for (int i = 0; i < delayTicks - ticksBeforeLeadWindow - 2; i++) {
			harness.service.tick();
		}
		assertEquals(TeleportStatus.SUCCESS, result.join(), "delayed preload should execute at delay deadline");
	}

	private static void testPreloadPathWaitsUntilChunkReady() {
		Harness harness = Harness.create();
		harness.preload.loaded = false;
		TargetTeleportOptions options = new TargetTeleportOptions(0, 0L, false, false);
		CompletableFuture<TeleportStatus> result = harness.service.request(TeleportRequest.resolved(
				harness.target(new Vec3(80.5D, 90.0D, 80.5D)), options));

		harness.service.tick();

		assertFalse(result.isDone(), "unloaded chunk should keep result pending");
		assertEquals(1, harness.preload.preloadCalls, "unloaded target should start preload");
		assertEquals(1, harness.preload.activeTicketCount(), "preload ticket should remain active while waiting");
		assertEquals(0, harness.runtime.teleportCalls, "preload wait should not teleport early");

		harness.preload.loaded = true;
		harness.service.tick();

		assertEquals(TeleportStatus.SUCCESS, result.join(), "ready preload should execute teleport");
		assertEquals(1, harness.runtime.teleportCalls, "ready preload should teleport once");
		assertEquals(0, harness.preload.activeTicketCount(), "successful teleport should release preload ticket");
		assertEquals(1, harness.preload.releaseCalls, "successful teleport should release exactly once");
	}

	private static void testDeathCancelsPendingBeforeExecution() {
		Harness harness = Harness.create();
		TargetTeleportOptions options = new TargetTeleportOptions(5, 0L, false, false);
		CompletableFuture<TeleportStatus> result = harness.service.request(TeleportRequest.resolved(
				harness.target(new Vec3(15.5D, 70.0D, 15.5D)), options));

		harness.service.onPlayerDeath();
		harness.service.tick();

		assertEquals(TeleportStatus.CANCELLED_BY_EVENT, result.join(), "death should cancel pending teleport");
		assertEquals(0, harness.runtime.teleportCalls, "cancelled pending should not execute");
	}

	private static Unsafe unsafe() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Unsafe) field.get(null);
		} catch (ReflectiveOperationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static ServerLevel allocateServerLevel() {
		try {
			return ServerLevel.class.cast(UNSAFE.allocateInstance(ServerLevel.class));
		} catch (InstantiationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static final class Harness {
		private final UUID playerUuid = UUID.randomUUID();
		private final ServerLevel world = allocateServerLevel();
		private final FakeRecordedSource recorded = new FakeRecordedSource();
		private final FakePreload preload = new FakePreload();
		private final FakeRuntime runtime = new FakeRuntime(playerUuid);
		private final ScenarioTeleportService service = new ScenarioTeleportService(
				playerUuid, runtime, recorded, new TeleportOperationManager(), preload, new TeleportBatchDispatcher());

		private static Harness create() {
			return new Harness();
		}

		private TeleportTarget target(Vec3 position) {
			return new TeleportTarget(world, position);
		}
	}

	/**
	 * Test-only copy of TeleportService orchestration. It keeps production core
	 * classes in the loop while replacing Minecraft runtime calls with fakes.
	 */
	private static final class ScenarioTeleportService {
		private final UUID playerUuid;
		private final FakeRuntime runtime;
		private final AsyncRecordedLocationSource recordedSource;
		private final TeleportOperationManager operationManager;
		private final FakePreload preload;
		private final TeleportBatchDispatcher dispatcher;
		private long currentTick;
		private int admissionRampTick;

		private ScenarioTeleportService(UUID playerUuid, FakeRuntime runtime, AsyncRecordedLocationSource recordedSource,
				TeleportOperationManager operationManager, FakePreload preload, TeleportBatchDispatcher dispatcher) {
			this.playerUuid = playerUuid;
			this.runtime = runtime;
			this.recordedSource = recordedSource;
			this.operationManager = operationManager;
			this.preload = preload;
			this.dispatcher = dispatcher;
		}

		private CompletableFuture<TeleportStatus> request(TeleportRequest request) {
			if (runtime.dead) {
				return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
			}
			long remainingCooldown = operationManager.getRemainingCooldownMillis(playerUuid, request.options().effectiveCooldownMillis());
			if (remainingCooldown > 0L) {
				return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
			}

			TeleportOperationManager.PendingCreateResult createResult = operationManager.createPending(playerUuid, request, currentTick);
			createResult.replaced().ifPresent(replaced -> preload.release(replaced.playerUuid(), replaced.pendingSequence()));

			TargetTeleportPending pending = createResult.pending();
			request.targetFuture().whenComplete((targetResult, throwable) -> {
				if (throwable != null) {
					pending.completeTarget(TeleportTargetResult.failed(TeleportStatus.FAILED));
				} else {
					pending.completeTarget(targetResult);
				}
			});
			return pending.resultFuture();
		}

		private void tick() {
			currentTick++;
			dispatcher.beginTick();
			handlePreloadTick();
			advancePending();
			dispatcher.drain(this::executeOne);
			updateAdmissionRamp();
		}

		private void onPlayerDeath() {
			operationManager.getCurrentOperation(playerUuid)
					.ifPresent(pending -> cancelPending(pending.pendingSequence(), TeleportStatus.CANCELLED_BY_EVENT));
		}

		private void cancelPending(long pendingSequence, TeleportStatus status) {
			if (operationManager.cancelPending(playerUuid, pendingSequence, status)) {
				preload.release(playerUuid, pendingSequence);
			}
		}

		private void handlePreloadTick() {
			FakePreload.TickResult result = preload.tick();
			for (TargetTeleportExecution entry : result.ready()) {
				if (operationManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
					submitReadyExecution(entry);
				}
			}
			for (TargetTeleportExecution entry : result.timedOut()) {
				finishEntry(entry, TeleportStatus.FAILED);
			}
		}

		private void advancePending() {
			int admissionLimit = currentReadyAdmissionLimit();
			int[] admitted = { 0 };
			operationManager.visitCurrentTargetPendings(pending -> {
				if (admitted[0] >= admissionLimit) {
					return false;
				}
				if (pending.isQueued() || !pending.isTargetDone()) {
					return true;
				}

				TeleportTargetResult targetResult = pending.targetResult();
				if (targetResult instanceof TeleportTargetResult.Failed failed) {
					cancelPending(pending.pendingSequence(), failed.reason());
					admitted[0]++;
					return true;
				}
				if (!(targetResult instanceof TeleportTargetResult.Resolved resolved)) {
					cancelPending(pending.pendingSequence(), TeleportStatus.FAILED);
					admitted[0]++;
					return true;
				}

				TargetTeleportExecution entry = toExecutionEntry(pending, resolved.target());
				if (!pending.isDelayDone(currentTick)) {
					if (shouldStartPreloadDuringDelay(pending) && !pending.isPreloadStarted() && !preload.isChunkLoaded(resolved.target())) {
						pending.markPreloadStarted();
						preload.preload(entry);
						admitted[0]++;
					}
					return true;
				}

				if (!preload.isChunkLoaded(resolved.target())) {
					pending.markPreloadStarted();
					preload.preload(entry);
					admitted[0]++;
					return true;
				}

				submitReadyExecution(entry);
				admitted[0]++;
				return true;
			});
		}

		private int currentReadyAdmissionLimit() {
			if (admissionRampTick == 0) {
				return TeleportServiceSettings.READY_ADMISSION_FIRST_TICK_LIMIT;
			}
			return TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT;
		}

		private boolean shouldStartPreloadDuringDelay(TargetTeleportPending pending) {
			return currentTick >= pending.delayUntilTick() - TeleportServiceSettings.PRELOAD_LEAD_TICKS;
		}

		private void updateAdmissionRamp() {
			if (operationManager.hasCurrentOperations() || dispatcher.queueSize() > 0 || preload.activeTicketCount() > 0) {
				admissionRampTick++;
			} else {
				admissionRampTick = 0;
			}
		}

		private void submitReadyExecution(TargetTeleportExecution entry) {
			if (!operationManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
				return;
			}
			if (!operationManager.markTargetQueuedIfCurrentAndDelayDone(entry.playerUuid(), entry.pendingSequence(), currentTick)) {
				return;
			}
			if (dispatcher.canUseFastPath()) {
				dispatcher.noteFastPathUse();
				executeOne(entry);
			} else {
				dispatcher.enqueue(entry);
			}
		}

		private TargetTeleportExecution toExecutionEntry(TargetTeleportPending pending, TeleportTarget target) {
			return new TargetTeleportExecution(pending, target);
		}

		private TeleportStatus executeOne(TargetTeleportExecution entry) {
			if (!operationManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
				preload.release(entry.playerUuid(), entry.pendingSequence());
				entry.resultFuture().complete(TeleportStatus.CANCELLED);
				return TeleportStatus.CANCELLED;
			}
			if (!runtime.online) {
				return finishEntry(entry, TeleportStatus.PLAYER_DISCONNECTED);
			}
			if (runtime.dead) {
				return finishEntry(entry, TeleportStatus.CANCELLED_BY_EVENT);
			}
			if (!runtime.levelAvailable) {
				return finishEntry(entry, TeleportStatus.TARGET_UNAVAILABLE);
			}
			if (!preload.isChunkLoaded(entry.target())) {
				preload.preload(entry);
				return TeleportStatus.ACCEPTED;
			}

			Vec3 destination = entry.target().position();
			if (entry.options().safetyEnabled()) {
				Optional<BlockPos> safePos = runtime.safeBlockPos(entry.target());
				if (safePos.isEmpty()) {
					return finishEntry(entry, TeleportStatus.NO_SAFE_POSITION);
				}
				BlockPos pos = safePos.get();
				destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			}

			if (entry.options().recordPrevious() && recordedSource != null) {
				recordedSource.recordPreviousTeleportLocation(entry.playerUuid(), runtime.playerBlockPos, runtime.playerDimension);
			}

			if (!runtime.teleport(destination)) {
				return finishEntry(entry, TeleportStatus.FAILED);
			}

			operationManager.markSuccess(entry.playerUuid(), entry.pendingSequence());
			entry.resultFuture().complete(TeleportStatus.SUCCESS);
			preload.release(entry.playerUuid(), entry.pendingSequence());
			return TeleportStatus.SUCCESS;
		}

		private TeleportStatus finishEntry(TargetTeleportExecution entry, TeleportStatus status) {
			if (status == TeleportStatus.SUCCESS) {
				operationManager.markSuccess(entry.playerUuid(), entry.pendingSequence());
				entry.resultFuture().complete(TeleportStatus.SUCCESS);
			} else {
				operationManager.cancelPending(entry.playerUuid(), entry.pendingSequence(), status);
				entry.resultFuture().complete(status);
			}
			preload.release(entry.playerUuid(), entry.pendingSequence());
			return status;
		}
	}

	private static final class FakeRuntime {
		@SuppressWarnings("unused")
		private final UUID playerUuid;
		private boolean online = true;
		private boolean dead;
		private boolean levelAvailable = true;
		private boolean teleportReturn = true;
		private BlockPos playerBlockPos = new BlockPos(1, 64, 1);
		private ResourceKey<Level> playerDimension = Level.OVERWORLD;
		private int teleportCalls;
		private Vec3 lastDestination;

		private FakeRuntime(UUID playerUuid) {
			this.playerUuid = playerUuid;
		}

		private Optional<BlockPos> safeBlockPos(TeleportTarget target) {
			return Optional.of(BlockPos.containing(target.position()));
		}

		private boolean teleport(Vec3 destination) {
			teleportCalls++;
			lastDestination = destination;
			return teleportReturn;
		}
	}

	private static final class FakePreload {
		private final Map<String, TargetTeleportExecution> active = new HashMap<>();
		private final Set<String> handedOff = new HashSet<>();
		private boolean loaded = true;
		private int preloadCalls;
		private int releaseCalls;

		private boolean isChunkLoaded(TeleportTarget target) {
			return loaded;
		}

		private void preload(TargetTeleportExecution entry) {
			String key = key(entry.playerUuid(), entry.pendingSequence());
			if (active.containsKey(key)) {
				return;
			}
			preloadCalls++;
			active.put(key, entry);
		}

		private TickResult tick() {
			List<TargetTeleportExecution> ready = new ArrayList<>();
			if (loaded) {
				for (Map.Entry<String, TargetTeleportExecution> entry : active.entrySet()) {
					if (handedOff.add(entry.getKey())) {
						ready.add(entry.getValue());
					}
				}
			}
			return new TickResult(ready, List.of());
		}

		private void release(UUID playerUuid, long pendingSequence) {
			String key = key(playerUuid, pendingSequence);
			if (active.remove(key) != null) {
				releaseCalls++;
			}
			handedOff.remove(key);
		}

		private int activeTicketCount() {
			return active.size();
		}

		private static String key(UUID playerUuid, long pendingSequence) {
			return playerUuid + ":" + pendingSequence;
		}

		private record TickResult(
				List<TargetTeleportExecution> ready,
				List<TargetTeleportExecution> timedOut) {
		}
	}

	private static final class FakeRecordedSource implements AsyncRecordedLocationSource {
		private final List<RecordedEntry> previousRecords = new ArrayList<>();

		@Override
		public CompletableFuture<Optional<RecordedLocationView>> getDeathLocation(UUID playerUuid) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		@Override
		public CompletableFuture<Optional<RecordedLocationView>> getPreviousTeleportLocation(UUID playerUuid) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		@Override
		public CompletableFuture<Void> recordDeathLocation(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> recordPreviousTeleportLocation(UUID playerUuid, BlockPos pos,
				ResourceKey<Level> dimension) {
			previousRecords.add(new RecordedEntry(playerUuid, pos, dimension));
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> removeDeathLocation(UUID playerUuid) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> removePreviousTeleportLocation(UUID playerUuid) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> removeRecord(UUID playerUuid) {
			return CompletableFuture.completedFuture(null);
		}
	}

	private record RecordedEntry(UUID playerUuid, BlockPos pos, ResourceKey<Level> dimension) {
	}
}
