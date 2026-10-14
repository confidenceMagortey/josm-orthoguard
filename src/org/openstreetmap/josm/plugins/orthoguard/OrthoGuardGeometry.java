/* GPL-2.0-or-later */
package org.openstreetmap.josm.plugins.orthoguard;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Geometry-only tests for likely rectangular building footprints.
 *
 * <p>This class deliberately does not decide whether an angled building is
 * wrong. It identifies convex, four-corner, roughly parallelogram-shaped
 * footprints and reports them when any interior corner angle differs from
 * 90 degrees beyond a tiny floating-point epsilon. Many-node rounded footprints are not reduced to four corners,
 * so they are ignored.</p>
 */
public final class OrthoGuardGeometry {
    /** Nodes that turn by less than this are treated as points along a straight edge. */
    static final double STRAIGHT_VERTEX_MAX_DEFLECTION_DEGREES = 6.0;

    /** Maximum allowed mismatch between the directions of opposite edges. */
    static final double MAX_OPPOSITE_EDGE_DIRECTION_DEVIATION_DEGREES = 35.0;

    /** Maximum relative length difference between opposite edges. */
    static final double MAX_OPPOSITE_EDGE_LENGTH_DIFFERENCE = 0.60;

    /** Maximum difference between opposite interior angles for a rectangle-like candidate. */
    static final double MAX_OPPOSITE_CORNER_ANGLE_DIFFERENCE_DEGREES = 40.0;

    private static final double EPSILON = 1.0e-8;
    private static final double ANGLE_EPSILON_DEGREES = 1.0e-7;

    private OrthoGuardGeometry() {
        // Utility class.
    }

    /**
     * Analyse a polygon ring. The last point may repeat the first point.
     * Returns {@code null} unless it is a likely rectangular candidate with
     * at least one interior corner angle below or above 90 degrees.
     */
    public static Analysis inspect(List<Point2D.Double> polygonRing) {
        List<Point2D.Double> corners = cleanRing(polygonRing);
        if (corners.size() < 4) {
            return null;
        }

        corners = simplifyStraightVertices(corners);
        // Rectangle/square footprints have four meaningful corners. This also
        // excludes circles and rounded polygons represented by many vertices.
        if (corners.size() != 4 || !isStrictlyConvex(corners)) {
            return null;
        }

        double[] sideLengths = sideLengths(corners);
        for (double length : sideLengths) {
            if (!Double.isFinite(length) || length <= EPSILON) {
                return null;
            }
        }

        double[] angles = interiorAngles(corners);
        if (!isLikelyRectangularCandidate(corners, sideLengths, angles)) {
            return null;
        }

        int cornersNotNinety = 0;
        double maxAngle = 0.0;
        for (double angle : angles) {
            if (!Double.isFinite(angle)) {
                return null;
            }
            maxAngle = Math.max(maxAngle, angle);
            if (Math.abs(angle - 90.0) > ANGLE_EPSILON_DEGREES) {
                cornersNotNinety++;
            }
        }
        if (cornersNotNinety == 0) {
            return null;
        }

        List<Double> resultAngles = new ArrayList<>(angles.length);
        for (double angle : angles) {
            resultAngles.add(angle);
        }
        return new Analysis(resultAngles, cornersNotNinety, maxAngle, corners.size());
    }

    private static List<Point2D.Double> cleanRing(List<Point2D.Double> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        List<Point2D.Double> points = new ArrayList<>();
        for (Point2D.Double point : source) {
            if (point == null || !Double.isFinite(point.x) || !Double.isFinite(point.y)) {
                return new ArrayList<>();
            }
            if (points.isEmpty() || points.get(points.size() - 1).distance(point) > EPSILON) {
                points.add(new Point2D.Double(point.x, point.y));
            }
        }
        if (points.size() > 1 && points.get(0).distance(points.get(points.size() - 1)) <= EPSILON) {
            points.remove(points.size() - 1);
        }
        return points;
    }

    /** Removes nearly collinear points one at a time; true round curves retain many corners. */
    private static List<Point2D.Double> simplifyStraightVertices(List<Point2D.Double> source) {
        List<Point2D.Double> points = new ArrayList<>(source);
        boolean removed = true;
        int safety = points.size() * 2;
        while (removed && points.size() > 4 && safety-- > 0) {
            removed = false;
            for (int i = 0; i < points.size(); i++) {
                Point2D.Double previous = points.get((i + points.size() - 1) % points.size());
                Point2D.Double current = points.get(i);
                Point2D.Double next = points.get((i + 1) % points.size());
                double deflection = turnDeflectionDegrees(previous, current, next);
                if (Double.isFinite(deflection)
                        && deflection <= STRAIGHT_VERTEX_MAX_DEFLECTION_DEGREES) {
                    points.remove(i);
                    removed = true;
                    break;
                }
            }
        }
        return points;
    }

