package id.kalanusantara.core.fauna;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * FaunaSpecies — Satwa Khas Kepulauan Nusantara (Tingkat 1: Vanilla Safe).
 * Menggunakan entitas dasar vanilla Minecraft sehingga 100% kompatibel
 * dengan Java Edition dan Bedrock MCPE (Geyser/Floodgate) tanpa mod wajib.
 */
public enum FaunaSpecies {
    KERBAU_PULAU(
            "KERBAU_PULAU",
            "Kerbau Lumpur Nusantara",
            EntityType.COW,
            Material.LEATHER,
            "Padang Rumput & Lembah Hijau",
            "Satwa pekerja tangguh yang membantu petani membajak sawah berundak dan mengangkut panen ke lumbung kampung.",
            "Umum / Dilindungi Desa",
            50.0
    ),
    RUSA_KALA(
            "RUSA_KALA",
            "Rusa Gunung Murka",
            EntityType.GOAT,
            Material.GOAT_HORN,
            "Lereng Gunung & Hutan Tinggi",
            "Satwa lincah yang mendiami lereng terjal Gunung Murka. Menurut mitos warga, kemunculannya di kaki gunung adalah penanda status aktivitas vulkanik.",
            "Langka / Sakral",
            75.0
    ),
    BABI_HUTAN(
            "BABI_HUTAN",
            "Celeng Rimba Selatan",
            EntityType.PIG,
            Material.PORKCHOP,
            "Hutan Lebat & Belantara Tropis",
            "Satwa endemik rimba tropis yang tangguh dan selalu berkelana mencari umbi-umbian di bawah kanopi lebat.",
            "Umum / Liar",
            50.0
    ),
    BURUNG_PESISIR(
            "BURUNG_PESISIR",
            "Cendrawasih Pesisir",
            EntityType.PARROT,
            Material.FEATHER,
            "Pesisir Pantai & Hutan Bakau",
            "Burung eksotis dengan bulu berkilau seperti zamrud samudra. Suaranya menjadi penuntun pelaut saat mendekati pulau berkarang.",
            "Sangat Langka / Ikonik",
            100.0
    ),
    PENYU_SAMUDRA(
            "PENYU_SAMUDRA",
            "Penyu Belimbing Segara",
            EntityType.TURTLE,
            Material.TURTLE_SCUTE,
            "Pantai Pasir Putih & Terumbu Karang",
            "Penjelajah samudra purba yang berenang ribuan mil melintasi arus Nusantara untuk bertelur di pasir hangat Labuhan Awal.",
            "Dilindungi / Keramat",
            75.0
    ),
    LUTUNG_JAWA(
            "LUTUNG_JAWA",
            "Lutung Kanopi Tropis",
            EntityType.FOX,
            Material.SWEET_BERRIES,
            "Hutan Kanopi & Lembah Bambu",
            "Primata lincah penghuni pucuk pohon tropis. Sering terlihat bergelantungan saat fajar menyingsing di atas kanopi Nusantara.",
            "Langka / Terancam Punah",
            75.0
    );

    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final Material icon;
    private final String habitat;
    private final String lore;
    private final String conservationStatus;
    private final double rewardRupiah;

    FaunaSpecies(String id, String displayName, EntityType entityType, Material icon,
                 String habitat, String lore, String conservationStatus, double rewardRupiah) {
        this.id = id;
        this.displayName = displayName;
        this.entityType = entityType;
        this.icon = icon;
        this.habitat = habitat;
        this.lore = lore;
        this.conservationStatus = conservationStatus;
        this.rewardRupiah = rewardRupiah;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public Material getIcon() { return icon; }
    public String getHabitat() { return habitat; }
    public String getLore() { return lore; }
    public String getConservationStatus() { return conservationStatus; }
    public double getRewardRupiah() { return rewardRupiah; }

    public static FaunaSpecies fromEntityType(EntityType type) {
        for (FaunaSpecies s : values()) {
            if (s.getEntityType() == type) return s;
        }
        return null;
    }
}
