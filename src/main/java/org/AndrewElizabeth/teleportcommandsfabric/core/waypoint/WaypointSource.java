package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public interface WaypointSource {
	record CreateFailure(String messageKey, ChatFormatting formatting, Object[] args) {
		public static CreateFailure of(String messageKey, ChatFormatting formatting, Object... args) {
			return new CreateFailure(messageKey, formatting, args);
		}
	}

	List<NamedLocation> getAll();

	Optional<NamedLocation> getByName(String name);

	void add(NamedLocation location) throws Exception;

	void remove(NamedLocation location) throws Exception;

	int getMaxLimit();

	boolean isEnabled();

	default Optional<CreateFailure> validateCreate(ServerPlayer player, String normalizedName) {
		return Optional.empty();
	}

	default NamedLocation createLocation(ServerPlayer player, String normalizedName) {
		return NamedLocation.create(normalizedName, player.getBlockX(), player.getY(), player.getBlockZ(),
				WorldResolver.getDimensionId(player.level().dimension()));
	}

	default void setDefault(NamedLocation location) throws Exception {
	}

	default boolean isDefault(NamedLocation location) {
		return false;
	}

	default void onAdded(NamedLocation location) {
	}

	default void onRemoved(NamedLocation location) {
	}

	default void onRenamed(NamedLocation location, String oldName) {
	}
}
