#!/bin/sh

# ----------------------------------------------------------------------------
# Maven Wrapper bootstrap script (Unix)
# ----------------------------------------------------------------------------

set -e

WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$(dirname "$0")/.mvn/wrapper/maven-wrapper.properties"
BOOTSTRAP_CLASS="org.apache.maven.wrapper.MavenWrapperMain"

# Download maven-wrapper.jar if missing
if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL=$(grep -E '^wrapperUrl=' "$WRAPPER_PROPERTIES" | sed -e 's/^wrapperUrl=//')
  if [ -z "$WRAPPER_URL" ]; then
    echo "[ERROR] Cannot find wrapperUrl in $WRAPPER_PROPERTIES" >&2
    exit 1
  fi
  echo "Downloading Maven Wrapper from $WRAPPER_URL"
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR" || wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
fi

JAVA_EXE="java"
if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$JAVA_EXE" -cp "$WRAPPER_JAR" "$BOOTSTRAP_CLASS" "$@"
