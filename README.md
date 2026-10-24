# 🌋 Kala Nusantara RPG — Minecraft Server

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4%20(26.3)-brightgreen.svg)](https://papermc.io)
[![Server Engine](https://img.shields.io/badge/Engine-PaperMC-blue.svg)](https://papermc.io)
[![Crossplay](https://img.shields.io/badge/Crossplay-Java%20%2B%20Bedrock%20MCPE-orange.svg)](https://geysermc.org)
[![Custom Plugin](https://img.shields.io/badge/Plugin-KalaCore%20v0.1.0-gold.svg)](./plugin/kala-core)

**Kala Nusantara** adalah proyek server Minecraft RPG Nusantara Survival Co-op berbasis arsitektur Paper 1.21.4 (build 26.3). Server ini dirancang khusus dengan dukungan penuh **crossplay Java Edition dan Bedrock Edition (HP/MCPE)**, mengusung tema kepulauan Nusantara, eksplorasi budaya, aktivitas vulkanik Gunung Murka, sistem profesi dan ekonomi Rupiah Kala, serta siklus bencana musiman yang berorientasi gotong royong non-destruktif.

---

## 🌟 Fitur Utama

### 1. Engine Inti & Plugin Khusus (`KalaCore`)
- **Bahasa & Arsitektur:** Java 21, Gradle 8.12, Paper API 1.21.4.
- **Penyimpanan:** SQLite WAL Mode (`kala_data.db`) dengan query asynchronous non-blocking.
- **Ekonomi:** Mata uang Rupiah Kala terintegrasi dengan `KalaVaultEconomyProvider` resmi untuk Vault.
- **Profesi & Jurnal:** 4 Profesi Nusantara (Petani, Penambang, Nelayan, Tukang Kayu) dengan sistem level, passive perk, dan jurnal petualangan otomatis.
- **Atlas Fauna Nusantara:** Penemuan 6 satwa endemik pulau (Komodo, Jalak Bali, Cendrawasih, Anoa, Bekantan, Harimau Sumatra) dengan GUI sentuh `/fauna`.

### 2. Aktivitas Vulkanik Gunung Murka (V6)
- **State Machine 5 Fase:** `TENANG` → `WASPADA` → `SIAGA` → `PUNCAK` → `PEMULIHAN`.
- **Encounter 6 Subfase PUNCAK:**
  1. `Pembuka Kawah` (0–45s) — Gempa seismik & title peringatan.
  2. `Gelombang Abu` (45–120s) — Hujan partikel abu vulkanik di seluruh kepulauan.
  3. `Salvo Rockfall` (120–240s) — Hujan salvo batuan tertelegrafis berkala.
  4. `Retakan Panas` (240–360s) — Hotspot uap kawah yang harus dihindari.
  5. `Encounter Bara` (360–480s) — Ancaman minion Percik Bara kawah.
  6. `Objective Final` (480–600s) — Seruan gotong royong Altar persembahan.
- **Sajen Gotong Royong:** Penyerahan persembahan di Altar (`/gunung`) untuk menenteramkan Murka dan membuka berkah kesuburan tanah.

### 3. Keamanan Total Aset Pemain (`WorldImpactPolicy Level 0`)
- **Zero Block Destruction:** 100% dijamin tidak ada blok terrain, bangunan rumah pemain, container, atau redstone yang hancur oleh event bencana.
- **Ground Telegraphing (2.5 Detik):** Lingkaran penanda api dan suara peringatan muncul di tanah sebelum batu jatuh, memberikan jendela waktu menghindar yang adil bagi pemain touch screen (Bedrock MCPE).
- **Intersepsi Event:** `EntityChangeBlockEvent` dicegat agar `FallingBlock` tidak memadat menjadi blok terrain di dunia.

### 4. Bencana Musiman Berbasis Gameplay (`SeasonalEventDirector`)
- Terhubung dengan `SeasonManager` siklus 8 minggu kalender Nusantara.
- **Badai Selat:** Hujan lebat, kabut laut pesisir, dan penyelamatan 8 Peti Hanyut kargo rempah & logistik di perairan pantai.
- **Abu dan Bara:** Titik bara semu berasap yang harus dipadamkan warga dengan interaksi klik, serta pengambilan Sampel Abu Vulkanik untuk riset desa.

### 5. Buku Besar Partisipasi Anti-Eksploitasi (`EventContributionLedger`)
- Tabel SQLite `event_contribution_ledger` dengan compound unique constraint `(event_id, uuid, objective_id)`.
- Menggunakan kurva **diminishing returns** (>80 poin) untuk mencegah monopoli hadiah.
- Proteksi klaim hadiah satu kali per event via `/kala claim-event <id_event>`.

### 6. Dukungan Penuh Crossplay (Bedrock MCPE + Java)
- Kompatibilitas Geyser 2.12.0 & Floodgate 2.2.5.
- GUI inventori sentuh tanpa kebutuhan tombol keyboard/shift-click.
- Notifikasi terpusat via `KalaNotificationService` yang tidak menutupi layar perangkat mobile.

---

## 📁 Struktur Repositori

```text
kala-nusantara/
├── docs/                        # Dokumentasi lengkap, desain arsitektur & implementation reports
│   ├── Kala-Nusantara-Master-Design-v2.0.md
│   ├── Kala-Nusantara-V6-Volcano-and-Seasonal-Events-Design.md
│   ├── Kala-Nusantara-V6-Volcano-and-Seasonal-Events-Implementation-Report-v6.md
│   └── ...
├── plugin/
│   └── kala-core/               # Source code plugin inti KalaCore (Java 21 + Gradle)
│       ├── build.gradle
│       ├── settings.gradle
│       └── src/main/java/id/kalanusantara/core/
│           ├── bootstrap/       # Lifecycle & startup checks
│           ├── commands/        # Perintah pemain & admin
│           ├── event/           # V6 Volcano Encounter, Hazard & Seasonal Directors
│           ├── fauna/           # Atlas Fauna Nusantara
│           ├── journal/         # Jurnal Penjelajah
│           ├── kala/            # State machine Gunung Murka & sajen altar
│           ├── listeners/       # Event handlers & world impact guard
│           ├── notify/          # UI ownership & prioritized notification service
│           ├── profession/      # Sistem profesi & level
│           ├── season/          # Kalender musim 8 minggu
│           ├── storage/         # SQLite persistence
│           └── ui/              # Menu GUI, Scoreboard & TabList
└── server/                      # Konfigurasi server Paper & plugin
    ├── config/                  # Konfigurasi Paper global & world defaults
    ├── plugins/                 # Konfigurasi plugin (BetterStructures, GriefPrevention, LuckPerms, dll)
    └── server.properties        # Pengaturan jaringan & gameplay server
```

---

## 🛠️ Cara Membangun & Menjalankan Server

### 1. Prasyarat
- Java JDK 21 atau lebih baru
- Gradle 8.12+ (atau gunakan gradle wrapper)
- PaperMC 1.21.4 (build 26.3-218)

### 2. Kompilasi Plugin KalaCore
```bash
cd plugin/kala-core
gradle shadowJar --no-daemon
```
File jar yang telah dikompilasi akan berada di `plugin/kala-core/build/libs/kala-core-0.1.0-SNAPSHOT.jar`. Salin file ini ke folder `server/plugins/KalaCore-0.1.0.jar`.

### 3. Menjalankan Server Paper
```bash
cd server
java -Xms2G -Xmx4G -XX:+UseG1GC -jar paper-26.3-218.jar nogui
```

---

## 🎮 Perintah Penting

### Perintah Pemain:
- `/kompas` — Membuka menu navigasi utama Nusantara.
- `/jurnal` — Membuka jurnal catatan ekspedisi & milestone pemain.
- `/gunung` — Melihat status Gunung Murka & menyetorkan sajen persembahan.
- `/fauna` — Membuka Atlas Satwa Endemik Nusantara.
- `/kala saldo` — Memeriksa saldo Rupiah Kala.
- `/kala claim-event <id_event>` — Mengklaim imbalan gotong royong pasca event bencana.

### Perintah Operator / Admin:
- `/ka status` — Memeriksa status dan kesehatan engine KalaCore.
- `/ka encounter <start|next|stop>` — Mengontrol subfase PUNCAK erupsi Gunung Murka.
- `/ka season-event <badai|abu|stop|status>` — Menguji mikro-event bencana musiman.
- `/ka rockfall [jumlah]` — Meluncurkan salvo lontaran batu tertelegrafis di lokasi pengirim.
- `/ka vent` — Memunculkan retakan panas uap sementara selama 30 detik.
- `/ka reload` — Memuat ulang konfigurasi, bahasa, dan data quest.

---

## 📜 Lisensi & Kontributor

Dikembangkan oleh **Achmad Dzaki Habibullah Al Azhar ([@xDzaky](https://github.com/xDzaky))** untuk komunitas Minecraft Indonesia.
Semua hak cipta dilindungi.
