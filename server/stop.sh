#!/bin/bash
# ============================================================
# stop.sh — Hentikan server Kala Nusantara dengan bersih
# ============================================================
cd "$(dirname "$0")"

PID=$(pgrep -f "paper-26.3")
if [ -z "$PID" ]; then
    echo "Server tidak berjalan."
    exit 0
fi

echo "Mengirim stop signal ke server (PID: $PID)..."
# Kirim SIGTERM untuk graceful shutdown
kill -TERM $PID

# Tunggu sampai 30 detik
for i in $(seq 1 30); do
    sleep 1
    if ! pgrep -f "paper-26.3" > /dev/null; then
        echo "Server berhenti dengan bersih."
        exit 0
    fi
    echo -n "."
done

echo ""
echo "Server tidak berhenti, force kill..."
kill -9 $PID 2>/dev/null
echo "Server dihentikan paksa."
