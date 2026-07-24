package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network;

public final class IntegrationProtocol {
	public static final int PROTOCOL_VERSION = 2;
	public static final int MIN_SUPPORTED_PROTOCOL_VERSION = 1;

	private IntegrationProtocol() {
	}

	public static boolean isSupported(int version) {
		return version >= MIN_SUPPORTED_PROTOCOL_VERSION && version <= PROTOCOL_VERSION;
	}
}
