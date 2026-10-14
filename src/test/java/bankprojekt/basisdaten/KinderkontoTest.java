package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.exceptions.UngueltigeKontonummerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das {@link Kinderkonto}: höchstens 50 € pro Abhebung, kein Überziehen.
 */
class KinderkontoTest {

    private Kinderkonto konto;
    private Kunde inhaber;

    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Mia", "Klein", "Spielweg 3", 2015, 6, 1);
        konto = new Kinderkonto(inhaber, 12345678);
        konto.einzahlen(new Geldbetrag(200));
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztInhaberUndNummer_undKontostandNull() {
            Kinderkonto neu = new Kinderkonto(inhaber, 4711);
            assertSame(inhaber, neu.getInhaber());
            assertEquals(4711, neu.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, neu.getKontostand());
            assertFalse(neu.isGesperrt());
        }

        @Test
        void konstruktor_wirftNullPointerException_wennInhaberNull() {
            assertThrows(NullPointerException.class, () -> new Kinderkonto(null, 4711));
        }

        @Test
        void konstruktor_wirftUngueltigeKontonummerException_beiUngueltigerNummer() {
            assertThrows(UngueltigeKontonummerException.class, () -> new Kinderkonto(inhaber, -1));
        }

        @Test
        void standardkonstruktor_erzeugtMustermannKonto() {
            Kinderkonto standard = new Kinderkonto();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(1234567, standard.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, standard.getKontostand());
        }
    }

    @Nested
    class HoechstbetragTests {
        @Test
        void abheben_gibtTrue_beiGenau50Euro() throws GesperrtException {
            assertTrue(konto.abheben(Kinderkonto.MAX_ABHEBUNG));
            assertEquals(new Geldbetrag(150), konto.getKontostand());
        }

        @Test
        void abheben_gibtFalse_beiEinemCentUeber50Euro_undKontostandBleibt() throws GesperrtException {
            assertFalse(konto.abheben(new Geldbetrag(50.01)));
            assertEquals(new Geldbetrag(200), konto.getKontostand());
        }

        @Test
        void hoechstbetrag_giltProAbhebung_undNichtInSumme() throws GesperrtException {
            assertTrue(konto.abheben(new Geldbetrag(50)));
            assertTrue(konto.abheben(new Geldbetrag(50)));
            assertTrue(konto.abheben(new Geldbetrag(50)));
            assertEquals(new Geldbetrag(50), konto.getKontostand());
        }

        @Test
        void hoechstbetrag_wirdInEuroVerglichen() throws GesperrtException {
            // 3075 MKD entsprechen genau 50 €
            assertFalse(konto.abheben(new Geldbetrag(3075.01, Waehrung.DENAR)));
            assertTrue(konto.abheben(new Geldbetrag(3075, Waehrung.DENAR)));
            assertEquals(new Geldbetrag(150), konto.getKontostand());
        }

        @Test
        void hoechstbetrag_giltAuchNachWaehrungswechsel() throws GesperrtException {
            konto.waehrungswechsel(Waehrung.DENAR);
            assertEquals(12300.0, konto.getKontostand().getBetrag(), 0.001);
            assertFalse(konto.abheben(new Geldbetrag(3075.01, Waehrung.DENAR)));
            assertTrue(konto.abheben(new Geldbetrag(3075, Waehrung.DENAR)));
            assertEquals(9225.0, konto.getKontostand().getBetrag(), 0.001);
            assertEquals(Waehrung.DENAR, konto.getKontostand().getWaehrung());
        }
    }

    @Nested
    class UeberziehenTests {
        @Test
        void abheben_gibtTrue_wennKontostandGenauNullWird() throws GesperrtException {
            Kinderkonto knapp = new Kinderkonto(inhaber, 4711);
            knapp.einzahlen(new Geldbetrag(30));
            assertTrue(knapp.abheben(new Geldbetrag(30)));
            assertEquals(Geldbetrag.NULL_EURO, knapp.getKontostand());
        }

        @Test
        void abheben_gibtFalse_wennEinCentFehlt_undKontostandBleibt() throws GesperrtException {
            Kinderkonto knapp = new Kinderkonto(inhaber, 4711);
            knapp.einzahlen(new Geldbetrag(30));
            assertFalse(knapp.abheben(new Geldbetrag(30.01)));
            assertEquals(new Geldbetrag(30), knapp.getKontostand());
        }

        @Test
        void restguthaben_kannCentgenauAbgehobenWerden() throws GesperrtException {
            // vor der Cent-Rundung in Geldbetrag blieben 0,09999999999999998 € übrig,
            // und die Abhebung der restlichen 0,10 € wurde als Überziehung abgelehnt
            Kinderkonto knapp = new Kinderkonto(inhaber, 4711);
            knapp.einzahlen(new Geldbetrag(1));
            assertTrue(knapp.abheben(new Geldbetrag(0.9)));
            assertTrue(knapp.abheben(new Geldbetrag(0.1)));
            assertEquals(Geldbetrag.NULL_EURO, knapp.getKontostand());
        }

        @Test
        void abheben_gibtFalse_beiLeeremKonto() throws GesperrtException {
            Kinderkonto leer = new Kinderkonto(inhaber, 4711);
            assertFalse(leer.abheben(new Geldbetrag(0.01)), "Kein Dispo beim Kinderkonto");
        }
    }

    @Test
    void abheben_wirftGesperrtException_wennGesperrt() {
        konto.sperren();
        assertThrows(GesperrtException.class, () -> konto.abheben(new Geldbetrag(10)));
        assertEquals(new Geldbetrag(200), konto.getKontostand());
    }

    @Test
    void abheben_wirftIllegalArgumentException_beiNullOderNegativ() {
        assertThrows(IllegalArgumentException.class, () -> konto.abheben(null));
        assertThrows(IllegalArgumentException.class, () -> konto.abheben(new Geldbetrag(-1)));
    }

    @Test
    void toString_beginntMitKontoart() {
        String ausgabe = konto.toString();
        assertTrue(ausgabe.startsWith("-- KINDERKONTO --"), ausgabe);
    }

    @Test
    void serialisierung_bewahrtTypKontostandUndSperre() throws Exception {
        konto.sperren();

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(konto);
        }
        Object gelesen;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            gelesen = in.readObject();
        }

        Kinderkonto kopie = assertInstanceOf(Kinderkonto.class, gelesen);
        assertEquals(new Geldbetrag(200), kopie.getKontostand());
        assertTrue(kopie.isGesperrt());
        assertEquals(konto, kopie);
    }
}
