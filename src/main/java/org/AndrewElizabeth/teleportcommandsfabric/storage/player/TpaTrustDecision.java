package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import java.util.Locale;
import java.util.Optional;

public enum TpaTrustDecision {
	DEFAULT,
	ACCEPT,
	DENY;

	public static TpaTrustDecision fromSerialized(String value) {
		return parseSerialized(value).orElse(DEFAULT);
	}

	public static Optional<TpaTrustDecision> parseSerialized(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		return switch (value.toLowerCase(Locale.ROOT)) {
		case "default" -> Optional.of(DEFAULT);
		case "accept" -> Optional.of(ACCEPT);
		case "deny" -> Optional.of(DENY);
		default -> Optional.empty();
		};
	}

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
