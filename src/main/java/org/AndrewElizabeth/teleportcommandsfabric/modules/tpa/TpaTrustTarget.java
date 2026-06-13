package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.UUID;

record TpaTrustTarget(UUID playerUuid, String displayName, boolean all) {
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
