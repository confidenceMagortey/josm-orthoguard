# Contributing to OrthoGuard

Thank you for helping improve OrthoGuard.

## Before making changes
- Read `README.md` to understand the current limitations and build steps.
- Open an issue first for major changes or algorithm redesigns so we can discuss the intended behaviour.
- Keep bug reports specific and, where possible, include anonymized example geometry or a small reproducible test case.

## Development
- Target the JOSM revision stated in `README.md` unless a change explicitly updates compatibility.
- Add or update geometry regression tests in `test/OrthoGuardGeometrySelfTest.java` for detector changes.
- Do not treat a geometric flag as proof that a real building is incorrectly mapped. Preserve review-first behavior.
- Do not commit personal data, OAuth tokens, account credentials, or private mapping extracts.

## Pull requests
1. Describe the problem and proposed change.
2. Explain the behaviour before and after the change.
3. Include test results and note any untested JOSM UI/API behaviour.
4. Keep each pull request focused on one subject where practical.

All contributions are expected to remain under the project's GPL-2.0-or-later license, subject to applicable contributor rights.
