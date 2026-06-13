package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

final class AdminMessages {
	private static final String DEFAULT_LANGUAGE = "en_us";

	private AdminMessages() {
	}

	static String language(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null ? DEFAULT_LANGUAGE : player.clientInformation().language().toLowerCase(Locale.ROOT);
	}

	static MutableComponent t(CommandSourceStack source, String key, MutableComponent... args) {
		return TranslationHelper.getTranslatedText(key, language(source), args);
	}

	static int sendCurrentValue(CommandSourceStack source, String name, MutableComponent value) {
		source.sendSuccess(() -> t(source, "commands.teleport_commands.admin.config.current",
				Component.literal(name), value).withStyle(ChatFormatting.YELLOW), false);
		return 0;
	}

	static void sendSuccess(CommandSourceStack source, MutableComponent message, boolean broadcastToOps) {
		source.sendSuccess(() -> message.copy().withStyle(ChatFormatting.GREEN), broadcastToOps);
	}

	static void sendError(CommandSourceStack source, MutableComponent message) {
		source.sendFailure(message.copy().withStyle(ChatFormatting.RED));
	}

	static MutableComponent enabledText(CommandSourceStack source, boolean enabled) {
		return t(source, enabled
				? "commands.teleport_commands.admin.stat.enabled"
				: "commands.teleport_commands.admin.stat.disabled");
	}
}
