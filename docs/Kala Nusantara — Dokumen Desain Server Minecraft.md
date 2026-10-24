# Kala Nusantara — Dokumen Desain Server Minecraft

Survival-RPG ringan bertema Nusantara, Java + Bedrock (MCPE) dalam satu dunia. Dokumen ini mencakup konsep, sistem bencana (dari sudut pandang pemain), ekonomi dan sumber daya, rancangan plugin custom, daftar tekstur, dan roadmap.

Catatan: versi software, harga plugin, dan fitur Geyser berubah cepat. Semua yang bertanda (cek) harus diverifikasi ulang saat mulai membangun.

## 1. Ringkasan dan prinsip desain

**Satu kalimat:** dunia survival bersama yang dibentuk ulang oleh satu bencana besar per musim, di mana kehancuran selalu menghasilkan kesuburan, bukan kehilangan permanen.

**Kenapa konsep ini:** dari riset, server ekonomi/crystal PvP dan klon DonutSMP sudah jenuh di Indonesia, MMORPG penuh (seperti Wynncraft) butuh konten sangat besar, sedangkan tema Nusantara hampir kosong. Tren game 2026 juga condong ke co-op dan crossplay.

**Lima prinsip yang jadi pegangan setiap keputusan:**

- **Tidak ada kehilangan yang tidak bisa dipulihkan.** Pemain boleh kehilangan waktu dan kenyamanan, tidak boleh kehilangan barang berharga tanpa jalan kembali.
- **Tidak ada kejutan.** Bencana selalu diumumkan lama sebelumnya, jelas, dan bisa dipersiapkan.
- **Risiko itu pilihan.** Pemain santai dan pemain berani punya jalur masing-masing.
- **Co-op di atas PvP.** Musuh utama adalah alam, bukan sesama pemain.
- **Satu aturan, satu menu, satu tujuan per tahap.** Fitur dibuka bertahap supaya pemain baru tidak kewalahan.

## 2. Gambaran pengalaman pemain

**Pemain baru (menit 0–30):**

- Muncul di pelabuhan **Labuhan Awal**, bertemu NPC pemandu **Mbah Kala** (penjaga cerita).
- Menerima **Kompas Pengembara** (item menu utama, satu-satunya menu yang perlu diingat).
- Quest berurutan: ambil kayu, buat alat dasar, klaim tanah kampung pertama, bangun tempat tidur, jual satu barang di pasar, lihat peta bahaya.
- Di akhir ada ringkasan: apa itu Kala, kapan letusan berikutnya, dan cara melindungi barang.

**Pemain rutin (minggu 1–6):** memilih jalur profesi, membangun kampung, bergotong royong mengumpulkan bahan persiapan, ikut event mingguan.

**Pemain malas atau jarang online:** tetap punya jalur aman. Lihat bagian 4.

## 3. Dunia dan lore

Semua nama wilayah dan tokoh **fiksi** terinspirasi budaya Nusantara (tidak memakai tokoh nyata atau kerajaan nyata sebagai karakter bermain, supaya aman dari sensitivitas dan isu hak cipta).

- **Kepulauan Kala:** gugusan pulau di sekitar gunung api besar bernama **Gunung Murka** (nama sementara).
- **Kala** adalah siklus waktu: setiap beberapa minggu gunung bangkit, menghancurkan sebagian, lalu menyuburkan tanah.
- Empat wilayah awal: **Pesisir** (pelaut dan dagang), **Rimba** (hutan dan hasil bumi), **Pegunungan** (tambang), **Pulau Seberang** (muncul dan berubah tiap musim).
- Gaya visual: ukiran, batik, kayu, bambu, batu; palet hangat tropis. Bukan fantasi Eropa.

## 4. Sistem Kala: bencana dari sudut pandang pemain

Ini bagian paling rawan membuat orang berhenti main, jadi dirancang paling teliti.

### 4.1 Struktur satu musim (8 minggu)

- **Minggu 1–4 Masa Tenang:** membangun, ekonomi normal, event kecil.
- **Minggu 5 Tanda Awal:** gempa kecil, asap tipis, cuaca berubah. Peta bahaya aktif. Pengumuman pertama.
- **Minggu 6 Persiapan:** misi gotong royong (tanggul, tanah lindung, bahan evakuasi). Bonus untuk yang bersiap.
- **Minggu 7 Letusan:** event terjadwal di jam ramai (diumumkan minimal 7 hari sebelumnya, diulang 72, 24, dan 1 jam sebelum).
- **Minggu 8 Pemulihan dan Kesuburan:** pembersihan, ladang subur, pulau baru, lore musim berikutnya.

