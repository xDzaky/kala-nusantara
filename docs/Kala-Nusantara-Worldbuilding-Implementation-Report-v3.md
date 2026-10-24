# Kala Nusantara — Worldbuilding & World Generation Implementation Report v1.0

**Tujuan:** menjadi arahan implementasi untuk AI lokal (Claude/GPT/Gemini) agar menyiapkan worldbuilding, world generator, struktur eksplorasi, dan optimasi tanpa menyimpang dari Master Design v2.0 dan implementasi KalaCore yang sudah dilaporkan.

**Status dokumen:** rekomendasi desain dan rencana pengujian. Bukan klaim bahwa plugin sudah terpasang atau kompatibilitas sudah terbukti di server aktual.

---

## 1. Keputusan eksekutif

**Jangan pasang semua world generator sekaligus.** Untuk Kala Nusantara, gunakan arsitektur dua jenis dunia:

1. **`kala_main` — dunia cerita dan pemukiman terkurasi.** Pulau utama, Gunung Murka, Labuhan Awal, desa, pelabuhan, landmark, quest, proyek gotong royong, dan lokasi bencana ditempatkan secara sengaja. Terrain penting dibuat/diolah di luar server menggunakan WorldPainter dan/atau WorldEdit/FAWE, kemudian diuji dan diimpor. Jangan menjalankan generator acak pada dunia utama yang sudah memiliki lore dan koordinat penting.
2. **`kala_resource` — dunia sumber daya untuk eksplorasi berulang.** Pilih **satu** generator untuk pengujian: TerraformGenerator sebagai kandidat pertama untuk enhanced-vanilla terrain; atau Iris bila tim memang ingin membangun paket worldgen khusus dengan kontrol terrain, biome, gua, objek, dan struktur. Jangan gunakan keduanya sebagai generator utama pada world yang sama.
3. **Struktur eksplorasi:** uji BetterStructures pada dunia sumber daya atau wilayah petualangan yang dapat dibuat ulang. Atur struktur berdasarkan biome, jarak, loot, dan frekuensi. Struktur yang terkait quest utama sebaiknya dibuat manual/terkurasi, bukan diletakkan secara acak.
4. **Chunk pre-generation:** gunakan Chunky setelah world generator dan border final dipilih. Pregenerate area yang akan dibuka, bukan radius besar tanpa perhitungan.
5. **Java + Bedrock:** worldgen server-side dapat tetap dimainkan tanpa mod klien, tetapi custom item/block/texture memerlukan tes terpisah untuk Java dan Bedrock. Jangan menjadikan resource pack kompleks sebagai prasyarat untuk memahami gameplay.

### Mengapa ini cocok?

Kala sudah memiliki satu ikon naratif, Gunung Murka, dan sistem yang bergantung pada lokasi tetap: Labuhan Awal, kampung, proyek, quest, ekspedisi, dan fase Kala. Dunia utama yang sepenuhnya acak berisiko membuat titik pandang gunung, jalur quest, dan jarak antarlokasi tidak konsisten. Sebaliknya, dunia resource yang dapat direset cocok untuk generator prosedural dan struktur acak.

---

## 2. Fakta implementasi yang tercatat dan hal yang belum terbukti

Implementation report menyebut:

- Paper `26.3-218`, Java 25, Geyser/Floodgate, GriefPrevention, LuckPerms, Vault/ThunderEconomy, Chunky, PlaceholderAPI.
- `kala_main` sebagai pulau utama dengan border radius 3.000 blok.
- `kala_resource` sebagai dunia tambang terpisah dengan border radius 3.000 blok dan prosedur reset.
- KalaCore berisi sistem quest, profesi, ekonomi, proyek kampung, kalender musim, ekspedisi, GUI Kompas, dan NPC.
- Rencana perintah pre-generation sebelumnya menggunakan radius 3.000 untuk `kala_main`.