    /** Angle between travel direction into and out of a vertex; straight line is 0 degrees. */
    private static double turnDeflectionDegrees(Point2D.Double previous,
            Point2D.Double current, Point2D.Double next) {
        double ax = current.x - previous.x;
        double ay = current.y - previous.y;
        double bx = next.x - current.x;
        double by = next.y - current.y;
        if (Math.hypot(ax, ay) <= EPSILON || Math.hypot(bx, by) <= EPSILON) {
            return Double.NaN;
        }
        return Math.toDegrees(Math.abs(Math.atan2(ax * by - ay * bx, ax * bx + ay * by)));
    }

    private static boolean isStrictlyConvex(List<Point2D.Double> points) {
        double expectedSign = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Point2D.Double a = points.get(i);
            Point2D.Double b = points.get((i + 1) % points.size());
            Point2D.Double c = points.get((i + 2) % points.size());
            double abx = b.x - a.x;
            double aby = b.y - a.y;
            double bcx = c.x - b.x;
            double bcy = c.y - b.y;
            double cross = abx * bcy - aby * bcx;
            if (Math.abs(cross) <= EPSILON) {
                return false;
            }
            double sign = Math.signum(cross);
            if (expectedSign == 0.0) {
                expectedSign = sign;
            } else if (sign != expectedSign) {
                return false;
            }
        }
        return true;
    }

    private static double[] sideLengths(List<Point2D.Double> points) {
        double[] lengths = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            lengths[i] = points.get(i).distance(points.get((i + 1) % points.size()));
        }
        return lengths;
    }

    private static double[] interiorAngles(List<Point2D.Double> points) {
        double[] result = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point2D.Double vertex = points.get(i);
            Point2D.Double previous = points.get((i + points.size() - 1) % points.size());
            Point2D.Double next = points.get((i + 1) % points.size());
            double ax = previous.x - vertex.x;
            double ay = previous.y - vertex.y;
            double bx = next.x - vertex.x;
            double by = next.y - vertex.y;
            double aLength = Math.hypot(ax, ay);
            double bLength = Math.hypot(bx, by);
            if (aLength <= EPSILON || bLength <= EPSILON) {
                result[i] = Double.NaN;
                continue;
            }
            double cosine = (ax * bx + ay * by) / (aLength * bLength);
            cosine = Math.max(-1.0, Math.min(1.0, cosine));
            result[i] = Math.toDegrees(Math.acos(cosine));
        }
        return result;
    }

    private static boolean isLikelyRectangularCandidate(List<Point2D.Double> points,
            double[] lengths, double[] angles) {
        // Opposite sides in a rectangle or a skewed rectangular footprint remain
        // roughly parallel and broadly similar in length. These intentionally
        // permissive thresholds catch substantial skew while rejecting many
        // trapezoids and irregular four-sided shapes.
        for (int i = 0; i < 2; i++) {
            int opposite = i + 2;
            double parallelDifference = lineDirectionDifferenceDegrees(
                    points.get((i + 1) % 4).x - points.get(i).x,
                    points.get((i + 1) % 4).y - points.get(i).y,
                    points.get((opposite + 1) % 4).x - points.get(opposite).x,
                    points.get((opposite + 1) % 4).y - points.get(opposite).y);
            if (parallelDifference > MAX_OPPOSITE_EDGE_DIRECTION_DEVIATION_DEGREES) {
                return false;
            }

            double maxLength = Math.max(lengths[i], lengths[opposite]);
            double relativeLengthDifference = Math.abs(lengths[i] - lengths[opposite]) / maxLength;
            if (relativeLengthDifference > MAX_OPPOSITE_EDGE_LENGTH_DIFFERENCE) {
                return false;
            }

            if (Math.abs(angles[i] - angles[opposite])
                    > MAX_OPPOSITE_CORNER_ANGLE_DIFFERENCE_DEGREES) {
                return false;
            }
        }
        return true;
    }

    /** Smallest angle between two undirected edge lines, in [0, 90] degrees. */
    private static double lineDirectionDifferenceDegrees(double ax, double ay, double bx, double by) {
        double denominator = Math.hypot(ax, ay) * Math.hypot(bx, by);
        if (denominator <= EPSILON) {
            return 90.0;
        }
        double cosine = Math.abs((ax * bx + ay * by) / denominator);
        cosine = Math.max(0.0, Math.min(1.0, cosine));
        return Math.toDegrees(Math.acos(cosine));
    }

    /** Immutable results for a candidate footprint. */
    public static final class Analysis {
        private final List<Double> cornerAnglesDegrees;
        private final int cornersNotNinety;
        private final double maximumCornerAngleDegrees;
        private final int cornerCount;

        private Analysis(List<Double> angles, int count, double maximum, int corners) {
            this.cornerAnglesDegrees = Collections.unmodifiableList(new ArrayList<>(angles));
            this.cornersNotNinety = count;
            this.maximumCornerAngleDegrees = maximum;
            this.cornerCount = corners;
        }

        public List<Double> getCornerAnglesDegrees() {
            return cornerAnglesDegrees;
        }

        /** Returns the count of corners whose measured angle differs from 90 degrees. */
        public int getCornersNotNinety() {
            return cornersNotNinety;
        }

        public double getMaximumCornerAngleDegrees() {
            return maximumCornerAngleDegrees;
        }

        public int getCornerCount() {
            return cornerCount;
        }
    }
}
