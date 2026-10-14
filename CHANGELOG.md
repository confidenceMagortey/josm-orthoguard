# Changelog

## 0.2.1 — non-90-degree corner detection fix
- Flag a likely rectangular footprint if any interior corner is less than or greater than 90° (beyond a tiny floating-point epsilon).
- Rename the review count to “Corners ≠90°” and mark every non-right angle in the angle summary.
- Update scan descriptions, upload warnings, README, and regression tests to match the symmetric threshold.


## 0.2.0 — interior-corner detection revision
- Replaced dominant edge-axis detection with measured interior corner angles. The original condition only flagged corners above 90 degrees; this was corrected in 0.2.1.
- Restricted candidates to convex four-corner footprints that pass a rectangular/parallelogram-shape heuristic.
- Simplified nearly collinear vertices along straight sides so rectangles with intermediate nodes can still be analysed.
- Excluded rounded/multi-corner polygons and explicitly round building types.
- Updated the review table to show each interior corner angle, mark angles above 90 degrees, and show the maximum angle.
- Updated pre-upload warnings to use the same new detector.
- Added 10 geometry regression tests covering exact and rotated rectangles, skewed/large-angle parallelograms, a displaced corner, intermediate edge nodes, trapezoids, round polygons, and degenerate shapes.

## 0.1.2 — first compiled test build
- Compiled successfully against the supplied JOSM r19613 JAR.
- Confirmed JOSM r19613 loads the plugin in an isolated test profile.
- Added the Java minimum-version manifest entry (11); plugin bytecode targets Java 11 and runs on the reported Java 21 runtime.

## 0.1.1 — initial source prototype
- Added a review table with map selection and zoom.
- Added individual and batch orthogonalization using JOSM's built-in command.
- Added an upload hook to warn about affected building footprints.
