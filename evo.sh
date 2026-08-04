#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(
    cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
    && pwd -P
)"

LAUNCHER="$SCRIPT_DIR/evo"

if [[ ! -x "$LAUNCHER" ]]; then
    echo "EVO launcher not found or not executable: $LAUNCHER" >&2
    exit 1
fi

exec "$LAUNCHER" "$@"
