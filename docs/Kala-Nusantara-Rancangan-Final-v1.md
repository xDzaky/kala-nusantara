# Kala Nusantara — Rancangan Final Sistem & Mekanisme Server

**Versi:** Rancangan konsep 1.0  
**Tanggal riset:** 24 Oktober 2026  
**Target:** Minecraft Java Edition + Bedrock/MCPE, pemain Indonesia dan komunitas global  
**Status:** Blueprint konsep; belum menjadi spesifikasi kode final

---

## 1. Keputusan utama

Kala Nusantara adalah **Co-op Survival RPG bertema kepulauan Nusantara**. Pemain membangun kampung, menjelajah pulau, menjalani profesi, berdagang, menemukan koleksi, membantu proyek komunitas, dan menghadapi satu siklus bencana alam yang bisa diprediksi setiap musim.

Pembeda utama bukan jumlah plugin, custom item, atau quest. Pembeda Kala adalah dunia yang terasa berubah, komunitas yang punya tujuan bersama, dan risiko yang bisa dipilih tanpa memaksa pemain casual kehilangan hasil kerja berbulan-bulan.

### Prinsip desain yang tidak boleh dilanggar

1. **Bangunan rumah utama tidak dihapus oleh reset musim.** Dunia kampung adalah tempat menetap jangka panjang.
2. **Bencana diumumkan, dapat dipahami, dan dapat dipersiapkan.** Tidak ada kehancuran tanpa penjelasan atau pemulihan yang jelas.
3. **Risiko bersifat sukarela dan tertera sebelum pemain mengonfirmasi klaim.** Area berisiko bukan jebakan.
4. **Co-op menjadi aktivitas utama.** PvP hanya di arena atau event yang dipilih pemain.
5. **Sesi pendek tetap bernilai.** Pemain yang hanya online 10–20 menit tetap bisa menyelesaikan kegiatan berguna.
6. **Tidak ada pay-to-win.** Uang asli tidak membeli kekuatan tempur, akses sumber daya terbaik, atau keunggulan ekonomi.
7. **Java dan Bedrock adalah pemain setara.** Tidak ada mekanisme penting yang hanya nyaman dimainkan dengan mouse/keyboard atau mod klien.
8. **Gameplay harus tetap menarik tanpa resource pack custom.** Pack hanya memperkuat presentasi, bukan menjadi syarat utama bermain.
9. **Fitur boleh banyak, proses server harus sedikit dan terukur.** Gunakan satu sumber data dan sistem bersama; hindari banyak plugin yang mengulang pekerjaan serupa.

## 2. Core loop yang membuat pemain ingin kembali

### Loop sesi pendek: 10–20 menit

Pemain masuk, membuka Kompas Pengembara, lalu memilih satu aktivitas kecil:

- menyelesaikan pesanan pasar atau permintaan kampung;
- menanam, memanen, memancing, atau membuat barang;
- mengirim material untuk proyek bersama;
- mengambil satu rute eksplorasi dan mencatat penemuan;
- memperbaiki satu fasilitas atau mengklaim hadiah aktivitas;
- mengikuti event singkat yang sedang aktif.

Hasilnya berupa Kepeng, progres profesi, catatan eksplorasi, reputasi kampung, atau kosmetik. Pemain tidak dipaksa menyelesaikan semua aktivitas setiap hari.

### Loop mingguan

Setiap minggu ada satu fokus yang berganti, misalnya pasar rakyat, lomba kapal, festival panen, ekspedisi reruntuhan, kontes bangunan, atau latihan evakuasi. Fokus mingguan mengubah tujuan komunitas, bukan memaksa seluruh pemain mengulangi quest identik.

### Loop musim: 8 minggu

| Fase | Pengalaman pemain | Perubahan dunia |
|---|---|---|
| Minggu 1–4: Masa Berkembang | Bangun rumah, profesi, eksplorasi, proyek kampung, event mingguan | Permintaan pasar dan lokasi penemuan berubah bertahap |
| Minggu 5: Tanda Kala | Menemukan retakan, asap, perubahan cuaca, dan petunjuk pulau baru | Peta risiko serta daftar persiapan dibuka |
| Minggu 6: Persiapan | Membantu tanggul, tempat evakuasi, lumbung, dan jalur kapal | Proyek komunitas memengaruhi hadiah dan kualitas pemulihan, bukan keselamatan rumah di zona aman |
| Minggu 7: Peristiwa Kala | Evakuasi, ekspedisi, misi penyelamatan, aktivitas bertahap | Efek dunia terjadwal; wilayah perbatasan dan pos berisiko terdampak |
| Minggu 8: Pemulihan | Memperbaiki pos, menanam kembali, festival, membuka catatan musim | Kesuburan sementara, jalur eksplorasi, dan Pulau Seberang baru |

