# Kala Nusantara — Status Implementasi Worldbuilding v3

**Tanggal pembaruan:** 24 Oktober 2026  
**Rujukan:** `Kala-Nusantara-Worldbuilding-Implementation-Report-v3.md`  
**Scope:** implementasi aman pada staging dan kode. Tidak ada perubahan langsung pada `kala_main` produksi.

## Yang sudah diimplementasikan dan dibuktikan

- `kala_resource_test` dibuat sebagai world staging terisolasi dengan border 1.000 blok.
- Chunky menyelesaikan pre-generation square radius 250 di staging: 1.089 chunk, selesai dalam 57 detik (lihat `server/logs/latest.log`).
- `NusantaraStructureEngine` menghasilkan empat struktur terkurasi pada staging: Pos Pelaut, Menara Pengawas, Kuil Kala, dan Gua Tambang.
- Struktur hanya boleh muncul di `kala_resource`/`kala_resource_test`; spawn Labuhan Awal dan klaim GriefPrevention ditolak oleh engine.
- Reset `kala_resource` sekarang memerlukan konfirmasi eksplisit: `/ka resource reset confirm` atau `/tambang reset confirm`.
- Reset memiliki lock proses, memblokir teleport saat reset, memvalidasi nama/path world, hanya menghapus `region`, `entities`, dan `poi`, serta mencatat hasil ke `plugins/KalaCore/resource-reset-audit.log`.
- Smoke test boot pada 24 Oktober 2026 berhasil: Paper 26.3-218 memuat KalaCore, `kala_resource`, Geyser UDP 19132, dan Java TCP 25565 tanpa error KalaCore.
- TerraformGenerator hanya diarahkan ke world isolasi `kala_terraform_test`. World production `kala_main` dan `kala_resource` tetap menggunakan vanilla generator.
- Struktur internal Nusantara aktif di world resource/staging (`kala_resource`, `kala_resource_test`, `kala_terraform_test`) dengan jarak minimum 256 blok, perlindungan spawn/claim, dan material vanilla yang dapat dirender Java serta Bedrock.
- BetterStructures dikunci agar TIDAK aktif di `kala_main` (`kala_main: false` pada `ValidWorlds.yml` dan `config.yml`). Struktur prosedural BetterStructures hanya diizinkan di resource/staging bila pack resmi dipasang kelak.

## Yang sengaja belum dipromosikan

- TerraformGenerator 28.0.0 dan BetterStructures 2.7.7 dipasang dari Modrinth resmi dan lolos boot test di Paper 26.3. Terraform hanya diarahkan ke `kala_terraform_test`; tidak ada generator eksternal aktif pada production world. BetterStructures belum memiliki content pack dan belum menghasilkan struktur otomatis.
- `kala_main` belum diubah. Penempatan Gunung Murka, Labuhan Awal, jalur laut, serta koordinat landmark membutuhkan map/asset dan persetujuan promosi tersendiri.
- Tidak ada pre-generation production radius 3.000, reset production, atau maintenance window yang dijalankan.

## Acceptance status v3

| Area | Status | Bukti / pekerjaan tersisa |
|---|---|---|
| Staging world | Selesai | `kala_resource_test` dan log Chunky tersedia. |
| Terrain generator eksternal | Belum dipilih | Tetap gunakan vanilla + struktur in-house sampai uji Terraform terisolasi dilakukan. |
| Struktur eksplorasi | Staging selesai | Loot balancing dan tes Bedrock masih perlu dilakukan. |
| Reset aman | Kode diperkuat | Masih perlu drill reset di maintenance window dan verifikasi manual pasca-reset. |
| Kurasi `kala_main` | Belum dimulai | Tidak aman diotomatisasi tanpa desain/koordinat yang disetujui. |
| Pre-generation produksi | Belum dimulai | Jalankan hanya setelah border/konten final dan kapasitas storage diuji. |
| Java + Bedrock QA | Belum diverifikasi | Wajib dilakukan pada staging sebelum promosi. |
| Audit/rollback blok | Bermasalah | CoreProtect 24.1 menolak Minecraft 26.3 dan menonaktifkan diri saat boot; jangan menganggap rollback tersedia sebelum versi yang kompatibel diuji. |
| BetterStructures official packs | Menunggu aset resmi | Gunakan `/nightbreaklogin <token>` lalu `/bs setup`, atau letakkan ZIP resmi di `plugins/BetterStructures/imports` dan jalankan `/bs reload`. Pilih satu pack gratis terlebih dahulu. |

## Runbook uji reset staging

1. Backup server dan pastikan tidak ada pemain pada staging.
2. Dari console/admin: `/ka staging info` lalu `/ka resource reset confirm` **hanya untuk** `kala_resource`.
3. Pastikan pemain dievakuasi, teleport masuk ditolak selama reset, dan `resource-reset-audit.log` berisi `SUCCESS`.
4. Pastikan `kala_main` tetap ada dan spawn/claim tidak berubah.
5. Uji join Java serta Bedrock, lalu catat hasil dan MSPT pada dokumen ini.

> Jangan menjalankan reset atau pre-generation pada `kala_main` menggunakan dokumen ini sebagai instruksi otomatis.
