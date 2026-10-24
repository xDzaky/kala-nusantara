# Kala Nusantara — Gunung Murka, Bencana Musiman & Encounter Design v6.0

**Jenis dokumen:** Rekomendasi desain dan implementasi lanjutan dari report v5.0  
**Target:** Paper 26.3-218 + Geyser/Floodgate, Java Edition dan Bedrock/MCPE  
**Prinsip:** atmosfer dramatis, dampak dunia terkendali, gameplay kooperatif, tetap ringan, tidak memerlukan client mod.  
**Status dokumen:** desain yang diusulkan; bukan bukti bahwa fitur-fitur baru di bawah sudah diimplementasikan.

---

## 1. Keputusan utama

V5 sudah memiliki pondasi penting: `VolcanoManager` dengan lima fase, `KalaNotificationService`, satu pemilik per komponen UI, GUI `/gunung`, kontribusi sajen, berkah pasca-erupsi, dan penyimpanan SQLite. Jangan mengganti semuanya. Tambahkan lapisan modular di atas sistem yang ada:

1. **`VolcanoEncounterDirector`** — mengurutkan adegan kecil selama fase `PUNCAK`: hujan abu, gempa, rockfall, retakan panas, monster opsional, lalu objective akhir.
2. **`HazardDirector`** — membuat bahaya dengan zona, peringatan, batas dampak, cooldown, dan pembersihan yang eksplisit.
3. **`SeasonalEventDirector`** — memilih bencana yang cocok dengan kalender musim KalaCore yang sudah ada. Jangan menambah plugin musim kedua.
4. **`WorldImpactPolicy`** — satu gerbang wajib untuk semua perubahan blok, api, damage, ledakan, dan spawn entity dari event.
5. **`EventContributionLedger`** — menyimpan partisipasi pemain secara idempotent supaya reward tidak bisa diklaim berkali-kali.

Tujuan desainnya bukan membuat erupsi yang merusak sebanyak mungkin, tetapi menjadikan pemain sibuk mengambil keputusan, bekerja sama, menghindari bahaya, dan mendapatkan hasil yang terasa berbeda setiap kali event berlangsung.

## 2. Apa yang perlu ditambahkan setelah V5?

| Prioritas | Komponen | Alasan | Definisi selesai |
|---|---|---|---|
| P0 | `WorldImpactPolicy` dan event cleanup | Menghindari kerusakan struktur, block entity, atau area klaim | Semua event dapat dihentikan; hazard dan entity sementara dibersihkan; tidak ada perubahan di area terlarang |
| P0 | Penanda zona aman/telegraph bahaya | Pemain harus bisa memahami dan menghindari serangan, termasuk di HP | Ada peringatan visual/audio beberapa detik sebelum damage |
| P0 | Batas jumlah entity, partikel, dan efek per tick | Menjaga TPS dan pengalaman mobile | Batas global/per-world/per-event dikonfigurasi dan tercatat |
| P1 | `VolcanoEncounterDirector` | Mengubah erupsi dari rentetan partikel menjadi encounter yang punya ritme | Minimal tiga jenis gelombang berbeda, dipilih dari daftar yang diatur konfigurasi |
| P1 | Objective berbasis peran | Pemain non-combat tetap punya tugas berarti | Ada tugas pengintai, logistik, peneliti, penjaga, dan pemulihan |
| P1 | Event musiman kecil | Menyediakan variasi tanpa selalu memakai Gunung Murka | Minimal dua event unik yang sudah diuji di staging |
| P1 | Reward ledger dan anti-exploit | Mencegah spam donasi, AFK reward, atau reward ganda | Satu pemain menerima hadiah event maksimal sekali sesuai aturan kontribusi |
| P2 | Monster elite / boss | Menambah klimaks, bukan sekadar mob acak | AI dan damage dapat ditangani server-side; vanilla fallback tetap berfungsi di Bedrock |
| P2 | Efek model/resource pack khusus | Menambah identitas visual | Java dan Bedrock pack dibuat dan diuji terpisah; paket ringan dan opsional/terukur |
| P2 | Museum/Chronicle of Disasters | Menghadirkan alasan untuk kembali setelah event selesai | Jurnal menyimpan event, kontribusi, ending, dan koleksi secara persisten |

---

## 3. Bentuk akhir pengalaman Gunung Murka

### 3.1 Pertahankan state machine tingkat tinggi V5

State tingkat tinggi yang sudah ada tetap menjadi sumber kebenaran:

`TENANG → WASPADA → SIAGA → PUNCAK → PEMULIHAN → TENANG`

Jangan membuat plugin atau scoreboard lain yang mencoba mengendalikan fase yang sama. `VolcanoEncounterDirector` hanya bekerja di dalam `PUNCAK`; dia tidak boleh mengubah fase global sendiri tanpa meminta `VolcanoManager`.

### 3.2 Tambahkan subfase encounter di dalam `PUNCAK`

Subfase ini adalah susunan adegan, bukan state machine musim baru:

