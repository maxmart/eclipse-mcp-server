#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

VERSION=$(<../VERSION)
JAR_NAME="eclipse.mcp.server_${VERSION}.jar"

# Eclipse plugins directory: argument > env var
PLUGINS="${1:-${ECLIPSE_PLUGINS:-}}"
if [ -z "$PLUGINS" ]; then
  echo "Usage: build.sh <eclipse-plugins-dir>" >&2
  echo "  or set ECLIPSE_PLUGINS env var" >&2
  exit 1
fi

# Resolve dependency JARs using globs (version-independent)
resolve() {
  local pattern="$1"
  local match
  match=$(ls "$PLUGINS"/$pattern 2>/dev/null | head -1)
  if [ -z "$match" ]; then
    echo "ERROR: No JAR matching $pattern in $PLUGINS" >&2
    exit 1
  fi
  echo "$match"
}

SEP=":"
JARS=""
for pattern in \
  "org.eclipse.osgi_*.jar" \
  "org.eclipse.equinox.common_*.jar" \
  "org.eclipse.core.runtime_*.jar" \
  "org.eclipse.core.resources_*.jar" \
  "org.eclipse.core.jobs_*.jar" \
  "org.eclipse.debug.core_*.jar" \
  "org.eclipse.debug.ui_*.jar" \
  "org.eclipse.ui_3.*.jar" \
  "org.eclipse.ui.console_*.jar" \
  "org.eclipse.ui.workbench_*.jar" \
  "org.eclipse.jface_*.jar" \
  "org.eclipse.swt_*.jar" \
  "org.eclipse.swt.gtk.linux.x86_64_*.jar" \
  "com.google.gson_*.jar" \
  "org.eclipse.jface.text_*.jar" \
  "org.eclipse.text_*.jar" \
; do
  jar=$(resolve "$pattern")
  JARS="${JARS:+$JARS$SEP}$jar"
done

echo "Compiling (target Java 17)..."
mkdir -p bin
javac --release 17 --add-modules jdk.httpserver -d bin -sourcepath src \
  -cp "$JARS" \
  src/eclipse/mcp/*.java src/eclipse/mcp/tools/*.java

echo "Building plugin JAR (v${VERSION})..."
jar cfm "$JAR_NAME" META-INF/MANIFEST.MF -C bin . plugin.xml
echo "Created $JAR_NAME"
