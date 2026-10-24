**Ya, konsep Kala Nusantara sudah sangat solid dan relevan dengan tren 2026.** Saya finalkan di bawah ini berdasarkan dokumen desainmu, tren server Minecraft saat ini (crossplay Java+Bedrock sangat dominan, seasonal content, co-op > hardcore PvP, quality-of-life, no permanent loss), demografi pemain (global rata-rata usia ~24 tahun, kelompok terbesar 15–21, banyak 22–35+; di Indonesia & Asia lebih muda + mobile/Bedrock-heavy), dan kebutuhan agar tidak boring, monoton, atau terlalu tegang/kompetitif.

### Finalisasi Konsep Inti (Versi 1.0 Siap Bangun)

**Satu kalimat final:**  

Survival-RPG ringan bertema Nusantara di mana satu bencana besar per musim menghancurkan tapi selalu menghasilkan kesuburan, pemain memilih risiko sendiri, barang berharga tidak pernah hilang permanen, dan musuh utama adalah alam + waktu, bukan sesama pemain.

**Prinsip yang dipertahankan & diperkuat:**

- Tidak ada kehilangan permanen (snapshot + Brankas Pengungsian + asuransi musim).

- Tidak ada kejutan (pengumuman jauh-jauh hari + Discord bot).

- Risiko = pilihan (Mode Santai default vs Mode Berani).

- Co-op di atas PvP (PvP hanya di arena/event/zona merah opsional).

- Satu menu utama (Kompas Pengembara) + fitur dibuka bertahap.

- Crossplay penuh Java + Bedrock (Geyser + Floodgate) dari hari pertama.

- Monetisasi hanya kosmetik (skin alat, efek, hiasan rumah, pet) — no pay-to-win.

**Penyesuaian tren 2026 + demografi:**

- Session pendek ramah mobile/Bedrock & pemain jarang online (banyak di Indonesia).

- Seasonal loop 8 minggu yang jelas (Tenang → Tanda → Persiapan → Letusan → Pemulihan).

- Regenerasi sumber daya kuat (dunia sumber daya di-reset, kesuburan pasca-letusan, simpul regenerasi).

- Social & gotong royong diberi reward jelas (diskon pemulihan, item kosmetik, arsip musim).

- Onboarding 30 menit yang sangat lembut + Bonus Kembali untuk absen.

Ini membedakan dari DonutSMP-style, crystal PvP, atau ekonomi grind murni yang sudah jenuh di Indonesia.

### Apakah Perlu Plugin/Mod/Texture Custom Sendiri?

**Ya, sebagian besar sistem inti harus custom**, karena mekanisme Kala (bencana bertahap + snapshot + pemulihan + zona risiko + regenerasi musim) tidak ada plugin siap pakai yang sempurna. Tapi **jangan build 100% dari nol**.

#### Strategi Hybrid (Rekomendasi Kuat)

1. **Gunakan plugin siap pakai dulu** sebagai fondasi & jembatan.

2. **Ambil mekanisme bagus** dari plugin/mod lain, lalu buat versi milik sendiri yang lebih ringan & terintegrasi.

3. **Custom plugin hanya untuk inti unik** (mulai sebagai satu plugin modular: KalaCore + modul).

**Stack Awal yang Direkomendasikan (Fase 0–1):**

- **Server:** Paper (versi stabil terbaru) + Velocity (kalau multi-world nanti).

- **Crossplay:** Geyser + Floodgate (wajib).

- **Permission & Core:** LuckPerms + PlaceholderAPI + Vault.

- **Claim dasar:** GriefPrevention atau Homestead / Lands (lalu ganti ke KalaClaim custom).

- **Economy sementara:** EssentialsX economy atau EconomySMP Core / lightman’s currency, lalu migrasi ke KalaEconomy.

- **Jobs/Profession sementara:** JobsReborn atau MrUltimateJobs → ambil sistem XP & level, lalu buat KalaProfession yang terikat bencana.

- **WorldEdit/FAWE:** untuk snapshot & pemulihan struktur.

- **NPC:** Citizens.

- **Custom items & pack:** ItemsAdder **atau** Oraxen (pilih satu). ItemsAdder lebih mudah untuk Bedrock pack generation.

- **Quest/Guide sementara:** DecentHolograms + custom book atau plugin quest sederhana → ganti ke KalaGuide.