Dokumen laporan tidak membuktikan melalui artefak yang dapat diperiksa bahwa generator tertentu sudah terpasang, bahwa seluruh area sudah dipregenerate, atau bahwa TPS/MSPT sudah lolos load test. Karena itu AI harus memeriksa file server, log startup, `plugins/`, konfigurasi dunia, dan hasil uji sebelum menganggap fitur tersedia.

**Penting:** laporan menyebut Paper 26.3-218. Sebelum mengunduh plugin, cek apakah build Paper yang benar-benar dipakai masih cocok dengan setiap plugin. Jangan meng-upgrade server hanya demi satu plugin; jangan juga menganggap label kompatibilitas Modrinth otomatis menjamin bebas bug pada setup ini.

---

## 3. Pilihan world generator dan struktur

### A. TerraformGenerator — kandidat pertama untuk `kala_resource`

Sumber: https://modrinth.com/plugin/terraformgenerator
Repo: https://github.com/Hex27/terraformgenerator
Wiki: https://github.com/Hex27/terraformgenerator/wiki

**Peran:** menghasilkan terrain yang lebih dramatis tetapi tetap terasa seperti Minecraft vanilla.

**Pakai untuk:** dunia tambang/sumber daya yang akan di-reset dan tidak menyimpan bangunan permanen pemain.

**Jangan pakai untuk:** mengganti terrain `kala_main` yang sudah disusun untuk cerita tanpa rencana migrasi dan backup.

**Sebelum implementasi:**
- Cocokkan versi plugin dengan versi Minecraft/Paper yang benar-benar dipakai.
- Buat world test baru dengan nama berbeda, misalnya `kala_resource_test`.
- Uji terrain, cave, biome, struktur vanilla, teleport, reset, dan performa generasi chunk.
- Pastikan reset menghapus folder world yang benar dan tidak menyentuh `kala_main`.
- Simpan seed/config agar hasil bisa direproduksi jika diperlukan.

### B. Iris — alternatif worldgen custom yang lebih luas

Repo: https://github.com/VolmitSoftware/Iris
Dokumentasi: https://github.com/VolmitSoftware/docs/tree/master/iris
Rilis: https://github.com/VolmitSoftware/Iris/releases

**Peran:** engine worldgen dengan pack yang dapat dikustomisasi, mencakup terrain, biome, gua, struktur, objek, dan entitas. Iris lebih menarik jika Kala ingin membuat gaya kepulauan/biome khas melalui pack, tetapi menambah kurva belajar dan kebutuhan validasi.

**Pilih Iris apabila:**
- Tim siap memelihara pack/config worldgen sendiri.
- World test menunjukkan hasil yang lebih sesuai daripada TerraformGenerator.
- Versi Iris yang dipilih mendukung versi server aktual.

**Jangan pilih Iris hanya karena fiturnya banyak.** Tentukan satu hasil yang ingin dicapai, misalnya pulau tropis dengan tebing pesisir dan gua mineral yang khas, lalu ukur apakah pack tersebut benar-benar memenuhi target.

**Aturan:** jangan menjalankan TerraformGenerator dan Iris sebagai dua generator utama untuk world yang sama. Pilih salah satu per-world dan uji perilaku generator secara terisolasi.

### C. BetterStructures — lapisan konten eksplorasi

Modrinth: https://modrinth.com/plugin/betterstructures
Repo: https://github.com/MagmaGuy/BetterStructures
Dokumentasi: https://github.com/MagmaGuy/BetterStructures/wiki

**Peran:** menambahkan schematic/struktur buatan, struktur modular, dungeon, loot, dan konfigurasi lokasi/spawn. Ini bukan pengganti terrain generator; ia mengisi dunia dengan tempat untuk ditemukan pemain.

**Rekomendasi:** uji pada `kala_resource_test` terlebih dahulu. Mulai dengan beberapa struktur, bukan semua pack sekaligus.

