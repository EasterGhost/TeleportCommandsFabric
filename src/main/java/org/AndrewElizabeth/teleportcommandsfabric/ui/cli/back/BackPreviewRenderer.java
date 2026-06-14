package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.back;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public final class BackPreviewRenderer {
	private static final String COMMAND_BACK_TP = "back tp";
	private static final String COMMAND_BACK_DEATH = "back death";
	private static final String DISPLAY_COMMAND_BACK_TP = "/back tp";
	private static final String DISPLAY_COMMAND_BACK_DEATH = "/back death";
	private static final String[] DIRECTION_KEYS = {
			"south",
			"southwest",
			"west",
			"northwest",
			"north",
			"northeast",
			"east",
			"southeast"
	};

	public MutableComponent render(ServerPlayer player, Optional<RecordedLocationView> previous,
			Optional<RecordedLocationView> death) {
		return render(player.clientInformation().language(), previous, death);
	}

	public MutableComponent render(String language, Optional<RecordedLocationView> previous,
			Optional<RecordedLocationView> death) {
		MutableComponent message = Component.empty();
		message.append(t(language, "commands.teleport_commands.back.preview.title")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		boolean hasPrevious = previous != null && previous.isPresent();
		boolean hasDeath = death != null && death.isPresent();
		if (!hasPrevious && !hasDeath) {
			message.append("\n");
			message.append(t(language, "commands.teleport_commands.back.preview.none").withStyle(ChatFormatting.YELLOW));
			return message;
		}

		message.append("\n");
		if (hasPrevious) {
			appendSection(message, language, "commands.teleport_commands.back.preview.previous",
					previous.get(), COMMAND_BACK_TP, DISPLAY_COMMAND_BACK_TP);
		}
		if (hasDeath) {
			if (hasPrevious) {
				message.append("\n");
			}
			appendSection(message, language, "commands.teleport_commands.back.preview.death",
					death.get(), COMMAND_BACK_DEATH, DISPLAY_COMMAND_BACK_DEATH);
		}
		return message;
	}

	private void appendSection(MutableComponent message, String language, String titleKey,
			RecordedLocationView location, String command, String displayCommand) {
		message.append(t(language, titleKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		message.append("\n");
		appendLine(message, language, "commands.teleport_commands.back.preview.world",
				Component.literal(location.getDimensionId()).withStyle(ChatFormatting.AQUA));
		BlockPos pos = location.getBlockPos();
		appendLine(message, language, "commands.teleport_commands.back.preview.position",
				Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.AQUA));
		if (location.getYRot() != null) {
			appendLine(message, language, "commands.teleport_commands.back.preview.facing",
					facing(language, location.getYRot()).withStyle(ChatFormatting.AQUA));
		}
		if (location.getXRot() != null) {
			appendLine(message, language, "commands.teleport_commands.back.preview.pitch",
					degrees(location.getXRot()).withStyle(ChatFormatting.AQUA));
		}
		message.append(teleportButton(language, command, displayCommand));
		message.append("\n");
	}

	private void appendLine(MutableComponent message, String language, String key, MutableComponent value) {
		message.append(t(language, key, value).withStyle(ChatFormatting.GRAY));
		message.append("\n");
	}

	private MutableComponent facing(String language, float yaw) {
		String directionKey = DIRECTION_KEYS[directionIndex(yaw)];
		return t(language, "commands.teleport_commands.back.preview.direction." + directionKey,
				degrees(yaw));
	}

	private int directionIndex(float yaw) {
		float normalized = yaw % 360.0F;
		if (normalized < 0.0F) {
			normalized += 360.0F;
		}
		return ((int) ((normalized + 22.5F) / 45.0F)) % DIRECTION_KEYS.length;
	}

	private MutableComponent degrees(float value) {
		return Component.literal(String.format(Locale.ROOT, "%.1f\u00b0", value));
	}

	private MutableComponent teleportButton(String language, String command, String displayCommand) {
		return t(language, "commands.teleport_commands.back.preview.teleport")
				.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal(displayCommand))));
	}

	private MutableComponent t(String language, String key, MutableComponent... args) {
		return TranslationHelper.getTranslatedText(key, language, args);
	}
}
