# Kala Nusantara — V9 Plugin Ecosystem & Experience Brainstorm Report

**Tanggal riset:** 25 Oktober 2026  
**Repo yang ditinjau:** [xDzaky/kala-nusantara](https://github.com/xDzaky/kala-nusantara)  
**Commit `main` yang terlihat saat riset:** [`74fc493b9ca4d3d72ef8517774194221f6deeccd`](https://github.com/xDzaky/kala-nusantara/commit/74fc493b9ca4d3d72ef8517774194221f6deeccd)  
**Tujuan:** mencari plugin, tooling resource pack, ide GUI, sistem gameplay, dan inspirasi dari server lain yang benar-benar sesuai dengan RPG survival kepulauan Nusantara, Paper, Geyser/Floodgate, Java + Bedrock/MCPE.

> **Batas verifikasi:** saya memeriksa file yang terlihat pada branch `main`, metadata commit, file kode/config terpilih, dan log CI GitHub Actions yang tersedia secara publik. Saya tidak menjalankan instance Paper atau klien Java/Bedrock dari sini. Klaim pengujian server pada report implementasi V8 diperlakukan sebagai hasil yang dilaporkan, bukan sebagai pengujian ulang mandiri. Status versi plugin dapat berubah; sebelum memasang, unduh hanya rilis resmi yang secara eksplisit cocok dengan build server aktual dan uji di staging.

---

## 1. Ringkasan keputusan

**Rekomendasi utama: jangan menambah banyak plugin gameplay sekaligus.** KalaCore sudah punya pondasi RPG yang cukup luas: profesi, ekonomi Rupiah Kala, kontrak kampung, katalog penemuan, jurnal, kosmetik, GUI Kompas, event Gunung Murka, event musiman, dan sistem kontribusi. Nilai V9 paling besar datang dari memperkuat sistem yang telah ada, menuntaskan UI/resource-pack lintas platform, lalu menguji hanya satu plugin baru per tahap.

Urutan prioritas yang direkomendasikan:

1. **P0 — bereskan CI dan tes integrasi.** CI pada commit terbaru gagal karena kompilasi `KalaVaultEconomyProvider.java` tidak menemukan Vault Economy API. Jangan anggap build V8 sudah tervalidasi oleh automation selama run itu masih merah.
2. **P0 — uji keandalan reward dan kontrak.** Saya menemukan pola dalam kode kontrak, penemuan, dan rekonsiliasi payout yang perlu dites sebelum ekonomi diperluas.
3. **P1 — selesaikan resource pack Java + Bedrock.** Direktori pack sudah ada, tetapi snapshot repo hanya memperlihatkan kerangka manifest dan mapping; hash distribusi masih `pending_distribution_build`. Mapping Geyser yang ada masih memakai format v1 lama, sedangkan dokumentasi Geyser sekarang menyebut format v1 deprecated dan tidak diperbarui.
4. **P1 — prototipe EvenMoreFish** sebagai tambahan konten pertama, karena cocok untuk profesi Nelayan tanpa harus mengganti seluruh sistem profesi.
5. **P1 — pertimbangkan CoreProtect** untuk riwayat blok dan rollback, dengan pengujian resmi Paper 26.3 dan konfigurasi agar tidak bertabrakan dengan GriefPrevention.
6. **P1/P2 — pilih satu peta web:** BlueMap untuk peta pulau 3D sebagai showcase, atau squaremap untuk peta 2D yang lebih ringan.
7. **P2 — poles GUI dan wayfinding.** Uji satu dari zMenu atau CommandPanels bila MenuManager internal tidak cukup. Jangan instal keduanya bersamaan hanya demi tampilan.
8. **P2 — visual 3D fauna/monster setelah parity Bedrock terbukti.** API custom entity Geyser masih eksperimental. Satu prototipe lebih baik daripada langsung memasang beberapa sistem model dan mob.

---

## 2. Kondisi repo terbaru yang terlihat

### 2.1 Hal yang sudah berkembang dengan baik

Pada commit `74fc493`, README dan struktur source menyebut:

- `KalaCore` sebagai plugin inti; API target di `build.gradle` masih dideklarasikan sebagai Paper `1.21.4-R0.1-SNAPSHOT`, Java release 21.
- Ekonomi Rupiah Kala dan implementasi provider Vault.
- Profesi, jurnal, kosmetik, Kompas, katalog penemuan, kontrak kampung, encounter Gunung Murka, dan event musiman.
- `server.properties` sekarang menampilkan `online-mode=true`; konfigurasi Geyser memakai `auth-type: floodgate`.
- Worldgen di `BetterStructures/ValidWorlds.yml` mematikan `kala_main` dan hanya mengizinkan world resource/staging yang ditetapkan.
- `.github/workflows/ci.yml` ada.
- Struktur `resource-packs/java`, `resource-packs/bedrock`, dan `resource-packs/mappings/geyser` sudah disiapkan.

Ini fondasi yang tepat untuk proyek yang mengutamakan Java + Bedrock tanpa mewajibkan pemain memasang Forge/Fabric.

### 2.2 Temuan penting yang sebaiknya dikerjakan sebelum memperluas plugin

| Prioritas | Temuan yang dapat dilacak | Dampak | Tindakan |
|---|---|---|---|
| **P0** | [GitHub Actions run terbaru](https://github.com/xDzaky/kala-nusantara/actions/runs/40115755679) gagal pada kompilasi `KalaVaultEconomyProvider.java`: `net.milkbowl.vault.economy.Economy` dan `EconomyResponse` tidak ditemukan. `build.gradle` mengandalkan `files('../../server/plugins/VaultUnlocked-2.20.2.jar')`, yaitu file lokal yang tidak terlihat dalam isi repo. | Automation belum membuktikan code V8 dapat dibangun pada runner bersih. | Ganti dependency API lokal dengan artefak API yang diambil dari repository Maven resmi/terpercaya yang sesuai, atau prosedur penyediaan dependency yang dapat direproduksi. Jangan memasukkan JAR runtime yang lisensi/distribusinya tidak mengizinkan. Jalankan ulang CI dan catat hasil. |
| **P0/P1** | [VillageContractManager.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/contract/VillageContractManager.java) menetapkan `WEEK_2026_W43` secara hardcoded, dan `initialize()` hanya memanggil `setupWeeklyContracts()`. Progress/completion juga disimpan di cache memori sebelum update DB async; completion langsung membayar. | Minggu tidak otomatis berotasi; jika progress/completion tidak dimuat kembali saat restart, kontrak dapat terulang dan dibayar ulang. | Buat week ID dinamis dari tanggal/ISO week; load progress dari DB saat player join/startup; idempotency key unik `(uuid, week_id, contract_id)`; transaksi reward dan tes restart/duplikasi. |
| **P0/P1** | [NusantaraDiscoveryManager.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/discovery/NusantaraDiscoveryManager.java) menambah discovery ke cache sebelum query async selesai. Reward dijalankan jika `player.isOnline()` ketika callback kembali. | Bila query gagal, cache dapat menandai penemuan tetap selesai; bila pemain disconnect sebelum reward, discovery tercatat tapi imbalan bisa hilang. | Jadikan DB sumber kebenaran; rollback cache saat insert gagal; gunakan reward/outbox ber-ID unik dan entitlement EXP yang bisa dikirim saat pemain login. |
| **P0/P1** | [EventContributionLedger.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java) saat merekonsiliasi `PENDING` memanggil `deposit()` lebih dahulu, lalu baru mengubah status menjadi `PAID`. Parameter `exp` diteruskan, tetapi tidak terlihat diterapkan pada jalur ini. | Crash setelah deposit dan sebelum status `PAID` disimpan dapat membuat deposit berulang pada boot berikutnya; EXP dapat tidak dipulihkan. | Jangan menjanjikan exactly-once hanya dengan status `PENDING/PAID`. Gunakan transaction ID yang idempotent dan tulis perubahan saldo + status reward dalam transaksi SQLite yang sama, atau transactional outbox yang benar-benar retry-safe. Simpan EXP sebagai entitlement tahan restart. |
| **P1** | [BackupManager.java](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/backup/BackupManager.java) membuat ZIP berisi DB, beberapa file config, dan manifest; tidak mencakup world region files atau keseluruhan konfigurasi/plugin/resource pack. `verifyBackup()` hanya mengecek keberadaan DB dan manifest, tidak menghitung ulang hash untuk mencocokkannya dengan manifest. | Nama “Full Server Backup” melebih-lebihkan cakupan. Archive kecil bukan bukti pemulihan world/server penuh. | Ganti nama menjadi core-state backup atau implementasikan backup world/server secara eksplisit dengan strategi konsisten, hash per file, retensi, salinan off-host, dan restore drill ke staging. |
| **P1** | [Mapping Geyser](https://github.com/xDzaky/kala-nusantara/blob/main/resource-packs/mappings/geyser/custom_mappings.json) menyebut `format_version: "1"` dan bentuk `bedrock_id`/`bedrock_data`; dokumentasi Geyser terkini menyatakan custom item format v1 deprecated dan menggunakan format v2. | Custom item bisa gagal dimuat/terpetakan pada Geyser baru. | Migrasikan ke format mapping v2 sesuai contoh resmi Geyser, cocokkan Java item model/component yang sebenarnya, dan validasi dengan versi Geyser target. |
| **P1** | [Resource pack Java](https://github.com/xDzaky/kala-nusantara/tree/main/resource-packs/java) yang terlihat hanya mempunyai `pack.mcmeta`; [Bedrock pack](https://github.com/xDzaky/kala-nusantara/tree/main/resource-packs/bedrock) baru memperlihatkan `manifest.json`; `manifest.yml` menyatakan hash pack masih `pending_distribution_build`. | Pipeline masih berupa kerangka, belum membuktikan tekstur item yang dipakai di klien. | Tambahkan aset nyata (`assets/.../models`, tekstur Java; `textures/item_texture.json`, tekstur/attachable Bedrock), build zip/`.mcpack`, SHA-256, hosting, dan tes Java serta Bedrock. |
| **P1** | README menyebut Paper 1.21.4/26.3, sementara `build.gradle` compile terhadap `paper-api:1.21.4-R0.1-SNAPSHOT`; `pack.mcmeta` memakai `pack_format: 46`. | Target API/pack dapat ketinggalan dari runtime yang diklaim. | Tetapkan satu matrix resmi: build Paper exact, versi Java runtime/toolchain, API compile, versi Geyser dan pack format. Build/test dengan target yang memang dijalankan, bukan sekadar mengasumsikan API lawas kompatibel. |

**Catatan penting:** hasil di atas berasal dari pembacaan kode/repo dan log CI, bukan reproduksi gameplay secara live. Temuan tersebut cukup untuk menjadi tiket verifikasi, tetapi penerimaan akhir tetap perlu tes pada server staging.

---

## 3. Shortlist plugin yang paling relevan

Peringkat ini menggabungkan kesesuaian tema, tambahan pengalaman, kompatibilitas yang ditampilkan pada halaman proyek saat riset, tumpang tindih dengan KalaCore, dan risiko Java–Bedrock. “Layak dicoba” tetap berarti **uji di staging dulu**, bukan otomatis aman untuk production.

| Kandidat | Peran | Keputusan | Kenapa cocok / batasan |
|---|---|---|---|
| [**CoreProtect**](https://coreprotect.net/latest/) | Logging blok, lookup, rollback/restore | **Prioritas tinggi** setelah CI | Melengkapi proteksi klaim GriefPrevention dengan histori perubahan untuk pemulihan pulau/bangunan. Halaman Modrinth menyebut hingga 26.2 tetapi menyediakan link resmi download 1.14–26.3; ambil rilis exact dari kanal resmi dan tes. Hindari memasang dua plugin logging yang sama tanpa kebutuhan. |
| [**EvenMoreFish**](https://modrinth.com/plugin/evenmorefish) | Ikan custom, bait, journal, kompetisi, fish shop | **Gameplay POC #1** | Versi halaman saat riset menampilkan Paper 26.3 dan MIT. Sangat selaras dengan profesi Nelayan dan tema pesisir. Nonaktifkan/kurangi reward uang bawaan; integrasikan event, jurnal, progress kontrak dan ekonomi ke sistem KalaCore agar tidak punya dua sumber saldo. Mulai dari ikan/rarity dan jurnal, baru kompetisi. |
| [**BlueMap**](https://modrinth.com/plugin/bluemap) **atau** [**squaremap**](https://modrinth.com/plugin/squaremap) | Peta web 3D atau 2D | **Pilih satu** sesuai tujuan | BlueMap membuat peta dunia 3D yang bagus untuk menampilkan gugusan pulau dan landmark. squaremap menyajikan peta 2D top-down, lebih langsung untuk navigasi dan punya fokus ringan. Keduanya butuh evaluasi disk/CPU dan pengaturan privasi. Jangan tampilkan lokasi rumah/player secara publik tanpa pertimbangan. |
| [**zMenu**](https://modrinth.com/plugin/zmenu) **atau** [**CommandPanels**](https://modrinth.com/plugin/commandpanels) | GUI konfigurasi, paginasi, aksi menu; CommandPanels juga menawarkan dialog/Form Bedrock | **POC jika GUI internal menghambat** | Keduanya menampilkan dukungan 26.3 dan opsi GUI Bedrock pada listing yang ditinjau. KalaCore sudah punya `MenuManager`/`/kompas`; jangan pindahkan semua menu sekaligus. Buat satu menu tiruan di staging lalu bandingkan waktu authoring, navigasi HP, fallback vanilla, dan kemungkinan duplikasi aksi. Jangan memasang keduanya. |
| [**FancyHolograms**](https://modrinth.com/plugin/fancyholograms) | Penunjuk teks/item/block yang melayang | **Opsional, risiko rendah** | Berguna untuk petunjuk dermaga, papan ekspedisi dan marker landmark. Halaman saat riset mencantumkan Paper/Folia 26.3. Jangan mengganti worldbuilding dengan ratusan hologram; uji jarak tampil, performa entitas, dan kompatibilitas Bedrock. |
| [**GSit**](https://modrinth.com/plugin/gsit) | Duduk, rebahan, merangkak | **Opsional sosial** | Mendukung 26.3 pada listing saat riset; tidak membutuhkan pack khusus. Bermanfaat di pelabuhan, rumah singgah, ruang komunitas, foto event. Dampak gameplay kecil sehingga prioritas setelah konten inti. |
| [**SimpleVoice-Geyser**](https://modrinth.com/plugin/simplevoice-geyser) | Bridge voice chat Java–Bedrock melalui web interface | **Eksperimen nanti** | Menambah imersi ekspedisi kooperatif, tetapi Bedrock mengandalkan UI/web interface dan hasil listing yang dilihat belum secara eksplisit menyebut 26.3 untuk versi server. Perlu domain/layanan, izin mikrofon, moderasi, privasi suara, dan fallback chat. Jangan jadikan syarat untuk ikut event. |
| [**NotQuests**](https://modrinth.com/plugin/notquests) | Quest bercabang, dialog, reputasi, kondisi, GUI | **Jangan ditambahkan bersamaan sekarang** | Fitur kuat dan listing menampilkan 26.3, tapi overlap besar dengan quest/tutor, VillageContractManager, jurnal, dan Discovery yang sudah ada. Layak dipertimbangkan hanya jika keputusan arsitektur adalah memigrasi sistem quest KalaCore ke satu plugin eksternal, bukan menambahkan sistem quest kedua. |
| [**WorldGuard**](https://modrinth.com/plugin/worldguard) | Zona dengan flag admin | **Hanya jika ada kebutuhan spesifik** | GriefPrevention sudah digunakan. Jangan menambah proteksi kedua sebelum pemilik setiap zona jelas. Bisa dipakai untuk arena event kurasi terpisah, asalkan tes precedence/flag tidak membingungkan pemain. |

Referensi kompatibilitas: [EvenMoreFish](https://modrinth.com/plugin/evenmorefish), [CoreProtect](https://modrinth.com/plugin/coreprotect), [BlueMap](https://modrinth.com/plugin/bluemap), [squaremap](https://modrinth.com/plugin/squaremap), [zMenu](https://modrinth.com/plugin/zmenu), [CommandPanels](https://modrinth.com/plugin/commandpanels), [FancyHolograms](https://modrinth.com/plugin/fancyholograms), [GSit](https://modrinth.com/plugin/gsit), [NotQuests](https://modrinth.com/plugin/notquests). Halaman tersebut dinamis; jangan mengandalkan label kompatibilitas tanpa menguji artifact yang akan diunduh.

### Plugin/pendekatan yang tidak saya sarankan dipasang sekarang

- **ItemsAdder sebagai solusi custom-content utama:** FAQ resmi ItemsAdder saat riset menyatakan Geyser tidak didukung secara resmi dan tidak menyarankan memakai Geyser untuk setup itu. Lihat [FAQ Geyser ItemsAdder](https://wiki.itemsadder.com/faq/geyser/). Ini bertentangan dengan prioritas parity Bedrock.
- **Oraxen + ItemsAdder + Nexo sekaligus:** semua memperbesar kompleksitas pack, item registry, resource generation, dan troubleshooting. Pilih paling banyak satu sistem jika pipeline manual nantinya tidak cukup.
- **MythicMobs + ModelEngine + BetterModel sekaligus:** risiko visual/packet dan dependensi tinggi; versi exact harus diverifikasi. Mulai dari entity vanilla dengan nama, atribut, efek, dan satu pack yang mendukung dua platform.
- **Plugin skill/profession/quests/economy baru:** bertabrakan dengan sistem KalaCore yang ada. Jangan menambahkan economy atau reward currency kedua.
- **Shopkeepers/shop plugins sekarang:** pasar pemain dan profesi Pedagang baru bernilai setelah API ekonomi dan reward idempotent selesai. Jika nanti dipasang, gunakan satu market/economy integration yang menulis ke Rupiah Kala; jangan memakai currency mandiri plugin.
- **Plugin/pack yang baru muncul tanpa riwayat atau belum diuji:** angka download atau thumbnail keren bukan bukti stabilitas maupun dukungan Bedrock. Hindari menjadikan plugin yang sangat baru sebagai dependensi utama production.

---

## 4. Tooling untuk GUI, tekstur, item dan model

### 4.1. Jangan campurkan tiga pekerjaan berbeda

Bagi proses visual menjadi tiga lapisan:

1. **Interaksi/UI:** inventory GUI, Bedrock Forms, tombol, navigasi, pesan dan state.
2. **Aset:** ikon, tekstur item, font, model, suara, palet warna.
3. **Transport lintas platform:** pack Java, pack Bedrock, custom item mappings, distribusi/hash, dan pengujian unduh.

Plugin GUI tidak otomatis membuat tekstur. Resource pack Java juga tidak otomatis menjadi resource pack Bedrock. Dokumentasi Geyser secara eksplisit menyebut bahwa Geyser tidak mengonversi Java resource pack dan tidak membuat custom item mappings otomatis; pack Bedrock perlu dipasok dan mapping format v2 harus digunakan untuk custom item modern. [Geyser Custom Items](https://geysermc.org/wiki/geyser/custom-items/), [Geyser Packs](https://geysermc.org/wiki/geyser/packs/).

### 4.2. Rekomendasi pipeline resource pack

**Gunakan repo `resource-packs/` sebagai sumber aset, lalu build artefak yang bisa dipasang — bukan sekadar menyimpan template.**

Langkah yang saya sarankan:

1. Tetapkan satu item percobaan, misalnya **Kompas Pengembara**. Pilih item vanilla dasar, model/identifier Java, mapping Geyser v2, tekstur Bedrock, nama/lore fallback, dan ID PDC yang dipakai KalaCore.
2. Migrasikan `custom_mappings.json` dari format v1 ke struktur v2 berdasarkan dokumentasi Geyser saat ini. Pastikan definisi mapping sebenarnya merujuk pada item Java + model yang benar, bukan sekadar identifier yang diciptakan sendiri.
3. Buat texture assets nyata: model dan tekstur Java; `textures/item_texture.json` dan file tekstur terkait di Bedrock. Nama yang digunakan di `icon`/identifier harus cocok dengan struktur file pack.
4. Gunakan **[GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow)** sebagai tooling dev untuk membantu menghasilkan mapping, locale overrides dan Bedrock pack dari resource pack yang didukung. Rainbow adalah mod Fabric sisi developer, bukan plugin wajib di server dan bukan sesuatu yang perlu dipasang oleh semua pemain. Proyek ini juga menyebut dirinya experimental, jadi periksa output manual.
5. Buat skrip build yang menghasilkan ZIP Java dan `.mcpack` Bedrock, manifest/checksum SHA-256 serta laporan aset yang hilang. Jangan menulis hash sampai file distribusi final benar-benar dihasilkan.
6. Host file dengan URL HTTPS stabil, konfigurasikan distribusi untuk Bedrock melalui Geyser, dan uji join dengan pack diterima/ditolak/gagal diunduh.
7. Setelah pack terbukti stabil, pertimbangkan `require-resource-pack=true` hanya jika keputusan desain mengharuskannya. Jangan menjadikannya syarat untuk objective utama selama fallback belum diuji.

**Catatan versi:** `pack.mcmeta` yang sekarang terlihat memakai `pack_format: 46` dan artefak mapping berbentuk v1. Revisi keduanya berdasarkan target server/klien yang benar. Jangan menebak `pack_format` dari versi plugin; cek spesifikasi resource pack untuk versi Minecraft yang sedang digunakan.

### 4.3. GUI visual: prinsip desain untuk server mobile-first

Gunakan arahan visual yang jelas, tidak seperti GUI hasil template generik:

- **Palet:** abu arang/batu vulkanik, krem/perkamen, kayu gelap, teal laut, aksen tembaga/emas. Merah hanya untuk bahaya/peringatan.
- **Bahasa ikon:** satu gaya dan ukuran konsisten. Gunakan ikon vanilla terlebih dahulu, lalu custom texture bila asset pipeline matang. Hindari banyak warna neon dan glyph yang hanya terlihat benar dengan font resource pack.
- **Kompas utama:** 27 slot, enam atau tujuh pilihan paling penting: Misi, Peta, Kampung, Progres, Koleksi, Event, Panduan/Pengaturan. Tab boleh mengikuti config saat ini, namun nama dan navigasinya harus sama jelas di Java dan Bedrock.
- **Layar detail:** 45/54 slot bila benar-benar perlu, tetapi jangan isi setiap slot. Pisahkan informasi jadi panel kategori/halaman.
- **Interaksi mobile:** klik biasa, tombol back/close yang konsisten, nama yang menjelaskan aksi, tidak mewajibkan shift-click/drag, konfirmasi untuk aksi yang menghabiskan item atau uang.
- **Fallback:** judul dan lore teks harus cukup menjelaskan aksi ketika pack tidak aktif. Jangan jadikan ikon custom sebagai satu-satunya informasi.

Tentukan ownership UI supaya elemen tidak tumpang tindih:

| Elemen | Gunakan untuk |
|---|---|
| Scoreboard | Status singkat yang tetap relevan: saldo ringkas, musim/event dan progres yang dipilih pemain. |
| BossBar | Progres fase event yang berjalan saat ini. |
| Title/subtitle | Awal/akhir event atau perubahan fase besar, bukan notifikasi kecil berulang. |
| Actionbar | Status singkat, cooldown, objective aktif. |
| Chat | Hadiah, instruksi dan pengumuman yang perlu dibaca kembali di chat. |
| GUI/Jurnal | Detail panjang, histori, koleksi, pilihan dan tooltip. |
| Hologram/marker dunia | Petunjuk yang sesuai lokasi, bukan salinan scoreboard/chat. |

### 4.4. Tool/model 3D dan custom mobs

- **BetterModel** adalah engine render/animasi model Blockbench untuk Java Edition berbasis display entities. Itu belum otomatis menjamin representasi yang sama pada Bedrock. Lihat [BetterModel](https://modrinth.com/plugin/bettermodel) dan [Geyser Custom Entities](https://geysermc.org/wiki/geyser/custom-entities/).
- **Geyser Custom Entity API masih experimental** dan detail mapping model/entity dapat berubah; jangan jadikan custom entity eksperimental sebagai kebutuhan untuk menyelesaikan quest/event.
- Pilih satu prototipe yang tidak kritis, misalnya satu fauna kecil atau maskot Pelabuhan. Lakukan pengujian model/animasi di Java dan Bedrock Android/PC, dengan fallback vanilla jika asset gagal.
- Untuk monster Gunung Murka, lebih aman mulai dari mob vanilla dengan nama, atribut, AI, particle, suara dan telegraph yang terukur. Tambahkan custom model hanya setelah performa dan parity visual terbukti.

---

## 5. Mekanisme dari server besar yang bisa diadaptasi

### 5.1. Terinspirasi dari Hypixel SkyBlock Collections

Hypixel mengatur collection sebagai milestone pengumpulan resource dengan reward seperti recipe, trade, skill experience atau progresi lainnya; item yang dibeli/ditukar tidak selalu memajukan koleksi. [Hypixel Collections](https://hypixelskyblock.minecraft.wiki/w/Collections).

Adaptasi untuk Kala Nusantara:

- **Atlas kepulauan:** penemuan fauna, mineral/sampel geologi, landmark, ikan dan artefak lore.
- **Milestone bertingkat:** 1, 3, 6, 12, dst. untuk landmark/observasi; angka harus ditentukan setelah playtest, bukan menyalin angka Hypixel.
- **Reward selain uang:** gelar Penjelajah, halaman jurnal/lore, kosmetik, recipe field kit, marker peta, dekorasi kampung, atau diskon layanan tertentu. Ini mengurangi tekanan inflasi Rupiah Kala.
- **Anti-exploit:** hanya progress dari aktivitas yang memenuhi aturan (misalnya menemukan atau menangkap sendiri), bukan hanya membeli ikan lalu menukar ulang. Pastikan progress tercatat secara durable.
- **Koleksi komunitas:** milestone bersama untuk semua pemain, contohnya “500 sampel abu terkumpul”, yang membuka dekorasi dermaga atau memperbaiki status kampung secara visual tanpa menghancurkan dunia.

Discovery manager V8 sudah memberikan titik awal; yang paling penting sekarang adalah pemulihan data/reward, katalog yang terlihat di GUI dan integrasi progres yang nyata.

### 5.2. Terinspirasi dari Wynncraft Professions

Wynncraft menghubungkan gathering dan crafting: material dari beberapa profesi digunakan oleh profesi crafting lain. [Wynncraft Professions](https://wynncraft.wiki.gg/wiki/Professions).

Adaptasi versi ringan:

- Nelayan mengumpulkan hasil laut; Petani menghasilkan bahan makanan; Tukang Kayu menyuplai kayu/komponen pelabuhan; Penambang menyediakan mineral/sampel; Pengrajin menggabungkan material menjadi item utilitas/kosmetik.
- Kontrak kampung meminta kontribusi berbagai profesi dan menunjukkan kebutuhan bersama. Jangan membuat satu profesi selalu paling menguntungkan.
- Gunakan 2–3 resep/produk antar-profesi dulu, bukan tree crafting masif ala MMORPG.
- Reward utama berupa akses, kosmetik, resep, atau progres koleksi; Rupiah tetap sebagai reward terbatas yang dipantau.

### 5.3. Loop permainan yang cocok dengan lore Nusantara

**Loop harian:** Mbah Kala/papan ekspedisi memberi satu misi lokal → pemain memilih jalur (pesisir, hutan, lereng vulkanik) → menemukan landmark/ikan/fauna/sampel → mencatatnya di Jurnal → mendapat milestone/reward.

**Loop mingguan:** Kontrak kampung lintas profesi → kontribusi individu + target kolektif → komunitas mendapat unlock dekorasi/cerita/bonus non-destruktif.

**Loop musiman:** Badai Selat, Abu dan Bara, atau event baru seperti Rob Labuhan/Kabut Hutan punya telegraph, objective yang dapat diakses dari HP, role berbeda, dan recovery/cleanup otomatis.

**Item utilitas yang punya fungsi jelas:**
- Kompas Pengembara: membuka menu Kompas dan menunjukkan arah/tujuan aktif.
- Kit Survei: memulai pencatatan fauna/geologi/landmark.
- Lampu Suar Pesisir: mengaktifkan sinyal pada titik pesisir yang telah ditetapkan.
- Kerudung Abu: cosmetic/utility dekat event kawah bila efek dan batasnya jelas.
- Peta Jalur Laut: membuka informasi rute antarpulau yang pernah ditemukan.

Masing-masing item harus punya ID PDC, syarat penggunaan, cooldown, anti-dupe, dan fallback vanilla. Jangan menambah item hanya untuk mengisi katalog tekstur.

---

## 6. Prioritas implementasi V9

### V9-A — Validasi dasar dan integritas data (P0)

- [ ] Perbaiki dependency Vault API agar CI bisa build dari checkout bersih tanpa JAR lokal yang tidak ada.
- [ ] Jalankan build/test dan simpan log sukses yang dapat diverifikasi; jangan tandai report “CI verified” sebelum lulus.
- [ ] Cocokkan Paper API, runtime Paper, Java toolchain, resource-pack format dan Geyser versi aktual.
- [ ] Bikin tes kontrak mingguan: restart, minggu baru, dua klaim serentak, DB gagal, disconnect saat hadiah.
- [ ] Bikin tes discovery: insert gagal, duplicate discovery, disconnect sebelum hadiah, reconnect dan payout tertunda.
- [ ] Bikin crash-injection untuk payout event sebelum/sesudah deposit dan update status. Gunakan id transaksi unik yang idempotent.
- [ ] Ubah “Full Backup” menjadi backup yang namanya sesuai cakupan atau implementasikan arsip world/runtime secara konsisten; cek hash dan restore ke server staging.
- [ ] Uji `CoreProtect` rilis resmi untuk versi server yang tepat bersama GriefPrevention; tetapkan retention dan jadwal query/rollback.

**Definition of Done:** CI hijau; tidak ada reward dobel/hilang di skenario restart dan disconnect; kontrak tidak bisa diulang lewat restart; backup dapat dipulihkan ke instance staging dan hash diverifikasi.

### V9-B — Resource pack dan UI crossplay (P1)

- [ ] Migrasikan mapping Geyser ke format v2 resmi.
- [ ] Build satu item dengan aset nyata di Java dan Bedrock.
- [ ] Build hash SHA-256 final dan hosting HTTPS; wiring ke Geyser diverifikasi dari log boot.
- [ ] Uji pack diterima, ditolak, gagal, dan versi pack lama di-cache.
- [ ] Desain ulang `/kompas` dan Jurnal secara visual, tetapi pertahankan fallback vanilla.
- [ ] Uji `zMenu` **atau** `CommandPanels` untuk satu halaman; pertahankan MenuManager jika plugin eksternal tidak memberikan peningkatan yang terukur.
- [ ] Uji di Java Desktop + Bedrock HP: ukuran tombol, teks, close/back, scroll/pagination, respons pada tap, dan pesan error.

**Definition of Done:** tidak ada fungsi utama yang terkunci di belakang custom pack; item utama tampil/terbaca di Java dan Bedrock; mapping dan manifest build konsisten.

### V9-C — Gameplay baru: Nelayan + Discovery (P1/P2)

- [ ] POC EvenMoreFish pada staging untuk 1–2 minggu; aktifkan ikan khusus dan journal lebih dulu.
- [ ] Nonaktifkan reward/market uang yang tidak terhubung ke ekonomi Kala; jangan punya saldo kedua.
- [ ] Kaitkan aktivitas memancing dengan kontrak mingguan/Discovery secara idempotent.
- [ ] Tambahkan milestone koleksi dan reward non-moneter, plus satu leaderboard personal opsional.
- [ ] Jalankan tes ekonomi inflasi dan exploit: ikan berpindah antar-pemain, dibeli dari shop, ditaruh di container, diambil ulang, atau di-relog.

**Definition of Done:** aktivitas Nelayan menambah loop gameplay yang jelas, tidak memberi reward ganda, tidak mendominasi ekonomi, dan bisa dimainkan Bedrock tanpa custom control rumit.

### V9-D — Navigasi dan sosial (P1/P2)

- [ ] Pilih BlueMap (3D showcase) atau squaremap (2D navigation) setelah benchmark.
- [ ] Map hanya menampilkan dunia yang memang aman untuk publik; matikan marker posisi pemain/rumah secara default.
- [ ] Tambahkan landmark/marker bertema Nusantara, pelabuhan, stasiun penelitian dan area event.
- [ ] Uji GSit bila komunitas mau aktivitas sosial dan FancyHolograms hanya jika penunjuk tambahan memang dibutuhkan.

### V9-E — Fauna, model dan ambience (P2)

- [ ] Satu prototipe model fauna / mob saja.
- [ ] Java–Bedrock parity test dan fallback vanilla.
- [ ] Profil spark saat event; jangan mengirim efek/entitas per tick ke semua pemain tanpa budget.
- [ ] Voice bridge hanya eksperimen opsional, bukan syarat gameplay.

---

## 7. Acceptance test yang perlu masuk ke report berikutnya

### Build dan dependensi
- CI dapat clone repo baru dan membangun JAR tanpa artefak rahasia/lokal.
- Version matrix mencatat commit, Paper build, Java runtime, Geyser/Floodgate build, dan artefak plugin yang benar-benar diuji.
- Dependency versions dipin; URL unduhan dan lisensi dicatat.

### Reward/data
- Dua permintaan bersamaan untuk discovery/contract/event menghasilkan satu reward.
- Crash di antara perubahan saldo/status tidak menduplikasi uang.
- Pemain offline tetap menerima entitlement yang belum diserahkan saat login berikutnya.
- Kegagalan DB tidak membuat cache menganggap progress sudah tersimpan.
- Minggu baru menghasilkan contract ID baru tanpa menghapus histori lama.

### Resource pack dan UI
- Geyser memuat mapping v2 tanpa warning/error.
- Pack Java benar sesuai target client dan Pack Bedrock benar sesuai target Bedrock.
- Semua custom item punya vanilla fallback yang tetap bisa menjalankan fitur.
- Java + Bedrock HP dapat membuka Kompas, kembali, menutup menu, membaca objective, dan submit interaksi tanpa shift-click/drag.
- Tes resource pack diterima/ditolak, download gagal, cache versi lama, reconnect.

### Backup, proteksi, performa
- Archive dicek dengan verifikasi hash, dibuka, dan direstore ke instance staging baru.
- World save yang benar-benar perlu dipulihkan tercakup; hasil restore tidak mengubah `kala_main` production.
- Uji CoreProtect rollback di dunia uji dan area GriefPrevention.
- Rekam TPS/MSPT, CPU, heap, jumlah entity dan durasi join/pack saat 1, 5, 10, 20 pemain atau jumlah realistis yang tersedia.
- Map web tidak mengekspos marker privat dan tidak membuat beban CPU/disk yang tidak wajar.

---

## 8. Rujukan utama yang ditelusuri

### Repo Kala Nusantara
- [Repo utama](https://github.com/xDzaky/kala-nusantara)
- [Commit implementasi V8](https://github.com/xDzaky/kala-nusantara/commit/74fc493b9ca4d3d72ef8517774194221f6deeccd)
- [GitHub Actions run terbaru — gagal](https://github.com/xDzaky/kala-nusantara/actions/runs/40115755679)
- [README](https://github.com/xDzaky/kala-nusantara/blob/main/README.md)
- [KalaCore build.gradle](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/build.gradle)
- [EventContributionLedger](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/event/EventContributionLedger.java)
- [VillageContractManager](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/contract/VillageContractManager.java)
- [NusantaraDiscoveryManager](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/discovery/NusantaraDiscoveryManager.java)
- [BackupManager](https://github.com/xDzaky/kala-nusantara/blob/main/plugin/kala-core/src/main/java/id/kalanusantara/core/backup/BackupManager.java)
- [Resource packs root](https://github.com/xDzaky/kala-nusantara/tree/main/resource-packs)
- [Geyser item mapping in repo](https://github.com/xDzaky/kala-nusantara/blob/main/resource-packs/mappings/geyser/custom_mappings.json)

### Resource pack / Geyser
- [Geyser Custom Items](https://geysermc.org/wiki/geyser/custom-items/)
- [Geyser Resource Packs](https://geysermc.org/wiki/geyser/packs/)
- [Geyser Custom Entities](https://geysermc.org/wiki/geyser/custom-entities/)
- [GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow)
- [ItemsAdder — Geyser FAQ](https://wiki.itemsadder.com/faq/geyser/)

### Plugin ecosystem
- [CoreProtect official download](https://coreprotect.net/latest/)
- [CoreProtect on Modrinth](https://modrinth.com/plugin/coreprotect)
- [EvenMoreFish](https://modrinth.com/plugin/evenmorefish)
- [BlueMap](https://modrinth.com/plugin/bluemap)
- [squaremap](https://modrinth.com/plugin/squaremap)
- [zMenu](https://modrinth.com/plugin/zmenu)
- [CommandPanels](https://modrinth.com/plugin/commandpanels)
- [FancyHolograms](https://modrinth.com/plugin/fancyholograms)
- [GSit](https://modrinth.com/plugin/gsit)
- [NotQuests](https://modrinth.com/plugin/notquests)
- [BetterModel](https://modrinth.com/plugin/bettermodel)
- [SimpleVoice-Geyser](https://modrinth.com/plugin/simplevoice-geyser)

### Mekanisme / inspirasi
- [Hypixel SkyBlock Collections](https://hypixelskyblock.minecraft.wiki/w/Collections)
- [Wynncraft Professions](https://wynncraft.wiki.gg/wiki/Professions)
- [Contoh tutorial GUI di YouTube](https://www.youtube.com/watch?v=vgyjZjOl0tY) — gunakan hanya untuk inspirasi; video lama bisa memakai plugin/resource-pack workflow yang sudah berubah.

---

## Kesimpulan akhir

Untuk Kala Nusantara, plugin yang paling layak dibawa ke tahap staging berikutnya adalah **CoreProtect** untuk operasional/pemulihan dan **EvenMoreFish** untuk memperkaya profesi Nelayan. Untuk peta pilih satu dari **BlueMap** atau **squaremap**; untuk GUI pilih satu dari **zMenu** atau **CommandPanels** hanya jika lebih baik daripada `MenuManager` internal.

Namun, blocker paling mendesak bukan plugin baru: **CI terbaru gagal**, payout/reward kontrak dan discovery membutuhkan tes idempotensi, backup bernama “full” belum berisi world penuh, dan mapping resource pack yang terlacak masih format v1. Tuntaskan ini dahulu; setelah itu, satu fitur baru yang terukur akan jauh lebih bernilai daripada sepuluh plugin yang saling tumpang tindih.