### 4.2 Tiga zona risiko

Peta menampilkan zona dengan warna jelas, dan sistem selalu memberi tahu pemain zona tempat tanah klaim mereka berada.

- **Zona Hijau (Aman):** wilayah inti kampung. Bencana hanya kosmetik (abu, langit gelap). Tidak ada kerusakan bangunan.
- **Zona Kuning (Terdampak):** kerusakan sedang dan **bisa dipulihkan penuh** lewat sistem pemulihan (bagian 4.4).
- **Zona Merah (Bahaya):** dekat gunung. Bangunan bisa hancur total. Sebagai gantinya, sumber daya langka hanya ada di sini. Pemain memilih sadar untuk membangun di sini (konfirmasi saat klaim).

### 4.3 Yang tidak pernah hilang

- **Inventori dan ender chest:** tidak terpengaruh bencana.
- **Isi peti di klaim terdaftar:** sebelum letusan, sistem otomatis membuat **snapshot** (blok dan isi peti). Isi peti yang terkena dipindahkan ke **Brankas Pengungsian** milik pemain (penyimpanan pribadi), jadi barang tidak pernah lenyap.
- **Item bernilai tinggi** (netherite, item profesi, item event) ditandai sebagai **Pusaka**: tidak bisa hancur oleh bencana, hanya bisa dicuri/hilang lewat PvP di zona PvP.

### 4.4 Rumah hancur: bagaimana pulihnya

Alurnya sengaja memberi dua jalur supaya yang rajin dan yang malas sama-sama nyaman.

- **Puing:** blok yang hancur berubah jadi item **Puing** (batu, kayu, tanah sesuai asal). Puing bisa dikumpulkan dan dipakai untuk membangun ulang. Jadi kehancuran menghasilkan bahan, bukan nol.
- **Jalur Pulih Cepat (untuk yang malas):** perintah/menu **Pulihkan Kampung** mengembalikan struktur dari snapshot. Biaya = persentase kecil bahan (misal 20–40%, nilai awal, diseimbangkan lewat uji) dan bisa dibayar dengan Puing yang dikumpulkan atau Kepeng.
- **Jalur Gotong Royong (untuk yang rajin):** pemain yang ikut membersihkan puing, memperbaiki fasilitas umum, dan membantu tetangga mendapat diskon pemulihan dan item kosmetik.
- **Asuransi Musim:** setiap pemain mendapat 1 pemulihan penuh gratis per musim (naik jadi 2 setelah level profesi tertentu). Bagi yang jarang online, ini jaring pengaman.
- **Rumah Darurat:** jika klaim hancur total dan pemain belum sempat memulihkan, sistem menyediakan satu **pondok sementara** di zona hijau supaya tidak bingung harus mulai dari mana.
- **Pemulihan tidak kedaluwarsa dalam satu musim**, jadi pemain yang absen seminggu tetap bisa kembali memulihkan.

### 4.5 Mode Santai dan Mode Berani

- **Santai (default):** tinggal di zona hijau/kuning, pemulihan murah, risiko minimal, hadiah standar.
- **Berani:** membangun di zona merah, mengikuti misi garis depan, hadiah langka dan kosmetik eksklusif, tetapi pemulihan lebih mahal.
- Pemain bisa pindah mode antar musim, tidak di tengah musim.

### 4.6 Komunikasi supaya tidak memicu rasa dirugikan

- Setiap peringatan menyebut dengan jelas: zona kamu, apa yang akan terjadi, apa yang aman, apa yang bisa dilakukan.
- Setelah letusan: **Laporan Kerusakan** pribadi di Kompas (berapa blok rusak, barang diselamatkan, biaya pulih, tombol pulihkan).
- Notifikasi Discord (via bot) untuk pemain yang tidak sedang online: peringatan 72 jam dan 24 jam.
- Tidak ada kerugian yang hanya terlihat setelah terjadi.

### 4.7 Cara pulang bagi pemain yang sudah lama absen

- **Bonus Kembali:** setelah absen lebih dari 7 hari, pemain menerima paket kecil (bahan, Kepeng, dan satu pemulihan tambahan), dan jalur singkat "yang terlewat" di Kompas.
- Data absen tidak dipakai untuk menghukum klaim: klaim tidak dihapus karena absen selama satu musim.

## 5. Sumber daya: supaya tidak menipis

Masalah klasik: dunia survival kehabisan sumber daya. Solusinya berlapis, dan bencana dijadikan mesin regenerasi, bukan sekadar perusak.

