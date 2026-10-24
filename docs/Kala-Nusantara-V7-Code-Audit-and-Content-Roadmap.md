# Kala Nusantara — V7 Code Audit & Content Roadmap

**Tanggal audit:** 24 Oktober 2026  
**Basis:** Report V6 yang diberikan, source code repo publik `xDzaky/kala-nusantara`, konfigurasi yang ter-track, dokumentasi resmi Paper/Geyser, dan beberapa plugin/resource-pack yang relevan.  
**Tujuan:** Menentukan prioritas sebelum menambah fitur, memperbaiki bug yang berisiko, lalu membuat server terasa lebih hidup tanpa membebani Java maupun Bedrock/MCPE.

> **Batas audit:** Ini adalah review source/config yang bisa dibaca dari repo, bukan akses langsung ke proses server live. Report V6 mencatat hasil staging/produksi, tetapi saya tidak menjalankan server atau build pada mesin pengguna. Label “verified/live operational” dalam report tidak dianggap bukti bahwa semua skenario edge-case sudah diuji. Tidak realistis mengaudit *semua* server Minecraft di internet satu per satu; rekomendasi plugin dan desain di bawah ini menggunakan dokumentasi resmi dan beberapa proyek yang relevan sebagai referensi.

---

## 1. Ringkasan eksekutif

Kala Nusantara tidak kekurangan ide. Pondasi yang sudah ada—KalaCore, event volcano, seasonal events, scoreboard, GUI vanilla, ledger kontribusi, Geyser/Floodgate—sudah cukup untuk membuat pengalaman co-op yang khas. Prioritas sekarang bukan menambah banyak plugin atau partikel, tetapi **menjadikan mekanik yang sudah ada aman, konsisten, terukur, dan benar-benar selesai dari awal sampai akhir**.

### Urutan prioritas

| Prioritas | Temuan | Dampak | Tindakan |
|---|---|---|---|
| **P0 — segera** | Secret management server ter-track dalam repo publik; `online-mode=false` bertentangan dengan dokumen; whitelist off | Risiko keamanan/autentikasi | Rotasi secret, sanitasi repo/history, verifikasi auth dan mode akses sebelum server publik |
| **P0 — segera** | Task impact rockfall tidak benar-benar berhenti; bisa memberi damage berulang | Pemain dapat kehilangan HP berkali-kali dari satu bongkah | Nonaktifkan rockfall/encounter sampai diperbaiki dan dites |
| **P1** | Encounter didesain 10 menit tetapi fase erupsi hanya 5 menit | Dua subfase terakhir terpotong | Satukan timeline dan phase duration |
| **P1** | Target objective seasonal melebihi jumlah objective yang muncul | Event tertentu mustahil sukses | Target harus mengikuti jumlah peti/hotspot aktual atau spawn objective tambahan |
| **P1** | Hazard muncul di sekitar spawn, bukan di Gunung Murka | Area aman dan onboarding berubah menjadi arena berbahaya | Tambah konfigurasi arena/koordinat dan proteksi safe zone |
| **P1** | Cleanup tidak memfilter `eventId` | Menghentikan satu event dapat membersihkan entitas event lain | Cleanup berdasarkan tag ID event dan ownership |
| **P1** | Ledger klaim dan SQLite belum benar-benar idempotent/durable | Reward bisa hilang, tidak bisa diklaim setelah restart, atau berisiko terbayar tidak konsisten | DB transaction, claim table, cache rebuild, payout idempotent |
| **P1** | `StorageManager` membagi satu koneksi SQLite ke jalur sync dan async | Risiko konkurensi, `SQLITE_BUSY` dan state tidak konsisten | Satu antrean writer, koneksi yang diserialisasi, atau koneksi terpisah dan transaksi |
| **P1** | Backup bukan full disaster recovery dan menyalin DB live secara langsung | Restore bisa tidak lengkap/tidak konsisten | SQLite online backup/checkpoint, backup world/config off-host, restore test |
| **P2** | BossBar dan notifikasi tidak dikoordinasikan secara nyata | Dua event/teks dapat bertumpuk | Satu event coordinator dan antrean notifikasi sesungguhnya |
| **P2** | Scoreboard dibuat ulang setiap 2 detik dan punya 10 baris, bukan 8 | Paket update/flicker dan mismatch laporan | Reuse objective/teams, update hanya jika berubah, koreksi desain |
| **P2** | Custom resource pack Bedrock belum dikonfigurasi | Identitas visual belum sampai ke MCPE | Pack kecil terpisah Java/Bedrock, lakukan uji penerimaan di HP |
| **P2** | Tidak ada test source/CI pada repo | Klaim “verified” sulit diulang | Test otomatis untuk event, ledger, cleanup, config dan build |

**Rekomendasi besar:** jangan menambah plugin gameplay terlebih dahulu. Bereskan P0 dan P1 di atas, lalu lakukan uji tertutup Java + Bedrock. Setelah stabil, tambahkan satu per satu lapisan visual dan konten.

---

## 2. Audit source code: temuan yang perlu diperbaiki

Semua link di bagian ini menunjuk langsung ke source code yang saat audit dapat dibaca di `main`.

### 2.1 P0 — Rockfall berpotensi memberi damage berulang

