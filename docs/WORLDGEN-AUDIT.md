# Kala Nusantara — World Generation & Environment Audit
**Dokumen:** `docs/WORLDGEN-AUDIT.md`  
**Waktu Audit & Implementasi:** 2026-10-24 20:55 WIB  
**Status Audit & Pengujian:** TAHAP A, B, C, D, F LULUS & TERVERIFIKASI  
**Tujuan:** Memverifikasi arsitektur lingkungan server, status storage, struktur dimensi dunia, implementasi TerraformGenerator di semua overworld world, integrasi BetterStructures, dan validasi pre-generation Chunky.

---

## 1. Verifikasi Lingkungan & Runtime Server

| Komponen | Target Dokumen | Hasil Inspeksi Aktual (Filesystem/Runtime) | Status | Catatan Audit |
|---|---|---|---|---|
| **Minecraft / Server Core** | Paper 26.3-218 | Paper 26.3-218-main@1b9bf5c (API 26.3.build.218-stable) | ✅ Terkonfirmasi | Paper berjalan stabil, TPS 20.0/20.0/20.0. |
| **Java Runtime (Server)** | OpenJDK 25/26 | OpenJDK 64-Bit Server VM build 26.0.1 (mixed mode, sharing) | ✅ Terkonfirmasi | Server runtime berjalan pada Java 26.0.1. |
| **Java Compiler (Plugin Build)**| OpenJDK 21 | OpenJDK 21.0.8 (/usr/lib/jvm/java-21-openjdk) | ✅ Terkonfirmasi | Kompilasi KalaCore menggunakan Gradle 8.12 + Java 21 toolchain. |
| **Geyser (Bedrock Bridge)** | Geyser-Spigot | Geyser-Spigot v2.12.0-b1270, UDP Port 19132 | ✅ Terkonfirmasi | STUN discovery aktif, Bedrock bridging siap. |
| **Floodgate (Bedrock Auth)** | Floodgate-Spigot | floodgate-spigot v2.2.5-SNAPSHOT (b141) | ✅ Terkonfirmasi | Autentikasi Bedrock tanpa lisensi ganda. |
| **TerraformGenerator** | v28.0.0 | TerraformGenerator v28.0.0 (Pack v26_3) | ✅ Terkonfirmasi | Injeksi sukses di semua overworld world (`kala_main`, `kala_resource`, `kala_resource_test`, `kala_terraform_test`). |
| **BetterStructures** | v2.7.7 | BetterStructures v2.7.7 | ✅ Terkonfirmasi | Fully initialized, mendeteksi v26, terhubung ke ValidWorlds. |
| **Chunky (Pre-generation)** | Chunky Bukkit | Chunky 1.5.3 (304.6 KB) | ✅ Terkonfirmasi | Berhasil pre-gen 729 chunk di `kala_terraform_test` (1m14s, 11 cps, TPS 20.0). |
| **Storage Kapasitas** | Cukup untuk pre-gen | Filesystem `/dev/sda2`, Total: 928G, Digunakan: 770G, **Tersedia: 154G (84%)** | ✅ Terkonfirmasi | Kapasitas sangat aman untuk pre-gen bertahap. |

---

## 2. Status Plugin & Modul Server

