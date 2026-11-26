#!/usr/bin/env bash
# EVO / FORGE — unified Java 25 LTS build.
#
# The entire EVO ecosystem is now built with a single Java 25 LTS JDK. The GPU
# module uses the stable Foreign Function & Memory API (java.lang.foreign),
# which is final in Java 25 (no --enable-preview), so there is no longer any
# separate Java 22 compilation stage.
#
# Stage 1 builds the standalone GPU jar (stable FFM, Java 25),
# Stage 2 stages it into the OSGi wrapper's lib/, and Stage 3 builds the Tycho
# reactor (Java 25 baseline) which packages the RCP products.
#
# JDK resolution: override with EVO_JDK (or JAVA_HOME); otherwise the script
# probes standard locations for a JDK >= 25.
#
# Usage:  ./build.sh [maven args...]   (extra args are forwarded to the reactor build)
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

log() { printf '[evo-build] %s\n' "$*"; }

# ---------------------------------------------------------------------------
# JDK resolution — one authoritative Java 25 JDK for the whole pipeline
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

JDK="${EVO_JDK:-}"
if [ -z "$JDK" ]; then
  JDK="$(find_jdk "Java 25 LTS" 25 \
    /usr/lib/jvm/java-25-openjdk-amd64 /usr/lib/jvm/openjdk-25 \
    "${JAVA_HOME:-}")" \
    || { echo "ERROR: no JDK >= 25 found. Set EVO_JDK (or JAVA_HOME)." >&2; exit 1; }
fi

log "JDK (whole pipeline): $JDK"

GPU_MODULE="eu.kalafatic.evolution.gpu"
GPU_JAR="$GPU_MODULE/target/eu.kalafatic.evolution.gpu.native-2.6.11-SNAPSHOT.jar"
GPU_LIB="eu.kalafatic.evolution.gpu.bundle/lib/gpu.jar"

# ---------------------------------------------------------------------------
# Stage 1 — GPU jar (stable FFM, Java 25)
# ---------------------------------------------------------------------------
log "Stage 1/3: building GPU jar (stable FFM) with $JDK"
JAVA_HOME="$JDK" ./mvnw -q -f "$GPU_MODULE/pom.xml" clean package -DskipTests

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
# Stage 3 — Tycho reactor (products)
# ---------------------------------------------------------------------------
log "Stage 3/3: building Tycho reactor (products) with $JDK"
JAVA_HOME="$JDK" ./mvnw install "$@"

log "Build complete."