1. **Pembuka kawah (0–45 detik):** suara dari arah gunung, siluet asap, abu, dan pengumuman zona bahaya. Tidak ada damage saat pemain belum diberi waktu membaca petunjuk.
2. **Gelombang abu (45–120 detik):** visibilitas atmosferik dengan partikel terbatas; pemain mendapat tugas mengamankan logistik atau memeriksa titik pengamatan.
3. **Rockfall (120–240 detik):** 2–4 salvo bongkahan batu visual, dipisahkan waktu jeda. Setiap titik jatuh ditandai terlebih dahulu.
4. **Retakan panas atau jalur lava semu (240–360 detik):** beberapa zona berbahaya muncul di arena khusus. Gunakan partikel/suara dan damage server-side; jangan menaruh aliran lava permanen.
5. **Encounter opsional (360–540 detik):** monster bara muncul jika tingkat ancaman, jumlah peserta, dan kesiapan event sesuai. Event tetap bisa dituntaskan oleh pemain yang memilih tugas non-combat.
6. **Objective final (540–720 detik):** warga memilih tugas akhir, misalnya menutup dua retakan, mengevakuasi peti logistik, atau mengambil sampel mineral.
7. **Akhir event:** hasil komunitas dihitung, hazard dinonaktifkan, entity sementara dihapus, reward diberikan satu kali, lalu fase bergerak ke `PEMULIHAN`.

Waktu di atas titik awal untuk playtest, bukan angka mutlak. Durasi sebaiknya dapat diatur melalui konfigurasi dan diskalakan ringan berdasarkan jumlah peserta aktif. Jangan mempercepat event sampai pemain Bedrock tidak sempat melihat telegraph.

### 3.3 Gunung terlihat hidup bahkan sebelum erupsi

Jangan membuat gunung aktif hanya saat server mengirim pesan besar. Gunakan isyarat kecil yang jarang dan berubah menurut fase:

- `TENANG`: asap tipis sesekali, burung menjauh dari lereng, NPC menyebut suara jauh.
- `WASPADA`: tremor ringan berupa suara dan partikel dekat titik tertentu; jurnal menerima petunjuk.
- `SIAGA`: asap lebih padat, lampu/penanda desa menyala, jalur menuju arena diberi tanda.
- `PUNCAK`: gelombang bahaya dan objective kooperatif.
- `PEMULIHAN`: mineral abu dapat diteliti, pedagang membuka kontrak pemulihan, satwa kembali ke habitat.

Jangan menjalankan partikel global terus-menerus di seluruh gunung. Emit hanya di titik-titik yang aktif, pada interval, dan ketika ada pemain dalam jarak relevan.

---

## 4. Apakah gunung harus memuntahkan bongkahan batu?

**Ya, bagus sebagai momen visual**, asalkan batu memiliki peringatan, jalur yang dapat dibaca, dan aturan benturan yang tidak merusak dunia secara acak.

### 4.1 Perilaku rockfall yang direkomendasikan

Setiap salvo memiliki urutan:

1. Pilih titik dampak di dalam `event arena` yang diizinkan; jangan memilih koordinat berdasarkan posisi pemain atau rumah secara langsung.
2. Tampilkan penanda dampak berwarna tembaga/merah selama kira-kira 2–3 detik. Di HP, jangan hanya mengandalkan suara atau partikel tipis; beri indikator yang mudah dilihat dan notifikasi actionbar singkat.
3. Luncurkan batu dari arah lereng/kawah menggunakan lintasan pendek yang telah divalidasi. Jangan menembak semua batu ke lokasi pemain.
4. Beri kesempatan menghindar. Jika pemain berada dekat titik dampak setelah telegraph, terapkan damage/knockback terbatas dari logika server; jangan menggunakan ledakan TNT.
5. Ketika batu menyentuh tanah, mainkan debu, suara batu, dan beberapa partikel. Batu menghilang; tidak menjadi blok permanen secara default.
6. Batasi jumlah batu hidup pada waktu bersamaan, jeda antarsalvo, jarak render, dan jumlah pemain yang boleh memicu efek per lokasi.

### 4.2 Pilihan implementasi visual

**Pilihan awal yang perlu dites:** `FallingBlock` vanilla dengan blok seperti `COBBLESTONE`, `TUFF`, atau `BLACKSTONE`, lalu set `setHurtEntities(false)`, `setDropItem(false)`, dan `setCancelDrop(true)`. Identifikasi entity event dengan `PersistentDataContainer`, batalkan perubahan blok saat mendarat, lalu hapus entity setelah efek benturan. Damage pemain ditangani oleh `HazardDirector` setelah memeriksa zona bahaya, bukan oleh fisika `FallingBlock`.

Paper mendokumentasikan kontrol drop dan damage pada `FallingBlock`; tetap lakukan pengujian di Paper build persis yang dipakai server. Referensi API: https://jd.papermc.io/paper/26.2/org/bukkit/entity/FallingBlock.html

**Jangan mengandalkan `BlockDisplay` sebagai satu-satunya cara batu terbang lintas platform.** `BlockDisplay` sangat cocok untuk dekorasi/interpolasi visual di Java, tetapi ia adalah display entity tanpa hitbox; laporan Geyser #6723 pada September 2026 mencatat kasus perubahan posisi display entity bergerak tidak tampak pada klien Bedrock. Lakukan proof-of-concept sebelum memakainya untuk efek inti:

- Paper display entities: https://docs.papermc.io/paper/dev/display-entities/
- Geyser issue #6723: https://github.com/GeyserMC/Geyser/issues/6723

