# KALA NUSANTARA
## Master Design Document — Dunia, Gameplay, Worldbuilding, Plugin, Voice, Resource Pack, dan Arahan AI

**Versi dokumen:** 2.0 — Master Blueprint  
**Tanggal pemeriksaan sumber eksternal:** 24 Oktober 2026  
**Status:** Spesifikasi konsep dan rencana teknis; belum menjadi bukti bahwa seluruh plugin telah dipasang atau diuji bersama.  
**Target pemain:** Indonesia dan komunitas global; Java Edition serta Bedrock Edition/MCPE melalui Geyser.  
**Prinsip teknis:** server-side gameplay, client mod tidak wajib, UI ramah sentuh, dan kompatibilitas diverifikasi sebelum setiap rilis.

> **Fungsi dokumen ini:** menjadi satu sumber kebenaran untuk pemilik server, builder, developer, dan AI coding agent. Jika ada prompt, percakapan, atau ide baru yang bertentangan dengan dokumen ini, jangan langsung mengubah rancangan. Tandai konflik, jelaskan dampak gameplay/teknisnya, lalu minta keputusan manusia atau buat proposal perubahan terpisah.

---

## Daftar Isi

1. Visi dan keputusan inti
2. Pengalaman yang dijanjikan kepada pemain
3. Target pemain dan prinsip aksesibilitas
4. Worldbuilding dan lore
5. Rancangan geografi dunia dan masalah satu gunung
6. Progresi pemain: tutorial sampai endgame
7. Core loop dan sistem konten anti-bosan
8. Sistem Kala dan bencana musiman
9. Klaim, keamanan bangunan, snapshot, dan pemulihan
10. Dunia sumber daya dan regenerasi
11. Profesi dan peran komunitas
12. Ekonomi, pasar, dan anti-inflasi
13. Jurnal, reputasi, koleksi, dan hadiah
14. Sosial, desa, party, PvP, kematian, dan teleport
15. Voice chat Java–Bedrock/MCPE
16. UI lintas Java dan Bedrock
17. Arsitektur server dan penyimpanan data
18. Plugin/mod/tool: pilihan, peran, dan instalasi
19. Custom development: apa yang harus dibangun dan bagaimana
20. Resource pack, texture, dan aset visual
21. Performa, stabilitas, keamanan, dan backup
22. Tahapan pengembangan dan acceptance criteria
23. Pengujian lintas perangkat dan playtest
24. Metrik keberhasilan dan cara memperbaiki kebosanan
25. Struktur repository proyek
26. Master prompt AI coding agent
27. Prompt khusus untuk tiap fase implementasi
28. Keputusan yang harus dikunci sebelum produksi
29. Daftar repo/dokumentasi primer
30. Glosarium

---

# 1. Visi dan keputusan inti

## 1.1 Konsep satu kalimat

**Kala Nusantara adalah Co-op Survival RPG ringan bertema kepulauan fiksi terinspirasi Nusantara, tempat pemain membangun kampung, berdagang, menjelajah, mengembangkan profesi, dan menghadapi perubahan alam yang diumumkan lebih awal. Setiap musim membawa dampak nyata pada rute, sumber daya, cerita, dan proyek komunitas, tetapi tidak menghapus progres utama pemain.**

Bukan server survival vanilla dengan daftar plugin yang ditempelkan. Identitas Kala harus terlihat dalam hubungan antarsistem: pemain membangun kampung agar punya pusat sosial; profesi membantu kampung bertahan; eksplorasi memasok bahan; pasar menghubungkan hasil tiap profesi; dan Kala mengubah kebutuhan serta peluang di musim berikutnya.

## 1.2 Pilar desain yang tidak boleh dilanggar

1. **Progres utama persisten.** Rumah dan kampung utama tidak dihapus oleh reset musim.
2. **Risiko diketahui sebelum dipilih.** Bencana, PvP, biaya, dan potensi kerusakan ditampilkan secara jelas.
3. **Co-op lebih utama daripada PvP.** Persaingan terutama berupa lomba, perdagangan, koleksi, dan pencapaian komunitas.
4. **Sesi singkat tetap berarti.** Pemain yang hanya punya 10–20 menit tetap bisa menyelesaikan aktivitas bermakna.
5. **Tidak ada pay-to-win.** Pembelian uang asli tidak memberi kekuatan tempur, keuntungan ekonomi, akses eksklusif ke material terbaik, atau proteksi gameplay yang lebih kuat.
6. **Java dan Bedrock setara dalam fungsi inti.** Quest, claim, jual-beli, event, dan progres tidak boleh mengandalkan fitur UI yang hanya mudah bagi pengguna mouse/keyboard.
7. **Resource pack memperkuat pengalaman, bukan syarat gameplay inti.** Tanpa pack, pemain tetap mengerti apa yang terjadi.
8. **Musim membawa perubahan yang dapat dirasakan.** Jangan membuat letusan hanya berupa judul layar, asap, dan suara.
9. **Kehilangan harus dapat dipahami dan dipulihkan.** Item serta bangunan berharga tidak boleh hilang diam-diam akibat event atau bug.
10. **Kompleksitas gameplay tidak sama dengan kompleksitas runtime.** Banyak fitur menggunakan data, antrean, event, dan layanan yang sama.
11. **Tidak membuat fitur sekadar untuk menaikkan waktu online.** Hindari daily chore wajib, sistem energi yang memaksa pemain kembali, serta hadiah besar yang hangus karena terlambat login.
12. **Keputusan teknis dibuktikan dengan tes.** Klaim kompatibilitas, kestabilan, dan kemampuan menangani banyak pemain tidak boleh dibuat tanpa bukti dari versi server yang benar-benar digunakan.

## 1.3 Keputusan final yang disarankan

| Area | Keputusan rancangan |
|---|---|
| Genre | Co-op Survival RPG / social sandbox ringan |
| Identitas | Kepulauan, pelabuhan, kampung, budaya visual Nusantara yang fiktif, dan Gunung Murka |
| Backend awal | Paper, satu backend, satu plugin custom modular |
| Crossplay | Geyser + Floodgate; protokol dan versi dikunci melalui compatibility matrix |
| World utama | Map custom terkurasi, dibuat offline atau dibangun manual; bukan worldgen acak tanpa kendali |
| Resource world | Satu generator yang dipilih dan diuji; jangan memasang banyak generator bersamaan di world yang sama |
| Struktur tambahan | BetterStructures hanya jika kompatibel dan hanya di world eksplorasi/resource yang sesuai |
| Voice chat | Uji VoiceCraft.Client + VoiceCraft.Java sebagai kandidat utama; tetap opsional dan sediakan text fallback |
| Custom art | Resource pack Java dan Bedrock dikelola/dicek terpisah; 16x/32x pixel art; item custom terbatas dulu |
| Data lokal | SQLite untuk prototipe; database server seperti MariaDB untuk produksi setelah perlu |
| Progres musim | Siklus 8 minggu sebagai target desain awal, dapat diperpanjang berdasarkan data playtest |
| PvP | Opt-in di arena/event atau zona yang dijelaskan secara eksplisit |
| Monetisasi | Belum ada pada MVP; setelah produk stabil, kosmetik saja |
| Target awal uji | 10–20 penguji, kemudian 20–50 concurrent sebagai sasaran pengujian beta, bukan jaminan kapasitas |

## 1.4 Hal yang tidak menjadi target versi awal

- MMO penuh dengan ratusan skill, dungeon raid, dan combat system kompleks.
- Custom client mod wajib untuk memasuki server.
- Custom mob AI dalam jumlah besar.
- Simulasi fisika lava, gempa, dan kebakaran yang menghitung seluruh dunia secara real-time.
- Penghancuran acak pada seluruh rumah pemain.
- World reset total yang menghapus kampung.
- Banyak backend dan proxy sebelum kebutuhan terbukti.
- Puluhan plugin yang masing-masing menyediakan GUI, ekonomi, quest, profesi, dan item tanpa integrasi yang dirancang.
- Item/texture 3D rumit sebelum mapping Java–Bedrock terbukti.
- Voice chat wajib untuk bisa menyelesaikan quest.

---

# 2. Pengalaman yang dijanjikan kepada pemain

Pemain harus bisa menjawab tiga hal kapan pun mereka masuk:

- **Di mana saya sekarang?** Kompas, landmark, papan peta, dan titik perjalanan memberi orientasi.
- **Apa yang bisa saya lakukan sekarang?** Ada beberapa opsi pendek: misi kampung, pesanan, eksplorasi, crafting, proyek komunitas, atau event.
- **Mengapa saya ingin kembali nanti?** Progres kampung, jurnal, profesi, penemuan musiman, relasi sosial, dan perubahan Kala memberikan tujuan yang terus berkembang.

## 2.1 Sesi 10–20 menit

Aktivitas pendek yang dapat diselesaikan dalam satu sesi:

- mengambil/menyelesaikan satu pesanan pasar;
- menanam dan memanen hasil;
- membuat beberapa barang yang sedang dibutuhkan kampung;
- menyerahkan bahan untuk fasilitas umum;
- memancing atau mengumpulkan material pada rute pendek;
- membuka satu catatan Jurnal Kepulauan;
- membantu pemain lain memperbaiki fasilitas;
- mengunjungi event kecil yang sedang berlangsung.

Aktivitas tidak harus selalu memberi uang. Progres dapat berupa reputasi, jurnal, bahan, hubungan kampung, kosmetik, atau akses cerita.

## 2.2 Sesi 30–60 menit

Pemain dapat memilih satu kegiatan yang lebih besar: ekspedisi ke reruntuhan, perjalanan kapal, menyelesaikan rantai quest, mengikuti kompetisi, atau bergabung dalam pembangunan balai kampung.

## 2.3 Sesi panjang

Pemain yang ingin bermain lama dapat membangun, merancang dekorasi, berdagang, melengkapi koleksi, mengejar profesi, mengorganisasi kampung, atau memimpin ekspedisi. Durasi panjang harus berasal dari pilihan dan kreativitas, bukan kewajiban mengulang aktivitas yang sama.

---

# 3. Target pemain dan prinsip aksesibilitas

## 3.1 Pemain yang dituju

- pemain Java yang suka survival, building, eksplorasi, ekonomi, dan komunitas;
- pemain Bedrock/MCPE yang bermain melalui HP/tablet dan biasanya memakai kontrol sentuh;
- pemain baru yang belum paham plugin atau istilah RPG;
- pemain casual yang tidak selalu online setiap hari;
- builder, kolektor, explorer, crafter, pedagang, dan pemain sosial;
- komunitas global yang membutuhkan instruksi bahasa Indonesia dan Inggris.

## 3.2 Aturan aksesibilitas

- Menu maksimal 5–7 opsi di satu layar utama.
- Jangan menggunakan rangkaian klik inventori yang panjang sebagai satu-satunya cara menyelesaikan fitur.
- Untuk Bedrock, sediakan forms atau pesan tindakan sederhana jika plugin mendukung Geyser/Floodgate API.
- Berikan tombol kembali, tutup, konfirmasi tindakan destruktif, dan ringkasan biaya.
- Pesan sistem tidak hanya menggunakan warna; warna selalu disertai teks atau ikon.
- Jangan mengandalkan clickable URL di chat Bedrock; tampilkan kode, nama situs, atau instruksi yang bisa disalin secara manual.
- Semua informasi gameplay penting tersedia di dalam game. Discord berguna untuk komunitas dan notifikasi, tetapi bukan satu-satunya sumber informasi.
- Tidak wajib voice chat, Discord, mod, atau texture pack untuk memahami quest inti.

## 3.3 Bahasa

- Bahasa utama: Indonesia.
- Bahasa kedua: English.
- Semua teks plugin dipisah dari kode ke file locale seperti `messages_id.yml` dan `messages_en.yml`.
- Lore utama memakai istilah fiksi konsisten; jangan campur istilah lokal/Inggris tanpa alasan.
- Nama wilayah adalah fiksi dan terinspirasi lingkungan Nusantara secara luas, bukan klaim akurasi budaya atau representasi kerajaan/komunitas nyata tertentu.

---

# 4. Worldbuilding dan lore

## 4.1 Premis dunia

Kepulauan Kala adalah gugusan pulau fiksi di sekitar Gunung Murka, sebuah gunung api besar yang mengalami siklus geologis misterius. Di wilayah ini, Kala berarti siklus perubahan: tanda-tanda muncul, alam bergolak, sebagian jalur berubah, lalu tanah menghasilkan kehidupan baru.

Warga tidak menganggap gunung sebagai musuh yang harus dibunuh. Mereka mempelajari tanda-tandanya, membangun jalur aman, saling membantu, dan mewariskan catatan dari musim ke musim.

Pemain tiba sebagai pengembara baru, bukan pahlawan yang langsung menyelamatkan dunia. Ia belajar memahami kepulauan, membangun tempat tinggal, mengenal warga, dan pada akhirnya berkontribusi pada sejarah musim.

## 4.2 Tokoh panduan

**Mbah Kala** adalah penjaga arsip dan pemandu yang mempelajari siklus Gunung Murka. Ia bukan tokoh agama atau figur sejarah nyata. Fungsinya adalah menjelaskan sistem, memberi petunjuk, dan menghubungkan pemain dengan lore.

Karakter tambahan yang bisa dibuat pada fase selanjutnya:

- **Nara, Syahbandar:** mengelola pelabuhan, perjalanan, dan perdagangan.
- **Sena, Juru Peta:** mengajari eksplorasi dan mengisi Jurnal Kepulauan.
- **Ratri, Peramu:** mengurus pertanian, tanaman, ramuan, dan pemulihan lahan.
- **Wira, Pandai:** mengelola crafting, peralatan, serta perbaikan fasilitas.
- **Laras, Pengelola Pasar:** memberi pesanan, harga acuan, dan event pasar.

Nama dan dialog masih dapat berubah. Tujuan mereka harus berbeda; jangan membuat NPC hanya menjadi wajah dari tombol GUI yang sama.

## 4.3 Tema visual

- arsitektur tropis: kayu, bambu, batu, atap bertingkat, serambi, dermaga, lumbung, kebun;
- pola visual terinspirasi tekstil, ukiran, anyaman, dan bentuk alam; gunakan karya asli atau aset dengan izin yang jelas;
- palet utama: cokelat kayu, terakota, hijau daun, biru laut, batu abu-abu, dan aksen emas redup;
- Hindari estetika fantasi Eropa sebagai bahasa visual utama;
- Hindari juga penggunaan simbol budaya yang sakral atau motif spesifik tanpa riset dan izin yang memadai;
- desain harus masih terbaca dengan vanilla blocks tanpa texture pack custom.

## 4.4 Konflik dan cerita

Konflik utama adalah ketidakpastian alam dan keputusan komunitas: kapan harus menyimpan persediaan, rute mana yang harus dibuka, fasilitas apa yang perlu diperbaiki, dan bagaimana membagi hasil ekspedisi.

