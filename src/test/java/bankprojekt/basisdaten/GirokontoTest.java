package bankprojekt.basisdaten;

import static org.junit.jupiter.api.Assertions.*;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Waehrung;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GirokontoTest {

    private Girokonto konto;
    private Kunde inhaber;


    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Mustermann", "Musterstraße 1", "01/01/90");
        konto = new Girokonto(inhaber, 12345678, new Geldbetrag(500, Waehrung.EURO));
    }

    @Test
    void einzahlen() {
        // 1. Test: Normale Einzahlung in Euro
        Geldbetrag einzahlungEuro = new Geldbetrag(100, Waehrung.EURO);
        konto.einzahlen(einzahlungEuro);
        assertEquals(100.0, konto.getKontostand().getBetrag(), 0.001);

        // 2. Test: Einzahlung in Fremdwährung
        Geldbetrag einzahlungDenar = new Geldbetrag(61.5, Waehrung.DENAR);
        konto.einzahlen(einzahlungDenar);

        // Da das Konto in Euro ist, müssen nun 101,00 Euro drauf sein (100 + 1)
        assertEquals(101.0, konto.getKontostand().getBetrag(), 0.001);
    }

    // Testet, ob bei negativen Beträgen eine Exception geworfen wird
    @Test
    void einzahlenUngueltig() {
        assertThrows(IllegalArgumentException.class, () -> {
            konto.einzahlen(new Geldbetrag(-50));
        });
    }

    @Test
    void einzahlenUngueltigBetrag() {
        assertThrows(IllegalArgumentException.class, () -> {
            konto.einzahlen(new Geldbetrag(-50));
        });
    }


}