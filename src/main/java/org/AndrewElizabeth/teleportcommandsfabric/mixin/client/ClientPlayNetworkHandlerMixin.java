package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

	@Shadow
	public abstract void sendCommand(String command);

	@Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
	private void teleportcommandsfabric$bypassConfirmForTrustedCommands(String command, Screen afterActionScreen,
			CallbackInfo ci) {
		if (!isTrustedTeleportCommand(command)) {
			return;
		}

		String trimmedCommand = command.trim();
		if (trimmedCommand.startsWith("/")) {
			trimmedCommand = trimmedCommand.substring(1);
		}

		if (!trimmedCommand.isEmpty()) {
			this.sendCommand(trimmedCommand);
		}
		ci.cancel();
	}

	private static boolean isTrustedTeleportCommand(String command) {
		if (command == null) {
			return false;
		}

		String normalized = command.trim().toLowerCase();
		if (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		return normalized.startsWith("tpaaccept ")
				|| normalized.startsWith("tpadeny ")
				|| normalized.equals("homes")
				|| normalized.startsWith("homes ")
				|| normalized.startsWith("home ")
				|| normalized.startsWith("tmphome ")
				|| normalized.startsWith("maphome ")
				|| normalized.startsWith("updatehome ")
				|| normalized.startsWith("defaulthome ")
				|| normalized.equals("warps")
				|| normalized.startsWith("warps ")
				|| normalized.startsWith("warp ")
				|| normalized.startsWith("mapwarp ")
				|| normalized.startsWith("updatewarp ")
				|| normalized.startsWith("gwarpmap ")
				|| normalized.equals("back")
				|| normalized.startsWith("back ")
				|| normalized.equals("worldspawn true")
				|| normalized.startsWith("tpc ")
				|| normalized.startsWith("teleportcommandsfabric:");
	}
}
