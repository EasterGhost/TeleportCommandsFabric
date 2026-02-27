package org.AndrewElizabeth.teleportcommandsfabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(RightClickOption.class)
public interface RightClickOptionAccessor {
	@Accessor("name")
	String tpc$getNameKey();

	@Accessor("index")
	int tpc$getIndex();

	@Accessor("target")
	IRightClickableElement tpc$getTarget();
}
