package org.AndrewElizabeth.teleportcommandsfabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import net.minecraft.util.RandomSource;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.getTranslatedText;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.TeleporterWithDelayAndCooldown;

public class wild {

	private static final int MAX_ATTEMPTS = 64;

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(Commands.literal("wild")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> {
					final ServerPlayer player = context.getSource().getPlayerOrException();

					if (!ConfigManager.CONFIG.getWild().isEnabled()) {
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.wild.disabled", player)
										.withStyle(ChatFormatting.RED),
								true);
						return 1;
					}

					int radius = ConfigManager.CONFIG.getWild().getRadius();
					if (radius < 1) {
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.wild.invalidRadius", player)
										.withStyle(ChatFormatting.RED),
								true);
						return 1;
					}

					try {
						return randomTeleport(player, radius);
					} catch (Exception e) {
						Constants.LOGGER.error("Error while executing /wild!", e);
						player.displayClientMessage(
								getTranslatedText("commands.teleport_commands.common.error", player)
										.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
								true);
						return 1;
					}
				}));
	}

	private static int randomTeleport(ServerPlayer player, int radius) {
		ServerLevel world = player.level();
		Optional<BlockPos> safePos = findSafeRandomPosition(world, player.blockPosition(), radius, world.random);

		if (safePos.isEmpty()) {
			player.displayClientMessage(
					getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					true);
			return 1;
		}

		BlockPos blockPos = safePos.get();
		Vec3 teleportPos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
		player.displayClientMessage(getTranslatedText("commands.teleport_commands.wild.go", player), true);
		TeleporterWithDelayAndCooldown(player, world, teleportPos, false);
		return 0;
	}

	private static Optional<BlockPos> findSafeRandomPosition(ServerLevel world, BlockPos center, int radius,
			RandomSource random) {
		int minY = world.getMinY() + 1;
		int maxY = world.getMaxY() - 2;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			int[] offset = randomOffsetInSphere(random, radius);
			int x = center.getX() + offset[0];
			int y = center.getY() + offset[1];
			int z = center.getZ() + offset[2];

			if (y < minY || y > maxY) {
				continue;
			}

			BlockPos pos = new BlockPos(x, y, z);
			if (isSafeTeleportPos(world, pos)) {
				return Optional.of(pos);
			}
		}

		return Optional.empty();
	}

	private static int[] randomOffsetInSphere(RandomSource random, int radius) {
		int r2 = radius * radius;
		while (true) {
			int x = random.nextInt(radius * 2 + 1) - radius;
			int y = random.nextInt(radius * 2 + 1) - radius;
			int z = random.nextInt(radius * 2 + 1) - radius;
			if (x * x + y * y + z * z <= r2) {
				return new int[] { x, y, z };
			}
		}
	}

	private static boolean isSafeTeleportPos(ServerLevel world, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState belowState = world.getBlockState(below);
		if (belowState.isAir() || !belowState.getFluidState().isEmpty()) {
			return false;
		}
		if (belowState.getCollisionShape(world, below).isEmpty()) {
			return false;
		}

		BlockState feetState = world.getBlockState(pos);
		BlockState headState = world.getBlockState(pos.above());
		BlockState headTopState = world.getBlockState(pos.above(2));
		return feetState.isAir() && headState.isAir() && headTopState.isAir();
	}
}
