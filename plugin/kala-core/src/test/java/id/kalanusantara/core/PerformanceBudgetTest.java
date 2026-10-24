package id.kalanusantara.core;

import id.kalanusantara.core.event.WorldImpactPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceBudgetTest {

    @Test
    @DisplayName("Batas aman performa dan anggaran entitas event harus konsisten")
    public void testBudgetCaps() {
        assertEquals(6, WorldImpactPolicy.MAX_ROCKFALL_PER_SALVO, "Maksimal rockfall per salvo adalah 6");
        assertEquals(12, WorldImpactPolicy.MAX_ACTIVE_HAZARDS, "Maksimal hazard aktif adalah 12");
        assertEquals(6, WorldImpactPolicy.MAX_EVENT_MOBS, "Maksimal mob encounter adalah 6");
        assertEquals(24, WorldImpactPolicy.TOTAL_EVENT_ENTITY_CAP, "Total cap entitas event adalah 24");
        assertTrue(WorldImpactPolicy.MAX_ROCKFALL_PER_SALVO + WorldImpactPolicy.MAX_ACTIVE_HAZARDS + WorldImpactPolicy.MAX_EVENT_MOBS >= WorldImpactPolicy.TOTAL_EVENT_ENTITY_CAP);
    }
}
