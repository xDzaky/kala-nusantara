package id.kalanusantara.core;

import id.kalanusantara.core.contract.VillageContractManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class VillageContractIsoWeekTest {

    private static final Pattern ISO_WEEK_PATTERN = Pattern.compile("^WEEK_\\d{4}_W\\d{2}$");

    @Test
    public void testIsoWeekFormat() {
        String weekId = VillageContractManager.getCurrentIsoWeekId();
        assertNotNull(weekId, "Week ID tidak boleh null");
        assertTrue(ISO_WEEK_PATTERN.matcher(weekId).matches(),
                "Week ID harus mengikuti pola format ISO WEEK_YYYY_Www, didapat: " + weekId);
    }

    @Test
    public void testIsoWeekValues() {
        String weekId = VillageContractManager.getCurrentIsoWeekId();
        String[] parts = weekId.split("_");
        assertEquals(3, parts.length, "Format weekId harus dipisahkan 3 token: WEEK, YYYY, Www");
        assertEquals("WEEK", parts[0]);

        int year = Integer.parseInt(parts[1]);
        assertTrue(year >= 2024, "Tahun harus valid (>= 2024)");

        String weekPart = parts[2];
        assertTrue(weekPart.startsWith("W"), "Token minggu harus diawali huruf W");
        int weekNumber = Integer.parseInt(weekPart.substring(1));
        assertTrue(weekNumber >= 1 && weekNumber <= 53, "Nomor minggu ISO harus berada di antara 1 dan 53");
    }

    @Test
    public void testKnownIsoWeekCalculation() {
        LocalDate date = LocalDate.of(2026, 10, 25);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String expected = String.format("WEEK_%04d_W%02d", year, week);

        assertEquals("WEEK_2026_W43", expected, "25 Oktober 2026 adalah minggu ISO ke-43 tahun 2026");
    }
}
