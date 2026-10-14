import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.plugins.orthoguard.OrthoGuardGeometry;

/** Lightweight regression test runner; uses only the JDK, no JUnit dependency. */
public final class OrthoGuardGeometrySelfTest {
    private static int passed;

    public static void main(String[] args) {
        expectNoFinding("true rectangle", ring(0,0, 10,0, 10,5, 0,5, 0,0));
        expectNoFinding("rotated rectangle", rotate(ring(0,0, 10,0, 10,5, 0,5, 0,0), 31.0));
        expectFinding("skewed parallelogram", ring(0,0, 10,0, 13,5, 3,5, 0,0), 100.0);
        expectFinding("large corner deviation", ring(0,0, 10,0, 18,5, 8,5, 0,0), 140.0);
        expectFinding("one displaced rectangle corner", ring(0,0, 10,0, 7,5, 0,5, 0,0), 110.0);
        expectFinding("slight angle below 90 degrees", ring(0,0, 10,0, 10.1,5, 0,5, 0,0), 90.5);
        expectFinding("slight angle above 90 degrees", ring(0,0, 10,0, 9.9,5, 0,5, 0,0), 90.5);
        expectNonRightCornerCount("slight deviation below and above 90 degrees",
                ring(0,0, 10,0, 10.1,5, 0,5, 0,0), 2);
        expectNoFinding("rectangle with intermediate edge nodes",
                ring(0,0, 5,0, 10,0, 10,2.5, 10,5, 5,5, 0,5, 0,2.5, 0,0));
        expectNoFinding("trapezoid", ring(0,0, 10,0, 8,5, 2,5, 0,0));
        expectNoFinding("32-vertex round footprint", circle(0, 0, 10, 32));
        expectNoFinding("128-vertex round footprint", circle(0, 0, 10, 128));
        expectNoFinding("degenerate triangle", ring(0,0, 10,0, 5,6, 0,0));
        System.out.println("PASS: " + passed + " OrthoGuard geometry regression tests");
    }

    private static void expectNoFinding(String name, List<Point2D.Double> points) {
        OrthoGuardGeometry.Analysis actual = OrthoGuardGeometry.inspect(points);
        if (actual != null) {
            throw new AssertionError(name + " should not be flagged; angles=" + actual.getCornerAnglesDegrees());
        }
        passed++;
        System.out.println("PASS ignored: " + name);
    }

    private static void expectFinding(String name, List<Point2D.Double> points, double expectedMinimumMaxAngle) {
        OrthoGuardGeometry.Analysis actual = OrthoGuardGeometry.inspect(points);
        if (actual == null) {
            throw new AssertionError(name + " should have been flagged");
        }
        if (actual.getMaximumCornerAngleDegrees() < expectedMinimumMaxAngle) {
            throw new AssertionError(name + " max angle too small: " + actual.getMaximumCornerAngleDegrees());
        }
        if (actual.getCornersNotNinety() < 1) {
            throw new AssertionError(name + " had no non-90-degree corners");
        }
        passed++;
        System.out.println("PASS flagged: " + name + " " + actual.getCornerAnglesDegrees());
    }


    private static void expectNonRightCornerCount(String name, List<Point2D.Double> points, int expectedCount) {
        OrthoGuardGeometry.Analysis actual = OrthoGuardGeometry.inspect(points);
        if (actual == null) {
            throw new AssertionError(name + " should have been flagged");
        }
        if (actual.getCornersNotNinety() != expectedCount) {
            throw new AssertionError(name + " expected " + expectedCount + " non-90-degree corners, got "
                    + actual.getCornersNotNinety() + " for " + actual.getCornerAnglesDegrees());
        }
        passed++;
        System.out.println("PASS counted: " + name + " " + actual.getCornerAnglesDegrees());
    }

    private static List<Point2D.Double> ring(double... coordinates) {
        List<Point2D.Double> points = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            points.add(new Point2D.Double(coordinates[i], coordinates[i + 1]));
        }
        return points;
    }

    private static List<Point2D.Double> circle(double cx, double cy, double radius, int vertices) {
        List<Point2D.Double> points = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            double a = 2.0 * Math.PI * i / vertices;
            points.add(new Point2D.Double(cx + radius * Math.cos(a), cy + radius * Math.sin(a)));
        }
        points.add(new Point2D.Double(points.get(0).x, points.get(0).y));
        return points;
    }

    private static List<Point2D.Double> rotate(List<Point2D.Double> points, double degrees) {
        double angle = Math.toRadians(degrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        List<Point2D.Double> rotated = new ArrayList<>();
        for (Point2D.Double p : points) {
            rotated.add(new Point2D.Double(p.x * cos - p.y * sin, p.x * sin + p.y * cos));
        }
        return rotated;
    }
}