### 5.1 Dunia berlapis

- **Tanah Kampung (persisten):** tempat tinggal. Sumber daya terbatas (hasil bumi dasar), tidak direset.
- **Rimba dan Tambang Kala (dunia sumber daya):** area ambil sumber daya. **Direset berkala** (mingguan untuk area biasa, per musim untuk area langka).
- **Pulau Seberang (baru tiap musim):** muncul dari aktivitas gunung. Berisi bijih dan bahan langka yang hanya ada satu musim.

### 5.2 Mekanisme regenerasi

- **Simpul Sumber Daya (resource node):** bijih dan pohon tertentu hidup kembali setelah jeda (menit hingga jam, dikonfigurasi per jenis).
- **Kesuburan Pasca-Letusan:** setelah letusan, tanah di zona kuning/hijau mendapat **buff panen** sementara (ladang tumbuh lebih cepat/hasil lebih banyak). Pesan inti: letusan menguntungkan juga.
- **Lava Membeku:** aliran lava mendingin menjadi ladang bijih baru dan blok langka (Batu Apung, Obsidian Murni, Abu Subur).
- **Penebangan wajib tanam:** pohon yang ditebang di area kampung memicu bibit otomatis (opsional, toggle server).

### 5.3 Anti-penipisan dan anti-grinding

- **Kelelahan Tanah:** setiap wilayah punya meter kesehatan sumber daya yang dilihat semua orang. Semakin sering ditambang dalam waktu singkat, hasil per blok menurun (diminishing returns). Meter pulih seiring waktu dan oleh aktivitas perawatan (tanam, perbaikan).
- **Kuota harian lembut:** bukan batas keras, hanya penurunan hasil setelah ambang tertentu, supaya tidak terasa dihukum.
- **Sumber daya langka terikat musim:** barang tertentu hanya muncul di musim tertentu, jadi harga pasar tidak runtuh.
- **Resep sink:** perbaikan alat, upgrade, dan misi gotong royong menyerap bahan, jadi stok tidak menumpuk tanpa batas.

### 5.4 Ekonomi

- Mata uang **Kepeng**. Tidak bisa dibeli dengan uang asli.
- **Pasar pemain** dengan pajak kecil (nilai awal 3–5%, disesuaikan) sebagai sink.
- **Toko NPC** hanya untuk bahan dasar, dengan harga **dinamis**: semakin banyak dijual, semakin turun harganya, lalu pulih pelan.
- **Batas penghasilan dari NPC per hari** (lunak) untuk mencegah inflasi.
- Monetisasi: hanya kosmetik (skin alat, efek, rumah, hewan peliharaan). **Tidak ada pay-to-win.** Alasan: kritik terhadap DonutSMP justru datang dari sana, dan ini pembeda kepercayaan komunitas.

## 6. Profesi (RPG ringan)

Pemain memilih satu jalur utama dan satu jalur sampingan terbuka belakangan. Setiap jalur punya peran penting saat bencana, jadi tidak ada yang "hanya pemain pasif".

- **Pandai (pengrajin besi):** alat dan senjata lebih baik, bisa **mempercepat pemulihan bangunan**.
- **Tabib:** ramuan, penyembuhan, mengurangi efek abu dan gas.
- **Pelaut:** kapal, perdagangan antar pulau, mengangkut evakuasi.
- **Pemburu/Penjelajah:** peta, deteksi bahaya, akses ke zona langka.
- Tiap jalur 3 tingkat awal (level 1–15). Konten tambahan datang per musim.
- Pengalaman didapat dari aktivitas wajar (menambang, meramu), bukan dari grind monster.

## 7. Kematian dan PvP

- Di zona hijau/kuning: mati menjatuhkan **Nisan** (peti makam) yang hanya bisa dibuka pemilik selama beberapa menit, lalu terbuka bagi siapa saja. Tidak ada kehilangan XP berlebihan.
- **PvP opsional** di Arena dan zona event. Tidak ada PvP di wilayah kampung secara default.
- Mode Berani boleh mengaktifkan PvP bersyarat di zona merah, dengan aturan jelas di panduan.

## 8. Onboarding dan panduan

- **Kompas Pengembara:** satu menu dengan lima tombol: Misi, Peta, Kampung, Profesi, Panduan.
- **Buku Panduan** bertahap: bab baru terbuka sesuai level. Bahasa Indonesia, opsi bahasa Inggris.
- **Daftar misi** dengan tanda centang. Sistem petunjuk (hint) memberi arahan jika pemain macet lebih dari 3 menit.
- **Panduan Bencana:** simulasi kecil (Latihan Kala) sebelum musim pertama: pemain mencoba evakuasi dan pemulihan dalam suasana aman.
- Semua UI dibuat dua versi (lihat bagian 9.3).

