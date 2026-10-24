# Kala Nusantara — V8 Research, Code Audit & Experience Expansion Report

**Versi dokumen:** 8.0  
**Tanggal riset:** 24 Oktober 2026  
**Repo yang diaudit:** [xDzaky/kala-nusantara](https://github.com/xDzaky/kala-nusantara)  
**Commit acuan:** `f0b97b3e973db98123a38ea67897ff4026d4a480`  
**Target:** Paper 26.3, Geyser + Floodgate, Java Edition dan Bedrock/MCPE  
**Tujuan:** Roadmap yang bisa diimplementasikan bertahap oleh AI lokal tanpa merusak fitur dan data produksi.

> **Batas verifikasi:** source code dan konfigurasi pada branch `main` diperiksa melalui GitHub, report V7 yang dilampirkan dibaca, dan dokumentasi publik resmi/plugin pages ditelusuri. Saya tidak memiliki akses langsung ke proses Paper yang sedang berjalan, tidak menjalankan server live dari sini, dan tidak mengulang tes klien Java/Bedrock. Klaim runtime dari report V7 diperlakukan sebagai bukti yang dilaporkan, bukan sebagai sesuatu yang saya nyatakan telah saya jalankan sendiri.

---

## 1. Ringkasan keputusan

V7 memang membawa perbaikan penting, tetapi ada beberapa celah yang membuat saya belum menyarankan penambahan banyak plugin ke server produksi. V8 sebaiknya memiliki tiga sasaran utama:

1. **Menuntaskan keamanan, transaksi reward, dan pemulihan data.** Ini lebih penting daripada menambah monster, animasi, atau texture.
2. **Membangun satu sistem GUI/resource pack yang benar-benar lintas Java–Bedrock.** Plugin GUI mengatur interaksi; resource pack dan mapping mengatur tampilan aset. Itu dua masalah berbeda.
3. **Menambahkan aktivitas yang memperkuat tema Nusantara tanpa menggandakan subsistem yang sudah ada:** koleksi penemuan, fishing, kontrak kampung, navigasi antarpulau, dan peran kerja sama saat bencana.

Saya tidak merekomendasikan langsung memasang Oraxen, ItemsAdder, Nexo, MythicMobs, ModelEngine, plugin GUI, dan plugin skill sekaligus. Itu akan menambah titik kegagalan, sumber konfigurasi, dependensi, serta potensi perbedaan tampilan antara Java dan Bedrock.

### Status ringkas

| Area | Penilaian | Keputusan V8 |
|---|---|---|
| Siklus task rockfall | Perbaikan utama terlihat di source | Pertahankan, tambahkan tes integrasi impak |
| Sinkronisasi timeline 600 detik | Enum subfase sekarang mendefinisikan rentang hingga 600 detik | Uji urutan scheduler dan penyelesaian aktual |
| Klaim reward | Lebih durable dari V6, tetapi belum atomic end-to-end | Prioritas P0 |
| Ekonomi/SQLite | Ada lock baru, tetapi belum semua akses memakai satu pola konsisten | Prioritas P0/P1 |
| Backup | Ada WAL checkpoint, tetapi backup saat ini bukan pemulihan lengkap dan masih berisiko race | Prioritas P0/P1 |
| Java–Bedrock custom pack | Geyser custom content diaktifkan, tetapi pack/mapping server buatan sendiri tidak tampak di snapshot repo | Prioritas P1 |
| GUI | KalaCore sudah punya `MenuManager`; tidak perlu mengganti semua GUI dengan plugin baru | Re-desain dahulu, POC plugin hanya bila ada kebutuhan |
| Fauna | Atlas sudah ada, enam spesies disebut README | Perlu sistem observasi/progresi, bukan sekadar menambah mob |
| Peta web | Belum terlihat viewer peta khusus dalam stack | `squaremap` layak diprototipekan jika peta publik diperlukan |
| Fishing | Profesi Nelayan sudah ada | `EvenMoreFish` adalah kandidat plugin baru paling sesuai untuk POC |
| Worldgen | Dokumen dan file konfigurasi belum sepenuhnya konsisten | Kunci scope dunia sebelum mengimpor pack struktur |

---

## 2. Temuan yang tersisa dari audit V7

### 2.1 P0 — autentikasi dan riwayat secret

Di branch `main`, `server/server.properties` dan `server/server.properties.example` masih menyetel `online-mode=false`. Namun, `docs/COMPATIBILITY.md` menyatakan `online-mode=true`. Geyser dikonfigurasi dengan `auth-type: floodgate`; dokumentasi Floodgate menjelaskan instalasi plugin dan pengaturan `auth-type`, sedangkan Paper menjelaskan bahwa `online-mode=true` memvalidasi akun Java dan `false` tidak melakukan validasi akun tersebut. Jangan membuat konfigurasi keamanan berdasarkan dua dokumen yang bertentangan.

**Tindakan:**
- Tetapkan satu pola autentikasi yang memang dipakai: untuk Paper yang langsung menerima koneksi Java + Geyser/Floodgate, gunakan konfigurasi online-mode yang aman dan uji Floodgate di staging. Jika arsitektur sesungguhnya memakai proxy/offline backend, dokumentasikan proxy tersebut dan batasi koneksi langsung ke backend.
- Uji bahwa akun Java tidak bisa ditiru hanya dengan memilih nama pemain lain.
- `management-server-secret` sekarang kosong pada file terbaru, tetapi nilai yang pernah masuk commit publik harus dianggap terekspos. Rotasi secret terkait meskipun management server saat ini tidak aktif. Penghapusan dari commit terbaru bukan penghapusan dari riwayat Git.
- Tambahkan secret scanning pada CI atau sebelum push.

**Rujukan repo:** [server.properties](https://github.com/xDzaky/kala-nusantara/blob/main/server/server.properties), [server.properties.example](https://github.com/xDzaky/kala-nusantara/blob/main/server/server.properties.example), [COMPATIBILITY.md](https://github.com/xDzaky/kala-nusantara/blob/main/docs/COMPATIBILITY.md).  
**Rujukan resmi:** [Paper server properties](https://docs.papermc.io/paper/reference/server-properties/), [Floodgate Setup](https://geysermc.org/wiki/floodgate/setup/).

### 2.2 P0 — file runtime dan data pemain masih terlacak Git

V7 menghapus cache unpack WorldEdit, tetapi snapshot repositori masih memuat sejumlah file yang lazimnya merupakan runtime/cache atau data pemain, antara lain:

- `server/plugins/Geyser-Spigot/cache/` dan `server/plugins/Geyser-Spigot/locales/`
- `server/plugins/GriefPreventionData/PlayerData/` dan `Logs/`
- `server/usercache.json`
- file konfigurasi server/runtime seperti `ops.json`, `banned-players.json`, `banned-ips.json`, dan whitelist

Tidak semua file tersebut selalu sensitif, tetapi data claim, UUID, nama, log, atau konfigurasi operasional sebaiknya tidak otomatis menjadi bagian repositori publik.

**Tindakan:** periksa setiap file yang saat ini terlacak; pisahkan `*.example.yml` dari konfigurasi runtime, keluarkan cache/log/data pemain dari Git, dan bersihkan riwayat bila berisi data privat. Pertahankan hanya konfigurasi template yang memang aman untuk dibagikan. Jangan mengunggah database pemain, log produksi, world save, private key Floodgate, backup, atau resource pack berlisensi yang tidak boleh didistribusikan.

### 2.3 P0 — klaim hadiah tidak sepenuhnya idempotent dari ujung ke ujung

`EventContributionLedger.claimReward()` sekarang:
1. memeriksa cache klaim,
2. menandai pemain sebagai sudah klaim di cache,
3. menulis klaim ke `event_claims` secara asynchronous,
4. setelah commit berhasil, menjadwalkan pembayaran saldo/EXP di main thread.

Ini meningkatkan daya tahan dibanding versi sebelum V7, tetapi masih punya dua skenario bermasalah:

- **DB gagal:** pemain sudah ditandai claimed di cache sebelum transaksi database berhasil. Setelah query gagal, cache dapat tetap menghalangi pemain mencoba klaim ulang sampai restart atau cache diperbaiki.
- **Crash setelah commit, sebelum payout:** record klaim sudah durable dan pemain akan dianggap sudah klaim, tetapi saldo/EXP mungkin belum diterima. Karena tabel klaim menyimpan nilai reward namun tidak memiliki alur status payout yang dapat dipulihkan, startup tidak bisa membedakan klaim yang sudah dibayar dari klaim yang belum dibayar.

**Desain yang disarankan untuk V8:** jadikan klaim sebagai *outbox/ledger payout* dengan state jelas, misalnya `PENDING`, `PAID`, `FAILED_RETRYABLE`. Sediakan ID transaksi reward yang unik. Ubah status menjadi `PAID` hanya setelah pembayaran terkonfirmasi; pada startup, rekonsiliasi record `PENDING`. Untuk EXP yang diberikan ke pemain offline, simpan sebagai entitlement yang bisa diantarkan saat join. Jangan menganggap `claimedCache` sebagai sumber kebenaran utama; database harus menjadi sumber kebenaran.

**Acceptance test:**
- Dua permintaan claim untuk pemain/event yang sama hanya boleh menghasilkan satu pembayaran.
- Matikan server secara terkontrol pada setiap batas transaksi: sebelum insert, setelah insert, setelah commit, sebelum deposit, setelah deposit.
- Jika query gagal, pemain dapat mencoba lagi; bila payout sudah terjadi, retry tidak membayar dua kali.
- Setelah restart, reward yang belum dibayar dapat dipulihkan tanpa duplikasi.

**Source:** [EventContributionLedger.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java).

### 2.4 P0/P1 — operasi ekonomi belum benar-benar atomic untuk semua keadaan

V7 menyinkronkan method ekonomi dan menambahkan `dbLock`, tetapi jalur offline masih menjadwalkan update saldo ke thread asynchronous dan method dapat kembali sebelum update selesai. `transfer()` memanggil withdraw/deposit; pada pemain offline, dua update dapat menjadi dua operasi async terpisah, sehingga klaim “atomic transfer” belum sepenuhnya didukung oleh bentuk implementasi sekarang. Selain itu, tidak semua query database, termasuk pola baca saldo dan log transaksi, menggunakan lock yang sama secara konsisten.

**Tindakan:**
- Tentukan satu strategi serialisasi SQLite. Untuk server skala awal hingga 20 pemain, satu executor khusus untuk operasi tulis ditambah transaksi SQLite konsisten sudah cukup masuk akal; alternatifnya, gunakan koneksi per operasi dengan transaksi dan penanganan busy/timeout yang tepat. Hindari beberapa thread memakai objek `Connection` dan statement yang sama secara bebas.
- Transfer harus berupa satu transaksi: cek saldo, debit, kredit, dan pencatatan transaksi harus commit bersama. Jangan mengembalikan `SUCCESS` sebelum operasi durable berhasil.
- Tetapkan aturan untuk pemain offline: saldo harus benar-benar berubah dalam transaksi yang sama, bukan dijadwalkan terpisah setelah method menyatakan berhasil.
- Rupiah Kala menggunakan nominal bulat pada UI. Pertimbangkan penyimpanan `INTEGER` (misalnya `long`) agar pembulatan, race, dan perhitungan transaksi tidak bergantung pada `double`.

**Acceptance test:** transfer online→online, online→offline, offline→online, saldo tidak cukup, jumlah nol/negatif/tak hingga, transfer ke diri sendiri, dua transfer bersamaan, DB busy, dan restart langsung setelah transaksi.

**Source:** [RupiahEconomy.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/economy/RupiahEconomy.java), [StorageManager.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/storage/StorageManager.java).

### 2.5 P1 — backup saat ini adalah snapshot parsial, bukan full disaster recovery

`BackupManager` telah menambahkan WAL checkpoint sebelum snapshot. Namun checkpoint dilakukan sebelum pekerjaan asynchronous membuat ZIP. Operasi database dapat berjalan lagi di antara checkpoint dan penyalinan file. Kode juga menyalin file database utama, bukan membuat snapshot SQLite yang konsisten melalui SQLite backup API atau mekanisme snapshot yang mengunci penulis sepanjang proses. Pada mode WAL, penyalinan file DB utama saja setelah ada transaksi baru dapat menghasilkan backup yang tidak mencerminkan state terakhir.

Isi ZIP saat ini terbatas pada database KalaCore serta beberapa file NPC/config; itu belum mencakup konfigurasi Paper, `server.properties`, plugin configs penting, resource pack/mapping, dan world save. Jadi label yang lebih tepat adalah **snapshot data KalaCore**, bukan backup lengkap server.

**Tindakan V8:**
1. Buat dua kategori: **database snapshot** dan **full server backup**.
2. Gunakan mekanisme backup SQLite yang konsisten; jangan hanya menyalin `kala_data.db` ketika server masih aktif.
3. Full backup harus mencakup world `kala_main`, `kala_resource`, konfigurasi server/plugin, resource pack Java/Bedrock, mapping Geyser, dan versi artefak KalaCore. Jangan menyertakan secret ke ZIP yang disimpan di Git.
4. Simpan salinan di luar mesin/server utama jika memungkinkan.
5. Tambahkan retensi otomatis, checksum, manifest versi, ukuran hasil, log sukses/gagal, dan **tes restore ke staging**. ZIP berhasil dibuat belum berarti backup bisa dipulihkan.

**Acceptance test:** pulihkan snapshot ke instance staging bersih, cek jumlah/profile pemain, saldo, quest, fauna discovery, proyek, klaim event, config, dan world yang di-backup.

**Source:** [BackupManager.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/backup/BackupManager.java).

### 2.6 P1 — pemeriksaan lokasi rockfall belum fail-closed

`HazardDirector.findSafeTargetLocation()` sudah mencoba menghindari safe zone spawn dan claim GriefPrevention. Tetapi ada dua celah yang terlihat dari source:

- Integrasi claim memakai reflection lalu menangkap `Throwable` dan mengembalikan `false`. Jika API berubah atau method tidak ditemukan, sistem menganggap lokasi aman meskipun pengecekan claim gagal.
- Fallback memakai `center.clone()` ketika target random gagal, tanpa mengulangi validasi claim dan permukaan padat untuk titik fallback itu.

Ada juga masalah posisi efek: koordinat `getHighestBlockYAt()` dipakai sebagai Y lokasi target, padahal koordinat itu adalah indeks blok permukaan. Untuk menandai permukaan, gunakan lokasi di atas blok permukaan, lalu validasi blok penyangga, blok di atasnya, cairan, claim, zona spawn, dan arena. Pastikan fallback tidak menjadi jalur yang melewati pemeriksaan.

**Tindakan:**
- Jika pemeriksaan claim gagal, tolak target tersebut (fail closed), bukan menyatakan bebas claim.
- Pisahkan `surfaceBlock` dan `impactLocation` dengan aturan offset Y yang eksplisit.
- Validasi semua jalur, termasuk fallback, memakai satu fungsi `isValidHazardTarget`.
- Batasi salvos untuk arena yang sudah ditentukan dan hindari target yang tidak terlihat dari pemain.
- Sediakan debug mode yang mencatat kenapa setiap kandidat ditolak, tanpa menulis log ribuan kali di produksi.

**Source:** [HazardDirector.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/HazardDirector.java).

### 2.7 P1 — cleanup perlu diuji juga setelah hard crash

V7 membersihkan entitas bertag pada shutdown dan akhir event. Namun pembersihan yang hanya berjalan saat `onDisable()` tidak otomatis berjalan ketika proses terhenti mendadak, OS kill, atau crash JVM. Pada jalur startup yang diperiksa, saya belum menemukan pemanggilan cleanup sweep yang secara eksplisit membersihkan entitas event tertinggal di semua world setelah world tersedia.

**Tindakan:** setelah world dimuat, jalankan startup sweep terkontrol untuk entitas dengan PDC `kala_event_entity`, lalu log jumlah yang dibuang. Bersihkan pula registry/task dan state event lama. Jangan menghapus item, hewan, NPC, atau armor stand pemain yang tidak memiliki tag milik KalaCore.

### 2.8 P1 — bencana musiman masih butuh desain lokasi dan interaksi

Target objektif sekarang mengikuti jumlah hotspot yang dibuat, tetapi penempatan Badai Selat masih berupa koordinat acak dalam persegi sekitar spawn. Itu belum menjamin bahwa peti benar-benar berada di garis pantai atau perairan yang cocok. Pada event Abu dan Bara, hotspot berupa lokasi dan partikel; pemain harus berinteraksi dengan blok dalam radius hotspot, padahal tidak ada marker fisik yang jelas dijanjikan pada semua posisi. Untuk pemain Bedrock di layar sentuh, “klik titik bara yang tidak mempunyai objek interaktif jelas” akan membingungkan.

**Tindakan:**
- Buat zona pesisir yang dikurasi: beberapa segmen pantai, dermaga, mercusuar, teluk, atau titik pelabuhan yang sudah diuji. Jangan gunakan persegi acak berdasarkan spawn.
- Peti harus punya identitas event yang jelas, dapat diselamatkan sekali, dan tidak meninggalkan item token tak sengaja atau duplikat jika inventori pemain penuh.
- Untuk hotspot abu/bara, gunakan lokasi marker yang disiapkan dalam map atau entity/marker yang sudah diuji benar di Java dan Bedrock. Hindari `BlockDisplay` bergerak sebagai ketergantungan utama lintas platform.
- Event harus menjelaskan objective, lokasi, durasi, peran, dan reward melalui `/kompas` atau `/gunung`, bukan hanya title/actionbar.
- Kunci jumlah target terhadap jumlah objective unik yang benar-benar bisa dicapai; bila satu objective hilang/terhapus, target perlu diperbarui atau objective pengganti dibuat.

**Source:** [SeasonalEventDirector.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/SeasonalEventDirector.java).

### 2.9 P1 — konfigurasi worldgen saling bertentangan

Di `docs/WORLDGEN-IMPLEMENTATION-STATUS.md`, narasi menyebut struktur hanya boleh muncul di resource/staging dan TerraformGenerator hanya diarahkan ke `kala_terraform_test`. Namun file konfigurasi yang dilacak menunjukkan:

- `server/plugins/BetterStructures/ValidWorlds.yml` masih memuat `kala_main: true`.
- `server/plugins/KalaCore/config.yml` masih memasukkan `kala_main` dalam `world.structures.enabled-worlds`.
- Dokumen status v3 menyebut generator eksternal hanya pada staging, tetapi juga menyatakan TerraformGenerator dikonfigurasi untuk semua Overworld Kala.

Dokumen yang sama menjelaskan belum ada content pack resmi BetterStructures yang dipasang, jadi konflik tersebut tidak otomatis berarti struktur sudah terbangun di produksi sekarang. Namun konfigurasi yang bertentangan berbahaya ketika pack/import baru nanti ditambahkan.

**Tindakan:** buat satu sumber kebenaran untuk daftar world. Sampai pengembangan dan persetujuan map utama selesai, set semua struktur prosedural BetterStructures dan generator eksternal agar tidak berlaku di `kala_main`. Tetap gunakan struktur utama yang dikurasi manual; uji generator dan pack di `kala_terraform_test` atau `kala_resource_test`. Jangan jalankan reset atau pregeneration skala besar pada `kala_main`.

**Source:** [WORLDGEN-IMPLEMENTATION-STATUS.md](https://github.com/xDzaky/kala-nusantara/blob/main/docs/WORLDGEN-IMPLEMENTATION-STATUS.md), [BetterStructures ValidWorlds.yml](https://github.com/xDzaky/kala-nusantara/blob/main/server/plugins/BetterStructures/ValidWorlds.yml), [KalaCore config.yml](https://github.com/xDzaky/kala-nusantara/blob/main/server/plugins/KalaCore/config.yml).

### 2.10 P1 — resource pack dan mapping Bedrock belum terlihat sebagai produk server yang terkelola

Dalam snapshot Git saat ini:
- `server/server.properties` memiliki `resource-pack=` kosong dan `require-resource-pack=false`;
- Geyser `enable-custom-content` sudah `true`, tetapi `resource-pack-urls` masih kosong;
- saya tidak menemukan folder `server/plugins/Geyser-Spigot/packs/` atau `custom_mappings/` berisi pack/mapping khusus Kala Nusantara dalam tree yang terlacak;
- pack yang terlihat adalah file bawaan di dalam `server/plugins/BetterStructures/resource_pack/`, bukan sebuah pack identitas Kala Nusantara yang terpisah dan dipelihara melalui pipeline sendiri.

Ini tidak membuktikan bahwa server runtime tidak punya folder yang tidak di-track, tetapi **repo saat ini belum cukup untuk membangun ulang dan memverifikasi pack custom server dari nol**.

Geyser menyatakan resource pack Java tidak otomatis dikonversi menjadi Bedrock; custom item memerlukan mapping dan pack Bedrock terpisah. Rainbow adalah alat konversi yang masih eksperimental dan dipasang di klien Java pengembang, bukan plugin server yang menyelesaikan semuanya otomatis. Lihat [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/), [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/), dan [Rainbow](https://geysermc.org/wiki/other/rainbow/).

**Tindakan:** V8 membuat struktur source yang jelas:
```text
resource-packs/
  java/
    assets/
    pack.mcmeta
  bedrock/
    manifest.json
    pack_icon.png
    textures/
  mappings/
    geyser/
  build/
    java-pack.zip
    bedrock-pack.mcpack
  manifest.yml
```
Folder `build/` berisi artefak hasil build; source yang diperlukan harus jelas, sedangkan binary pack final dapat disimpan sebagai release artifact atau hosting khusus. Buat skrip build/checksum dan catat `pack version`, hash, ukuran, serta hasil tes klien.

### 2.11 P2 — laporan test V7 lebih luas daripada test yang ada di source

Report V7 menyebut tiga hasil tes yang lolos. Namun dua file test yang terlihat dalam repo saat ini masing-masing memiliki satu method `@Test`: satu memvalidasi rentang durasi subfase di enum, dan satu memvalidasi konstanta budget. Itu berguna, tetapi tidak membuktikan scheduler live, reward setelah crash, klaim, protection hook, atau visual yang benar di Bedrock.

Repo yang terlihat juga belum menunjukkan workflow CI yang menjalankan test saat push.

**Tindakan:** pertahankan unit tests, tetapi tambahkan:
- tes service-level untuk claim/reward/economy dan database;
- integration test dengan MockBukkit atau lingkungan Paper staging, sesuai kesesuaian API;
- smoke test boot dan test command event;
- CI build/test pada push dan pull request;
- checklist manual Java PC + Bedrock Android/Windows/konsol yang memang tersedia untuk diuji.

Untuk klaim “lancar pada 20 pemain”, gunakan profiling saat event aktif, bukan satu tes startup tanpa pemain.

### 2.12 P2 — scoreboard dan notification perlu UX test, bukan sekadar optimasi paket

Scoreboard terbaru memang menggunakan objective yang dapat dipakai ulang dan hanya memperbarui prefix saat nilai berubah. Ia menampilkan tujuh informasi: musim, lokasi, profesi, proyek, dompet, Gunung Murka, dan jumlah online. Itu lebih relevan daripada menampilkan IP dan ping sebagai baris wajib, tetapi tidak sama persis dengan daftar 7 baris pada report V7.

Notification service sekarang menurunkan pesan prioritas rendah menjadi actionbar ketika title yang lebih tinggi masih aktif. Namun itu belum sama dengan antrean yang menjamin semua notifikasi diputar setelah title berakhir; `currentTitleExpiry` disetel 3,5 detik sedangkan total waktu title adalah 4,5 detik. Selain itu, map cooldown dan preferensi tips harus memiliki cleanup/persistence yang jelas.

**Tindakan:** jangan menyatakan “zero flicker / tidak ada title duplikat” tanpa uji di Java dan Bedrock. Uji juga apakah emoji/font yang dipakai di scoreboard dapat dirender pada berbagai perangkat Bedrock. Fitur penting harus tetap dapat dipahami tanpa glyph custom atau emoji.

---

## 3. Riset plugin dan aset: apa yang benar-benar cocok untuk Kala Nusantara?

### 3.1 Prinsip pemilihan

Pilih plugin berdasarkan **fungsi yang belum dimiliki**, bukan berdasarkan jumlah fitur yang ditampilkan di halaman plugin. KalaCore sudah memiliki menu, profesi, jurnal, ekonomi, quest, proyek kampung, musim, atlas fauna, serta event. Plugin baru yang membuat sistem profesi/ekonomi/quest kedua justru dapat membingungkan pemain dan memperbesar beban integrasi.

| Kandidat | Fungsi yang bisa ditambahkan | Keputusan |
|---|---|---|
| **CommandPanels** | Inventory GUI, dialog, dan Bedrock/Floodgate Forms; bisa dikelola melalui YAML/editor | **POC staging** bila tim ingin mengubah menu tanpa rebuild Java. Jangan mengganti semua menu KalaCore sekaligus. |
| **EvenMoreFish** | Ikan koleksi, rarity, jurnal ikan, umpan, kompetisi memancing, reward dan integrasi PlaceholderAPI | **Kandidat gameplay utama** untuk profesi Nelayan. Uji integrasi ekonomi/progresi agar tidak menciptakan ekonomi kedua. |
| **GSit** | Duduk di tangga/kursi, berbaring/crawl; fitur sosial tanpa custom resource pack | **Opsional, risiko rendah** setelah tes kontrol Bedrock. Cocok untuk dermaga, pasar, dan kampung. |
| **squaremap** | Peta 2D live berbasis browser, relatif ringan dan kompatibel 26.3 menurut halaman plugin | **Layak diprototipekan** jika komunitas butuh peta kepulauan; sembunyikan marker pemain/base privat. |
| **BlueMap** | Peta 3D browser | Alternatif squaremap jika peta 3D benar-benar penting. Pilih **satu** viewer saja dan ukur CPU/disk. |
| **WorldGuard** | Proteksi region tetap pada arena, landmark, dan event zone | Opsional. Hindari aturan tumpang tindih dengan GriefPrevention; tentukan plugin mana yang mengontrol setiap zona. |
| **HuskHomes** | Homes, warps, teleport requests, public homes | Tidak diprioritaskan. Teleport bebas bisa memotong rute kapal, ekspedisi, dan eksplorasi yang merupakan inti server. Pertimbangkan nanti dengan batas rumah/biaya/perkembangan. |
| **CoreProtect** | Audit dan rollback blok/container | Tetap nonaktif sampai ada artefak yang benar-benar mencantumkan dukungan Paper 26.3 dan lolos uji startup serta rollback. Listing Hangar untuk 24.1 masih menyebut 26.2; halaman proyek umum menyatakan unduhan hingga 26.3, sehingga jangan mengandalkan klaim umum tanpa menguji artefak aktual. |
| **Oraxen / ItemsAdder / Nexo** | Custom item/model/texture dan resource pack | Jangan dipasang bersamaan. Tentukan satu pipeline setelah uji Geyser. ItemsAdder menyatakan Geyser tidak didukung secara resmi; Oraxen punya dokumentasi dukungan Bedrock melalui addon BedrockGen, tetapi dokumentasi BedrockGen yang diperiksa mencantumkan versi server sampai 26.2, sehingga kompatibilitas 26.3 perlu dikonfirmasi sebelum dipakai. |
| **AuraSkills / Jobs Reborn / plugin quest umum** | Skill/profesi/quest tambahan | Jangan dipasang sekarang; akan tumpang tindih dengan profesi, quest, ekonomi, dan jurnal KalaCore. |
| **MythicMobs + ModelEngine** | Skill mob kompleks dan model 3D | Simpan untuk tahap akhir. Custom entity dan model bukan jaminan pengalaman identik di Bedrock; buat satu POC dulu. |

Rujukan:
- [CommandPanels — kompatibilitas dan jenis panel](https://modrinth.com/plugin/commandpanels), [dokumentasi panel](https://docs.commandpanels.net/).
- [EvenMoreFish](https://modrinth.com/plugin/evenmorefish), [fitur dan dokumentasi](https://evenmorefish.github.io/EvenMoreFish/).
- [GSit](https://modrinth.com/plugin/gsit).
- [squaremap](https://modrinth.com/project/squaremap), [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap).
- [HuskHomes](https://modrinth.com/plugin/huskhomes), [WorldGuard](https://modrinth.com/plugin/worldguard).
- [CoreProtect version list](https://hangar.papermc.io/CORE/CoreProtect/versions).
- [Oraxen](https://docs.oraxen.com/), [BedrockGen requirements](https://docs.bedrockgen.com/), [ItemsAdder Geyser FAQ](https://wiki.itemsadder.com/faq/geyser/).
- [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/), [Geyser entity API](https://geysermc.org/wiki/geyser/custom-entities/).

### 3.2 GUI plugin bukan pembuat texture otomatis

Ada tiga lapisan yang perlu dipisahkan:

1. **GUI logic:** slot, halaman, tombol, permission, kondisi, dan aksi klik.
2. **Icon/art:** item vanilla atau custom item model yang muncul pada slot.
3. **Resource pack:** aset yang menjadikan icon/model/suara terlihat khusus. Java dan Bedrock perlu jalur aset yang sesuai.

CommandPanels mengelola panel dan memiliki tipe panel untuk Bedrock/Floodgate. Ia bukan pengganti mapping dan texture pack. KalaCore juga sudah memiliki `MenuManager`; jadi pilihan terbaik bukan memasang plugin baru hanya untuk mendapatkan GUI. Buat design system dan mockup dulu, lalu putuskan apakah:
- menu inti tetap memakai kode KalaCore agar transaksi aman dan state terintegrasi; atau
- panel sederhana seperti panduan, info, FAQ, pengaturan, dan navigator dipindahkan ke CommandPanels.

Jangan membuat dua GUI yang sama-sama mengatur saldo, claim event, atau transfer. Operasi ekonomi harus melewati API/validasi KalaCore.

### 3.3 Resource pack lintas Java dan Bedrock

Geyser menyatakan secara eksplisit bahwa pack Java tidak otomatis dikonversi ke format Bedrock dan custom item membutuhkan mapping beserta Bedrock pack. Artinya, resource pack tidak boleh dianggap “selesai” hanya karena item terlihat benar pada Java.

**Rekomendasi implementasi:**
- Mulai dengan pack kecil: logo/identitas, ikon item khusus, beberapa variasi item fauna, suara pendek untuk event, dan gaya visual yang konsisten. Jangan mulai dengan tekstur ulang semua blok.
- Gunakan item vanilla sebagai fallback agar pemain yang menolak pack tetap bisa memahami fungsi item.
- Versioning: setiap build memiliki manifest, pack version, hash, catatan perubahan, dan hasil tes kedua platform.
- Tes pack di Java dengan resource pack server aktif serta di Bedrock pada Android/Windows yang tersedia. Jika pack wajib, beri prompt yang menjelaskan alasan dan jangan memaksa sebelum pack stabil.
- Rainbow dapat membantu workflow conversion, tetapi masih eksperimental dan merupakan alat client-side untuk pembuat pack. Jangan membuat pipeline produksi bergantung pada Rainbow tanpa tes ulang setiap perubahan.

**Sumber resmi:** [Geyser packs](https://geysermc.org/wiki/geyser/packs/), [Geyser Custom Items](https://geysermc.org/wiki/geyser/custom-items/), [Rainbow](https://geysermc.org/wiki/other/rainbow/).

### 3.4 Desain GUI: referensi dan arah visual Kala Nusantara

Referensi yang bisa dipelajari:
- [Minecraft GUIs Collection — Behance](https://www.behance.net/gallery/162872991/Minecraft-GUIs-Collection): ide susunan icon navigasi dalam pola grid dan latar bernuansa peta.
- [Oraxen item appearance](https://docs.oraxen.com/configuration/items/appearance): contoh penggunaan model/texture untuk menegaskan identitas item.
- [Hypixel SkyBlock Collections](https://hypixelskyblock.minecraft.wiki/w/Collections): progresi koleksi memakai kategori, milestone, dan reward yang dapat dipahami.
- [Wynncraft Professions](https://wynncraft.wiki.gg/wiki/Professions): inspirasi memisahkan aktivitas gathering dari crafting dan memberi profesi progresi jangka panjang.

Gunakan prinsip desain itu sebagai inspirasi, bukan menyalin gambar, texture, atau aset berlisensi server lain.

**Arah visual yang kusarankan:**
- **Palet 4 warna utama:** arang/abu vulkanik untuk dasar, perkamen/kayu untuk informasi, teal laut untuk navigasi, emas tembaga untuk progress/reward. Merah dipakai hanya untuk bahaya/aksi destruktif.
- **Ikon:** gunakan Material vanilla yang mudah dikenali sebelum membuat custom item; jangan memberi semua icon warna neon berbeda.
- **Latar:** inventori chest tetap inventori Minecraft standar untuk stabilitas. Untuk Java dan Bedrock, jangan bergantung pada glyph font buatan yang mungkin tidak tersedia di client lain.
- **Teks:** nama item ringkas; lore 2–4 baris dengan format tetap: deskripsi, status/progres, aksi atau persyaratan.
- **Mobile first:** menu utama cukup 3 baris/27 slot dengan 6–7 pilihan yang jelas. Menu informasi detail boleh memakai 45/54 slot, tetapi hindari memasukkan semua fitur ke satu layar.
- **Konsistensi:** tombol “Kembali”, “Tutup”, halaman berikutnya dan aksi utama selalu berada di posisi yang konsisten.
- **Feedback:** suara klik/sukses/gagal ringan, bukan suara setiap detik; tidak semua klik harus memanggil title.

#### Struktur GUI yang disarankan

| Menu | Isi | Tujuan |
|---|---|---|
| `/kompas` — Navigator | Misi, Peta, Kampung, Profesi, Jurnal, Fauna, Musim | Hub utama, maksimal 6–7 pilihan |
| Profil | Profesi/level, saldo, status tutorial, koleksi, gelar aktif | Ringkas dan personal |
| Peta/Navigasi | Lokasi sekarang, tujuan, rute kapal, daerah terkunci, panduan perjalanan | Membantu pemain yang belum hafal pulau |
| Jurnal & Koleksi | Fauna, ikan, situs sejarah, sampel vulkanik, pencapaian | Memberi progres jangka panjang |
| Event | Status Gunung Murka, objective aktif, sisa waktu, kontribusi, reward, zona aman | Tempat detail; bukan menaruh semua informasi di actionbar |
| Pengaturan | Partikel, suara event, title, tips, scoreboard, mode hemat HP | Membantu pemain Bedrock/perangkat lemah |

### 3.5 Scoreboard, BossBar, title, actionbar: siapa menampilkan apa?

Agar tidak duplikat, tetapkan *UI ownership matrix* dan jadikan aturan di KalaCore:

| Kanal | Tanggung jawab | Yang tidak boleh dilakukan |
|---|---|---|
| Scoreboard | Data yang stabil: musim, lokasi, profesi, dompet, event/proyek | Countdown berkedip tiap tick atau mengulang seluruh deskripsi event |
| BossBar | Progres objective event utama dan timer | Dua BossBar panjang yang saling menumpuk tanpa prioritas |
| Title | Mulai fase, peringatan penting, klimaks, hasil | Tips kecil atau pesan yang sama berkali-kali |
| Actionbar | Peringatan sesaat, kontrol singkat, status interaksi | Menjadi satu-satunya tempat informasi penting |
| Chat | Pengumuman start/end, detail hasil, reward, instruksi yang dapat dibaca kembali | Spam per detik |
| GUI/jurnal | Informasi panjang, reward detail, persyaratan, riwayat | Diganti pesan title yang hilang sesudah beberapa detik |

**Koreksi terhadap report V7:** scoreboard yang ada sekarang menampilkan musim, lokasi, profesi, proyek, saldo, Gunung Murka, dan jumlah online. Itu tujuh baris dan cukup masuk akal. Jangan menambah IP/ping hanya karena sempat disebut di report. Ping dapat tersedia lewat command/status sederhana, dan alamat server lebih cocok berada pada halaman panduan, bukan sidebar gameplay.

Tambahkan pengujian Bedrock karena simbol Unicode/emoji tidak selalu identik di semua perangkat. Jika simbol tidak terbaca, icon vanilla dan teks biasa harus tetap menjelaskan fungsi.

---

## 4. Fitur permainan baru yang paling masuk akal

### 4.1 Prioritas utama: Koleksi Penemuan Nusantara

KalaCore sudah memiliki Atlas Fauna enam spesies dan database `player_fauna_discovery`. Jangan sekadar menambah 20 mob. Jadikan discovery sebagai sistem yang memberi tujuan eksplorasi:

- Tiap pulau memiliki katalog: fauna, ikan, tumbuhan, landmark, cerita lokal, dan sampel geologi.
- Pemain menyelesaikan *observasi*, bukan harus membunuh atau menangkap satwa.
- Ada tier penemuan umum, langka, musiman, dan temuan komunitas.
- Reward terutama berupa entri jurnal, title kosmetik, dekorasi rumah, resep, badge, dan kontribusi proyek kampung—bukan statistik tempur yang membuat pemain baru tertinggal.
- Discovery dapat terhubung dengan NPC peneliti, nelayan, petani, dan penjaga mercusuar.
- Simpan UUID pemain, ID discovery, waktu, region, musim, dan sumber unlock secara durable. Gunakan unique constraint supaya penemuan tidak dobel.
- GUI memperlihatkan siluet/teks “belum ditemukan” dan petunjuk habitat, tetapi tidak membocorkan seluruh koordinat langka sejak awal.

Ini mengambil pola koleksi/milestone yang mudah dipahami dari sistem koleksi server RPG besar, lalu memberi identitas sendiri melalui pulau dan fauna Nusantara. Hypixel SkyBlock memakai koleksi berbasis material dan milestone reward; Wynncraft membedakan gathering dan crafting sebagai profesi tersendiri. Kala dapat memakai pola progresi tersebut tanpa menyalin keseimbangan atau aset mereka.

### 4.2 POC gameplay baru: fishing yang benar-benar menarik

Profesi Nelayan cocok menjadi sistem konten pertama untuk plugin tambahan. `EvenMoreFish` secara publik mencantumkan dukungan 26.3 dan menyediakan custom fish, rarity, journal, bait, competition, rewards, serta aturan biome/region.

Rencana uji:
1. Pasang hanya di `kala_resource_test`/staging, jangan langsung di production.
2. Mulai dengan 8–12 ikan bertema perairan Nusantara, bukan langsung mengaktifkan semua konten.
3. Buat ikan umum untuk aktivitas biasa; ikan langka muncul dari kombinasi waktu, biome/area, umpan, atau musim.
4. Hubungkan reward/progresi ke Profesi Nelayan, Jurnal, Atlas/Koleksi, dan Rupiah Kala.
5. Nonaktifkan ekonomi terpisah atau reward uang default jika menimbulkan ekonomi kedua.
6. Pakai kompetisi kecil sebagai event sampingan pada waktu yang tidak bentrok dengan erupsi.
7. Uji item yang masuk inventori Bedrock, nama/lore, enchantment, dan tindakan jual.

**Sumber:** [EvenMoreFish di Modrinth](https://modrinth.com/plugin/evenmorefish), [dokumentasi resmi](https://evenmorefish.github.io/EvenMoreFish/).

### 4.3 Barang utilitas yang memiliki fungsi nyata

Jangan membuat banyak item custom hanya untuk terlihat keren. Mulai dari 4–6 item bermakna dengan base material vanilla dan PDC ID internal:

| Item | Fungsi | Batas anti-eksploitasi |
|---|---|---|
| **Kompas Pengembara** | Membuka navigator, menampilkan petunjuk lokasi/landmark | Bukan teleporter gratis tanpa batas |
| **Kit Survei** | Menyelesaikan objective observasi, menandai lokasi penelitian | Satu objective/area memberi poin sekali |
| **Lampu Suar** | Objective kelompok untuk menyalakan sinyal pada event badai | Cooldown, tidak menciptakan api menyebar |
| **Kerudung Abu** | Item/proteksi ringan untuk mengurangi penalti abu/retakan tertentu | Harus ada trade-off dan durasi; bukan imunitas semua hazard |
| **Peti Logistik** | Token objective yang benar-benar disimpan sebagai event item | Unik per ID objective, tidak dapat duplikasi/claim ganda |
| **Peta Jalur Laut** | Membuka rute/tujuan yang sudah ditemukan | Informasi tetap tersedia lewat GUI untuk pemain yang kehilangan item |

Untuk V8 awal, cukup implementasikan Kompas Pengembara dan Kit Survei menggunakan item vanilla bernama khusus. Tambahkan custom model setelah pipeline pack Java/Bedrock siap.

### 4.4 Kontrak kampung dan reward berjenjang

Setiap minggu, NPC dapat menawarkan 3–5 kontrak:
- kontrak profesi (panen, tambang, hasil laut, kayu);
- kontrak eksplorasi (temukan landmark, kirim sampel fauna);
- kontrak pemulihan (bantu pasca-erupsi, amankan logistik);
- kontrak sosial (sumbang proyek kampung, bantu pemain baru).

Kontrak harus punya batas mingguan dan rotasi agar ekonomi tidak inflasi. Reward terbaik adalah unlock dekorasi, title, resep, entri lore, atau kosmetik. Uang/EXP tetap ada, tetapi bukan satu-satunya alasan bermain.

### 4.5 Variasi bencana musiman setelah V7 stabil

Jangan meluncurkan semua bencana sekaligus. Rework dua event saat ini sampai target dan marker-nya jelas, lalu tambah satu event baru per iterasi:

- **Badai Selat:** objective berada di titik pantai/dermaga yang sengaja dikurasi, pemain mengamankan kargo, memperbaiki sinyal, dan membantu kapal.
- **Abu dan Bara:** hotspot yang dapat dilihat dan diklik dengan jelas; pemain membawa sampel ke pos riset atau menyiapkan perlengkapan proteksi.
- **Rob Labuhan:** air tidak dimodifikasi pada blok dunia. Pemain mengevakuasi logistik ke titik tinggi, mengaktifkan pompa dekoratif, dan membuka jalur aman.
- **Kabut Hutan:** event navigasi; pemain mengikuti suar/petunjuk, menemukan satwa, dan mengantar perlengkapan.
- **Longsor Jalur:** marker bahaya dan jalur aman di arena kurasi; tidak menimpa bangunan pemain.
- **Gempa Ringan:** puzzle kerja sama, stabilisasi beacon/struktur yang sudah disiapkan dalam map; tidak menggeser block pemain.

#### Peran kooperatif saat event

Agar bencana tidak selalu menjadi “lari dari partikel”, sediakan beberapa peran yang memberi kontribusi seimbang:
- **Pengamat:** mengidentifikasi titik/menyerahkan sampel.
- **Navigator:** menemukan jalur aman dan mengaktifkan suar.
- **Penolong:** mengantarkan barang/menyelamatkan kargo.
- **Penjaga:** melindungi tim dari mob event.
- **Pemulih:** mengantar material dan menaikkan progres desa.

Skor jangan hanya dihitung dari damage per hit. Setiap tindakan yang mengubah objective harus idempotent per objek/event; kontribusi combat perlu cap per mob dan cooldown, supaya macro hit tidak mengalahkan pemain yang menyelesaikan tugas.

### 4.6 Reward yang adil dan menjaga ekonomi

Gunakan empat lapisan reward:
1. **Partisipasi minimum** untuk pemain yang benar-benar melakukan objective.
2. **Kontribusi tambahan** untuk objective unik yang berbeda.
3. **Bonus komunitas** jika target kelompok berhasil.
4. **Reward musiman jangka panjang**: jurnal, cosmetic, title, recipe, dekorasi, atau reputasi.

Batasi multiplier, dan beri cap kontribusi per objective/player. Hindari memberi 5 poin untuk setiap hit selamanya; hit berulang pada satu mob sebaiknya tidak menghasilkan reward tanpa batas. Simpan `objective_instance_id` yang unik (misalnya ID hotspot/event mob) agar interaksi yang sama tidak bisa dimainkan ulang untuk score.

---

## 5. Fauna, monster, animasi, dan efek tanpa mod client

### 5.1 Kenapa tidak mulai dari mod hewan besar?

Mod Java seperti mod fauna yang memerlukan Forge/Fabric di klien tidak akan otomatis tersedia pada Bedrock/MCPE. Untuk target “masuk tanpa mod client”, jalur yang lebih aman adalah:
- perilaku/server logic lewat Paper plugin;
- entitas vanilla yang sudah dimengerti Geyser;
- tekstur terpisah untuk Java dan Bedrock;
- mapping khusus hanya setelah diuji pada versi Geyser aktual.

Geyser memiliki Entity API custom yang ditandai **experimental**. Karena itu jangan menjadikan custom entity API tersebut sebagai kebutuhan agar pemain Bedrock bisa ikut event utama. Mulai dengan monster berbasis entity vanilla (misalnya Magma Cube kecil sebagai Percik Bara) dan kosmetik pack yang tidak mengubah kemampuan dasar.

**Sumber:** [Geyser Entity API](https://geysermc.org/wiki/geyser/custom-entities/).

### 5.2 Cara menghadirkan satwa Nusantara

Tahap awal cukup memakai spesies yang perilakunya bisa direpresentasikan oleh entitas vanilla:
- Komodo: sebagai spesies observasi khusus dengan perilaku terbatas di area pulau yang ditentukan.
- Burung endemik: variasi observasi berbasis parrot atau entitas terbang yang sudah disediakan server.
- Fauna hutan/pesisir: gunakan entity vanilla sebagai basis sambil menjaga ukuran populasi.
- Ikan koleksi: mulai melalui sistem fishing, karena tidak perlu menambah puluhan entitas hidup di chunk.

Bila ingin model yang benar-benar baru, bangun dan uji satu spesies di staging. Ukur render, hitbox, interaksi, nama, item drop, dan perilaku pada Java serta Bedrock. Jangan menambah populasi atau model sampai mapping dinyatakan cocok pada dua klien.

### 5.3 Bahasa visual bencana yang jelas

| Efek | Visual yang disarankan | Tindakan pemain |
|---|---|---|
| Pertanda erupsi | Suara gemuruh pelan dan asap jauh dari puncak | Persiapan |
| Zona rockfall | Cincin di tanah, suara cue yang konsisten, 2,5–3 detik telegraph | Menghindar |
| Benturan batu | Satu kepulan debu, suara batu, satu resolusi damage | Menjauh dari radius |
| Retakan panas | Asap vertikal dan perubahan bunyi dari titik tetap | Menghindar atau mengenakan alat proteksi |
| Badai Selat | Kabut ringan lokal, suara angin berkala, suar di pantai | Navigasi/kargo |
| Abu vulkanik | Partikel lebih jarang dan kondisi atmosferik lokal | Membawa sampel/menolong desa |

**Aturan optimasi:** efek tidak harus terus-menerus. Gunakan telegraph yang terlihat dan konsisten, lalu hentikan efek setelah event. Berikan pengaturan partikel rendah/normal/mati dan suara event on/off pada `/pengaturan`. Jangan mengirim asap besar setiap tick ke semua pemain. Uji dengan 1, 5, 10, dan 20 pemain serta perangkat Bedrock kelas rendah.

### 5.4 Zero Block Destruction tetap, tetapi tidak berarti no consequence

`kala_main` tetap tidak diubah oleh event. Konsekuensi bisa datang dari:
- objective yang belum berhasil;
- reward yang lebih kecil;
- jalur sementara tertutup lewat marker/event state;
- bonus proyek kampung yang tidak terbuka;
- misi pemulihan yang muncul;
- fragment/item riset yang hanya tersedia selama fase tertentu.

Dengan cara ini dunia terasa bereaksi tanpa menghancurkan rumah pemain. Bila kelak ada kerusakan kecil, lakukan hanya pada zona staging/arena yang disetujui, memakai block allowlist, snapshot/rollback, claim exclusion dan tes eksplisit. Jangan ubah world utama secara dinamis hanya agar bencana terlihat dramatis.

---

## 6. Performa: ukur sebelum menambah konten

Paper menyertakan spark profiler pada versi modern; dokumentasinya merekomendasikan profiling ketika masalah sedang terjadi. Jalankan profil saat event benar-benar aktif, bukan hanya saat server idle.

**Baseline yang perlu direkam:**
- TPS dan MSPT (rata-rata serta p95/p99 jika tersedia);
- CPU dan waktu tick saat partikel/event aktif;
- heap usage dan pola GC;
- jumlah entitas event dan task scheduler;
- ukuran world/region files;
- waktu chunk generation;
- durasi join/reconnect Java dan Bedrock;
- packet/scoreboard spam selama event;
- ukuran pack Java dan Bedrock, durasi download dan keberhasilan pack reload.

**Prosedur profiling:**
```text
/spark profiler start --timeout 600
```
Lakukan ketika event 10 menit sedang berlangsung dan simpan URL hasilnya bersama commit SHA dan jumlah pemain. Profiling server harus diulang setelah perubahan entity, particle loop, UI update, resource pack pipeline, atau plugin baru.

**Sumber:** [PaperMC profiling/spark](https://docs.papermc.io/paper/profiling/).

Jangan menambah spark kedua secara otomatis jika versi yang sesuai sudah dibundel Paper. Pastikan resource packs tidak terlalu besar dan bahwa `Chunky`/worldgen tidak berjalan bersamaan dengan event produksi. Render map web juga perlu diuji pada beban CPU yang realistis.

---

## 7. Roadmap V8 yang disarankan

### V8-A — Stabilitas dan keamanan (P0)

**Jangan menambah plugin gameplay pada tahap ini.**
- [ ] Selaraskan `online-mode` di config, template, dan `COMPATIBILITY.md`; uji Java dan Bedrock/Floodgate.
- [ ] Rotasi secret historis dan bersihkan file runtime/private data dari Git.
- [ ] Ubah claim reward menjadi payout ledger dengan status pending/paid dan rekonsiliasi setelah restart.
- [ ] Benahi transfer offline/online agar satu transaksi, dan serialisasi semua akses database secara konsisten.
- [ ] Buat backup database konsisten + full backup/restore teruji.
- [ ] Jalankan startup sweep untuk event entity lama setelah world tersedia.
- [ ] Perbaiki validasi lokasi rockfall: claim check fail-closed, fallback tervalidasi, surface Y benar.
- [ ] Kunci struktur/generator agar tidak aktif di `kala_main` sampai ada persetujuan map.
- [ ] Tambahkan CI yang benar-benar menjalankan test.

**Definition of Done:** tidak ada pembayaran hilang/duplikat pada crash test, transfer tidak sukses sebelum commit, server Java/Bedrock dapat join dengan identitas benar, dan full backup berhasil di-restore ke staging.

### V8-B — Polesan GUI dan resource packs (P1)

- [ ] Tetapkan palette dan format nama/lore/ikon.
- [ ] Buat `/kompas` mobile-first, halaman Profil, Peta, Koleksi, Event, dan Pengaturan.
- [ ] Tetapkan ownership matrix scoreboard/BossBar/title/actionbar/chat.
- [ ] Implementasikan preferensi partikel/suara/tips/scoreboard pemain.
- [ ] Siapkan source pack Java dan Bedrock terpisah, version/hash dan build script.
- [ ] POC satu item khusus yang mempunyai fallback vanilla.
- [ ] POC CommandPanels pada staging hanya untuk menu yang cocok; jika tidak lebih baik daripada `MenuManager`, jangan menambah dependency.
- [ ] Tes ikon, lore, klik, tombol kembali, inventory close, dan pack reload pada Java + Bedrock.

**Definition of Done:** setiap fitur utama dapat dipakai tanpa custom asset, dan custom asset yang ada tampil benar pada kedua platform.

### V8-C — Konten yang memperkuat tema (P1/P2)

- [ ] Rework Badai Selat ke zona pantai tetap dan marker yang jelas.
- [ ] Rework Abu dan Bara menjadi hotspot interaktif yang jelas untuk layar sentuh.
- [ ] Tambahkan katalog discovery/progresi fauna.
- [ ] POC `EvenMoreFish` pada staging; integrasikan ke Profesi Nelayan, jurnal, dan ekonomi Kala.
- [ ] Buat 3–5 kontrak kampung mingguan yang rotasi.
- [ ] Tambahkan satu event non-destruktif baru (saran: Kabut Hutan atau Rob Labuhan).
- [ ] Tambahkan GSit jika komunitas ingin interaksi sosial di tempat publik.
- [ ] Uji squaremap; bila diterima, buat marker landmark tanpa menampilkan rumah/lokasi privat.

**Definition of Done:** satu aktivitas baru punya tutorial, objective jelas, indikator progres, reward, cooldown, cleanup, tes mobile, serta profil performa.

### V8-D — Visual fauna/monster dan item custom (P2)

- [ ] Pilih satu species fauna baru dan satu monster event yang memakai entity vanilla sebagai fallback.
- [ ] Buat custom texture pack Java/Bedrock dan mapping Geyser hanya setelah pipeline pack terverifikasi.
- [ ] Uji satu model/item sebelum membuat banyak variasi.
- [ ] Jangan mengaktifkan API custom entity eksperimental sebagai dependency gameplay utama.
- [ ] Tambahkan animasi yang tetap masuk akal apabila display entity tidak bergerak atau pack tidak diterima.

**Definition of Done:** tidak ada item/monster yang membuat objective tidak bisa diselesaikan ketika pack gagal diunduh, dan tidak ada perbedaan hitbox/perilaku yang merugikan pemain Bedrock.

---

## 8. Checklist QA V8

### Keamanan dan data
- [ ] Secret scanner bersih pada commit terbaru dan audit riwayat Git selesai.
- [ ] `server.properties`, template, Paper config, Geyser, dan dokumentasi menjelaskan auth-mode yang sama.
- [ ] Tidak ada data pemain/log/cache/private key atau world save sensitif dalam Git.
- [ ] Transfer saldo online/offline transaksional.
- [ ] Reward event tidak hilang atau berlipat pada crash injection/restart.
- [ ] Backup dapat dipulihkan dalam instance staging baru.

### Event dan dunia
- [ ] Rockfall tidak menyentuh spawn safe zone/claim; bila claim API gagal, hazard tidak spawn.
- [ ] Fallback target tetap punya permukaan valid dan berada di zona arena.
- [ ] Satu batu hanya menimbulkan satu resolusi damage.
- [ ] Restart mendadak tidak meninggalkan peti/mob/hazard event setelah startup cleanup.
- [ ] Badai menggunakan titik pesisir yang ditentukan, bukan lokasi acak yang mungkin berada di rumah/jalan pedalaman.
- [ ] Hotspot bara terlihat dan bisa diklik dari kontrol Bedrock.
- [ ] `objectiveTarget` selalu dapat dicapai berdasarkan objective yang masih aktif.
- [ ] Struktur/generator prosedural tidak berjalan pada `kala_main` sebelum disetujui.

### UI dan resource pack
- [ ] GUI tidak memakai glyph custom sebagai satu-satunya informasi penting.
- [ ] Semua tombol penting bisa digunakan tanpa shift-click dan drag.
- [ ] Tombol kembali/tutup/halaman konsisten.
- [ ] Title, BossBar, actionbar, scoreboard dan chat tidak mengulang pesan yang sama.
- [ ] Pack Java dapat diunduh dan hash cocok.
- [ ] Pack Bedrock dan mapping Geyser dimuat setelah restart; versi pack dicatat.
- [ ] Pack ditolak/gagal pun tidak membuat objective utama mustahil.
- [ ] Uji Java dan Bedrock pada 1, 5, 10 dan 20 pemain bila kapasitas pengujian memungkinkan.

### Performa
- [ ] Spark profile direkam saat event aktif.
- [ ] Tidak ada task/cooldown/ledger cache tanpa kebijakan cleanup.
- [ ] Budget entitas dan partikel dapat diubah melalui config dan dipatuhi saat runtime.
- [ ] Chunk generation/pregeneration tidak berjalan bersamaan dengan event produksi.
- [ ] Peta web tidak mengungkap koordinat privat dan tidak menyebabkan CPU/disk spike yang tidak perlu.

---

## 9. Prompt implementasi untuk AI lokal

Salin prompt ini sebagai instruksi kerja. Jangan meminta AI memasang semua plugin sekaligus.

```text
Kamu adalah senior PaperMC plugin engineer dan reviewer server Minecraft crossplay.
Repo: https://github.com/xDzaky/kala-nusantara
Target server yang dilaporkan: Paper 26.3, Geyser + Floodgate, Java dan Bedrock/MCPE.
Baca dokumen V8 Research, Code Audit & Experience Expansion Report sebelum mengubah kode.

ATURAN WAJIB
1. Audit source dan konfigurasi terbaru terlebih dahulu. Jangan menganggap status di report berarti semua kasus sudah diperbaiki.
2. Jangan mengubah atau mereset world production kala_main. Jangan mengaktifkan worldgen/BetterStructures untuk kala_main sebelum scope dikonfirmasi dalam config dan acceptance tests.
3. Jangan mengunduh atau menginstal plugin tanpa memeriksa halaman resmi, kompatibilitas Paper 26.3, lisensi, dependensi, dan jalur Java/Bedrock.
4. Jangan menambah lebih dari satu plugin baru pada satu tahap; gunakan staging terlebih dahulu.
5. Jangan mengganti ekonomi, profesi, quest, NPC, menu atau musim KalaCore dengan sistem kedua yang tumpang tindih.
6. Semua item/event entity harus memakai ID/PDC, memiliki cap, cleanup, dan anti-duplication.
7. Semua perubahan transaksi database harus diuji pada kegagalan DB dan crash/restart; jangan menyatakan reward durable hanya karena data klaim tercatat.
8. Jangan menjadikan custom item/texture/custom entity sebagai syarat menyelesaikan gameplay Bedrock.
9. Jangan memasukkan secret, database runtime, world save, logs, cache Geyser, atau data pemain ke Git.
10. Jangan mengklaim test berhasil kecuali perintah dan hasil sebenarnya tersedia.

URUTAN KERJA
Tahap A: audit `server.properties`, example, Geyser auth, secrets, tracked runtime files, worldgen scope, dan perbaiki dokumentasi yang bertentangan.
Tahap B: perbaiki payout ledger, transaksi ekonomi, SQLite serialization, backup/restore, startup entity sweep, dan validasi rockfall fail-closed.
Tahap C: buat tes yang menguji perilaku sebenarnya: claim retry, crash point, transfer online/offline, rockfall one-shot, event cleanup, seasonal objective dan protection.
Tahap D: rework lokasi dan interaksi Badai Selat/Abu dan Bara supaya jelas bagi Bedrock touch controls.
Tahap E: buat UI design system, ownership matrix, preferensi pemain, dan pipeline resource pack Java + Bedrock.
Tahap F: prototipe satu fitur baru yang dipilih berdasarkan report (utamakan fishing/collections); tambahkan plugin hanya jika tidak menduplikasi KalaCore.
Tahap G: profile menggunakan spark ketika event berlangsung dan tes Java/Bedrock.

UNTUK SETIAP TAHAP
- Buat branch terpisah atau commit kecil yang terfokus.
- Sebelum mengubah file, catat baseline commit SHA.
- Tampilkan daftar file yang diubah dan alasan.
- Jalankan Gradle tests dan build dengan JDK/API yang kompatibel dengan server target.
- Jalankan tes staging untuk perubahan runtime.
- Berikan log asli, kondisi test, hasil aktual, dan kendala.
- Perbarui COMPATIBILITY.md dan changelog.
- Jangan melakukan deploy ke production atau mengubah world utama otomatis.
- Jika ada ketidakpastian, pilih default paling aman dan dokumentasikan apa yang belum terverifikasi.
```

---

## 10. Sumber riset dan tautan

### Repo dan dokumen Kala Nusantara
- [Repo utama](https://github.com/xDzaky/kala-nusantara)
- [V7 Code Audit & Content Roadmap](https://github.com/xDzaky/kala-nusantara/blob/main/docs/Kala-Nusantara-V7-Code-Audit-and-Content-Roadmap.md)
- [V7 Implementation Report](https://github.com/xDzaky/kala-nusantara/blob/main/docs/Kala-Nusantara-V7-Code-Audit-and-Implementation-Report.md)
- [Worldgen status](https://github.com/xDzaky/kala-nusantara/blob/main/docs/WORLDGEN-IMPLEMENTATION-STATUS.md)
- [Compatibility matrix](https://github.com/xDzaky/kala-nusantara/blob/main/docs/COMPATIBILITY.md)
- [HazardDirector](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/HazardDirector.java)
- [EventContributionLedger](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java)
- [RupiahEconomy](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/economy/RupiahEconomy.java)
- [BackupManager](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/backup/BackupManager.java)
- [KalaCore config](https://github.com/xDzaky/kala-nusantara/blob/main/server/plugins/KalaCore/config.yml)
- [BetterStructures ValidWorlds](https://github.com/xDzaky/kala-nusantara/blob/main/server/plugins/BetterStructures/ValidWorlds.yml)

### Paper, security, profiling
- [Paper server properties](https://docs.papermc.io/paper/reference/server-properties/)
- [Paper profiling / spark](https://docs.papermc.io/paper/profiling/)
- [Paper Velocity backend security](https://docs.papermc.io/velocity/security/)

### Crossplay and packs
- [Geyser setup](https://geysermc.org/wiki/geyser/setup/)
- [Floodgate setup](https://geysermc.org/wiki/floodgate/setup/)
- [Geyser custom content](https://geysermc.org/wiki/geyser/custom-content/)
- [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/)
- [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/)
- [Geyser custom entity API](https://geysermc.org/wiki/geyser/custom-entities/)
- [Rainbow conversion tool](https://geysermc.org/wiki/other/rainbow/)
- [Oraxen documentation](https://docs.oraxen.com/)
- [BedrockGen requirements](https://docs.bedrockgen.com/)
- [ItemsAdder Geyser FAQ](https://wiki.itemsadder.com/faq/geyser/)

### GUI, content, utility plugins
- [CommandPanels — Modrinth](https://modrinth.com/plugin/commandpanels)
- [CommandPanels documentation](https://docs.commandpanels.net/)
- [Triumph GUI developer docs](https://triumphteam.dev/docs/triumph-gui/introduction)
- [EvenMoreFish — Modrinth](https://modrinth.com/plugin/evenmorefish)
- [EvenMoreFish documentation](https://evenmorefish.github.io/EvenMoreFish/)
- [GSit — Modrinth](https://modrinth.com/plugin/gsit)
- [HuskHomes — Modrinth](https://modrinth.com/plugin/huskhomes)
- [squaremap — Modrinth](https://modrinth.com/project/squaremap)
- [BlueMap GitHub](https://github.com/BlueMap-Minecraft/BlueMap)
- [WorldGuard — Modrinth](https://modrinth.com/plugin/worldguard)
- [CoreProtect version list](https://hangar.papermc.io/CORE/CoreProtect/versions)

### Contoh sistem dari server lain
- [Hypixel SkyBlock Collections](https://hypixelskyblock.minecraft.wiki/w/Collections)
- [Wynncraft Professions](https://wynncraft.wiki.gg/wiki/Professions)
- [Minecraft GUI design collection — Behance](https://www.behance.net/gallery/162872991/Minecraft-GUIs-Collection)

---

## Rekomendasi final

**Jangan mengejar jumlah plugin atau monster dahulu.** V8 paling berharga jika mampu menyelesaikan autentikasi, reward yang crash-safe, transaksi ekonomi, backup yang bisa dipulihkan, dan batas dunia/claim. Setelah itu, bangun GUI dan resource pack yang konsisten di kedua platform, baru tambahkan satu konten baru yang mendukung profesi yang sudah ada.

Urutan yang paling saya sarankan: **V8-A stabilitas → V8-B GUI/pack → V8-C fishing/collections → V8-D fauna dan visual monster**. Setiap tahap harus mempunyai acceptance test yang dapat diulang, sehingga report berikutnya berisi bukti yang lebih kuat daripada sekadar log startup dan klaim bahwa server “sudah siap produksi”.
