package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.UUID;

record TpaTrustTarget(UUID playerUuid, String displayName, boolean all) {
	TpaTrustTarget {
		if (all) {
			playerUuid = null;
			displayName = "all";
		} else {
			if (playerUuid == null) {
				throw new IllegalArgumentException("playerUuid must not be null for player trust target");
			}
			if (displayName == null || displayName.isBlank()) {
				displayName = playerUuid.toString();
			}
		}
	}

	static TpaTrustTarget allPlayers() {
		return new TpaTrustTarget(null, "all", true);
	}

	static TpaTrustTarget player(ServerPlayer player) {
		return new TpaTrustTarget(player.getUUID(), player.getName().getString(), false);
	}

	static TpaTrustTarget player(NameAndId profile) {
		return new TpaTrustTarget(profile.id(), profile.name(), false);
	}
}
