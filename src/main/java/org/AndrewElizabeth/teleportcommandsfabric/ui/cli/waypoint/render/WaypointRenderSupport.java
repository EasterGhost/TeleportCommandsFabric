package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class WaypointRenderSupport {
	private static final String MINECRAFT_NAMESPACE = "minecraft:";

	private WaypointRenderSupport() {
	}

	static MutableComponent stateButton(MutableComponent label, String command, boolean active) {
		ChatFormatting color = active ? ChatFormatting.GOLD : ChatFormatting.AQUA;
		return Component.literal("[")
				.append(label)
				.append("]")
				.withStyle(color)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
	}

	static String shortDimensionId(String dimensionId) {
		return dimensionId.startsWith(MINECRAFT_NAMESPACE) ? dimensionId.substring(MINECRAFT_NAMESPACE.length()) : dimensionId;
	}
}