**Kurasi konten:**
- Reruntuhan pos pelaut: loot dasar, catatan lore, tidak terlalu berbahaya.
- Menara pengawas tua: landmark yang terlihat dari area sekitar.
- Gua tambang terbengkalai: mineral dan jalur eksplorasi.
- Kuil/ruang ritual Kala: tantangan berkelompok, loot terbatas, hanya jika desain encounter sudah diuji.
- Kampung kecil atau tempat singgah: dekorasi dan suplai dasar.

**Aturan spawn:** struktur harus punya jarak minimum, aturan biome/terrain, daftar world yang diizinkan, dan loot yang seimbang. Jangan biarkan struktur acak muncul di spawn, menimpa klaim, memblokir jalur utama, atau menghasilkan loot yang merusak ekonomi.

**Quest penting:** lokasi quest yang wajib ditemukan tidak boleh hanya bergantung pada peluang generasi acak. Tempatkan manual atau pastikan ada sistem pencarian/penanda dan koordinat yang stabil.

### D. WorldPainter + WorldEdit/FAWE — kontrol map utama

WorldPainter: https://www.worldpainter.net/
WorldEdit: https://github.com/EngineHub/WorldEdit
FAWE: https://github.com/IntellectualSites/FastAsyncWorldEdit

**Peran:** membentuk pulau, garis pantai, gunung, lembah, jalur sungai, dan komposisi terrain yang disengaja. WorldEdit/FAWE membantu operasi editing di dalam Minecraft; WorldPainter cocok untuk sculpting terrain skala besar.

**Pakai untuk:** `kala_main`, terutama Gunung Murka, Labuhan Awal, rute laut, teluk, kampung utama, dan titik pandang.

**Workflow:**
1. Buat backup penuh sebelum perubahan.
2. Rancang peta top-down dan koordinat landmark.
3. Bentuk terrain di salinan world atau proyek WorldPainter.
4. Ekspor/import ke world staging, bukan langsung produksi.
5. Bangun struktur utama dan jalur perjalanan.
6. Periksa titik pandang dari spawn, pelabuhan, mercusuar, dan jalur menuju wilayah lain.
7. Uji collision, aksesibilitas, ketinggian, batas world, dan jalur Bedrock/mobile.
8. Baru promosikan ke produksi setelah QA dan backup terverifikasi.

---

## 4. Worldbuilding target Kala Nusantara

### 4.1 `kala_main`: peta cerita

Map utama bukan sekadar tempat membangun rumah. Ia harus menyampaikan cerita melalui geografi dan landmark.

**Zona yang disarankan:**

1. **Labuhan Awal** — area spawn/tutorial. Aman, mudah dipahami, tanpa struktur acak yang mengganggu. Menyediakan papan petunjuk, NPC, dermaga, peta, dan jalur menuju kampung.
2. **Gunung Murka** — landmark naratif utama. Bentuk gunung dibuat manual agar siluetnya kuat dari titik pandang tertentu. Pemain tidak harus melihatnya dari seluruh map; gunakan garis pandang yang sengaja dibuka dari pelabuhan, mercusuar, atau punggung bukit.
3. **Kampung utama** — tempat pemain membangun rumah, mengembangkan proyek, berdagang, dan berinteraksi. Proyek seperti Lumbung dan Dermaga harus memberi manfaat nyata, bukan sekadar progress bar.
4. **Pesisir dan teluk** — jalur pelayaran, memancing, perdagangan, dan tempat menemukan cerita lingkungan.
5. **Rimba dan dataran tinggi** — bahan alami, jalur eksplorasi, pemandangan, dan tempat singgah.
6. **Lokasi lore terkurasi** — reruntuhan atau landmark yang terhubung dengan quest. Jumlah awal kecil tetapi masing-masing memiliki cerita, hadiah, dan tujuan.
7. **Zona aman pemukiman** — klaim pemain tidak boleh terkena generasi struktur, reset, atau perubahan bencana yang tidak diinformasikan.

