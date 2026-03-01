#!/bin/bash
# Start both receiver-app and caller-app simultaneously

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cleanup() {
    echo "Stopping applications..."
    kill "$RECEIVER_PID" "$CALLER_PID" 2>/dev/null
    wait "$RECEIVER_PID" "$CALLER_PID" 2>/dev/null
    echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting receiver-app on :8080..."
java -jar "$SCRIPT_DIR/receiver-app/target/receiver-app-0.1.0-SNAPSHOT.jar" &
RECEIVER_PID=$!

echo "Starting caller-app on :8081..."
java -jar "$SCRIPT_DIR/caller-app/target/caller-app-0.1.0-SNAPSHOT.jar" &
CALLER_PID=$!

echo "Both apps started (receiver PID=$RECEIVER_PID, caller PID=$CALLER_PID)"
echo "Press Ctrl+C to stop."

wait "$RECEIVER_PID" "$CALLER_PID"
