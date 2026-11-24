#!/usr/bin/env bash
# EVO / FORGE — reproducible two-stage build.
#
# Stage 1 builds the standalone GPU jar against stable Java 22+ FFM (no preview),
# Stage 2 stages it into the OSGi wrapper's lib/, and Stage 3 builds the Tycho
# reactor (Java 21 baseline) which packages the RCP products.
#
# JDK resolution: override with EVO_JDK_FFM (>=22) and EVO_JDK_TYCHO (21); otherwise
# the script probes standard locations and falls back to JAVA_HOME.
#
# Usage:  ./build.sh [maven args...]   (extra args are forwarded to the reactor build)
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

log() { printf '[evo-build] %s\n' "$*"; }

# ---------------------------------------------------------------------------
# JDK resolution
# ---------------------------------------------------------------------------
java_major() { # $1 = javac path -> major version (empty on failure)
  "$1" -version 2>&1 | sed -n 's/.*javac \([0-9][0-9]*\).*/\1/p' | head -1
}

find_jdk() { # $1 = label, $2 = min major, $3... = candidate dirs
  local label="$1" min="$2"; shift 2
  local v
  for d in "$@"; do
    [ -n "$d" ] && [ -x "$d/bin/javac" ] || continue
    v="$(java_major "$d/bin/javac")"
    [ -n "$v" ] || continue
    if [ "$v" -ge "$min" ]; then
      printf '%s' "$d"
      return 0
    fi
  done
  return 1
}

FFM_JDK="${EVO_JDK_FFM:-}"
if [ -z "$FFM_JDK" ]; then
  FFM_JDK="$(find_jdk "stable FFM (>=22)" 22 \
    /usr/lib/jvm/java-25-openjdk-amd64 /usr/lib/jvm/openjdk-25 \
    /usr/lib/jvm/java-22-openjdk-amd64 /usr/lib/jvm/openjdk-22 \
    "${JAVA_HOME:-}")" \
    || { echo "ERROR: no JDK >= 22 found for stable FFM. Set EVO_JDK_FFM." >&2; exit 1; }
fi

TYCHO_JDK="${EVO_JDK_TYCHO:-}"
if [ -z "$TYCHO_JDK" ]; then
  TYCHO_JDK="$(find_jdk "Tycho (21)" 21 \
    /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/openjdk-21 \
    "${JAVA_HOME:-}")" \
    || { echo "ERROR: no JDK 21 found for the Tycho reactor. Set EVO_JDK_TYCHO." >&2; exit 1; }
fi

log "FFM JDK (GPU jar):  $FFM_JDK"
log "Tycho JDK (reactor): $TYCHO_JDK"

GPU_MODULE="eu.kalafatic.evolution.gpu"
GPU_JAR="$GPU_MODULE/target/eu.kalafatic.evolution.gpu.native-2.6.11-SNAPSHOT.jar"
GPU_LIB="eu.kalafatic.evolution.gpu.bundle/lib/gpu.jar"

# ---------------------------------------------------------------------------
# Stage 1 — GPU jar (stable FFM)
# ---------------------------------------------------------------------------
log "Stage 1/3: building GPU jar (stable FFM) with $FFM_JDK"
JAVA_HOME="$FFM_JDK" ./mvnw -q -f "$GPU_MODULE/pom.xml" clean package -DskipTests

if [ ! -f "$GPU_JAR" ]; then
  echo "ERROR: GPU jar was not produced: $GPU_JAR" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Stage 2 — stage the fresh jar into the wrapper (never a stale artifact)
# ---------------------------------------------------------------------------
log "Stage 2/3: staging GPU jar into wrapper lib/"
mkdir -p "$(dirname "$GPU_LIB")"
cp "$GPU_JAR" "$GPU_LIB"
log "Staged: $GPU_LIB ($(stat -c%s "$GPU_LIB") bytes)"

# ---------------------------------------------------------------------------
# Stage 3 — Tycho reactor
# ---------------------------------------------------------------------------
log "Stage 3/3: building Tycho reactor (products) with $TYCHO_JDK"
JAVA_HOME="$TYCHO_JDK" ./mvnw install "$@"

log "Build complete."