### 4.2 `kala_resource`: eksplorasi yang bisa diperbarui

Dunia ini memenuhi kebutuhan mineral dan material yang dapat di-reset. Ia boleh memiliki terrain prosedural dan struktur acak karena bangunan permanen pemain tidak dijanjikan bertahan di sana.

Aturan:
- Tampilkan peringatan saat pemain masuk: dunia resource dapat di-reset.
- Hindari mengizinkan klaim permanen kecuali ada kebutuhan desain yang jelas.
- Evakuasi pemain sebelum reset, hentikan teleport masuk selama reset, verifikasi folder target, lalu buat ulang world.
- Reset harus memiliki lock/idempotency agar dua admin atau dua task tidak menjalankan reset bersamaan.
- Simpan log siapa menjalankan reset, waktu, world name, hasil, dan error.
- Uji pemulihan bila proses terhenti di tengah jalan.

### 4.3 Gunung Murka dan jarak pandang

Jangan membuat requirement “gunung selalu terlihat dari mana saja”. Hal itu tidak realistis karena view distance server dan render distance client membatasi chunk yang tampil.

Desain yang benar:
- Buat siluet gunung besar dan unik dari titik-titik pandang yang memang dirancang.
- Jaga koridor pandang dari Labuhan Awal, mercusuar, dan beberapa jalur utama.
- Gunakan peta, kompas, landmark kecil, nama wilayah, dan jurnal untuk orientasi di area yang gunungnya tidak terlihat.
- Hindari membuat pulau utama terlalu lebar sehingga semua landmark menjadi tidak terbaca.
- Tes pada Bedrock mobile dengan render distance rendah; jangan mengandalkan detail kecil yang hanya terlihat dari jarak jauh.

### 4.4 Apakah map harus besar?

Tidak. Map yang besar tetapi kosong membuat perjalanan monoton dan menambah biaya pre-generation, penyimpanan, serta beban eksplorasi. Mulai dari area yang padat dengan landmark dan loop gameplay. Perluas border hanya ketika ada konten yang membenarkan perluasan.

---

## 5. Cara menghindari eksplorasi monoton

Tujuannya bukan mengisi setiap chunk dengan bangunan. Berikan ritme antara perjalanan, penemuan, tantangan, dan kembali ke kampung.

### Jenis aktivitas

- **Landmark terlihat dari jauh:** mercusuar, reruntuhan di tebing, pohon raksasa, menara pengawas.
- **Tempat singgah:** pos pelaut, pondok tua, dermaga kecil yang menyediakan petunjuk atau suplai.
- **Temuan opsional:** catatan lore, peta sobek, koleksi artefak, lukisan/pola/banner.
- **Aktivitas profesi:** bahan khusus yang mendorong pemain menjelajahi zona berbeda, tetapi tidak mengunci quest utama pada satu profesi.
- **Ekspedisi co-op:** tujuan jelas, durasi yang masuk akal, mekanisme revive/evakuasi sederhana bila dibutuhkan, dan hadiah yang tidak bisa dieksploitasi.
- **Perubahan musiman:** beberapa jalur atau aktivitas berubah, bukan seluruh map dihapus. Misalnya, pesanan pasar berganti, lokasi ekspedisi baru dibuka, atau kampung meminta bahan tertentu.
- **Kontribusi kampung:** temuan pemain bisa membuka dekorasi, fasilitas, atau cerita baru untuk seluruh komunitas.

### Struktur yang bagus vs struktur yang hanya menambah jumlah

Setiap struktur custom yang dipilih harus menjawab:
1. Apa yang dilihat pemain dari luar?
2. Mengapa pemain mendekatinya?
3. Apa aktivitas yang dilakukan di dalamnya?
4. Apa hadiah atau pengetahuan yang didapat?
5. Apakah lokasi itu tetap menarik setelah pertama kali dikunjungi?
6. Apakah loot-nya merusak ekonomi atau profesi?
7. Apakah pemain Bedrock dapat menyelesaikan mekanismenya melalui kontrol sentuh?