## 9. Arsitektur teknis

### 9.1 Stack inti

- **Paper** (versi Minecraft stabil terbaru saat mulai, cek) sebagai server.
- **Geyser + Floodgate** supaya Bedrock bisa masuk ke server Java. Tidak membuat jembatan sendiri; itu proyek besar dan sudah diurus komunitas.
- **LuckPerms** (izin), **Vault** atau API ekonomi sendiri, **PlaceholderAPI**.
- **FastAsyncWorldEdit (FAWE) API** untuk snapshot dan pemulihan struktur (cek kompatibilitas versi).
- **Citizens** atau NPC sendiri untuk Mbah Kala dan pedagang.
- **ItemsAdder atau Oraxen** untuk item custom dan pack (cek lisensi dan harga). ItemsAdder punya generator pack Bedrock, Oraxen lewat mapping Geyser. Pilih satu, jangan campur.
- Proxy **Velocity** jika nanti butuh banyak server (lobby, kampung, dunia sumber daya).
- Database: **SQLite** untuk prototipe, **MariaDB/MySQL** untuk produksi.

### 9.2 Plugin custom yang perlu dibuat

Mulai sebagai **satu plugin dengan modul terpisah** (lebih mudah di-debug), pecah jadi beberapa plugin jika perlu.

- **KalaCore:** mesin musim dan bencana. Penjadwal, fase letusan (gempa, abu, aliran lava), zona risiko, pengumuman, boss bar, API event untuk modul lain.
- **KalaClaim:** klaim tanah kampung, tingkat zona, snapshot otomatis, Brankas Pengungsian, Puing, pemulihan (biaya, asuransi, antrean pemulihan agar tidak membebani server).
- **KalaResource:** simpul sumber daya, regenerasi, Kelelahan Tanah, reset dunia sumber daya, pembuatan Pulau Seberang.
- **KalaEconomy:** Kepeng, pasar pemain, harga dinamis NPC, pajak, laporan inflasi untuk admin.
- **KalaProfession:** jalur profesi, XP, perk, interaksi dengan bencana.
- **KalaGuide:** Kompas, misi, buku panduan, hint, notifikasi Discord, lapisan UI lintas edisi.

### 9.3 UI lintas Java dan Bedrock

Bedrock tidak menampilkan semua chest GUI Java dengan baik, dan beberapa hal tidak bisa dijembatani (misalnya custom head di inventori, tautan klik di chat, efek glowing; cek daftar batasan terbaru Geyser). Solusinya satu **lapisan abstraksi UI**:

- Kode menu ditulis sekali (judul, tombol, aksi).
- Untuk pemain Java: dirender sebagai chest GUI dengan ikon dari resource pack.
- Untuk pemain Bedrock (dideteksi via Floodgate): dirender sebagai **form Floodgate** (sederhana, ramah sentuh).
- Jangan bergantung pada klik kanan/kiri rumit atau shortcut keyboard: Bedrock mobile dan konsol harus tetap nyaman.

### 9.4 Cara menjaga performa

- Pemulihan dan snapshot dijalankan **asinkron dan bertahap** (per chunk, dengan batas per tick).
- Letusan memakai **efek ringan** (partikel dan perubahan blok terjadwal), bukan simulasi fluida besar.
- Batasi jumlah entitas di event. Uji dengan banyak pemain bayangan (bot) sebelum rilis.
- Dev lokal di laptop RAM sekitar 11 GB tanpa GPU diskrit: Paper + Geyser cukup ringan untuk uji fungsi, tetapi jangan menjalankan client Minecraft, server, IDE, dan build sekaligus; uji Bedrock lewat HP di jaringan yang sama.

### 9.5 Keamanan dan keadilan

- Anti-cheat (cek opsi yang kompatibel dengan Geyser, karena gerakan Bedrock sering memicu false positive).
- Log transaksi ekonomi dan pemulihan agar sengketa bisa ditelusuri.
- Backup otomatis harian, uji restore berkala.
- Aturan dan moderasi: tim kecil + Discord ticket.

## 10. Tekstur dan aset

### 10.1 Prinsip