Bila `FallingBlock` yang diberi tag event tidak tampil atau bergerak konsisten di Bedrock, gunakan fallback lintasan batu berbasis partikel + suara + damage zone; jangan membuat visual utama Java-only. Untuk test ini, pastikan pemain Java dan Bedrock melihat waktu, arah, dan area dampak yang masuk akal.

### 4.3 Dampak benturan

Default `kala_main`: **tidak mengubah blok dunia**. Benturan boleh:

- memberi damage ringan/menengah yang dibatasi;
- memberi knockback kecil jika pemain masih berada di zona dampak;
- memberi efek `SLOWNESS` atau `MINING_FATIGUE` singkat, dengan cooldown;
- menciptakan drop sementara seperti `Batu Apung` atau `Fragmen Murka` yang dihapus setelah event berakhir;
- mengubah objective atau membuka titik pengumpulan sementara.

Jangan membuat batu menghancurkan peti, redstone, tanaman, rumah, NPC, atau claim pemain. Jangan gunakan TNT atau ledakan blok sebagai jalan pintas.

---

## 5. Monster lava/api: bagus jika menjadi encounter opsional, bukan spam mob

Monster meningkatkan variasi ketika mereka mempunyai pola serangan yang terbaca dan alasan untuk dilawan. Kalau sekadar spawn banyak Blaze atau Magma Cube, event cepat berubah menjadi mob farm dan ekonomi drop bisa rusak.

### 5.1 Rekomendasi roster awal

| Nama lore | Basis vanilla | Peran | Perilaku yang aman untuk versi awal |
|---|---|---|---|
| **Percik Bara** | Small Magma Cube | Gangguan ringan | Bergerak cepat, HP rendah, drop kosmetik/material kecil |
| **Penjaga Kawah** | Blaze | Penjaga jarak jauh | Proyektil dibatasi; tidak menyalakan api blok; spawn hanya di arena |
| **Golem Abu** | Husk atau Wither Skeleton | Elite melee | Serangan ayun dengan tanda 2 detik; knockback kecil; tanpa efek Wither permanen |
| **Panglima Bara** | Magma Cube ukuran besar atau mob vanilla yang dipilih | Boss musiman opsional | 2–3 fase serangan, area bahaya diumumkan, loot diberikan sekali per pemain yang memenuhi syarat |

Nama-nama di atas adalah konsep. Atribut, AI, dan model belum diimplementasikan hanya dengan menulis report.

### 5.2 Contoh pola boss

- **Hantaman Kawah:** boss berhenti sebentar, zona lingkaran muncul, lalu gelombang damage ringan menyebar; pemain bisa keluar dari lingkaran.
- **Lontaran Bara:** 3 proyektil yang memberikan damage terbatas ke pemain, tanpa membakar blok atau meledakkan struktur.
- **Panggilan Percik:** memanggil maksimal 2 Percik Bara di arena; jangan terus memanggil saat jumlah mob sudah mencapai cap.
- **Fase Rapuh:** setelah dua serangan besar, boss diam beberapa detik; tim bisa menyerang atau menyelesaikan objective samping.

Buat boss sebagai *opsional/berbasis kondisi*: jika tidak ada pemain yang cocok atau peserta terlalu sedikit, event menjalankan objective pemulihan non-combat sebagai gantinya. Hindari spawn otomatis di dekat Labuhan Awal, rumah pemain, atau area onboarding.

### 5.3 Kompatibilitas Java–Bedrock

Tahap pertama sebaiknya menggunakan entitas vanilla yang sudah dikenal Geyser, lalu mengubah nama, atribut, loot, dan perilaku dari KalaCore. Dengan cara ini pemain Bedrock tetap melihat makhluk dasar yang valid tanpa client mod. Custom model baru menjadi lapisan berikutnya melalui resource pack Java dan Bedrock terpisah.

Geyser memiliki API custom entity yang didokumentasikan sebagai **eksperimental**; custom entity Bedrock memerlukan definisi/entity assets dan resource pack. Jangan jadikan API eksperimental sebagai syarat agar event inti bisa diselesaikan. Referensi: https://geyser.kastle.dev/wiki/geyser/custom-entities/

---

## 6. Bencana musiman yang punya gameplay, bukan hanya efek cuaca

Ikuti kalender musim KalaCore yang sudah ada. V5 menyebut siklus musim delapan minggu, tetapi mapping tepat setiap nama musim harus dibaca dari konfigurasi/implementasi saat ini; jangan mengasumsikan durasi tiap musim dan jangan membuat kalender kedua.

### 6.1 Empat event pendamping