Jika jawaban hanya “ada chest berisi loot”, struktur tersebut belum cukup kuat sebagai konten Kala.

---

## 6. Resource pack dan custom asset

### Prinsip

Mulai dengan resource pack ringan dan konsisten, bukan model 3D kompleks dalam jumlah besar. Identitas visual bisa dibangun dari palet blok, banner, peta, signage, suara ambient, dan beberapa item ikonik.

### Paket yang disarankan

**`KalaPack-Java`**
- Nama item/lore dan ikon yang memakai resource pack.
- Model sederhana untuk beberapa item lore/quest bila diperlukan.
- Suara ambient/efek pendek dengan volume yang tidak mengganggu.
- Font/icon hanya jika tetap terbaca dan tidak merusak UI.

**`KalaPack-Bedrock`**
- Mapping aset yang setara untuk Bedrock.
- Ikon/menu yang terbaca pada layar kecil.
- Tes unduh pack, prompt penerimaan, reconnect, dan perangkat mobile.
- Tidak menganggap pack Java bisa digunakan langsung di Bedrock.

**Urutan aset:**
1. Identitas visual spawn dan signage.
2. Ikon untuk Kompas/Jurnal/Profesi yang terbaca.
3. Item lore dan artefak yang sangat terbatas.
4. Suara ambience/efek untuk event Kala.
5. Furniture/custom blocks hanya jika memiliki manfaat gameplay dan sudah diuji crossplay.

### Pilihan plugin item

Jangan mengunci proyek ke ItemsAdder atau Oraxen sebelum uji kompatibilitas dengan versi server, lisensi, distribusi pack Bedrock, dan Geyser. Pilih satu sistem custom-item saja setelah proof-of-concept berhasil. Jika aset awal dapat diwujudkan dengan resource pack vanilla-compatible dan plugin kecil buatan sendiri, itu mungkin lebih sederhana untuk MVP.

---

## 7. Plugin/tool stack yang disarankan

| Komponen | Keputusan | Tujuan |
|---|---|---|
| Paper | Pertahankan build yang telah diuji | Server plugin utama |
| Geyser + Floodgate | Pertahankan; uji setiap update | Java/Bedrock crossplay |
| KalaCore | Pertahankan sebagai gameplay core | Quest, profesi, musim, proyek, UI |
| GriefPrevention | Pertahankan bila test klaim lulus | Proteksi bangunan pemain |
| LuckPerms | Pertahankan | Permission dan pemisahan akses admin |
| Chunky | Pertahankan | Pre-generation terkontrol |
| WorldPainter | Gunakan pada pipeline pembuatan map | Sculpting map utama |
| WorldEdit atau FAWE | Pilih sesuai kebutuhan dan versi | Editing terrain/struktur; jangan anggap keduanya harus dipasang bersamaan |
| TerraformGenerator | Kandidat utama untuk resource-world test | Terrain enhanced-vanilla |
| Iris | Alternatif, bukan tambahan wajib | Worldgen pack yang lebih bisa dikustomisasi |
| BetterStructures | Kandidat untuk resource-world test | Struktur dan dungeon modular |
| BlueMap atau squaremap | Opsional, pilih satu untuk MVP | Peta web; periksa privasi lokasi/claim dan konsumsi resource |
| spark | Direkomendasikan untuk diagnosis | Profiling tick dan bottleneck |
| CoreProtect | Pertimbangkan setelah verifikasi kompatibilitas | Audit/perlindungan perubahan blok; uji pada build aktual |

**Jangan memasang semua opsi sekaligus.** Setiap plugin tambahan memperbesar permukaan konflik, konsumsi memori, dan beban pemeliharaan. Pilih satu plugin per fungsi dan dokumentasikan alasan pemasangannya.

