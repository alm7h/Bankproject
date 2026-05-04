package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SparbuchTest {

    private Sparbuch sparbuch;
    private Kunde inhaber;

    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Mustermann", "Musterstraße 1", "01/01/90");
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
    // Wir versuchen 2001€ abzuheben
    @Test
    void testAbhebenLimitUeberschritten() throws GesperrtException {
        boolean erfolg = sparbuch.abheben(new Geldbetrag(2001, Waehrung.EURO));
        assertFalse(erfolg, "Sollte fehlschlagen, da > 2000€");
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
    
    
}