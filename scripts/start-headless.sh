#!/bin/bash
# Evolution Platform - Headless Product Starter

set -euo pipefail

PORT="${1:-28080}"
SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}/.."

JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' | cut -d. -f1)"
if [[ "${JAVA_MAJOR}" != "21" && -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    export PATH="${JAVA_HOME}/bin:${PATH}"
    JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' | cut -d. -f1)"
fi
if [[ "${JAVA_MAJOR}" != "21" ]]; then
    echo "ERROR: JDK 21 is required. Set JAVA_HOME to a JDK 21 installation." >&2
    exit 1
fi

PRODUCT_DIR="eu.kalafatic.evolution.repository/target/products/headless/linux/gtk/x86_64/headless"
LAUNCHER="${PRODUCT_DIR}/eclipse"

if [[ ! -x "${LAUNCHER}" ]]; then
    echo "Headless product not found: ${LAUNCHER}" >&2
    echo "Run scripts/build.sh first." >&2
    exit 1
fi

echo "Starting Evolution headless product on port ${PORT}..."
exec "${LAUNCHER}" -nosplash -consoleLog --port "${PORT}"
