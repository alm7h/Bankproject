package bankprojekt.basisdaten;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Aufzählung {@link Waehrung}.
 */
class WaehrungTest {

    @Test
    void enthaeltGenauDieVierVorgesehenenWaehrungen() {
        assertArrayEquals(
                new Waehrung[]{Waehrung.EURO, Waehrung.DENAR, Waehrung.FRANC, Waehrung.DOBRA},
                Waehrung.values());
    }

    @ParameterizedTest
    @CsvSource({
            "EURO,  1.0,       EUR",
            "DENAR, 61.5,      MKD",
            "FRANC, 491.96775, KMF",
            "DOBRA, 24.5,      STN"
    })
    void kursUndKuerzel_entsprechenDerFestlegung(Waehrung waehrung, double kurs, String kuerzel) {
        assertEquals(kurs, waehrung.getUmrechnungskursZuEuro(), 0.0);
        assertEquals(kuerzel, waehrung.getKuerzel());
    }

    @ParameterizedTest
    @EnumSource(Waehrung.class)
    void toString_liefertDasKuerzel(Waehrung waehrung) {
        assertEquals(waehrung.getKuerzel(), waehrung.toString());
    }

    @ParameterizedTest
    @EnumSource(Waehrung.class)
    void umrechnungskurs_istPositiv(Waehrung waehrung) {
        assertTrue(waehrung.getUmrechnungskursZuEuro() > 0);
    }
}
