package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

record PlayerProfileSnapshot(PlayerProfile profile) implements PlayerProfileView {
	@Override
	public UUID getPlayerUuid() {
		return profile.getPlayerUuid();
	}

	@Override
	public UUID getDefaultHomeUuid() {
		return profile.getDefaultHomeUuid();
	}

	@Override
	public String getDefaultHomeName() {
		return profile.getDefaultHomeName();
	}

	@Override
	public List<NamedLocationView> getHomes() {
		return NamedLocationSnapshot.list(profile.getHomes());
	}

	@Override
	public Set<UUID> getHiddenWarpUuids() {
		return profile.getHiddenWarpUuids();
	}

	@Override
	public TpaTrustDecision getDefaultTpaTrust() {
		return profile.getDefaultTpaTrust();
	}

	@Override
	public TpaTrustDecision getDefaultTpaHereTrust() {
		return profile.getDefaultTpaHereTrust();
	}

	@Override
	public Map<UUID, TpaTrustEntry> getTpaTrustEntries() {
		return profile.getTpaTrustEntries();
	}

	@Override
	public Optional<NamedLocationView> getHomeByName(String name) {
		return NamedLocationSnapshot.optional(profile.getHomeByName(name));
	}

	@Override
	public Optional<NamedLocationView> getHome(UUID uuid) {
		return NamedLocationSnapshot.optional(profile.getHome(uuid));
	}

	@Override
	public Optional<NamedLocationView> getDefaultHomeLocation() {
		return NamedLocationSnapshot.optional(profile.getDefaultHomeLocation());
	}

	@Override
	public Optional<NamedLocationView> getTemporaryHomeLocation() {
		return NamedLocationSnapshot.optional(profile.getTemporaryHomeLocation());
	}

	@Override
	public boolean hasTemporaryHome() {
		return profile.hasTemporaryHome();
	}

	@Override
	public boolean isWarpHidden(UUID warpUuid) {
		return profile.isWarpHidden(warpUuid);
	}

	@Override
	public TpaTrustDecision resolveTpaTrust(UUID requesterUuid, Tpa.Type type) {
		return profile.resolveTpaTrust(requesterUuid, type);
	}

	@Override
	public boolean isEligibleDefaultHome(NamedLocationView home) {
		return home != null && !home.isTemporary() && !home.isExpired();
	}
}