Empat minggu awal tidak boleh kosong. Event mingguan, pesanan pasar, proyek kampung, koleksi, dan penemuan kecil tetap berjalan.

Untuk pemain global, event besar sebaiknya memiliki **beberapa jendela partisipasi** atau aktivitas pendamping yang setara sepanjang 24–48 jam, bukan satu jam yang hanya cocok untuk satu zona waktu. Hasil utama musim tidak boleh ditentukan hanya oleh siapa yang bisa online pada jam tertentu.

## 3. Dunia dan progresi

### Dunia tetap dan dunia yang direset

Gunakan tiga jenis wilayah secara konseptual:

1. **Kepulauan Kampung — persisten.** Rumah, kampung, proyek komunitas, dan bangunan penting tidak direset setiap musim.
2. **Rimba dan Tambang Kala — dunia sumber daya.** Tempat menambang, menebang, dan mengumpulkan bahan. Area ini direset terjadwal setelah pengumuman dan pemeriksaan pemain yang masih berada di dalamnya.
3. **Pulau Seberang — wilayah musiman.** Lokasi eksplorasi yang berubah atau berganti tiap musim. Hadiah utamanya penemuan, bahan musiman, lore, blueprint dekoratif, dan kosmetik; bukan senjata yang membuat pemain lama tidak tertandingi.

Di fase pertama, dunia-dunia tersebut dapat berupa beberapa world di satu backend Paper. Jangan langsung membuat jaringan multi-server dan proxy sebelum jumlah pemain atau beban benar-benar membutuhkan pemisahan.

### Kampung dan klaim tanah

- Setiap pemain mendapat lahan awal yang kecil tetapi layak untuk hidup.
- Klaim dapat menjadi kampung bersama dengan izin yang jelas: pemilik, pengelola, pembangun, dan tamu.
- Fasilitas kampung seperti dermaga, lumbung, taman, balai, dan rumah singgah menjadi proyek kolaboratif.
- Perluasan klaim berasal dari progres permainan dan kontribusi, bukan pembayaran uang asli.
- Klaim memiliki batas ukuran dan batas entitas/farm. Batas ini diperlukan untuk mencegah satu pemain membebani performa dunia.
- Pemilik dapat mengatur status aman atau terekspos untuk bangunan yang terdaftar mengikuti event berisiko.

### Koleksi dan penemuan

Tambahkan **Jurnal Kepulauan** sebagai progres jangka panjang yang ringan: ikan, tanaman, material, lokasi, cerita NPC, resep, landmark, dan pencapaian komunitas. Koleksi tidak perlu memberikan bonus statistik besar. Hadiah utamanya gelar, dekorasi, skin alat, judul di arsip musim, dan akses ke cerita/blueprint kosmetik.

Jurnal memberikan target untuk penjelajah, completionist, serta pemain yang tidak ingin bertarung atau grinding.

## 4. Mekanisme Kala: bencana yang berkesan tanpa menghukum pemain

Bencana adalah fitur khas server, tetapi juga risiko implementasi tertinggi. Rancangan final memisahkan **efek dunia**, **risiko bangunan**, dan **pemulihan barang** agar tidak menjadi satu operasi besar yang rentan bug.

### Tiga tingkat risiko

| Zona | Fungsi | Perlakuan final |
|---|---|---|
| Hijau — Kampung Aman | Tempat tinggal utama dan pemula | Tidak ada penghancuran bangunan oleh Kala. Efek abu, langit, suara, NPC, dan misi evakuasi bersifat atmosferik atau kosmetik. |
| Kuning — Sabuk Terdampak | Eksplorasi dan bangunan yang memilih ikut risiko | Dampak terbatas pada bagian/struktur yang didaftarkan untuk event. Pemain menerima laporan dan jalur pemulihan yang jelas. Jangan membuat seluruh klaim terkena kerusakan acak tanpa persetujuan yang eksplisit. |
| Merah — Garis Depan | Pos ekspedisi, sumber daya langka, dan tantangan sukarela | Risiko kerusakan tertinggi hanya pada pos atau plot yang secara eksplisit ditandai terekspos. Jangan jadikan ini tempat wajib untuk rumah utama atau penyimpanan barang penting. |

Peringkat zona harus terlihat sebelum klaim dibuat, tetapi **opsi risiko pada masing-masing plot juga harus ditampilkan**. Lokasi berbahaya tidak boleh menyembunyikan aturan penting dari pemain.

### Dampak yang benar-benar mengubah permainan

Agar Kala tidak terasa hanya sebagai pergantian cuaca, setiap musim mengubah beberapa hal yang terukur:

