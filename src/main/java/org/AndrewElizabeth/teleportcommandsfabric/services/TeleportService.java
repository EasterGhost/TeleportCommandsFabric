package org.AndrewElizabeth.teleportcommandsfabric.services;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigClass;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.PreviousTeleportLocationStorage;
import org.AndrewElizabeth.teleportcommandsfabric.storage.TeleportCooldownManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class TeleportService {
	private static final TicketType PRELOAD_TICKET_TYPE = new TicketType(
			2L,
			TicketType.FLAG_LOADING | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED);
	private static final int PRELOAD_TICKET_LEVEL = 31;
	private static final long ONE_TICK_MS = 50L;

	private static final ScheduledExecutorService TELEPORT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(
			runnable -> {
				Thread thread = new Thread(runnable, "teleportcommands-teleport-delay");
				thread.setDaemon(true);
				return thread;
			});

	private TeleportService() {
	}

	public static boolean teleportWithDelayAndCooldown(ServerPlayer player, ServerLevel world, Vec3 coords,
			boolean bypassDelay) {
		return teleportWithDelayAndCooldown(player, world, coords, bypassDelay, null, true);
	}

	public static boolean teleportWithDelayAndCooldown(ServerPlayer player, ServerLevel world, Vec3 coords,
			boolean bypassDelay, Runnable onSuccess) {
		return teleportWithDelayAndCooldown(player, world, coords, bypassDelay, onSuccess, true);
	}

	public static boolean teleportWithDelayAndCooldown(ServerPlayer player, ServerLevel world, Vec3 coords,
			boolean bypassDelay, Runnable onSuccess, boolean recordPreviousLocation) {
		UUID playerUuid = player.getUUID();
		int delay = ConfigManager.CONFIG.getTeleporting().getDelay();
		int cooldown = ConfigManager.CONFIG.getTeleporting().getCooldown();

		int remainingCooldown = TeleportCooldownManager.getRemainingCooldown(playerUuid, cooldown);
		if (remainingCooldown > 0) {
			player.sendSystemMessage(getTranslatedText("commands.teleport_commands.common.cooldown", player,
					Component.literal(String.valueOf(remainingCooldown)))
					.withStyle(ChatFormatting.RED), true);
			return false;
		}

		if (delay == 0 || bypassDelay) {
			recordPreviousTeleportLocation(player, recordPreviousLocation);
			teleportWithManagedPreload(player, world, coords, () -> {
				TeleportCooldownManager.updateLastTeleportTime(playerUuid);
				runOnSuccess(onSuccess);
			});
			return true;
		}

		long teleportId = TeleportCooldownManager.scheduleTeleport(playerUuid);
		player.sendSystemMessage(getTranslatedText("commands.teleport_commands.common.delayStart", player,
				Component.literal(String.valueOf(delay)))
				.withStyle(ChatFormatting.YELLOW), true);

		TELEPORT_SCHEDULER.schedule(() -> {
			if (!TeleportCooldownManager.isScheduledTeleportValid(playerUuid, teleportId)) {
				return;
			}
			if (player.hasDisconnected()) {
				TeleportCooldownManager.cancelScheduledTeleport(playerUuid);
				return;
			}
			if (TeleportCommands.SERVER == null) {
				return;
			}
			TeleportCommands.SERVER.execute(() -> {
				if (!TeleportCooldownManager.isScheduledTeleportValid(playerUuid, teleportId)) {
					return;
				}
				if (player.hasDisconnected()) {
					TeleportCooldownManager.cancelScheduledTeleport(playerUuid);
					return;
				}

				recordPreviousTeleportLocation(player, recordPreviousLocation);
				teleportWithManagedPreload(player, world, coords, () -> {
					TeleportCooldownManager.updateLastTeleportTime(playerUuid);
					runOnSuccess(onSuccess);
				});
				TeleportCooldownManager.cancelScheduledTeleport(playerUuid);
			});
		}, delay, TimeUnit.SECONDS);

		return true;
	}

	private static void recordPreviousTeleportLocation(ServerPlayer player, boolean recordPreviousLocation) {
		if (!recordPreviousLocation) {
			return;
		}

		UUID playerUuid = player.getUUID();
		BlockPos pos = player.blockPosition();
		String worldId = org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver
				.getDimensionId(player.level().dimension());
		PreviousTeleportLocationStorage.setPreviousTeleportLocation(playerUuid, pos, worldId);
	}

	private static void teleportWithManagedPreload(ServerPlayer player, ServerLevel world, Vec3 coords,
			Runnable onSuccess) {
		ConfigClass.Teleporting teleportConfig = ConfigManager.CONFIG.getTeleporting();
		boolean preloadEnabled = teleportConfig.isPreloadEnabled();
		int radiusChunks = Math.max(0, teleportConfig.getPreloadRadiusChunks());
		if (!preloadEnabled) {
			finishSuccessfulTeleport(teleport(player, world, coords), onSuccess);
			return;
		}

		List<ChunkPos> chunks = collectChunkSquare(BlockPos.containing(coords), radiusChunks);
		issuePreloadTickets(world, chunks);

		TELEPORT_SCHEDULER.schedule(() -> {
			if (TeleportCommands.SERVER == null) {
				return;
			}
			TeleportCommands.SERVER.execute(() -> {
				if (player.hasDisconnected()) {
					return;
				}
				finishSuccessfulTeleport(teleport(player, world, coords), onSuccess);
			});
		}, ONE_TICK_MS, TimeUnit.MILLISECONDS);
	}

	private static void finishSuccessfulTeleport(boolean success, Runnable onSuccess) {
		if (success && onSuccess != null) {
			runOnSuccess(onSuccess);
		}
	}

	private static void runOnSuccess(Runnable onSuccess) {
		if (onSuccess != null) {
			onSuccess.run();
		}
	}

	private static List<ChunkPos> collectChunkSquare(BlockPos centerPos, int radiusChunks) {
		ChunkPos center = new ChunkPos(centerPos);
		List<ChunkPos> result = new ArrayList<>((radiusChunks * 2 + 1) * (radiusChunks * 2 + 1));
		for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
			for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
				result.add(new ChunkPos(center.x + dx, center.z + dz));
			}
		}
		return result;
	}

	private static void issuePreloadTickets(ServerLevel world, List<ChunkPos> chunks) {
		for (ChunkPos chunk : chunks) {
			world.getChunkSource().addTicket(new Ticket(PRELOAD_TICKET_TYPE, PRELOAD_TICKET_LEVEL), chunk);
			world.getChunk(chunk.x, chunk.z);
		}
	}

	public static boolean teleport(ServerPlayer player, ServerLevel world, Vec3 coords) {
		if (player.hasDisconnected()) {
			return false;
		}

		boolean crossDimension = player.level() != world;

		world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.0D, 0.0D,
				0.0D, 0.01);
		world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 1.0D,
				0.0D, 0.03);
		world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()),
				SoundSource.PLAYERS, 0.4f, 1.0f);

		boolean flying = player.getAbilities().flying;
		player.teleportTo(world, coords.x, coords.y, coords.z, Set.of(), player.getYRot(), player.getXRot(), false);

		if (flying) {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		}

		world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()),
				SoundSource.PLAYERS, 0.4f, 1.0f);

		if (!crossDimension) {
			world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY(), player.getZ(), 20,
					0.0D, 1.0D, 0.0D, 0.01);
			world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15,
					0.0D, 0.0D, 0.0D, 0.03);
			return true;
		}

		TELEPORT_SCHEDULER.schedule(() -> {
			if (TeleportCommands.SERVER == null) {
				return;
			}
			TeleportCommands.SERVER.execute(() -> {
				if (player.hasDisconnected() || !(player.level() instanceof ServerLevel currentWorld)) {
					return;
				}

				currentWorld.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY(), player.getZ(), 20,
						0.0D, 1.0D, 0.0D, 0.01);
				currentWorld.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15,
						0.0D, 0.0D, 0.0D, 0.03);
			});
		}, 100, TimeUnit.MILLISECONDS);
		return true;
	}
}
