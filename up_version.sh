#!/bin/bash

# up_version.sh
# Automates bumping the app-version (PATCH) and app-versionCode in gradle/libs.versions.toml

set -e

TOML_FILE="gradle/libs.versions.toml"

if [ ! -f "$TOML_FILE" ]; then
    echo "Error: $TOML_FILE not found!"
    exit 1
fi

get_toml_version() {
    grep "^$1 =" "$TOML_FILE" | cut -d'=' -f2 | tr -d ' "'
}

CURRENT_VERSION=$(get_toml_version "app-version")
CURRENT_CODE=$(get_toml_version "app-versionCode")

if [ -z "$CURRENT_VERSION" ] || [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not read app-version or app-versionCode from $TOML_FILE"
    exit 1
fi

echo "Current Version: $CURRENT_VERSION"
echo "Current Version Code: $CURRENT_CODE"

# Calculate new versions
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"
NEW_CODE=$((CURRENT_CODE + 1))

echo "New Version: $NEW_VERSION"
echo "New Version Code: $NEW_CODE"

# Update TOML (MacOS compatible sed)
sed -i '' "s/^app-version = .*/app-version = \"$NEW_VERSION\"/" "$TOML_FILE"
sed -i '' "s/^app-versionCode = .*/app-versionCode = \"$NEW_CODE\"/" "$TOML_FILE"

echo "Successfully updated $TOML_FILE to $NEW_VERSION (code $NEW_CODE)"
