#!/bin/bash
# Verifies all four repos in dependency order: compile, runData (output must match what is committed),
# a headless boot, then GameTests. Stops at the first failure.
# Usage: verify_all.sh [LOG_DIR] [REPO...]   (default: all four; logs go to LOG_DIR, default a temp dir)
set -o pipefail
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPTS/../.." && pwd)"
LOG_DIR="${1:-$(mktemp -d)}"
shift 2>/dev/null
REPOS=("${@:-CoFHCore ThermalCore ThermalDynamics ThermalExpansion}")
REPOS=(${REPOS[@]})
mkdir -p "$LOG_DIR"

# The Thermal repos build against whatever CoFHCore (and ThermalCore) have checked out.
BRANCH=$(git -C "$ROOT/CoFHCore" branch --show-current)
for repo in CoFHCore ThermalCore ThermalDynamics ThermalExpansion; do
    b=$(git -C "$ROOT/$repo" branch --show-current)
    if [ "$b" != "$BRANCH" ]; then
        echo "FAIL: $repo is on '$b', CoFHCore is on '$BRANCH'"
        exit 1
    fi
done
echo "Branch $BRANCH, logs in $LOG_DIR"

step() {

    local repo=$1 name=$2
    shift 2
    printf '%-18s %-12s' "$repo" "$name"
    if (cd "$ROOT/$repo" && "$@") > "$LOG_DIR/$repo-$name.log" 2>&1; then
        echo "ok"
    else
        echo "FAIL (see $LOG_DIR/$repo-$name.log)"
        exit 1
    fi
}

data_unchanged() {

    local before after
    before=$(git status --porcelain -- src/main/generated src/main/resources)
    ./gradlew runData --console=plain || return 1
    after=$(git status --porcelain -- src/main/generated src/main/resources)
    if [ "$before" != "$after" ]; then
        echo "runData changed committed output:"
        diff <(echo "$before") <(echo "$after")
        return 1
    fi
}

for repo in "${REPOS[@]}"; do
    step "$repo" compile ./gradlew compileJava --console=plain
    step "$repo" data data_unchanged
    step "$repo" boot "$SCRIPTS/verify_runserver.sh" . "$LOG_DIR/$repo-server.log" 300
    if [ -d "$ROOT/$repo/src/gametest" ]; then
        step "$repo" gametests ./gradlew runGameTestServer --console=plain
    fi
done
echo "All checks passed."
