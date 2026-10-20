package bankprojekt.basisdaten;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

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

    /**
     * Der exakte Kurs ist genau der festgelegte Dezimalwert. Beim Franc unterscheidet
     * sich dieser von der Binärdarstellung des double (491,96775000000002…).
     * @param waehrung die Währung
     * @param kurs der festgelegte Kurs als Dezimaltext
     */
    @ParameterizedTest
    @CsvSource({
            "EURO,  1",
            "DENAR, 61.5",
            "FRANC, 491.96775",
            "DOBRA, 24.5"
    })
    void kursExakt_entsprichtGenauDemDezimalwert(Waehrung waehrung, String kurs) {
        assertEquals(0, new BigDecimal(kurs).compareTo(waehrung.getUmrechnungskursExakt()));
    }

    /**
     * Der double-Kurs wird aus dem exakten Kurs abgeleitet: Er ist der nächstgelegene
     * double-Wert, also derselbe Wert wie das frühere double-Literal.
     * @param waehrung die Währung
     */
    @ParameterizedTest
    @EnumSource(Waehrung.class)
    void kursAlsDouble_istDerNaechstgelegeneWertZumExaktenKurs(Waehrung waehrung) {
        assertEquals(waehrung.getUmrechnungskursExakt().doubleValue(), waehrung.getUmrechnungskursZuEuro());
    }

    @ParameterizedTest
    @EnumSource(Waehrung.class)
    void umrechnungskursExakt_istPositiv(Waehrung waehrung) {
        assertEquals(1, waehrung.getUmrechnungskursExakt().signum());
    }
}
