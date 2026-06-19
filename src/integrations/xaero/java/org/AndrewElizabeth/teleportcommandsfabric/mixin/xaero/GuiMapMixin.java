package org.AndrewElizabeth.teleportcommandsfabric.mixin.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client.XaeroWaypointCommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;

@Mixin(GuiMap.class)
public class GuiMapMixin {
	private static final String TELEPORT_OPTION_KEY = "gui.xaero_right_click_map_teleport";

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

			String command = XaeroWaypointCommandHelper.buildTeleportCommand(waypoint);
			if (command == null) {
				continue;
			}

			RightClickOption replacement = new RightClickOption(
					TELEPORT_OPTION_KEY,
					optionAccessor.tpc$getIndex(),
					optionAccessor.tpc$getTarget()) {
				@Override
				public void onAction(Screen parent) {
					Minecraft mc = Minecraft.getInstance();
					if (mc.player == null || mc.player.connection == null) {
						return;
					}
					mc.player.connection.sendCommand(command);
				}
			};
			replacement.setActive(option.isActive());
			options.set(i, replacement);
			return;
		}
	}
}