- rute ke Pulau Seberang terbuka atau tertutup;
- simpul sumber daya tertentu berpindah atau beregenerasi;
- permintaan pasar dan pesanan NPC berubah;
- proyek komunitas menentukan kualitas fasilitas umum dan bonus pemulihan;
- lokasi yang terdampak menghasilkan puing dan bahan untuk crafting;
- setelah peristiwa, area tertentu mendapat bonus pertanian sementara;
- lore dan Jurnal Kepulauan membuka catatan baru.

Kehancuran bukan satu-satunya cara menunjukkan dampak. Perubahan akses, ekonomi, misi, sumber daya, dan visual juga dapat terasa bermakna.

### Snapshot, Brankas Pengungsian, Puing, dan pemulihan

Konsep awal tentang snapshot serta Brankas Pengungsian tetap dipertahankan, tetapi pelaksanaannya harus dibatasi:

1. Hanya klaim/pos yang secara jelas ikut risiko yang masuk antrean snapshot.
2. Snapshot dibuat dalam persiapan bertahap, bukan semua sekaligus tepat sebelum letusan.
3. Ukuran klaim, jumlah container, dan fitur blok yang didukung harus dibatasi serta dites.
4. Proses pemulihan dijalankan melalui antrean per wilayah/chunk dengan batas kerja per tick.
5. Pemain menerima **Laporan Dampak** yang mencatat struktur terdampak, item yang diamankan, biaya pemulihan, dan opsi tindakan.
6. Item tidak boleh ada di bangunan hasil restore dan Brankas Pengungsian sekaligus. Setiap proses transfer/pemulihan memerlukan ID transaksi yang dapat ditelusuri dan aman saat dicoba ulang.
7. Simpan salinan cadangan server terpisah. Snapshot fitur gameplay bukan pengganti backup.
8. Uji pemulihan untuk peti, hopper, banner, pintu, tangga, sign, item berkomponen, dan blok ber-entity sebelum memperluas cakupan.

**Batas penting:** jangan membangun sistem penghancuran semua klaim untuk MVP. Versi awal Kala dapat menguji jadwal, pengumuman, efek, kontribusi, dan pemulihan pada satu area uji kecil. Pemulihan struktural massal baru diaktifkan setelah uji beban dan uji duplikasi item lulus.

### Pengamanan untuk pemain yang jarang online

- Klaim rumah utama tidak dihapus karena absen.
- Pemain yang kembali mendapat ringkasan perubahan musim, petunjuk singkat, dan pilihan tugas catch-up.
- Bonus kembali tidak boleh cukup besar untuk membuat pemain menciptakan akun alternatif demi farming hadiah.
- Pemulihan klaim tidak kedaluwarsa hanya karena pemain tidak login.
- Jangan memberi streak harian yang membuat pemain merasa dihukum ketika absen.

## 5. Aktivitas, profesi, dan pemain dengan gaya berbeda

### Profesi awal

Mulai dengan empat profesi dari dokumen dasar:

- **Pandai:** crafting, perbaikan alat, bahan konstruksi, dan bantuan pemulihan.
- **Tabib/Peramu:** ramuan, makanan, perawatan, serta pengurangan efek bahaya tertentu.
- **Pelaut:** kapal, logistik antarpulau, rute perdagangan, dan kontribusi evakuasi.
- **Penjelajah:** pemetaan, pendeteksian lokasi, pengungkapan lore, dan ekspedisi.

Pertanian, memancing, membangun, dan perdagangan tetap berguna bagi semua pemain, tidak dikunci oleh satu profesi. Profesi menjadi spesialisasi, bukan izin eksklusif untuk melakukan aktivitas dasar.

### Aturan progresi profesi

- Level awal cukup 1–15 dengan sedikit perk yang mudah dipahami.
- XP berasal dari aktivitas wajar, bukan farming mob tanpa henti.
- Perk memberikan utilitas, pilihan crafting, kemudahan kerja, atau kosmetik; jangan memberi kekuatan tempur yang wajib dimiliki untuk berkompetisi.
- Pemain dapat mengganti jalur dengan biaya ringan atau pada batas musim. Jangan membuat pilihan profesi pertama menjadi hukuman permanen.
- Konten awal cukup memiliki 2–3 perk yang benar-benar terasa untuk setiap profesi. Skill tree panjang bisa datang setelah beta.

### Aktivitas tanpa tekanan kompetitif

- kontes bangunan dengan kategori berbeda;
- fishing dan festival panen;
- ekspedisi penemuan;
- pesanan NPC dan kontrak antarkampung;
- proyek bersama untuk dermaga, lumbung, mercusuar, kebun, atau jalur evakuasi;
- koleksi, cerita, dekorasi, dan arsip musim;
- arena PvP opsional yang terpisah.

