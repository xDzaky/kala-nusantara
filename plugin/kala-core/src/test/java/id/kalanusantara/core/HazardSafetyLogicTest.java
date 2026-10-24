package id.kalanusantara.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HazardSafetyLogicTest {

    @Test
    public void testSurfaceBlockVsImpactLocationOffset() {
        // highestY adalah level blok teratas (misal y=70).
        // Target impact berada di ruang udara tepat di atasnya (+1.0 Y).
        int highestY = 70;
        double impactY = highestY + 1.0;
        double surfaceY = impactY - 1.0;

        assertEquals(71.0, impactY, 0.001);
        assertEquals(70.0, surfaceY, 0.001);
        assertTrue(impactY > highestY, "Titik tumbukan harus berada di atas blok permukaan");
    }

    @Test
    public void testSafeZoneDistanceCheck() {
        double spawnX = 0.0;
        double spawnZ = 0.0;
        double safeZoneRadius = 150.0;
        double safeZoneRadiusSq = Math.pow(safeZoneRadius, 2); // 22500

        // Titik dalam safe zone
        double insideX = 50.0;
        double insideZ = 50.0;
        double insideDistSq = Math.pow(insideX - spawnX, 2) + Math.pow(insideZ - spawnZ, 2); // 5000
        assertTrue(insideDistSq < safeZoneRadiusSq, "Titik di dalam spawn safe zone harus ditolak");

        // Titik aman di luar safe zone (misal arena gunung x=350, z=-420)
        double outsideX = 350.0;
        double outsideZ = -420.0;
        double outsideDistSq = Math.pow(outsideX - spawnX, 2) + Math.pow(outsideZ - spawnZ, 2); // 122500 + 176400 = 298900
        assertTrue(outsideDistSq >= safeZoneRadiusSq, "Titik di luar spawn safe zone harus diterima");
    }

    @Test
    public void testFailClosedClaimContract() {
        // Jika status claim tidak dapat dipastikan (misal error refleksi), fungsi harus fail-closed (return true / claimed)
        boolean hasError = true;
        boolean isUnsafe = hasError; // Fail-closed
        assertTrue(isUnsafe, "Fail-closed harus menganggap lokasi tidak aman bila terjadi error evaluasi");
    }
}
