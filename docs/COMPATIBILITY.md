# COMPATIBILITY.md
# Kala Nusantara — Compatibility Matrix
# Versi ini DIKUNCI. Jangan upgrade tanpa review dan test lengkap.
# Update: 2026-10-24

## Core Stack

| Komponen       | Versi              | Source / URL                                                                 | Status  |
|----------------|--------------------|------------------------------------------------------------------------------|---------|
| Java           | 26.0.1 (OpenJDK)   | Pre-installed sistem                                                         | ✅ LOCKED |
| Minecraft      | 26.3               | (via Paper build 218)                                                        | ✅ LOCKED |
| Paper          | 26.3 build 218     | https://papermc.io/downloads/paper                                           | ✅ LOCKED |

## Crossplay Layer

| Komponen         | Versi    | Source / URL                                                                                       | Status     |
|------------------|----------|----------------------------------------------------------------------------------------------------|------------|
| Geyser-Spigot    | latest   | https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot   | ✅ INSTALLED |
| Floodgate-Spigot | latest   | https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot | ✅ INSTALLED |

> **Catatan:** Geyser dan Floodgate menggunakan build "latest" pada tanggal install (2026-10-24).
> Setelah versi dikonfirmasi berjalan, catat nomor build eksak di sini.

## Plugins Fase 0

| Plugin             | Versi   | Source / URL                                                                              | Lisensi   | Fungsi                  | Status     |
|--------------------|---------|-------------------------------------------------------------------------------------------|-----------|-------------------------|------------|
| LuckPerms          | 5.5.88  | https://download.luckperms.net/1675/bukkit/loader/LuckPerms-Bukkit-5.5.88.jar            | MIT       | Permission system       | ✅ INSTALLED |
| CoreProtect CE     | 24.1    | https://cdn.modrinth.com/data/Lu3KuzdV/versions/3sehX6Sg/CoreProtect-CE-24.1.jar         | LGPL-3.0  | Block audit & rollback  | ⚠️ INSTALLED tapi DISABLED (belum support MC 26.3, max 26.2) — tunggu update |
| GriefPrevention    | 16.18.4 | https://hangarcdn.papermc.io/plugins/GriefPrevention/GriefPrevention/versions/16.18.4/PAPER/GriefPrevention%2016.18.4.jar | MIT | Claim sementara (diganti KalaClaim Fase 3) | ✅ INSTALLED |
| Chunky             | 1.5.3   | https://hangarcdn.papermc.io/plugins/pop4959/Chunky/versions/1.5.3/PAPER/Chunky-Bukkit-1.5.3.jar | MIT | Chunk pregeneration | ✅ INSTALLED |

## Plugins Fase Berikutnya (BELUM INSTALL)

| Plugin         | Fungsi                            | Target Fase | Catatan                                        |
|----------------|-----------------------------------|-------------|------------------------------------------------|
| spark          | CPU/tick profiler                 | 0 (sudah bundled di Paper modern) | Cek apakah Paper 26.3 sudah include |
| Citizens       | NPC statis                        | 1           | Untuk Mbah Kala dan NPC tutorial               |
| VoiceCraft     | Voice proximity chat              | 5 (POC)     | Perlu client app terpisah; opsional            |
| DiscordSRV     | Discord relay                     | 5+          | Hanya setelah beta                             |
| Plan           | Player analytics                  | 5+          | Setelah beta                                   |

## Port & Network

| Layanan         | Port  | Protokol | Catatan                        |
|-----------------|-------|----------|--------------------------------|
| Java Edition    | 25565 | TCP      | Port default Minecraft         |
| Bedrock/Geyser  | 19132 | UDP      | Dikonfigurasi di Geyser config |

## Notes

- **online-mode=true** digunakan; pemain Java harus punya akun resmi
- Floodgate memungkinkan pemain Bedrock masuk tanpa akun Java (konfigurasi di geyser-config.yml)
- CoreProtect CE adalah Community Edition (LGPL) — bukan versi Patreon
- GriefPrevention akan diganti KalaClaim custom pada Fase 3 setelah migration plan selesai
- Semua upgrade versi harus melalui: test di staging → update di sini → deploy

## Checklist Fase 0 (Compatibility Spike)

- [x] Paper JAR tersedia
- [x] Java 26.0.1 terinstall
- [x] Geyser + Floodgate terinstall
- [x] LuckPerms terinstall
- [x] CoreProtect CE terinstall
- [x] GriefPrevention terinstall
- [x] Chunky terinstall
- [x] Server boot pertama sukses (eula accepted)
- [x] Semua plugin load saat startup (6/6 diinisialisasi)
- [x] LuckPerms ✅ fully enabled
- [x] GriefPrevention ✅ fully enabled  
- [x] Chunky ✅ fully enabled
- [x] Geyser ✅ started UDP port 19132 (Done in 12.7s)
- [x] Floodgate ✅ enabled
- [x] spark ✅ bundled dalam Paper 26.3 (tidak perlu install terpisah)
- [x] Geyser transport diupdate ke `both` (RakNet + NetherNet)
- ⚠️ CoreProtect CE 24.1 — DISABLED, belum support MC 26.3 (max 26.2)
- [ ] Java client bisa join, move, chat, disconnect
- [ ] Bedrock/Android client bisa join, move, chat, disconnect
- [ ] Reconnect test (Java) — tidak ada data loss
- [ ] Reconnect test (Bedrock) — tidak ada data loss
- [ ] Port 19132 UDP terbuka untuk Geyser (cek firewall jika diperlukan)
