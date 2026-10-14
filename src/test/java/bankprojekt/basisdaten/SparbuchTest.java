package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.nuetzliches.Kalender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das {@link Sparbuch} mit echtem {@link Kalender}.
 * Alles, was vom Datum abhängt (Monatslimit, Monats- und Jahreswechsel), steht in
 * {@link SparbuchMockitoTest}, damit die Tests nicht vom heutigen Datum abhängen.
 */
class SparbuchTest {

    private Sparbuch sparbuch;
    private Kunde inhaber;

    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Mustermann", "Musterstraße 1", "01.01.90");
        sparbuch = new Sparbuch(inhaber, 12345678);
        sparbuch.einzahlen(new Geldbetrag(1000, Waehrung.EURO));
    }

    // Testet den normalen Fall
    @Test
    void testAbhebenErfolgreich() throws GesperrtException {
        boolean erfolg = sparbuch.abheben(new Geldbetrag(100, Waehrung.EURO));
        assertTrue(erfolg);
        assertEquals(900.0, sparbuch.getKontostand().getBetrag(), 0.001);
    }

    // Deckt die GesperrtException ab
    @Test
    void testAbhebenGesperrt() {
        sparbuch.sperren();
        assertThrows(GesperrtException.class, () -> {
            sparbuch.abheben(new Geldbetrag(10, Waehrung.EURO));
        });
    }

    // Deckt das 2000€-Limit pro Monat ab
    // Wir versuchen 2001€ abzuheben. Damit wirklich das Limit (und nicht das
    // Mindestguthaben) greift, muss genug Geld auf dem Konto sein.
    @Test
    void testAbhebenLimitUeberschritten() throws GesperrtException {
        sparbuch.einzahlen(new Geldbetrag(4000, Waehrung.EURO));
        boolean erfolg = sparbuch.abheben(new Geldbetrag(2001, Waehrung.EURO));
        assertFalse(erfolg, "Sollte fehlschlagen, da > 2000€");
        assertEquals(new Geldbetrag(5000), sparbuch.getKontostand());
    }

    // Deckt die Regel ab: Kontostand darf nicht unter 0.50€ fallen.
    // Aktuell 1000€ drauf. Abhebung von 999.60€ würde 0.40€ hinterlassen.
    @Test
    void testAbhebenMindeststandUnterschritten() throws GesperrtException {
        boolean erfolg = sparbuch.abheben(new Geldbetrag(999.60, Waehrung.EURO));
        assertFalse(erfolg, "Sollte fehlschlagen, da Restsaldo < 0.50€");
    }

    // Prüft den Fall: betrag == null
    @Test
    void testAbhebenNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            sparbuch.abheben(null);
        });
    }

    // Prüft den Fall: betrag.isNegativ()
    @Test
    void testAbhebenNegativ() {
        Geldbetrag negativ = new Geldbetrag(-10, Waehrung.EURO);
        assertThrows(IllegalArgumentException.class, () -> {
            sparbuch.abheben(negativ);
        });
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztInhaberUndNummer_undKontostandNull() {
            Sparbuch neu = new Sparbuch(inhaber, 4711);
            assertSame(inhaber, neu.getInhaber());
            assertEquals(4711, neu.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, neu.getKontostand());
        }

        @Test
        void konstruktor_wirftNullPointerException_wennInhaberNull() {
            assertThrows(NullPointerException.class, () -> new Sparbuch(null, 4711));
            assertThrows(NullPointerException.class, () -> new Sparbuch(null, 4711, new Kalender()));
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_wennKalenderNull() {
            assertThrows(IllegalArgumentException.class, () -> new Sparbuch(inhaber, 4711, null));
        }

        @Test
        void standardkonstruktor_erzeugtLeeresMustermannSparbuch() throws GesperrtException {
            Sparbuch standard = new Sparbuch();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(1234567, standard.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, standard.getKontostand());
            // Auch das Standard-Sparbuch hat einen Kalender und kann abheben
            standard.einzahlen(new Geldbetrag(10));
            assertTrue(standard.abheben(new Geldbetrag(1)));
        }
    }

    @Nested
    class MindestguthabenTests {
        @Test
        void abheben_gibtTrue_wennGenauDasMinimumBleibt() throws GesperrtException {
            assertTrue(sparbuch.abheben(new Geldbetrag(999.50)));
            assertEquals(Sparbuch.MINIMUM, sparbuch.getKontostand());
        }

        @Test
        void abheben_gibtFalse_wennEinCentUnterDemMinimum_undKontostandBleibt() throws GesperrtException {
            assertFalse(sparbuch.abheben(new Geldbetrag(999.51)));
            assertEquals(new Geldbetrag(1000), sparbuch.getKontostand());
        }

        @Test
        void abheben_gibtFalse_beiLeeremSparbuch() throws GesperrtException {
            Sparbuch leer = new Sparbuch(inhaber, 4711);
            assertFalse(leer.abheben(new Geldbetrag(0.01)), "Kein Überziehen möglich");
        }
    }

    @Nested
    class WaehrungTests {
        @Test
        void abheben_rechnetFremdwaehrungUm() throws GesperrtException {
            assertTrue(sparbuch.abheben(new Geldbetrag(61.5, Waehrung.DENAR)));
            assertEquals(new Geldbetrag(999), sparbuch.getKontostand());
            assertEquals(Waehrung.EURO, sparbuch.getKontostand().getWaehrung());
        }

        @Test
        void mindestguthaben_giltAuchNachWaehrungswechsel() throws GesperrtException {
            sparbuch.waehrungswechsel(Waehrung.DENAR);
            assertEquals(61500.0, sparbuch.getKontostand().getBetrag(), 0.001);
            // 0,50 € entsprechen 30,75 MKD
            assertFalse(sparbuch.abheben(new Geldbetrag(61469.26, Waehrung.DENAR)));
            assertTrue(sparbuch.abheben(new Geldbetrag(61469.25, Waehrung.DENAR)));
            assertEquals(30.75, sparbuch.getKontostand().getBetrag(), 0.001);
            assertEquals(Waehrung.DENAR, sparbuch.getKontostand().getWaehrung());
        }
    }

    @Test
    void toString_enthaeltKontoartUndZinssatz() {
        String ausgabe = sparbuch.toString();
        assertTrue(ausgabe.startsWith("-- SPARBUCH --"), ausgabe);
        assertTrue(ausgabe.contains("Zinssatz: 3.0%"), ausgabe);
    }
}
