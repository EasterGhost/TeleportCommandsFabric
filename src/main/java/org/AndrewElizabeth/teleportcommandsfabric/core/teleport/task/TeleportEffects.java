package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class TeleportEffects {
	private static volatile boolean enabled = true;

	private TeleportEffects() {
	}

	public static void setEnabled(boolean enabled) {
		TeleportEffects.enabled = enabled;
	}

	static boolean isEnabled() {
		return enabled;
	}

	static void playBefore(ServerPlayer player) {
		if (player.level() instanceof ServerLevel world) {
			playParticles(world, player.getX(), player.getY(), player.getZ(), true);
			playSound(world, player);
		}
	}

	static void playAfter(ServerPlayer player) {
		if (player.level() instanceof ServerLevel world) {
			playSound(world, player);
			playParticles(world, player.getX(), player.getY(), player.getZ(), false);
		}
	}

	private static void playParticles(ServerLevel world, double x, double y, double z, boolean beforeTeleport) {
		world.sendParticles(ParticleTypes.SNOWFLAKE, x, y + (beforeTeleport ? 1.0D : 0.0D), z, 20, 0.0D, beforeTeleport ? 0.0D : 1.0D, 0.0D, 0.01D);
		world.sendParticles(ParticleTypes.WHITE_SMOKE, x, y, z, 15, 0.0D, beforeTeleport ? 1.0D : 0.0D, 0.0D, 0.03D);
	}

	private static void playSound(ServerLevel world, ServerPlayer player) {
		world.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4F, 1.0F);
	}
}