Tidak semua hadiah harus berupa uang atau item kuat. Variasi aktivitas, pengakuan kontribusi, cerita, dan dekorasi juga menjadi reward.

## 6. Ekonomi yang sederhana dan sulit dieksploitasi

Mata uang utama bernama **Kepeng**. Ekonomi harus menjadi lapisan pendukung aktivitas, bukan pekerjaan kedua yang harus dimainkan setiap hari.

### Tahap pertama

- Mulai dari beberapa pesanan NPC dan jual-beli bahan dasar dengan harga yang jelas.
- Tambahkan transaksi pemain yang aman dan terekam setelah jalur dasar stabil.
- Gunakan sink ekonomi yang mudah dimengerti: biaya perbaikan, bahan proyek umum, dekorasi, ongkos layanan, dan biaya listing yang ringan bila pasar pemain sudah ada.
- Catat semua transaksi ekonomi pada ledger: sumber uang, pengeluaran, pengirim, penerima, waktu, dan alasan.
- Batasi duplikasi hadiah dari alt account, spam event, reconnect, dan transaksi yang diulang ketika terjadi lag.

### Tunda fitur ekonomi ini

Jangan memulai beta dengan bursa pemain kompleks, harga dinamis rumit, puluhan mata uang, pajak yang sulit dijelaskan, atau penghasilan otomatis tanpa batas. Fitur-fitur tersebut mudah menghasilkan inflasi dan memperbesar beban debugging.

Setelah ada data ekonomi nyata, harga NPC dapat menyesuaikan suplai secara perlahan dan transparan. Jangan menggunakan harga yang berubah drastis secara tersembunyi.

### Monetisasi

Hanya kosmetik yang tidak memberi keunggulan: skin alat, efek visual ringan, dekorasi rumah, pet kosmetik, gelar, atau tema UI. Hadiah gameplay inti tetap bisa diperoleh tanpa bayar. Hindari menjual Kepeng, perlindungan tambahan yang wajib, slot kekuatan, atau item langka kompetitif.

## 7. Onboarding dan satu pusat navigasi

Kompas Pengembara tetap menjadi pusat navigasi dengan menu inti:

1. **Lanjutkan** — misi yang relevan dan tindakan berikutnya.
2. **Kampung** — klaim, anggota, fasilitas, dan laporan.
3. **Jelajah** — peta risiko, dunia sumber daya, Pulau Seberang, Jurnal.
4. **Profesi** — progres dan pilihan perk.
5. **Musim Kala** — kalender, persiapan, kontribusi, dan laporan dampak.
6. **Panduan** — bantuan, pengaturan, dan bahasa.

Tutorial 20–30 menit tidak perlu menampilkan semua sistem. Ajarkan secara bertahap: bergerak dan mengumpulkan bahan, menetapkan rumah, membuka menu, menjalankan satu pesanan, memahami risiko, lalu menemukan satu aktivitas yang disukai.

Petunjuk muncul ketika pemain meminta bantuan atau berhenti lama di satu langkah; jangan spam chat dengan hint otomatis setiap beberapa detik.

## 8. Crossplay Java + Bedrock/MCPE sebagai syarat desain

### Aturan mekanik

- Tidak ada kewajiban memasang mod Fabric/Forge/NeoForge di sisi pemain.
- Jangan mendasarkan mekanik penting pada ketepatan klik kanan/kiri, kombinasi tombol, drag inventory yang rumit, atau command panjang.
- Navigasi Bedrock harus ramah sentuh: tombol jelas, teks pendek, konfirmasi untuk tindakan permanen, dan tidak terlalu banyak halaman.
- Gunakan satu model aksi dan data gameplay; tampilan Java dan Bedrock boleh berbeda.
- Java dapat memakai inventory GUI bila sesuai, sedangkan Bedrock menggunakan form Floodgate/Cumulus atau alternatif yang sudah dites.
- Seluruh UI wajib diuji di Java, Bedrock mobile, serta Bedrock Windows/konsol bila menjadi sasaran rilis.

### Resource pack dan custom asset

- Mulai dengan gameplay vanilla-compatible dan tekstur 16×16 atau 32×32 yang ringan.
- Prioritaskan ikon, bahan Kala, Kepeng, buku, dan kosmetik 2D; tunda custom entity, animasi, serta blok 3D rumit.
- Geyser tidak otomatis mengonversi resource pack Java menjadi pack Bedrock. Custom item membutuhkan resource pack Bedrock serta mapping yang sesuai.
- Rainbow dapat membantu menghasilkan mapping dan pack Bedrock dari konten custom Java, tetapi dokumentasi yang dicek pada tanggal riset menyebut proyek ini masih eksperimental. Karena itu, gunakan sebagai alat bantu, bukan jaminan bahwa semua item/blok akan tampil sempurna.
- Sediakan fallback yang masuk akal jika pack ditolak, gagal dimuat, atau tidak kompatibel dengan perangkat tertentu.

