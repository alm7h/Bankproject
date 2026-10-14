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
 * Testklasse für das {@link Festgeldkonto}: Abheben ist nur bis zur Höhe des zuvor
 * gekündigten Betrags erlaubt.
 */
class FestgeldkontoTest {

    private Festgeldkonto konto;
    private Kunde inhaber;

    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Mustermann", "Musterstraße 1", 1980, 5, 5);
        konto = new Festgeldkonto(inhaber, 12345678);
        konto.einzahlen(new Geldbetrag(1000));
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztWerte_undNichtsIstGekuendigt() {
            Festgeldkonto neu = new Festgeldkonto(inhaber, 4711);
            assertSame(inhaber, neu.getInhaber());
            assertEquals(4711, neu.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, neu.getKontostand());
            assertEquals(Geldbetrag.NULL_EURO, neu.getGekuendigterBetrag());
        }

        @Test
        void konstruktor_wirftNullPointerException_wennInhaberNull() {
            assertThrows(NullPointerException.class, () -> new Festgeldkonto(null, 4711));
        }

        @Test
        void konstruktor_wirftUngueltigeKontonummerException_beiUngueltigerNummer() {
            assertThrows(UngueltigeKontonummerException.class, () -> new Festgeldkonto(inhaber, 10_000_000_000L));
        }

