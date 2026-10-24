# 📋 Panduan Setup In-Game — Phase 1 Kala Nusantara

> Jalankan perintah-perintah ini setelah kamu join server sebagai OP.

---

## 1. Set Dirimu Sebagai OP & Admin

```
/op Dzaky_Ori
```
*(sudah dilakukan jika kamu bisa ketik perintah OP)*

Lalu set LuckPerms admin:
```
/lp user Dzaky_Ori parent set admin
/lp user Dzaky_Ori permission set '*' true
```

---

## 2. Setup World Border

Batasi dunia 6000×6000 (radius 3000):
```
/worldborder set 6000
/worldborder center 0 0
```

---

## 3. Cari & Set Spawn Labuhan Awal

Pindah ke mode Creative untuk scouting:
```
/gamemode creative
/tp 0 100 0
```

Cari tempat yang bagus (pantai/pelabuhan) lalu:
```
/setworldspawn
/ka setspawn
```

Set world border center di spawn:
```
/worldborder center ~ ~
```

---

## 4. Spawn NPC Mbah Kala

Plugin NPCs (AugmentedThunder) menggunakan skin-based NPC.
NMS fake-player tidak tersedia di MC 26.3, tapi NPC armor-stand bekerja:

```
/npc create "§6Mbah Kala" --type ARMOR_STAND
```

Atau coba tipe PLAYER jika tersedia:
```
/npc create "§6Mbah Kala"
/npc select
/npc skin MHF_Alex
```

Untuk lihat semua command NPC:
```
/npc help
```

---

## 5. Test KalaCore Commands

```
/kompas          ← Buka Kompas Pengembara (GUI 54 slot)
/jurnal          ← Buka jurnal
/kala saldo      ← Cek saldo Rupiah Kala
/kala profil     ← Lihat profil
/ka status       ← Status admin KalaCore
/ka reload       ← Reload config
```

---

## 6. Pre-gen Spawn Area (opsional, berat)

Pre-gen chunk 500 blok di sekitar spawn agar tidak lag:
```
/chunky center 0 0
/chunky radius 500
/chunky start
```

*(Ini akan memakan waktu ~5-10 menit, bisa ditinggal)*

---

## 7. Give Kompas Pengembara ke Dirimu

```
/give Dzaky_Ori minecraft:compass 1
```

Lalu klik kanan → menu Kompas Pengembara terbuka!

---

## 8. Test Ekonomi

```
/ka give-rupiah Dzaky_Ori 1000
/kala saldo
```

---

## Catatan Issue

| Plugin | Status | Keterangan |
|--------|--------|------------|
| NPCs 0.2.4 | ⚠️ Partial | NMS fake-player tidak support MC 26.3, NPC tipe lain OK |
| CoreProtect 24.1 | ❌ Disabled | Tidak support MC 26.3 |
| KalaCore 0.1.0 | ✅ Running | Version string muncul `${project.version}` — cosmetic only |

---

## IP Server

- **Java:** `192.168.1.14:25565`  
- **Bedrock:** `192.168.1.14:19132`