Tidak perlu membuat seluruh cerita berakhir pada satu boss. Cerita Kala berlapis:

1. **Cerita kedatangan:** siapa pemain, bagaimana hidup di pelabuhan, dan mengapa warga mempelajari gunung.
2. **Cerita kampung:** kebutuhan warga, persahabatan, keterampilan, dan kontribusi komunitas.
3. **Cerita ekspedisi:** catatan lama, reruntuhan, rute laut, dan pulau-pulau yang berubah.
4. **Cerita musim:** kejadian khusus musim ini, keputusan persiapan, dan dampak sesudahnya.
5. **Cerita arsip:** setelah beberapa musim, pemain bisa membaca bagaimana kontribusi komunitas membentuk sejarah kepulauan.

---

# 5. Rancangan geografi dunia dan masalah satu gunung

## 5.1 Keputusan: satu gunung utama, bukan gunung yang selalu terlihat

Gunung Murka tetap menjadi satu gunung berapi utama. Secara teknis tidak masuk akal menjanjikan gunung terlihat dari setiap titik map pada semua perangkat: render distance klien, view distance server, garis pandang, kabut, bukit, dan bangunan membatasi apa yang benar-benar terlihat.

Solusinya adalah **desain sudut pandang dan orientasi**, bukan menaikkan view distance tanpa batas.

- Dari Labuhan Awal, gunung tampak di belakang teluk sebagai siluet utama.
- Dari mercusuar dan beberapa bukit, gunung muncul sebagai landmark.
- Dari wilayah yang terlalu jauh atau terhalang, pemain tetap bisa menentukan arah memakai Kompas, papan peta, koordinat landmark, penunjuk jalur, dan nama wilayah.
- Asap/awan visual dapat dipakai sebagai efek pada lokasi yang memang sedang ter-render; jangan menjanjikan efek tersebut akan terlihat dari seluruh kepulauan.
- Jangan membuat semua wilayah sangat jauh satu sama lain hanya demi skala. Map yang compact lebih mudah diisi dan lebih hemat untuk server serta pemain mobile.

**Aturan desain:** gunung harus mudah dikenali dari lokasi-lokasi yang dirancang untuk memperlihatkannya, tetapi game tidak boleh bergantung pada visibilitas gunung untuk navigasi atau quest.

## 5.2 Metode pembuatan world utama

**Rekomendasi:** gunakan WorldPainter untuk membentuk terrain dan garis pantai awal, lalu lakukan finishing bangunan/struktur di Creative Mode dengan WorldEdit atau FAWE. WorldPainter adalah aplikasi map generator, bukan plugin runtime; peta diekspor sebagai world Minecraft. Dokumentasi: https://www.worldpainter.net/ dan https://www.worldpainter.net/trac/wiki/FAQ

Keuntungannya:

- posisi gunung, pelabuhan, pulau, garis pantai, dan jalur pandang dapat direncanakan;
- tutorial dan landmark tidak berubah secara acak setelah regenerasi;
- kamu dapat membatasi ukuran map utama;
- lore dan quest dapat memakai koordinat/lokasi yang stabil.

Tidak perlu menaruh generator worldgen di world utama jika terrain tersebut sudah dibuat dan dikurasi. World generator berjalan ketika chunk dibuat; map buatan tangan memberi kontrol lebih baik untuk cerita utama.

## 5.3 Ukuran map awal yang disarankan

Angka berikut adalah **target desain awal untuk prototipe**, bukan spesifikasi wajib:

- Map utama: area eksplorasi sekitar 2.000 × 2.000 sampai 3.000 × 3.000 blok.
- Area tutorial: cukup kompak agar semua tujuan awal bisa dicapai dalam 15–30 menit.
- Pelabuhan awal ke titik pandang Gunung Murka: target beberapa ratus blok, disesuaikan saat uji visual.
- Kampung aman berjarak cukup satu sama lain untuk terasa berbeda, tetapi masih masuk akal ditempuh dengan jalan, perahu, atau teleport antar-waystation setelah ditemukan.
- World border digunakan agar map tidak tumbuh tanpa tujuan dan tidak ada generasi chunk liar yang tidak perlu.

Sebelum final, lakukan tes jarak pandang dengan render distance rendah dan menengah (misalnya 6, 8, 10, 12 chunk) pada laptop serta HP. Angka chunk bukan jaminan jarak penglihatan yang sama di semua perangkat.

## 5.4 Wilayah utama

Nama di bawah adalah draft worldbuilding yang dapat diubah tanpa mengubah sistem teknis.

| Wilayah | Identitas | Aktivitas utama | Progres yang didukung |
|---|---|---|---|
| **Labuhan Awal** | Pelabuhan awal, pasar, papan peta, NPC tutorial, dermaga aman | Tutorial, berdagang, menerima misi, bertemu pemain | Memulai rumah/kampung, memahami Kompas |
| **Pesisir Serambi** | Pantai, terumbu, kampung nelayan, jalur perahu | Memancing, perdagangan, pengiriman barang, festival laut | Profesi Pelaut dan Jurnal laut |
| **Rimba Waringin** | Hutan tropis, kebun, sungai, titik bahan organik | Bertani, mengumpulkan tumbuhan, menemukan jejak/ruins | Profesi Peramu dan koleksi alam |
| **Pegunungan Batuara** | Dataran tinggi, tambang, gua dan jalur batu | Menambang, crafting, ekspedisi, pengambilan mineral | Profesi Pandai dan bahan crafting |
| **Karang Purnama** | Pulau kecil, mercusuar, laguna, perairan dangkal | Navigasi, fishing challenge, eksplorasi santai | Jurnal, perjalanan dan event mingguan |
| **Sabuk Kala** | Wilayah sekitar gunung dengan batas aman/berisiko yang jelas | Misi observasi, pengumpulan material tertentu, event musiman | Tanda Kala, persiapan, eksplorasi opt-in |
| **Pulau Seberang** | Destinasi eksplorasi musiman | Penemuan baru, arsip, material terbatas, reruntuhan | Tujuan musim dan completionist |

## 5.5 Gunung Murka sebagai pusat cerita

Gunung dibangun dengan empat cincin fungsi, tidak seluruhnya harus dapat diakses:

1. **Kaki Gunung:** jalur pengamatan, papan larangan, tempat pandang aman, dan NPC peneliti.
2. **Sabuk Hijau:** hutan dan jalur eksplorasi, ancaman ringan, pengumpulan bahan umum.
3. **Sabuk Terdampak:** zona event sementara, jalan yang bisa ditutup, pos terdaftar, dan misi evakuasi.
4. **Kawah/garis depan:** lokasi endgame/seasonal opsional yang hanya diakses dalam event yang tepat. Bukan rumah atau tempat penyimpanan permanen.

Gunung jangan dirancang hanya sebagai lorong panjang menuju boss. Isinya harus memberikan alasan untuk naik: menemukan catatan, mengobservasi perubahan, menyelesaikan tantangan navigasi, atau membantu misi komunitas.

## 5.6 Peran map

Map tidak hanya menjadi ruang untuk membangun rumah. Map mempunyai lima fungsi yang saling terhubung:

- **Tempat tinggal:** membangun rumah dan mengembangkan kampung.
- **Ruang kerja:** menanam, crafting, produksi, berdagang.
- **Ruang penemuan:** menjelajah, menemukan landmark, mengisi jurnal.
- **Panggung komunitas:** festival, lomba, proyek publik, pasar.
- **Dunia yang berubah:** rute, sumber daya, tanda Kala, pulau musiman, dan arsip sejarah.

Pemain boleh fokus pada satu fungsi, tetapi seluruh fungsi saling membantu. Builder dapat menyumbang fasilitas untuk ekspedisi; explorer membawa material unik untuk dekorasi; petani memasok event; pedagang menyediakan alat untuk persiapan.

---

# 6. Progresi pemain: tutorial sampai endgame

## 6.1 Menit 0–10: onboarding tanpa informasi berlebihan

1. Pemain spawn di Labuhan Awal.
2. Sambutan singkat Mbah Kala, tanpa monolog panjang.
3. Pemain menerima Kompas Pengembara versi vanilla-compatible.
4. Satu objective aktif: ikuti penanda menuju meja panduan atau NPC.
5. Pemain belajar tiga kontrol dasar: buka Kompas, ambil quest, cek panduan.
6. Peta sederhana memperlihatkan pelabuhan, lahan awal, dan titik bahaya pertama.

Jangan tampilkan seluruh profesi, pasar, museum, voice settings, dan detail bencana dalam satu layar awal.

## 6.2 Menit 10–30: satu rumah, satu jual-beli, satu penemuan

Urutan tutorial yang disarankan:

1. Kumpulkan kayu/bahan awal.
2. Buat alat dasar atau gunakan stasiun kerja yang disediakan.
3. Pilih lahan pemula di zona hijau dan baca ringkasan klaim.
4. Letakkan tempat tidur dan bangun struktur kecil.
5. Bawa hasil sederhana ke pasar untuk menyelesaikan transaksi pertama.
6. Temukan papan peta atau landmark.
7. Buka ringkasan: cara melindungi rumah, cara kembali, kapan informasi Kala dapat dilihat, dan apa tiga aktivitas berikutnya.

Tutorial tidak perlu memaksa pemain membangun rumah yang besar atau indah. Tujuannya memberi pengalaman berhasil, bukan menguji kemampuan building.

## 6.3 Jam 1–3: menemukan gaya bermain

Kompas menawarkan tiga rekomendasi, bukan kewajiban:

- **Bangun:** tambah satu ruang rumah, kebun, atau dekorasi.
- **Jelajah:** cari satu landmark atau item jurnal.
- **Bantu kampung:** kirim bahan ke proyek umum atau bantu pesanan.

Setelah aktivitas ini, pemain mulai melihat opsi profesi dan komunitas. Profesi dapat dipilih setelah pemain paham apa yang akan dilakukan profesi tersebut.

## 6.4 Hari pertama

Pemain idealnya sudah:

- tahu lokasi rumah dan cara melindunginya;
- melakukan satu transaksi ekonomi;
- mengetahui satu atau dua wilayah lain;
- menyelesaikan setidaknya satu aktivitas sosial atau proyek bersama;
- memahami bahwa Gunung Murka dan musim adalah bagian dari dunia;
- mengetahui bahwa event utama diumumkan dan rumah utama di zona hijau aman.

## 6.5 Minggu 1–4: mengembangkan kampung dan spesialisasi

Pemain dapat membangun rumah, mengundang teman, meningkatkan fasilitas, mengembangkan profesi, menyelesaikan pesanan, membuka rute perjalanan, dan menemukan isi jurnal. Tidak ada satu jalur progres yang wajib dipakai semua pemain.

## 6.6 Minggu 5–8: menghadapi Kala

Pemain membaca peringatan, memutuskan apakah ingin berpartisipasi dalam misi, mengumpulkan bahan, mengamankan pos opt-in, menyelesaikan event, lalu memulihkan/membangun lagi dan membaca arsip musim.

## 6.7 Jika alur utama selesai, apa yang dilakukan?

Kala Nusantara tidak boleh menjadi game yang tamat lalu kosong. Setelah cerita pengenalan selesai, tujuan tidak lagi hanya quest berurutan. Endgame adalah kombinasi konten yang dapat diperbarui:

1. **Jurnal Kepulauan 100%:** landmark, flora, fauna, material, resep, cerita, dan catatan musiman.
2. **Mastery profesi:** membuka variasi crafting, layanan, desain, dan kosmetik; bukan statistik tempur tak terbatas.
3. **Kampung berkembang:** fasilitas publik, desain arsitektur, taman, dermaga, pasar, museum.
4. **Proyek komunitas:** membangun observatorium, lumbung besar, jalur kapal, balai festival, atau tempat evakuasi.
5. **Koleksi musiman:** arsip, hiasan, skin alat, gelar kontribusi, dan blueprint dekorasi.
6. **Event rotasi:** lomba kapal, fishing contest, bazar, festival hasil bumi, scavenger hunt, dan kontes bangunan.
7. **Ekspedisi baru:** Pulau Seberang berubah setiap musim, dengan cerita/ruins yang berbeda.
8. **Ekonomi sosial:** membuat barang yang dibutuhkan pemain lain, berdagang, menjalankan toko, atau membantu pemain baru.
9. **Kegiatan santai:** pertanian, memancing, dekorasi, pet, role-play ringan, fotografi/peta, dan jalan-jalan.

Tidak semua konten perlu dibuat langsung. MVP hanya perlu menunjukkan bahwa satu core loop bisa menghasilkan kegiatan yang bervariasi.

---

# 7. Core loop dan sistem konten anti-bosan

## 7.1 Core loop inti

**Temukan kebutuhan → pilih aktivitas → dapatkan atau olah material → berkontribusi/menjual/membangun → buka progres baru → dunia berubah → pilih tujuan berikutnya.**

Setiap fitur yang dibuat harus terhubung ke minimal dua hal dari loop ini. Contoh: hasil tambang dapat dipakai Pandai untuk memperbaiki fasilitas; fasilitas mendukung misi Kala; misi menghasilkan bahan/arsip; arsip membuka tujuan eksplorasi baru.

## 7.2 Sumber variasi yang sehat

- **Lokasi:** lingkungan berbeda menyediakan tantangan dan material berbeda.
- **Pilihan:** quest memberi pilihan aktivitas/imbalan, tidak selalu satu rute paksa.
- **Musim:** akses, kebutuhan pasar, dan cerita berubah.
- **Sosial:** bekerja sendiri tetap memungkinkan, bekerja sama lebih efektif untuk proyek komunitas.
- **Koleksi:** Jurnal memberi tujuan bagi pemain tanpa combat.
- **Kreativitas:** bangunan dapat memamerkan hasil kerja dan membantu kampung.
- **Event:** format berubah tiap minggu, tetapi aturan tetap bisa dipahami.

## 7.3 Event mingguan

Rotasi contoh (tidak berarti seluruhnya harus dibuat pada minggu pertama):

| Minggu rotasi | Event | Aktivitas | Hadiah utama |
|---|---|---|---|
| A | Pasar Rakyat | Penuhi pesanan terpilih, trade fair | Kepeng terbatas, reputasi pasar, kosmetik |
| B | Lomba Kapal | Rute perahu dengan checkpoint | Gelar, hiasan kapal, catatan jurnal |
| C | Festival Panen | Tanam, panen, dan kirim hasil berkualitas | Benih dekoratif, dekorasi kebun |
| D | Ekspedisi Reruntuhan | Cari petunjuk dan susun catatan | Entri jurnal, hiasan, bahan crafting |
| E | Latihan Kala | Simulasi evakuasi, perbaikan pos | Tiket latihan/kosmetik, pengenalan mekanisme |
| F | Hari Gotong Royong | Pilih proyek publik dan sumbang bahan | Arsip kontribusi, diskon layanan terbatas |
| G | Kontes Bangunan | Tema arsitektur atau fasilitas kampung | Piala dekorasi, galeri pemenang |
| H | Festival Laut | Fishing contest santai dan koleksi | Skin alat pancing, gelar |