- **Monitoring:** spark + (opsional) ServerPulse / mc-monitor.

**Yang HARUS custom (prioritas):**

- **KalaCore** — mesin musim, fase letusan, zona, pengumuman, boss bar, event API.

- **KalaClaim** — klaim + snapshot otomatis + Brankas Pengungsian + Puing + pemulihan berbiaya + asuransi.

- **KalaResource** — regenerasi, kelelahan tanah, reset dunia sumber daya, Pulau Seberang.

- **KalaEconomy** — Kepeng, harga dinamis, pasar pemain, sink.

- **KalaProfession** — 4 jalur utama yang berinteraksi dengan bencana.

- **KalaGuide** — Kompas (satu item menu), misi bertahap, hint, notifikasi Discord, UI abstraksi Java vs Bedrock form.

Mulai sebagai **satu plugin besar dengan modul** (lebih mudah debug), pecah nanti kalau perlu.

**Texture/Resource Pack:**

- Wajib custom (tema Nusantara: batik, ukiran, kayu, bambu, palet hangat).

- Buat di Blockbench (16x/32x dulu).

- Satu pack Java sebagai master → generate Bedrock via ItemsAdder/Geyser mapping.

- Fokus item sistem + bahan bencana + alat tier Kala + ikon GUI dulu (~60 aset fase 1 seperti di dokumen).

- Hindari custom head & model 3D kompleks di awal (Bedrock lemah).

### Mekanisme yang Perlu Ditambah/Update agar Tidak Boring & Tidak Tegang

Dokumenmu sudah bagus. Tambahan/penyempurnaan berdasarkan tren retensi:

**Anti-boring (variasi & tujuan jangka panjang):**

- **Arsip Musim + Buku Sejarah** — setiap musim berakhir dengan “buku” yang menampilkan kontribusi pemain (kampung, jasa pemulihan, discovery). Alasan emosional untuk kembali.

- **Pulau Seberang yang berubah setiap musim** + misi discovery ringan.

- **Event mingguan ringan** yang selalu beda: pasar rakyat, lomba kapal, misi gotong royong, fishing contest, “Latihan Kala” simulasi.

- **Profesi level 1–15 awal** + konten baru per musim (bukan grind monster, tapi aktivitas natural).

- **Kosmetik unlock** dari aktivitas (bukan bayar) + shop kosmetik berbayar.

- **Daily/Weekly soft goals** lewat Kompas (bukan quest grind keras).

- **Meter kesehatan tanah** yang terlihat semua orang → sosial & strategi.

**Anti-tegang / anti-kompetitif berlebihan:**

- Mode Santai default (zona hijau/kuning, pemulihan murah).

- PvP hanya di tempat khusus + opt-in.

- Kematian di zona aman = Nisan (peti makam) yang protected sebentar.

- Asuransi 1–2x full recovery per musim.

- Bonus Kembali untuk absen >7 hari.

- Tidak ada rank pay-to-win atau kit OP berbayar.

**Quality of Life tambahan yang sangat direkomendasikan:**

- Teleport ringan antar kampung (dengan cooldown/cost kecil).

- One-click “Pulihkan Kampung” dari Kompas.

- Laporan Kerusakan pribadi yang jelas setelah letusan.

- Discord bot notifikasi otomatis (72 jam, 24 jam, hasil letusan).

- UI form Bedrock yang sangat sederhana & touch-friendly.

### Roadmap Praktis (Disesuaikan 1 Developer)

- **Fase 0 (2–4 minggu):** Paper + Geyser + claim dasar + economy sederhana + 1 letusan dummy + snapshot 1 klaim + Kompas basic + resource pack minimal.

- **Fase 1 (1–2 bulan):** KalaCore + KalaClaim lengkap + zona + Brankas + tutorial 30 menit + aset fase 1.

- **Fase 2 (beta):** Resource + Economy + Profesi + 1 musim penuh 20–50 pemain.

- **Fase 3:** Rilis publik + monetisasi kosmetik + bot Discord + moderasi.

### Keputusan yang Perlu Segera

1. Nama final (server + gunung).

2. ItemsAdder vs Oraxen.

3. Lokasi hosting (latency Indonesia prioritas).

4. Apakah beta pertama gratis atau “supporter” ringan.

5. Mulai custom plugin sekarang atau pakai plugin siap pakai dulu selama 4–6 minggu.