        @Test
        void standardkonstruktor_erzeugtMustermannKonto() {
            Festgeldkonto standard = new Festgeldkonto();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(1234567, standard.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, standard.getGekuendigterBetrag());
        }
    }

    @Nested
    class KuendigenTests {
        @Test
        void kuendigen_merktSichDenBetrag() {
            konto.kuendigen(new Geldbetrag(300));
            assertEquals(new Geldbetrag(300), konto.getGekuendigterBetrag());
        }

        @Test
        void kuendigen_erlaubtGenauDenKontostand() {
            konto.kuendigen(new Geldbetrag(1000));
            assertEquals(new Geldbetrag(1000), konto.getGekuendigterBetrag());
        }

        @Test
        void kuendigen_begrenztAufDenKontostand() {
            konto.kuendigen(new Geldbetrag(1000.01));
            assertEquals(new Geldbetrag(1000), konto.getGekuendigterBetrag());
        }

        @Test
        void kuendigen_vergleichtFremdwaehrungInEuro() {
            // 61.500,01 MKD sind etwas mehr als 1000 € -> Begrenzung auf den Kontostand
            konto.kuendigen(new Geldbetrag(61500.01, Waehrung.DENAR));
            assertEquals(new Geldbetrag(1000), konto.getGekuendigterBetrag());
            konto.kuendigen(new Geldbetrag(6150, Waehrung.DENAR));
            assertEquals(new Geldbetrag(100), konto.getGekuendigterBetrag());
        }

        @Test
        void kuendigen_wirftIllegalArgumentException_beiNullOderNegativ_undLaesstBetragUnveraendert() {
            konto.kuendigen(new Geldbetrag(300));
            assertThrows(IllegalArgumentException.class, () -> konto.kuendigen(null));
            assertThrows(IllegalArgumentException.class, () -> konto.kuendigen(new Geldbetrag(-0.01)));
            assertEquals(new Geldbetrag(300), konto.getGekuendigterBetrag());
        }
    }

    @Nested
    class AbhebenTests {
        @Test
        void abheben_gibtFalse_ohneKuendigung_undKontostandBleibt() throws GesperrtException {
            assertFalse(konto.abheben(new Geldbetrag(0.01)));
            assertEquals(new Geldbetrag(1000), konto.getKontostand());
        }

        @Test
        void abheben_gibtTrue_beiGenauDemGekuendigtenBetrag() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(300));
            assertTrue(konto.abheben(new Geldbetrag(300)));
            assertEquals(new Geldbetrag(700), konto.getKontostand());
            assertEquals(Geldbetrag.NULL_EURO, konto.getGekuendigterBetrag());
        }

        @Test
        void abheben_gibtFalse_beiEinemCentUeberDemGekuendigtenBetrag() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(300));
            assertFalse(konto.abheben(new Geldbetrag(300.01)));
            assertEquals(new Geldbetrag(1000), konto.getKontostand());
            assertEquals(new Geldbetrag(300), konto.getGekuendigterBetrag(), "Abgelehnte Abhebung verbraucht nichts");
        }

        @Test
        void gekuendigterBetrag_sinktMitJederAbhebung() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(100));
            assertTrue(konto.abheben(new Geldbetrag(33.33)));
            assertTrue(konto.abheben(new Geldbetrag(33.33)));
            assertTrue(konto.abheben(new Geldbetrag(33.33)));
            assertEquals(new Geldbetrag(0.01), konto.getGekuendigterBetrag());
            assertFalse(konto.abheben(new Geldbetrag(0.02)));
            assertTrue(konto.abheben(new Geldbetrag(0.01)));
            assertEquals(new Geldbetrag(900), konto.getKontostand());
        }

        @Test
        void restDesGekuendigtenBetrags_kannCentgenauAbgehobenWerden() throws GesperrtException {
            // vor der Cent-Rundung in Geldbetrag blieben 0,09999999999999998 € übrig
            konto.kuendigen(new Geldbetrag(1));
            assertTrue(konto.abheben(new Geldbetrag(0.9)));
            assertTrue(konto.abheben(new Geldbetrag(0.1)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getGekuendigterBetrag());
        }

        @Test
        void derselbeGekuendigteBetrag_kannNichtZweimalAbgehobenWerden() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(200));
            assertTrue(konto.abheben(new Geldbetrag(200)));
            assertFalse(konto.abheben(new Geldbetrag(200)));
            assertEquals(new Geldbetrag(800), konto.getKontostand());
        }

        @Test
        void abheben_rechnetFremdwaehrungUm() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(10));
            assertFalse(konto.abheben(new Geldbetrag(615.01, Waehrung.DENAR)));
            assertTrue(konto.abheben(new Geldbetrag(615, Waehrung.DENAR)));
            assertEquals(new Geldbetrag(990), konto.getKontostand());
            assertEquals(Geldbetrag.NULL_EURO, konto.getGekuendigterBetrag());
        }

        @Test
        void kontostand_wirdNieNegativ_auchWennAllesGekuendigtUndAbgehobenWird() throws GesperrtException {
            konto.kuendigen(new Geldbetrag(5000));
            assertTrue(konto.abheben(new Geldbetrag(1000)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
            assertFalse(konto.abheben(new Geldbetrag(0.01)));
        }

        @Test
        void abheben_wirftGesperrtException_wennGesperrt_undGekuendigterBetragBleibt() {
            konto.kuendigen(new Geldbetrag(300));
            konto.sperren();
            assertThrows(GesperrtException.class, () -> konto.abheben(new Geldbetrag(100)));
            assertEquals(new Geldbetrag(1000), konto.getKontostand());
            assertEquals(new Geldbetrag(300), konto.getGekuendigterBetrag());
        }

        @Test
        void abheben_wirftIllegalArgumentException_beiNullOderNegativ() {
            assertThrows(IllegalArgumentException.class, () -> konto.abheben(null));
            assertThrows(IllegalArgumentException.class, () -> konto.abheben(new Geldbetrag(-1)));
        }
    }

    @Test
    void toString_enthaeltKontoartUndGekuendigtenBetrag() {
        String ausgabe = konto.toString();
        assertTrue(ausgabe.startsWith("-- FESTGELDKONTO --"), ausgabe);
        assertTrue(ausgabe.contains("Gekündigt: "), ausgabe);
    }

    @Test
    void serialisierung_bewahrtKontostandUndGekuendigtenBetrag() throws Exception {
        konto.kuendigen(new Geldbetrag(300));

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(konto);
        }
        Object gelesen;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            gelesen = in.readObject();
        }

        Festgeldkonto kopie = assertInstanceOf(Festgeldkonto.class, gelesen);
        assertEquals(new Geldbetrag(1000), kopie.getKontostand());
        assertEquals(new Geldbetrag(300), kopie.getGekuendigterBetrag());
        assertTrue(kopie.abheben(new Geldbetrag(300)));
    }
}