Jangan menuntut pemain online pada satu jam agar tidak tertinggal hadiah inti. Untuk pemain beda zona waktu, event kompetitif dapat punya beberapa gelombang; proyek komunitas tetap berjalan selama 24–48 jam.

## 7.4 Daily dan weekly goals

Tujuan harian hanyalah saran ringan. Maksimal tiga tugas rekomendasi tampil sekaligus. Tugas yang terlewat tidak menghasilkan hukuman, streak loss, atau kebutuhan mengulang dari nol. Hadiah harian tidak boleh menjadi sumber utama uang karena mudah dimanipulasi melalui akun alt.

---

# 8. Sistem Kala dan bencana musiman

## 8.1 Durasi musim

Target awal adalah 8 minggu, dengan durasi dapat diubah melalui konfigurasi. Jangan hardcode tanggal/periode ke dalam kode.

| Fase | Waktu target | Keadaan dunia | Pengalaman pemain |
|---|---|---|---|
| Masa Berkembang | Minggu 1–4 | Kegiatan normal; pasar/event mingguan aktif | Membangun, berdagang, menjelajah, mengembangkan profesi |
| Tanda Kala | Minggu 5 | Petunjuk visual/naratif bertambah; peta risiko diumumkan | Mengamati, membuka guide, memilih persiapan |
| Persiapan | Minggu 6 | Proyek tanggul, pos, logistik, dan jalur evakuasi | Kontribusi sukarela, pembelian perlengkapan, latihan |
| Peristiwa Kala | Minggu 7 | Event terjadwal dengan efek bertahap | Evakuasi, observasi, misi garis depan, dampak wilayah opt-in |
| Pemulihan & Kesuburan | Minggu 8 | Jalur baru, area pasca-event, festival dan catatan musim | Memulihkan pos, menanam, ekspedisi, membaca arsip |

Minggu 1–4 tidak boleh menjadi periode tanpa konten. Pasar, proyek, jurnal, dan event mingguan tetap tersedia.

## 8.2 Tanda awal yang benar-benar berguna

Sinyal bertahap:

- Minggu 5: Mbah Kala mengumumkan pengamatan, peta menunjukkan tingkat risiko, NPC memberi penjelasan singkat.
- Beberapa hari sebelum event: perubahan langit/partikel terbatas, suara lingkungan opsional, notifikasi 72 jam dan 24 jam.
- Saat persiapan: Kompas menunjukkan checklist yang bisa ditandai selesai.
- Sebelum mulai: status online menampilkan waktu event lokal/UTC dengan jelas.
- Sesudah event: laporan perubahan dan ringkasan hasil.

Hindari efek partikel terus-menerus di seluruh dunia atau sound spam. Efek atmosferik hanya untuk pemain yang relevan/berdekatan dan berlangsung pada interval yang dibatasi.

## 8.3 Zona risiko

| Zona | Fungsi | Kerusakan | Hadiah/aktivitas |
|---|---|---|---|
| **Hijau — Kampung Aman** | Rumah utama, pemula, hub sosial | Tidak ada kerusakan bangunan permanen oleh event Kala | Kegiatan umum, akses semua pemain |
| **Kuning — Sabuk Terdampak** | Rute, pos, lahan yang memilih ikut event | Dampak terbatas hanya pada plot/struktur yang ditandai ikut risiko | Misi bantuan, bahan pasca-event, bonus musiman |
| **Merah — Garis Depan** | Pos ekspedisi sementara dan lokasi tantangan | Risiko tertinggi hanya pada plot opt-in dan dalam aturan yang ditampilkan | Bahan langka, catatan lore, gelar/kosmetik dan tantangan |

Warna zona harus disertai ikon, teks, dan ringkasan aturan. Klaim aman tidak boleh berubah menjadi klaim berisiko tanpa persetujuan pemilik.

## 8.4 Letusan sebagai rangkaian fase, bukan simulasi fluida

Rangkaian event yang disarankan:

1. **Pengumuman:** timeline, zona, lokasi evakuasi, dan dampak yang diharapkan.
2. **Observasi:** pemain mengunjungi titik pengamatan dan mencatat fenomena.
3. **Logistik:** pemain dapat memasok kayu, batu, makanan, dan bahan alat untuk proyek publik.
4. **Evakuasi simulasi:** pemain mengantar NPC/objek quest ke titik aman; implementasi pertama boleh sepenuhnya non-destruktif.
5. **Puncak Kala:** perubahan pencahayaan/atmosfer yang didukung versi, efek suara/partikel terbatas, akses rute berubah, dan hanya plot opt-in yang diproses.
6. **Penutupan:** dunia kembali tenang, laporan pribadi dirilis, dan perubahan pasca-event aktif.
7. **Pemulihan:** fasilitas, jalur, dan plot opt-in dapat dipulihkan melalui antrean.
8. **Arsip:** catat kontribusi kolektif dan buka entri musim.

Jangan memakai simulasi lava massal, perubahan ribuan blok tanpa antrean, atau entitas bergerombol sebagai jalan pintas untuk membuat event terlihat dramatis.

## 8.5 Konsekuensi yang membuat Kala bermakna

Setiap event harus mengubah setidaknya dua hal gameplay yang dapat diuji:

- satu rute perjalanan berubah sementara;
- beberapa simpul sumber daya berpindah/aktif;
- permintaan pasar berubah;
- satu bagian Jurnal atau cerita terbuka;
- proyek komunitas memberi perubahan nyata pada fasilitas publik;
- satu wilayah mendapat bonus pertanian sementara;
- Pulau Seberang/reruntuhan baru bisa dijelajahi;
- pos berisiko yang terdaftar membutuhkan pemulihan.

Jika tidak ada perubahan akses, sumber daya, ekonomi, cerita, atau tujuan, event itu terlalu kosmetik dan perlu dirancang ulang.

## 8.6 Waktu event untuk global

- Umumkan jadwal dalam UTC dan konversi pemain jika ada UI pendukung.
- Berikan notifikasi 72 jam, 24 jam, dan 1 jam untuk event besar; atur supaya tidak berulang setiap login.
- Sediakan event window lebih panjang atau beberapa sesi untuk pemain di Asia, Eropa, dan Amerika.
- Tidak memberi hadiah utama hanya kepada pemain yang online saat menit puncak.
- Peran offline/asinkron bisa menyelesaikan persiapan, bahan, peta, atau proyek komunitas.

---

# 9. Klaim, keamanan bangunan, snapshot, dan pemulihan

Ini adalah sistem dengan risiko implementasi tertinggi. Dibuat setelah server crossplay dan core loop dasar terbukti stabil.

## 9.1 Klaim

- Pemain mendapat plot awal yang kecil dan aman.
- Klaim mempunyai UUID pemilik, daftar anggota, peran, batas area, dan status zona.
- Peran minimal: Owner, Manager, Builder, Visitor.
- Semua tindakan anggota dicatat yang relevan untuk audit; akses penyimpanan harus mengikuti izin.
- Batasi area per klaim, jumlah klaim per pemain, dan entitas/farm di area.
- Claim management harus bisa digunakan dari Java GUI dan Bedrock forms.

## 9.2 Pemisahan rumah utama dan plot event

**Aturan penting:** rumah utama tidak otomatis menjadi target kehancuran. Pemilik dapat mendaftarkan pos/plot tertentu ke event berisiko dengan konfirmasi eksplisit. Pemilihan risiko tidak boleh mengubah status rumah utama secara tidak sengaja.

MVP menggunakan GriefPrevention untuk proteksi klaim generik; custom KalaClaim baru menggantikannya setelah fitur, import/migrasi, dan tes keamanan siap. Jangan menjalankan dua plugin klaim pada world yang sama tanpa menentukan mana yang authoritative.

## 9.3 Barang yang tidak terpengaruh

- Inventori pemain, ender chest, dan barang kosmetik tidak diproses oleh Kala.
- Item bernilai/quest penting tidak boleh hilang akibat operasi blok.
- Barang yang disimpan dalam container pada plot berisiko harus dicatat melalui snapshot tervalidasi sebelum operasi destruktif.
- Jangan mengandalkan nama display item sebagai identitas unik; gunakan persistent data/tags yang disediakan API plugin jika diperlukan.

## 9.4 Rancangan snapshot aman

1. Hanya struktur/plot yang terdaftar ikut event yang masuk antrean.
2. Validasi pemilik, batas plot, status event, dan checksum/ID snapshot.
3. Baca blok/chunk melalui API server pada thread yang aman menurut platform. Jangan mengakses Bukkit world state sembarangan dari asynchronous thread.
4. Proses pemindaian dan perubahan blok dalam batch kecil di scheduler; ukur beban per tick. Operasi disk/kompresi/serialisasi yang aman dapat dilakukan asynchronous setelah data diambil secara aman.
5. Simpan snapshot struktur dan data container dengan ID event, ID plot, waktu, versi skema, dan checksum.
6. Pastikan snapshot berhasil ditulis sebelum plot berubah.
7. Saat isi container dipindah ke Brankas Pengungsian, gunakan ID transaksi/idempotency key supaya pemulihan ulang tidak menggandakan barang.
8. Proses restore tidak menimpa perubahan baru tanpa konfirmasi atau aturan konflik.
9. Gunakan antrean restore, progress, tombol cancel sebelum tahap commit, dan log administratif.
10. Jika server mati di tengah operasi, saat startup sistem membaca status job dan melanjutkan dengan aman atau membatalkan melalui prosedur recovery.

**WorldEdit/FAWE bukan sistem transaksi barang yang otomatis aman.** Tool tersebut membantu manipulasi struktur/blok; snapshot inventori, claim permissions, idempotensi, vault, konflik data, dan recovery masih harus dirancang dan diuji pada Kala.

## 9.5 Brankas Pengungsian

- Penyimpanan per pemain atau per plot dengan batas slot yang masuk akal.
- Setiap paket hasil pengamanan disertai laporan sumber: event, plot asal, waktu, dan status.
- Status paket: `PREPARING`, `SNAPSHOTTED`, `ITEMS_SECURED`, `READY_TO_RESTORE`, `RESTORING`, `COMPLETE`, `FAILED_REVIEW`.
- Jangan menghapus snapshot sebelum restore benar-benar selesai dan diverifikasi.
- Admin memiliki audit log, bukan tombol untuk mengubah data tanpa jejak.

## 9.6 Jalur pemulihan

- **Pulih Mandiri:** pemain mengumpulkan Puing/material dan membayar biaya yang terlihat sebelum commit.
- **Pulih Cepat:** pulihkan dari snapshot memakai bahan/Kepeng serta batas per musim yang jelas.
- **Gotong Royong:** pemain lain membantu menyediakan bahan atau memperbaiki fasilitas bersama.
- **Asuransi Musim:** satu pemulihan penuh per musim sebagai target awal; keseimbangan final ditentukan playtest.
- **Rumah Darurat:** pemain yang rumah event-nya tidak dapat digunakan mendapat lokasi aman sementara.
- Pemulihan tidak kedaluwarsa hanya karena pemain absen seminggu.

Biaya contoh (misalnya 20–40% material) hanya parameter awal untuk uji ekonomi, bukan angka final. Semua biaya harus configurable.

## 9.7 Puing dan perbaikan

Puing dapat mewakili material yang bisa dipakai ulang, tetapi jangan menjatuhkan entitas item untuk setiap blok yang hancur. Untuk menjaga performa, hasil puing dapat dikonversi menjadi satu ringkasan klaim/bundel atau ditambahkan ke daftar material pemulihan.

---

# 10. Dunia sumber daya dan regenerasi

## 10.1 Jenis world

1. **`kala_main`**: dunia utama persisten, landmark tetap, kampung/claim.
2. **`kala_resources`**: world pengumpulan material, dapat direset terjadwal.
3. **`kala_expedition_s01`** atau sistem Pulau Seberang serupa: world musiman terpisah atau area terbatas yang dikelola dengan prosedur rotasi.

Nama final tergantung konfigurasi server. Ini adalah pemisahan konseptual; pada prototipe, world bisa berada di satu proses Paper. Proxy/multi-backend tidak dibutuhkan sejak awal.

## 10.2 Reset resource world

- Umumkan batas waktu reset.
- Tolak teleport masuk saat penutupan dimulai.
- Beri countdown dan pindahkan pemain yang masih di world ke titik aman.
- Periksa player, chest/claim yang perlu dipertahankan, dan antrean event yang aktif.
- Backup world sebelum reset.
- Reset hanya resource world, bukan world utama.
- Generate/pregenerate area baru secara terukur sebelum dibuka.
- Catat seed, generator, konfigurasi, dan versi plugin dalam arsip operasi.

## 10.3 Simpul sumber daya

Resource node dapat digunakan untuk pohon/ore/hasil bumi tertentu. Hindari terus memeriksa semua blok dunia pada setiap tick. Gunakan daftar node yang benar-benar didaftarkan, expiry timestamp, dan event pemicu interaksi. Untuk MVP, gunakan reset world dan loot table normal terlebih dahulu; custom node engine ditambahkan jika playtest membuktikan kebutuhan.

## 10.4 Kelelahan tanah

Ini mekanisme opsional, bukan syarat rilis. Jika dikembangkan:

- hanya pada node/region yang dipantau, bukan menghitung setiap blok secara global;
- diminishing returns lembut dan dijelaskan;
- pemulihan terjadi secara waktu/event, bukan hanya login harian;
- tidak menghukum pemain yang mengumpulkan material kebutuhan dasar;
- status disimpan agregat per zona, bukan setiap aksi kecil secara berlebihan.

## 10.5 Kesuburan pasca-Kala

Sesudah event, wilayah tertentu dapat memperoleh bonus panen sementara, tanaman baru, atau resep bahan. Hindari mengubah random tick speed global jika efek dapat dicapai melalui perhitungan plugin yang lebih terukur. Uji apakah mekanisme tidak menghasilkan duplikasi hasil atau beban entity besar.

---

# 11. Profesi dan peran komunitas

Pemain memulai tanpa memilih profesi. Setelah tutorial, ia dapat memilih satu profesi utama. Jalur sampingan dibuka kemudian setelah sistem awal seimbang.

## 11.1 Empat profesi

### A. Pandai

- crafting dan perbaikan peralatan/fasilitas;
- membantu memulihkan pos terdaftar;
- membuat komponen atau dekorasi;
- memberi layanan crafting kepada pemain lain.

### B. Peramu

- pertanian, tumbuhan, bahan pangan/ramuan;
- merawat kebun dan fasilitas pangan kampung;
- mengenali tanaman dan sumber daya pasca-Kala;
- membuat produk yang dijual ke pemain lain.

### C. Pelaut

