#!/bin/bash

# release_app.sh
# Automates the release process:
# 1. Bumps app.version (PATCH) and app.versionCode in gradle.properties
# 2. Commits the change
# 3. Tags the release
# 4. Pushes to origin (main + tags)

set -e

PROPERTIES_FILE="gradle.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "Error: $PROPERTIES_FILE not found!"
    exit 1
fi

# Function to get property value
get_property() {
    grep "^$1=" "$PROPERTIES_FILE" | cut -d'=' -f2
}

# 1. Read current versions
CURRENT_VERSION=$(get_property "app.version")
CURRENT_CODE=$(get_property "app.versionCode")

if [ -z "$CURRENT_VERSION" ] || [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not read app.version or app.versionCode from $PROPERTIES_FILE"
    exit 1
fi

echo "Current Version: $CURRENT_VERSION"
echo "Current Version Code: $CURRENT_CODE"

# 2. Calculate new versions
# Split version into components (assuming MAJOR.MINOR.PATCH)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"
NEW_CODE=$((CURRENT_CODE + 1))

echo "New Version: $NEW_VERSION"
echo "New Version Code: $NEW_CODE"

# 3. Update gradle.properties (MacOS compatible sed)
sed -i '' "s/^app.version=.*/app.version=$NEW_VERSION/" "$PROPERTIES_FILE"
sed -i '' "s/^app.versionCode=.*/app.versionCode=$NEW_CODE/" "$PROPERTIES_FILE"

echo "Updated $PROPERTIES_FILE"

# 4. Git operations
git add "$PROPERTIES_FILE"
COMMIT_MSG="Release v$NEW_VERSION"
git commit -m "$COMMIT_MSG"

TAG_NAME="v$NEW_VERSION"
git tag "$TAG_NAME"

echo "Commited and tagged: $TAG_NAME"

# 5. Push
echo "Pushing to origin..."
git push origin main
git push origin "$TAG_NAME"

echo "Release $NEW_VERSION completed successfully!"
