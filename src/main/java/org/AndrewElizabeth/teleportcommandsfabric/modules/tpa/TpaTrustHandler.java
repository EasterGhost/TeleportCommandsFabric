package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustDecision;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustEntry;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class TpaTrustHandler {
	private TpaTrustHandler() {
	}

	static int showTrust(ServerPlayer owner, TpaTrustTarget target) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			TpaMessages.send(owner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		MinecraftServer server = owner.level().getServer();
		UUID ownerUuid = owner.getUUID();
		manager.query(ownerUuid, profile -> target.all()
				? new TpaTrustEntry(profile.getDefaultTpaTrust(), profile.getDefaultTpaHereTrust())
				: profile.getTpaTrustEntries().getOrDefault(target.playerUuid(), TpaTrustEntry.defaults()))
				.whenComplete((entry, throwable) -> server.execute(() -> {
					ServerPlayer currentOwner = server.getPlayerList().getPlayer(ownerUuid);
					if (currentOwner == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while reading TPA trust settings.", throwable);
						TpaMessages.send(currentOwner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
						return;
					}
					TpaMessages.sendTrustStatus(currentOwner, target, entry);
				}));
		return 0;
	}

	static int setTrust(ServerPlayer owner, TpaTrustTarget target, Tpa.Type type, TpaTrustDecision decision) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			TpaMessages.send(owner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		MinecraftServer server = owner.level().getServer();
		UUID ownerUuid = owner.getUUID();
		manager.mutateVoid(ownerUuid, profile -> {
			if (target.all()) {
				profile.setDefaultTpaTrust(type, decision);
			} else {
				profile.setPlayerTpaTrust(target.playerUuid(), type, decision);
			}
		}).whenComplete((ignored, throwable) -> server.execute(() -> {
			ServerPlayer currentOwner = server.getPlayerList().getPlayer(ownerUuid);
			if (currentOwner == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating TPA trust settings.", throwable);
				TpaMessages.send(currentOwner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			TpaMessages.sendTrustUpdated(currentOwner, target, type, decision);
		}));
		return 0;
	}

	static int setTrust(ServerPlayer owner, TpaTrustTarget target, TpaTrustDecision tpaDecision,
			TpaTrustDecision tpaHereDecision) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			TpaMessages.send(owner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		MinecraftServer server = owner.level().getServer();
		UUID ownerUuid = owner.getUUID();
		manager.mutateVoid(ownerUuid, profile -> {
			if (target.all()) {
				profile.setDefaultTpaTrust(Tpa.Type.TPA, tpaDecision);
				profile.setDefaultTpaTrust(Tpa.Type.TPAHERE, tpaHereDecision);
			} else {
				profile.setPlayerTpaTrust(target.playerUuid(), Tpa.Type.TPA, tpaDecision);
				profile.setPlayerTpaTrust(target.playerUuid(), Tpa.Type.TPAHERE, tpaHereDecision);
			}
		}).whenComplete((ignored, throwable) -> server.execute(() -> {
			ServerPlayer currentOwner = server.getPlayerList().getPlayer(ownerUuid);
			if (currentOwner == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating TPA trust settings.", throwable);
				TpaMessages.send(currentOwner, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			TpaMessages.sendTrustUpdated(currentOwner, target, tpaDecision, tpaHereDecision);
		}));
		return 0;
	}
}
