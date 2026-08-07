package org.AndrewElizabeth.teleportcommandsfabric.integration.bluemap;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class BlueMapWarpMarkers {
	private static final String MARKER_SET_ID = "teleport_commands_fabric_warps";
	private static final String MARKER_SET_LABEL = "Warps";

	private BlueMapWarpMarkers() {
	}

	static void apply(BlueMapAPI api, List<NamedLocationView> warps) {
		clear(api);
		Map<BlueMapWorld, MarkerSet> markerSets = new HashMap<>();
		for (NamedLocationView warp : warps) {
			world(api, warp).ifPresent(world -> markerSets
					.computeIfAbsent(world, ignored -> MarkerSet.builder()
							.label(MARKER_SET_LABEL)
							.toggleable(true)
							.defaultHidden(false)
							.build())
					.put(warp.getUuid().toString(), marker(warp)));
		}
		for (Map.Entry<BlueMapWorld, MarkerSet> entry : markerSets.entrySet()) {
			for (BlueMapMap map : entry.getKey().getMaps()) {
				map.getMarkerSets().put(MARKER_SET_ID, entry.getValue());
			}
		}
	}

	static void clear(BlueMapAPI api) {
		for (BlueMapMap map : api.getMaps()) {
			map.getMarkerSets().remove(MARKER_SET_ID);
		}
	}

	private static Optional<BlueMapWorld> world(BlueMapAPI api, NamedLocationView warp) {
		Optional<BlueMapWorld> world = api.getWorld(warp.getDimension());
		if (world.isPresent()) {
			return world;
		}
		return api.getWorld(warp.getDimensionId());
	}

	private static POIMarker marker(NamedLocationView warp) {
		String detail = "Warp: " + htmlEscape(warp.getName()) + "<br>"
				+ "Dimension: " + htmlEscape(warp.getDimensionId()) + "<br>"
				+ "Location: " + warp.getX() + ", " + formatY(warp.getYPrecise()) + ", " + warp.getZ();
		return POIMarker.builder()
				.label(warp.getName())
				.position(new Vector3d(warp.getX() + 0.5D, warp.getYPrecise(), warp.getZ() + 0.5D))
				.detail(detail)
				.build();
	}

	private static String formatY(double value) {
		if (value == Math.rint(value)) {
			return Integer.toString((int) value);
		}
		return Double.toString(value);
	}

	private static String htmlEscape(String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