---

## 8. Pre-generation dan optimasi

Laporan implementasi menyarankan `chunky world kala_main radius 3000 start`. Jangan jalankan perintah itu secara otomatis tanpa memastikan sintaks versi Chunky yang terpasang, bentuk world, center, dan anggaran storage.

### Prosedur aman
1. Ambil backup dan verifikasi backup dapat dibaca.
2. Tetapkan world, center, radius, dan border final.
3. Hitung area yang akan digenerate. Radius 3.000 blok mencakup area lingkaran sekitar 28,27 juta blok persegi sebelum memperhitungkan chunk dan bentuk pesisir; jumlah chunk/penyimpanan aktual bergantung pada area dan generator.
4. Mulai dari area spawn dan jalur utama, lalu perluas bertahap.
5. Pantau MSPT/TPS, penggunaan RAM, disk I/O, ukuran world, dan durasi.
6. Jalankan pre-generation saat server sepi atau pada staging; gunakan pause/resume bila performa turun.
7. Uji join pemain dan eksplorasi ketika pre-generation berjalan.
8. Jangan pregenerate seluruh radius besar pada mesin lokal dengan storage terbatas tanpa mengukur kebutuhan.

### Target penerimaan awal

- Tidak ada error fatal saat boot atau generate chunk.
- Tidak ada teleport ke void atau area belum siap.
- Tidak ada struktur yang muncul di spawn/claim secara tidak sengaja.
- Tick time tetap dalam target operasional yang disepakati ketika 5–10 pemain menjelajah serentak di staging.
- Catat median dan p95 MSPT, bukan hanya rata-rata TPS.
- Bedrock mobile dapat berjalan di area yang sama tanpa UI yang terpotong.

Angka target performa final harus ditetapkan berdasarkan CPU, RAM, storage, jumlah pemain, dan jarak simulasi server aktual. Jangan mengklaim server siap menampung jumlah pemain tertentu tanpa load test.

---

## 9. Prosedur implementasi untuk AI lokal

### Aturan keras untuk coding agent

1. Baca `Kala-Nusantara-Master-Design-v2.0.md`, `Kala-Nusantara-Master-Implementation-Report.md`, `Kala-Nusantara-Post-Implementation-Roadmap-v1.md`, dan dokumen ini sebelum mengubah file.
2. Audit repository dan server aktual terlebih dahulu. Jangan menganggap semua isi report benar-benar ada di filesystem.
3. Jangan mengubah `kala_main` produksi secara langsung.
4. Jangan memasang TerraformGenerator dan Iris bersama sebagai generator aktif untuk world yang sama.
5. Jangan membuat/reset world sebelum memverifikasi path world dan backup.
6. Jangan menghapus atau mengganti plugin produksi tanpa approval eksplisit.
7. Jangan mengunduh JAR dari situs mirror tidak resmi; prioritaskan halaman proyek resmi/Modrinth/GitHub release.
8. Catat versi Minecraft, Paper build, Java runtime, plugin version, checksum bila tersedia, tanggal unduh, dan URL sumber.
9. Jika kompatibilitas tidak jelas, hentikan tahap instalasi dan laporkan temuan; jangan menebak.
10. Buat perubahan dalam branch/folder staging, satu komponen setiap iterasi.
11. Uji Java dan Bedrock. Uji juga player baru, pemain dengan claim, reset resource-world, backup/restore, dan restart server.
12. Jangan menyatakan berhasil jika hanya mengedit konfigurasi. Lampirkan log boot, perintah uji, hasil aktual, dan masalah tersisa.

### Master prompt untuk AI lokal

