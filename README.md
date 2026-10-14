# JOSM OrthoGuard

**Version:** 0.2.1 (non-90-degree corner detection fix)  
**Target JOSM revision:** r19613  
**Target runtime:** Java 21 (plugin bytecode targets Java 11)  
**Tagline:** Detect. Review. Correct.

OrthoGuard is a JOSM plugin prototype for building-footprint geometry quality assurance. It measures **interior corner angles** and flags likely rectangular footprints when any interior corner angle is **less than or greater than 90 degrees**. The scan, review dialog, and pre-upload warning all use the same detection logic.

## What changed in 0.2.1

Version 0.2.1 corrects the detection condition: any interior corner angle below or above 90° is considered non-orthogonal. Version 0.2.0 introduced direct interior-angle measurement instead of comparing edges against a dominant orthogonal axis.

- The review table shows all four interior corner angles; any angle that differs from 90° is marked with `*`.
- Exact and rotated rectangles are not flagged.
- Strongly skewed parallelogram-like footprints are flagged even when corner deviations are large.
- Nearly collinear intermediate nodes on straight edges are simplified before counting meaningful corners.
- Rounded shapes and polygons that retain more or fewer than four meaningful corners are excluded.
- Common explicit round-building values (`round`, `roundhouse`, `circular`, `cylindrical`, `dome`, `silo`, `storage_tank`, `water_tower`) are also excluded.
- Obvious trapezoids and irregular quadrilaterals are filtered using opposite-edge direction, side-length, and opposite-corner-angle heuristics.

## Important limits

- **A flag is a review candidate, not a finding of fact.** Geometry alone cannot determine whether a skewed footprint is a digitizing error or a real architectural shape.
- Only closed building ways (`building=*` or `building:part=*`) are analysed. Relation-based multipolygon buildings are not supported yet.
- Candidate classification uses geometric heuristics: four meaningful convex corners, opposite-edge direction difference up to 35°, opposite side-length difference up to 60%, and opposite corner-angle difference up to 40°. These limits are intended to catch substantial skew while excluding many non-rectangular quadrilaterals. Unusual shapes can still be ambiguous.
- The plugin flags an angle when its absolute difference from 90° exceeds a tiny floating-point epsilon (0.0000001°). Values below and above 90° are both flagged. There is no broad angular tolerance, so even small geometry deviations can be flagged; review the angle values before correcting.
- Correction uses JOSM's built-in Orthogonalize Shape command. That command can alter dimensions or distort genuine irregular buildings. Confirm the correction, inspect the resulting shape, and use Undo if needed.
- Shared nodes may affect adjacent objects during correction. Review neighbouring geometry, especially in attached buildings.
- The upload hook checks qualifying building ways affected by the pending upload and allows the mapper to review findings, upload anyway, or cancel.

## Build on Windows

Requirements: JDK 21 (or another supported JDK), Apache Ant, and a JOSM core JAR matching the target revision.

1. Place your downloaded JOSM r19613 JAR at `lib/josm-tested.jar`, or pass its path using `-Djosm.jar`.
2. Open Command Prompt or PowerShell in this project folder.
3. Run:

   ```powershell
   ant clean dist
   ```

   Or specify a path directly:

   ```powershell
   ant "-Djosm.jar=C:/path/to/josm-tested.jar" clean dist
   ```

4. The plugin JAR is created at `dist/orthoguard.jar`.

## Run the geometry regression tests

These tests use only the JDK and do not require a JOSM data layer or JUnit. From the project directory, run:

```powershell
mkdir build\geometry-test
javac -d build\geometry-test src\org\openstreetmap\josm\plugins\orthoguard\OrthoGuardGeometry.java test\OrthoGuardGeometrySelfTest.java
java -cp build\geometry-test OrthoGuardGeometrySelfTest
```

The tests cover exact and rotated rectangles, skewed and strongly skewed parallelograms, a displaced corner, intermediate edge nodes, trapezoids, round polygons with 32 and 128 vertices, and a degenerate triangle.

## Install for local testing

Copy `dist/orthoguard.jar` into `%APPDATA%\JOSM\plugins\`, restart JOSM, and enable OrthoGuard in Preferences → Plugins. The menu entry is under **More tools → OrthoGuard: Scan rectangular building angles...**.

## Verification status

- Compiled against the exact JOSM r19613 JAR provided for development.
- The geometry regression harness passes 10/10 cases in the development environment.
- The revised plugin must still be tested by the user in JOSM on representative real mapping data. In particular, verify that your Mapathoner examples are detected as expected and that curved building footprints remain unflagged. Treat this as an early test build, not a mature public release.

## License

GPL-2.0-or-later. See `LICENSE` and <https://www.gnu.org/licenses/old-licenses/gpl-2.0.html>.

## Contributing
Bug reports, feature ideas, and pull requests are welcome. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) first. The GitHub issue templates help capture JOSM/Java versions and reproducible geometry examples. This is an early prototype; the detector should be evaluated against representative real data before it is relied on in production mapping.

## Source repository contents
- `src/` — Java implementation.
- `test/` — lightweight geometry regression tests.
- `build.xml` — Apache Ant build.
- `LICENSE` — GPL-2.0-or-later.
- `.github/` — issue and pull-request templates.

Compiled plugin JARs are intended to be attached to versioned GitHub Releases rather than committed into the source tree. To build locally, provide the matching JOSM JAR as described above.
