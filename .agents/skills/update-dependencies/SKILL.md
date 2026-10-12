---
name: update-dependencies
description: Check for dependency and GitHub Actions updates, and update stable versions across gradle and workflow files
---

1. Run `python3 .agents/skills/update-dependencies/scripts/check_updates.py` (from the project root). This script fetches the latest versions for all project dependencies (`gradle/libs.versions.toml`) as well as GitHub Actions used in workflows (`.github/workflows/*.yml`), outputting summary tables.
2. Review the output from both tables. Look for rows where the status is `Update Avail`.
3. Determine which dependencies and GitHub Actions have new **stable** versions available. Do not update to pre-release, alpha, or beta versions (such as `alpha`, `beta`, `RC`, etc.) unless the user explicitly requests it.
4. For each stable update available, update the corresponding version strings:
   - **Gradle dependencies**: Update the version in `gradle/libs.versions.toml`.
   - **Gradle plugins in settings**: **CRITICAL** — you must ALSO update the version in `settings.gradle.kts` if the updated dependency is a plugin defined there. For example, if `kotlin`, `compose`, `agp`, `serialization`, or `foojay-resolver` is updated, you MUST replace their version strings in the `plugins { ... }` block of `settings.gradle.kts` to match the TOML file.
   - **Other Gradle files**: Check `*.gradle.kts` files (like `build.gradle.kts`, `app-*/build.gradle.kts`) for any hardcoded versions of those dependencies.
   - **GitHub Actions**: Update the action version tags (e.g. `uses: actions/checkout@v5`, `uses: actions/setup-java@v5`, `uses: gradle/actions/setup-gradle@v5`) across all workflow files in `.github/workflows/` (such as `ci.yml` and `release.yml`).
5. Build the project (`./gradlew :app-desktop:run` or build checks) to verify that updates did not break the build.
6. **Documentation**: Also update `README.md` and any other documentation files if necessary to document which dependency versions were updated (e.g. in the Tech Stack & Dependencies table).
7. Present the summary of updated dependencies and actions to the user and ask if they want to test or commit the changes.
