package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.VisibilityCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

final class HomeVisibilityActions {
	private HomeVisibilityActions() {
	}

	static void setVisibility(ServerPlayer player, String homeName, boolean visible) throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> VisibilityCommandSupport.update(
				visible,
				() -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED,
						false),
				NamedLocation::isXaeroVisible,
				NamedLocation::setXaeroVisible,
				() -> HomeMessages.sendMapVisibilityAlready(player, visible),
				() -> {
					HomeMessages.sendMapVisibilityChanged(player, visible);
					HomeCommandSupport.printHomes(player, playerStorage);
				}));
	}

	static void setVisibilitySilently(ServerPlayer player, String homeName, boolean visible) throws Exception {
		HomeCommandSupport.withOptionalPlayerStorage(player, playerStorage -> {
			if (playerStorage == null) {
				return;
			}

			VisibilityCommandSupport.update(
					visible,
					() -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED,
							true),
					NamedLocation::isXaeroVisible,
					NamedLocation::setXaeroVisible,
					() -> {
					},
					() -> {
					});
		});
	}

	static void setVisibilitySilentlyAndShowPage(ServerPlayer player, String homeName, boolean visible, int page)
			throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> VisibilityCommandSupport.update(
				visible,
				() -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED,
						true),
				NamedLocation::isXaeroVisible,
				NamedLocation::setXaeroVisible,
				() -> HomeCommandSupport.printHomes(player, playerStorage, page),
				() -> HomeCommandSupport.printHomes(player, playerStorage, page)));
	}

}
