package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;

import java.util.ArrayList;

@Mixin(GuiMap.class)
public class GuiMapMixin {
	private static final String TELEPORT_OPTION_KEY = "gui.xaero_right_click_map_teleport";
	private static final String WARP_TAG_PREFIX = "TPC-W ";
	private static final String HOME_TAG_PREFIX = "TPC-H ";

	@Inject(method = "getRightClickOptions", at = @At("RETURN"))
	private void tpc$replaceWaypointTeleportOption(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
		ArrayList<RightClickOption> options = cir.getReturnValue();
		if (options == null || options.isEmpty()) {
			return;
		}

		for (int i = 0; i < options.size(); i++) {
			RightClickOption option = options.get(i);
			if (!(option instanceof RightClickOptionAccessor optionAccessor)) {
				continue;
			}

			String key = optionAccessor.tpc$getNameKey();
			Object target = optionAccessor.tpc$getTarget();
			if (!TELEPORT_OPTION_KEY.equals(key) || !(target instanceof Waypoint waypoint)) {
				continue;
			}

			String command = tpc$buildTeleportCommand(waypoint);
			if (command == null) {
				continue;
			}

			/*
			 * LEGACY(kept): RightClickOption replacement path.
			 *
			 * RightClickOption replacement = new RightClickOption( * TELEPORT_OPTION_KEY, * optionAccessor.tpc$getIndex(),
			 * optionAccessor.tpc$getTarget()) { * @Override
			 * public void onAction(Screen parent) { * Minecraft mc = Minecraft.getInstance();
			 * if (mc.player == null || mc.player.connection == null) { * return;
			 * }
			 * mc.player.connection.sendCommand(command);
			 * }
			 * };
			 * replacement.setActive(option.isActive());
			 * options.set(i, replacement);
			 */

			// TEMPORARY FALLBACK:
			// Xaero callback signature currently drifts across mappings (class_437 vs Screen).
			// Keep GUI replacement disabled and let MapWaypointTeleportFallbackMixin handle teleport command dispatch.
			return;
		}
	}

	private static String tpc$buildTeleportCommand(Waypoint waypoint) {
		String name = waypoint.getName();
		if (name == null) {
			return null;
		}

		String normalizedName = tpc$stripTaggedPrefix(name);
		if (normalizedName.isBlank()) {
			return null;
		}

		if (name.startsWith(WARP_TAG_PREFIX)) {
			return "warp " + tpc$quoteCommandArgument(normalizedName);
		}
		if (name.startsWith(HOME_TAG_PREFIX)) {
			return "home " + tpc$quoteCommandArgument(normalizedName);
		}
		return null;
	}

	private static String tpc$quoteCommandArgument(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static String tpc$stripTaggedPrefix(String name) {
		if (name.startsWith(WARP_TAG_PREFIX)) {
			return name.substring(WARP_TAG_PREFIX.length()).trim();
		}
		if (name.startsWith(HOME_TAG_PREFIX)) {
			return name.substring(HOME_TAG_PREFIX.length()).trim();
		}
		return name.trim();
	}
}