**Keputusan:** jangan menjadikan pembelian ItemsAdder/Oraxen atau 60 aset custom sebagai prasyarat untuk mulai membangun gameplay. Buat satu proof-of-concept item terlebih dahulu, validasi di Java dan Bedrock, baru putuskan pipeline aset.

## 9. Arsitektur teknis yang disarankan

### Fase awal

- **Server runtime:** Paper untuk server Java berbasis plugin.
- **Crossplay:** Geyser + Floodgate.
- **Permission:** LuckPerms.
- **Claim sementara:** GriefPrevention atau satu solusi claim lain yang kompatibel. Jangan menjalankan dua sistem claim bersamaan.
- **NPC:** Citizens jika dibutuhkan untuk tutorial.
- **Bangunan/schematic:** WorldEdit atau FAWE untuk pekerjaan bangunan, alat admin, dan eksperimen pemulihan. Jangan menganggap riwayat edit atau fitur snapshot alat admin sudah otomatis memenuhi semua kebutuhan Brankas Pengungsian.
- **Profiling:** gunakan spark yang tersedia pada Paper modern; jangan menambah profiler kedua tanpa alasan.
- **Analytics pemain:** Plan setelah ada beta dengan pemain sungguhan.
- **Resource pack:** pilih satu pipeline setelah proof-of-concept Java/Bedrock berhasil; jangan mencampur beberapa plugin custom-content di fase awal.
- **Database:** SQLite untuk prototipe; pindah ke MariaDB/MySQL ketika server produksi memerlukan operasi bersamaan, backup, dan administrasi lebih kuat.
- **Proxy Velocity:** baru dipertimbangkan ketika benar-benar diperlukan untuk memisahkan lobby/world atau mengelola beberapa backend.

### KalaCore sebagai satu codebase modular

Buat satu codebase custom plugin dengan modul yang dipisah jelas, bukan langsung enam plugin yang saling bergantung:

- `core`: konfigurasi, lifecycle, identitas pemain, event internal.
- `guide-ui`: Kompas, tutorial, bahasa, adapter Java/Bedrock.
- `season`: kalender dan fase Kala.
- `settlement`: klaim, izin, fasilitas, dan laporan kampung.
- `resource`: simpul sumber daya dan reset dunia sumber daya.
- `recovery`: snapshot, puing, brankas, dan antrean pemulihan; baru masuk setelah risikonya teruji.
- `profession`: XP dan perk profesi.
- `economy`: ledger Kepeng, NPC orders, dan transaksi.
- `telemetry`: metrik gameplay, tindakan administratif, anti-abuse, dan audit.

Modul-modul tersebut tidak harus dibuat sekaligus. Pada MVP cukup `core`, `guide-ui`, `season`, dan fungsi klaim dasar. Modul recovery yang rumit tidak boleh menjadi penghambat untuk membuktikan bahwa servernya menyenangkan.

### Prinsip implementasi data

- Pisahkan data pemain, klaim, musim, kontribusi, transaksi, dan transaksi pemulihan.
- Gunakan ID untuk event dan transaksi; aksi yang dicoba ulang tidak boleh menggandakan item atau uang.
- Database I/O dan kompresi data dapat dikerjakan di background sesuai batas aman API, tetapi perubahan world harus melalui mekanisme API/plugin yang aman dan terkontrol.
- Semua proses besar masuk antrean dan memiliki batas pekerjaan per tick.
- Hindari loop yang memeriksa semua pemain, semua klaim, atau seluruh blok setiap tick. Gunakan event, indeks, dan scheduler berinterval.
- Log keputusan admin dan pemulihan sehingga sengketa dapat ditelusuri.

## 10. Performa dan aksesibilitas perangkat

Target bukan sekadar “bisa dijalankan di laptop”, tetapi pengalaman stabil ketika beberapa sistem aktif bersamaan.

