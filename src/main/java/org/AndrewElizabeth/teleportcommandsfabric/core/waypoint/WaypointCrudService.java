package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.CommandExecutionSupport;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class WaypointCrudService {

	private WaypointCrudService() {
	}

	public static int set(ServerPlayer player, String name, WaypointSource source, String logPrefix, String errorKey,
			String successKey, String existsKey, String maxReachedKey) {

		return CommandExecutionSupport.execute(player, logPrefix, errorKey, () -> {
			final String normalizedName = LocationResolver.normalizeName(name);
			final int max = source.getMaxLimit();
			final boolean alreadyExists = source.getByName(normalizedName).isPresent();
			final Optional<WaypointSource.CreateFailure> createFailure;

			if (alreadyExists) {
				CommandExecutionSupport.send(player, existsKey, ChatFormatting.RED);
				return;
			}

			createFailure = source.validateCreate(player, normalizedName);
			if (createFailure.isPresent()) {
				sendCreateFailure(player, createFailure.get());
				return;
			}

			if (max > 0 && source.getAll().size() >= max) {
				CommandExecutionSupport.sendWithArgs(player, maxReachedKey, ChatFormatting.RED, String.valueOf(max));
				return;
			}

			final NamedLocation location = source.createLocation(player, normalizedName);

			source.add(location);
			source.onAdded(location);
			CommandExecutionSupport.send(player, successKey);
		});
	}

	private static void sendCreateFailure(ServerPlayer player, WaypointSource.CreateFailure failure) {
		if (failure.args().length == 0) {
			CommandExecutionSupport.send(player, failure.messageKey(), failure.formatting());
			return;
		}
		CommandExecutionSupport.sendWithArgs(player, failure.messageKey(), failure.formatting(), failure.args());
	}

	public static int delete(ServerPlayer player, String name, WaypointSource source, String logPrefix, String errorKey,
			String successKey, String notFoundKey) {

		return CommandExecutionSupport.execute(player, logPrefix, errorKey, () -> {
			Optional<NamedLocation> location = source.getByName(name);
			if (location.isEmpty()) {
				CommandExecutionSupport.send(player, notFoundKey, ChatFormatting.RED);
				return;
			}

			source.remove(location.get());
			source.onRemoved(location.get());
			CommandExecutionSupport.send(player, successKey);
		});
	}

	public static int update(ServerPlayer player, String name, WaypointSource source, String logPrefix, String errorKey,
			String successKey, String notFoundKey, String sameLocationKey) {

		return CommandExecutionSupport.execute(player, logPrefix, errorKey, () -> {
			Optional<NamedLocation> location = source.getByName(name);
			if (location.isEmpty()) {
				CommandExecutionSupport.send(player, notFoundKey, ChatFormatting.RED);
				return;
			}

			final String worldString = WorldResolver.getDimensionId(player.level().dimension());
			if (player.blockPosition().equals(location.get().getBlockPos())
					&& worldString.equals(location.get().getWorldString())) {
				CommandExecutionSupport.send(player, sameLocationKey, ChatFormatting.AQUA);
				return;
			}

			location.get().setCoordinates(player.getBlockX(), player.getY(), player.getBlockZ(), worldString);

			source.onRenamed(location.get(), location.get().getName());
			CommandExecutionSupport.send(player, successKey);
		});
	}

	public static int rename(ServerPlayer player, String name, String newName, WaypointSource source, String logPrefix,
			String errorKey, String successKey, String notFoundKey, String nameExistsKey) {

		return CommandExecutionSupport.execute(player, logPrefix, errorKey, () -> {
			final String normalizedNewName = LocationResolver.normalizeName(newName);

			if (source.getByName(normalizedNewName).isPresent()) {
				CommandExecutionSupport.send(player, nameExistsKey, ChatFormatting.RED);
				return;
			}

			Optional<NamedLocation> location = source.getByName(name);
			if (location.isEmpty()) {
				CommandExecutionSupport.send(player, notFoundKey, ChatFormatting.RED);
				return;
			}

			String oldName = location.get().getName();
			location.get().setName(normalizedNewName);
			source.onRenamed(location.get(), oldName);
			CommandExecutionSupport.send(player, successKey);
		});
	}

	public static int setDefault(ServerPlayer player, String name, WaypointSource source, String logPrefix,
			String errorKey, String successKey, String notFoundKey, String sameDefaultKey) {

		return CommandExecutionSupport.execute(player, logPrefix, errorKey, () -> {
			Optional<NamedLocation> location = source.getByName(name);
			if (location.isEmpty()) {
				CommandExecutionSupport.send(player, notFoundKey, ChatFormatting.RED);
				return;
			}

			if (source.isDefault(location.get())) {
				CommandExecutionSupport.send(player, sameDefaultKey, ChatFormatting.AQUA);
				return;
			}

			source.setDefault(location.get());
			CommandExecutionSupport.send(player, successKey);
		});
	}
}
