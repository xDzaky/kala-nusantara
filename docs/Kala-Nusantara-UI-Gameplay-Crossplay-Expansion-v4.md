# Kala Nusantara — UI, Gameplay & Crossplay Expansion Plan v1.0

**Basis:** `Kala-Nusantara-Worldbuilding-Implementation-Report-v4.md`  
**Tanggal riset:** 24 Oktober 2026  
**Target yang dilaporkan:** Paper 26.3-218, Geyser/Floodgate, Java + Bedrock/MCPE  
**Tujuan:** memperkaya pengalaman pemain tanpa menumpuk plugin, merusak identitas Nusantara, atau membuat performa dan kompatibilitas Bedrock sulit dijaga.

> **Catatan validasi:** rekomendasi ini didasarkan pada isi report V4 dan dokumentasi publik yang diperiksa. Aku tidak mengakses filesystem server, source code KalaCore, atau menjalankan test di instance milikmu. Jadi status plugin yang benar-benar terpasang, tampilan GUI saat ini, dan hasil beban produksi tetap harus diverifikasi dari server/staging, bukan dianggap terkonfirmasi dari dokumen saja.

---

## 1. Keputusan utama

Kala Nusantara tidak perlu mengejar sebanyak mungkin plugin atau mod. Identitas server sudah punya dasar yang cukup khas: Labuhan Awal, Gunung Murka, sistem profesi, proyek gotong royong, ekspedisi, musim Kala delapan minggu, ekonomi, jurnal, dan dunia tambang terpisah.

**Arah yang disarankan:**

