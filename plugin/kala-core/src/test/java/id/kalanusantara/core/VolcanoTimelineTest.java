package id.kalanusantara.core;

import id.kalanusantara.core.event.VolcanoEncounterDirector.EncounterSubphase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VolcanoTimelineTest {

    @Test
    @DisplayName("Subfase Volcano Encounter harus memiliki total durasi 600 detik (10 menit) tanpa jeda")
    public void testTimelineDurations() {
        EncounterSubphase[] subphases = EncounterSubphase.values();
        assertEquals(6, subphases.length, "Harus ada tepat 6 subfase berirama");

        // Periksa kontinuitas interval
        int prevEnd = 0;
        for (EncounterSubphase sp : subphases) {
            assertEquals(prevEnd, sp.getStartSec(), "Subfase " + sp.name() + " harus dimulai tepat di akhir subfase sebelumnya");
            assertTrue(sp.getEndSec() > sp.getStartSec(), "Durasi subfase harus bernilai positif");
            prevEnd = sp.getEndSec();
        }

        assertEquals(600, prevEnd, "Total waktu akhir subfase harus tepat 600 detik (10 menit)");
    }
}
