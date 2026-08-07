package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;

public record TpaTrustEntry(TpaTrustDecision tpa, TpaTrustDecision tpaHere) {
	public TpaTrustEntry {
		tpa = tpa == null ? TpaTrustDecision.DEFAULT : tpa;
		tpaHere = tpaHere == null ? TpaTrustDecision.DEFAULT : tpaHere;
	}

	public static TpaTrustEntry defaults() {
		return new TpaTrustEntry(TpaTrustDecision.DEFAULT, TpaTrustDecision.DEFAULT);
	}

	public TpaTrustDecision decision(Tpa.Type type) {
		return type == Tpa.Type.TPAHERE ? tpaHere : tpa;
	}

	public TpaTrustEntry withDecision(Tpa.Type type, TpaTrustDecision decision) {
		TpaTrustDecision safeDecision = decision == null ? TpaTrustDecision.DEFAULT : decision;
		return type == Tpa.Type.TPAHERE
				? new TpaTrustEntry(tpa, safeDecision)
				: new TpaTrustEntry(safeDecision, tpaHere);
	}

	public boolean isDefault() {
		return tpa == TpaTrustDecision.DEFAULT && tpaHere == TpaTrustDecision.DEFAULT;
	}
}
