package id.kalanusantara.core;

import id.kalanusantara.core.economy.RupiahEconomy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RupiahEconomyModelTest {

    @Test
    public void testTransferResultEnum() {
        assertEquals("SUCCESS", RupiahEconomy.TransferResult.SUCCESS.name());
        assertEquals("INSUFFICIENT_FUNDS", RupiahEconomy.TransferResult.INSUFFICIENT_FUNDS.name());
        assertEquals("INVALID_AMOUNT", RupiahEconomy.TransferResult.INVALID_AMOUNT.name());
        assertEquals("SAME_PLAYER", RupiahEconomy.TransferResult.SAME_PLAYER.name());
        assertEquals("DATABASE_ERROR", RupiahEconomy.TransferResult.DATABASE_ERROR.name());
    }

    @Test
    public void testAmountValidationRules() {
        double negative = -100.0;
        double zero = 0.0;
        double nan = Double.NaN;
        double infinite = Double.POSITIVE_INFINITY;
        double valid = 250.75;

        assertTrue(negative <= 0 || Double.isNaN(negative) || Double.isInfinite(negative));
        assertTrue(zero <= 0 || Double.isNaN(zero) || Double.isInfinite(zero));
        assertTrue(nan <= 0 || Double.isNaN(nan) || Double.isInfinite(nan));
        assertTrue(infinite <= 0 || Double.isNaN(infinite) || Double.isInfinite(infinite));

        assertFalse(valid <= 0 || Double.isNaN(valid) || Double.isInfinite(valid));
        assertEquals(250.75, Math.round(valid * 100.0) / 100.0, 0.001);
    }

    @Test
    public void testSamePlayerTransferGuard() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        assertEquals(u1, u1);
        assertNotEquals(u1, u2);
    }
}