> Kamu adalah Minecraft server engineer dan worldbuilding designer untuk Kala Nusantara. Baca seluruh dokumen desain dan laporan implementasi yang tersedia sebelum bertindak. Pertahankan identitas Kala Nusantara: survival kooperatif Nusantara, Gunung Murka sebagai landmark naratif, Labuhan Awal sebagai onboarding, kampung permanen, musim Kala, ekspedisi, dan kompatibilitas Java–Bedrock.
>
> Tugas pertama adalah AUDIT, bukan instalasi. Periksa repository, folder server, versi Paper/Java, plugin JAR dan versinya, konfigurasi world, seed, border, folder `kala_main` dan `kala_resource`, backup, dan log. Buat tabel “terkonfirmasi / belum terkonfirmasi / bermasalah”. Jangan menyimpulkan status dari report saja.
>
> Rekomendasi arsitektur: `kala_main` adalah world cerita terkurasi yang tidak boleh diganti generator atau di-reset tanpa persetujuan; `kala_resource_test` adalah world staging untuk menguji satu generator. Uji TerraformGenerator terlebih dahulu untuk enhanced vanilla. Uji Iris sebagai alternatif terpisah jika hasil Terraform tidak memenuhi art direction. Uji BetterStructures hanya setelah terrain stabil. Jangan aktifkan dua generator utama pada world yang sama.
>
> Buat rencana uji dan minta persetujuan sebelum mengunduh atau mengubah server. Semua unduhan harus berasal dari sumber resmi dan versi harus cocok dengan build aktual. Backup sebelum setiap perubahan. Jangan pernah reset `kala_main`. Untuk reset resource world, verifikasi nama/path, evakuasi pemain, lock operasi, log, dan rollback.
>
> Setelah approval, kerjakan satu tahap saja: (1) audit, (2) buat `kala_resource_test`, (3) pasang satu generator, (4) generate area kecil, (5) inspeksi terrain, (6) uji BetterStructures, (7) ukur performa, (8) baru rekomendasikan promosi. Jangan otomatis mempromosikan staging ke produksi.
>
> Deliverable setiap tahap: file yang berubah, alasan, URL dan versi dependency, konfigurasi, perintah yang dijalankan, log relevan, hasil tes Java dan Bedrock, hasil performa, risiko, dan rollback plan. Jika tes gagal atau versi tidak jelas, berhenti dan jelaskan. Jangan mengarang hasil tes.

---

## 10. Tahapan implementasi yang direkomendasikan

### Tahap A — Audit lingkungan
- Catat versi aktual Paper/Java/Geyser/Floodgate/Chunky.
- Periksa daftar plugin dan log startup.
- Pastikan folder world dan backup.
- Periksa apakah `kala_resource` benar-benar world terpisah atau path dimensi; jangan menebak dari nama folder saja.
- Simpan report audit ke `docs/WORLDGEN-AUDIT.md`.

### Tahap B — Buat staging world
- Buat `kala_resource_test` dengan seed baru.
- Jangan mengubah world produksi.
- Ukur ukuran folder sebelum dan sesudah generate.

### Tahap C — Bandingkan generator
- Test TerraformGenerator terlebih dahulu.
- Jika hasil kurang cocok, test Iris di world staging lain.
- Bandingkan bentuk terrain, biome, gua, navigasi, performa, dan kompatibilitas.
- Pilih satu berdasarkan hasil nyata.

### Tahap D — Struktur
- Tambahkan BetterStructures setelah terrain dipilih.
- Mulai dengan 3–5 struktur dan konfigurasi loot minimal.
- Pastikan struktur tidak menimpa claim dan tidak muncul di area cerita.

### Tahap E — Kurasi map utama
- Jangan ubah world produksi langsung.
- Buat copy/staging map, tentukan koordinat Gunung Murka dan titik pandang.
- Uji navigasi, landmark, jalur laut, spawn, serta akses mobile.

### Tahap F — Pre-generation dan promosi
- Tentukan area target berdasarkan konten yang siap.
- Pregenerate bertahap, pantau metrik, lalu QA.
- Promosikan dengan maintenance window, backup, dan rollback yang jelas.

