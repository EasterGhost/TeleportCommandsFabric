package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlayerProfileView {
	UUID getPlayerUuid();

	UUID getDefaultHomeUuid();

	String getDefaultHomeName();

	List<NamedLocationView> getHomes();

	Set<UUID> getHiddenWarpUuids();

	TpaTrustDecision getDefaultTpaTrust();

	TpaTrustDecision getDefaultTpaHereTrust();

	Map<UUID, TpaTrustEntry> getTpaTrustEntries();

	Optional<NamedLocationView> getHomeByName(String name);

	Optional<NamedLocationView> getHome(UUID uuid);

	Optional<NamedLocationView> getDefaultHomeLocation();

	Optional<NamedLocationView> getTemporaryHomeLocation();

	boolean hasTemporaryHome();

	boolean isWarpHidden(UUID warpUuid);

	TpaTrustDecision resolveTpaTrust(UUID requesterUuid, Tpa.Type type);

	boolean isEligibleDefaultHome(NamedLocationView home);
}
