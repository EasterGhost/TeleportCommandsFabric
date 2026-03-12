package org.AndrewElizabeth.teleportcommandsfabric.utils;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
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

	/**
	 * Teleport with delay and cooldown checks.
	 *
	 * @param player Player to teleport
	 * @param world Target world
	 * @param coords Target coordinates
	 * @param bypassDelay If true, skip delay but still check cooldown
	 * @return true if teleport was initiated, false if on cooldown
	 */
	public static boolean teleportWithDelayAndCooldown(ServerPlayer player, ServerLevel world, Vec3 coords,
			boolean bypassDelay) {
		String uuid = player.getStringUUID();
		int delay = ConfigManager.CONFIG.getTeleporting().getDelay();
		int cooldown = ConfigManager.CONFIG.getTeleporting().getCooldown();

		int remainingCooldown = TeleportCooldownManager.getRemainingCooldown(uuid, cooldown);
		if (remainingCooldown > 0) {
			player.displayClientMessage(getTranslatedText("commands.teleport_commands.common.cooldown", player,
					Component.literal(String.valueOf(remainingCooldown)))
					.withStyle(ChatFormatting.RED), true);
			return false;
		}

		if (delay == 0 || bypassDelay) {
			teleportWithManagedPreload(player, world, coords);
			TeleportCooldownManager.updateLastTeleportTime(uuid);
			return true;
		}

		long teleportId = TeleportCooldownManager.scheduleTeleport(uuid);
		player.displayClientMessage(getTranslatedText("commands.teleport_commands.common.delayStart", player,
				Component.literal(String.valueOf(delay)))
				.withStyle(ChatFormatting.YELLOW), true);

		TELEPORT_SCHEDULER.schedule(() -> {
			if (!TeleportCooldownManager.isScheduledTeleportValid(uuid, teleportId)) {
				return;
			}
			if (player.hasDisconnected()) {
				TeleportCooldownManager.cancelScheduledTeleport(uuid);
				return;
			}
			if (TeleportCommands.SERVER == null) {
				return;
			}
			TeleportCommands.SERVER.execute(() -> {
				if (!TeleportCooldownManager.isScheduledTeleportValid(uuid, teleportId)) {
					return;
				}
				if (player.hasDisconnected()) {
					TeleportCooldownManager.cancelScheduledTeleport(uuid);
					return;
				}

				teleportWithManagedPreload(player, world, coords);
				TeleportCooldownManager.updateLastTeleportTime(uuid);
				TeleportCooldownManager.cancelScheduledTeleport(uuid);
			});
		}, delay, TimeUnit.SECONDS);

		return true;
	}

	private static void teleportWithManagedPreload(ServerPlayer player, ServerLevel world, Vec3 coords) {
		ConfigManager.ConfigClass.Teleporting teleportConfig = ConfigManager.CONFIG.getTeleporting();
		boolean preloadEnabled = teleportConfig.isPreloadEnabled();
		int radiusChunks = Math.max(0, teleportConfig.getPreloadRadiusChunks());
		if (!preloadEnabled) {
			teleport(player, world, coords);
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
				teleport(player, world, coords);
			});
		}, ONE_TICK_MS, TimeUnit.MILLISECONDS);
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

	/**
	 * Immediate teleport without cooldown or delay checks.
	 */
	public static void teleport(ServerPlayer player, ServerLevel world, Vec3 coords) {
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
			return;
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
	}
}
