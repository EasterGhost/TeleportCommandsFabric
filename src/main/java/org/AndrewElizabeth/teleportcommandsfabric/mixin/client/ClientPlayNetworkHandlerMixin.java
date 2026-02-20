package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    public abstract void sendCommand(String command);

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    private void teleportcommandsfabric$bypassConfirmForTrustedCommands(String command, Screen afterActionScreen, CallbackInfo ci) {
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
                || normalized.startsWith("home ")
                || normalized.startsWith("defaulthome ")
                || normalized.startsWith("warp ")
                || normalized.equals("back true")
                || normalized.equals("worldspawn true")
                || normalized.startsWith("teleportcommandsfabric:");
    }
}
