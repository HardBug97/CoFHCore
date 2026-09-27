#!/bin/bash
# Headless boot check: runs ./gradlew runServer in REPO_DIR, waits for "Done (" and stops the server.
# Usage: verify_runserver.sh REPO_DIR LOG_FILE [MAX_WAIT]. Exits 0 on "Done", 1 on failure or timeout.
set -o pipefail
REPO_DIR="$(cd "$1" && pwd)" || exit 1
LOG_FILE="$2"
MAX_WAIT="${3:-300}"
[ -n "$LOG_FILE" ] || { echo "usage: $0 REPO_DIR LOG_FILE [MAX_WAIT]"; exit 1; }
case "$LOG_FILE" in /*) ;; *) LOG_FILE="$PWD/$LOG_FILE" ;; esac

# Server JVMs (ModDevGradle's server run, NeoGradle's forgeserveruserdev) whose working directory is this repo's run/,
# so a client or another repo's server is left alone.
repo_game_pids() {

    for pid in $(pgrep -f "devlaunch.Main.*serverRunProgramArgs|forgeserveruserdev"); do
        cwd=$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p')
        [ "$cwd" = "$REPO_DIR/run" ] && echo "$pid"
    done
}

stop_game() {

    for pid in $(repo_game_pids); do
        echo "Stopping game JVM (PID $pid)"
        kill "$pid" 2>/dev/null
        for _ in 1 2 3 4 5; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
        kill -9 "$pid" 2>/dev/null
    done
}

# Clear what a previous, interrupted run leaves behind.
stop_game
rm -f "$REPO_DIR/run/world/session.lock" "$LOG_FILE"

cd "$REPO_DIR" || exit 1
./gradlew runServer --console=plain > "$LOG_FILE" 2>&1 &
GRADLE_PID=$!

RESULT=1
elapsed=0
while [ $elapsed -lt "$MAX_WAIT" ]; do
    if grep -qE "\]: Done \(" "$LOG_FILE" 2>/dev/null; then
        echo "RESULT: SERVER STARTED SUCCESSFULLY"
        RESULT=0
        break
    fi
    if grep -qE "FAILURE|BUILD FAILED" "$LOG_FILE" 2>/dev/null || ! kill -0 "$GRADLE_PID" 2>/dev/null; then
        echo "RESULT: BUILD/START FAILED"
        break
    fi
    sleep 3
    elapsed=$((elapsed + 3))
done
[ $elapsed -ge "$MAX_WAIT" ] && echo "RESULT: TIMED OUT after ${MAX_WAIT}s"

echo "--- last 40 log lines ---"
tail -40 "$LOG_FILE"

stop_game
kill "$GRADLE_PID" 2>/dev/null
wait "$GRADLE_PID" 2>/dev/null
rm -f "$REPO_DIR/run/world/session.lock"
exit $RESULT