---

## 11. Acceptance checklist

### World utama
- [ ] Spawn Labuhan Awal jelas dan ramah pemain baru.
- [ ] Gunung Murka terlihat dari titik pandang yang memang dirancang.
- [ ] Jalur eksplorasi mengarah ke landmark atau aktivitas, bukan ruang kosong berkepanjangan.
- [ ] Claim pemain aman dari generasi struktur dan reset.
- [ ] Quest penting memiliki lokasi stabil.
- [ ] Area dapat dimainkan pada render distance Bedrock mobile yang rendah.

### Resource world
- [ ] Generator yang dipilih hanya satu per world.
- [ ] Reset tidak dapat menyentuh world utama.
- [ ] Evakuasi dan lock reset berfungsi.
- [ ] Struktur tidak terlalu rapat dan loot seimbang.
- [ ] Pre-generation memiliki batas dan dapat dihentikan.

### Operasional
- [ ] Backup dibuat dan diuji restore.
- [ ] Versi semua dependency dicatat.
- [ ] Tidak ada error fatal pada boot.
- [ ] Tes Java dan Bedrock lulus.
- [ ] Metrik performa dan ukuran world dicatat.
- [ ] Rollback plan tersedia.

---

## 12. Referensi resmi untuk pemeriksaan versi

- TerraformGenerator (Modrinth): https://modrinth.com/plugin/terraformgenerator
- TerraformGenerator (GitHub): https://github.com/Hex27/terraformgenerator
- Iris (GitHub): https://github.com/VolmitSoftware/Iris
- Iris releases: https://github.com/VolmitSoftware/Iris/releases
- Iris docs: https://github.com/VolmitSoftware/docs/tree/master/iris
- BetterStructures (Modrinth): https://modrinth.com/plugin/betterstructures
- BetterStructures (GitHub): https://github.com/MagmaGuy/BetterStructures
- WorldPainter: https://www.worldpainter.net/
- WorldEdit: https://github.com/EngineHub/WorldEdit
- FastAsyncWorldEdit: https://github.com/IntellectualSites/FastAsyncWorldEdit
- Chunky: https://github.com/pop4959/Chunky
- Paper documentation: https://docs.papermc.io/paper/
- Geyser documentation: https://geysermc.org/wiki/

**Catatan versi:** hasil riset publik yang diperiksa pada 24 Oktober 2026 menunjukkan TerraformGenerator mencantumkan dukungan 26.3, BetterStructures juga mencantumkan 26.3, dan Iris memiliki rilis yang menargetkan 26.1.2–26.3 dengan Java 25. Ini adalah deklarasi proyek, bukan jaminan kompatibilitas pada konfigurasi Kala yang spesifik. Selalu cocokkan release, Paper build, Java runtime, dan log startup sebelum produksi.

---

## Rekomendasi final

Untuk Kala Nusantara saat ini, pilihan paling aman dan seimbang adalah:

- **`kala_main`:** map terkurasi dengan WorldPainter + WorldEdit/FAWE sebagai alat pembuatan, bukan generator acak yang mengganti terrain cerita.
- **`kala_resource_test`:** mulai uji TerraformGenerator.
- **BetterStructures:** tambahkan setelah terrain stabil, mulai dari sedikit struktur dengan loot terkontrol.
- **Iris:** jadikan alternatif bila dibutuhkan worldgen yang lebih khusus; jangan memasangnya hanya karena fitur lebih banyak.
- **Chunky:** pregenerate area yang sudah diputuskan, secara bertahap dan terukur.
- **KalaPack Java/Bedrock:** buat aset ringan dan diuji terpisah.

Keberhasilan bukan diukur dari banyaknya plugin, melainkan apakah pemain menemukan tempat menarik, punya alasan menjelajah, kembali membawa hasil ke kampung, dan bisa melakukannya tanpa server tersendat atau progres permanen terancam.
