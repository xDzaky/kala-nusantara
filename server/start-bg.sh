#!/bin/bash
# Mulai server di background (nohup)
cd "$(dirname "$0")"

if pgrep -f "paper-26.3" > /dev/null; then
    echo "⚠️  Server sudah berjalan (PID: $(pgrep -f paper-26.3))"
    exit 1
fi

echo "🚀 Memulai server di background..."
nohup bash start.sh > logs/stdout.log 2>&1 &
SERVER_PID=$!
echo "PID: $SERVER_PID"
echo "Monitor: tail -f logs/latest.log"