- transportasi barang dan rute laut;
- tantangan navigasi/kapal;
- mengirim pesanan antarpulau;
- membuka shortcut/waystation melalui eksplorasi yang sudah ditempuh.

### D. Penjelajah

- peta, ekspedisi, pengumpulan bukti, dan penemuan;
- menandai tanda Kala lebih awal pada misi yang tersedia;
- membuka catatan Jurnal dan rute baru;
- membuat peta/layanan ekspedisi bagi tim.

## 11.2 XP tanpa grind

XP diperoleh dari variasi aktivitas normal dan tujuan unik. Anti-exploit:

- XP untuk action yang relevan, bukan setiap klik;
- cooldown atau diminishing return per sumber yang mudah di-spam;
- aktivitas berulang antar akun dalam waktu pendek ditandai untuk analisis;
- XP tidak dapat diperjualbelikan;
- level memberi opsi, kosmetik, layanan, atau efisiensi terbatas, bukan damage yang membuat pemain lama menang otomatis.

## 11.3 Peran saat Kala

| Profesi | Kontribusi pada Kala | Tidak boleh menjadi keunggulan wajib |
|---|---|---|
| Pandai | Memperbaiki pos dan menghasilkan komponen | Tidak menjadi satu-satunya yang boleh memulihkan |
| Peramu | Menyiapkan makanan/obat dan memulihkan kebun | Tidak membuat pemain tanpa profesi tidak bisa ikut event |
| Pelaut | Logistik dan rute kapal | Tidak mengunci seluruh area bagi non-Pelaut |
| Penjelajah | Informasi, peta, dan catatan lokasi | Tidak menyembunyikan peringatan keselamatan dari pemain lain |

---

# 12. Ekonomi, pasar, dan anti-inflasi

## 12.1 Mata uang

Nama mata uang: **Kepeng**. Pada awalnya, Kepeng didapat dari quest, pesanan, pasar, pekerjaan, event, dan transaksi antarpemain. Kepeng tidak bisa dibeli dengan uang asli.

## 12.2 Sumber uang dan sink

Sumber (money source):

- pesanan NPC dengan batas dan variasi;
- penjualan barang dasar yang dibutuhkan server;
- hadiah event yang terukur;
- misi tutorial dan seasonal objectives;
- layanan pemain kepada pemain lain.

Sink (money sink):

- biaya listing dan pajak pasar;
- biaya perjalanan/layanan yang opsional;
- perbaikan item atau layanan crafting;
- upgrade fasilitas kampung;
- kosmetik yang hanya tersedia melalui permainan;
- biaya pemulihan pos opt-in.

Jangan membuat NPC membeli semua item dengan harga tetap tanpa batas karena dapat menimbulkan inflasi.

## 12.3 Pasar pemain

- Listing memiliki harga, kuantitas, waktu kedaluwarsa, dan pemilik UUID.
- Pajak awal 3–5% hanya titik awal uji; harus configurable.
- Hanya satu transaksi commit untuk satu listing pada satu waktu.
- Setelah barang terjual, inventory dan catatan transaksi harus konsisten; jika gagal, rollback secara aman.
- Log transaksi menyimpan seller, buyer, item key, jumlah, harga, pajak, dan timestamp.
- Listing tidak boleh menduplikasi item melalui disconnect/restart.
- Pasar tidak harus menggunakan GUI inventory yang kompleks untuk Bedrock; forms/command singkat harus tersedia.

## 12.4 Harga NPC dinamis

Untuk MVP, gunakan daftar harga yang stabil dan jelas. Harga dinamis ditambahkan setelah ada data transaksi. Jika dipakai:

- batas minimum/maksimum;
- perubahan harga berdasarkan volume yang sah, bukan hanya transaksi satu akun;
- pemulihan harga gradual;
- larangan arbitrase buy/sell yang menghasilkan uang tanpa risiko;
- jangan membuat kebutuhan utama pemain tiba-tiba terlalu mahal;
- admin dapat melihat laporan inflasi dan volume transaksi.

## 12.5 Keadilan ekonomi

- Tidak menjual Kepeng dengan uang asli.
- Tidak menjual skill atau profesi eksklusif lewat donasi.
- Tidak membuat donatur memperoleh material langka atau proteksi lebih kuat.
- Kosmetik harus bisa dipahami dan tidak menyerupai item gameplay yang menyesatkan.
- Akses dasar ke quest, profesi, dan event tetap gratis.

---

# 13. Jurnal, reputasi, koleksi, dan hadiah

## 13.1 Jurnal Kepulauan

Jurnal berisi kategori:

- landmark dan lokasi;
- cerita/diari NPC;
- flora, hasil kebun, ikan, dan bahan alam;
- reruntuhan dan artefak;
- resep/kerajinan yang ditemukan;
- arsip Kala per musim;
- kontribusi pada proyek komunitas.

Pencatatan hanya terjadi setelah kondisi discovery benar-benar dipenuhi. Jangan membuat pemain mengulang discovery yang sudah tercatat kecuali ada mode koleksi musiman yang dijelaskan.

## 13.2 Arsip Musim

Setelah satu musim, sistem membuat halaman sejarah berisi:

- nama musim dan ringkasan fenomena;
- rute/fasilitas yang terbuka;
- proyek komunitas yang diselesaikan;
- penemuan penting;
- daftar kontribusi menurut beberapa kategori, bukan hanya satu ranking total;
- foto/spotlight bangunan pemenang jika fitur ini dikembangkan;
- item kosmetik/gelar yang bisa didapat melalui aktivitas.

Jangan memberi satu pemain hadiah dominan hanya karena jam bermainnya jauh lebih tinggi. Buat kategori kontribusi: discovery, building, logistics, crafting, support, event participation.

## 13.3 Reputasi kampung

Reputasi berasal dari proyek yang selesai, fasilitas yang digunakan, ekspedisi yang dikontribusikan, dan bantuan komunitas. Tidak boleh disamakan dengan mata uang atau dibeli secara langsung. Reputasi membuka dekorasi dan cerita, bukan statistik tempur besar.

---

# 14. Sosial, desa, party, PvP, kematian, dan teleport

## 14.1 Kampung

Kampung adalah gabungan klaim, fasilitas umum, anggota, dan proyek.

Fitur awal:

- undang anggota dan atur peran;
- daftar proyek publik;
- lumbung kontribusi bahan dengan log transaksi;
- nama dan banner/icon kampung yang tidak harus menggunakan custom item;
- daftar fasilitas yang sudah tersedia;
- buku sejarah kontribusi.

Fitur lanjutan:

- peringkat kampung berbasis beragam kategori;
- festival antar-kampung;
- pameran bangunan;
- papan pesanan regional.

## 14.2 Party dan ekspedisi

Party mendukung perjalanan/quest bersama dan pembagian progress secara jelas. Reward harus menghindari eksploitasi idle: partisipasi minimal dapat berupa berada di lokasi event, menyelesaikan tindakan, atau membantu objective.

## 14.3 Kematian

Rancangan awal:

- di zona aman, nisan/paket kematian terlindungi untuk pemilik selama periode singkat (contoh awal 10 menit) sebelum aturan publik/expiry berlaku;
- barang quest penting tidak hilang permanen;
- di event, aturan kematian ditampilkan sebelum masuk;
- tidak mengurangi XP secara berlebihan;
- jangan menggunakan custom entity banyak-banyak hanya untuk menampilkan nisan; fallback berupa chest/marker standar yang aman.

Durasi dan isi kematian perlu playtest. Semua item penting yang dijanjikan terlindungi harus diuji saat disconnect dan restart.

## 14.4 PvP

- Off secara default di kampung dan area normal.
- Aktif di arena atau event opt-in.
- Zona merah dapat menyediakan tantangan PvP terpisah di fase lanjut, tetapi tidak mengubah seluruh Sabuk Kala menjadi area bebas grief.
- Tidak ada raid offline terhadap rumah pemain.
- Pembatasan anti-cheat diuji untuk pemain Bedrock agar perbedaan movement tidak salah dianggap cheat.

## 14.5 Teleport dan perjalanan

- Travel awal menggunakan jalan, jembatan, kapal, dan landmark agar map terasa nyata.
- Teleport hanya ke hub/waystation yang telah ditemukan atau endpoint yang aman, dengan cooldown/cost opsional.
- Teleport dilarang menghindari fase tertentu dari event atau masuk langsung ke area bahaya yang belum dibuka.
- Fitur kembali ke rumah/hub bisa disediakan setelah tutorial, tetapi harus mempunyai aturan yang sederhana dan tidak menghapus kebutuhan eksplorasi.

---

# 15. Voice chat Java–Bedrock/MCPE

## 15.1 Kenyataan teknis

Geyser menerjemahkan protokol gameplay Java–Bedrock; itu tidak berarti Minecraft Bedrock memiliki voice chat proximity universal yang bisa diaktifkan hanya dengan membuat sebuah menu plugin. Mikrofon dan speaker dikendalikan aplikasi klien/sistem operasi. Plugin server tidak dapat secara bebas mengambil input mic HP atau mengubah perangkat audio output.

VoiceCraft adalah ekosistem terpisah yang berisi aplikasi client, backend voice, dan bridge Java. Dokumentasinya menyebut dukungan client native desktop dan mobile; setiap pemain yang ingin berbicara/mendengar proximity voice perlu menjalankan client tersebut. Pada jalur Geyser/Java, kandidat bridge adalah VoiceCraft.Java (nama lama pada beberapa materi bisa disebut GeyserVoice). Sumber: https://docs.voicecraft.chat/download, https://docs.voicecraft.chat/client/installation, https://github.com/AvionBlock/VoiceCraft.Java

## 15.2 Rekomendasi

**Pilih VoiceCraft.Client + VoiceCraft.Java sebagai kandidat pertama untuk proof-of-concept**, bukan langsung menulis transport voice sendiri. Gunakan versi client/server/bridge yang cocok dan ikuti instruksi deployment resmi. Jangan menganggapnya sudah siap produksi hanya karena tertera dukungan Java/Bedrock: uji mobile foreground/background, UDP/firewall, audio latency, reconnect, Geyser/Floodgate UUID, dan versi Minecraft yang benar-benar dipakai.

VoiceCraft memiliki aplikasi Android/iOS dan desktop. Dokumentasi yang diperiksa menyatakan browser client lama tidak lagi termasuk rilis inti 1.7, dan tabel perangkat menyatakan Xbox/PlayStation tidak didukung native. Maka:

- Java desktop: dapat memakai aplikasi desktop yang cocok.
- Bedrock di Android: dapat diuji melalui aplikasi VoiceCraft Android dan Minecraft berjalan bersamaan.
- Bedrock di iOS: jalur klien tersedia tetapi distribusi/permission perlu diuji pada perangkat sebenarnya.
- Bedrock di konsol: jangan menjanjikan voice proximity native lewat VoiceCraft; sediakan gameplay normal dan opsi komunikasi alternatif yang sesuai aturan/platform.

## 15.3 Pengalaman voice yang diinginkan

- Proximity voice: suara terdengar sesuai jarak, dengan pelemahan volume saat pemain menjauh jika didukung konfigurasi integrasi.
- Push-to-talk: default yang jelas, ditambah opsi voice activation jika backend/client mendukung dan pengujian menunjukkan aman.
- Mute diri dan mute pemain lain.
- Petunjuk pengaturan mic/output di aplikasi VoiceCraft, bukan berpura-pura bahwa GUI Minecraft dapat memilih perangkat audio sistem.
- Label status voice opsional di UI/Kompas jika API integrasi menyediakan status yang dapat dipercaya.
- Kanal ekspedisi/radio hanya fase lanjut jika API mendukung secara stabil dan tidak menjadi kewajiban quest.
- Tidak merekam suara secara default; jelaskan privasi, laporan penyalahgunaan, dan aturan komunitas.
- Voice selalu opsional. Semua quest inti tetap dapat dijalankan melalui teks, petunjuk visual, dan menu.

## 15.4 UI voice untuk MCPE

Kompas Bedrock dapat memiliki bagian **Suara & Bantuan** dengan opsi:

1. Status: belum disiapkan / panduan tersedia / terhubung jika status itu dapat dibaca secara valid.
2. Petunjuk perangkat: install aplikasi resmi, beri izin mic, tambahkan endpoint server, dan bind akun sesuai dokumentasi.
3. Tombol salin endpoint/kode jika platform UI mendukung; fallback menampilkan teks yang mudah disalin.
4. Panduan memilih input, output, push-to-talk, dan volume.
5. Tombol mute bantuan atau panduan moderasi.

Menu Minecraft hanya menjadi pusat petunjuk/status. Ia tidak dapat membuka setting sistem operasi dengan jaminan, menghidupkan mic secara diam-diam, atau mengubah audio routing tanpa dukungan aplikasi klien.

## 15.5 Alternatif yang belum direkomendasikan untuk publik

SimpleVoice-Geyser menawarkan pendekatan berbasis website untuk pemain Java/Bedrock dan memerlukan integrasi terkait seperti Simple Voice Chat serta Geyser/Floodgate sesuai mod/plugin stack. Namun, halaman proyek yang diperiksa menandai status pengembangan dan menyatakan audio client-to-server tidak dienkripsi secara default; untuk WAN diperlukan proxy/HTTPS yang sesuai. Karena itu, jangan jadikan alternatif ini jalur utama produksi sebelum audit keamanan, pengujian versi, dan pengujian jaringan. Sumber: https://modrinth.com/project/GJLuArlK

## 15.6 Voice acceptance test

- Uji 2 Java player dan 2 Bedrock mobile players pada satu lokasi.
- Uji jarak 5, 20, 40, dan 60 blok serta area berbeda world.
- Uji tidak ada audio lintas dunia/instance yang tidak diharapkan.
- Uji mute, permission mic ditolak, koneksi voice gagal, app background, reconnection, dan server restart.
- Uji endpoint voice tidak dapat dipakai oleh pihak tak berizin.
- Pastikan text chat tetap cukup untuk menyelesaikan event.
- Catat perangkat/OS dan hasil, bukan hanya “voice sepertinya jalan”.

---

# 16. UI lintas Java dan Bedrock

## 16.1 Kompas Pengembara

Satu item utama atau shortcut yang membuka lima tab:

1. **Misi** — objective aktif, hadiah, dan petunjuk.
2. **Peta** — wilayah, landmark, zona Kala, dan rute yang ditemukan.
3. **Kampung** — rumah, claim, anggota, fasilitas, serta proyek.
4. **Progres** — profesi, Jurnal Kepulauan, reputasi, dan arsip.
5. **Panduan** — aturan, voice setup, pengaturan, dan bantuan.

Pasar/event bisa menjadi sub-menu; hindari menaruh semua fitur di home screen.

## 16.2 Abstraksi UI custom

Kode bisnis harus memanggil satu aksi seperti `openJournal(player)` atau `showClaimConfirmation(player, claimId)`; implementasi presentasinya dipilih menurut kemampuan klien:

