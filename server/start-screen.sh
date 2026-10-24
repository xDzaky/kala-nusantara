#!/usr/bin/env bash
# ============================================================
# Kala Nusantara — Start Server di Screen Session
# Memudahkan akses konsol server tanpa block terminal
# ============================================================
# PENGGUNAAN:
#   ./start-screen.sh           → start di screen session
#   ./start-screen.sh attach    → langsung attach ke session
#   screen -r kala-server       → attach manual
#   CTRL+A, D                   → detach dari screen
# ============================================================

SCREEN_NAME="kala-server"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if screen -list | grep -q "$SCREEN_NAME"; then
  echo "[INFO] Screen session '$SCREEN_NAME' sudah berjalan."
  if [[ "$1" == "attach" ]]; then
    screen -r "$SCREEN_NAME"
  else
    echo "       Gunakan: screen -r $SCREEN_NAME  (untuk attach)"
    echo "       Atau: ./stop.sh  (untuk stop)"
  fi
else
  echo "[INFO] Memulai server di screen session '$SCREEN_NAME'..."
  screen -dmS "$SCREEN_NAME" bash "$SCRIPT_DIR/start.sh"
  sleep 2
  if screen -list | grep -q "$SCREEN_NAME"; then
    echo "[OK]   Server started. Gunakan: screen -r $SCREEN_NAME"
    if [[ "$1" == "attach" ]]; then
      screen -r "$SCREEN_NAME"
    fi
  else
    echo "[ERROR] Screen session tidak berhasil dibuat."
    exit 1
  fi
fi