1. **Pertahankan KalaCore sebagai gameplay core** untuk quest, profesi, proyek desa, musim, NPC, ekonomi, kompas, dan Atlas Fauna.
2. **Pakai satu pemilik untuk setiap lapisan UI.** Jika TAB dipakai untuk sidebar, jangan membuat sidebar kedua dari KalaCore. TAB dapat menangani tablist, nametag, scoreboard, dan bossbar; pilih fitur yang dipakai, matikan sisanya. TAB mencantumkan dukungan Paper 26.3 pada dokumentasinya saat riset dilakukan. [TAB compatibility](https://github.com/NEZNAMY/TAB/wiki/Compatibility)
3. **Gunakan chest GUI vanilla sebagai dasar lintas platform.** Nama item, lore, slot, tombol, aksi, dan paging bisa dikustomisasi tanpa mod client. Untuk tampilan pixel-art penuh, baru tambahkan resource pack dan glyph/font; itu proyek terpisah dari sekadar memasang plugin GUI.
4. **Jangan menambahkan mod Java yang mewajibkan pemain memasangnya.** Untuk fauna, mulai dari entitas vanilla dengan perilaku/varian tematik; custom model lintas Bedrock adalah tahap eksperimen tersendiri.
5. **Prioritaskan keselamatan operasional sebelum fitur kosmetik:** backup yang bisa dipulihkan, audit grief, tes crossplay, pembuktian resource reset, dan tes beban dengan pemain.

### Susunan lapisan yang dianjurkan

| Lapisan | Pemilik yang disarankan | Batas tanggung jawab |
|---|---|---|
| Gameplay dan data | `KalaCore` | Quest, profesi, proyek, musim, ekonomi, NPC, fauna, Atlas Fauna |
| Persistent UI | `TAB` *atau* KalaCore, jangan keduanya untuk fitur yang sama | Tablist, nametag, sidebar, bossbar; satu pemilik per jenis tampilan |
| Menu statis/config-driven | Opsional: `zMenu` | Bantuan, menu administrasi ringan, atau prototipe menu; jangan menduplikasi `/kompas` tanpa alasan |
| Visual GUI | Resource pack Java + pack Bedrock yang setara | Latar, glyph, font, texture item dan UI; bukan logic ekonomi/quest |
| Observabilitas dan pemulihan | `spark`, `CoreProtect`, backup | Profiling, audit perubahan blok, restore dan drill pemulihan |

---

## 2. Review kritis report V4

V4 punya arah arsitektur yang baik, tetapi beberapa klaim dan detail perlu dirapikan sebelum laporan dipakai sebagai bukti bahwa server benar-benar siap dibuka publik.

### 2.1 Klaim production-ready lebih luas daripada bukti yang ditampilkan

Uji yang dicantumkan menunjukkan pre-generation **729 chunk di staging selama 74 detik**, dengan TPS sekitar 20 pada pengujian itu. Itu bukti smoke test yang berguna; belum membuktikan performa saat banyak pemain, banyak entity, GUI dibuka berulang, NPC diklik bersamaan, database menerima transaksi, atau world resource di-reset saat sesi ramai.

Saran penulisan status:

- **Verified:** hanya yang punya log/config/test artifact yang dapat diperiksa.
- **Passed in staging:** lolos pengujian khusus yang disebutkan.
- **Production candidate:** belum melewati closed alpha dan tes pemulihan penuh.
- Pakai **Production Ready** setelah kriteria penerimaan di bagian 11 benar-benar dijalankan dan bukti disimpan.

### 2.2 Ketidaksesuaian angka penyimpanan

Tabel storage menyebut total 928 GB, terpakai 770 GB, tersedia 154 GB, lalu menyebut 84%. Angka 154 GB dari total 928 GB setara sekitar **16,6% tersedia**, bukan 84%. Selain itu, 770 + 154 = 924 GB, jadi ada selisih sekitar 4 GB yang perlu dijelaskan (misalnya pembulatan, partisi, atau ruang yang tidak tercakup).

Ini penting karena jadwal backup 6 jam sekali dapat cepat menghabiskan ruang bila seluruh data dunia di-zip setiap kali. Tulis ukuran backup aktual, retensi, lokasi salinan kedua, serta prosedur restore; jangan mengandalkan angka kapasitas saja.

### 2.3 BetterStructures belum dibuktikan dalam bukti yang ditampilkan

V4 menyebut `BetterStructures` bersama `NusantaraStructureEngine`, tetapi log dan contoh implementasi yang diperlihatkan terutama menunjukkan `NusantaraStructureEngine`. Report ini belum menunjukkan daftar plugin aktif, versi BetterStructures, konfigurasi pack/loot, maupun hasil tes integrasinya.

**Keputusan:** untuk tahap awal, gunakan `NusantaraStructureEngine` sebagai satu sistem struktur tematik. Tambahkan BetterStructures hanya jika setelah evaluasi memang membutuhkan variasi yang tidak praktis dibuat sendiri. Jangan menjalankan dua sistem pembuat struktur dengan aturan overlap/loot yang belum diuji.

### 2.4 Peralihan generator di `kala_main` perlu diuji visual

Dokumen menyebut dunia utama dikurasi, tetapi chunk baru di luarnya memakai TerraformGenerator. Ini bisa berhasil, tetapi harus diuji di batas antara area buatan/yang sudah tergenerate dan terrain baru: ketinggian pantai, biome, sungai, gua, pepohonan, serta garis pandang ke Gunung Murka. Simpan titik koordinat dan screenshot sebelum promosi ke produksi.

### 2.5 Belum ada bukti visual GUI/title/scoreboard

V4 menjelaskan `/kompas`, tetapi belum menyertakan screenshot setiap halaman, konfigurasi sidebar, kebijakan title, refresh interval, maupun uji tap Bedrock. Karena itu aku belum bisa menyimpulkan desain sekarang sudah rapi atau ada duplikasi. Bagian berikut memberikan standar target dan cara mengauditnya.

---

## 3. Referensi pola dari server lain

Jangan menyalin UI server besar secara mentah. Ambil pola yang terbukti mudah dipahami, lalu terapkan ke identitas pulau Nusantara.

### A. Hypixel SkyBlock — menu utama sebagai pusat kategori

SkyBlock Menu memisahkan hal yang berbeda seperti Skills, Collection, Recipe Book, Trades, Quest Log, Calendar/Events, Storage, Pets, Settings, dan Fast Travel. [Referensi SkyBlock Menu](https://hypixel-skyblock.fandom.com/wiki/SkyBlock_Menu)

**Yang layak diadaptasi:** satu menu pusat yang selalu bisa dibuka, kategori dengan fungsi jelas, serta fitur lanjutan yang baru muncul ketika pemain sudah mengenalnya.

**Adaptasi Kala:** `/kompas` menjadi pusat navigasi, bukan tempat menaruh setiap command kecil. Tampilkan 8–10 kategori utama saja; fitur dengan alur mendalam masuk ke submenu. Pemain baru tidak perlu melihat seluruh sistem sekaligus.

### B. Wynncraft — scoreboard informatif sesuai konteks

Di Wynncraft, scoreboard party menampilkan status anggota party dan level; anggota offline dibedakan, dan kedekatan party dapat ditunjukkan terpisah. [Referensi Party Wynncraft](https://wynncraft.wiki.gg/wiki/Party)

**Yang layak diadaptasi:** sidebar harus menjawab kebutuhan saat ini, bukan menjadi papan iklan berisi semua statistik server. Jika nanti Kala punya sistem kelompok ekspedisi, tampilkan status party saat sedang berada dalam ekspedisi; jangan selalu menampilkan 8 baris status party ketika pemain sedang membangun rumah.

### C. EarthMC — ekonomi menjadi alasan pemain berinteraksi

EarthMC menonjolkan mata uang berbasis item, toko milik pemain, perdagangan, town, pajak, dan nilai lahan. [Dokumentasi EarthMC](https://earthmc.net/docs) · [Sistem ekonomi](https://earthmc.net/economy)

**Yang layak diadaptasi:** ekonomi terasa hidup ketika ada alasan untuk membeli, menjual, menyumbang, dan melakukan perjalanan. Kala sudah punya ekonomi dan proyek bersama; lanjutkan dengan pesanan desa, papan kebutuhan material, pasar pemain, dan pengeluaran uang yang punya manfaat jelas.

**Jangan menyalin mentah:** sistem pajak/real-estate skala EarthMC dapat terlalu kompleks untuk server survival co-op kecil. Kala lebih cocok dengan ekonomi gotong royong, bukan geopolitik.

### D. Server SMP berbasis onboarding — beri tujuan, lalu biarkan pemain memilih

Contoh SMP yang menggabungkan quest, skills, ekonomi, klaim, dan bos sering memulai pemain dari spawn, memberi titik kontak yang jelas, lalu membiarkan pemain memilih jalur. Salah satu contoh publik adalah [SkaiaCraft](https://skaiacraft.dev/), yang menjelaskan alur awal berupa spawn, quest keeper, claim, lalu pilihan aktivitas.

**Yang layak diadaptasi:** onboarding singkat di Labuhan Awal: pemain mengerti cara membuka Kompas, mengambil jurnal pertama, mencari lokasi aman untuk rumah, dan ikut proyek desa. Setelah itu, pemain boleh memilih profesi atau eksplorasi tanpa dipaksa mengikuti tutorial panjang.

---

## 4. Plugin dan alat untuk GUI yang bisa dikustomisasi

Perlu dibedakan tiga level customization:

1. **Menu logic:** slot, icon item, nama, lore, click action, permission, paging, confirm purchase.
2. **Skin inventory:** texture slot/window, custom typography, decorative frame, glyph dan background berbasis resource pack.
3. **HUD:** sidebar, tablist, name tag, bossbar, actionbar dan title.

Plugin menu biasanya menyelesaikan level 1. Plugin itu **tidak otomatis** memberi tampilan seperti game RPG dengan jendela pixel-art penuh. Untuk level 2 dibutuhkan resource pack/font/glyph yang sesuai; untuk pemain Bedrock diperlukan pack Bedrock terpisah karena Geyser tidak mengonversi pack Java otomatis. [Geyser: resource packs](https://geysermc.org/wiki/geyser/packs/) · [Geyser: custom items](https://geysermc.org/wiki/geyser/custom-items/)

### Opsi menu

| Opsi | Kegunaan | Kompatibilitas/kelebihan yang ditemukan | Rekomendasi Kala |
|---|---|---|---|
| **`KalaCore` + Paper Inventory API** | Menu inti dengan logic quest/proyek/ekonomi yang spesifik | Tidak menambah plugin runtime baru. Kontrol penuh atas transaksi, izin dan bahasa. | **Pilihan utama untuk `/kompas`** karena core sudah ada. Jadikan GUI modular, event-driven, dan reusable. |
| **Triumph GUI** atau **Inventory Framework** | Library Java untuk menata inventory GUI dalam plugin sendiri | Tercantum sebagai library UI di dokumentasi komunitas Paper/Adventure. [Daftar library Paper](https://docs.papermc.io/adventure/community-libraries/) | Pilih satu jika menu KalaCore mulai sulit dirawat. Library bukan skin grafis dan harus diuji pada toolchain proyek. Tidak wajib bila API Paper native sudah cukup. |
| **zMenu** | Menu berbasis YAML, placeholder, MiniMessage, halaman dan editor online | Halaman resminya menyebut open-source dan aktif; listing Modrinth saat riset mencantumkan kompatibilitas 26.3 serta Paper. [Website](https://zmenu.dev/) · [Modrinth](https://modrinth.com/plugin/zmenu) | **Kandidat terbaik untuk pilot menu statis** seperti `/panduan` atau menu staff di staging. Listing menyebut Bedrock support dalam konteks addon `zMenu+`; pastikan syarat/biayanya sebelum bergantung padanya. Jangan sekaligus memigrasi semua menu. |
| **DeluxeMenus** | Menu berbasis konfigurasi | Listing Modrinth mencantumkan 26.3, tetapi halaman yang dibaca menunjukkan update terakhir lebih lama daripada zMenu. [Modrinth](https://modrinth.com/plugin/deluxemenus/versions?l=paper) | Alternatif bila konfigurasi yang sudah dimiliki cocok. Uji versi jar tepat pada 26.3, jangan memasang bersama zMenu untuk fungsi yang sama. |
| **MC GUI Designer** | Mendesain background GUI/HUD berbasis glyph/resource pack; ekspor font provider dan komponen teks atau config integrasi | Dokumentasinya menjelaskan ekspor untuk font/resource pack dan integrasi plugin glyph seperti ItemsAdder, Nexo, CraftEngine. [Dokumentasi](https://docs.mcgui.app/) · [Editor](https://mcgui.app) | Gunakan sebagai **alat desain**, bukan dependency gameplay. Dokumentasi yang diperiksa menyebut pendaftaran beta sedang ditutup; kalau belum dapat akses, desain aset dengan Krita/Aseprite/Blockbench dan buat pack manual. |

### Putusan akhir untuk GUI

- Jangan pasang **zMenu + DeluxeMenus + sistem menu KalaCore** untuk menampilkan menu yang sama.
- Keep `/kompas`, `/profesi`, `/jurnal`, `/proyek`, `/pesanan`, `/musim`, `/ekspedisi`, `/kosmetik` tetap dikelola `KalaCore`; semua command mengarah ke satu sistem navigasi dengan ikon/lore konsisten.
- Bila butuh menu konfigurasi cepat, uji zMenu sebagai tambahan opsional pada staging saja. Tidak perlu memindahkan logic transaksi dari KalaCore ke YAML.
- Full custom background diputuskan setelah desain versi vanilla selesai dan diuji pada Java serta Bedrock.

---

## 5. Arah visual GUI Kala Nusantara

### 5.1 Art direction: “Peta Laut, Kayu, dan Batu Vulkanik”

Tampilan perlu terasa seperti jurnal pelaut dan peta kepulauan, bukan sci-fi neon atau GUI RPG generik.

**Palet dasar yang disarankan:**

- **Biru laut gelap** `#172B35` — dasar/identitas utama.
- **Hijau-teal laut** `#477C78` — tombol aktif, navigasi, dan progress.
- **Pasir/perkamen** `#D5C39A` — teks sekunder, jurnal, peta, informasi sejarah.
- **Tembaga vulkanik** `#B85E3D` — peringatan, Gunung Murka, event penting saja.
- **Abu batu** `#606A6B` — nonaktif atau placeholder.

Gunakan material vanilla sebagai simbol fitur, bukan emoji yang berwarna-warni. Misalnya: kompas untuk navigasi, buku untuk jurnal, peta untuk peta, spyglass untuk ekspedisi, fishing rod untuk memancing, kayu untuk proyek, gold ingot untuk ekonomi, rabbit foot atau buku untuk Atlas Fauna. Pilih satu icon per kategori dan pertahankan di seluruh menu.

### 5.2 Layout `/kompas` yang disarankan

Menu 54 slot bisa berisi 8 tombol utama plus tombol akun/setting. Jangan memenuhi semua slot dengan puluhan icon.

| Menu | Icon vanilla yang disarankan | Fungsi |
|---|---|---|
| Jurnal & Misi | Book / Written Book | Misi aktif, cerita, target berikutnya |
| Peta Kepulauan | Map / Compass | Landmark, arah, teleport yang memang diizinkan |
| Profesi | Iron Pickaxe | Level profesi, XP, tugas profesi |
| Proyek Bersama | Oak Planks / Barrel | Kontribusi material dan progres proyek desa |
| Ekspedisi | Spyglass | Tempat yang belum dikunjungi, persiapan dan hadiah eksplorasi |
| Atlas Fauna | Rabbit Foot / Book | Hewan yang ditemukan dan habitatnya |
| Pasar & Dompet | Gold Ingot / Emerald | Saldo, transfer, pesanan, pasar |
| Kosmetik | Amethyst Shard | Cosmetic yang dimiliki dan pilihan tampilan |
| Kalender Kala | Clock | Fase musim, agenda komunitas, event berikutnya |
| Panduan & Pengaturan | Compass / Paper | Tutorial, kontrol Bedrock, preferensi UI |

Gunakan **tiga status yang konsisten** di semua halaman:

- Aktif: bisa dibuka atau dikerjakan sekarang.
- Terkunci: tampilkan alasan dan syarat pembukaannya.
- Selesai: tunjukkan progres/hadiah tanpa memancing pemain mengulang aksi yang tidak bisa diulang.

**Standar item lore:** nama 1 baris yang jelas; deskripsi maksimal 2 baris; baris terakhir menjelaskan aksi atau syarat. Untuk transaksi, tampilkan jumlah, biaya dan item/hasil sebelum konfirmasi. Setelah pembelian, klik yang sama tidak boleh menggandakan item atau memproses biaya kedua.

### 5.3 Referensi visual

Pakai referensi ini untuk mempelajari kontras, konsistensi border, dan keterbacaan; jangan menyalin skin yang terlalu ramai.

- [Dark OreUI Recreation](https://modrinth.com/resourcepack/dark-oreui-recreation/gallery) — contoh tampilan chest dengan warna gelap dan grid jelas.
- [DesaturatedWine GUI](https://modrinth.com/resourcepack/desaturatedwine-gui/gallery) — contoh palet redup dan tidak terlalu neon.
- [Hollow GUI](https://modrinth.com/resourcepack/hollow-gui/gallery) — contoh frame ornamental; cukup ambil ide border, jangan terlalu banyak ornamen.
- [Glass-Style GUI](https://modrinth.com/resourcepack/glass-style-gui) — referensi tampilan minimalis dan transparan.
- [MC GUI Designer](https://docs.mcgui.app/) — contoh workflow custom GUI berbasis font glyph/resource pack.

**Rekomendasi visual:** pilih satu arah dominan: biru laut gelap + perkamen + aksen teal. Jangan menggabungkan neon sculk, emas RPG, hologram futuristis dan ornamen medieval sekaligus. Gunakan frame custom hanya untuk `/kompas` atau jurnal utama; menu transaksi dan konfirmasi boleh tetap sederhana agar mudah dibaca pemain HP.

### 5.4 Prinsip performa dan aksesibilitas

- Icon memakai item vanilla sebagai baseline; pack khusus menjadi bonus visual.
- Jangan membuat informasi penting hanya terlihat pada texture khusus. Nama item dan lore tetap menjelaskan fungsi walau pemain menolak resource pack.
- Pakai tekstur GUI/item 16× atau 32× sebagai titik awal; lakukan tes ukuran file dan waktu download sebelum menambah animasi.
- Hindari GIF-style animasi yang harus mengubah banyak elemen UI terus-menerus. Untuk highlight, cukup ubah icon/teks saat state berubah.
- Semua tombol harus mudah ditap di Bedrock mobile: jangan mengandalkan shift-click, drag, hover, atau shortcut keyboard.
- Gunakan tombol “Kembali”, “Tutup”, dan “Konfirmasi” di posisi konsisten; operasi berisiko butuh konfirmasi eksplisit.

---

## 6. Title, actionbar, bossbar, tablist, nametag dan scoreboard

Duplikasi biasanya bukan disebabkan banyaknya teks semata, tetapi beberapa sistem yang sama-sama merasa berhak menguasai slot UI yang sama. Buat **UI ownership matrix** sebagai aturan arsitektur.

### 6.1 Matriks pemilik UI

| Elemen | Pemilik yang direkomendasikan | Aturan |
|---|---|---|
| Sidebar scoreboard | **TAB** *atau* KalaCore; pilih satu. Rekomendasi: TAB untuk render, data khusus diambil dari placeholder KalaCore | KalaCore tidak boleh membuat sidebar kedua; jangan ada scoreboard lain yang menampilkan objective sidebar bersamaan. |
| Tablist header/footer | TAB | Server name, pemain online, panduan singkat. Jangan ulangi seluruh statistik sidebar. |
| Prefix/suffix nametag | TAB + LuckPerms | Satu sistem yang mengatur scoreboard teams/nametag. Jangan membuat prefix yang sama dari KalaCore/NPC plugin. |
| Bossbar | TAB hanya untuk event global terjadwal; KalaCore untuk progress personal, tetapi jangan tampilkan dua bar dengan pesan yang sama | Bossbar untuk countdown atau progress yang memiliki awal/akhir. Matikan ketika tidak relevan. |
| Title/subtitle | `KalaNotificationService` di KalaCore | Untuk chapter story, quest milestone, dan event penting saja; gunakan queue/priority/cooldown. |
| Actionbar | KalaCore | Untuk status singkat: progress quest, “3/10 kayu terkumpul”, interaksi dekat NPC. Hindari pesan permanen yang membuat chat/HUD penuh. |
| NPC/hologram | NPC manager yang ada | Jangan menampilkan nama/lore NPC sekaligus lewat beberapa layer. Batasi entity visual di area padat. |

TAB secara resmi menyediakan fungsi scoreboard, tablist, nametag, bossbar dan beberapa display lain; dokumentasinya juga menjelaskan bahwa nametag menggunakan scoreboard teams dan konflik dapat terjadi bila beberapa plugin mengendalikan prefix/suffix. [TAB compatibility](https://github.com/NEZNAMY/TAB/wiki/Compatibility) · [Panduan nametag](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Nametags) · [Panduan scoreboard](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Scoreboard)

### 6.2 Contoh sidebar yang tidak terlalu ramai

**Kompas Kala**

- `Musim: Angin Timur`
- `Lokasi: Labuhan Awal`
- `Misi: Temui Mbah Kala`
- `Proyek: Dermaga 62%`
- `Dompet: 125 Koin`
- `Online: 8`
- ` /kompas — buka menu `

Saran batas: sekitar **6–8 baris yang benar-benar berguna**, dengan teks pendek. Ini target desain, bukan batas teknis mutlak. Pada halaman biasa, tampilkan musim, lokasi, tujuan misi, proyek terdekat dan saldo. Saat pemain memasuki ekspedisi, gunakan profile scoreboard ekspedisi yang mengganti baris tidak relevan (misalnya progress ekspedisi dan status anggota). Hindari memajang ping/TPS/versi server untuk pemain biasa; data itu lebih cocok sebagai footer tablist atau perintah admin.

Jangan tampilkan hal yang sama di sidebar, bossbar, title, actionbar dan NPC sekaligus. Contoh yang benar: bossbar countdown untuk event; sidebar menunjukkan event apa; title hanya ketika event dimulai.

### 6.3 Kebijakan title/notifikasi

Buat queue kecil di KalaCore dengan prioritas, deduplikasi dan cooldown. Urutan prioritas yang disarankan:

1. **SYSTEM:** restart darurat, reset resource world, keamanan akun.
2. **STORY/EVENT:** bab cerita baru, fase musim, event komunitas.
3. **QUEST:** milestone quest yang relevan.
4. **NPC/COSMETIC:** sapaan atau efek kosmetik, prioritas paling rendah.

Aturan operasional:

- Jangan mengirim title besar setiap kali player pindah chunk atau membuka menu.
- Sambutan onboarding dikirim sekali untuk pemain baru, bukan setiap reconnect.
- Jika dua title datang bersamaan, simpan yang lebih rendah dalam antrean atau gabungkan menjadi pesan singkat.
- Actionbar yang identik jangan dikirim berulang tanpa perubahan status.
- Berikan opsi `/kompas > Pengaturan > Notifikasi` untuk menurunkan/mematikan title kosmetik dan tips, tetapi jangan menonaktifkan peringatan keselamatan.
- Gunakan MiniMessage/Adventure untuk warna dan formatting yang konsisten; pastikan teks penting tidak bergantung pada glyph khusus.

Dokumentasi Paper mencantumkan library UI untuk Adventure, dan Adventure menyediakan komponen text serta title/bossbar. [Paper/Adventure community libraries](https://docs.papermc.io/adventure/community-libraries/)

---

## 7. Fauna Nusantara dan dukungan Java–Bedrock

### Rekomendasi ringkas: **ya, tambahkan fauna, tetapi mulai dari varian entitas vanilla**. Jangan mulai dari mod yang mewajibkan client Java memasang loader, karena itu tidak menjadi pengalaman universal untuk pemain MCPE.

### 7.1 Tiga tingkat implementasi

#### Tingkat 1 — aman dan murah: vanilla entity + perilaku/data KalaCore

Pakai entitas vanilla yang sudah dikenal Geyser, kemudian tambahkan habitat, nama/lore, waktu kemunculan, drop seimbang, interaksi dan data penemuan. Ini tidak membutuhkan model client khusus dan menjadi baseline kompatibilitas.

| Konsep fauna | Basis vanilla awal | Bioma/habitat tematik | Gameplay yang cocok |
|---|---|---|---|
| Kerbau/ternak pulau | Cow | Padang rumput dan kampung | Pangan/ternak, proyek lumbung |
| Babi hutan | Pig | Hutan dan pinggiran perkampungan | Ekologi, bahan masak, tantangan ringan |
| Rusa hutan | Goat atau varian hewan yang paling sesuai setelah playtest | Hutan/hutan tinggi | Penemuan, foto/observasi, jurnal fauna |
| Burung pesisir | Parrot atau jenis vanilla yang sesuai | Hutan pesisir dan pulau | Suara/ambient, koleksi penemuan |
| Satwa perairan | Turtle, Dolphin, Cod, Tropical Fish | Terumbu/pantai/laut | Eksplorasi laut, jurnal, konservasi habitat |
| Satwa pegunungan | Goat, Fox, Rabbit | Lereng dan hutan tinggi | Ekspedisi, petunjuk arah, hadiah jurnal |

Nama-nama di atas merupakan **konsep desain**, bukan klaim bahwa bentuk vanilla sudah menyerupai spesies Indonesia. Tanpa texture/model khusus, pemain akan melihat bentuk bawaan Minecraft; beri label yang jujur atau gunakan nama tematik seperti “Rusa Pegunungan Kala” sambil menjelaskan bahwa visualnya adalah representasi bergaya.

#### Tingkat 2 — cosmetic model dengan resource pack

Untuk bentuk lebih realistis, model dapat menggunakan model/texture client-side, tetapi harus ada dua implementasi asset yang diuji terpisah: Java resource pack dan Bedrock pack. Geyser menegaskan bahwa pack Java tidak otomatis diubah menjadi Bedrock; mapping custom item juga perlu dikonfigurasi. [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/) · [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/)

Terapkan model custom pada **satu spesies prototipe** terlebih dahulu. Jangan membuat 10 fauna sekaligus sebelum bentuk, hitbox, animasi, nametag, drop, breeding/taming, dan perilakunya lolos uji pada Java dan MCPE.

#### Tingkat 3 — custom entity Bedrock

Dokumentasi Geyser menyatakan custom entity API diperkenalkan pada Geyser API 2.11.0 dan berstatus **eksperimental** dalam dokumentasi yang diperiksa. Custom entity Bedrock membutuhkan definisi entity, texture/animasi dan resource pack. Di Java, visual serupa umumnya disimulasikan dengan entity vanilla/display/armor stand, sedangkan Bedrock dapat memakai tipe entity custom. [Geyser Entity API](https://github.com/GeyserMC/GeyserWebsite/blob/master/wiki/geyser/custom-entities.md)

**Kesimpulan:** sangat mungkin sebagai proyek engineering tersendiri, tetapi bukan fitur “install plugin lalu otomatis kompatibel”. Jangan menjadikannya ketergantungan untuk quest utama sebelum API stabil dan versi Geyser yang dipakai terbukti cocok.

### 7.2 Plugin mob yang ditemukan

- **MythicMobs:** sangat kaya untuk mob/boss dan skill, tetapi listing versi yang diperiksa mencantumkan 26.2 serta sejumlah versi 1.21/1.20 — **26.3 tidak tercantum** pada halaman yang dibaca. Jangan pasang ke produksi sampai rilis yang menyatakan support 26.3 tersedia dan berhasil dites. [Versi MythicMobs](https://modrinth.com/plugin/mythicmobs/versions)
- **BetterMob:** listing yang ditemukan mencantumkan 26.3, tetapi saat riset jumlah unduh/follower masih kecil dan proyek baru diperbarui. Perlakukan sebagai eksperimen staging, bukan komponen inti yang langsung dipercaya. [BetterMob](https://modrinth.com/plugin/bettermob/)
- **ModelEngine/custom models:** cocok untuk model 3D yang terlihat pada client melalui resource pack, tetapi pack Java saja tidak menjamin hasil visual Bedrock. Uji setiap jenis entitas, jangan mengasumsikan kompatibilitas hanya karena mob-nya muncul.
- **RealisticSeasons:** tidak disarankan ditambahkan sekarang. Listing yang diperiksa hanya menyebut 1.20.x/1.21.x, sementara Kala sudah memiliki siklus musim delapan minggu. Plugin musim kedua akan berisiko membuat kalender, cuaca, crop modifier dan mob spawn menjadi dua sistem terpisah. [RealisticSeasons](https://modrinth.com/plugin/realisticseasons)

### 7.3 Sistem yang membuat fauna berguna

Buat **Atlas Fauna Kala**, bukan sekadar menambah jumlah hewan. Fitur yang disarankan:

- Pemain memperoleh entri saat pertama kali melihat/berinteraksi dengan spesies.
- Entri mencatat habitat, waktu umum, kelangkaan dan fakta lore singkat.
- Ada tujuan observasi/konservasi mingguan; hindari mendorong pemburuan satwa langka tanpa batas.
- Proyek desa dapat meminta pembangunan kandang/area konservasi atau pemulihan habitat.
- Penemuan fauna bisa membuka kosmetik, halaman jurnal atau resep tematik, bukan memberi uang/drop berlebihan yang merusak ekonomi.
- Untuk fauna langka, gunakan spawn chance dan batas jumlah per area; jangan membuat banyak entitas dekoratif di setiap chunk.

### 7.4 Batas performa fauna

- Mulai dari spesies vanilla; hindari mob dengan pathfinding kompleks di seluruh dunia.
- Batasi spawn per biome/area, dan turunkan density di sekitar spawn/permukiman.
- Tidak semua hewan harus aktif bergerak sepanjang waktu; gunakan AI vanilla dan event saat pemain berada dekat habitat.
- Hindari armor stand/display entity yang berlipat-lipat untuk setiap hewan bila model sederhana sudah cukup.
- Jangan jalankan spawn scan besar setiap tick. Jadwalkan distribusi spawn secara hemat dan ukur dengan `spark`.
- Uji pada Java desktop dan Bedrock Android: animasi, hitbox, suara, interaksi, nama, loot, despawn, dan reconnect.

---

## 8. Sistem lain yang layak ditambahkan untuk tema Kala

Jangan implementasikan semuanya sekaligus. Pilih yang menguatkan loop survival, eksplorasi, dan kerja sama.

### Prioritas A — menambah kedalaman tanpa banyak beban

**1. Papan kebutuhan desa / pesanan gotong royong**  
Desa meminta material spesifik (misalnya kayu, batu, makanan) dengan target mingguan. Kontribusi pemain terakumulasi; progres ditampilkan di Kompas, dan penyelesaian membuka fasilitas kosmetik/akses/cerita. Ini menghubungkan ekonomi, profesi dan proyek yang sudah ada.

**2. Jurnal ekspedisi dan landmark**  
Setiap tempat baru memberi entri jurnal singkat dan arah ke satu titik menarik berikutnya. Gunung Murka menjadi landmark cerita; tempat lain memberi cerita lingkungan, bukan hanya loot chest.

**3. Kalender dan event komunitas**  
Integrasikan dengan musim Kala yang sudah ada. Buat event yang dirancang khusus: perbaikan dermaga, ekspedisi pengamatan fauna, festival panen, pengumpulan material untuk pos pelaut. Hindari plugin kalender kedua.

**4. Atlas Fauna dan koleksi penemuan**  
Mengikat biome, eksplorasi dan cerita. Hadiah utamanya kosmetik, lore, atau akses aktivitas; hindari hadiah ekonomi yang terlalu besar.

### Prioritas B — sangat berguna untuk operasional

**5. Audit grief dan rollback — CoreProtect**  
GriefPrevention membantu klaim/izin, sedangkan CoreProtect memberi histori untuk investigasi dan rollback. Sumber proyek menyebut dukungan versi sampai 26.3 pada daftar unduhan Bukkit/Hangar yang diperiksa. Tetap cocokan jar yang tepat dengan Paper 26.3 sebelum produksi. [CoreProtect](https://coreprotect.org/) · [Hangar](https://hangar.papermc.io/CORE/CoreProtect) · [Dokumentasi](https://docs.coreprotect.net/)

**6. Peta web opsional — BlueMap**  
BlueMap mendukung 26.3 menurut listing yang diperiksa dan bisa menampilkan permukaan dunia dalam web viewer 3D. Ini cocok untuk peta kepulauan, landmark, dan jalur ekspedisi. Jalankan render terkontrol, pasang proteksi endpoint, dan tentukan apakah lokasi pemain/rumah/claim boleh terlihat. Jangan mengaktifkannya jika hosting belum siap menyimpan hasil render atau privasi pemain belum jelas. [BlueMap](https://modrinth.com/plugin/bluemap)

**7. Backup dan restore drill**  
Backup bukan fitur yang selesai hanya karena `/ka backup create` berhasil membuat ZIP. Simpan salinan di lokasi berbeda dari disk server, tetapkan retensi, pantau ruang kosong, dan lakukan uji restore ke direktori staging. Untuk world reset, lakukan snapshot tervalidasi dan simpan log sebelum menghapus data. Jangan mengandalkan backup di disk yang sama sebagai satu-satunya pemulihan.

### Prioritas C — lakukan setelah data alpha

**8. Pasar pemain dan ekonomi sink**  
Pasar bisa menjadi pusat interaksi, tetapi dibutuhkan biaya/tujuan yang mengurangi uang beredar secara sehat: perawatan fasilitas, kosmetik, ongkos ekspedisi/kapal, dan proyek desa. Pantau jumlah koin yang dibuat vs dihapus; jangan menambah toko admin yang membeli semua barang dengan harga tetap tanpa batas.

**9. Waypoint dan peta eksplorasi**  
Gunakan peta/kompas dan marker di jurnal untuk menjaga navigasi. Jika menggunakan BlueMap, jangan membuat fitur inti bergantung pada situs web; pemain MCPE harus tetap bisa orientasi di dalam game.

**10. Bantuan lintas perangkat**  
Halaman `Panduan Bedrock` dalam `/kompas` menjelaskan tap controls, mengakses menu, penggunaan item khusus, download resource pack, serta fallback jika pack tidak diterima. Sediakan command alternatif untuk tindakan penting.

---

## 9. Paket plugin/alat yang disarankan

Paket ini sengaja kecil. Bukan berarti harus memasang semua komponen pada hari yang sama.

| Status | Plugin/alat | Keputusan |
|---|---|---|
| Pertahankan | Paper, Geyser, Floodgate, LuckPerms, GriefPrevention, Vault provider KalaCore, KalaCore, TerraformGenerator, Chunky | Sudah menjadi fondasi yang dinyatakan di V4; audit versi/config actual sebelum update. |
| Tambah/tes lebih dulu | **TAB** | Gunakan untuk nametag, header/footer, tablist dan jika dipilih juga sidebar. Tetapkan satu pemilik setiap elemen UI. Listing dokumentasi menyebut 26.3. |
| Tambah/tes lebih dulu | **CoreProtect** | Logging perubahan blok/container dan rollback terarah; uji kompatibilitas jar 26.3. |
| Opsional | **zMenu** | Hanya untuk prototipe menu statis/config-driven; jangan menyalin semua command KalaCore ke menu kedua. |
| Opsional | **BlueMap** | Peta web, hanya jika ada kebutuhan dan kebijakan privasi. Render/akses web harus diuji. |
| Gunakan bila perlu | **Triumph GUI / Inventory Framework** | Pilih satu library untuk struktur kode GUI KalaCore, bukan instal sebagai dua plugin bersamaan. |
| Jangan tambah sekarang | DeluxeMenus bersamaan dengan zMenu; plugin musim kedua; beberapa plugin scoreboard/sidebar; plugin NPC kedua; generator struktur kedua tanpa tes | Fungsi tumpang tindih menaikkan kompleksitas, bukan kualitas otomatis. |
| Staging saja | MythicMobs, BetterMob, ModelEngine/custom entity API | Tunggu versi yang tepat, tes visual Java–Bedrock, hitbox, drop, resource pack, dan performa. |

### Catatan tentang `spark`

Paper modern sudah menyertakan spark profiler menurut dokumentasi Paper. Sebelum memasang jar spark terpisah, cek apakah versi bundled yang aktif sudah cukup; menghindari duplikasi plugin adalah pilihan default. Profiling harus dilakukan saat masalah atau beban yang ingin dipahami memang sedang terjadi. [Paper profiling docs](https://docs.papermc.io/paper/profiling/)

---

## 10. Aturan implementasi agar GUI dan fitur tidak berat

### GUI/menu

- Muat dan hitung isi menu ketika dibuka atau ketika datanya berubah; hindari loop refresh cepat untuk semua menu yang terbuka.
- Gunakan cache placeholder dan ambil data SQLite secara asynchronous bila operasinya cukup berat, tetapi jangan memanggil API Bukkit/Paper yang hanya aman di main thread dari worker thread.
- Aksi menu seperti membeli item, transfer, kontribusi proyek, dan hadiah quest harus atomik: validasi saldo/inventory/izin, catat transaksi, baru berikan hasil.
- Semua pembelian memakai confirmation page dan idempotency/cooldown untuk mencegah double-click atau replay event.
- Klik item di GUI tidak boleh mengeluarkan item filler, membuka crafting tak sengaja, atau memindahkan item ke inventory bila seharusnya tombol virtual.
- Tambahkan test otomatis untuk izin, saldo kurang, inventory penuh, offline player, double click, reconnect dan reset di tengah transaksi.

### Scoreboard/title

- Perbarui nilai berbasis event dan interval yang wajar (misalnya 1–2 detik untuk informasi dinamis; nilai jarang berubah diperbarui ketika event terjadi).
- Kirim hanya perubahan nilai jika library/plugin mengizinkan.
- Jangan jadikan TPS, ping, runtime, memory atau daftar semua command sebagai bagian wajib sidebar pemain biasa.
- Batasi title; utamakan chat/actionbar untuk informasi kecil.
- Beri konfigurasi notifikasi per kategori.

### Fauna dan dunia

- Perhatikan jumlah entitas dan chunk yang aktif, bukan hanya jumlah plugin.
- Tempatkan struktur/entitas langka dengan batas per area dan jadwal terkontrol.
- Jangan memakai custom model sebagai satu-satunya penanda mekanik penting. Bedrock player yang gagal menerima pack tetap harus dapat mengenali fungsi/target lewat nama, lore atau bentuk vanilla.
- Lakukan pregen secara bertahap; simpan metrik chunk, penggunaan disk, MSPT, heap dan durasi.

---

## 11. Rencana pengujian sebelum Closed Alpha

Lakukan di staging terisolasi. Jangan menguji custom pack, mob baru atau sistem reset pertama kali di `kala_main` produksi.

### Tahap A — audit aktual

- Catat daftar `.jar` aktif dan versi dari log startup.
- Ekspor config TAB/scoreboard/title/NPC/resource pack; cek apakah KalaCore telah mengirim sidebar atau nametag.
- Pastikan BetterStructures benar-benar aktif atau hapus klaim tersebut dari laporan.
- Audit konfigurasi authentication Geyser/Floodgate dan akses server. Jangan mengubah `online-mode` secara membabi buta; sesuaikan dengan mode instalasi Geyser dan lindungi backend dari koneksi langsung yang bisa melewati autentikasi/proxy.
- Perbaiki angka storage dan retensi backup.

### Tahap B — GUI

- Screenshot `/kompas` dan semua submenu pada Java dan Bedrock Android.
- Cek setiap tombol, item, lore, kembali, tutup, pagination, izin admin, dan state terkunci/selesai.
- Uji dengan resource pack diterima dan ditolak; fungsi penting tetap harus bisa dipahami.
- Uji 2 pemain membuka menu dan mengklik aksi yang sama bersamaan; verifikasi tidak ada transaksi duplikat.
- Pastikan hanya satu sidebar tampil, title tidak menimpa title quest lain, dan rank prefix hanya muncul sekali.

### Tahap C — fauna

- Mulai dari 1–2 spesies vanilla-style.
- Uji spawn/despawn, hitbox, nama, suara, breeding/taming bila ada, loot, claim, dan Atlas Fauna di kedua platform.
- Lakukan stress test bertahap dengan kepadatan fauna yang realistis di spawn, hutan dan pesisir.
- Jika bentuk Bedrock tidak sesuai, nonaktifkan model khusus untuk Bedrock sampai mapping/pack diselesaikan; jangan membiarkan entity invisible di gameplay penting.

### Tahap D — beban dan recovery

- Capture spark 10 menit saat kondisi yang ingin diuji aktif, misalnya pregen atau sesi bermain ramai: `/spark profiler start --timeout 600`.
- Jalankan closed test dengan kelompok kecil Java + MCPE, lalu naikkan jumlah pemain ke target beta.
- Pantau TPS, MSPT p95/max, GC, heap, CPU, database latency, ukuran disk, dan jumlah entity/chunk aktif.
- Uji resource reset dengan pemain berada di world tersebut; pastikan evakuasi, lock, backup, unload, recreate, spawn dan audit log benar-benar selesai.
- Restore backup ke staging dan verifikasi block, player data, KalaCore SQLite, plugin configs, world files dan resource pack.

### Kriteria penerimaan awal

| Area | Kriteria |
|---|---|
| UI | Tidak ada sidebar ganda; setiap title punya prioritas; actionbar tidak spam; GUI punya fungsi sama di Java dan Bedrock. |
| Data | Tidak ada transaksi ganda dari double-click/reconnect; ekonomi dan proyek konsisten setelah restart. |
| Fauna | Tidak ada entity invisible atau loot penting yang gagal di salah satu platform; density dan AI tidak mengganggu server. |
| Performa | Selama target beban, targetkan TPS stabil mendekati 20 dan MSPT p95 di bawah 40 ms; catat kondisi uji, bukan hanya angka idle. Ambang ini adalah target awal operasional yang bisa disesuaikan dengan perangkat/host. |
| Keamanan | Restore backup berhasil di staging; reset menolak path selain `kala_resource`; operasi admin tercatat; authentication/proxy mode diaudit. |
| Dokumentasi | Setiap klaim “verified” punya artefak uji/log/config dan tanggal. |

---

## 12. Roadmap eksekusi

### Fase 1 — rapikan fondasi (sebelum plugin baru)

1. Audit daftar plugin aktif dan semua display UI di KalaCore.
2. Bereskan status Production Ready dan angka storage di V4.
3. Tambahkan prosedur backup off-host dan lakukan restore drill.
4. Pastikan `NusantaraStructureEngine` vs BetterStructures punya pemilik dan aturan yang jelas.

### Fase 2 — UI consistency

1. Buat design token: warna, nama tombol, lore, state dan suara.
2. Rapikan `/kompas` dengan 8–10 kategori utama.
3. Pilih satu owner untuk sidebar, nametag, tablist, title dan bossbar. Pilot TAB di staging jika ingin mengurangi kode UI persisten di KalaCore.
4. Uji GUI Java/Bedrock menggunakan icon vanilla tanpa custom pack sebagai baseline.

### Fase 3 — resource pack ringan

1. Buat satu prototipe custom frame untuk `/kompas` atau jurnal.
2. Buat pack Java dan pack Bedrock yang ekuivalen; ukur ukuran dan waktu unduh.
3. Uji dengan pack ditolak, device mobile, reconnect dan update versi pack.
4. Jangan menambahkan model fauna sebelum UI pack stabil.

### Fase 4 — gameplay tematik

1. Implementasikan Atlas Fauna dengan 1–2 spesies awal.
2. Hubungkan penemuan fauna ke jurnal, ekspedisi, dan proyek desa.
3. Tambahkan papan kebutuhan desa/pesanan mingguan.
4. Evaluasi apakah BlueMap diperlukan setelah pemain alpha memberi masukan navigasi.

### Fase 5 — Closed Alpha

1. Undang tester Java dan Bedrock (termasuk pengguna layar sentuh).
2. Catat bug berdasarkan platform, layar, menu, entitas, serta log server.
3. Jalankan spark profile dan restore drill.
4. Ubah status “Production Ready” hanya jika kriteria penerimaan benar-benar lulus.

---

## 13. Sumber yang diperiksa

### Menu, UI dan server references

- [zMenu — website](https://zmenu.dev/)
- [zMenu — Modrinth dan versi](https://modrinth.com/plugin/zmenu)
- [DeluxeMenus — Modrinth](https://modrinth.com/plugin/deluxemenus/versions?l=paper)
- [Paper/Adventure — community UI libraries](https://docs.papermc.io/adventure/community-libraries/)
- [MC GUI Designer docs](https://docs.mcgui.app/)
- [Hypixel SkyBlock Menu reference](https://hypixel-skyblock.fandom.com/wiki/SkyBlock_Menu)
- [Wynncraft Party reference](https://wynncraft.wiki.gg/wiki/Party)
- [EarthMC docs](https://earthmc.net/docs)
- [EarthMC economy](https://earthmc.net/economy)
- [SkaiaCraft onboarding/features](https://skaiacraft.dev/)

### Scoreboard, Geyser and crossplay

- [TAB compatibility — up-to-date server version support](https://github.com/NEZNAMY/TAB/wiki/Compatibility)
- [TAB nametags](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Nametags)
- [TAB scoreboard](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Scoreboard)
- [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/)
- [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/)
- [Geyser custom entity API](https://github.com/GeyserMC/GeyserWebsite/blob/master/wiki/geyser/custom-entities.md)

### Gameplay/operasional

- [MythicMobs version compatibility](https://modrinth.com/plugin/mythicmobs/versions)
- [BetterMob](https://modrinth.com/plugin/bettermob/)
- [RealisticSeasons](https://modrinth.com/plugin/realisticseasons)
- [CoreProtect](https://coreprotect.org/)
- [CoreProtect documentation](https://docs.coreprotect.net/)
- [BlueMap](https://modrinth.com/plugin/bluemap)
- [Paper profiling / spark](https://docs.papermc.io/paper/profiling/)

**Peringatan akhir:** compatibility di halaman proyek adalah filter awal, bukan jaminan setiap kombinasi Paper build, Geyser, resource pack dan dependency akan bekerja sempurna. Pin versi yang diuji, simpan konfigurasi, lakukan staging, dan catat hasil uji lintas platform sebelum rilis.