Source: [`HazardDirector.java`, `scheduleImpactCheck`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/HazardDirector.java#L128-L166)

Di `scheduleImpactCheck`, task berulang dibuat dengan `runTaskTimer(..., 5L, 5L)`. Begitu batu sudah menyentuh tanah, tidak valid, atau melewati batas ticks, kode menjalankan efek tumbukan, memberi damage, menghapus batu, lalu memanggil method `cancel()` lokal yang kosong. Itu **tidak membatalkan `BukkitTask` yang dibuat scheduler**. Task dapat masuk kembali ke kondisi yang sama tiap lima tick dan memproses damage lagi.

**Tindakan wajib:**
1. Jangan jalankan `/ka rockfall` maupun `/ka encounter start` di server berisi pemain sampai fix sudah dipasang.
2. Simpan handle `BukkitTask` dan batalkan secara nyata ketika impact diproses, atau gunakan `BukkitRunnable` lalu panggil `cancel()` miliknya.
3. Tambahkan guard one-shot (`resolved`/state) sehingga setiap batu hanya menghasilkan satu impact.
4. Hapus task dari registry saat selesai; jangan menyimpan task yang sudah selesai sepanjang umur server.
5. Uji: batu jatuh, batu dihapus paksa, timeout 80 ticks, pemain tepat di target, pemain di tepi radius, pemain respawn/teleport dan event dihentikan.

### 2.2 P1 — Heat vent tidak mengakhiri task sendiri

Source: [`HazardDirector.java`, `spawnHeatVent`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/HazardDirector.java#L172-L203)

Ketika `elapsed > durationSeconds`, runnable hanya `return`; ia tidak cancel. Jadi task tetap terjadwal selamanya sampai ada pemanggilan `cancelAllHazards()`. Durasi juga dihitung dengan jumlah eksekusi scheduler, bukan timestamp deadline yang persisten.

**Perbaikan:** ketika durasi habis, cancel task dan unregister dari task registry. Tambahkan cooldown damage per pemain agar tidak terkena efek panas setiap detik tanpa jeda. Setiap heat vent harus punya `eventId`, lokasi di arena yang valid, dan aturan cleanup.

### 2.3 P1 — Gunung Murka menyerang dekat spawn, bukan di gunung

Sources: [`VolcanoEncounterDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/VolcanoEncounterDirector.java#L100-L183), [`HazardDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/HazardDirector.java#L41-L105)

`VolcanoEncounterDirector` memakai `world.getSpawnLocation()` sebagai titik pusat; rockfall dilepas dari spawn, tiga heat vent dibuat di radius sekitar spawn, dan magma cube muncul di sekitar spawn. Jadi di source saat ini, encounter lebih tepat disebut **event di spawn** daripada event di Gunung Murka. Ini berpotensi mengganggu Labuhan Awal dan onboarding.

**Perbaikan desain:**
- Buat konfigurasi lokasi `volcano.world`, `volcano.center.x/y/z`, `volcano.arena-radius`, serta safe-zone polygons/regions.
- Semua lokasi target harus divalidasi: world yang tepat, block top solid, tidak berada di air/atap rumah, tidak di area claim, bukan di onboarding, dan tidak terlalu dekat pemain yang tidak mengikuti event.
- Pisahkan `spectator/safe zone`, `hazard zone`, dan `objective zone`.
- Jika Gunung Murka ternyata berada jauh dari spawn, jangan menggunakan `world.getSpawnLocation()` sebagai fallback diam-diam. Jika konfigurasi tidak ada, event ditolak dengan log yang jelas.

### 2.4 P1 — Durasi 5 menit vs encounter 10 menit

Sources: [`KalaEventManager.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/kala/KalaEventManager.java#L72-L75) dan [`VolcanoEncounterDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/VolcanoEncounterDirector.java#L70-L97).

KalaEventManager mengatur `PUNCAK_DURATION_SEC = 300` (5 menit), sementara VolcanoEncounterDirector memiliki timeline yang memerlukan 600 detik:
- 0–45: Pembuka Kawah
- 45–120: Gelombang Abu
- 120–240: Rockfall
- 240–360: Retakan Panas
- 360–480: Encounter Bara
- 480–600: Objective Final

Fase PUNCAK berakhir sebelum subfase Retakan Panas selesai; pada transisi pemulihan, encounter dihentikan. **Encounter Bara dan Objective Final tidak dicapai lewat jadwal normal.** Log pada report menunjukkan transisi dua fase hanya berjarak sekitar sembilan detik, jadi log itu tidak membuktikan jadwal waktu normal; mungkin tes manual `/ka encounter next`.

**Keputusan:** pilih salah satu timeline resmi:
- **Opsi A, direkomendasikan:** PUNCAK = 10 menit dan subfase memakai timing di atas.
- Opsi B: seluruh subfase dipadatkan secara eksplisit jadi 5 menit, dengan uji ulang keterbacaan telegraph untuk HP.

Jangan biarkan dua konstanta durasi berbeda tanpa satu sumber konfigurasi.

### 2.5 P1 — Seasonal objective bisa mustahil diselesaikan

Source: [`SeasonalEventDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/SeasonalEventDirector.java#L93-L210)

`objectiveTarget = max(10, onlinePlayers * 4)`. Namun:
- Badai Selat hanya spawn **8 peti**, jadi target minimum 10 tidak mungkin tercapai.
- Abu dan Bara hanya spawn **10 hotspot**. Jika ada 3 pemain atau lebih, target minimal 12 dan objective maksimal 10, jadi event tidak dapat sukses.

**Perbaikan:** target harus berdasarkan jumlah objective yang benar-benar berhasil ditempatkan. Contoh: spawn 8 peti → target 8 untuk wave pertama; jika server memiliki banyak pemain, gunakan beberapa wave peti dengan jumlah dan waktu yang dihitung, bukan target palsu. Catat `spawnedObjectives`, `completedObjectives`, `failedObjectives`; bila lokasi gagal spawn, kurangi target atau cari lokasi pengganti.

Saat event habis, gunakan resolusi sukses/gagal yang berdampak pada reward yang sudah ditentukan. Jangan menandai sukses hanya dari hitungan yang tidak mungkin tercapai.

### 2.6 P1 — Seasonal event belum benar-benar terhubung ke kalender musim

`SeasonalEventDirector` mengimpor `SeasonManager`, tetapi implementasinya menerima `plugin`, `policy`, dan `ledger`; event dimulai lewat perintah admin dan source yang dibaca tidak menunjukkan scheduler otomatis berbasis musim delapan minggu. Maka klaim report bahwa event sudah terhubung ke kalender perlu diperlakukan sebagai desain yang belum dibuktikan oleh kode ini.

Buat satu `EventScheduler`/`ActiveEventCoordinator` yang menentukan:
- event yang boleh muncul pada tiap fase musim;
- jendela cooldown dan waktu mulai yang wajar;
- minimum pemain online, maksimal event aktif, dan larangan overlap dengan erupsi utama;
- apakah event berjalan saat pemain tidur/AFK atau tidak ada pemain di zona;
- cara melanjutkan/abort setelah restart.

Jangan jadwalkan bencana besar hanya karena server menyala. Beri pemberitahuan awal, periode pendaftaran/siaga, dan pilih waktu saat pemain memang dapat ikut.

### 2.7 P1 — Hujan/cuaca tidak dikembalikan ke kondisi semula

Source: [`SeasonalEventDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/SeasonalEventDirector.java#L121-L150)

Ketika Badai Selat berhenti, kode selalu `setStorm(false)` dan `setThundering(false)`. Jika cuaca sebelum event sudah hujan/badai, keadaan itu hilang. Ini juga mudah bentrok bila lebih dari satu sistem mengontrol cuaca.

Simpan snapshot cuaca awal (storm, thundering, weather duration/clear duration yang relevan) dan pulihkan persis saat normal end, admin stop, exception, dan shutdown. Lebih baik ada `WeatherLease` tunggal agar dua event tidak saling mematikan cuaca.

### 2.8 P1 — Cleanup menghapus entitas event lain

Source: [`WorldImpactPolicy.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/WorldImpactPolicy.java#L97-L133)

`cleanupAllEventEntities(String eventId)` menerima `eventId` namun tidak memfilter berdasarkan ID tersebut; semua `trackedEntities` dihapus. Jika Volcano berhenti saat item Badai Selat masih ada, cleanup gunung dapat menghapus peti dari event musiman. Scan darurat juga hanya memeriksa `kala_main` dan `kala_resource`, bukan semua world.

**Perbaikan:**
- Filter `NamespacedKey` event entity **dan** `event_id` yang sesuai.
- Gunakan `cleanupEvent(eventId)` untuk event tertentu dan `cleanupAll()` hanya untuk shutdown/emergency.
- Pastikan setiap entitas sementara (batu, magma cube, peti, marker) terdaftar, memiliki owner ID, dan terhapus saat normal end, abort, reload, player teleport, dan chunk/world unload yang relevan.
- Terapkan batas entitas terpisah per kategori dan total. Angka report berbeda dari source saat ini; tetapkan satu nilai di config dan tampilkan hasil runtime lewat `/ka status`.

### 2.9 P1 — Reward ledger belum benar-benar idempotent lintas restart

Source: [`EventContributionLedger.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java#L44-L144)

Masalah yang tampak:
- poin disimpan di `pointsCache` memory, tetapi source yang diperiksa tidak memuat ulang total poin dari SQLite ketika plugin mulai; kontribusi lama bisa tidak terlihat oleh `getPlayerPoints` setelah restart;
- `claimedCache` juga memori; `claimReward()` tidak memeriksa `reward_claimed` di DB sebelum memberi hadiah;
- mata uang/XP diberikan sebelum update `reward_claimed` yang dilakukan asynchronously; pemberian hadiah dan pencatatan klaim bukan satu operasi durable;
- unique key `(event_id, uuid, objective_id)` mencegah duplikasi row, tetapi upsert tetap menambah poin, sehingga bukan idempotency terhadap event interaksi yang terkirim dua kali;
- diminishing returns hanya memotong poin jadi separuh setelah 80 poin; bukan hard cap, dan tidak ada cooldown per aksi di ledger.

**Desain disarankan:**
1. Gunakan tabel `event_claims` dengan `UNIQUE(event_id, uuid)` dan transaksi untuk claim.
2. Simpan/rekonstruksi skor dari DB; cache hanya optimasi, bukan sumber kebenaran.
3. Beri setiap objective interaction idempotency key/action ID yang unik. Untuk objektif satu-kali, DB menolak ID yang sama.
4. Catat reward sebagai `PENDING` di ledger transaksi; delivery gunakan idempotent payout/transaction ID; setelah sukses tandai `PAID`. Tentukan strategi recovery untuk crash di tengah proses.
5. Hit combat diberi cooldown/kontribusi berbasis damage valid per target, bukan 5 poin untuk setiap paket hit. Tetapkan hard cap per objektif atau kurva diminishing returns yang berhenti bertambah setelah batas.

### 2.10 P1 — Satu koneksi SQLite digunakan dari jalur sync dan async

Source: [`StorageManager.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/storage/StorageManager.java#L24-L46), [`StorageManager.java` connection access](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/storage/StorageManager.java#L199-L225), dan [`EventContributionLedger.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java#L58-L80).

StorageManager memiliki satu field `Connection` yang diinisialisasi sekali dan dikembalikan lagi oleh `ensureConnection()`. Event ledger menjalankan SQL di async thread; beberapa sistem ekonomi/profile juga menggunakan koneksi ini dari jalur biasa. Mengakses satu JDBC connection secara bersamaan dari banyak thread berisiko tabrakan transaksi/statement dan `SQLITE_BUSY` atau data tidak konsisten.

Paper juga mengingatkan banyak API Bukkit tidak aman dipanggil dari async thread. Karena itu, capture `player.getName()` dan data player lain di main thread sebelum menulis async.

**Pilih satu pola yang jelas:** writer tunggal berurutan (bounded queue) untuk seluruh write SQL, atau connection-per-operation/thread dengan transaksi dan lifecycle yang jelas. Hindari satu connection bersama yang digunakan serampangan. Jangan mengakses world/entity/player dari jalur async.

### 2.11 P1 — Backup bukan full disaster recovery

Source: [`BackupManager.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/backup/BackupManager.java#L39-L97)

`BackupManager` hanya memasukkan `kala_data.db`, `npcs.yml`, dan `config.yml` ke ZIP. Tidak ada world region, server configuration, plugin manifest, resource pack, atau seluruh plugin config. Itu backup konfigurasi/database terbatas, bukan backup penuh agar server bisa direstore dari disk rusak.

Selain itu SQLite diaktifkan dengan WAL; menyalin file utama database secara langsung saat server aktif tanpa prosedur SQLite online backup/checkpoint dapat menghasilkan snapshot yang tidak lengkap terhadap transaksi yang masih berada di WAL.

**Perbaikan minimum:**
- gunakan SQLite online backup API atau mekanisme checkpoint/snapshot yang konsisten;
- tambahkan world `kala_main` (tidak boleh hilang), backup sebelum reset `kala_resource`, seluruh config penting dan pack;
- backup keluar dari komputer/drive produksi (off-site), bukan hanya ke folder di disk yang sama;
- retention (misal harian 7, mingguan 4, bulanan 3), checksum dan ukuran backup;
- lakukan restore test ke staging secara berkala dan dokumentasikan waktu pemulihan;
- kirim pesan ke `CommandSender` kembali di main thread setelah tugas async, dan jangan membuat klaim backup sukses sebelum file ZIP ditutup dan diverifikasi.

### 2.12 P1 — Deposit offline dan transfer belum atomik

Source: [`RupiahEconomy.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/economy/RupiahEconomy.java#L49-L85).

`deposit(UUID, ...)` hanya mengubah saldo jika `IdentityManager.get(uuid)` menghasilkan profile yang sudah tersedia. Bila penerima offline/tidak ada di cache, saldo tidak terlihat berubah tetapi transaksi log bisa tetap tercatat. Transfer juga menjalankan withdraw dan deposit secara terpisah, serta tidak menggunakan transaksi database atomik yang mengunci keduanya.

Selesaikan ini sebelum menambah reward event yang besar:
- saldo tersimpan durable untuk pemain online maupun offline;
- transfer debit/kredit dilakukan dalam satu transaksi;
- cegah transfer ke diri sendiri, jumlah non-finite/overflow, target tidak valid, dan double-click;
- gunakan transaction ID idempotent untuk payout dari event;
- buat tes transaksi gagal, saldo tidak cukup, target offline, restart, dan race dua transfer.

### 2.13 P2 — Notifikasi belum menjadi antrean prioritas sesungguhnya

Source: [`KalaNotificationService.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/notify/KalaNotificationService.java#L46-L121).

Walau ada enum `Priority`, `sendTitle()` tidak menggunakan tingkatan prioritas untuk queue/preemption; title langsung ditampilkan. `broadcastActionBar()` langsung mengirim ke semua pemain tanpa memakai deduplikasi yang ada pada `sendActionBar()`. Preferensi tips juga hanya berada di memory dan tidak persisten.

Buat queue yang nyata per pemain: SYSTEM > STORY_EVENT > QUEST > TIPS_COSMETIC, TTL untuk notifikasi tertunda, cooldown per channel, dan kebijakan untuk tidak menumpuk title. Jangan menahan peringatan bencana terlalu lama: alarm bahaya yang sudah kedaluwarsa sebaiknya dibuang, bukan tampil setelah bahaya selesai. Simpan pengaturan notifikasi pemain di profile/database.

### 2.14 P2 — Scoreboard lebih sering dibuat ulang dan lebih banyak baris dari klaim report

Source: [`KalaScoreboardManager.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/ui/KalaScoreboardManager.java#L44-L145).

`updateAll()` berjalan setiap 40 ticks dan `update()` meng-unregister lalu register ulang objective setiap kali. Baris yang ditulis berjumlah 10, termasuk dua baris spacer, padahal report menyebut maksimal 8. `Component.text(text)` juga menerima string legacy `§` yang bukan komponen Adventure berwarna secara eksplisit.

**Rekomendasi:** pertahankan scoreboard custom (tidak perlu TAB sekaligus hanya untuk hal yang sama), buat objective sekali per pemain, update nilai/prefix hanya ketika berubah, dan serialisasi warna menjadi Adventure component. Rancang 6–8 baris informasi yang benar-benar berguna: musim, lokasi, misi aktif, proyek desa, saldo, status event, online, shortcut menu. Jangan ulangi detail yang sama di scoreboard, bossbar, actionbar, dan chat.

### 2.15 P2 — BossBar dan event bisa berjalan tumpang tindih

`KalaEventManager` punya BossBar sendiri, dan `SeasonalEventDirector` punya BossBar sendiri. `startEvent()` seasonal tidak menolak ketika erupsi utama sedang berlangsung. Jadi bisa ada dua event besar dengan title, cuaca, dan progress masing-masing. Ini bertentangan dengan gagasan “single UI owner”.

Tambahkan `ActiveEventCoordinator` yang memberi kepemilikan tunggal terhadap BossBar dan mengatur eksklusivitas event:
- satu event utama dan paling banyak satu mikro-event pendamping jika sudah didesain kompatibel;
- rekomendasi MVP: **jangan jalankan Badai Selat/Abu dan Bara saat fase Gunung Murka bukan `IDLE`**;
- antrean notifikasi dikoordinasikan dengan event aktif;
- cleanup tidak boleh membatalkan entitas event lain.

### 2.16 Ketidaksesuaian report vs source

- Report V6 menyebut budget maksimal 16 batu, 8 mob, total 24; source membatasi `MAX_ROCKFALL_PER_SALVO=6`, `MAX_EVENT_MOBS=6`, `MAX_ACTIVE_HAZARDS=12`, total di `canSpawnMoreEntities()` menjadi 18. Seasonal item/hotspot tidak jelas dihitung di budget entitas yang sama.
- Report menggambarkan event seasonal terhubung ke kalender, sedangkan source yang dibaca masih memulai event melalui admin command dan tidak menggunakan `SeasonManager` di constructor/event scheduling.
- Report menyebut `Schema database OK (5 tabel)`, tetapi `StorageManager.createTables()` dalam source membuat 11 tabel aplikasi.
- README mengklaim Paper 1.21.4/26.3 berdampingan; `build.gradle` compile terhadap Paper API 1.21.4 dan `plugin.yml` `api-version: '1.21'`, sementara lingkungan ditulis Paper 26.3. Itu belum otomatis berarti plugin gagal, tetapi compatibility claim harus dibuktikan dengan API/build dan test sesuai runtime.

Buat report berikutnya dihasilkan dari versi commit yang sama dengan binary yang dipasang; cantumkan commit SHA, build artifact SHA256, versi plugin tepat, waktu test, test commands, dan hasil. Hindari membuat status “verified production” hanya dari `BUILD SUCCESSFUL` atau log boot.

### 2.17 P1 — Jadwal erupsi tidak persisten dan alur klaim reward bertentangan

Source: [`KalaEventManager.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/kala/KalaEventManager.java#L56-L75) dan [`VolcanoEncounterDirector.java`](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/VolcanoEncounterDirector.java#L190-L209).

`lastEruptionTime` diinisialisasi ke waktu saat kelas dibuat dan interval tetap 24 jam, bukan interval acak terkontrol 24–48 jam yang disebut rancangan. Restart/reload mereset hitungan. Jadwal event perlu disimpan sebagai `nextEventAt`/deadline di database/config agar restart tidak mengatur ulang jam secara diam-diam.

Ada juga ketidakkonsistenan reward: `VolcanoEncounterDirector.stopEncounter()` mencoba memanggil `claimReward()` otomatis untuk semua pemain online di `kala_main`, sedangkan event mengumumkan `/kala claim-event <id_event>` untuk klaim manual. `stopEncounter()` juga dipanggil saat fase selesai, penghentian admin, dan `onDisable`, sehingga alur penyelesaian normal dapat tercampur dengan pembatalan atau shutdown. Pisahkan `completeEncounter()` (menutup objective lalu membuka klaim) dari `abortEncounter(reason)` (cleanup, tanpa auto-payout kecuali kebijakan menyatakan hadiah partisipasi tetap berlaku). Simpan status `COMPLETED/ABORTED/CLAIMABLE` pada DB dan tentukan satu pola klaim resmi.

---

## 3. Desain gameplay V7: bencana yang menantang tanpa menghancurkan dunia

### Prinsip inti: “Hazard is temporary; world state is protected”

Pertahankan `WorldImpactPolicy Level 0` untuk `kala_main`: tidak ada mutasi blok dari event, tidak ada api menyebar, tidak ada lava yang mengalir, tidak ada ledakan destruktif, dan tidak menghapus block/container pemain. Konsekuensi bisa terasa melalui damage yang tertelegrafis, penutupan jalur sementara, objective, hadiah, cooldown, ekonomi, dan perubahan akses/misi.

### 3.1 Urutan event Gunung Murka yang direkomendasikan

1. **Pertanda (30–60 detik):** suara gemuruh dari arah gunung, asap jauh, satwa turun dari lereng, peta/jurnal menampilkan tingkat bahaya. Tidak ada damage.
2. **Peringatan publik (60–120 detik):** NPC/GUI memberi tiga peran: *Logistik*, *Pengamatan*, *Penjagaan*. Pemain menyiapkan bahan, memilih target yang ingin dibantu, atau menuju safe zone.
3. **Gelombang abu:** atmosfer per-player dan objective sampel abu/logistik. Efek kecil di layar dan sound lokal; tidak memberi blindness berat pada mobile.
4. **Rockfall salvo:** batu diluncurkan hanya pada area arena gunung yang disahkan. Lingkaran target jelas, berubah intensitas/warna atau bunyi mendekati waktu benturan. Waktu peringatan minimal 2.5 detik; tambahkan pilihan “Low effects” yang mengurangi dekorasi tetapi tidak menghilangkan telegraph.
5. **Heat vents:** 2–3 sumber uap di jalur yang aman, muncul dengan tanda visual sebelum damage; terkena memberikan slow singkat atau damage kecil dengan cooldown, tidak membuat api/blocks.
6. **Encounter Bara (tidak setiap erupsi):** gelombang kecil Magma Cube bernama. Maksimum mob dipatuhi; mob diberi tag event, tidak membakar blok dan tidak memberi loot vanilla di luar loot table yang diatur. Bisa dilewati melalui objective logistik/observasi dengan reward sedikit berbeda.
7. **Resolusi Altar:** persembahan kolektif adalah salah satu pilihan, tetapi bukan satu-satunya cara menang. Misalnya pemain dapat memilih mengamankan logistik, menyumbang material, menuntaskan survei, atau mengalahkan minion. Gunung mereda pada akhir timer; kontribusi komunitas memengaruhi hasil, bonus pemulihan dan kontrak.
8. **Pemulihan 3–5 menit:** kontrak pedagang membeli komoditas, riset membutuhkan sampel abu, petani mendapat bonus terbatas, dan NPC memberikan jurnal “apa yang berhasil”. Setelah event, tutup ledger, claim reward, lalu cleanup.

### 3.2 Bikin rockfall tampak meyakinkan

- **Telegraph:** lingkaran berdiameter konsisten tepat di atas top block, suara pendek yang tidak menyakitkan, actionbar singkat untuk pemain dekat.
- **Flight:** FallingBlock vanilla plus partikel abu/asap hemat di sekitar lintasan. Jangan pakai banyak `BlockDisplay` bergerak tanpa tes Geyser/Bedrock; packet/entity rendering berbeda di klien.
- **Impact:** satu burst `BLOCK`/debu, satu sound impact, satu damage resolution, satu knockback. Resolusi dilakukan sekali oleh server, terpisah dari render entity.
- **Gameplay:** dampak hanya pada pemain yang gagal menghindar di arena event; gunakan cooldown damage per pemain untuk menghindari satu pemain dihantam beberapa hitbox di salvo yang sama.
- **Safety:** pilih target dari daftar koordinat/open ground yang telah disiapkan; hindari rumah, claim, peti, redstone, NPC, portal, area onboarding, dan pemain yang sedang tidak ikut event.
- **Cleanup:** saat event stop, restart atau exception, task dihentikan, entitas event dihapus berdasarkan ID, objective ditutup, BossBar disembunyikan, dan cuaca dikembalikan.

**Penting:** visual batu tidak harus berupa blok yang benar-benar mengganti terrain. Untuk rasa dramatis, lintasan FallingBlock + debu + suara + warning yang benar sudah lebih aman dan mudah diterjemahkan Geyser daripada simulasi lava/terrain yang kompleks.

### 3.3 Badai Selat dan Abu & Bara jangan terasa seperti “ambil barang lalu selesai”

**Badai Selat:**
- Peti ditempatkan pada titik pesisir yang sudah diset, bukan kotak random di sekitar spawn.
- Buat 2–3 peran: penyelamat kargo, operator sinyal mercusuar, dan pengumpul logistik.
- Saat pemain mengambil peti, satu objective dicatat satu kali dan item visualnya tidak masuk ke inventory sebagai chest biasa kecuali memang direncanakan sebagai loot.
- Sukses bersama membuka kontrak rempah/langkah pemulihan; kegagalan tidak merusak rumah, tetapi kupon/bonus komunitas berkurang.

**Abu & Bara:**
- Hotspot ditampilkan dengan partikel kecil dan suara lokal, disertai interaction marker yang jelas.
- Jangan menerima klik pada sembarang blok dekat titik. Gunakan interaksi marker/item/target block yang didefinisikan, atau cek bahwa klik tepat pada lokasi/aksi yang dimaksud.
- Sampel abu sebaiknya item custom yang diberi PDC/item ID, bukan sekadar `GUNPOWDER` bernama. Validasi saat NPC menerima item dan hindari item bernama palsu.
- Objective total mengikuti jumlah hotspot yang benar-benar tersedia.

### 3.4 Variasi lanjutan setelah dua MVP stabil

Tambahkan satu event tiap rilis, bukan empat sekaligus:
- **Rob Labuhan:** amankan gudang, angkut logistik ke elevasi aman, bersihkan jalur; jangan mengalirkan air sungguhan ke rumah.
- **Longsor Lereng:** jalur ekspedisi ditutup sementara melalui hazard marker; cari peti ekspedisi/survei jalur alternatif.
- **Musim Kemarau:** kontrak distribusi air/benih dan tanaman komunitas; hindari mengubah seluruh tanah/crop global secara mendadak.

Tiap event mempunyai: teaser, peringatan, 2–3 objective berbeda, reward personal, reward komunal, timeout yang tetap menghasilkan resolusi, dan summary di jurnal. Tidak semua event perlu boss atau monster.

---

## 4. Reward dan progres yang tahan eksploit

### 4.1 Jangan memberi poin hanya karena spam hit

Untuk tiap event, definisikan objective dengan poin maksimal yang jelas:
- membantu logistik: maksimal satu kali per paket/ID peti;
- mengamati gunung: dapat skor dari milestone, bukan dari berdiri di dekat gunung;
- combat: poin berdasarkan damage kontribusi yang sah per target, dengan cooldown; kill credit tidak boleh diberikan jika target tidak berada dalam event yang aktif;
- altar: poin per transaksi bahan yang berhasil, dengan validasi inventory dan event ID;
- penyelamatan: poin berdasarkan objective server-side yang selesai.

Setiap action memiliki ID immutable yang disimpan untuk menolak pengiriman ganda. Simpan total poin pada DB, bukan hanya cache.

### 4.2 Model reward yang direkomendasikan

`Reward = Partisipasi dasar + Bonus objective personal + Bonus keberhasilan komunitas`, dengan cap per event. Hindari reward tanpa batas dari jumlah pukulan atau donasi item berulang.

| Tipe | Hadiah | Tujuan |
|---|---|---|
| Partisipasi minimum | EXP kecil + Rupiah Kala | Menghargai pemain yang benar-benar melakukan satu objective valid |
| Objective personal | title/jurnal/progres profesi | Membuat peran berbeda sama-sama menarik |
| Keberhasilan komunitas | kontrak pasar, bonus pertanian singkat, proyek desa | Mendorong gotong royong tanpa P2W |
| Eksplorasi/riset | sampel atau recipe/proyek terbuka | Membuat event berlanjut setelah bencana |
| Prestasi langka | kosmetik/gelar, bukan uang sangat besar | Menghindari inflasi ekonomi |

### 4.3 Hasil event harus tetap menarik saat target gagal

Jangan memakai binary sukses/gagal yang hanya menampilkan chat. Misalnya:
- 0–39%: event berakhir, hadiah partisipasi dasar;
- 40–79%: pemulihan dasar, satu kontrak riset;
- 80–99%: bonus komunal kecil;
- 100%: bonus komunitas penuh dan variant kontrak.

Persentase hanyalah contoh awal—kalibrasi dari playtest dan jumlah pemain. Hasil harus dihitung dari objective yang sah, bukan target yang melebihi jumlah item yang bisa ditemukan.

---

## 5. Partikel, suara, animasi dan performa

### 5.1 Buat 3 preset efek

| Preset | Target | Perilaku |
|---|---|---|
| **Rendah** | HP lama / koneksi lemah | Telegraph jelas, partikel impact sedikit, no ambient cloud mass, suara bahaya pendek |
| **Seimbang (default)** | Mayoritas pemain | Abu tipis, smoke burst kecil, satu sound event lokal, model vanilla |
| **Sinematik** | Java/PC atau pemain yang memilih | Debu/ash tambahan, beberapa efek atmosfer dan musik ambience opsional; tetap di bawah cap server |

Pengaturan ini sebaiknya per-pemain untuk dekorasi/audio, tetapi **tidak pernah menyembunyikan tanda bahaya utama**. Gunakan nama `visualIntensity=LOW|BALANCED|CINEMATIC` dan `eventSoundsEnabled` yang persist di profile.

### 5.2 Aturan optimasi

- Jangan spawn 20 cloud particles dengan radius 10 setiap detik untuk setiap pemain selama lima menit bila jumlah pemain bertambah. Emit partikel dekat pemain yang sedang di world/zona event dan lakukan sampling berkala.
- Batasi jumlah per event, per zona, dan per tick; budget total mencakup item crates/hotspots, bukan cuma FallingBlock/mob.
- Suara tidak perlu global setiap detik; pakai radius/sumber suara di lokasi gunung, dan alarm personal hanya jika pemain dekat.
- Timer untuk deadline gunakan timestamp target, bukan hanya jumlah callback; server bisa turun dari 20 TPS sehingga 600 ticks tidak selalu sama dengan 30 detik dinding.
- Task registry harus selalu unregister task selesai; tidak boleh ada runnable yang berhenti bekerja tetapi terus terjadwal.
- Ukur MSPT p95/p99, penggunaan heap, jumlah entity event, jumlah packet/partikel perkiraan, serta pengalaman HP dengan server diisi 10–20 pemain staging.

Paper menjelaskan bahwa satu tick idealnya 50 ms; task sync mengubah performa tick, sedangkan async harus berhati-hati karena banyak API world/player Bukkit tidak thread-safe. Lihat: [Paper Scheduler](https://docs.papermc.io/paper/dev/scheduler/).

---

## 6. GUI dan resource pack: cukup custom, tidak perlu berat

### 6.1 Pertahankan MenuManager sebagai sumber GUI utama

`MenuManager` sudah memiliki menu vanilla chest yang dapat dipakai lintas platform. Jangan menambah DeluxeMenus/zMenu/TAB/Kamenu hanya untuk menduplikasi `/kompas`, `/gunung`, `/fauna`, `/musim` dan sejenisnya. Lebih baik:
- samakan posisi Back/Close pada seluruh menu;
- teks item maksimum 1–2 baris penting dahulu, lore detail di bawah;
- hanya satu ikon untuk satu kategori, konsisten antar-menu;
- gunakan ruang kosong seperlunya, bukan border filler di semua slot jika itu mengurangi keterbacaan;
- menu 27 slot untuk aksi singkat, 45/54 slot hanya untuk koleksi/jurnal;
- hindari konfirmasi berlapis untuk aksi aman; wajib konfirmasi hanya untuk aksi yang membelanjakan item/uang atau berisiko.

### 6.2 Tema seni “Peta Laut & Perkamen Vulkanik”

Gunakan warna yang sudah dimiliki V5: biru laut gelap, teal, pasir/perkamen, tembaga vulkanik. Hindari glow/rainbow di semua item. Rekomendasi ikon vanilla untuk versi pertama:
- Kompas: `COMPASS`;
- jurnal: `WRITABLE_BOOK`;
- fauna: `FEATHER` atau item spesifik yang mudah dibedakan;
- gunung: `MAGMA_BLOCK`/`BASALT`;
- cuaca: `PRISMARINE_SHARD`/`HEART_OF_THE_SEA`;
- kontrak pasar: `PAPER`/`EMERALD`;
- proyek komunitas: `BARREL`/`CRAFTING_TABLE`.

Inspirasi tekstur GUI untuk studi visual (bukan untuk langsung menyalin aset):
- [Shrimp’s Immersive Interfaces](https://minecraft-inside.ru/resource-packs/179082-shrimps-immersive-interfaces.html)
- [Hellim’s TrueTexture GUI gallery](https://www.curseforge.com/minecraft/texture-packs/hellims-truetexture-gui/gallery)
- [Bedrock Flavor](https://modrinth.com/resourcepack/bedrock-flavor/) — referensi visual saja; periksa lisensi sebelum reuse, jangan menyalin aset tanpa izin.

Bentuk custom pack sendiri dengan frame kayu sederhana, peta, glyph kecil, dan icon item. Tidak perlu membuat seluruh UI jadi tekstur baru pada iterasi awal.

### 6.3 Java dan Bedrock wajib diberi pack berbeda

Geyser dapat mengirim Bedrock resource pack tetapi **tidak otomatis mengonversi pack Java ke format Bedrock**. Pack Bedrock bisa dipasang pada `plugins/Geyser-Spigot/packs` atau melalui URL langsung dengan header download yang sesuai. Custom item memerlukan Bedrock pack serta mapping yang cocok; integrasi model bukan otomatis. Dokumentasi:
- [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/)
- [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/)

Lakukan tahap per tahap:
1. pack Java tipis (ikon/gui/texture yang dibutuhkan);
2. pack Bedrock yang disiapkan terpisah dan diuji di perangkat sebenarnya;
3. custom item model 2D lebih dulu;
4. baru model 3D kecil jika mapping stabil;
5. jangan mewajibkan client-side mods untuk gameplay inti.

[GeyserMC Rainbow](https://github.com/GeyserMC/rainbow) adalah alat bantu pengembangan client-side untuk menghasilkan mapping dan pack Bedrock; proyeknya sendiri menyebut status eksperimental. Gunakan untuk eksperimen, bukan sebagai satu-satunya jaminan kompatibilitas produksi.

---

## 7. Plugin, mods, dan tools yang layak dipertimbangkan

**Jangan menambah plugin hanya karena server besar memilikinya.** Plugin adalah dependency, bukan fitur otomatis. Prioritaskan yang memperbaiki masalah nyata.

| Kandidat | Keputusan untuk Kala | Alasan dan batasan |
|---|---|---|
| **Paper spark (sudah ada di Paper config)** | Pertahankan | Gunakan profiler untuk membuktikan MSPT saat event. Tidak perlu pasang profiler kedua tanpa alasan. |
| **CoreProtect CE** | Tetap nonaktif sampai kompatibel | Matrix repo mencatat versi yang ada belum mendukung 26.3. Listing versi yang diperiksa hanya menampilkan Java 26.2 dan versi sebelumnya: [CoreProtect versions](https://modrinth.com/plugin/coreprotect/versions). Aktifkan hanya setelah upstream mengumumkan 26.3 dan tes staging lolos. |
| **BlueMap** | Opsional, prioritas menengah | Plugin map 3D server-side, listing saat diperiksa menyebut 26.3/Paper: [BlueMap versions](https://modrinth.com/plugin/bluemap/versions). Batasi world yang dirender; jangan expose resource world/claim pribadi, pantau disk dan CPU render. |
| **BetterStructures** | Pertahankan dan audit generasi | Listing saat diperiksa menyebut Paper 26.3 dan struktur dapat digenerate lewat pack: [BetterStructures](https://modrinth.com/plugin/betterstructures/). Pastikan tidak overlap dengan `NusantaraStructureEngine` dan tidak membuat dua sistem mengambil ownership struktur yang sama. |
| **KaMenu** | Jangan dipasang dulu ke produksi | Plugin dialog GUI terbaru yang menyebut 26.3: [KaMenu](https://modrinth.com/plugin/kamenu). Dialog native perlu dites pada Geyser/Bedrock; MenuManager vanilla saat ini lebih predictable untuk touch/crossplay. |
| **Nexo + Scaffolding** | Opsi masa depan, bukan wajib | Menyederhanakan custom content, tetapi ada dependency/biaya dan detail Bedrock tetap butuh pack/mapping. Jangan menambah hanya untuk beberapa ikon sederhana. |
| **TAB** | Jangan menumpuk dengan sistem custom | Bisa mengelola scoreboard/tablist, tetapi KalaCore sudah punya `KalaScoreboardManager` dan `TabListManager`. Pertimbangkan mengganti seluruh owner bila memang perlu fitur TAB; jangan dua plugin menulis sidebar. |
| **ItemsAdder** | Bukan pilihan awal untuk Geyser wajib | FAQ ItemsAdder menyatakan Geyser tidak didukung secara resmi: [ItemsAdder Geyser FAQ](https://wiki.itemsadder.com/faq/geyser/). Jangan anggap custom model otomatis bekerja di MCPE. |
| **Voice chat plugin/mod** | Opsional fase berikutnya | Voice proximity biasanya butuh instalasi atau langkah client tambahan; jangan jadikan syarat untuk pemain HP jika gameplay inti harus universal. |
| **Rainbow** | Tool pipeline development | Berguna untuk menyiapkan Geyser mappings/pack, tapi eksperimental: [repo Rainbow](https://github.com/GeyserMC/rainbow). Bukan plugin server gameplay. |

### Roster plugin yang disarankan sekarang

Gunakan plugin yang sudah benar-benar terpasang/diuji dari manifest aktual; dari konfigurasi yang terlihat, fondasi relevan mencakup Paper, KalaCore, Geyser/Floodgate, LuckPerms, GriefPrevention, Chunky, BetterStructures, TerraformGenerator, PlaceholderAPI, Vault dan WorldEdit. **Repo meng-ignore file `.jar`, jadi konfigurasi yang ada tidak membuktikan semua plugin tersebut aktif saat runtime.** Buat `docs/PLUGIN-MANIFEST.md` dengan kolom:
- nama dan exact version/build;
- URL resmi dan tanggal unduh;
- SHA-256 artifact;
- status: active / disabled / archived / test-only;
- fungsi/owner fitur;
- versi Java/Paper yang diuji;
- kompatibilitas Geyser/Bedrock yang telah diuji;
- lisensi;
- dependensi/plugin yang tumpang tindih.

---

## 8. Keamanan repo dan autentikasi — wajib ditangani

Source: [`server/server.properties`](https://github.com/xDzaky/kala-nusantara/blob/main/server/server.properties), [`docs/COMPATIBILITY.md`](https://github.com/xDzaky/kala-nusantara/blob/main/docs/COMPATIBILITY.md), [`server/config/paper-global.yml`](https://github.com/xDzaky/kala-nusantara/blob/main/server/config/paper-global.yml).

### 8.1 Secret management server masuk ke repo publik

`server/server.properties` yang ter-track berisi `management-server-secret` dalam teks biasa walaupun nilai `management-server-enabled` saat ini disetel false. Karena repo publik, jangan anggap nilainya aman.

1. Rotasi/ganti secret terkait.
2. Pastikan endpoint management server tetap disable kecuali memang diperlukan dan dibatasi ke localhost/jaringan tepercaya.
3. Hapus secret dari file yang dilacak dan siapkan `server.properties.example` dengan nilai kosong/placeholder.
4. Bersihkan riwayat Git bila secret pernah terpublikasi dan periksa artefak/backups yang ikut memuatnya.
5. Jangan masukkan `key.pem` Geyser/Floodgate, token, database pemain, user cache, atau file live credential ke repo.

Dokumentasi Floodgate secara eksplisit memperingatkan agar key tidak didistribusikan: [Floodgate setup](https://geysermc.org/wiki/floodgate/setup/).

### 8.2 `online-mode` bertentangan

`server/server.properties` menyebut `online-mode=false`; `docs/COMPATIBILITY.md` menyebut `online-mode=true`; `paper-global.yml` memuat proxy configuration tetapi Velocity/Bungee mode di file yang diperiksa tidak tampak diaktifkan. Jangan mengubah setting secara membabi buta: tentukan apakah Paper diakses langsung atau melalui proxy, lalu ikuti setup resmi Geyser/Floodgate untuk topologi tersebut. Jika Paper langsung terekspos ke internet, pastikan autentikasi Java resmi tetap aktif (kecuali ada arsitektur proxy terproteksi yang benar-benar memerlukannya).

### 8.3 Closed Alpha

Jika server belum terbuka untuk publik, gunakan whitelist, uji username Java dan Floodgate Bedrock, serta pastikan akun Bedrock tidak dapat meniru identitas akun Java. `white-list=false` dan `enforce-whitelist=false` dalam konfigurasi saat ini tidak cocok dengan server yang ingin dibatasi ke tester.

### 8.4 Bersihkan repositori

Tree repo saat audit berisi 671 item; 296 item berada di `server/plugins/WorldEdit/.archive-unpack/` (cache terurai) dan ada cache/locale/log runtime. Pertahankan repo sebagai source/config template, bukan mirror dari server live.

- untrack `.archive-unpack`, cache Geyser, log TerraformGenerator, player data/config runtime yang tidak perlu, user cache dan file hasil generasi;
- pertahankan template config yang sudah disanitasi;
- pindahkan credential menjadi secret lokal yang tidak di-track;
- perbarui `.gitignore` dan cek `git ls-files` setelahnya;
- jika perlu mengarsipkan konfigurasi runtime untuk diagnosis, sanitasi dan simpan di repo privat/artefak lokal yang tidak berisi data pengguna.

---

## 9. Build, test, dan release engineering

### 9.1 Selaraskan target build dengan server yang dinyatakan

Repo target runtime Paper 26.3/JDK26 pada report, sedangkan `plugin/kala-core/build.gradle` menggunakan Paper API `1.21.4-R0.1-SNAPSHOT`, source/target/release Java 21, dan `plugin.yml` `api-version: '1.21'`. Plugin lama mungkin tetap bisa berjalan di Paper lebih baru, tetapi itu bukan bukti compile/test untuk API target 26.3.

- Gunakan Paper API target yang sama dengan build server (pin exact API build bila tersedia; hindari dynamic latest di release reproducible build).
- Tentukan Java target secara sadar. Paper 26.3 saat ini terdaftar pada channel stable dan API version 26.3 pada metadata resmi Paper; Java 25+ diperlukan untuk lini MC 26.1+. Referensi: [Paper Gradle properties](https://github.com/PaperMC/Paper/blob/main/gradle.properties), [Paper setup](https://docs.papermc.io/paper/getting-started/), [plugin.yml api-version](https://docs.papermc.io/paper/dev/plugin-yml/).
- Simpan build ID exact, dependency lock, Gradle wrapper dan build SHA.
- Hindari “latest” untuk Geyser/Floodgate di catatan produksi; catat versi build exact dan tanggal smoke test.

### 9.2 Tambahkan tests sebelum fitur baru

Repo yang diperiksa tidak memiliki `src/test/` atau workflow GitHub Actions. Tambahkan test otomatis untuk:
1. event state machine hanya berpindah legal;
2. encounter mencapai semua 6 subfase pada timing yang ditetapkan;
3. setiap batu hanya satu impact, task benar-benar berhenti;
4. vent stop pada deadline;
5. objective target tidak pernah melebihi objective valid yang tersedia;
6. `cleanupEvent(A)` tidak menghapus entitas event `B`;
7. satu event claim menghasilkan paling banyak satu payout setelah double click/restart/concurrency;
8. payout offline dan transfer ekonominya atomic;
9. cuaca persis dikembalikan saat normal finish/abort/restart;
10. cap entitas dan particle budget tetap dipatuhi;
11. backup DB dapat direstore dan data hasil restore lolos integrity check;
12. config missing/invalid ditolak aman.

### 9.3 Gate staging sebelum production

- `gradle clean test shadowJar` berhasil pada target build yang dikunci;
- startup staging tanpa exception dan versi build tercatat;
- tes Java desktop dan Bedrock Android/iOS, termasuk reconnect, inventory interaction, particle, sound, item custom dan GUI;
- uji 1, 5, 10, 20 pemain simulasi jika feasible;
- record spark profile, MSPT p95/p99 dan penggunaan heap;
- tes shutdown ketika event di semua fase;
- backup, destroy staging copy, restore backup dan verifikasi data;
- baru promosikan tag release ke production.

---

## 10. Roadmap implementasi yang disarankan

### Sprint A — Stabilitas/security (jangan dilewati)
- [ ] Rotasi secret dan sanitasi file/riwayat Git publik.
- [ ] Putuskan topologi autentikasi; selaraskan `server.properties` dan `COMPATIBILITY.md`.
- [ ] Fix rockfall task one-shot dan heat vent task timeout.
- [ ] Batasi hazard ke arena Gunung Murka, bukan spawn.
- [ ] Fix timeline volcano (5 vs 10 menit).
- [ ] Fix objective target Badai Selat/Abu & Bara.
- [ ] Fix cleanup dengan event ID.
- [ ] Buat smoke/regression tests minimal.

### Sprint B — Konsistensi gameplay/data
- [ ] EventCoordinator: satu event besar aktif dan satu owner BossBar.
- [ ] Cuaca snapshot/restore.
- [ ] SQLite writer/transaction strategy.
- [ ] Durable ledger dan klaim hadiah yang benar-benar idempotent.
- [ ] Fix offline deposit dan transfer atomic.
- [ ] Buat test backup/restore; pisahkan backup full dari config snapshot.
- [ ] Reconcile cap angka report dengan config runtime.

### Sprint C — UX dan efek
- [ ] Notification queue sesungguhnya.
- [ ] Scoreboard reusable dan 6–8 baris yang stabil.
- [ ] Preset low/balanced/cinematic plus suara on/off.
- [ ] Tes telegraph rockfall pada Bedrock low-end.
- [ ] Resource pack Java + Bedrock awal dengan warna/ikon tematik yang sederhana.

### Sprint D — Konten & polish
- [ ] Hubungkan event seasonal ke kalender delapan minggu.
- [ ] Variasi objective, summary jurnal, kontrak pemulihan dan reward komunitas.
- [ ] Tambahkan BlueMap untuk `kala_main` saja bila ada kebutuhan pemetaan web.
- [ ] Baru kembangkan fauna/model custom dan event Rob Labuhan/Longsor setelah dua event MVP stabil.

---

## 11. Prompt implementasi untuk AI lokal

Gunakan prompt ini setelah AI lokal dapat membaca working tree repository. Tujuannya mengubah source dengan patch yang aman dan dapat ditinjau, bukan sekadar menulis report yang mengklaim perubahan berhasil.

```text
Kamu adalah senior PaperMC plugin engineer dan QA reviewer untuk repo xDzaky/kala-nusantara.

KONTEKS
- Server: Paper 26.3, Java/Bedrock crossplay lewat Geyser/Floodgate.
- Plugin utama: KalaCore.
- Jangan menambahkan client-side mods sebagai syarat gameplay inti.
- WorldImpactPolicy LEVEL_0 berlaku untuk kala_main: event tidak boleh memutasikan block atau menghancurkan bangunan.
- Baca source, config, docs dan report; jangan mengandalkan klaim report tanpa memverifikasi kode.

ATURAN KERJA
1. Audit git status dan commit SHA sebelum mengubah apa pun; jangan menimpa perubahan pengguna.
2. Kerjakan satu prioritas per patch kecil; jelaskan files yang akan disentuh sebelum patch.
3. Jangan menyentuh secret live atau mencetak credential ke output. Buat sanitized example; jangan menghapus riwayat Git tanpa backup/approval.
4. Jangan menjalankan rockfall/volcano pada production. Semua test gameplay pakai staging.
5. Jangan mengubah world terrain, menghapus world directory atau reset kala_main.
6. Jangan menambah plugin baru hanya untuk menggantikan fitur yang sudah ada; jaga satu pemilik GUI, BossBar, scoreboard dan notifikasi.
7. Jika membutuhkan versi API/dependency baru, pin versi resmi, cek license, kompatibilitas 26.3, dan crossplay; jangan menggunakan random download mirror.
8. Jangan menganggap kompilasi sukses = runtime verified. Bedakan unit test, integration test, dan tes visual manual.

URUTAN IMPLEMENTASI
A. Perbaiki HazardDirector:
- satu rock menghasilkan tepat satu impact;
- BukkitTask benar-benar cancel dan remove dari registry;
- satu pemain tidak menerima damage berulang dari batu yang sama atau beberapa hit dalam satu salvo;
- vent punya deadline dan cancel sendiri;
- lokasi target divalidasi terhadap arena & safe zones.
B. Tambahkan VolcanoArenaConfig/koordinat konfigurabel; hapus pemakaian spawn sebagai arena default; jika config tidak ada, fail closed dan jelaskan.
C. Satukan duration state machine dan subphase durations dalam satu config. Pilih timeline 10 menit atau tulis desain 5 menit lengkap; tidak boleh keduanya bertentangan.
D. Perbaiki SeasonalEventDirector:
- objective target berasal dari objective yang berhasil dibuat;
- Badai Selat dapat sukses dengan jumlah peti yang ada;
- marker interaction tidak menerima klik sebarang blok dekat hotspot;
- cuaca dipulihkan dari snapshot;
- event ID dan cleanup saling terisolasi;
- event tidak overlap dengan erupsi utama;
- sambungkan ke SeasonManager dengan cooldown dan minimum player count.
E. Ledger/storage:
- tentukan writer/connection strategy aman;
- capture data Player di main thread;
- durable score cache rebuild;
- unique event claim table + transaction + idempotent payout;
- atomic deposit/withdraw/transfer, termasuk penerima offline.
F. Backup:
- gunakan SQLite online backup/checkpoint yang konsisten;
- backup world/config penting ke off-site storage;
- checksum, retention, dan restore test.
G. UI/perf:
- notification priority queue yang benar;
- satu event coordinator/BossBar owner;
- scoreboard reuse tanpa unregister/register setiap 2 detik;
- preset LOW/BALANCED/CINEMATIC untuk cosmetic effects.
H. Repository/build:
- bersihkan generated cache/runtime files dari tracked files;
- sanitize server.properties;
- align Paper API/Java target/api-version dengan Paper 26.3;
- tambahkan CI/tests; update plugin manifest exact versions.

VALIDASI WAJIB
- Test impact exactly once, vent timeout, cap enforcement, event cleanup isolation, objective target, claim replay/restart, offline economy, weather restore, backup restore.
- Jalankan clean test/build; laporkan output aktual dan commit SHA.
- Buat staging test checklist Java + Bedrock. Jika tidak bisa menjalankan client Bedrock atau server runtime, tulis TIDAK DIUJI; jangan mengklaim verified.
- Jangan deploy production atau menjalankan event hazard di production tanpa instruksi eksplisit.

OUTPUT
1. Patch source kecil dengan komentar seperlunya.
2. Tests yang benar-benar dijalankan beserta hasil.
3. File docs/V7-CHANGELOG.md yang mencatat masalah, perubahan, test pass/fail, dan manual checks pending.
4. Ringkasan risiko yang tersisa dan perintah deployment/rollback.
```

---

## 12. Referensi resmi dan riset plugin

- Paper scheduler & thread safety: https://docs.papermc.io/paper/dev/scheduler/
- Paper Java/build/API: https://docs.papermc.io/paper/getting-started/ · https://github.com/PaperMC/Paper/blob/main/gradle.properties · https://docs.papermc.io/paper/dev/plugin-yml/
- Geyser resource packs: https://geysermc.org/wiki/geyser/packs/
- Geyser custom items: https://geysermc.org/wiki/geyser/custom-items/
- Geyser/Floodgate setup: https://geysermc.org/wiki/floodgate/setup/
- Rainbow tool: https://github.com/GeyserMC/rainbow
- CoreProtect supported version listing: https://modrinth.com/plugin/coreprotect/versions
- BetterStructures supported version listing: https://modrinth.com/plugin/betterstructures/versions
- BlueMap supported version listing: https://modrinth.com/plugin/bluemap/versions
- KaMenu supported version listing: https://modrinth.com/plugin/kamenu
- ItemsAdder Geyser FAQ: https://wiki.itemsadder.com/faq/geyser/
- Server repo: https://github.com/xDzaky/kala-nusantara

---

## Kesimpulan

Kala Nusantara sudah memiliki identitas yang menarik: pulau Nusantara, gotong royong, Gunung Murka, profesi, fauna dan musim. Fitur tambahan terbaik berikutnya adalah yang membuat fitur tersebut dapat dipercaya: satu event hanya punya satu timeline dan satu owner UI; setiap efek punya batas dan cleanup; reward tidak bisa diduplikasi atau hilang saat restart; dan semua efek penting berfungsi baik di Java maupun Bedrock.

**Urutan paling aman:** security → rockfall task leak → lokasi arena/timing/objective → ledger/storage/economy/backup → tests → GUI/pack → event tambahan. Dengan urutan ini, server bisa menjadi lebih imersif tanpa mengorbankan dunia pemain atau menambah dependency yang belum diperlukan.