- Mulai dengan beta 10–20 pemain; naik ke 20–50 setelah hasil uji stabil.
- Jaga target 20 TPS dan gunakan 50 ms sebagai batas anggaran waktu satu tick. Target internal perlu lebih ketat pada beban normal agar tersedia ruang saat event.
- Mulai dari view-distance sekitar 6–8 dan simulation-distance 4–6, lalu sesuaikan memakai hasil profiling dan uji bermain. Ini titik awal pengujian, bukan angka universal.
- Pregenerate area utama agar eksplorasi ramai tidak terus memicu generasi chunk baru.
- Beri batas wajar untuk villager, mob, hopper, item jatuh, farm otomatis, dan mesin redstone.
- Efek letusan harus terjadwal dan dibatasi: suara, cuaca, partikel selektif, boss bar, dan kelompok blok yang sudah ditentukan. Hindari membanjiri dunia dengan entitas atau memicu simulasi lava/air skala besar.
- Reset dunia sumber daya dilaksanakan pada jadwal dengan pemeriksaan pemain, backup, dan laporan; jangan menghapus folder dunia ketika masih dipakai server.
- Lakukan backup otomatis dan tes pemulihan berkala. Backup yang belum pernah dicoba dipulihkan belum terbukti berguna.
- Uji resource pack pada ponsel Android kelas menengah dan jaringan seluler, bukan hanya PC developer.

## 11. Repo dan proyek yang layak dijadikan referensi

Daftar berikut adalah fondasi teknis dan contoh implementasi. **Jangan memasang semuanya sekaligus.** Cek lisensi, versi Minecraft, dependensi, aktivitas pengembangan, dan kompatibilitas plugin sebelum memasang pada produksi.