| Event | Fenomena | Gameplay utama | Dampak dunia default |
|---|---|---|---|
| **Badai Selat** | Angin, hujan lebat, petir jauh, kabut pesisir | Nyalakan mercusuar, antarkan sinyal, temukan peti hanyut, bantu kapal/NPC kembali ke pelabuhan | Cuaca/partikel terbatas; tidak menenggelamkan bangunan atau memecah kapal pemain |
| **Rob Labuhan** | Gelombang pasang di zona pesisir tertentu | Pindahkan logistik ke titik tinggi, bersihkan saluran, cari 3 paket bantuan di pesisir | Tidak memindahkan blok air; zona banjir dibuat sebagai efek visual/hazard sementara |
| **Abu dan Bara** | Abu tebal dan titik bara di hutan dekat zona vulkanik | Padamkan titik bara semu, evakuasi satwa, ambil benih, kumpulkan sampel abu | Tidak menyebarkan `FIRE`; api hanya visual atau hazard terlokalisasi di arena yang diizinkan |
| **Longsor Lereng** | Batu dan tanah bergerak dari lereng | Baca penanda jalur longsor, selamatkan peti ekspedisi, buka rute aman, ambil mineral dari area yang baru terbuka | Rockfall visual; opsi perubahan blok hanya di zona yang dirancang khusus |

Variasi lanjutan seperti hujan meteor, tornado besar, sinkhole acak, wabah atau bencana yang menutup seluruh peta tidak perlu masuk tahap awal. Tema Kala Nusantara lebih cocok dengan cuaca kepulauan, abrasi/rob, longsor, abu vulkanik, dan aktivitas pesisir daripada menumpuk seluruh bencana generik.

### 6.2 Mekanik agar tidak membosankan

Setiap event harus memilih objective dari beberapa kategori, bukan selalu “tunggu timer lalu pukul monster”:

- **Observasi:** cocokkan tiga tanda alam, tandai titik bahaya, atau bawa sampel ke peneliti.
- **Logistik:** antar makanan, batu, kayu, atau bahan perbaikan ke NPC/pos yang berbeda.
- **Penyelamatan:** bantu satwa atau warga menuju titik aman dengan berinteraksi pada lokasi, bukan memindahkan entitas berat terus-menerus.
- **Pertahanan:** bertahan di titik tertentu dan menghadapi sedikit elite mob.
- **Eksplorasi:** cari petunjuk, mineral, atau peti di zona yang hanya aktif selama event.
- **Pemulihan:** bersihkan 3–5 titik hazard, tanam bibit, atau antarkan bahan untuk memperbaiki fasilitas kampung.

Tiap run mengambil 2–4 objective acak dari daftar yang aman, dengan syarat jumlah pemain, wilayah, cuaca, dan status server. Objective harus memiliki cara alternatif agar pemain solo atau pemain HP tidak dirugikan.

### 6.3 Konsekuensi hasil yang bermakna

Jangan sekadar mengubah angka reward berdasarkan skor. Beri akibat yang dapat dirasakan pada aktivitas berikutnya:

- **Respons warga sangat baik:** hazard lebih singkat, satu bonus kontrak pedagang, lebih banyak sampel penelitian, dan kosmetik/gelar event.
- **Respons cukup:** hazard berakhir normal, tersedia pekerjaan pemulihan standar.
- **Respons rendah:** satu rute ekspedisi sementara tertutup atau pedagang membuka kontrak perbaikan tambahan; tidak ada rumah pemain yang dihancurkan.

Gunakan hasil untuk membuat *follow-up quest* pada jurnal. Contohnya, jika Longsor Lereng membuka jalur tambang, rute tersebut menjadi titik eksplorasi selama 30–60 menit; jika Rob Labuhan selesai, NPC pasar menawarkan kontrak menyelamatkan barang. Setelah waktu habis, rute kembali normal atau hanya marker yang berubah.

---

## 7. Dampak ke dunia: gunakan Impact Budget

Semua dampak harus melewati `WorldImpactPolicy`. Jangan menyebarkan aturan kerusakan ke setiap handler event secara terpisah.

### Level 0 — Atmosferik (default)

- Partikel, suara, title/actionbar, bossbar yang sudah dimiliki `KalaNotificationService`/`KalaEventManager`.
- Damage/slow/knockback yang dihitung server-side di area yang telah ditandai.
- Item hadiah sementara dan objective.
- **Tidak ada perubahan blok**.

### Level 1 — Perubahan lokal kecil (opt-in setelah playtest)

Hanya pada zona event terkurasi dan bukan area rumah/kampung:

- Maksimal 1–3 blok natural yang telah di-whitelist per benturan; total budget event dibatasi, misalnya 20 blok.
- Jangan pernah menyentuh block entity/container, redstone, bed, workstation, portal, tanaman pangan pemain, struktur NPC, claim, atau blok yang dikenal sebagai buatan pemain.
- Simpan snapshot material + block data sebelum perubahan dan catat identitas event serta posisi.
- Saat restore, kembalikan blok **hanya jika** blok sekarang masih sama dengan blok sementara yang dibuat event. Jika pemain telah mengubahnya, jangan overwrite; buat log untuk pemeriksaan.
- Jika tidak bisa memastikan sebuah blok adalah natural dan aman, jangan ubah blok tersebut.

### Level 2 — Kerusakan lingkungan terkurasi (bukan untuk peluncuran)

Hanya pada arena atau pulau event yang sengaja dibuat untuk menanggung perubahan. Maksimal beberapa perubahan yang telah dihitung sebelumnya, dengan snapshot, rollback, dan validasi. Jangan aktifkan di `kala_main` secara global.

### Kebijakan per world

