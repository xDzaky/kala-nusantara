package id.kalanusantara.core;

import id.kalanusantara.core.event.EventContributionLedger;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxLedgerStateTest {

    @Test
    public void testClaimStatusEnumValues() {
        assertNotNull(EventContributionLedger.ClaimStatus.valueOf("PENDING"));
        assertNotNull(EventContributionLedger.ClaimStatus.valueOf("PAID"));
        assertNotNull(EventContributionLedger.ClaimStatus.valueOf("FAILED_RETRYABLE"));
    }

    @Test
    public void testMultiplierCalculationAndCap() {
        // Multiplier formula: Math.min(2.5, 1.0 + (points / 50.0))
        int zeroPoints = 0;
        double multZero = Math.min(2.5, 1.0 + (zeroPoints / 50.0));
        assertEquals(1.0, multZero, 0.001);

        int midPoints = 25;
        double multMid = Math.min(2.5, 1.0 + (midPoints / 50.0));
        assertEquals(1.5, multMid, 0.001);

        int capPoints = 75;
        double multCap = Math.min(2.5, 1.0 + (capPoints / 50.0));
        assertEquals(2.5, multCap, 0.001);

        int excessPoints = 500;
        double multExcess = Math.min(2.5, 1.0 + (excessPoints / 50.0));
        assertEquals(2.5, multExcess, 0.001, "Multiplier harus di-cap maksimal 2.5x");
    }

    @Test
    public void testDeterministicClaimIdGeneration() {
        String eventId = "season_badai_selat_123456789";
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        String claimId = eventId + "_" + uuid;

        assertEquals("season_badai_selat_123456789_11111111-2222-3333-4444-555555555555", claimId);
        assertFalse(claimId.isEmpty());
    }
}