| Repo/proyek | Peran untuk Kala Nusantara | Keputusan |
|---|---|---|
| [PaperMC/Paper](https://github.com/PaperMC/Paper) | Runtime server plugin dan API pengembangan | Fondasi utama |
| [GeyserMC/Geyser](https://github.com/GeyserMC/Geyser) | Bridge Java–Bedrock | Wajib untuk crossplay |
| [GeyserMC/Floodgate](https://github.com/GeyserMC/Floodgate) | Integrasi pemain Bedrock dan API form | Wajib jika memakai alur autentikasi Floodgate dan form Bedrock |
| [GeyserMC/Rainbow](https://github.com/GeyserMC/Rainbow) | Membantu membuat mapping custom-content dan pack Bedrock | Eksperimen; uji satu item sebelum dipakai luas |
| [LuckPerms/LuckPerms](https://github.com/LuckPerms/LuckPerms) | Permission dan kelompok staf | Direkomendasikan |
| [GriefPrevention/GriefPrevention](https://github.com/GriefPrevention/GriefPrevention) | Klaim dan perlindungan anti-grief dasar | Fondasi sementara; pin branch/release stabil yang sesuai |
| [IntellectualSites/FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit) | Operasi world-editing dan pekerjaan blok besar | Alat admin/pemulihan yang diuji, bukan pengganti desain recovery |
| [EngineHub/WorldEdit](https://github.com/EngineHub/WorldEdit) | Schematic, clipboard, dan operasi bangunan | Referensi format dan alat builder |
| [CitizensDev/Citizens2](https://github.com/CitizensDev/Citizens2) | NPC tutorial dan tokoh seperti Mbah Kala | Opsional |
| [Oraxen/Oraxen](https://github.com/oraxen/oraxen) | Pipeline item dan resource pack custom | Pilih hanya setelah uji Bedrock; jangan diasumsikan konversi otomatis |
| [lucko/spark](https://github.com/lucko/spark) | Profiling server/client/proxy | Gunakan untuk mencari penyebab lag |
| [plan-player-analytics/Plan](https://github.com/plan-player-analytics/Plan) | Aktivitas dan analitik pemain | Tambahkan ketika beta dimulai |
| [jpenilla/squaremap](https://github.com/jpenilla/squaremap) | Peta dunia web ringan bergaya vanilla | Opsional; jangan prioritaskan sebelum core loop stabil |
| [itzg/docker-minecraft-server](https://github.com/itzg/docker-minecraft-server) | Menjalankan server dengan lingkungan yang reproducible | Berguna untuk local/dev atau deployment; ini image server, bukan panel hosting |
| [EssentialsX/Essentials](https://github.com/EssentialsX/Essentials) | Utilitas server umum | Opsional; aktifkan hanya fitur yang memang dibutuhkan |

Dokumentasi teknis yang perlu dibaca sebelum implementasi:

- [Paper — Getting Started](https://docs.papermc.io/paper/getting-started/)
- [Paper — Project Setup](https://docs.papermc.io/paper/dev/project-setup/)
- [Paper — Profiling](https://docs.papermc.io/paper/profiling/)
- [Geyser — Supported Versions](https://geysermc.org/wiki/geyser/supported-versions/)
- [Geyser — Custom Items](https://geysermc.org/wiki/geyser/custom-items/)
- [Geyser — Resource Packs](https://geysermc.org/wiki/geyser/packs/)
- [Geyser — Forms/Cumulus](https://geysermc.org/wiki/geyser/forms/)
- [FAWE — README/API](https://github.com/IntellectualSites/FastAsyncWorldEdit)

Pada tanggal riset, dokumentasi Geyser mencantumkan dukungan Java 26.3 dan Bedrock 26.30–26.52. Dokumentasi Paper menyatakan Minecraft 26.1+ memerlukan Java 25. Angka ini adalah kondisi saat riset, bukan janji untuk masa depan; **pilih versi server berdasarkan irisan kompatibilitas semua plugin**, lalu pin versi untuk beta. Jangan memperbarui server produksi otomatis setiap ada rilis.

## 12. Instalasi lokal yang realistis

Untuk eksperimen awal, cukup jalankan server lokal dengan:

1. Paper pada versi yang telah lolos tes kompatibilitas.
2. Geyser-Spigot dan Floodgate.
3. LuckPerms.
4. Satu plugin claim sementara, misalnya GriefPrevention.
5. Citizens bila tutorial memerlukan NPC.
6. WorldEdit/FAWE hanya bila sedang menguji pembangunan atau operasi wilayah.
7. Spark yang sudah dibundel Paper modern; tambahkan versi plugin terpisah hanya bila ada kebutuhan yang jelas.

Belum perlu memasang ItemsAdder/Oraxen, sistem profesi besar, auction house, banyak plugin quest, panel web, Velocity, atau semua modul custom secara bersamaan. Pertama buktikan bahwa pemain Java dan Bedrock dapat masuk, melihat instruksi, mengklaim lahan, dan memainkan satu siklus kecil tanpa lag.

Untuk server lokal di perangkat RAM terbatas, gunakan world kecil, batas pemain, dan jarak render rendah. Hindari menjalankan klien Minecraft, IDE berat, build, dan server berukuran besar secara bersamaan saat profiling.

## 13. Urutan pengembangan — berdasarkan risiko, bukan banyaknya fitur

### Tahap A — Vertical slice crossplay

Tujuan: membuktikan jalur pemain dari join hingga bermain.

- Paper + Geyser + Floodgate berjalan.
- Login, chat, teleport, inventori, permission, dan keluar/masuk diuji di Java serta Bedrock.
- Tutorial sederhana, Kompas versi dasar, satu klaim, satu pesanan NPC, satu proyek kampung kecil.
- Belum ada kerusakan struktural oleh Kala.

**Lulus bila:** UI dan tindakan utama setara di dua edisi, data tersimpan setelah restart, dan tidak ada duplikasi hadiah ketika pemain reconnect.

### Tahap B — Core loop survival

- World kampung persisten dan world sumber daya.
- Klaim, izin kampung, Kepeng sederhana, aktivitas harian/mingguan yang tidak menghukum absen.
- Satu profesi prototipe dan Jurnal Kepulauan kecil.
- Satu event non-destruktif yang mengubah akses, cuaca, permintaan pasar, dan tujuan komunitas.

**Lulus bila:** pemain yang tidak tertarik PvP tetap memiliki beberapa jalur progres yang jelas.

### Tahap C — Musim alpha

- Implementasi kalender musim delapan minggu.
- Event mingguan dan kalender Kala.
- Pulau Seberang atau satu wilayah event terbatas.
- Empat profesi awal, kontribusi kampung, dan arsip musim.
- Plan/telemetry untuk retensi dan analisis aktivitas.

**Lulus bila:** penguji tetap dapat menemukan tujuan setelah tutorial dan tidak harus login setiap hari.

### Tahap D — Recovery dan kerusakan terbatas

- Satu klaim uji dengan batas ukuran jelas.
- Snapshot, Brankas Pengungsian, restore, Puing, dan audit transaksi.
- Uji container dan blok kompleks, crash/restart, transaksi yang dicoba ulang, serta backup/restore.
- Simulasikan beban saat beberapa klaim dipulihkan dalam antrean.

**Lulus bila:** tidak terjadi duplikasi item, pemulihan tidak mematikan TPS, dan jalur restore/rollback dapat diandalkan. Barulah cakupan diperluas.

### Tahap E — Beta publik kecil

Mulai dengan sekitar 10–20 penguji dari perangkat dan gaya bermain berbeda, lalu naik menuju 20–50 jika metrik stabil. Uji bahasa, tutorial, ekonomi, event pada jam berbeda, perangkat Bedrock, dan prosedur moderasi.

### Tahap F — Rilis

Baru setelah core loop terbukti:

- finalisasi resource pack dan kosmetik;
- marketplace pemain yang aman;
- Discord bot untuk pengingat dan ringkasan musim;
- peta web opsional;
- panel pengelolaan hosting;
- monetisasi kosmetik yang transparan.

## 14. Metrik keberhasilan

Jangan menilai keberhasilan hanya dari jam bermain atau total login. Gunakan metrik yang membantu menjawab apakah pemain menemukan alasan yang menyenangkan untuk kembali.

- **Onboarding:** berapa banyak pemain menuntaskan langkah dasar dan berhasil mengklaim rumah.
- **Retensi D1/D7/D30:** berapa banyak pemain kembali setelah sesi pertama, satu minggu, dan satu bulan.
- **Retensi pasca-Kala:** berapa banyak pemain kembali setelah event dan berapa yang menyelesaikan pemulihan.
- **Keseimbangan aktivitas:** distribusi aktivitas membangun, eksplorasi, berkebun, perdagangan, dan event.
- **Ekonomi:** sumber dan pengeluaran Kepeng, median saldo, transaksi gagal, dan tanda inflasi.
- **Performa:** TPS/MSPT, penggunaan CPU/memori, entitas, chunk aktif, dan laporan spark pada jam ramai.
- **Crossplay:** error UI, kegagalan pack, laporan item tidak tampil, dan masalah input pada perangkat Bedrock.
- **Keamanan:** rollback griefing, transaksi ganda, eksploitasi klaim, dan waktu penyelesaian tiket.

Buat perubahan desain berdasarkan data dan umpan balik, bukan sekadar menambah konten ketika jumlah pemain turun.

## 15. Risiko utama dan mitigasi

| Risiko | Mitigasi |
|---|---|
| Pemain berhenti karena rumah hancur | Rumah utama terlindungi; risiko kerusakan opt-in; laporan dan pemulihan transparan |
| Pemulihan membuat duplikasi atau lag | Scope terbatas, antrean, ledger transaksi, uji crash/retry, profiling dan backup |
| Bedrock melihat item/GUI berbeda | UI adapter; form sederhana; pack dan mapping diuji pada perangkat nyata; fallback vanilla |
| Ekonomi menjadi grind atau inflasi | Mulai dengan pesanan sederhana; ledger; sink jelas; tidak ada uang otomatis tanpa batas |
| Terlalu banyak plugin atau modul | Satu sistem untuk setiap fungsi; inventaris dependensi; custom code untuk mekanik unik saja |
| Pengembangan terlalu luas untuk satu orang | Vertical slice dulu; tunda pasar besar, konten custom kompleks, proxy, dan destructible-world global |
| Event hanya cocok untuk pemain pada jam tertentu | Beberapa jendela partisipasi, durasi panjang, aktivitas catch-up, hadiah tidak hanya berdasar waktu online |
| Perubahan versi merusak plugin | Pin versi server/dependensi; staging server; uji pada beta sebelum produksi |

## 16. Keputusan final yang direkomendasikan

| Keputusan | Default yang disarankan |
|---|---|
| Genre | Co-op Survival RPG bertema Nusantara |
| Durasi musim | 8 minggu dengan event kecil tiap minggu |
| Dunia kampung | Persisten, tidak direset setiap musim |
| Dunia sumber daya | Terpisah dan direset secara terjadwal |
| Bencana | Bertahap, diumumkan jauh hari, mengubah event/sumber daya/akses; kerusakan bangunan terbatas dan opt-in |
| PvP | Arena/event opt-in saja |
| Progres | Aktivitas harian tidak wajib, profesi fleksibel, Jurnal Kepulauan |
| Ekonomi | Kepeng; pesanan NPC dahulu, pasar pemain belakangan |
| Java/Bedrock | Paper + Geyser + Floodgate; adapter GUI/form |
| Custom asset | Minimal, ringan, fallback vanilla; pipeline dipilih setelah proof-of-concept |
| Plugin unik | Satu codebase KalaCore modular, dikembangkan bertahap |
| Pengujian awal | 10–20 pemain lintas perangkat; target berkembang setelah profiling |
| Hosting | Prioritaskan koneksi stabil untuk basis pemain utama di Asia Tenggara; validasi ping dari komunitas sebelum membeli kapasitas lebih besar |

**Kesimpulan:** Kala Nusantara tidak perlu menjadi server dengan fitur terbanyak. Ia perlu menjadi server yang memiliki satu siklus bermain yang kuat: **membangun sesuatu, menemukan hal baru, memberi kontribusi pada kampung, melihat dunia berubah, lalu kembali untuk menikmati hasil perubahan itu**. Jika siklus ini menyenangkan tanpa custom item rumit dan tanpa bencana destruktif massal, semua fitur besar sesudahnya akan lebih mudah dibenarkan dan dikembangkan.