| World | Impact budget awal | Aturan |
|---|---|---|
| `kala_main` | Level 0 | Lindungi spawn, rumah, kampung, landmark, dan jalur cerita. Jika ingin sedikit perubahan, hanya di arena yang dibatasi eksplisit setelah mekanisme restore lolos test. |
| `kala_resource` | Level 0; Level 1 hanya di event zone | Dunia bisa direset, tetapi jangan menjadikan reset sebagai alasan mengabaikan rumah sementara/loot pemain. |
| World staging | Level 1/2 sesuai skenario test | Tempat satu-satunya untuk uji kerusakan blok, restore, dan crash recovery sebelum produksi. |
| Nether/End | Tidak ikut event | Jangan mencampur event Kala dengan mekanik portal/Ender Dragon tanpa desain terpisah. |

GriefPrevention claim saja bukan bukti bahwa blok di luar claim adalah terrain alami. Untuk Level 1 perlu zona yang sudah ditandai, provenance blok yang dapat diandalkan, atau pendekatan konservatif yang tidak mengubah blok sama sekali.

---

## 8. Cara event membuat pemain mau ikut

### 8.1 Kontribusi bukan tombol untuk langsung membatalkan bencana

V5 saat ini menyebut target 100 poin sajen dapat meredakan event. Pertahankan sajen sebagai mekanik penting, tetapi ubah perannya menjadi **satu jalur mitigasi**, bukan satu tombol skip. Jika target tercapai terlalu cepat, gelombang/encounter menghilang dan pemain yang belum sempat hadir kehilangan pengalaman.

Gunakan tiga meter gabungan:

- **Kesiapan Desa:** persediaan, logistik, dan bantuan NPC.
- **Pengetahuan Kawah:** observasi, sampel, dan aktivasi pos pengamatan.
- **Penanganan Bahaya:** menuntaskan retakan, menghindari dampak besar, dan mengalahkan/menenangkan elite bila hadir.

Sajen meningkatkan Kesiapan Desa atau memperpendek durasi ancaman. Objective lain juga bisa memajukan kemajuan. Event tetap memiliki klimaks; kontribusi menentukan **seberapa baik komunitas mengendalikan konsekuensinya**, bukan apakah semua adegan terhapus.

### 8.2 Reward dan anti-exploit

- Beri hadiah dasar kecil kepada pemain yang mengikuti minimal satu objective dan masih berada di event zone pada waktu yang relevan.
- Reward kontribusi tambahan didasarkan pada objective berbeda, bukan jumlah item yang dikorbankan saja.
- Batasi poin dari satu material/pemain per interval; gunakan diminishing returns setelah ambang tertentu.
- Jangan memberi reward penuh untuk AFK, spam interaksi, atau pemain yang masuk sebentar ketika event hampir selesai.
- Simpan `event_id`, `player_uuid`, `objective_id`, `contribution`, dan `reward_claimed` dalam ledger; gunakan unique constraint/idempotent transaction.
- Hadiah ekonomi dan loot perlu diuji terhadap inflasi. Material langka tidak boleh keluar tanpa batas dari boss farm.

### 8.3 Jalur kontribusi yang setara

Sediakan setidaknya tiga cara menyumbang:

1. **Kepala logistik:** mengantarkan bahan/pangan.
2. **Pengamat dan peneliti:** menemukan petunjuk dan sampel.
3. **Tim lapangan:** menghadapi hazard/elite dan menyelesaikan rescue/repair.

Kekuatan atau hadiah total tidak boleh hanya bergantung pada damage combat; pemain Bedrock dengan kontrol sentuh harus memiliki objective yang kompetitif dan nyaman.

---

## 9. Penjadwalan event dan pencegahan spam

- Jangan memulai event besar ketika server baru boot, sedang melakukan reset dunia, atau jumlah pemain aktif di bawah kebutuhan minimum.
- Gunakan periode acak terkontrol dan jendela jadwal. Pengumuman pendahuluan harus muncul sebelum event.
- Jika tidak ada pemain aktif, simpan event sebagai `PENDING`; jangan membuangnya atau menjalankan damage ketika tidak ada orang.
- Terapkan cooldown global di antara event besar dan cooldown per tipe, supaya Gunung Murka tidak mengalahkan semua aktivitas lain.
- Hindari menjalankan dua event yang bertabrakan pada zona dan komponen UI yang sama.
- Jika pemain berada jauh atau di world lain, mereka bisa menerima ringkasan di `/kompas` atau jurnal—bukan semua spam title.
- Simpan state event dan jadwal dengan benar saat restart. Jika server crash ketika event aktif, startup recovery harus memilih aman: membersihkan entity/visual sementara, menyelesaikan atau membatalkan event, dan tidak memberi reward dua kali.

**Baseline playtest untuk awal:** satu event kecil per 1–2 sesi bermain komunitas dan satu event besar per beberapa sesi; setelah data menunjukkan retensi/partisipasi, jadwalkan lebih rapat. Jangan menetapkan interval 24 jam hanya karena terdengar menarik sebelum mengetahui pola online pemain.

---

## 10. Performance budget untuk Java dan HP Bedrock

Nilai awal yang dapat diuji (bukan batas universal):

