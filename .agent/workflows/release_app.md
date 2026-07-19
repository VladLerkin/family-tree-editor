---
description: description: Release a new version of the app (Bump version, Commit, Tag, Push)
---

1. Run `./up_version.sh` to automatically bump the `app-version` and `app-versionCode` in `gradle/libs.versions.toml`.
2. Ask the user what changes should be included in the release notes, OR review the recent git commits to generate a summary of changes.
3. Update `RELEASE_NOTES.md` by prepending a new section for the new version (e.g., `## v1.0.8 (Month Year)`) and listing the changes.
4. Run the release script (which will read the new version, commit the changes, and tag the release):
// turbo
./release_app.sh