# Kala Nusantara — V9 Changelog

Tanggal: 25 Oktober 2026  
Versi Target: KalaCore 0.1.0-V9 (Robust Multiplatform & Audit Compliance)  
Platform: PaperMC 26.3, Java 21/26, Geyser 2.12.0-b1270, floodgate 2.2.5

---

## 1. Perbaikan Prioritas Utama (Audit V9 Fixes)

### P0: CI Dependency Resolution & Clean Build Environment
- **Root Cause**: `plugin/kala-core/build.gradle` mengandalkan dependensi file lokal `files('../../server/plugins/VaultUnlocked-2.20.2.jar')`. Karena folder `.jar` di-git-ignore, GitHub Actions runner mengalami error kompilasi `package net.milkbowl.vault.economy does not exist` (Run ID: 40115755679).
- **Resolusi**:
  - Menambahkan JitPack Maven Repository `https://jitpack.io` ke konfigurasi `repositories`.
  - Mengganti file dependency lokal dengan artefak resmi: `compileOnly 'com.github.MilkBowl:VaultAPI:1.7.1'`.
  - Menambahkan Paper API dan VaultAPI ke dalam `testImplementation` sehingga pengujian unit JUnit 5 berjalan konsisten dan bersih di lingkungan CI.

### P0/P1: Dynamic ISO Week Village Contracts & Idempotency
- **Rotasi Minggu Dinamis**: Mengganti hardcoded `WEEK_2026_W43` di `VillageContractManager` dengan generator minggu ISO-8601 berbasis kalender waktu nyata `getCurrentIsoWeekId()` format `WEEK_YYYY_Www`.
- **Persistensi Progress**: Menambahkan pemuatan otomatis progres kontrak dari database SQLite (`player_contract_progress`) saat server start/reload serta saat pemain bergabung (`PlayerJoinEvent`).
- **Skema DB Idempotent**: Menambahkan kolom `week_id TEXT NOT NULL DEFAULT 'WEEK_INITIAL'` dengan constraint `UNIQUE(uuid, week_id, contract_id)` agar kontrak mingguan tidak dapat dibayar atau dikerjakan berulang secara tidak sah.

### P0/P1: Database-as-SSOT & Offline Reward Entitlements
- **Discovery Manager**: Memindahkan pembaruan cache in-memory `playerDiscoveries` setelah operasi `INSERT OR IGNORE` ke SQLite berhasil, mencegah inkonsistensi cache saat query gagal.
- **Tabel Entitlement**: Membuat tabel SQLite baru `player_reward_entitlements` dengan constraint `UNIQUE(uuid, source, source_id)`.
- **Offline Recovery**: Jika pemain offline saat event atau discovery reward selesai diproses, hak Rupiah Kala dan EXP tersimpan sebagai entitlement dan otomatis diklaim saat pemain login kembali melalui `StorageManager.claimPendingEntitlements()`.
- **Event Outbox EXP**: Menuntaskan parameter `exp` pada rekonsiliasi klaim event outbox di `EventContributionLedger` sehingga reward EXP tidak hilang saat rekonsiliasi asinkron.

### P1: Clarified Backup Scope & SHA-256 Checksum Verification
- **Klarifikasi Lingkup Backup**: Mengubah penamaan dari klaim berlebih "Full Server Backup" menjadi **"Core State Backup (DB + Configs + Manifest)"** untuk membedakan arsip status inti server dengan full world/region disaster recovery.
- **Verifikasi SHA-256 Aktif**: Memperbarui metode `BackupManager.verifyBackup(File zipFile)` agar membaca stream `kala_data.db` di dalam ZIP, menghitung ulang hash SHA-256 aktual, dan membandingkannya secara presisi dengan `database_sha256` di dalam `manifest.json`.

### P1: Geyser Custom Item Mapping Format V2
- **Migrasi Schema V2**: Memperbarui `resource-packs/mappings/geyser/custom_mappings.json` dari struktur legacy `format_version: "1"` (map berbasis key) menjadi Geyser Item Mapping `format_version: "2"` (array items terstruktur dengan field `name`, `java_item`, `bedrock_item`, `custom_model_data`, dan `icon`).
- **Sinkronisasi Manifest**: Memperbarui `resource-packs/manifest.yml` ke versi `9.0.0` dengan penanda engine `PaperMC 26.3 + Geyser 2.12.0 (Custom Items v2)`.

---

## 2. Pengujian Unit & Verifikasi
- Menambahkan `VillageContractIsoWeekTest` (JUnit 5): Memvalidasi pola regex `^WEEK_\d{4}_W\d{2}$`, batasan tahun, dan nomor minggu ISO.
- Menambahkan `BackupSha256VerificationTest` (JUnit 5): Memvalidasi integritas archive ZIP, pencocokan hash SHA-256, deteksi file tampered, dan penolakan file tanpa manifest.
- Total 17 unit test lulus tanpa kegagalan: `BUILD SUCCESSFUL in 22s`.
- Verifikasi server Paper 26.3 live console:
  - `kalaadmin status`: Aktif & operasional.
  - `kalaadmin backup full`: Menghasilkan `kala_core_backup_20261025_055157.zip` dengan manifest SHA-256.
  - `kalaadmin backup list`: Menampilkan status Core State Backups dan SQLite Database Snapshots.
