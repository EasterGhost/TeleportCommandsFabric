package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class RtpService {

	private static final int MAX_ATTEMPTS = 4096;
	private static final int ATTEMPTS_PER_TICK = 256;
	private static final Map<UUID, RtpSearchJob> SEARCH_JOBS = new HashMap<>();
	private static boolean tickHookRegistered = false;

	private RtpService() {
	}

	public static void initialize() {
		if (tickHookRegistered) {
			return;
		}
		tickHookRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(RtpService::onServerTick);
	}

	public static int enqueueRandomTeleport(ServerPlayer player, int maxRadius, int minRadius) {
		SEARCH_JOBS.put(player.getUUID(), new RtpSearchJob(player, player.blockPosition().immutable(),
				player.level().dimension(), maxRadius, minRadius, MAX_ATTEMPTS));
		return 0;
	}

	private static void onServerTick(MinecraftServer server) {
		if (SEARCH_JOBS.isEmpty()) {
			return;
		}

		for (RtpSearchJob job : new ArrayList<>(SEARCH_JOBS.values())) {
			ServerPlayer player = job.player();
			UUID uuid = player.getUUID();
			if (player.hasDisconnected()) {
				SEARCH_JOBS.remove(uuid);
				continue;
			}
			if (!player.level().dimension().equals(job.centerDimension())) {
				SEARCH_JOBS.remove(uuid);
				continue;
			}

			ServerLevel world = player.level();
			int budget = Math.min(ATTEMPTS_PER_TICK, job.remainingAttempts());
			Optional<BlockPos> safePos = findSafeRandomPosition(world, job.center(), job.maxRadius(), job.minRadius(),
					world.getRandom(), budget);
			if (safePos.isPresent()) {
				BlockPos blockPos = safePos.get();
				Vec3 teleportPos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
				if (TeleportService.teleportWithDelayAndCooldown(player, world, teleportPos, false)) {
					player.sendSystemMessage(getTranslatedText("commands.teleport_commands.rtp.go", player), true);
				}
				SEARCH_JOBS.remove(uuid);
				continue;
			}

			int remaining = job.remainingAttempts() - budget;
			if (remaining <= 0) {
				player.sendSystemMessage(
						getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
								.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						true);
				SEARCH_JOBS.remove(uuid);
				continue;
			}

			SEARCH_JOBS.put(uuid,
					new RtpSearchJob(player, job.center(), job.centerDimension(), job.maxRadius(), job.minRadius(), remaining));
		}
	}

	private static Optional<BlockPos> findSafeRandomPosition(ServerLevel world, BlockPos center, int maxRadius,
			int minRadius, RandomSource random, int attemptBudget) {
		int minY = world.getMinY() + 1;
		int maxY = world.getMaxY();
		int maxR2 = maxRadius * maxRadius;
		int minR2 = minRadius * minRadius;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headTopPos = new BlockPos.MutableBlockPos();
		int centerX = center.getX();
		int centerY = center.getY();
		int centerZ = center.getZ();
		boolean restrictNetherRoofBedrock = world.dimension().equals(Level.NETHER) && centerY < 128;

		for (int attempt = 0; attempt < attemptBudget; attempt++) {
			int dx = random.nextInt(maxRadius * 2 + 1) - maxRadius;
			int dz = random.nextInt(maxRadius * 2 + 1) - maxRadius;
			int horizontalR2 = dx * dx + dz * dz;
			if (horizontalR2 > maxR2) {
				continue;
			}

			int x = centerX + dx;
			int z = centerZ + dz;

			int yMin = Math.max(minY, centerY - maxRadius + 1);
			int yMax = Math.min(maxY, centerY + maxRadius);
			yMax = Math.min(yMax, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
			if (yMin > yMax) {
				continue;
			}

			int dy = (yMin + random.nextInt(yMax - yMin + 1)) - centerY;
			int distance2 = horizontalR2 + dy * dy;
			if (distance2 > maxR2 || distance2 < minR2) {
				continue;
			}
			int y = centerY + dy;

			pos.set(x, y, z);
			if (isSafeTeleportPos(world, pos, belowPos, headPos, headTopPos, restrictNetherRoofBedrock)) {
				return Optional.of(pos.immutable());
			}
		}

		return Optional.empty();
	}

	private static boolean isSafeTeleportPos(ServerLevel world, BlockPos pos, BlockPos.MutableBlockPos belowPos,
			BlockPos.MutableBlockPos headPos, BlockPos.MutableBlockPos headTopPos, boolean restrictNetherRoofBedrock) {
		belowPos.set(pos.getX(), pos.getY() - 1, pos.getZ());
		BlockState belowState = world.getBlockState(belowPos);
		if (restrictNetherRoofBedrock && belowPos.getY() == 127 && belowState.is(Blocks.BEDROCK)) {
			return false;
		}
		if (belowState.isAir() || !belowState.getFluidState().isEmpty()) {
			return false;
		}
		if (belowState.getCollisionShape(world, belowPos).isEmpty()) {
			return false;
		}

		BlockState feetState = world.getBlockState(pos);
		headPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
		BlockState headState = world.getBlockState(headPos);
		return feetState.isAir() && headState.isAir();
	}

	private record RtpSearchJob(ServerPlayer player, BlockPos center, ResourceKey<Level> centerDimension, int maxRadius,
			int minRadius, int remainingAttempts) {
	}
}
