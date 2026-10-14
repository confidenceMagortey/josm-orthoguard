/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.util.List;
import java.util.Locale;

import org.openstreetmap.josm.data.osm.Way;

/** Angle measurements and reference for one rectangular building candidate. */
public final class OrthoGuardFinding {
    private final Way way;
    private final List<Double> cornerAnglesDegrees;
    private final int cornersNotNinety;
    private final double maximumCornerAngleDegrees;
    private final int cornerCount;

    public OrthoGuardFinding(Way way, OrthoGuardGeometry.Analysis analysis) {
        this.way = way;
        this.cornerAnglesDegrees = analysis.getCornerAnglesDegrees();
        this.cornersNotNinety = analysis.getCornersNotNinety();
        this.maximumCornerAngleDegrees = analysis.getMaximumCornerAngleDegrees();
        this.cornerCount = analysis.getCornerCount();
    }

    public Way getWay() {
        return way;
    }

    public int getCornerCount() {
        return cornerCount;
    }

    public int getCornersNotNinety() {
        return cornersNotNinety;
    }

    public double getMaximumCornerAngleDegrees() {
        return maximumCornerAngleDegrees;
    }

    public List<Double> getCornerAnglesDegrees() {
        return cornerAnglesDegrees;
    }

    /** Every angle that differs from 90 degrees is marked with an asterisk. */
    public String getCornerAngleSummary() {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < cornerAnglesDegrees.size(); i++) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            double angle = cornerAnglesDegrees.get(i);
            summary.append('C').append(i + 1).append('=')
                    .append(String.format(Locale.ROOT, "%.1f°%s", angle,
                            Math.abs(angle - 90.0) > 1.0e-7 ? "*" : ""));
        }
        return summary.toString();
    }

    public String getDisplayId() {
        if (way.isNew()) {
            return "new way (" + way.getId() + ")";
        }
        return "way/" + way.getId();
    }

    public String getBuildingType() {
        String value = way.get("building");
        if (value == null || value.isEmpty()) {
            value = way.get("building:part");
        }
        return value == null || value.isEmpty() ? "building" : value;
    }
}
