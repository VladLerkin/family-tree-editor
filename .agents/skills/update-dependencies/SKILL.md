---
name: update-dependencies
description: Check for dependency updates and apply stable versions to both TOML and KTS files
---

1. Run the script `scripts/check_updates.py` (from the project root) to get a table of available updates.
   ```bash
   python3 .agents/skills/update-dependencies/scripts/check_updates.py
   ```
2. Analyze the output table to find dependencies where `Status` is `Update Avail`.
   - **CRITICAL EXCEPTION**: DO NOT update `vosk` under any circumstances (leave it at its current version).
3. For those dependencies, find the `Latest Stable` version from the output.
4. Update the versions in `gradle/libs.versions.toml`.
5. **Important**: Also check and update `*.gradle.kts` files (like `build.gradle.kts`, `settings.gradle.kts`, or `app-*/build.gradle.kts`) for any hardcoded versions of those dependencies.
6. **Documentation**: Also check and update `README.md` and any other documentation files that might contain the updated dependency versions (such as version badges or Tech Stack tables).
7. Present the summary of updated dependencies to the user and ask if they want to test or commit the changes.