- Maksimal 4–6 batu bergerak per salvo dan maksimal 8–12 entity visual/hazard sementara yang hidup bersamaan di satu arena.
- Maksimal 1 boss + 2–4 add; gunakan cap entity yang ketat dan jangan spawn saat arena sudah penuh.
- Emit partikel hanya pada lokasi event dan dekat pemain; kurangi jumlah/density untuk area dengan banyak pemain.
- Hindari mengulang scan seluruh world setiap tick. Hitung daftar pemain/target dan zona saat event dimulai, lalu perbarui per interval kecil.
- Gunakan scheduler Paper yang sesuai, jangan melakukan operasi disk/SQLite berat di main thread. Untuk transaksi event penting, lakukan operasi database dengan pola yang aman dan jangan memanggil Bukkit world/entity API dari thread async.
- Semua entity sementara menyimpan tag `event_id`; sediakan `cleanupEvent(eventId)` yang idempotent.
- Ukur p95 MSPT, TPS, heap, entity count, packet/visual stability, dan jumlah efek yang benar-benar terlihat di Bedrock. Median saja tidak cukup untuk mendeteksi spike.

Tes perangkat minimum: Java low-to-mid PC, Android MCPE kelas bawah/menengah, serta layar sentuh pada resolusi rendah. Jika visual tidak terlihat sama, pilih visual fallback daripada memaksakan efek Java-only.

---

## 11. Plugin dan source code: mana yang dipelajari, mana yang diinstal?

Karena KalaCore sudah memiliki event manager dan database, jangan memasang beberapa plugin bencana sekaligus di server produksi. Gunakan plugin eksternal sebagai **referensi perilaku** pada staging, lalu port ide yang berguna ke API internal.

