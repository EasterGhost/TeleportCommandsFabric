package org.AndrewElizabeth.teleportcommandsfabric.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class ServerStartMixin {

	@Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", ordinal = 0))
	private void runServer(CallbackInfo info) {
		TeleportCommands.initializeMod((MinecraftServer) (Object) this);
	}
}
