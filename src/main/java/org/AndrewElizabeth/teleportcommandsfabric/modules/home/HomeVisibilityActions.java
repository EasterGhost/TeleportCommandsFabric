package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.command.VisibilityCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

final class HomeVisibilityActions {
	private HomeVisibilityActions() {
	}

	static void setVisibility(ServerPlayer player, String homeName, boolean visible) throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> VisibilityCommandSupport.update(visible,
				() -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED, false),
				NamedLocation::isXaeroVisible, NamedLocation::setXaeroVisible,
				() -> HomeMessages.sendMapVisibilityAlready(player, visible), () -> {
					HomeMessages.sendMapVisibilityChanged(player, visible);
					HomeCommandSupport.printHomes(player, playerStorage);
				}));
	}

	static void setVisibilitySilently(ServerPlayer player, String homeName, boolean visible) throws Exception {
		HomeCommandSupport.withOptionalPlayerStorage(player, playerStorage -> {
			if (playerStorage == null) {
				return;
			}

			VisibilityCommandSupport.update(visible,
					() -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED, true),
					NamedLocation::isXaeroVisible, NamedLocation::setXaeroVisible, () -> {
					}, () -> {
					});
		});
	}

	static void setVisibilitySilentlyAndShowPage(ServerPlayer player, String homeName, boolean visible, int page)
			throws Exception {
		setVisibilitySilentlyAndShowPage(player, homeName, visible, page, null);
	}

	static void setVisibilitySilentlyAndShowPage(ServerPlayer player, String homeName, boolean visible, int page,
			String dimensionFilter)
			throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> VisibilityCommandSupport.update(
				visible, () -> HomeCommandSupport.resolveHomeForCommand(playerStorage, homeName, player, ChatFormatting.RED, true),
				NamedLocation::isXaeroVisible, NamedLocation::setXaeroVisible,
				() -> HomeCommandSupport.printHomes(player, playerStorage, page, dimensionFilter),
				() -> HomeCommandSupport.printHomes(player, playerStorage, page, dimensionFilter)));
	}

}
