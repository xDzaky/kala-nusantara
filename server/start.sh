#!/bin/bash
# ============================================================
# start.sh — Mulai server Kala Nusantara
# ============================================================
cd "$(dirname "$0")"

if pgrep -f "paper-26.3" > /dev/null; then
    echo "⚠️  Server sudah berjalan! (PID: $(pgrep -f paper-26.3))"
    echo "Gunakan stop.sh untuk menghentikannya terlebih dahulu."
    exit 1
fi

SERVER_IP=$(hostname -i 2>/dev/null | awk '{print $1}')
SERVER_IP=${SERVER_IP:-localhost}

echo "🚀 Memulai Kala Nusantara Server..."
echo "   IP Java: ${SERVER_IP}:25565"
echo "   IP Bedrock: ${SERVER_IP}:19132"
echo ""

exec java \
  -Xms2G -Xmx4G \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseG1GC \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1HeapRegionSize=8M \
  -XX:G1ReservePercent=20 \
  -XX:InitiatingHeapOccupancyPercent=15 \
  -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:MaxGCPauseMillis=200 \
  -XX:+DisableExplicitGC \
  -XX:SurvivorRatio=32 \
  -XX:+PerfDisableSharedMem \
  -XX:MaxTenuringThreshold=1 \
  -Dfile.encoding=UTF-8 \
  -jar paper-26.3-218.jar nogui