| Plugin | Versi Jar | Status di Runtime | Peran Kala Nusantara | Status Implementasi |
|---|---|---|---|---|
| **KalaCore** | 0.1.0 (13 MB) | ✅ Aktif (Enabled) | Master Core: Quest, Profesi, Ekonomi Rupiah, Musim, Ekspedisi, Proyek, In-House NusantaraStructureEngine, ResourceWorldManager | In-House Core aktif, Vault provider mandiri, generator hooks siap. |
| **TerraformGenerator** | 28.0.0 (2.87 MB) | ✅ Aktif (Enabled) | Procedural world generator enhanced-vanilla | Diimplementasikan di semua overworld world (`bukkit.yml` & `ResourceWorldManager`). |
| **BetterStructures** | 2.7.7 (7.14 MB) | ✅ Aktif (Enabled) | Struktur modular & dungeon eksplorasi | Aktif di semua overworld world terdaftar. |
| **Chunky** | 1.5.3 (297 KB) | ✅ Aktif (Enabled) | Pre-generator chunk terkontrol | Teruji pada radius 200–250 blok tanpa drop TPS. |
| **Geyser-Spigot** | 2.12.0 (51.9 MB)| ✅ Aktif (Enabled) | Crossplay Java-Bedrock | Aktif port UDP 19132. |
| **floodgate** | 2.2.5 (11.5 MB) | ✅ Aktif (Enabled) | Autentikasi Bedrock | Aktif. |
| **GriefPrevention** | 16.18.4 (365 KB)| ✅ Aktif (Enabled) | Proteksi klaim lahan pemain | Aktif, terintegrasi dengan structure safety guard. |
| **LuckPerms** | 5.5.88 (1.5 MB) | ✅ Aktif (Enabled) | Permission & role access admin/player | Aktif. |
| **PlaceholderAPI** | 2.12.3 (1.16 MB)| ✅ Aktif (Enabled) | Placeholder hook untuk Scoreboard/Tab | Aktif. |
| **VaultUnlocked** | 2.20.2 (134 KB) | ✅ Aktif (Enabled) | Service API Economy & Perms | Terhubung ke KalaVaultEconomyProvider. |
| **WorldEdit** | 7.4.6 (7.87 MB) | ✅ Aktif (Enabled) | World editing & terrain manipulation | Aktif. |
| **spark** | 1.10.191 | ✅ Aktif (Enabled) | Profiler tick, MSPT, dan memory leak check | Aktif, median tick duration 0.5 ms. |

---

## 3. Verifikasi Arsitektur Dimensi & Status Generator

Arsitektur Paper 26.3 dengan TerraformGenerator diimplementasikan pada seluruh overworld world:

| World Name | Environment | Generator Aktif | Ukuran Disk | Peran & Kebijakan |
|---|---|---|---|---|
| **`kala_main`** | NORMAL | `TerraformGenerator` | 19 MB | Dunia utama (Labuhan Awal, Kampung, Koridor Gunung Murka). Chunks baru menggunakan Terraform terrain. |
| **`kala_resource`** | NORMAL | `TerraformGenerator` | 11 MB | Dunia tambang/resource yang dapat di-reset berkala via `/ka resource reset confirm`. |
| **`kala_resource_test`**| NORMAL | `TerraformGenerator` | 18 MB | Staging test world untuk pengujian reset & pre-gen. |
| **`kala_terraform_test`**| NORMAL| `TerraformGenerator` | 15 MB | Staging world khusus evaluasi terrain Terraform & BetterStructures (729 chunks pre-generated). |
| **`the_nether`** | NETHER | Vanilla Minecraft | 2.2 MB | Dibiarkan vanilla agar portal & progression stabil. |
| **`the_end`** | THE_END | Vanilla Minecraft | 2.2 MB | Dibiarkan vanilla agar Dragon fight & end city stabil. |

---

## 4. Hasil Uji Performa Pre-Generation (Tahap F)

Pengujian dilakukan menggunakan Chunky pada staging world `kala_terraform_test` dengan generator aktif TerraformGenerator + BetterStructures + NusantaraStructureEngine:

- **Radius Pre-gen:** 200 blok (square region, total 729 chunks).
- **Durasi Eksekusi:** 01 menit 14 detik.
- **Rata-rata Chunk Generation Rate:** 10.0 – 12.4 chunks/detik.
- **MSPT / Tick Duration:** Median 0.4 – 0.5 ms (ambang batas aman < 45 ms).
- **Stabilitas TPS:** 20.0 / 20.0 / 20.0 (tidak ada tick drop fatal).
- **Struktur Berhasil Terpasang:** 
  - `NusantaraStructureEngine`: Berhasil mendirikan `POS_PELAUT` di (184, 64, 88) dan `GUA_TAMBANG` di (-168, 89, -200).
  - `BetterStructures`: Footprint check berjalan aktif mencegah tabrakan dengan spawn area.
- **Ukuran Folder World:** Meningkat dari 48 KB menjadi 15 MB (rasio storage terprediksi dan efisien).
