#!/bin/bash

# release_app.sh
# Automates the release process for Version Catalog:
# 1. Reads current app-version and app-versionCode from gradle/libs.versions.toml
# 2. Commits the changes
# 3. Tags the release
# 4. Pushes to origin (main + tags)

set -e

TOML_FILE="gradle/libs.versions.toml"

if [ ! -f "$TOML_FILE" ]; then
    echo "Error: $TOML_FILE not found!"
    exit 1
fi

# Function to get property value from TOML (stripping quotes)
get_toml_version() {
    grep "^$1 =" "$TOML_FILE" | cut -d'=' -f2 | tr -d ' "'
}

# 1. Read current versions
CURRENT_VERSION=$(get_toml_version "app-version")
CURRENT_CODE=$(get_toml_version "app-versionCode")

if [ -z "$CURRENT_VERSION" ] || [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not read app-version or app-versionCode from $TOML_FILE"
    exit 1
fi

echo "Releasing Version: $CURRENT_VERSION"
echo "Releasing Version Code: $CURRENT_CODE"

# 2. Git operations
git add .
COMMIT_MSG="Release v$CURRENT_VERSION"
git commit -m "$COMMIT_MSG"

TAG_NAME="v$CURRENT_VERSION"

# Extract release notes for the new version from RELEASE_NOTES.md
# It looks for the section starting with "**vCURRENT_VERSION**" and stops at the next "**v"
RELEASE_NOTES=$(awk "/^\*\*v$CURRENT_VERSION\*\*/ {flag=1; next} /^\*\*v/ {flag=0} flag" RELEASE_NOTES.md | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

if [ -n "$RELEASE_NOTES" ]; then
    git tag -a "$TAG_NAME" -m "Release $TAG_NAME" -m "$RELEASE_NOTES"
    echo "Commited and tagged: $TAG_NAME with release notes."
else
    git tag "$TAG_NAME"
    echo "Commited and tagged: $TAG_NAME (no release notes found in RELEASE_NOTES.md)."
fi

# 5. Push
echo "Pushing to origin..."
git push origin main
git push origin "$TAG_NAME"

echo "Release $CURRENT_VERSION completed successfully!"
