package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

final class HomeMutationActions {
	private HomeMutationActions() {
	}

	static int setDefaultHome(ServerPlayer player, String homeName, PlayerHomeSource source) {
		return HomeMessages.execute(player, "Error while setting the default home!",
				"commands.teleport_commands.home.defaultError", () -> {
					Optional<NamedLocation> optionalHome = source.getByName(homeName);
					if (optionalHome.isEmpty()) {
						HomeMessages.sendNotFound(player, ChatFormatting.RED);
						return;
					}

					NamedLocation home = optionalHome.get();
					if (!source.canBeDefault(home)) {
						HomeMessages.send(player, "commands.teleport_commands.home.defaultTemporary", ChatFormatting.RED);
						return;
					}

					if (source.isDefault(home)) {
						HomeMessages.send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
						return;
					}

					source.setDefault(home);
					HomeMessages.send(player, "commands.teleport_commands.home.default");
				});
	}
}