- Java: inventory GUI atau dialog/command yang didukung platform.
- Bedrock via Floodgate: Bedrock form sederhana yang ramah sentuh jika API relevan tersedia.
- Fallback: chat instructions/command untuk fungsi penting.

Jangan menggunakan custom head sebagai ikon penting. Dokumentasi Geyser mencantumkan beberapa batasan lintas edisi seperti custom heads di inventory dan clickable links di chat; daftar ini perlu dicek ulang saat versi target berubah: https://geysermc.org/wiki/geyser/current-limitations/

## 16.3 Aturan menu

- Tidak melakukan aksi destruktif langsung melalui satu klik tanpa konfirmasi.
- Tampilkan biaya, syarat, cooldown, efek, dan zona risiko sebelum commit.
- Jangan mengirim form berulang pada setiap tick.
- Tutup/refresh menu saat data berubah; hindari click-spam untuk menyegarkan konten.
- Nama pemain bukan ID database.

---

# 17. Arsitektur server dan penyimpanan data

## 17.1 Topologi awal

```text
Java Edition Client ──────┐
                          ├── Paper Server ── KalaCore (plugin modular)
Bedrock/MCPE Client ─ Geyser + Floodgate

Opsional saat voice terbukti:
Java/Bedrock player ─ VoiceCraft.Client ─ VoiceCraft.Server
                                   ▲
                         VoiceCraft.Java bridge di Paper

Paper ─ SQLite (local prototype) / MariaDB (production saat dibutuhkan)
Paper ─ Resource packs: Java pack + Bedrock pack/mappings via Geyser
Monitoring ─ spark + Plan (akses dashboard dibatasi)
```

Satu proses Paper dengan beberapa world lebih mudah untuk MVP. Velocity dan beberapa backend ditambahkan hanya jika ada kebutuhan nyata seperti pemisahan beban, lobby, atau regional backend.

## 17.2 Versi server dan Java

Pada pemeriksaan 24 Oktober 2026, dokumentasi Paper yang dibuka menyebut Java 25 untuk Minecraft 26.1 ke atas. Dokumentasi Iris yang dibuka menyebut target Paper 26.1.2–26.3. Ini adalah snapshot dokumentasi, bukan instruksi untuk mengupgrade server produksi secara otomatis. Sumber: https://docs.papermc.io/paper/getting-started/ dan https://github.com/VolmitSoftware/Iris

Sebelum mengunci versi:

1. Pilih satu versi patch Minecraft target.
2. Cocokkan versi Paper, Geyser, Floodgate, plugin voice, generator, resource pack/mappings, dan library API.
3. Install hanya artifacts yang mendukung versi tersebut.
4. Jalankan tes lintas edisi dan backup/restore.
5. Pin nomor versi dan checksum/asal download dalam `docs/COMPATIBILITY.md`.
6. Jangan menjalankan updater otomatis pada plugin/game version di produksi.

## 17.3 Data store

- Prototipe: SQLite cukup untuk satu plugin dengan volume rendah.
- Produksi: MariaDB/MySQL dapat dipertimbangkan setelah kebutuhan concurrency/operasional jelas.
- Gunakan connection pool untuk database eksternal.
- Semua query database berpotensi lambat tidak boleh dijalankan blocking pada main server thread.
- Beri batas waktu dan fallback error yang jelas.
- Semua data pemain keyed by stable UUID/identity abstraction, bukan username.
- Simpan schema version dan migration scripts.
- Untuk ekonomi/pemindahan item, gunakan transaksi atomik atau pola idempotent yang didesain secara eksplisit.

## 17.4 Model data minimum

Entitas awal yang mungkin diperlukan:

- `player_profile` — UUID internal, identitas platform terhubung, locale, created_at, last_seen.
- `player_progress` — tutorial flag, quest flags, level/XP profesi, jurnal discovery.
- `claims` — claim_id, owner_id, bounds, world_id, risk_mode, version.
- `claim_members` — claim_id, player_id, role.
- `season_state` — season_id, phase, timestamps, world/event state.
- `event_contribution` — event_id, player_id, contribution_type, amount, timestamp.
- `recovery_job` — job_id, claim_id, snapshot_id, state, progress, last_error.
- `snapshot_manifest` — snapshot_id, event_id, claim_id, checksum, schema_version, path.
- `vault_transaction` — tx_id, player_id, source_snapshot_id, state, payload_ref.
- `wallet` — player_id, balance, version.
- `transaction_ledger` — tx_id, actor IDs, item/money deltas, purpose, status, created_at.
- `market_listing` — listing_id, seller_id, item payload/key, quantity, price, status, expiry.
- `journal_entry` — player_id, entry_id, discovered_at, source.
- `audit_event` — actor, action, object, outcome, correlation_id, timestamp.

Ini bukan skema SQL final. AI harus menurunkan migrasi, indeks, constraints, dan transaksi dari use cases yang disetujui. Jangan langsung membuat semua tabel sebelum fase itu dibangun.

---

# 18. Plugin, mod, dan tool: pilihan, fungsi, serta batasan

## 18.1 Prinsip memilih dependency

- Download hanya dari repo/situs resmi atau distributor tepercaya yang ditautkan sumber primer.
- Baca lisensi, dependency, syarat versi, dan status aktif sebelum production.
- Hindari dua plugin yang mengontrol domain data yang sama (dua economy, dua claims, dua quest engine).
- Jangan menginstall plugin hanya karena fiturnya banyak.
- Semua dependency harus terdaftar di `docs/COMPATIBILITY.md`: versi, URL, lisensi/harga, fungsi, status, serta alasan dipakai.
- Uji satu dependency baru pada staging, bukan langsung di world utama.

## 18.2 Stack dasar yang direkomendasikan

| Komponen | Status | Fungsi | Perlakuan |
|---|---|---|---|
| **Paper** — https://papermc.io/downloads/paper | Wajib | Runtime server Java dan API plugin | Pin versi patch; tes semua plugin |
| **Geyser** — https://github.com/GeyserMC/Geyser | Wajib untuk Bedrock | Jembatan Bedrock ke server Java | Gunakan build yang cocok; konfigurasi jaringan/port Bedrock |
| **Floodgate** — https://github.com/GeyserMC/Floodgate | Wajib jika menggunakan jalur Floodgate | Memudahkan autentikasi pemain Bedrock sesuai konfigurasi Geyser | Gunakan metode autentikasi resmi; jangan mematikan keamanan online-mode tanpa memahami konfigurasi |
| **LuckPerms** — https://github.com/LuckPerms/LuckPerms | Wajib | Permission staff/player | Definisikan permission plugin custom sejak awal |
| **PlaceholderAPI** — https://github.com/PlaceholderAPI/PlaceholderAPI | Opsional | Integrasi placeholder dengan plugin lain | Tambahkan hanya jika ada dependency UI/scoreboard yang membutuhkan |
| **CoreProtect** — https://github.com/PlayPro/CoreProtect | Sangat disarankan | Audit blok, container, rollback insiden | Bukan pengganti backup dan bukan pengganti snapshot Kala |
| **Chunky** — https://github.com/pop4959/Chunky | Sangat disarankan | Pregenerate chunk supaya eksplorasi awal tidak memaksa banyak generasi saat live | Jalankan sebelum publikasi dan dengan batas beban |
| **Plan** — https://github.com/plan-player-analytics/Plan | Disarankan setelah beta | Analitik aktivitas dan retensi | Batasi akses dashboard; aktifkan HTTPS/login jika diakses dari luar |
| **spark** — https://github.com/lucko/spark | Wajib untuk diagnosis | Profiling CPU/tick/heap | Paper modern membundel spark; gunakan versi bawaan kecuali ada alasan upgrade |
| **WorldEdit** — https://github.com/EngineHub/WorldEdit | Disarankan untuk admin/build | Build map dan manipulasi struktur | Permission ketat, tidak untuk semua player |
| **FastAsyncWorldEdit (FAWE)** — https://github.com/IntellectualSites/FastAsyncWorldEdit | Opsional | Operasi besar dengan queue/performance tools | Pilih bila kebutuhan builder nyata; jangan anggap snapshot item otomatis aman |
| **Citizens** — https://github.com/CitizensDev/Citizens2 | Opsional/disarankan | NPC panduan/pasar | NPC statis secukupnya; jangan buat ratusan NPC aktif |
| **Multiverse-Core** — https://github.com/Multiverse/Multiverse-Core | Opsional | Memudahkan pengelolaan beberapa world dalam satu server | Tambahkan bila workflow multi-world membutuhkan; jangan sekaligus memperkenalkan proxy tanpa alasan |
| **DiscordSRV** — https://github.com/discordsrv/discordsrv | Fase berikutnya | Relay chat/alert server dan Discord | Gunakan channel terbatas, bot dengan permission minimal, tidak membocorkan console/admin |

## 18.3 World generator dan struktur

| Tool | Cocok untuk | Keputusan |
|---|---|---|
| **WorldPainter** — https://www.worldpainter.net/ | Map utama terkurasi | Pilihan utama untuk bentuk kepulauan/Gunung Murka sebelum server hidup |
| **Iris** — https://github.com/VolmitSoftware/Iris | Worldgen penuh yang dapat dikonfigurasi melalui packs | Kandidat terbaik jika ingin world/resource biome yang benar-benar custom; perlu belajar, cek license/build/version dan tes performance |
| **TerraformGenerator** — https://github.com/Hex27/TerraformGenerator | Terrain dengan rasa vanilla yang ditingkatkan | Bukan pilihan utama produksi sebelum status maintenance/bug dan support versi patch diperiksa. Repo menyatakan alpha dan memberi peringatan untuk lingkungan produksi yang butuh dukungan ketat |
| **BetterStructures** — https://github.com/MagmaGuy/BetterStructures | Menambah schematic/ruins/dungeon pada world yang telah digenerasi | Opsional untuk resource/exploration world; cek release yang cocok, kebutuhan WorldEdit, pack dan lisensi setiap pack |
| **Chunky** | Pregeneration | Dipakai untuk pregenerate world sebelum pemain mengeksplorasi; tidak mengganti world generator |

**Jangan memasang Iris dan TerraformGenerator pada satu world dengan asumsi keduanya menumpuk.** Pilih satu generator per world. Main world bisa memakai map WorldPainter dan tidak menggunakan generator custom tambahan. BetterStructures adalah lapisan struktur, bukan generator terrain.

### Pilihan final rancangan world

- `kala_main`: WorldPainter + finishing manual; persisten, tidak di-reset.
- `kala_resources`: pilih **Iris atau vanilla terrain + BetterStructures** berdasarkan prototipe. Jangan menambahkan dua generator.
- `kala_expedition`: map musiman terkurasi/di-rotate dengan prosedur backup atau generator yang sama dengan resource world, bukan campuran acak.

Untuk MVP, jika belajar Iris akan menghabiskan waktu terlalu banyak, gunakan terrain vanilla dengan area resource yang dibatasi lalu tambahkan BetterStructures setelah uji. Map utama terkurasi tetap memberi identitas yang kuat.

## 18.4 Quest, NPC, ekonomi, dan item

| Plugin/tool | Peran | Rekomendasi |
|---|---|---|
| **Citizens** | NPC | Pakai NPC untuk wajah pemandu, pedagang, dan pembuka quest; logika/izin gameplay tetap di KalaCore |
| **BetonQuest** — https://github.com/BetonQuest/BetonQuest | Quest/conversation scripting yang matang | Kandidat jika ingin banyak dialog bercabang tanpa membangun editor quest dari nol. Uji penggunaan UI pada Bedrock dan integrasi Kala dulu; jangan duplikasi quest engine |
| **Vault** — https://github.com/MilkBowl/Vault | API ekonomi/permission lama yang banyak dipakai plugin lain | Opsional; gunakan jika perlu kompatibilitas plugin ekonomi lain. Jika KalaEconomy jadi satu-satunya ekonomi, jangan menambahkan lapisan tanpa kebutuhan |
| **EssentialsX** — https://github.com/EssentialsX/Essentials | Command/QoL dan economy sementara | Tidak wajib. Bisa mempersingkat prototype, tetapi audit command dan matikan fitur yang bertabrakan dengan desain Kala |
| **Jobs Reborn** — https://github.com/Zrips/Jobs | Jobs generik | Tidak direkomendasikan sebagai profesi final karena Kala butuh profesi terhubung langsung dengan Kala dan proyek komunitas. Bisa diuji sebagai prototipe terbatas saja |

**Keputusan:** tutorial awal dapat memakai dialog NPC sederhana + quest YAML di KalaGuide. Jika alur cerita bertambah besar, lakukan spike test BetonQuest pada Bedrock sebelum menentukan apakah quest engine tetap external atau menjadi sistem custom. Jangan menulis sistem dialog superkompleks pada MVP.

## 18.5 Resource pack dan custom items

| Tool | Kegunaan | Risiko/keputusan |
|---|---|---|
| **Blockbench** — https://www.blockbench.net/ | Model/texture item dan dekorasi | Gunakan model sederhana; asset dibuat sesuai palet Kala |
| **Java resource pack** | Texture/item model Java | Buat versi terpisah dan commit source assets |
| **Bedrock resource pack** | Agar custom items terlihat di MCPE | Dikelola/dihasilkan terpisah dan dites di device nyata |
| **Geyser custom mappings** — https://geysermc.org/wiki/geyser/custom-items/ | Memetakan item custom Java ke representasi Bedrock | Diperlukan untuk item tertentu; Geyser tidak otomatis mengonversi Java pack atau membuat semua mapping |
| **Rainbow** — https://github.com/GeyserMC/Rainbow | Membantu alur konversi aset/mapping | Anggap bantuan eksperimental/alat konversi, bukan jaminan pack selesai otomatis |
| **Oraxen** — https://docs.oraxen.com/ | Plugin berbayar untuk custom content + pack generation di Paper | Bukan dependency wajib. Jika dipilih, uji integrasi Geyser/Bedrock secara eksplisit; dokumentasi Oraxen tidak menggantikan kebutuhan mapping/pack Bedrock |
| **ItemsAdder** — https://wiki.itemsadder.com/faq/geyser/ | Custom content/resource pack | Dokumentasi FAQ yang diperiksa menyatakan Geyser tidak didukung resmi saat ini; jangan jadikan fondasi wajib untuk server crossplay tanpa tes dan verifikasi terbaru |

**Rekomendasi final:** jangan membeli Oraxen/ItemsAdder pada tahap ide atau prototipe. Mulai dengan vanilla items + nama/lore dan pack minimal buatan sendiri. Setelah item sistem benar-benar dibutuhkan, evaluasi satu solusi saja berdasarkan integrasi Bedrock, lisensi, biaya, dan kompatibilitas.

## 18.6 Voice chat