- Satu **resource pack Java** sebagai sumber, dengan **pack Bedrock yang dihasilkan** (ItemsAdder/Geyser mapping), diuji di perangkat Bedrock sungguhan.
- Utamakan **item custom lewat CustomModelData** dan tekstur 16x atau 32x. Model 3D kompleks dan blok custom penuh ditunda ke fase lanjut karena dukungan Bedrock-nya terbatas.
- Shader tidak dipakai (tidak jalan lintas edisi).
- Gaya: pixel art, palet hangat (terakota, hijau daun, biru laut), motif batik/ukiran pada GUI.
- Alat yang disarankan: **Blockbench** untuk model dan tekstur (gratis).

### 10.2 Daftar aset fase 1 (perkiraan sekitar 60 item)

- **Item sistem:** Kompas Pengembara, Buku Panduan, Kepeng (koin 3 ukuran), Tiket Pemulihan, Surat Asuransi.
- **Bahan bencana:** Puing (batu/kayu/tanah), Abu Subur, Batu Apung, Obsidian Murni, Bijih Kala.
- **Alat dan perlengkapan:** set alat tier Kala (kayu ukir, perunggu, besi Kala) untuk kapak, beliung, sekop, pedang; empat set pakaian profesi sederhana.
- **Profesi:** ikon jalur (empat), item khas tiap jalur (palu pandai, ramuan tabib, peta pelaut, teropong pemburu).
- **GUI:** latar menu, ikon tombol, bingkai, ikon zona (hijau, kuning, merah), ikon status bencana.
- **HUD dan pesan:** ikon peringatan, bar bencana (via boss bar + judul), font kustom opsional (cek dukungan Bedrock).
- **Kosmetik awal:** beberapa skin alat dan satu set hiasan rumah (untuk monetisasi etis).

### 10.3 Batasan yang harus dihormati

- Jangan memakai custom head sebagai ikon penting (Bedrock tidak menampilkannya dengan baik).
- Hindari tekstur beresolusi tinggi (berat di HP kelas menengah).
- Setiap aset baru wajib lolos uji di Java dan Bedrock sebelum masuk rilis.

## 11. Event dan retensi

- **Mingguan:** pasar rakyat, lomba kapal, misi gotong royong.
- **Per musim:** Letusan, lalu festival pemulihan dengan hadiah kosmetik.
- **Arsip Musim:** setiap musim berakhir dengan "buku sejarah" yang menampilkan kontribusi pemain (nama kampung, jasa pemulihan), memberi alasan emosional untuk kembali.
- Metrik yang dipantau: retensi hari 1/7/30, persentase pemain yang kembali setelah letusan, rasio pemulihan versus meninggalkan klaim, inflasi Kepeng, kelelahan tanah rata-rata.

## 12. Risiko dan jawabannya

- **Pemain berhenti setelah letusan:** tiga pengaman (barang tidak hilang, pemulihan murah, asuransi) dan komunikasi jelas. Ukur persentase yang tidak login dalam 7 hari setelah letusan; jika tinggi, turunkan biaya pulih.
- **Terlalu rumit untuk pemula:** fitur dibuka bertahap, Kompas tunggal, satu menu per tahap.
- **Server lag saat letusan:** efek terjadwal, pemulihan asinkron, uji beban.
- **Cacat tekstur di Bedrock:** uji perangkat nyata sejak fase awal.
- **Beban pengembang tunggal:** prioritaskan KalaCore dan KalaClaim dulu, sisanya bisa memakai plugin siap pakai sementara.

## 13. Roadmap

Perkiraan kasar untuk satu pengembang, sebaiknya dikoreksi setelah prototipe pertama.

- **Fase 0 (prototipe, 2–4 minggu):** server Paper + Geyser/Floodgate berjalan, satu letusan sederhana, snapshot dan pemulihan satu klaim, satu menu Kompas lintas Java/Bedrock.
- **Fase 1 (alpha tertutup, 1–2 bulan):** KalaCore dan KalaClaim lengkap, zona risiko, Brankas Pengungsian, tutorial 30 menit, aset fase 1.
- **Fase 2 (beta Musim 1):** KalaResource dan KalaEconomy, profesi awal, satu musim penuh dengan 20–50 pemain, ukur retensi dan keseimbangan.
- **Fase 3 (rilis publik):** moderasi, monetisasi kosmetik, Discord bot, jadwal musim reguler.

## 14. Keputusan yang perlu diambil berikutnya

- Nama final server dan gunung (nama sementara di dokumen ini).
- Pilihan ItemsAdder atau Oraxen (tergantung anggaran dan kebutuhan Bedrock).
- Tempat hosting dan lokasi server (latensi pemain Indonesia).
- Seberapa berani biaya pemulihan di Mode Berani.
- Apakah musim pertama berupa beta berbayar atau gratis.
