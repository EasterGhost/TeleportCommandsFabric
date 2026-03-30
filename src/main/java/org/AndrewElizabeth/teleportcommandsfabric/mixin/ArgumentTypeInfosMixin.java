package org.AndrewElizabeth.teleportcommandsfabric.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.AndrewElizabeth.teleportcommandsfabric.utils.UnicodeStringArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make UnicodeStringArgumentType serialize as a vanilla StringArgumentType
 * when the command tree is sent to clients. This allows the mod to work server-side only
 * without requiring clients to install it.
 */
@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosMixin {

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "unpack", at = @At("HEAD"), cancellable = true)
	private static void handleUnicodeStringType(ArgumentType argumentType, CallbackInfoReturnable cir) {
		if (argumentType instanceof UnicodeStringArgumentType) {
			cir.setReturnValue(ArgumentTypeInfos.unpack(StringArgumentType.greedyString()));
		}
	}
}
