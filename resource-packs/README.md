# Kala Nusantara — Resource Pack & Mapping Pipeline (v8.0)

Dokumen dan struktur ini mendefinisikan pipeline pengelolaan asset lintas Java Edition dan Bedrock Edition (Geyser/Floodgate).

## 1. Prinsip Desain
- **Vanilla Fallback:** Setiap custom item memiliki padanan material vanilla sehingga gameplay tetap dapat diselesaikan 100% meskipun pemain menolak atau gagal mengunduh resource pack.
- **Crossplay Parity:** Pack Java dan Bedrock dipelihara secara terpisah dengan manifest yang terverifikasi.
- **Tanpa Client Mod:** Tidak membutuhkan Forge/Fabric/OptiFine di sisi klien pemain.

## 2. Struktur Direktori
```text
resource-packs/
  ├── java/                # Resource pack standar Java Edition (pack.mcmeta, assets)
  ├── bedrock/             # Resource pack Bedrock (.mcpack manifest.json, textures)
  ├── mappings/geyser/     # Geyser custom item mappings (custom_mappings.json)
  ├── manifest.yml         # Manifest ringkasan versi dan daftar item server
  └── build/               # Tempat artefak build zip/.mcpack sebelum hosting
```

## 3. Daftar Item Bertema Nusantara
1. **Kompas Pengembara** (`minecraft:compass`) — Membuka navigator kepulauan.
2. **Kit Survei Lapangan** (`minecraft:spyglass`) — Pencatatan observasi satwa dan sampel geologi.
3. **Lampu Suar Pesisir** (`minecraft:lantern`) — Penanda sinyal pelabuhan saat event cuaca.
4. **Kerudung Abu Vulkanik** (`minecraft:leather_helmet`) — Proteksi visual di sekitar kawah.