- **VoiceCraft** — https://github.com/AvionBlock/VoiceCraft
- **VoiceCraft.Java** — https://github.com/AvionBlock/VoiceCraft.Java
- **Dokumentasi download** — https://docs.voicecraft.chat/download

Install hanya setelah server dasar stabil. Gunakan release resmi yang sama ekosistem versinya; jangan build source sendiri jika tidak dibutuhkan. Periksa requirements OS, port UDP/TCP, autentikasi, mode proxy/direct, dan backup config.

## 18.7 Deployment panel

Pilihan tool operasional, tidak berpengaruh langsung pada gameplay:

- **Pelican Panel** — https://github.com/pelican-dev/panel
- **Pterodactyl Panel** — https://github.com/pterodactyl/panel
- **itzg/docker-minecraft-server** — https://github.com/itzg/docker-minecraft-server

Jangan pasang panel pada laptop lokal hanya untuk membuat prototipe rumit. Untuk lokal, start script biasa sudah cukup. Panel baru penting ketika server dipindahkan ke host produksi dan perlu manajemen staf.

---

# 19. Custom development: apa yang harus dibangun dan bagaimana

## 19.1 Bentuk proyek

Mulai dari **satu repository dan satu plugin JAR `KalaCore` yang modular**, bukan enam plugin terpisah yang saling berkomunikasi sebelum domain datanya stabil. Di dalam satu codebase, modul tetap terpisah melalui package/service/API internal. Jika beban tim bertambah, modul dapat dipisahkan setelah batas integrasi jelas.

Nama package contoh: `id.kalanusantara.core`.

```text
KalaCore
├── bootstrap/       # entrypoint plugin, dependency wiring, lifecycle
├── config/          # config validation, defaults, reload rules
├── identity/        # stable player identity, Java/Bedrock mapping
├── season/          # state machine musim dan phase transitions
├── event/           # event registry, schedule, contribution tracking
├── claim/           # claim bounds, roles, safety/opt-in exposure
├── snapshot/        # manifests, snapshot jobs, checksums
├── recovery/        # vault, restore queue, idempotency
├── resource/        # resource nodes, resets, resource regions
├── economy/         # Kepeng, ledger, market listings
├── profession/      # profession, XP, perks, activity triggers
├── quest/           # objectives, prerequisites, rewards, quest state
├── journal/         # discovery, collection entries, seasonal records
├── ui/              # Java UI and Bedrock form adapter abstraction
├── voice/           # help/status integration only, not audio transport
├── integration/     # Geyser/Floodgate, Citizens, WorldEdit etc. adapters
├── storage/         # repository interfaces and SQLite/MariaDB implementation
├── audit/           # structured audit log
└── metrics/         # low-cardinality operational/gameplay counters
```

## 19.2 Apa yang tidak perlu di-custom dari awal

Tidak perlu menulis sendiri:

- koneksi Bedrock ke Java (pakai Geyser);
- sistem permission umum (LuckPerms);
- block logging/rollback umum (CoreProtect);
- NPC movement/creation jika Citizens cukup;
- profiler (spark);
- panel hosting;
- world editor;
- generic server worldgen jika map utama memang dibuat offline;
- audio streaming voice;
- UI custom font untuk setiap ikon.

## 19.3 Custom modules dan definisi tugasnya

### KalaCore / Bootstrap

Tugas: lifecycle, dependency checks, config, layanan internal, perintah admin, dan status kesehatan plugin. Tidak memuat semua logika ke satu class besar.

### KalaSeason

Tugas: state machine fase musim, schedule UTC, pengumuman, transisi aman, dan event hooks. Semua fase dapat dikonfigurasi. Saat plugin startup, validasi bahwa state dan waktu konsisten.

### KalaGuide

Tugas: Kompas, quest tutorial/seasonal, hint, petunjuk Bedrock, dan ringkasan aktivitas. Quest data disimpan dalam YAML/JSON yang divalidasi schema-nya. Jangan hardcode seluruh dialog dalam class Java.

### KalaClaim

Tugas (fase lanjut): claim ID, batas area, roles, status aman/opt-in, API izin, dan integrasi proteksi. MVP memakai GriefPrevention sampai klaim custom lulus tes migrasi dan keamanan.

### KalaSnapshot/KalaRecovery

Tugas: membuat snapshot pada plot yang eksplisit ikut event, menyimpan manifest, melindungi isi container, mengatur Brankas Pengungsian, menjalankan restore queue, dan menangani crash/restart. Ini bukan sekadar memanggil FAWE copy/paste.

### KalaResource

Tugas: resource region, reset schedule, pembukaan world baru, pregeneration control, dan node yang tercatat. Tidak menjalankan scan seluruh world tiap tick.

### KalaEconomy

Tugas: Kepeng, transaksi ledger, NPC price/reward policy, market listing, pajak, antiduplikasi, dan laporan admin. Semua perubahan saldo memakai transaksi dan audit.

### KalaProfession

Tugas: pilihan profesi, XP, reward/perk, cooldown, anti-exploit, dan interaksi dengan quest/event. Aktivitas valid diperiksa oleh server.

### KalaJournal

Tugas: discovery, koleksi, seasonal archive, dan kontribusi. Discovery harus idempotent agar event yang terpicu dua kali tidak menggandakan hadiah.

### KalaUI

Tugas: interface tindakan yang sama tetapi presenter berbeda untuk Java/Bedrock/fallback. UI tidak boleh berisi aturan ekonomi atau hak akses; ia memanggil services.

### KalaVoiceIntegration

Tugas: link panduan, perintah bind, help/status jika API tersedia, policy opt-in, dan feedback kegagalan. Jangan implementasikan audio codec/network sendiri.

### KalaMetrics

Tugas: metrik minimal seperti jumlah quest selesai, event participation, error restore, TPS/MSPT sample, dan transisi season. Jangan merekam percakapan voice atau data sensitif.

## 19.4 Aturan kode wajib

- Semua gameplay authority berada di server; jangan percaya nilai harga, item ID, reward, cooldown, atau permission dari client.
- UUID/stable identity menjadi key data, bukan username.
- Semua command punya permission eksplisit.
- Semua config divalidasi saat startup dan memberi error yang actionable.
- Semua operasi ekonomi/pemindahan item punya idempotency/transaction design.
- Semua pekerjaan blok besar memakai queue/budget, bukan satu operasi synchronous yang mengubah ribuan blok di satu tick.
- Jangan baca/tulis world state dari async thread kecuali API dan operasi tersebut memang aman menurut dokumentasi platform.
- I/O database tidak menghambat main thread.
- Semua task scheduled memiliki lifecycle/cancellation yang jelas.
- Semua pesan pemain dilokalkan.
- Jangan membuat dependency wajib ke plugin berbayar yang belum disetujui.
- Bila dependency opsional tidak ditemukan, plugin harus menonaktifkan integrasi terkait dengan pesan yang jelas atau gagal startup secara eksplisit jika itu kritikal.
- Jangan menelan exception tanpa log konteks/correlation ID.

## 19.5 API dan event internal

Contoh event custom yang mungkin dibutuhkan:

- `SeasonPhaseChangedEvent`
- `KalaWarningIssuedEvent`
- `KalaEventStartedEvent`
- `KalaEventCompletedEvent`
- `ClaimRiskChangedEvent`
- `SnapshotJobStateChangedEvent`
- `RecoveryCompletedEvent`
- `PlayerDiscoveryRecordedEvent`
- `CommunityProjectProgressEvent`
- `MarketTransactionCommittedEvent`

Event tersebut harus membawa ID, bukan object yang sensitif/berat bila tidak dibutuhkan. Hindari dependency siklik antar package.

## 19.6 Konfigurasi yang harus bisa diubah tanpa rebuild

- season duration / tanggal dan timezone;
- zona dan deskripsi risikonya;
- cooldown, batas klaim, ukuran plot;
- biaya pemulihan/asuransi;
- reward, harga, pajak dan NPC limits;
- profession XP dan unlock thresholds;
- event schedule dan hadiah;
- batas pekerjaan block queue dan pause threshold;
- bahasa dan pesan;
- voice endpoint/instruksi serta link dokumen yang benar;
- daftar world yang dikelola;
- feature flags agar modul bisa diuji bertahap.

---

# 20. Resource pack, texture, dan aset visual

## 20.1 Fase aset

### Fase 0 — tanpa pack wajib

Gunakan item vanilla, nama item, lore, NPC, papan, map, dan blok vanilla. Ini mempermudah tes gameplay dan menjamin pack tidak menutupi bug fitur.

### Fase 1 — pack ringan

Buat aset prioritas:

- Kompas Pengembara;
- ikon Kepeng atau representasi pasar;
- Buku Panduan;
- ikon fase/zona Kala;
- bahan unik maksimal beberapa item;
- beberapa kosmetik alat;
- dekorasi kampung kecil.

Semua gameplay masih harus terbaca tanpa asset tersebut.

### Fase 2 — pack visual konsisten

Tambah texture/variant dekorasi, banner kampung, alat profesi, material pasca-Kala, dan prop kecil. Uji file size, memori, loading, item mapping, serta resource-pack refusal behavior.

### Fase 3 — model 3D tertentu

Hanya untuk aset yang benar-benar mengubah identitas visual dan setelah mapping Java–Bedrock stabil. Hindari blok custom penuh/animasi rumit di fase awal.

## 20.2 Spesifikasi art direction

- Pixel art 16x atau 32x untuk mayoritas item.
- Siluet jelas saat item berada di hotbar/GUI.
- Satu item = satu bentuk dan satu fungsi yang mudah dikenali.
- Batasi palet; jangan memakai glow/rainbow untuk semua item.
- Ikon menu menggunakan vanilla item atau ikon konsisten; hindari custom skull sebagai ikon wajib.
- Aset budaya harus dibuat orisinal atau memiliki lisensi yang jelas.
- Pack tidak boleh mengubah block biasa sedemikian rupa sehingga pemain Bedrock salah membaca collision/fungsi.

## 20.3 Folder aset yang disarankan

```text
assets/
├── source/                   # file asli Blockbench/PNG/source yang belum dipaketkan
├── java-pack/
│   └── assets/kala/          # textures, item models, language, sounds
├── bedrock-pack/
│   ├── manifest.json
│   ├── textures/
│   ├── models/
│   └── texts/
├── geyser-mappings/          # JSON mappings yang diuji untuk versi target
├── palette.md
└── asset-register.csv        # asset, pembuat, lisensi, versi, lokasi file
```

## 20.4 Penting tentang Java–Bedrock pack

Geyser tidak otomatis mengonversi resource pack Java menjadi Bedrock dan tidak otomatis menghasilkan semua custom item mapping. Custom item membutuhkan Bedrock resource pack dan mapping yang sesuai. Referensi: https://geysermc.org/wiki/geyser/custom-items/

Jangan menjadikan “jalankan converter lalu selesai” sebagai kriteria release. Converter dapat mempercepat proses tetapi hasil wajib diuji pada Java client dan Bedrock device nyata. Custom item v1/item-model mapping memiliki perubahan terkait versi; baca format dokumentasi terbaru ketika mengimplementasikan.

## 20.5 Prosedur aset baru

1. Tulis tujuan gameplay dan fungsi visual.
2. Buat mockup item tanpa texture custom.
3. Pastikan ikon mudah dimengerti.
4. Buat aset source di Blockbench atau editor gambar.
5. Buat Java pack dan bedrock counterpart/mapping.
6. Cek pack file size, paths, metadata dan namespace.
7. Uji pada Java.
8. Uji pada Bedrock HP, termasuk GUI/held item dan reconnect.
9. Pastikan item lama dan recipe tidak rusak.
10. Commit source + mapping + catatan versi; jangan commit JAR berlisensi atau asset yang tidak boleh didistribusikan.

---

# 21. Performa, stabilitas, keamanan, dan backup

## 21.1 Profil beban utama

Beban yang harus dikendalikan:

- chunk generation saat pemain baru menjelajah;
- entity/farm yang terlalu banyak;
- plugin tick loops atau scans global;
- event yang mengubah banyak blok sekaligus;
- snapshot/restoration klaim;
- query DB synchronous;
- particle/sound spam;
- resource-pack download dan reconnect;
- voice server/network yang menambah proses serta port;
- plugin dengan fitur duplikat atau konflik event.

## 21.2 Optimasi map

- Pregenerate map utama dan area resource world sesuai batas yang disetujui melalui Chunky atau tool sejenis.
- Tetapkan world border.
- Hindari membuat ribuan block entities, armor stands, hologram, mobs, dan item entities sebagai dekorasi.
- Jangan gunakan redstone clocks/farms besar di kampung publik tanpa batas.
- Batasi NPC hidup; gunakan entity sederhana/statis jika cukup.
- Gunakan struktur/ruins statis daripada mob AI berat untuk setiap penemuan.

## 21.3 Optimasi plugin

- Event-driven lebih disukai daripada loop per tick.
- Jika polling diperlukan, lakukan pada interval wajar dan hanya untuk objek aktif.
- Cache config dan data read-heavy secara aman.
- Batch writes ke DB; jangan menulis database setiap tick.
- Batasi cardinality metric: jangan membuat label per player UUID.
- Matikan logging debug pada produksi; audit tindakan penting tetap disimpan.
- Profiling dilakukan saat beban bermasalah terjadi. Paper menyertakan spark pada rilis modern; dokumentasi: https://docs.papermc.io/paper/profiling/

## 21.4 Pengaturan view dan simulation distance

Mulai dari konfigurasi konservatif yang sesuai hardware, bukan mengejar render jauh. Sebagai titik awal playtest, pertimbangkan server `view-distance` 8–10 dan `simulation-distance` 5–6, lalu ukur. Ini bukan angka universal; hardware, farms, jumlah pemain, generator, dan plugin akan mengubah hasil. Paper mendefinisikan keduanya sebagai jarak chunk yang dilayani/ditick: https://docs.papermc.io/paper/reference/server-properties/

**Gunung yang terlihat dari tempat penting harus diselesaikan melalui skala map dan komposisi landmark, bukan dengan menjanjikan view-distance tinggi untuk seluruh pemain.**

## 21.5 Prosedur backup

- Backup otomatis world, config, plugin data, pack, dan database.
- Backup harus konsisten: gunakan prosedur aman untuk penyimpanan aktif (mis. backup database-native dan shutdown/flush world sesuai prosedur host).
- Simpan beberapa generasi backup di luar mesin/volume utama.
- Lakukan restore drill berkala ke staging; backup yang belum diuji restore belum terbukti dapat dipakai.
- Snapshot Kala bukan pengganti full backup server.
- Jangan mengekspos token bot, password DB, secret voice, atau credential panel ke repository.

## 21.6 Security checklist

