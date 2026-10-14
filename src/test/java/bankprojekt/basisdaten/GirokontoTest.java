package bankprojekt.basisdaten;

import static org.junit.jupiter.api.Assertions.*;

import bankprojekt.exceptions.GesperrtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testklasse für das {@link Girokonto}.
 */
class GirokontoTest {

    private Girokonto konto;
    private Kunde inhaber;


    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Mustermann", "Musterstraße 1", "01.01.90");
        konto = new Girokonto(inhaber, 12345678, new Geldbetrag(500, Waehrung.EURO));
    }

    @Test
    void einzahlen() throws GesperrtException {
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

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztInhaberNummerUndDispo_undKontostandNull() {
            assertSame(inhaber, konto.getInhaber());
            assertEquals(12345678, konto.getKontonummer());
            assertEquals(new Geldbetrag(500), konto.getDispo());
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void konstruktor_wirftNullPointerException_wennInhaberNull() {
            assertThrows(NullPointerException.class, () -> new Girokonto(null, 1, new Geldbetrag(500)));
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_beiUngueltigemDispo() {
            assertThrows(IllegalArgumentException.class, () -> new Girokonto(inhaber, 1, null));
            assertThrows(IllegalArgumentException.class, () -> new Girokonto(inhaber, 1, new Geldbetrag(-0.01)));
            assertThrows(IllegalArgumentException.class, () -> new Girokonto(inhaber, 1, Geldbetrag.NULL_EURO));
        }

        @Test
        void standardkonstruktor_erzeugtMustermannKontoMit500EuroDispo() {
            Girokonto standard = new Girokonto();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(99887766, standard.getKontonummer());
            assertEquals(new Geldbetrag(500), standard.getDispo());
            assertFalse(standard.isGesperrt());
        }
    }

    @Nested
    class DispoTests {
        @Test
        void setDispo_setztGueltigenWert() {
            konto.setDispo(new Geldbetrag(1000));
            assertEquals(new Geldbetrag(1000), konto.getDispo());
        }

        @Test
        void setDispo_erlaubtKleinstenPositivenWert() {
            konto.setDispo(new Geldbetrag(0.01));
            assertEquals(new Geldbetrag(0.01), konto.getDispo());
        }

        @Test
        void setDispo_wirftIllegalArgumentException_beiNull_undLaesstDispoUnveraendert() {
            // Laut Dokumentation muss der Dispo größer als 0 sein – auch in Fremdwährung
            assertThrows(IllegalArgumentException.class, () -> konto.setDispo(Geldbetrag.NULL_EURO));
            assertThrows(IllegalArgumentException.class, () -> konto.setDispo(new Geldbetrag(0, Waehrung.DENAR)));
            assertEquals(new Geldbetrag(500), konto.getDispo());
        }

        @Test
        void setDispo_wirftIllegalArgumentException_beiNullOderNegativ_undLaesstDispoUnveraendert() {
            assertThrows(IllegalArgumentException.class, () -> konto.setDispo(null));
            assertThrows(IllegalArgumentException.class, () -> konto.setDispo(new Geldbetrag(-1)));
            assertEquals(new Geldbetrag(500), konto.getDispo());
        }
    }

    /**
     * Prüft die Einschubmethode {@code pruefeAbhebung} über die Template-Methode {@code abheben}.
     */
    @Nested
    class AbhebenTests {
        @Test
        void abheben_gibtTrue_wennGenauBisZumDispo() throws GesperrtException {
            assertTrue(konto.abheben(new Geldbetrag(500)));
            assertEquals(new Geldbetrag(-500), konto.getKontostand());
        }

        @Test
        void abheben_gibtFalse_wennEinCentUeberDispo_undKontostandBleibt() throws GesperrtException {
            assertFalse(konto.abheben(new Geldbetrag(500.01)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void abheben_beruecksichtigtGuthabenUndDispo() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(100));
            assertTrue(konto.abheben(new Geldbetrag(600)));
            assertEquals(new Geldbetrag(-500), konto.getKontostand());
            assertFalse(konto.abheben(new Geldbetrag(0.01)), "Dispo ist ausgeschöpft");
        }

        @Test
        void abheben_rechnetFremdwaehrungUm() throws GesperrtException {
            assertTrue(konto.abheben(new Geldbetrag(61.5, Waehrung.DENAR)));
            assertEquals(new Geldbetrag(-1), konto.getKontostand());
            assertEquals(Waehrung.EURO, konto.getKontostand().getWaehrung());
        }

        @Test
        void abheben_wirftGesperrtException_wennGesperrt() {
            konto.sperren();
            assertThrows(GesperrtException.class, () -> konto.abheben(new Geldbetrag(1)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }
    }

    @Nested
    class UeberweisungAbsendenTests {
        @Test
        void ueberweisungAbsenden_gibtTrue_undBuchtAb_genauBisZumDispo() throws GesperrtException {
            assertTrue(konto.ueberweisungAbsenden(new Geldbetrag(500), "Erika", 1, 2, "Miete"));
            assertEquals(new Geldbetrag(-500), konto.getKontostand());
        }

        @Test
        void ueberweisungAbsenden_gibtFalse_wennEinCentUeberDispo_undKontostandBleibt() throws GesperrtException {
            assertFalse(konto.ueberweisungAbsenden(new Geldbetrag(500.01), "Erika", 1, 2, "Miete"));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void ueberweisungAbsenden_wirftGesperrtException_wennGesperrt_auchVorDerParameterpruefung() {
            konto.sperren();
            assertThrows(GesperrtException.class,
                    () -> konto.ueberweisungAbsenden(new Geldbetrag(1), "Erika", 1, 2, "Miete"));
            // Die Sperre wird zuerst geprüft, daher keine IllegalArgumentException
            assertThrows(GesperrtException.class,
                    () -> konto.ueberweisungAbsenden(null, null, 1, 2, null));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void ueberweisungAbsenden_wirftIllegalArgumentException_beiUngueltigenParametern() {
            Geldbetrag eins = new Geldbetrag(1);
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungAbsenden(null, "Erika", 1, 2, "Miete"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungAbsenden(new Geldbetrag(-1), "Erika", 1, 2, "Miete"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungAbsenden(eins, null, 1, 2, "Miete"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungAbsenden(eins, "Erika", 1, 2, null));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }
    }

    @Nested
    class UeberweisungEmpfangenTests {
        @Test
        void ueberweisungEmpfangen_erhoehtKontostand() throws GesperrtException {
            konto.ueberweisungEmpfangen(new Geldbetrag(100), "Erika", 1, 2, "Geschenk");
            assertEquals(new Geldbetrag(100), konto.getKontostand());
        }

        @Test
        void ueberweisungEmpfangen_wirftGesperrtException_wennGesperrt_auchVorDerParameterpruefung() {
            konto.sperren();
            assertThrows(GesperrtException.class,
                    () -> konto.ueberweisungEmpfangen(new Geldbetrag(100), "Erika", 1, 2, "Geschenk"));
            // Die Sperre wird zuerst geprüft, daher keine IllegalArgumentException
            assertThrows(GesperrtException.class,
                    () -> konto.ueberweisungEmpfangen(null, null, 1, 2, null));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void ueberweisungEmpfangen_buchtWieder_nachEntsperren() throws GesperrtException {
            konto.sperren();
            konto.entsperren();
            konto.ueberweisungEmpfangen(new Geldbetrag(100), "Erika", 1, 2, "Geschenk");
            assertEquals(new Geldbetrag(100), konto.getKontostand());
        }

        @Test
        void ueberweisungEmpfangen_rechnetFremdwaehrungUm() throws GesperrtException {
            konto.ueberweisungEmpfangen(new Geldbetrag(61.5, Waehrung.DENAR), "Erika", 1, 2, "Geschenk");
            assertEquals(new Geldbetrag(1), konto.getKontostand());
            assertEquals(Waehrung.EURO, konto.getKontostand().getWaehrung());
        }

        @Test
        void ueberweisungEmpfangen_wirftIllegalArgumentException_beiUngueltigenParametern() {
            Geldbetrag eins = new Geldbetrag(1);
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungEmpfangen(null, "Erika", 1, 2, "Geschenk"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungEmpfangen(new Geldbetrag(-1), "Erika", 1, 2, "Geschenk"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungEmpfangen(eins, null, 1, 2, "Geschenk"));
            assertThrows(IllegalArgumentException.class,
                    () -> konto.ueberweisungEmpfangen(eins, "Erika", 1, 2, null));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }
    }

    @Nested
    class WaehrungswechselTests {
        @Test
        void waehrungswechsel_rechnetKontostandUndDispoUm() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(10));
            konto.waehrungswechsel(Waehrung.DENAR);

            assertEquals(Waehrung.DENAR, konto.getKontostand().getWaehrung());
            assertEquals(615.0, konto.getKontostand().getBetrag(), 0.001);
            assertEquals(Waehrung.DENAR, konto.getDispo().getWaehrung());
            assertEquals(30750.0, konto.getDispo().getBetrag(), 0.001);
        }

        @Test
        void abheben_haeltDispogrenzeAuchInNeuerWaehrungEin() throws GesperrtException {
            konto.waehrungswechsel(Waehrung.DENAR);
            assertFalse(konto.abheben(new Geldbetrag(30750.01, Waehrung.DENAR)));
            assertTrue(konto.abheben(new Geldbetrag(30750, Waehrung.DENAR)));
            assertEquals(-30750.0, konto.getKontostand().getBetrag(), 0.001);
        }
    }

    @Nested
    class AusgabeTests {
        @Test
        void toString_enthaeltKontoartUndDispo() {
            String ausgabe = konto.toString();
            assertTrue(ausgabe.startsWith("-- GIROKONTO --"), ausgabe);
            assertTrue(ausgabe.contains("Dispo: "), ausgabe);
        }
    }
}
