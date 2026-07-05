package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.test;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target.*;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.RecordedLocationTeleportTargets;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointTeleportTargets;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationNbtCodec;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationNbtCodec;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfile;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileLifecycle;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfile;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileLifecycle;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileNbtCodec;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustDecision;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import static org.AndrewElizabeth.teleportcommandsfabric.testsupport.ScenarioTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

public final class TeleportCoreTestSuite {
	private static TeleportTarget dummyTarget;
	private static ServerLevel dummyWorld;

	TeleportCoreTestSuite() {
	}

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		dummyTarget = createDummyTarget();
		dummyWorld = createDummyWorld();
		debug("GLOBAL PARAMS", "fastPathThreshold=" + TeleportServiceSettings.FAST_PATH_THRESHOLD
				+ ", maxBatchSize=" + TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK
				+ ", admissionRamp=[" + TeleportServiceSettings.READY_ADMISSION_FIRST_TICK_LIMIT
				+ ", " + TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT + " steady]"
				+ ", maxBudgetNanos=" + formatNumber(TeleportServiceSettings.MAX_TELEPORT_BUDGET_NANOS)
				+ ", timeCheckInterval=" + TeleportServiceSettings.TIME_CHECK_INTERVAL
				+ ", safetyWorkerThreads=" + TeleportServiceSettings.SAFETY_WORKER_THREADS
				+ ", safetyBatchSize=" + TeleportServiceSettings.SAFETY_BATCH_SIZE);
	}

	@TestFactory
	Stream<DynamicTest> scenarios() {
		return Stream.of(
				scenario("TargetTeleportOptions values and effective cooldown",
						"Verify current option constructor and effective cooldown values.",
						"caseA delayTicks=0 cooldownMillis=0 safetyEnabled=true recordPrevious=true; caseB delayTicks=3 cooldownMillis=100",
						TeleportCoreTestSuite::testTargetTeleportOptions),
				scenario("TeleportOperationManager pending lifecycle",
						"Verify create, replace, event cancel, success cooldown refresh, and quit cancel.",
						"delayTicks=5 cooldownMillis=10000 createdAtTicks=[10,20,30,40] quitTick=50 eventCancelStatus=CANCELLED_BY_EVENT",
						TeleportCoreTestSuite::testPendingLifecycle),
				scenario("TeleportOperationManager visit mutation safety",
						"Verify visitors may finish or cancel operations without mutating the active set during iteration.",
						"pendingPlayers=2 visitorAction=cancelCurrent",
						TeleportCoreTestSuite::testVisitCurrentOperationsMutationSafety),
				scenario("TeleportBatchDispatcher fast path threshold",
						"Verify same-tick fast path stops after the configured threshold.",
						"fastPathThreshold=" + TeleportServiceSettings.FAST_PATH_THRESHOLD,
						TeleportCoreTestSuite::testDispatcherFastPathThreshold),
				scenario("Ready admission ramp limits",
						"Verify the configured ready admission ramp used to avoid first-tick burst spikes.",
						"tick1=16 ticks2+=" + TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT,
						TeleportCoreTestSuite::testReadyAdmissionRamp),
				scenario("TeleportBatchDispatcher hard limit",
						"Verify one drain cannot process more than the per-tick hard limit.",
						"queued=" + (TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK + 20)
								+ " maxBatchSize=" + TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK,
						TeleportCoreTestSuite::testDispatcherHardLimit),
				scenario("TeleportBatchDispatcher budget hit",
						"Verify a slow executor stops drain when the time budget is exceeded.",
						"queued=32 executorSleepMillis=1 maxBudgetNanos=" + formatNumber(TeleportServiceSettings.MAX_TELEPORT_BUDGET_NANOS)
								+ " timeCheckInterval=" + TeleportServiceSettings.TIME_CHECK_INTERVAL,
						TeleportCoreTestSuite::testDispatcherBudgetHit),
				scenario("TeleportBatchDispatcher queue convergence",
						"Verify a large queue drains across ticks and eventually reaches zero.",
						"queued=" + formatNumber(10_000) + " maxBatchSize=" + TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK,
						TeleportCoreTestSuite::testDispatcherQueueConvergence),
				scenario("TeleportSafety fabricated main-thread workload",
						"Estimate main-thread safety cost with real BlockState collision checks on a fake BlockGetter.",
						"requests=10,000 admissionRamp=16/128 worldPattern=lateSafeOffset(3,-3,3) defaultBlock=AIR",
						TeleportCoreTestSuite::testFabricatedSafetyMainThreadWorkload),
				scenario("TeleportSafety fabricated worker workload",
						"Estimate batch safety cost when worker threads perform collision checks concurrently.",
						"requests=10,000 workerThreads=" + TeleportServiceSettings.SAFETY_WORKER_THREADS
								+ " safetyBatchSize=" + TeleportServiceSettings.SAFETY_BATCH_SIZE
								+ " admissionRamp=16/128 worldPattern=lateSafeOffset(3,-3,3)",
						TeleportCoreTestSuite::testFabricatedSafetyWorkerWorkload),
				scenario("TeleportSafety rejects tall support collision blocks",
						"Verify fences, walls, and fence gates are not accepted as standing support.",
						"targetBelow=oak_fence fallbackBelow=stone",
						TeleportCoreTestSuite::testSafetyRejectsTallSupportCollisionBlocks),
				scenario("RecordedLocationSnapshot is detached",
						"Verify recorded location snapshots do not expose mutable storage objects.",
						"initialPos=(1,2,3) mutatedSourcePos=(4,5,6) dimension=minecraft:overworld",
						TeleportCoreTestSuite::testRecordedLocationSnapshot),
				scenario("Named location rotation persists and resolves",
						"Verify named locations preserve optional yaw/pitch and old data remains compatible.",
						"rotation=(90.0,12.5) legacyRotation=absent",
						TeleportCoreTestSuite::testNamedLocationRotation),
				scenario("TPA trust rules resolve and persist",
						"Verify per-player TPA trust overrides default incoming rules and survives NBT round-trip.",
						"defaultTpa=deny requesterTpa=accept requesterTpahere=deny legacyTrust=absent",
						TeleportCoreTestSuite::testTpaTrustRules),
				scenario("Global profile load preserves unavailable-dimension warps",
						"Verify startup load does not delete global warps just because ServerLevel is not available yet.",
						"warps=1 dimension=tpc:test_dimension serverLevel=unavailable",
						TeleportCoreTestSuite::testGlobalProfileLoadPreservesUnavailableDimensionWarps),
				scenario("Player profile load preserves unavailable-dimension homes",
						"Verify profile load does not delete homes just because ServerLevel is not available yet.",
						"homes=1 dimension=tpc:test_dimension serverLevel=unavailable",
						TeleportCoreTestSuite::testPlayerProfileLoadPreservesUnavailableDimensionHomes),
				scenario("Recorded target resolver maps empty target",
						"Verify missing death/previous records map to a failed target result.",
						"input=Optional.empty expectedStatus=TARGET_UNAVAILABLE",
						TeleportCoreTestSuite::testRecordedTargetEmpty));
	}

	private static void testTargetTeleportOptions() {
		TargetTeleportOptions options = new TargetTeleportOptions(0, 0L, true, true);
		assertEquals(0, options.delayTicks(), "delay ticks should clamp");
		assertEquals(0L, options.cooldownMillis(), "cooldown millis should match constructor value");
		assertEquals(0L, options.effectiveCooldownMillis(), "effective cooldown should use configured millis");

		TargetTeleportOptions delayed = new TargetTeleportOptions(3, 100L, false, false);
		assertEquals(3, delayed.delayTicks(), "delay should use configured value");
		assertEquals(100L, delayed.effectiveCooldownMillis(), "cooldown should use configured millis");
		debug("DATA options", "clampedDelay=" + options.delayTicks()
				+ ", cooldownMillis=" + options.cooldownMillis()
				+ ", delayedDelay=" + delayed.delayTicks()
				+ ", delayedCooldownMillis=" + delayed.effectiveCooldownMillis());
	}

	private static void testPendingLifecycle() {
		TeleportOperationManager manager = new TeleportOperationManager();
		UUID playerUuid = UUID.randomUUID();
		TeleportRequest firstRequest = request();
		TeleportRequest secondRequest = request();

		TeleportOperationManager.PendingCreateResult first = manager.createPending(playerUuid, firstRequest, 10L);
		assertTrue(manager.isCurrent(playerUuid, first.pending().pendingSequence()), "first pending should be current");
		assertTrue(manager.hasCurrentOperations(), "first pending should make manager active");
		assertEquals(15L, first.pending().delayUntilTick(), "delay deadline should be based on current tick");
		debug("DATA pending.first", "sequence=" + first.pending().pendingSequence()
				+ ", createTick=" + first.pending().createTick()
				+ ", delayUntilTick=" + first.pending().delayUntilTick());

		TeleportOperationManager.PendingCreateResult second = manager.createPending(playerUuid, secondRequest, 20L);
		assertTrue(first.pending().resultFuture().isDone(), "replaced pending should complete");
		assertEquals(TeleportStatus.CANCELLED, first.pending().resultFuture().join(), "replaced pending should cancel");
		assertTrue(second.replaced().isPresent(), "replacement should be reported");
		assertFalse(manager.isCurrent(playerUuid, first.pending().pendingSequence()), "first pending should no longer be current");
		assertTrue(manager.isCurrent(playerUuid, second.pending().pendingSequence()), "second pending should be current");
		assertEquals(1, manager.visitCurrentTargetPendings(ignored -> true), "replacement should leave one active pending");
		debug("DATA pending.replace", "oldSequence=" + first.pending().pendingSequence()
				+ ", newSequence=" + second.pending().pendingSequence()
				+ ", oldStatus=" + first.pending().resultFuture().join());

		assertTrue(manager.cancelPending(playerUuid, second.pending().pendingSequence(), TeleportStatus.CANCELLED_BY_EVENT), "cancel should succeed");
		assertEquals(TeleportStatus.CANCELLED_BY_EVENT, second.pending().resultFuture().join(), "cancel status should be preserved");
		assertFalse(manager.isCurrent(playerUuid, second.pending().pendingSequence()), "cancelled pending should not be current");
		assertFalse(manager.hasCurrentOperations(), "cancelled pending should leave no active pending");
		debug("DATA pending.cancel", "sequence=" + second.pending().pendingSequence()
				+ ", status=" + second.pending().resultFuture().join());

		TeleportOperationManager.PendingCreateResult currentCancel = manager.createPending(playerUuid, request(), 25L);
		Optional<TeleportOperation> cancelledCurrent = manager.cancelCurrent(playerUuid, TeleportStatus.CANCELLED);
		assertTrue(cancelledCurrent.isPresent(), "cancelCurrent should return the removed operation");
		assertEquals(currentCancel.pending().pendingSequence(), cancelledCurrent.get().pendingSequence(),
				"cancelCurrent should remove the current operation");
		assertEquals(TeleportStatus.CANCELLED, currentCancel.pending().resultFuture().join(),
				"cancelCurrent should complete the pending future");
		assertTrue(manager.cancelCurrent(playerUuid, TeleportStatus.CANCELLED).isEmpty(),
				"cancelCurrent should be empty when no operation remains");
		assertFalse(manager.hasCurrentOperations(), "cancelCurrent should leave no active pending");
		debug("DATA pending.cancelCurrent", "sequence=" + currentCancel.pending().pendingSequence()
				+ ", status=" + currentCancel.pending().resultFuture().join());

		TeleportOperationManager.PendingCreateResult success = manager.createPending(playerUuid, request(), 30L);
		manager.markSuccess(playerUuid, success.pending().pendingSequence());
		assertFalse(manager.isCurrent(playerUuid, success.pending().pendingSequence()), "successful pending should be cleared");
		assertFalse(manager.hasCurrentOperations(), "successful pending should leave no active pending");
		long remainingCooldown = manager.getRemainingCooldownMillis(playerUuid, 10_000L);
		assertTrue(remainingCooldown > 0L, "success should refresh cooldown");
		debug("DATA pending.success", "sequence=" + success.pending().pendingSequence()
				+ ", remainingCooldownMillis=" + formatNumber(remainingCooldown));

		TeleportOperationManager.PendingCreateResult quit = manager.createPending(playerUuid, request(), 40L);
		Optional<TeleportOperation> quitPending = manager.onPlayerQuit(playerUuid, 50L);
		assertTrue(quitPending.isPresent(), "quit should return current pending");
		assertEquals(quit.pending().pendingSequence(), quitPending.get().pendingSequence(), "quit should return matching pending");
		assertEquals(TeleportStatus.CANCELLED, quit.pending().resultFuture().join(), "quit should cancel pending");
		assertFalse(manager.hasCurrentOperations(), "quit should leave no active pending");
		debug("DATA pending.quit", "sequence=" + quit.pending().pendingSequence()
				+ ", status=" + quit.pending().resultFuture().join());

		UUID targetUuid = UUID.randomUUID();
		Tpa.Session session = new Tpa.Session(UUID.randomUUID(), playerUuid, targetUuid, Tpa.Type.TPA, Long.MAX_VALUE, 2, 3_000L, true);
		TeleportOperationManager.OperationCreateResult<TpaTeleportPending> tpa = manager.createOperation(playerUuid, 60L,
				(sequence, tick) -> TpaTeleportPending.fromSession(session, sequence, tick, 2, 3_000L, true));
		assertTrue(manager.getCurrentOperation(playerUuid, TpaTeleportPending.class).isPresent(), "TPA operation should be current by type");
		assertEquals(1, manager.currentOperations(TpaTeleportPending.class).size(), "TPA operation should be listed by type");

		TeleportOperationManager.OperationCreateResult<RtpTeleportPending> rtp = manager.createOperation(playerUuid, 70L,
				(sequence, tick) -> new RtpTeleportPending(playerUuid, sequence, tick, 0, 0L, true,
						BlockPos.ZERO, Level.OVERWORLD, 4, 32, 4096));
		assertTrue(tpa.pending().resultFuture().isDone(), "replaced TPA operation should complete");
		assertEquals(TeleportStatus.CANCELLED, tpa.pending().resultFuture().join(), "replaced TPA operation should cancel");
		assertTrue(rtp.replaced().isPresent(), "RTP replacement should report old operation");
		assertTrue(manager.getCurrentOperation(playerUuid, RtpTeleportPending.class).isPresent(), "RTP operation should be current by type");
		assertEquals(0, manager.currentOperations(TpaTeleportPending.class).size(), "TPA typed list should be empty after replacement");
		assertEquals(1, manager.currentOperations(RtpTeleportPending.class).size(), "RTP typed list should contain current operation");
		debug("DATA pending.generic", "tpaSequence=" + tpa.pending().pendingSequence()
				+ ", rtpSequence=" + rtp.pending().pendingSequence()
				+ ", replacedType=" + rtp.replaced().get().getClass().getSimpleName());
	}

	private static void testVisitCurrentOperationsMutationSafety() {
		TeleportOperationManager manager = new TeleportOperationManager();
		UUID firstPlayer = UUID.randomUUID();
		UUID secondPlayer = UUID.randomUUID();
		manager.createPending(firstPlayer, request(), 1L);
		manager.createPending(secondPlayer, request(), 1L);

		int visited = manager.visitCurrentTargetPendings(pending -> {
			assertTrue(manager.cancelPending(pending.playerUuid(), pending.pendingSequence(), TeleportStatus.CANCELLED),
					"visitor should be able to cancel the current pending");
			return true;
		});

		assertEquals(2, visited, "visitor should process both pending operations");
		assertFalse(manager.hasCurrentOperations(), "all pending operations should be removed");
		debug("DATA pending.visitMutation", "visited=" + visited
				+ ", hasCurrentOperations=" + manager.hasCurrentOperations());
	}

	private static void testDispatcherFastPathThreshold() {
		TeleportBatchDispatcher dispatcher = new TeleportBatchDispatcher();
		dispatcher.beginTick();
		assertTrue(dispatcher.canUseFastPath(), "empty dispatcher should allow fast path");
		for (int i = 0; i < TeleportServiceSettings.FAST_PATH_THRESHOLD; i++) {
			assertTrue(dispatcher.canUseFastPath(), "fast path should be allowed before threshold is exhausted");
			dispatcher.noteFastPathUse();
		}
		assertFalse(dispatcher.canUseFastPath(), "fast path should stop after threshold");
		debug("DATA dispatcher.fastPath", "threshold=" + TeleportServiceSettings.FAST_PATH_THRESHOLD
				+ ", queueSize=" + dispatcher.queueSize()
				+ ", canUseFastPathAfterThreshold=" + dispatcher.canUseFastPath());
	}

	private static void testReadyAdmissionRamp() {
		int[] firstTwentyOneLimits = new int[21];
		for (int i = 0; i < firstTwentyOneLimits.length; i++) {
			firstTwentyOneLimits[i] = simulatedReadyAdmissionLimit(i + 1);
		}
		assertEquals(TeleportServiceSettings.READY_ADMISSION_FIRST_TICK_LIMIT, firstTwentyOneLimits[0], "tick 1 admission should be first-tick limit");
		int steadyLimit = TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT;
		assertEquals(steadyLimit, firstTwentyOneLimits[1], "tick 2 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[4], "tick 5 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[5], "tick 6 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[9], "tick 10 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[10], "tick 11 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[14], "tick 15 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[15], "tick 16 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[19], "tick 20 admission should be steady limit");
		assertEquals(steadyLimit, firstTwentyOneLimits[20], "tick 21 admission should be steady limit");
		debug("DATA admission.ramp", "first21Limits=[" + formatIntArray(firstTwentyOneLimits) + "]");
	}

	private static void testDispatcherHardLimit() {
		TeleportBatchDispatcher dispatcher = new TeleportBatchDispatcher();
		int queued = TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK + 20;
		for (int i = 0; i < queued; i++) {
			dispatcher.enqueue(entry(i));
		}

		TeleportBatchDispatcher.DrainResult result = dispatcher.drain(entry -> TeleportStatus.SUCCESS);
		assertEquals(TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK, result.processed(), "dispatcher should honor hard limit");
		assertEquals(20, dispatcher.queueSize(), "remaining entries should stay queued");
		debug("DATA dispatcher.hardLimit", "queued=" + queued
				+ ", processed=" + result.processed()
				+ ", remaining=" + dispatcher.queueSize()
				+ ", budgetHit=" + result.budgetHit()
				+ ", elapsedNanos=" + formatNumber(result.elapsedNanos()));
	}

	private static void testDispatcherBudgetHit() {
		TeleportBatchDispatcher dispatcher = new TeleportBatchDispatcher();
		for (int i = 0; i < 32; i++) {
			dispatcher.enqueue(entry(i));
		}

		TeleportBatchDispatcher.DrainResult result = dispatcher.drain(entry -> {
			try {
				Thread.sleep(1L);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new AssertionError(exception);
			}
			return TeleportStatus.SUCCESS;
		});

		assertTrue(result.budgetHit(), "slow executor should hit budget");
		assertTrue(result.processed() < 32, "budget hit should leave entries queued");
		debug("DATA dispatcher.budget", "queued=32"
				+ ", processed=" + result.processed()
				+ ", remaining=" + dispatcher.queueSize()
				+ ", budgetHit=" + result.budgetHit()
				+ ", elapsedNanos=" + formatNumber(result.elapsedNanos()));
	}

	private static void testDispatcherQueueConvergence() {
		TeleportBatchDispatcher dispatcher = new TeleportBatchDispatcher();
		int queued = 10_000;
		for (int i = 0; i < queued; i++) {
			dispatcher.enqueue(entry(i));
		}

		int ticks = 0;
		int totalProcessed = 0;
		int budgetHits = 0;
		long elapsedNanos = 0L;
		while (dispatcher.queueSize() > 0) {
			TeleportBatchDispatcher.DrainResult result = dispatcher.drain(entry -> TeleportStatus.SUCCESS);
			ticks++;
			totalProcessed += result.processed();
			elapsedNanos += result.elapsedNanos();
			if (result.budgetHit()) {
				budgetHits++;
			}
			assertTrue(result.processed() <= TeleportServiceSettings.MAX_BATCH_SIZE_PER_TICK, "drain should honor hard limit on every tick");
			assertTrue(result.processed() > 0, "dispatcher should make progress while queue is non-empty");
		}

		assertEquals(queued, totalProcessed, "dispatcher should process all queued entries");
		debug("DATA dispatcher.convergence", "queued=" + formatNumber(queued)
				+ ", ticks=" + ticks
				+ ", totalProcessed=" + formatNumber(totalProcessed)
				+ ", finalQueue=" + dispatcher.queueSize()
				+ ", budgetHits=" + budgetHits
				+ ", totalElapsedNanos=" + formatNumber(elapsedNanos));
	}

	private static void testFabricatedSafetyMainThreadWorkload() {
		FakeSafetyWorld world = new FakeSafetyWorld(Blocks.AIR.defaultBlockState());
		BlockPos base = new BlockPos(0, 64, 0);
		BlockPos expectedSafePos = base.offset(3, -3, 3);
		world.setBlock(expectedSafePos.below(), Blocks.STONE.defaultBlockState());

		Optional<BlockPos> singleResult = TestTeleportSafety.getSafeBlockPos(base, world);
		assertEquals(expectedSafePos, singleResult.orElseThrow(), "fabricated world should resolve the late safe offset");

		long warmupStart = System.nanoTime();
		int warmupIterations = 5_000;
		for (int i = 0; i < warmupIterations; i++) {
			if (TestTeleportSafety.getSafeBlockPos(base, world).isEmpty()) {
				throw new IllegalStateException("Warmup safety check failed");
			}
		}
		long warmupElapsed = System.nanoTime() - warmupStart;
		debug("DATA safety.main.warmup", "elapsedNanos=" + formatNumber(warmupElapsed) + ", iterations=" + formatNumber(warmupIterations));

		int requests = 10_000;
		int processed = 0;
		int ticks = 0;
		long totalSafetyElapsedNanos = 0L;
		long maxTickNanos = 0L;
		int maxTickIndex = -1;
		long readsBeforeBatch = world.blockStateReads();
		long firstTickNanos = 0L;
		long secondTickNanos = 0L;
		long thirdTickNanos = 0L;
		long lastTickNanos = 0L;
		long[] firstTwentyTickNanos = new long[20];

		while (processed < requests) {
			int batchSize = Math.min(simulatedReadyAdmissionLimit(ticks + 1), requests - processed);
			long tickStart = System.nanoTime();
			for (int i = 0; i < batchSize; i++) {
				Optional<BlockPos> safePos = TestTeleportSafety.getSafeBlockPos(base, world);
				assertEquals(expectedSafePos, safePos.orElseThrow(), "safety should resolve the same fabricated safe point");
			}
			long tickElapsed = System.nanoTime() - tickStart;
			totalSafetyElapsedNanos += tickElapsed;
			if (ticks == 0) {
				firstTickNanos = tickElapsed;
			} else if (ticks == 1) {
				secondTickNanos = tickElapsed;
			} else if (ticks == 2) {
				thirdTickNanos = tickElapsed;
			}
			if (ticks < firstTwentyTickNanos.length) {
				firstTwentyTickNanos[ticks] = tickElapsed;
			}
			lastTickNanos = tickElapsed;
			if (tickElapsed > maxTickNanos) {
				maxTickNanos = tickElapsed;
				maxTickIndex = ticks + 1;
			}
			processed += batchSize;
			ticks++;
		}

		long readsDuringBatch = world.blockStateReads() - readsBeforeBatch;
		debug("DATA safety.fabricated", "requests=" + formatNumber(requests)
				+ ", ticks=" + ticks
				+ ", processed=" + formatNumber(processed)
				+ ", blockStateReads=" + formatNumber(readsDuringBatch)
				+ ", readsPerRequest=" + formatDecimal((double) readsDuringBatch / requests)
				+ ", totalSafetyElapsedNanos=" + formatNumber(totalSafetyElapsedNanos)
				+ ", avgTickNanos=" + formatNumber(totalSafetyElapsedNanos / ticks)
				+ ", maxTickNanos=" + formatNumber(maxTickNanos)
				+ ", maxTickIndex=" + maxTickIndex
				+ ", firstTickNanos=" + formatNumber(firstTickNanos)
				+ ", secondTickNanos=" + formatNumber(secondTickNanos)
				+ ", thirdTickNanos=" + formatNumber(thirdTickNanos)
				+ ", lastTickNanos=" + formatNumber(lastTickNanos)
				+ ", first20TickNanos=[" + formatLongArray(firstTwentyTickNanos) + "]"
				+ ", avgRequestNanos=" + formatNumber(totalSafetyElapsedNanos / requests));
	}

	private static void testFabricatedSafetyWorkerWorkload() {
		FakeSafetyWorld world = new FakeSafetyWorld(Blocks.AIR.defaultBlockState());
		BlockPos base = new BlockPos(0, 64, 0);
		BlockPos expectedSafePos = base.offset(3, -3, 3);
		world.setBlock(expectedSafePos.below(), Blocks.STONE.defaultBlockState());

		Optional<BlockPos> singleResult = TestTeleportSafety.getSafeBlockPos(base, world);
		assertEquals(expectedSafePos, singleResult.orElseThrow(), "fabricated world should resolve the late safe offset");

		int requests = 10_000;
		int processed = 0;
		int ticks = 0;
		long totalTickWallNanos = 0L;
		long totalTaskNanos = 0L;
		long maxTickNanos = 0L;
		int maxTickIndex = -1;
		long firstTickNanos = 0L;
		long secondTickNanos = 0L;
		long thirdTickNanos = 0L;
		long lastTickNanos = 0L;
		long[] firstTwentyTickNanos = new long[20];
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				TeleportServiceSettings.SAFETY_WORKER_THREADS,
				TeleportServiceSettings.SAFETY_WORKER_THREADS,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				runnable -> {
					Thread thread = new Thread(runnable, "TeleportSafetyTestWorker");
					thread.setDaemon(true);
					return thread;
				});
		executor.prestartAllCoreThreads();

		long warmupStart = System.nanoTime();
		// Intensive warmup phase for safety workers, JIT compilation, and ThreadLocal
		int threads = TeleportServiceSettings.SAFETY_WORKER_THREADS;
		int warmupIterationsPerThread = 5_000;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		List<CompletableFuture<Void>> warmupFutures = new ArrayList<>(threads);
		for (int i = 0; i < threads; i++) {
			warmupFutures.add(CompletableFuture.runAsync(() -> {
				try {
					barrier.await(5, TimeUnit.SECONDS);
				} catch (Exception ignored) {
				}
				for (int j = 0; j < warmupIterationsPerThread; j++) {
					long start = System.nanoTime();
					Optional<BlockPos> safePos = TestTeleportSafety.getSafeBlockPos(base, world);
					SafetyTaskResult result = new SafetyTaskResult(safePos, System.nanoTime() - start);
					if (result.safePos().isEmpty()) {
						throw new IllegalStateException("Warmup safety check failed");
					}
				}
			}, executor));
		}
		for (CompletableFuture<Void> future : warmupFutures) {
			future.join();
		}
		long warmupElapsed = System.nanoTime() - warmupStart;
		debug("DATA safety.concurrent.warmup",
				"elapsedNanos=" + formatNumber(warmupElapsed) + ", iterationsPerThread=" + formatNumber(warmupIterationsPerThread));

		long readsBeforeBatch = world.blockStateReads();
		try {
			while (processed < requests) {
				int tickBatchSize = Math.min(simulatedReadyAdmissionLimit(ticks + 1), requests - processed);
				long tickStart = System.nanoTime();
				long tickTaskNanos = 0L;
				int remainingThisTick = tickBatchSize;
				while (remainingThisTick > 0) {
					int workerBatchSize = Math.min(TeleportServiceSettings.SAFETY_BATCH_SIZE, remainingThisTick);
					List<CompletableFuture<SafetyTaskResult>> futures = new ArrayList<>(workerBatchSize);
					for (int i = 0; i < workerBatchSize; i++) {
						futures.add(CompletableFuture.supplyAsync(() -> {
							long start = System.nanoTime();
							Optional<BlockPos> safePos = TestTeleportSafety.getSafeBlockPos(base, world);
							return new SafetyTaskResult(safePos, System.nanoTime() - start);
						}, executor));
					}

					for (CompletableFuture<SafetyTaskResult> future : futures) {
						SafetyTaskResult result = future.join();
						assertEquals(expectedSafePos, result.safePos().orElseThrow(),
								"safety should resolve the same fabricated safe point");
						tickTaskNanos += result.elapsedNanos();
					}
					remainingThisTick -= workerBatchSize;
				}
				long tickElapsed = System.nanoTime() - tickStart;
				totalTickWallNanos += tickElapsed;
				totalTaskNanos += tickTaskNanos;
				if (ticks == 0) {
					firstTickNanos = tickElapsed;
				} else if (ticks == 1) {
					secondTickNanos = tickElapsed;
				} else if (ticks == 2) {
					thirdTickNanos = tickElapsed;
				}
				if (ticks < firstTwentyTickNanos.length) {
					firstTwentyTickNanos[ticks] = tickElapsed;
				}
				lastTickNanos = tickElapsed;
				if (tickElapsed > maxTickNanos) {
					maxTickNanos = tickElapsed;
					maxTickIndex = ticks + 1;
				}
				processed += tickBatchSize;
				ticks++;
			}
		} finally {
			executor.shutdownNow();
		}

		long readsDuringBatch = world.blockStateReads() - readsBeforeBatch;
		debug("DATA safety.concurrent", "requests=" + formatNumber(requests)
				+ ", ticks=" + ticks
				+ ", processed=" + formatNumber(processed)
				+ ", workerThreads=" + TeleportServiceSettings.SAFETY_WORKER_THREADS
				+ ", safetyBatchSize=" + TeleportServiceSettings.SAFETY_BATCH_SIZE
				+ ", blockStateReads=" + formatNumber(readsDuringBatch)
				+ ", readsPerRequest=" + formatDecimal((double) readsDuringBatch / requests)
				+ ", totalTickWallNanos=" + formatNumber(totalTickWallNanos)
				+ ", totalTaskNanos=" + formatNumber(totalTaskNanos)
				+ ", avgTickWallNanos=" + formatNumber(totalTickWallNanos / ticks)
				+ ", avgRequestWallNanos=" + formatNumber(totalTickWallNanos / requests)
				+ ", avgRequestTaskNanos=" + formatNumber(totalTaskNanos / requests)
				+ ", maxTickNanos=" + formatNumber(maxTickNanos)
				+ ", maxTickIndex=" + maxTickIndex
				+ ", firstTickNanos=" + formatNumber(firstTickNanos)
				+ ", secondTickNanos=" + formatNumber(secondTickNanos)
				+ ", thirdTickNanos=" + formatNumber(thirdTickNanos)
				+ ", lastTickNanos=" + formatNumber(lastTickNanos)
				+ ", first20TickNanos=[" + formatLongArray(firstTwentyTickNanos) + "]");
	}

	private static void testSafetyRejectsTallSupportCollisionBlocks() {
		FakeSafetyWorld world = new FakeSafetyWorld(Blocks.AIR.defaultBlockState());
		BlockPos base = new BlockPos(0, 64, 0);
		BlockPos fallback = base.offset(-1, 0, 0);
		world.setBlock(base.below(), Blocks.OAK_FENCE.defaultBlockState());
		world.setBlock(base.offset(1, -1, 0), Blocks.COBBLESTONE_WALL.defaultBlockState());
		world.setBlock(base.offset(0, -1, 1), Blocks.OAK_FENCE_GATE.defaultBlockState());
		world.setBlock(fallback.below(), Blocks.STONE.defaultBlockState());

		Optional<BlockPos> result = TestTeleportSafety.getSafeBlockPos(base, world);

		assertEquals(fallback, result.orElseThrow(),
				"safety should skip tall collision supports and choose the stone fallback");
		debug("DATA safety.tallSupport", "baseBelow=oak_fence"
				+ ", wallBelowOffset=(1,-1,0)"
				+ ", gateBelowOffset=(0,-1,1)"
				+ ", fallback=" + fallback
				+ ", result=" + result.orElse(null));
	}

	private record SafetyTaskResult(Optional<BlockPos> safePos, long elapsedNanos) {
	}

	private static int simulatedReadyAdmissionLimit(int tickIndex) {
		if (tickIndex == 1) {
			return TeleportServiceSettings.READY_ADMISSION_FIRST_TICK_LIMIT;
		}
		return TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT;
	}

	private static void testRecordedLocationSnapshot() {
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse("minecraft:overworld"));
		RecordedLocation location = new RecordedLocation(new BlockPos(1, 2, 3), dimension, 180.0F, 25.0F);
		Optional<RecordedLocationView> snapshot = RecordedLocationSnapshot.optional(Optional.of(location));
		assertTrue(snapshot.isPresent(), "snapshot should be present");

		location.setBlockPos(new BlockPos(4, 5, 6));
		location.setRotation(90.0F, 10.0F);
		assertEquals(new BlockPos(1, 2, 3), snapshot.get().getBlockPos(), "snapshot should keep original block position");
		assertEquals("minecraft:overworld", snapshot.get().getDimensionId(), "dimension id should be derived from key");
		assertEquals(180.0F, snapshot.get().getYRot(), "snapshot should keep original yaw");
		assertEquals(25.0F, snapshot.get().getXRot(), "snapshot should keep original pitch");

		RecordedLocation decoded = RecordedLocationNbtCodec.fromNbt(RecordedLocationNbtCodec.toNbt(RecordedLocation.copyOf(snapshot.get())));
		TeleportTarget target = RecordedLocationTeleportTargets.toTarget(decoded, dummyWorld);
		assertEquals(180.0F, target.yRot(), "recorded target should use stored yaw");
		assertEquals(25.0F, target.xRot(), "recorded target should use stored pitch");
		debug("DATA record.snapshot", "sourcePos=" + location.getBlockPos()
				+ ", snapshotPos=" + snapshot.get().getBlockPos()
				+ ", dimensionId=" + snapshot.get().getDimensionId()
				+ ", yaw=" + snapshot.get().getYRot()
				+ ", pitch=" + snapshot.get().getXRot());
	}

	private static void testNamedLocationRotation() {
		NamedLocation location = NamedLocation.create("spawn", 10, 64.25D, -3, Level.OVERWORLD, 90.0F, 12.5F);
		NamedLocation decoded = NamedLocationNbtCodec.fromNbt(NamedLocationNbtCodec.toNbt(location));
		assertEquals(90.0F, decoded.getYRot(), "yaw should survive NBT round trip");
		assertEquals(12.5F, decoded.getXRot(), "pitch should survive NBT round trip");

		NamedLocationSnapshot snapshot = NamedLocationSnapshot.from(decoded);
		TeleportTarget target = WaypointTeleportTargets.toTarget(snapshot, dummyWorld);
		assertEquals(90.0F, target.yRot(), "target should use stored yaw");
		assertEquals(12.5F, target.xRot(), "target should use stored pitch");

		net.minecraft.nbt.CompoundTag legacyTag = NamedLocationNbtCodec.toNbt(location);
		legacyTag.remove("YRot");
		legacyTag.remove("XRot");
		NamedLocation legacyDecoded = NamedLocationNbtCodec.fromNbt(legacyTag);
		assertNull(legacyDecoded.getYRot(), "legacy data should load without yaw");
		assertNull(legacyDecoded.getXRot(), "legacy data should load without pitch");

		TeleportTarget legacyTarget = WaypointTeleportTargets.toTarget(legacyDecoded, dummyWorld);
		assertNull(legacyTarget.yRot(), "legacy target should keep current player yaw at execution time");
		assertNull(legacyTarget.xRot(), "legacy target should keep current player pitch at execution time");
		debug("DATA named.rotation", "decodedYaw=" + decoded.getYRot()
				+ ", decodedPitch=" + decoded.getXRot()
				+ ", legacyYaw=" + legacyDecoded.getYRot()
				+ ", legacyPitch=" + legacyDecoded.getXRot());
	}

	private static void testTpaTrustRules() {
		UUID ownerUuid = UUID.randomUUID();
		UUID requesterUuid = UUID.randomUUID();
		PlayerProfile profile = new PlayerProfile(ownerUuid);

		assertEquals(TpaTrustDecision.DEFAULT, profile.resolveTpaTrust(requesterUuid, Tpa.Type.TPA),
				"missing trust should use default request flow");
		profile.setDefaultTpaTrust(Tpa.Type.TPA, TpaTrustDecision.DENY);
		profile.setDefaultTpaTrust(Tpa.Type.TPAHERE, TpaTrustDecision.ACCEPT);
		assertEquals(TpaTrustDecision.DENY, profile.resolveTpaTrust(requesterUuid, Tpa.Type.TPA),
				"default TPA rule should apply");
		assertEquals(TpaTrustDecision.ACCEPT, profile.resolveTpaTrust(requesterUuid, Tpa.Type.TPAHERE),
				"default TPAHere rule should apply");

		profile.setPlayerTpaTrust(requesterUuid, Tpa.Type.TPA, TpaTrustDecision.ACCEPT);
		profile.setPlayerTpaTrust(requesterUuid, Tpa.Type.TPAHERE, TpaTrustDecision.DENY);
		assertEquals(TpaTrustDecision.ACCEPT, profile.resolveTpaTrust(requesterUuid, Tpa.Type.TPA),
				"per-player TPA rule should override default");
		assertEquals(TpaTrustDecision.DENY, profile.resolveTpaTrust(requesterUuid, Tpa.Type.TPAHERE),
				"per-player TPAHere rule should override default");

		PlayerProfile decoded = PlayerProfileNbtCodec.fromNbt(PlayerProfileNbtCodec.toNbt(profile));
		assertEquals(TpaTrustDecision.DENY, decoded.getDefaultTpaTrust(), "default TPA rule should persist");
		assertEquals(TpaTrustDecision.ACCEPT, decoded.getDefaultTpaHereTrust(), "default TPAHere rule should persist");
		assertEquals(TpaTrustDecision.ACCEPT, decoded.resolveTpaTrust(requesterUuid, Tpa.Type.TPA),
				"per-player TPA rule should persist");
		assertEquals(TpaTrustDecision.DENY, decoded.resolveTpaTrust(requesterUuid, Tpa.Type.TPAHERE),
				"per-player TPAHere rule should persist");

		profile.setPlayerTpaTrust(requesterUuid, Tpa.Type.TPA, TpaTrustDecision.DEFAULT);
		profile.setPlayerTpaTrust(requesterUuid, Tpa.Type.TPAHERE, TpaTrustDecision.DEFAULT);
		assertFalse(profile.getTpaTrustEntries().containsKey(requesterUuid), "default/default entry should be removed");

		var legacyTag = PlayerProfileNbtCodec.toNbt(profile);
		legacyTag.remove("TpaTrust");
		PlayerProfile legacyDecoded = PlayerProfileNbtCodec.fromNbt(legacyTag);
		assertEquals(TpaTrustDecision.DEFAULT, legacyDecoded.resolveTpaTrust(requesterUuid, Tpa.Type.TPA),
				"legacy profile without trust tag should use default request flow");
		debug("DATA tpa.trust", "defaultTpa=" + decoded.getDefaultTpaTrust()
				+ ", defaultTpahere=" + decoded.getDefaultTpaHereTrust()
				+ ", requesterTpa=" + decoded.resolveTpaTrust(requesterUuid, Tpa.Type.TPA)
				+ ", requesterTpahere=" + decoded.resolveTpaTrust(requesterUuid, Tpa.Type.TPAHERE));
	}

	private static void testGlobalProfileLoadPreservesUnavailableDimensionWarps() {
		GlobalProfile profile = new GlobalProfile();
		ResourceKey<Level> unavailableDimension = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse("tpc:test_dimension"));
		NamedLocation warp = NamedLocation.create("remote", 10, 64.0D, -10, unavailableDimension);

		assertTrue(profile.addWarp(warp), "test warp should be accepted");
		boolean changed = GlobalProfileLifecycle.prepareLoaded(profile);

		assertFalse(changed, "startup prepare should not treat unavailable dimensions as invalid global data");
		assertEquals(1, profile.getWarpCount(), "startup prepare should preserve global warps");
		assertTrue(profile.getWarpByName("remote").isPresent(), "preserved warp should remain indexed by name");
		debug("DATA global.load", "warps=" + profile.getWarpCount()
				+ ", dimension=" + warp.getDimensionId()
				+ ", changed=" + changed);
	}

	private static void testPlayerProfileLoadPreservesUnavailableDimensionHomes() {
		PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
		ResourceKey<Level> unavailableDimension = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse("tpc:test_dimension"));
		NamedLocation home = NamedLocation.create("remote", 10, 64.0D, -10, unavailableDimension);

		assertTrue(profile.addHome(home), "test home should be accepted");
		boolean changed = PlayerProfileLifecycle.prepareLoaded(profile);

		assertFalse(changed, "profile prepare should not treat unavailable dimensions as invalid player data");
		assertEquals(1, profile.getHomeCount(), "profile prepare should preserve homes");
		assertTrue(profile.getHomeByName("remote").isPresent(), "preserved home should remain indexed by name");
		debug("DATA player.load", "homes=" + profile.getHomeCount()
				+ ", dimension=" + home.getDimensionId()
				+ ", changed=" + changed);
	}

	private static void testRecordedTargetEmpty() {
		TeleportTargetResult result = RecordedLocationTeleportTargets.toTargetResult(Optional.empty(), null);
		assertTrue(result instanceof TeleportTargetResult.Failed, "empty record target should fail");
		TeleportTargetResult.Failed failed = (TeleportTargetResult.Failed) result;
		assertEquals(TeleportStatus.TARGET_UNAVAILABLE, failed.reason(), "empty record target should map to target unavailable");
		debug("DATA record.target.empty", "status=" + failed.reason());
	}

	private static TeleportRequest request() {
		return new TeleportRequest(
				CompletableFuture.completedFuture(TeleportTargetResult.failed(TeleportStatus.TARGET_UNAVAILABLE)),
				new TargetTeleportOptions(5, 10_000L, false, false));
	}

	private static TargetTeleportExecution entry(int id) {
		UUID playerUuid = new UUID(0L, id);
		TargetTeleportPending pending = new TargetTeleportPending(playerUuid, id,
				TeleportRequest.resolved(dummyTarget, TargetTeleportOptions.DEFAULT), 0L);
		return new TargetTeleportExecution(pending, dummyTarget);
	}

	private static TeleportTarget createDummyTarget() {
		try {
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			Unsafe unsafe = (Unsafe) unsafeField.get(null);
			return (TeleportTarget) unsafe.allocateInstance(TeleportTarget.class);
		} catch (ReflectiveOperationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static ServerLevel createDummyWorld() {
		try {
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			Unsafe unsafe = (Unsafe) unsafeField.get(null);
			return (ServerLevel) unsafe.allocateInstance(ServerLevel.class);
		} catch (ReflectiveOperationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static final class TestTeleportSafety {
		private static final int SEARCH_RADIUS = 3;
		private static final int CACHE_X_SIZE = SEARCH_RADIUS * 2 + 1;
		private static final int CACHE_Y_SIZE = SEARCH_RADIUS * 2 + 3;
		private static final int CACHE_Z_SIZE = SEARCH_RADIUS * 2 + 1;
		private static final int CACHE_Y_OFFSET = SEARCH_RADIUS + 1;
		private static final byte CACHE_UNKNOWN = 0;
		private static final byte MASK_SUPPORT = 1;
		private static final byte MASK_BODY_CLEAR = 2;
		private static final Offset[] CANDIDATE_OFFSETS = createCandidateOffsets();
		private static final ThreadLocal<SearchContext> SEARCH_CONTEXT = ThreadLocal.withInitial(SearchContext::new);
		private static final Set<Block> UNSAFE_COLLISION_FREE_BLOCKS = Set.of(
				Blocks.LAVA,
				Blocks.END_PORTAL,
				Blocks.END_GATEWAY,
				Blocks.FIRE,
				Blocks.SOUL_FIRE,
				Blocks.POWDER_SNOW,
				Blocks.NETHER_PORTAL);

		private static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, BlockGetter world) {
			SearchContext context = SEARCH_CONTEXT.get();
			context.reset(blockPos, world);
			try {
				for (Offset offset : CANDIDATE_OFFSETS) {
					if (context.isSafe(offset)) {
						return Optional.of(context.toBlockPos(offset));
					}
				}
				return Optional.empty();
			} finally {
				context.clearWorld();
			}
		}
/*
		private static void warmup() {
			//SearchContext context = SEARCH_CONTEXT.get();
			BlockGetter dummyGetter = new BlockGetter() {
				@Override
				public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
					return null;
				}

				@Override
				public BlockState getBlockState(BlockPos pos) {
					return Blocks.AIR.defaultBlockState();
				}

				@Override
				public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) {
					return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
				}

				@Override
				public int getHeight() {
					return 256;
				}

				@Override
				public int getMinY() {
					return 0;
				}
			};
			Blocks.AIR.defaultBlockState().getCollisionShape(dummyGetter, BlockPos.ZERO);
			Blocks.STONE.defaultBlockState().getCollisionShape(dummyGetter, BlockPos.ZERO);
		}
*/
		private static Offset[] createCandidateOffsets() {
			List<Offset> offsets = new ArrayList<>();
			for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
				for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
					for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
						if (x == 0 && y == 0 && z == 0) {
							offsets.add(new Offset(x, y, z));
							continue;
						}
						if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) <= SEARCH_RADIUS) {
							offsets.add(new Offset(x, y, z));
						}
					}
				}
			}

			offsets.sort(Comparator
					.comparingInt((Offset offset) -> yPriority(offset.y()))
					.thenComparingInt(Offset::horizontalDistanceSquared)
					.thenComparingInt(Offset::distanceSquared)
					.thenComparingInt(Offset::z)
					.thenComparingInt(Offset::x));
			return offsets.toArray(Offset[]::new);
		}

		private static int yPriority(int y) {
			if (y == 0) {
				return 0;
			}
			return Math.abs(y) * 2 - (y > 0 ? 1 : 0);
		}

		private static final class SearchContext {
			private int baseX;
			private int baseY;
			private int baseZ;
			private BlockGetter world;
			private final byte[] maskCache = new byte[CACHE_X_SIZE * CACHE_Y_SIZE * CACHE_Z_SIZE];
			private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

			private void reset(BlockPos blockPos, BlockGetter world) {
				this.baseX = blockPos.getX();
				this.baseY = blockPos.getY();
				this.baseZ = blockPos.getZ();
				this.world = world;
				Arrays.fill(maskCache, CACHE_UNKNOWN);
			}

			private void clearWorld() {
				this.world = null;
			}

			private boolean isSafe(Offset offset) {
				if (!hasMask(offset.x(), offset.y() - 1, offset.z(), MASK_SUPPORT)) {
					return false;
				}
				if (!hasMask(offset.x(), offset.y(), offset.z(), MASK_BODY_CLEAR)) {
					return false;
				}
				return hasMask(offset.x(), offset.y() + 1, offset.z(), MASK_BODY_CLEAR);
			}

			private boolean hasMask(int relativeX, int relativeY, int relativeZ, byte requiredMask) {
				return (getMask(relativeX, relativeY, relativeZ) & requiredMask) != 0;
			}

			private byte getMask(int relativeX, int relativeY, int relativeZ) {
				int index = cacheIndex(relativeX, relativeY, relativeZ);
				byte cached = maskCache[index];
				if (cached != CACHE_UNKNOWN) {
					return (byte) (cached - 1);
				}

				mutablePos.set(baseX + relativeX, baseY + relativeY, baseZ + relativeZ);
				BlockState state = world.getBlockState(mutablePos);
				byte mask = createMask(state);
				maskCache[index] = (byte) (mask + 1);
				return mask;
			}

			private byte createMask(BlockState state) {
				if (state.isAir() || state.getBlock() instanceof DoorBlock) {
					return MASK_BODY_CLEAR;
				}

				boolean collisionEmpty = state.getCollisionShape(world, mutablePos).isEmpty();
				byte mask = 0;

				if (state.is(Blocks.WATER)) {
					return MASK_SUPPORT | MASK_BODY_CLEAR;
				}

				if (!collisionEmpty && !isUnsafeSupport(state.getBlock())) {
					mask |= MASK_SUPPORT;
				}
				if (collisionEmpty && !UNSAFE_COLLISION_FREE_BLOCKS.contains(state.getBlock())) {
					mask |= MASK_BODY_CLEAR;
				}

				return mask;
			}

			private boolean isUnsafeSupport(Block block) {
				return block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof WallBlock;
			}

			private int cacheIndex(int relativeX, int relativeY, int relativeZ) {
				int x = relativeX + SEARCH_RADIUS;
				int y = relativeY + CACHE_Y_OFFSET;
				int z = relativeZ + SEARCH_RADIUS;
				return (y * CACHE_Z_SIZE + z) * CACHE_X_SIZE + x;
			}

			private BlockPos toBlockPos(Offset offset) {
				return new BlockPos(baseX + offset.x(), baseY + offset.y(), baseZ + offset.z());
			}
		}

		private record Offset(int x, int y, int z) {
			private int horizontalDistanceSquared() {
				return x * x + z * z;
			}

			private int distanceSquared() {
				return x * x + y * y + z * z;
			}
		}
	}

	private static final class FakeSafetyWorld implements BlockGetter {
		private final BlockState defaultBlockState;
		private final Map<BlockPos, BlockState> states = new HashMap<>();
		private final LongAdder blockStateReads = new LongAdder();

		private FakeSafetyWorld(BlockState defaultBlockState) {
			this.defaultBlockState = defaultBlockState;
		}

		private void setBlock(BlockPos pos, BlockState state) {
			states.put(pos.immutable(), state);
		}

		private long blockStateReads() {
			return blockStateReads.sum();
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return null;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			blockStateReads.increment();
			return states.getOrDefault(pos, defaultBlockState);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return getBlockState(pos).getFluidState();
		}

		@Override
		public int getHeight() {
			return 384;
		}

		@Override
		public int getMinY() {
			return -64;
		}
	}
}