- Permission least privilege.
- Admin console tidak pernah dibuka ke Discord umum.
- Web dashboard Plan hanya dapat diakses lewat autentikasi dan HTTPS jika dibuka ke jaringan.
- Validasi semua input command, nama item, lokasi, dan ID.
- Cegah duplikasi item/saldo saat reconnect dan crash.
- Jangan mengandalkan nama Bedrock/Java sebagai identity key.
- Lindungi endpoint voice dan kredensial backend.
- Moderasi chat/voice memiliki proses laporan, mute, block, dan penanganan pelanggaran.
- Jangan merekam voice tanpa kebutuhan, persetujuan, kebijakan, dan dasar yang jelas.
- Dokumentasikan cara melaporkan exploit atau kehilangan data.

---

# 22. Tahapan pengembangan dan acceptance criteria

## Fase 0 — keputusan dan compatibility spike

Tujuan: menghilangkan risiko versi dan arsitektur sebelum menulis banyak kode.

Pekerjaan:
- pilih Minecraft/Paper patch yang akan diuji;
- buat `COMPATIBILITY.md`;
- jalankan Paper bersih;
- pasang Geyser/Floodgate;
- uji Java + Bedrock mobile;
- pilih metode map utama;
- buat world kecil dan satu lokasi tutorial;
- uji voice POC, tetapi tidak menghalangi gameplay jika gagal;
- ukur baseline spark/MSPT.

Lulus jika:
- Java dan Bedrock dapat join, move, chat dan disconnect/reconnect;
- tidak ada plugin penting yang error saat startup;
- baseline disimpan;
- versi/config bisa direproduksi dari repo.

## Fase 1 — playable vertical slice

Pekerjaan:
- Labuhan Awal mini-map;
- NPC Mbah Kala atau petunjuk alternatif;
- Kompas basic;
- tutorial 15–30 menit;
- claim sementara GriefPrevention;
- satu pesanan NPC/ekonomi sederhana;
- satu proyek kampung;
- satu discovery Jurnal;
- satu event Kala non-destruktif.

Lulus jika penguji tanpa instruksi developer memahami tujuan, menyelesaikan tutorial, dan menemukan dua aktivitas lanjut.

## Fase 2 — inti komunitas

Pekerjaan:
- profesi versi awal;
- project/community contribution;
- Jurnal Kepulauan;
- event weekly pertama;
- sistem market yang transaksinya aman atau shop sederhana;
- analitik retensi minimal;
- text/localization Indonesia + Inggris.

Lulus jika tidak ada duplikasi transaksi kritis, Bedrock dapat mengakses UI inti, dan event memberi perubahan yang tampak.

## Fase 3 — simulasi Kala terbatas

Pekerjaan:
- state machine musim;
- peringatan, peta risiko, countdown;
- project persiapan;
- plot opt-in;
- satu snapshot/restore uji terbatas;
- crash/restart recovery;
- audit log.

Lulus jika snapshot, vault, dan restore tidak kehilangan/menduplikasi container items dalam test cases, termasuk shutdown di tengah proses.

## Fase 4 — resource world dan content depth

Pekerjaan:
- resource world dan reset aman;
- pregeneration;
- satu generator (atau vanilla + structures) yang telah dikunci;
- variasi ruins/landmark;
- event mingguan tambahan;
- ekonomi dan profesi disetel berdasarkan data.

Lulus jika reset tidak menghapus world utama, player tidak tertinggal di area yang akan direset, dan resource baru tidak menimbulkan lag/generasi liar saat puncak event.

## Fase 5 — public beta

Pekerjaan:
- moderasi, rules, ticket, backups, restore drill;
- VoiceCraft setelah lulus POC;
- pack Java/Bedrock minimal;
- event season pertama penuh;
- metrik D1/D7/D30;
- load test dan bug triage.

Lulus jika server stabil untuk target beta hasil uji, alur onboarding mudah dimengerti, tidak ada exploit kritis yang diketahui, dan operasi admin terdokumentasi.

## Fase 6 — produksi dan seasonal updates

Pekerjaan:
- rilis rutin;
- kalender event;
- kosmetik etis bila monetisasi digunakan;
- ekspedisi seasonal;
- fitur baru dipilih dari metrik dan feedback.

Jangan membangun semua fase sekaligus. Setiap fase menghasilkan server yang tetap bisa dijalankan dan diuji.

---

# 23. Pengujian lintas perangkat dan playtest

## 23.1 Perangkat minimum pengujian

- Java client dengan keyboard/mouse.
- Bedrock/MCPE pada Android dengan layar sentuh.
- Bedrock di iOS jika dukungan iOS menjadi target publik.
- Setidaknya satu perangkat dengan render distance rendah/performa sederhana.
- Voice: setidaknya dua Android/desktop yang berbeda, serta iOS jika disupport.

Tidak cukup jika pengembang hanya menguji melalui Java client di PC.

## 23.2 Test matrix fitur

| Fitur | Java | Bedrock mobile | Restart/reconnect | Failure path |
|---|---|---|---|---|
| Login/tutorial | Wajib | Wajib | Wajib | spawn fallback |
| Kompas/menu | Wajib | Wajib | Wajib | command fallback |
| Claim | Wajib | Wajib | Wajib | denied/no permission |
| NPC/dialog | Wajib | Wajib | Wajib | NPC/plugin unavailable |
| Ekonomi/pasar | Wajib | Wajib | Wajib | transaction rollback |
| Jurnal | Wajib | Wajib | Wajib | repeated discovery |
| Kala warnings | Wajib | Wajib | Wajib | timezone/late login |
| Snapshot/restore | Wajib | Wajib | Wajib | crash mid-job |
| Resource reset | Wajib | Wajib | Wajib | player still inside |
| Voice | Wajib jika client ada | Wajib jika app kompatibel | Wajib | mic denied/disconnected |
| Resource pack | Wajib | Wajib | Wajib | pack declined/failed |

## 23.3 Playtest dengan kelompok berbeda

- builder yang hanya ingin membangun;
- explorer yang suka mencari lokasi;
- pedagang/crafter;
- casual yang online hanya sebentar;
- pemain MCPE yang memakai kontrol sentuh;
- pemain yang tidak menggunakan voice/Discord.

Tanyakan: “Apa yang kamu lakukan selama 15 menit terakhir?”, “Apa tujuanmu berikutnya?”, “Bagian mana yang bikin bingung?”, “Apakah kamu merasa harus online pada waktu tertentu?”, dan “Apa yang akan membuatmu kembali minggu depan?”

Jangan hanya meminta rating suka/tidak suka. Observasi perilaku dan completion rate lebih berguna.

---

# 24. Metrik keberhasilan dan cara memperbaiki kebosanan

## 24.1 Gameplay metrics

- completion rate tutorial;
- waktu hingga pemain melakukan aksi pertama yang mereka pilih sendiri;
- D1/D7/D30 retention;
- proporsi pemain yang menyelesaikan setidaknya satu aktivitas dalam sesi 10–20 menit;
- partisipasi event tanpa ketergantungan pada satu profesi;
- jumlah discovery baru per pemain aktif;
- jumlah proyek komunitas selesai;
- jumlah transaksi valid dan kegagalan transaksi;
- tingkat kembali setelah event Kala;
- persentase plot opt-in yang dipulihkan;
- jumlah pemain yang mengakses core feature melalui Bedrock.

## 24.2 Stability metrics

- MSPT/TPS dan spike pada fase event;
- durasi snapshot/restore dan kegagalan job;
- memory/GC pressure;
- crash/restart frequency;
- chunk generation saat live;
- error rate plugin;
- voice reconnect/latency/failure jika metrik tersedia tanpa mengumpulkan audio;
- ukuran resource pack dan failure-to-load rate.

## 24.3 Cara membaca kebosanan

- Banyak pemain berhenti setelah tutorial: onboarding atau tujuan berikutnya kurang jelas.
- Pemain hanya melakukan satu aktivitas: reward/akses untuk jalur lain tidak cukup menarik atau ekonomi memaksa satu strategi.
- Pemain berhenti setelah Kala: risiko terlalu tinggi, hadiah persiapan kurang berarti, atau fase pemulihan terlalu grindy.
- Pemain tidak kembali saat event: jadwal terlalu sempit, notifikasi buruk, atau event tidak berdampak.
- Bedrock completion jauh lebih rendah: UI terlalu rumit/tidak kompatibel atau interaksi memerlukan kontrol PC.
- Builder tidak punya tujuan: buat showcase, proyek publik, tema musiman dan fasilitas fungsional, bukan memaksa mereka bertarung.

Jangan langsung menambahkan fitur baru sebelum tahu masalah mana yang terjadi.

---

# 25. Struktur repository proyek

Struktur yang disarankan:

```text
kala-nusantara/
├── README.md
├── MASTER_DESIGN.md                  # salinan/tautan dokumen induk yang disetujui
├── CHANGELOG_DESIGN.md               # keputusan desain yang berubah
├── docs/
│   ├── COMPATIBILITY.md              # versi Paper/Geyser/Java/plugin/voice/pack
│   ├── ARCHITECTURE.md
│   ├── GAMEPLAY_RULES.md
│   ├── WORLD_MAP.md
│   ├── VOICE_SETUP.md
│   ├── SECURITY.md
│   ├── BACKUP_RESTORE.md
│   ├── TEST_PLAN.md
│   ├── RELEASE_RUNBOOK.md
│   └── ADR/                          # Architecture Decision Records
├── server/
│   ├── start-dev.sh
│   ├── server.properties.example
│   └── compose.yaml                  # hanya jika Docker dipakai
├── plugin/
│   └── kala-core/
│       ├── build.gradle.kts
│       ├── settings.gradle.kts
│       ├── src/main/java/
│       ├── src/main/resources/plugin.yml
│       ├── src/main/resources/config.yml
│       └── src/test/java/
├── data/
│   ├── quests/
│   ├── events/
│   ├── items/
│   ├── professions/
│   ├── locales/
│   └── schemas/
├── map/
│   ├── worldpainter-source/          # file source/metadata, bukan world produksi
│   ├── schematics/
│   ├── landmarks.yml
│   └── worldgen-notes.md
├── resource-pack/
│   ├── source/
│   ├── java/
│   ├── bedrock/
│   ├── geyser-mappings/
│   └── asset-register.csv
├── deployment/
│   ├── backup/
│   ├── monitoring/
│   └── firewall-notes.md
└── tests/
    ├── acceptance/
    ├── fixtures/
    └── load/
```

Jangan commit `.jar` komersial/third-party jika lisensi atau distribusi melarang; jangan commit world save besar, credential, UUID nyata pemain beta tanpa alasan, atau file database produksi. Simpan sample config yang aman.

---

# 26. Master prompt AI coding agent

Prompt ini untuk Claude/GPT/Gemini saat mengerjakan repository. Ganti lokasi file bila nama repo berubah.

```text
Kamu adalah senior Minecraft server engineer, gameplay systems designer, dan reviewer performa untuk proyek Kala Nusantara.

SUMBER KEBENARAN
1. Baca MASTER_DESIGN.md dan dokumen di docs/ sebelum mengubah kode.
2. Baca docs/COMPATIBILITY.md. Jangan memilih/menaikkan versi Paper, Minecraft, Geyser, Floodgate, Java, atau dependency sendiri.
3. Jika permintaan baru bertentangan dengan dokumen, jangan diam-diam memilih salah satu. Tulis konflik, dampak, opsi, dan rekomendasi. Implementasikan hanya keputusan yang disetujui atau yang secara eksplisit tidak mengubah desain.

KONTEKS PRODUK
Kala Nusantara adalah Co-op Survival RPG bertema kepulauan fiksi terinspirasi Nusantara. Server utama Paper dengan crossplay Bedrock melalui Geyser/Floodgate. Gameplay inti harus berfungsi tanpa mod klien dan tanpa wajib voice chat/resource pack. Risiko harus transparan; rumah utama di zona aman tidak boleh dihancurkan oleh Kala; PvP opt-in; monetisasi tidak boleh pay-to-win.

ATURAN IMPLEMENTASI
- Gunakan API/version yang sudah dipin pada COMPATIBILITY.md.
- Jangan menambah plugin, library, layanan hosted, atau dependency berbayar tanpa proposal dan persetujuan.
- Jangan membangun custom voice transport; integrasikan provider resmi melalui interface yang kecil.
- Jangan membuat asumsi bahwa resource pack Java otomatis bekerja di Bedrock. Semua mapping/pack harus diuji.
- Jangan menjalankan block scan, disk IO, DB query, atau serialisasi berat secara tidak aman pada main thread.
- World/block API hanya diakses melalui pola thread/scheduler yang aman menurut API Paper dan operasi terkait.
- Semua pekerjaan blok besar memakai queue, batch/budget, progress, cancel/fail recovery, dan log.
- Semua transaksi ekonomi/item/vault harus idempotent, dapat diaudit, dan tahan restart.
- Gunakan UUID/internal stable identity, bukan username sebagai key.
- Semua aksi penting divalidasi server-side; jangan percaya input client.
- Semua pesan masuk ke file locale Indonesia dan Inggris.
- Fitur utama harus punya fallback UI untuk Bedrock dan command/text jika form gagal.
- Jangan menambahkan task berjalan setiap tick kecuali terbukti perlu dan telah di-review.
- Jangan menelan exception; tulis log yang menyebut tindakan, ID terkait, dan langkah penanganan tanpa membocorkan secret.
- Tidak boleh ada hadiah/item duplikat ketika listener dipicu berulang, pemain disconnect, atau server restart.

CARA BEKERJA
1. Sebelum coding, ringkas tujuan dan acceptance criteria yang terkait.
2. Baca struktur proyek dan modul yang sudah ada; jangan menimpa implementasi yang benar tanpa alasan.
3. Tulis rencana file yang akan diubah dan dependency yang diperlukan.
4. Kerjakan satu slice kecil; hindari refactor luas yang tidak diperlukan.
5. Tambahkan/ubah unit test dan integration/acceptance test yang relevan.
6. Jalankan build dan test yang tersedia. Jika tidak bisa dijalankan, katakan secara eksplisit; jangan mengklaim lulus.
7. Periksa log startup dan compatibility bila dapat diakses.
8. Review security, Bedrock UX, performance, transaction/restart safety, dan config migration.
9. Laporkan: file yang berubah, perilaku yang ditambahkan, hasil build/test, known limitations, cara manual test Java/Bedrock, dan keputusan yang masih perlu manusia.

DEFINISI SELESAI
Tugas selesai hanya jika acceptance criteria yang disepakati dipenuhi, build/test yang relevan dijalankan atau keterbatasannya dicatat, tidak ada dependency tak disetujui, dan perubahan didokumentasikan. Kode yang hanya dikompilasi belum otomatis berarti gameplay benar.
```

---

# 27. Prompt khusus untuk tiap fase implementasi

## 27.1 Prompt: fondasi Paper + Geyser/Floodgate