| Kandidat | Apa yang bisa dipelajari | Status untuk Paper 26.3 menurut listing yang diperiksa pada 24 Okt 2026 | Saran |
|---|---|---|---|
| [Deadly Disasters](https://modrinth.com/plugin/deadly-disasters) | Katalog bencana, konfigurasi, objektif, custom mobs, dan cara mengaktifkan/menonaktifkan tipe event | Listing mencantumkan hingga 26.2, bukan 26.3; lisensi GPL-3.0-only | Jangan salin jar langsung ke produksi. Untuk eksperimen, periksa repo, build staging, audit perubahan blok, lalu port hanya konsep yang dibutuhkan. Patuhi GPL jika mendistribusikan fork/derivatif. |
| [Disaster SMP / DMSP](https://modrinth.com/plugin/disaster-smp) | Contoh desain debris visual, scheduler, shelter/ward, boss, dan reward | Listing mencantumkan 26.1–26.1.1 dan 1.21.x, bukan 26.3 | Referensi desain; jangan anggap kompatibel dengan build server ini tanpa port dan tes. Listing-nya menyebut `BlockDisplay`, sehingga batasan Geyser tetap harus diuji. |
| [MythicMobs](https://modrinth.com/plugin/mythicmobs/versions) | Desain skill/mob/boss melalui konfigurasi | Versi yang terlihat mencantumkan hingga 26.2, bukan 26.3; lisensi ARR | Jangan menjadikannya dependensi sebelum ada build yang mendukung versi server. Karena proprietary/ARR, gunakan dokumentasi/API/config yang diizinkan; jangan menganggap boleh mendekompilasi atau mendistribusikan modifikasi. |
| [Paper Display Entities API](https://docs.papermc.io/paper/dev/display-entities/) | Cara menggerakkan props, text, dan objek visual di Java | API resmi Paper; interoperabilitas animasi dinamis pada Bedrock perlu tes | Pakai untuk dekorasi statis/visual yang aman dan fallback Java; jangan jadikan satu-satunya implementasi rockfall lintas platform. |
| [Paper `FallingBlock` API](https://jd.papermc.io/paper/26.2/org/bukkit/entity/FallingBlock.html) | Entitas batu vanilla, kontrol drop, dan konfigurasi damage | API Paper; cocok untuk proof-of-concept tetapi harus diuji pada build 26.3 yang digunakan | Kandidat utama untuk prototipe batu, dengan damage off, drop off, cancel drop, event tagging, dan penghapusan saat impact. |

**Kesimpulan plugin:** untuk rilis pertama, implementasikan event di KalaCore dan gunakan entitas vanilla. Jalankan plugin bencana eksternal hanya di server staging yang terpisah. Jangan memasang `Deadly Disasters` dan `DMSP` sekaligus; kedua proyek memiliki skala fitur yang lebih luas dari kebutuhan desain Kala dan menambah area integrasi yang perlu diaudit.

### Catatan reverse-engineer-anything / reverse-engineering skill

Repo skill yang dimaksud, [juank115/reverse-engineerskill](https://github.com/juank115/reverse-engineerskill), menjelaskan alur enam tahap: triage, static analysis, disassembly/decompilation, dynamic analysis, behavioral analysis, dan reporting. Skill ini adalah panduan kerja, bukan jaminan bahwa plugin yang dianalisis aman atau otomatis cocok dengan Paper 26.3.

Workflow yang dianjurkan untuk AI lokal:

1. **Inventory dahulu:** catat nama plugin, versi, sumber unduhan, lisensi, hash SHA-256, dependency, versi server yang dinyatakan, dan fungsi yang ingin dipakai.
2. **Utamakan source resmi:** clone repo, lihat tag/release dan commit; jangan langsung mengubah JAR produksi.
3. **Static review:** baca `plugin.yml`, `paper-plugin.yml` bila ada, konfigurasi, API yang digunakan, operasi file/network, listener block-break/block-place/explosion, scheduler, dan entity spawn. Jangan menganggap hasil pencarian string saja cukup untuk menyimpulkan keamanan.
4. **Build dan jalankan terisolasi:** buat folder/server staging yang terpisah, data pemain dummy, backup dan prosedur rollback. Plugin mendapat hak eksekusi penuh di proses server, jadi jangan menguji binary yang tidak dipercaya di instance produksi. Untuk sample yang benar-benar mencurigakan, ikuti execution gate dan isolasi yang dijelaskan skill; jangan jalankan sampel asing langsung di host.
5. **Port atau extend:** jika source open-source dan lisensi mengizinkan, buat fork sendiri dan catat perubahan. Jika proprietary/ARR, gunakan konfigurasi, API, add-on, atau minta izin; skill reverse-engineering bukan izin untuk mengabaikan lisensi.
6. **Diff dan acceptance tests:** build baru harus bisa dibandingkan dengan upstream, lulus tes Paper 26.3, tidak membuat file/world di luar root, tidak menghapus data, dan tidak menambah listener konflik. Simpan report per versi.
7. **Promosi terkontrol:** backup, stop server bersih, pasang build terverifikasi, cek log startup, lakukan tes Java + Bedrock, lalu pantau spark/MSPT dan siapkan rollback.

Jangan meminta AI “install plugin lalu custom sampai berhasil” secara bebas. Berikan pagar: **tidak menyentuh production world, tidak mengganti dependency tanpa persetujuan, tidak mengubah `server.properties`/auth, tidak menghapus file atau world, dan tidak mengklaim tes lulus tanpa log bukti.**

---

## 12. Prompt implementasi untuk AI lokal

Salin prompt ini ke agent yang memiliki akses ke repository KalaCore dan server staging. Ganti path sesuai layout nyata yang sudah ada.

> Kamu bertindak sebagai senior PaperMC gameplay engineer untuk project Kala Nusantara. Baca report V5 dan source code aktual sebelum membuat perubahan. Jangan menganggap isi report sebagai bukti bahwa setiap file/fitur masih sama; cocokkan nama kelas, database, konfigurasi, plugin.yml, dan log runtime.
>
> **Tujuan:** mengembangkan encounter Gunung Murka dan event musiman secara modular tanpa merusak fitur V5, menjaga crossplay Java/Bedrock, dan meminimalkan beban server/mobile.
>
> **Urutan wajib:**
> 1. Buat inventaris source, plugin, dependency, lisensi, build target, dan current event flow. Laporkan temuan sebelum coding besar.
> 2. Buat backup source/config/data staging. Jangan akses/mengubah world produksi atau menghapus dunia.
> 3. Audit `VolcanoManager`, `KalaEventManager`, `KalaNotificationService`, `KalaScoreboardManager`, `MenuManager`, SQLite schema, `GriefPrevention` integration, dan jadwal musim. Pertahankan satu pemilik UI.
> 4. Rancang `VolcanoEncounterDirector`, `HazardDirector`, `WorldImpactPolicy`, dan `EventContributionLedger` sebagai modul yang terpisah. Reuse state machine lima fase V5; tambah subfase hanya di dalam `PUNCAK`.
> 5. Implementasikan dahulu mode `DRY_RUN` / staging-only: tulis log prediksi zona, jumlah entity/partikel, reward, dan perubahan blok yang ditolak; jangan mengubah blok nyata pada awalnya.
> 6. Buat proof-of-concept rockfall memakai `FallingBlock` vanilla. Atur agar tidak memberi damage otomatis, tidak drop item, dan tidak menjadi blok. Tag entity sebagai milik event dan bersihkan di impact/timeout/stop. Jika tidak kompatibel di Bedrock, gunakan fallback partikel + hazard telegraph.
> 7. Tambah mob dengan vanilla entity dulu. Tidak menggunakan custom entity eksperimental untuk objective wajib. Spawn hanya di event arena; batasi boss/add count; serangan harus punya telegraph; jangan menyalakan api, membuat ledakan blok, atau menghancurkan struktur.
> 8. Buat dua event musiman sebagai MVP, masing-masing punya 2–4 variasi objective, aturan zona, reward, dan recovery. Ambil musim yang benar dari implementasi sekarang; jangan membuat plugin musim baru.
> 9. Semua perubahan world wajib lewat `WorldImpactPolicy`. Default `kala_main` = no block mutation. Bila Level 1 diuji, gunakan zona eksplisit, whitelist natural, snapshot, budget, audit log, dan restore hanya jika blok belum diubah pemain.
> 10. Tambahkan idempotency key agar reward tidak dobel. Jangan memberikan reward berdasarkan item yang dibayar saja; kontribusi harus bisa berasal dari observasi, logistik, rescue, combat, dan pemulihan.
> 11. Buat unit/integration tests untuk: event cancel, restart ketika event aktif, reset world, pemain logout/masuk, reward ganda, entity cleanup, player masuk zona terlambat, player di claim, block entity, jumlah mob maksimal, dan Bedrock fallback.
> 12. Uji di Paper build persis yang dipakai staging, satu klien Java dan minimal satu klien Bedrock. Rekam versi, langkah reproduksi, log, spark/MSPT sebelum/sesudah, entity peak, dan hasil visual. Jangan menulis “terverifikasi penuh” tanpa bukti.
>
> **Hard limits:** jangan mengubah auth/online-mode, port, firewall, permission, backup retention, ekonomi, atau reset resource world kecuali perubahan itu secara eksplisit diperlukan dan disetujui. Jangan download/run JAR asing di production. Jangan mengubah file pemain; jangan menghapus world. Jangan memasang dua plugin yang mengambil alih event/scoreboard/notifikasi yang sama. Jika kompatibilitas tidak jelas, hentikan tahap integrasi, laporkan risiko, dan lanjutkan dengan fallback internal.
>
> **Output:** (A) audit awal, (B) desain arsitektur sebelum coding, (C) diff yang mudah direview, (D) tests + log, (E) daftar fitur yang belum teruji, (F) rollback steps, (G) update dokumen report. Implementasi bertahap; jangan mengerjakan semua fitur sekaligus.

---

## 13. Acceptance checklist sebelum Closed Alpha

### Dunia dan keselamatan
- [ ] Tidak ada perubahan blok di `kala_main` selama dry-run, salvo batu, monster, dan recovery.
- [ ] Event menghormati zona aman, claim, block entity, dan area spawn.
- [ ] Tidak ada api/ledakan yang merusak blok atau menular ke struktur.
- [ ] Semua entity, task, particle loop, dan hazard memiliki cleanup path, termasuk setelah restart/crash.
- [ ] Reset `kala_resource` tidak bisa berjalan bersamaan dengan event yang memakai world itu.

### Gameplay
- [ ] Pemain dapat menghindari rockfall melalui penanda yang jelas.
- [ ] Event tetap dapat diselesaikan tanpa membunuh boss.
- [ ] Ada objective solo/mobile dan objective co-op.
- [ ] Hasil event mengubah quest/kontrak/loot dengan cara terbatas dan terukur.
- [ ] Reward tidak dobel ketika reconnect/restart atau saat perintah diulang.

### Java–Bedrock
- [ ] Batu terlihat dari kedua platform; arah dan waktu dampak masuk akal.
- [ ] Title, bossbar, actionbar, dan GUI tidak saling menutupi secara berlebihan.
- [ ] Mob khusus tetap terlihat dan dapat dilawan tanpa client mod.
- [ ] Resource pack Java dan Bedrock diuji sendiri-sendiri bila dipakai.
- [ ] Jika suatu visual hanya bekerja di Java, ada fallback yang wajar di Bedrock.

### Performa dan operasional
- [ ] Ada batas jumlah mob/batu/partikel dan konfigurasi cooldown.
- [ ] Pengukuran mencakup p95 MSPT, entity peak, heap, dan observasi klien, bukan hanya TPS/median.
- [ ] Shutdown saat event aktif menyelesaikan cleanup tanpa entity permanen atau reward duplikat.
- [ ] Backup/restore staging diuji sebelum perubahan dipromosikan ke produksi.

---

## 14. Rujukan web

1. [PaperMC — Display Entities](https://docs.papermc.io/paper/dev/display-entities/) — display entity, transformasi dan interpolasi visual.
2. [Paper API — FallingBlock](https://jd.papermc.io/paper/26.2/org/bukkit/entity/FallingBlock.html) — kontrol block data, drop, damage, dan cancel drop.
3. [GeyserMC — Entity API](https://geyser.kastle.dev/wiki/geyser/custom-entities/) — API custom entity masih eksperimental dan memerlukan definisi/asset Bedrock.
4. [GeyserMC — Issue #6723](https://github.com/GeyserMC/Geyser/issues/6723) — laporan animasi perubahan posisi display entity pada Bedrock, dibuka 18 September 2026.
5. [Deadly Disasters — Modrinth](https://modrinth.com/plugin/deadly-disasters) — contoh variasi bencana; listing yang diperiksa mencantumkan sampai Minecraft 26.2 dan GPL-3.0-only.
6. [Disaster SMP / DMSP — Modrinth](https://modrinth.com/plugin/disaster-smp) — contoh desain debris, shelter, wards, boss, dan reward; listing yang diperiksa mencantumkan 26.1–26.1.1/1.21.x.
7. [MythicMobs — Modrinth versions](https://modrinth.com/plugin/mythicmobs/versions) — referensi custom mob/skill; listing yang diperiksa belum mencantumkan 26.3.
8. [reverse-engineerskill — GitHub](https://github.com/juank115/reverse-engineerskill) — workflow triage, static analysis, dynamic/behavioral analysis, dan reporting.
9. [PaperMC — Adding plugins](https://docs.papermc.io/paper/adding-plugins/) — catatan bahwa plugin diberi akses luas ke proses server sehingga sumbernya harus dipercaya.

**Kesimpulan:** mulai dengan rockfall terkontrol, satu encounter monster opsional, dan dua event musiman yang masing-masing punya objective. Jadikan event terasa seperti ekspedisi kolaboratif dengan persiapan, keputusan, bahaya yang bisa dihindari, serta tindak lanjut di dunia—bukan bencana yang terus menghancurkan peta. Simpan modifikasi blok untuk arena khusus setelah mekanisme audit dan restore benar-benar lulus tes.
