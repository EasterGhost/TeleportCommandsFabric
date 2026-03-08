package org.AndrewElizabeth.teleportcommandsfabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.TeleporterWithDelayAndCooldown;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.getTranslatedText;

public class rtp {

	private static final int MAX_ATTEMPTS = 4096;
	private static final int ATTEMPTS_PER_TICK = 32;
	private static final Map<UUID, RtpSearchJob> SEARCH_JOBS = new ConcurrentHashMap<>();
	private static boolean tickHookRegistered = false;

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		initTickHook();
		commandDispatcher.register(tpc$buildRtpCommand("rtp"));
		commandDispatcher.register(tpc$buildRtpCommand("wild"));
	}

	private static void initTickHook() {
		if (tickHookRegistered) {
			return;
		}
		tickHookRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(rtp::onServerTick);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> tpc$buildRtpCommand(String commandName) {
		return Commands.literal(commandName)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!ConfigManager.CONFIG.getRtp().isEnabled()) {
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.rtp.disabled", player)
										.withStyle(ChatFormatting.RED),
								true);
						return 1;
					}

					int radius = ConfigManager.CONFIG.getRtp().getRadius();
					if (radius < ConfigManager.ConfigClass.Rtp.MIN_RADIUS
							|| radius > ConfigManager.ConfigClass.Rtp.MAX_RADIUS) {
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.rtp.invalidRadius", player)
										.withStyle(ChatFormatting.RED),
								true);
						return 1;
					}

					try {
						return enqueueRandomTeleport(player, radius);
					} catch (Exception e) {
						Constants.LOGGER.error("Error while executing /rtp!", e);
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.common.error", player)
										.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
								true);
						return 1;
					}
				});
	}

	private static int enqueueRandomTeleport(ServerPlayer player, int radius) {
		SEARCH_JOBS.put(player.getUUID(), new RtpSearchJob(player, player.blockPosition().immutable(), radius, MAX_ATTEMPTS));
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

			ServerLevel world = player.level();
			int budget = Math.min(ATTEMPTS_PER_TICK, job.remainingAttempts());
			Optional<BlockPos> safePos = findSafeRandomPosition(world, job.center(), job.radius(), world.random, budget);
			if (safePos.isPresent()) {
				BlockPos blockPos = safePos.get();
				Vec3 teleportPos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
				player.displayClientMessage(getTranslatedText("commands.teleport_commands.rtp.go", player), true);
				TeleporterWithDelayAndCooldown(player, world, teleportPos, false);
				SEARCH_JOBS.remove(uuid);
				continue;
			}

			int remaining = job.remainingAttempts() - budget;
			if (remaining <= 0) {
				player.displayClientMessage(
						getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
								.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						true);
				SEARCH_JOBS.remove(uuid);
				continue;
			}

			SEARCH_JOBS.put(uuid, new RtpSearchJob(player, job.center(), job.radius(), remaining));
		}
	}

	private static Optional<BlockPos> findSafeRandomPosition(ServerLevel world, BlockPos center, int radius,
			RandomSource random, int attemptBudget) {
		int minY = world.getMinY() + 1;
		int maxY = world.getMaxY();
		int r2 = radius * radius;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headTopPos = new BlockPos.MutableBlockPos();
		int centerX = center.getX();
		int centerY = center.getY();
		int centerZ = center.getZ();

		for (int attempt = 0; attempt < attemptBudget; attempt++) {
			int dx = random.nextInt(radius * 2 + 1) - radius;
			int dz = random.nextInt(radius * 2 + 1) - radius;
			int horizontalR2 = dx * dx + dz * dz;
			if (horizontalR2 >= r2) {
				continue;
			}

			int x = centerX + dx;
			int z = centerZ + dz;

			int yMin = Math.max(minY, centerY - radius + 1);
			int yMax = Math.min(maxY, centerY + radius);
			yMax = Math.min(yMax, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
			if (yMin > yMax) {
				continue;
			}

			int dy = (yMin + random.nextInt(yMax - yMin + 1)) - centerY;
			if (horizontalR2 + dy * dy > r2) {
				continue;
			}
			int y = centerY + dy;

			pos.set(x, y, z);
			if (isSafeTeleportPos(world, pos, belowPos, headPos, headTopPos)) {
				return Optional.of(pos.immutable());
			}
		}

		return Optional.empty();
	}

	private static boolean isSafeTeleportPos(ServerLevel world, BlockPos pos, BlockPos.MutableBlockPos belowPos,
			BlockPos.MutableBlockPos headPos, BlockPos.MutableBlockPos headTopPos) {
		belowPos.set(pos.getX(), pos.getY() - 1, pos.getZ());
		BlockState belowState = world.getBlockState(belowPos);
		if (belowState.isAir() || !belowState.getFluidState().isEmpty()) {
			return false;
		}
		if (belowState.getCollisionShape(world, belowPos).isEmpty()) {
			return false;
		}

		BlockState feetState = world.getBlockState(pos);
		headPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
		BlockState headState = world.getBlockState(headPos);
		headTopPos.set(pos.getX(), pos.getY() + 2, pos.getZ());
		BlockState headTopState = world.getBlockState(headTopPos);
		return feetState.isAir() && headState.isAir() && headTopState.isAir();
	}

	private record RtpSearchJob(ServerPlayer player, BlockPos center, int radius, int remainingAttempts) {
	}
}