```text
Implementasikan hanya setup development server untuk compatibility spike Kala Nusantara. Jangan buat gameplay custom dulu.

Tugas:
- Buat dokumentasi versi Java, Minecraft, Paper, Geyser, dan Floodgate yang dipilih.
- Sediakan contoh startup script serta config example tanpa secret.
- Tulis checklist menjalankan server bersih, Java join, Bedrock Android join, reconnect, chat, dan shutdown.
- Jangan menambahkan plugin lain selain yang disetujui.
- Catat port Java dan Bedrock, kebutuhan firewall, dan setting autentikasi secara aman.
- Sediakan langkah cleanup/reset world test.

Acceptance: langkah reproduksi di Linux jelas; hasil uji Java/Bedrock dicatat; belum ada klaim yang tidak diuji.
```

## 27.2 Prompt: KalaCore skeleton

```text
Buat skeleton plugin KalaCore modular, tanpa mengimplementasikan seluruh gameplay.

Implementasikan:
- plugin lifecycle dan dependency checks;
- config loader dengan validasi;
- locale id/en;
- command admin health/status dengan permission;
- service wiring sederhana;
- repository interface dan SQLite implementation minimal;
- schema migration/version marker;
- unit test untuk config dan storage;
- README development/build.

Jangan implementasikan snapshot, ekonomi pasar, atau event destruktif pada tugas ini. Buat batas package sesuai MASTER_DESIGN.md. Acceptance: clean build, tests, dan server boot test terdokumentasi.
```

## 27.3 Prompt: Kompas dan tutorial

```text
Implementasikan Kompas Pengembara dan tutorial pertama sesuai MASTER_DESIGN.md.

Batas:
- maksimal lima tab utama: Misi, Peta, Kampung, Progres, Panduan;
- gunakan vanilla items sebagai fallback icon;
- implementasikan Java UI dan Bedrock form adapter atau fallback yang terbukti didukung;
- tutorial hanya: kumpulkan bahan, claim lahan aman, bangun tempat tidur, transaksi pertama, discovery pertama;
- tidak ada voice atau pack wajib;
- quest data dipisah dari kode dan punya schema validation.

Acceptance: test Java/Bedrock; tutorial idempotent; pemain reconnect tidak mengulang hadiah; UI dapat ditutup/dibuka; tidak ada menu berulang tiap tick.
```

## 27.4 Prompt: satu event Kala dummy

```text
Implementasikan satu event Kala non-destruktif sebagai vertical slice. Jangan melakukan snapshot massal atau mengubah rumah pemain.

Fitur:
- state machine dan schedule konfigurabel;
- pengumuman awal dan countdown;
- map/hint status area;
- satu objective persiapan;
- fase event dengan efek atmosferik ringan;
- hasil event mengubah satu resource/route/quest state yang jelas;
- ringkasan hasil dan arsip event;
- restart-safe state.

Acceptance: phase transition valid, event tidak start dua kali, late join mendapat status saat ini, tidak ada kerusakan bangunan, event tetap berjalan setelah restart dari state tersimpan.
```

## 27.5 Prompt: snapshot/restore terbatas

```text
Rancang dan uji snapshot/restore hanya untuk satu plot test kecil yang secara eksplisit opt-in. Jangan mengaktifkan untuk semua klaim.

Sebelum coding, berikan threat model untuk item duplication/loss, crash, concurrent build, container contents, invalid chunk, world edit collision, dan recovery job stuck.

Implementasikan hanya setelah desain transaksi dan state machine direview. Gunakan snapshot manifest/checksum, queue, batching, idempotency key, audit log, progress, cancel/failure states, startup recovery, dan test fixtures.

Acceptance: test isi container dan blok, server shutdown pada setiap state, restore diulang dua kali, restore setelah pemain login, dan invalid/corrupt snapshot. Tidak boleh ada duplikasi atau kehilangan item pada test suite. Jika belum bisa dijamin, jangan expose fitur pada production; kembalikan kegagalan dengan status yang tidak destruktif.
```

## 27.6 Prompt: voice integration

```text
Lakukan proof-of-concept integrasi VoiceCraft untuk Paper + Geyser/Floodgate. Jangan buat voice transport custom.

Periksa docs resmi saat tugas dimulai, pin versi plugin/client/server, dokumentasikan port/protocol, authentication, topology direct/proxy, dan privacy. Buat panduan penggunaan suara dan halaman Kompas yang hanya menampilkan instruksi/status yang memang tersedia melalui API.

Jangan menyatakan mic aktif atau output telah berubah jika aplikasi client tidak memberi status tersebut. Voice harus opsional; semua quest inti memiliki text fallback. Sertakan test Java desktop dan Bedrock Android, serta iOS bila masuk target.
```

## 27.7 Prompt: resource pack Java + Bedrock

```text
Buat pipeline aset pertama Kala Nusantara dengan satu aset Kompas dan maksimal dua item lain.

Deliverables:
- asset source dan register lisensi;
- Java resource pack;
- Bedrock pack/manifest;
- Geyser mapping yang sesuai versi target;
- panduan build/versioning;
- screenshot/checklist hasil tes Java dan Bedrock nyata;
- fallback vanilla jika pack gagal.

Jangan menganggap converter otomatis berhasil. Jangan membuat custom block/entity kompleks. Jangan menambahkan dependency pack generator berbayar tanpa keputusan eksplisit.
```

---

# 28. Keputusan yang harus dikunci sebelum produksi

Ini bukan pertanyaan yang harus diputuskan semuanya hari ini. Namun setiap keputusan perlu status `proposed`, `accepted`, atau `deferred` di `docs/ADR/`.

1. Nama final server dan apakah nama Gunung Murka dipertahankan.
2. Versi target Minecraft/Paper patch setelah semua plugin lulus tes.
3. Map utama dibuat dengan WorldPainter atau dibangun manual; ukuran dan koordinat landmark.
4. Generator resource world: Iris, TerraformGenerator, atau vanilla terrain + BetterStructures.
5. Apakah voice app terpisah dapat diterima untuk pemain MCPE; jika tidak, ekspektasi fitur perlu disesuaikan.
6. Apakah custom resource pack diperlukan untuk MVP atau baru fase berikutnya.
7. Apakah quest system dibangun sederhana di KalaGuide atau memakai BetonQuest setelah tes Bedrock.
8. Kapan GriefPrevention diganti dengan KalaClaim custom.
9. Apakah ekonomi awal berbasis plugin sementara atau langsung KalaEconomy; migrasi saldo/item harus jelas.
10. Ukuran host produksi, lokasi region hosting, dan target concurrent hasil load test.
11. Jadwal musim dan event global berdasarkan zona waktu pemain uji.
12. Kebijakan moderasi voice/chat, privasi, report/mute, dan aturan untuk pemain usia beragam.
13. Apakah monetisasi kosmetik akan diterapkan; tidak ada monetisasi saat MVP.

---

# 29. Daftar repo dan dokumentasi primer

Tanggal pengecekan sumber: 24 Oktober 2026. Link di bawah ditujukan untuk memulai pemeriksaan. Versi rilis, lisensi, kompatibilitas dan harga tetap perlu dicek kembali ketika mengunduh/men-deploy.

## Server, crossplay, izin

- Paper downloads: https://papermc.io/downloads/paper
- Paper getting started / Java requirement: https://docs.papermc.io/paper/getting-started/
- Paper server properties: https://docs.papermc.io/paper/reference/server-properties/
- Paper profiling/spark: https://docs.papermc.io/paper/profiling/
- Geyser: https://github.com/GeyserMC/Geyser
- Geyser custom items: https://geysermc.org/wiki/geyser/custom-items/
- Geyser limitations: https://geysermc.org/wiki/geyser/current-limitations/
- Floodgate: https://github.com/GeyserMC/Floodgate
- LuckPerms: https://github.com/LuckPerms/LuckPerms
- PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI

## World creation, generation, structures

- WorldPainter: https://www.worldpainter.net/
- WorldPainter FAQ/export: https://www.worldpainter.net/trac/wiki/FAQ
- Iris: https://github.com/VolmitSoftware/Iris
- Iris docs: https://github.com/VolmitSoftware/docs
- TerraformGenerator: https://github.com/Hex27/TerraformGenerator
- BetterStructures: https://github.com/MagmaGuy/BetterStructures
- Chunky: https://github.com/pop4959/Chunky
- WorldEdit: https://github.com/EngineHub/WorldEdit
- FastAsyncWorldEdit: https://github.com/IntellectualSites/FastAsyncWorldEdit
- Multiverse-Core: https://github.com/Multiverse/Multiverse-Core

## Protection, NPC, quests, data

- GriefPrevention: https://github.com/GriefPrevention/GriefPrevention
- GriefPrevention documentation: https://docs.griefprevention.com/configuration/
- CoreProtect: https://github.com/PlayPro/CoreProtect
- Citizens: https://github.com/CitizensDev/Citizens2
- BetonQuest: https://github.com/BetonQuest/BetonQuest
- EssentialsX: https://github.com/EssentialsX/Essentials
- Vault: https://github.com/MilkBowl/Vault
- Jobs Reborn: https://github.com/Zrips/Jobs

## Resource packs and voice

- Blockbench: https://www.blockbench.net/
- Oraxen docs: https://docs.oraxen.com/
- ItemsAdder Geyser FAQ: https://wiki.itemsadder.com/faq/geyser/
- Geyser Rainbow/conversion tools: https://github.com/GeyserMC/Rainbow
- VoiceCraft: https://github.com/AvionBlock/VoiceCraft
- VoiceCraft downloads: https://docs.voicecraft.chat/download
- VoiceCraft client install: https://docs.voicecraft.chat/client/installation
- VoiceCraft.Java bridge: https://github.com/AvionBlock/VoiceCraft.Java
- SimpleVoice-Geyser (alternative, developmental): https://modrinth.com/project/GJLuArlK

## Monitoring, analytics, deployment

- spark: https://github.com/lucko/spark
- Plan player analytics: https://github.com/plan-player-analytics/Plan
- DiscordSRV: https://github.com/discordsrv/discordsrv
- Docker Minecraft Server: https://github.com/itzg/docker-minecraft-server
- Pelican Panel: https://github.com/pelican-dev/panel
- Pterodactyl Panel: https://github.com/pterodactyl/panel

### Catatan validasi sumber

- Paper docs yang dibuka pada tanggal riset menyebut Java 25 untuk Minecraft 26.1+; pin versi yang benar-benar dipilih.
- Geyser docs menegaskan Java pack tidak otomatis dikonversi ke Bedrock dan custom item membutuhkan mapping/Bedrock pack yang sesuai.
- Iris repo/docs mempublikasikan dukungan lintas versi/platform yang berkembang cepat; jangan menganggap rilis paling baru cocok dengan server yang terlanjur dipin.
- TerraformGenerator README menyebut proyek pada status alpha dan memberi peringatan untuk produksi yang membutuhkan dukungan ketat.
- BetterStructures menambah struktur pada world dan menyatakan membutuhkan WorldEdit; konten pack pihak ketiga punya lisensi terpisah.
- Oraxen adalah produk berbayar; uji integrasi Bedrock pada versi yang dipakai sebelum membeli atau menjadikannya fondasi.
- ItemsAdder FAQ Geyser yang ditinjau menyatakan Geyser tidak didukung resmi saat ini.
- VoiceCraft 1.7 docs menyebut native desktop/mobile clients, browser client tidak lagi menjadi bagian rilis inti tersebut, dan perangkat konsol utama tidak tercantum sebagai target native.
- SimpleVoice-Geyser menyatakan status pengembangan dan bahwa client-to-server audio tidak dienkripsi default; jangan dipakai sebagai jalur publik tanpa hardening.

---

# 30. Glosarium

- **Core loop:** rangkaian aktivitas yang diulangi pemain dan memberi tujuan/progres.
- **Vertical slice:** potongan kecil gameplay yang lengkap dari awal sampai hasil, digunakan untuk membuktikan konsep.
- **MVP:** versi minimum yang cukup dimainkan untuk memvalidasi konsep, bukan versi yang berisi semua fitur.
- **Opt-in risk:** pemain secara sadar memilih plot/event berisiko setelah menerima penjelasan.
- **Resource world:** dunia pengumpulan material yang bisa di-reset terjadwal.
- **Worldgen:** proses membentuk terrain/biome/struktur saat chunk dihasilkan.
- **Pregeneration:** membuat chunk lebih dulu agar pemain tidak memicu banyak worldgen saat event/live.
- **Snapshot:** salinan data struktur/plot pada waktu tertentu yang dapat menjadi basis restore.
- **Idempotency:** menjalankan operasi yang sama berulang kali tidak menggandakan hadiah/item atau merusak state.
- **MSPT:** waktu yang dibutuhkan server memproses satu tick dalam milidetik; spike dapat menyebabkan lag meskipun angka TPS terlihat normal.
- **Resource pack:** aset visual/audio yang diterima client; Java dan Bedrock punya format serta mapping yang berbeda.
- **Geyser mapping:** aturan pemetaan konten custom Java ke representasi Bedrock.
- **Acceptance criteria:** kondisi yang harus benar-benar dipenuhi agar suatu fase dinyatakan selesai.
- **ADR:** Architecture Decision Record; catatan keputusan teknis beserta alasan dan konsekuensinya.

---

## Ringkasan eksekutif akhir

Kala Nusantara akan paling kuat bila identitasnya bukan “server dengan plugin paling banyak”, melainkan **dunia yang bisa ditinggali, dipahami, dan berubah dari musim ke musim**. Dunia utama dikurasi agar memiliki cerita dan landmark tetap; wilayah sumber daya memberi eksplorasi berulang; kampung menjadi proyek sosial; profesi menghubungkan aktivitas; pasar mempertemukan pemain; Jurnal memberi target jangka panjang; Kala memberi perubahan dunia yang diumumkan dan dapat diikuti tanpa menghancurkan progres rumah utama.

Pilihan teknis awal adalah Paper + Geyser/Floodgate + LuckPerms, WorldPainter untuk map utama, Chunky untuk pregeneration, GriefPrevention sementara, CoreProtect, spark, dan seperangkat plugin dasar yang sudah diverifikasi versi. Worldgen resource memilih satu jalur saja. Resource pack dimulai ringan dan dipisahkan Java/Bedrock. VoiceCraft menjadi kandidat untuk POC dengan aplikasi client eksternal dan fallback teks; suara tidak menjadi syarat permainan.

Custom development dimulai dari KalaCore skeleton, Kompas/tutorial, lalu satu event Kala non-destruktif. Snapshot/restore, ekonomi penuh, dan resource regeneration lanjutan dibuat setelah core loop terbukti menarik serta transaksi barang dapat diuji aman. AI coding agent hanya mengimplementasikan satu fase dengan acceptance criteria, tes, dokumentasi, dan kompatibilitas yang telah dipin — bukan membuat semua ide sekaligus.

**Dokumen ini adalah blueprint kerja yang harus diuji.** Semua klaim kompatibilitas dan performa harus dikonfirmasi di staging sebelum rilis publik.
