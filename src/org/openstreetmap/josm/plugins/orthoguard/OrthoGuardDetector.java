/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/** Detects likely rectangular buildings that have one or more interior corners above 90 degrees. */
public final class OrthoGuardDetector {
    private OrthoGuardDetector() {
        // Utility class.
    }

    public static List<OrthoGuardFinding> scan(Collection<Way> ways) {
        List<OrthoGuardFinding> findings = new ArrayList<>();
        if (ways == null) {
            return findings;
        }
        for (Way way : ways) {
            OrthoGuardFinding finding = inspect(way);
            if (finding != null) {
                findings.add(finding);
            }
        }
        return findings;
    }

    /** Returns a finding when a likely rectangular building has any non-right interior corner. */
    public static OrthoGuardFinding inspect(Way way) {
        if (!isBuildingFootprint(way) || way.hasIncompleteNodes()) {
            return null;
        }

        List<Node> nodes = way.getNodes();
        if (nodes == null || nodes.size() < 5) {
            // A closed quadrilateral has four unique nodes plus its repeated first node.
            return null;
        }

        List<Point2D.Double> ring = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            EastNorth eastNorth = node.getEastNorth();
            if (eastNorth == null || !Double.isFinite(eastNorth.east())
                    || !Double.isFinite(eastNorth.north())) {
                return null;
            }
            ring.add(new Point2D.Double(eastNorth.east(), eastNorth.north()));
        }

        OrthoGuardGeometry.Analysis analysis = OrthoGuardGeometry.inspect(ring);
        return analysis == null ? null : new OrthoGuardFinding(way, analysis);
    }

    /**
     * Scope is intentionally limited to closed building ways. Relation-based
     * multipolygon footprints are not analysed in this version.
     */
    public static boolean isBuildingFootprint(Way way) {
        if (way == null || !way.isUsable() || !way.isClosed() || !way.isArea()) {
            return false;
        }

        String building = way.get("building");
        String buildingPart = way.get("building:part");
        if (!isAffirmativeFeatureValue(building) && !isAffirmativeFeatureValue(buildingPart)) {
            return false;
        }
        return !hasExplicitlyRoundBuildingTag(building) && !hasExplicitlyRoundBuildingTag(buildingPart);
    }

    private static boolean isAffirmativeFeatureValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !"no".equals(normalized) && !"false".equals(normalized) && !"0".equals(normalized);
    }

    /** Explicitly circular building types are not candidates even if simplified to four vertices. */
    private static boolean hasExplicitlyRoundBuildingTag(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return "round".equals(normalized)
                || "roundhouse".equals(normalized)
                || "circular".equals(normalized)
                || "cylindrical".equals(normalized)
                || "dome".equals(normalized)
                || "silo".equals(normalized)
                || "storage_tank".equals(normalized)
                || "water_tower".equals(normalized);
    }
}
