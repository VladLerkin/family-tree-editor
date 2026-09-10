---
name: release-app
description: Release a new version of the app (Bump version, Commit, Tag, Push)
---

1. Run `scripts/up_version.sh` (from the project root) to automatically bump the `app-version` and `app-versionCode` in `gradle/libs.versions.toml`.
2. Ask the user what changes should be included in the release notes, OR review the recent git commits to generate a summary of changes.
3. Update `RELEASE_NOTES.md` by prepending a new section for the new version (e.g., `## v1.0.8 (Month Year)`) and listing the changes.
4. Run the release script from the project root (which will read the new version, commit the changes, and tag the release):
// turbo
scripts/release_app.sh
