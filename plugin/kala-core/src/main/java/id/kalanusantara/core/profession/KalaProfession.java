package id.kalanusantara.core.profession;

/**
 * KalaProfession — daftar profesi yang tersedia di Kala Nusantara.
 *
 * Setiap profesi punya:
 *   - id          : key unik (disimpan ke DB)
 *   - displayName : nama tampilan dalam game (bahasa Indonesia)
 *   - description : deskripsi singkat
 *   - icon        : Material untuk ikon di Kompas GUI
 *   - questStart  : quest pertama yang otomatis diberi saat pilih profesi
 */
public enum KalaProfession {

    PENGEMBARA(
        "Pengembara",
        "Petualang bebas yang belum memilih jalan.",
        "COMPASS",
        null
    ),
    TUKANG_KAYU(
        "Tukang Kayu",
        "Mengolah kayu menjadi barang berguna untuk kampung.",
        "OAK_LOG",
        "quest_tukang_kayu"
    ),
    PANDAI_BESI(
        "Pandai Besi",
        "Menempa logam dan membuat peralatan tahan lama.",
        "IRON_INGOT",
        null  // quest belum dibuat
    ),
    PETANI(
        "Petani",
        "Menanam, merawat, dan memanen hasil bumi.",
        "WHEAT_SEEDS",
        null
    ),
    NELAYAN(
        "Nelayan",
        "Menjelajah lautan dan menangkap ikan segar.",
        "FISHING_ROD",
        null
    ),
    PERAMU(
        "Peramu",
        "Mengumpulkan tumbuhan, ramuan, dan bahan alam.",
        "FERN",
        null
    ),
    PELAUT(
        "Pelaut",
        "Menjadi navigator kapal dan membuka rute perdagangan.",
        "MAP",
        null
    );

    private final String displayName;
    private final String description;
    private final String iconMaterial;   // Material name string (Bukkit)
    private final String startQuestId;

    KalaProfession(String displayName, String description, String iconMaterial, String startQuestId) {
        this.displayName   = displayName;
        this.description   = description;
        this.iconMaterial  = iconMaterial;
        this.startQuestId  = startQuestId;
    }

    public String getDisplayName()   { return displayName; }
    public String getDescription()   { return description; }
    public String getIconMaterial()  { return iconMaterial; }
    public String getStartQuestId()  { return startQuestId; }

    /**
     * Cari profesi dari string id (key enum, case-insensitive).
     * Return PENGEMBARA jika tidak ditemukan.
     */
    public static KalaProfession fromId(String id) {
        if (id == null) return PENGEMBARA;
        for (KalaProfession p : values()) {
            if (p.name().equalsIgnoreCase(id)) return p;
        }
        return PENGEMBARA;
    }
}
