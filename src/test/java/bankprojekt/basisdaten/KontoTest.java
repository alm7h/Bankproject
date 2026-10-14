package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.exceptions.UngueltigeKontonummerException;
import javafx.beans.value.WritableValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die abstrakte Oberklasse {@link Konto}.
 * <p>
 * Da {@code Konto} abstrakt ist, wird über die kleine Testunterklasse {@link TestKonto}
 * getestet. Sie lässt sich so einstellen, dass {@code pruefeAbhebung} die Abhebung erlaubt
 * oder ablehnt, und protokolliert die Aufrufe der beiden Einschubmethoden. Dadurch kann
 * die Template-Methode {@link Konto#abheben(Geldbetrag)} unabhängig von den Regeln einer
 * konkreten Kontoart geprüft werden.
 */
class KontoTest {

    /**
     * Konkrete Testunterklasse von Konto, die die Aufrufe der Einschubmethoden protokolliert.
     */
    static class TestKonto extends Konto {
        /**
         * Rückgabewert von pruefeAbhebung
         */
        boolean erlaubt = true;

        /**
         * Protokoll der Aufrufe der Einschubmethoden (in Aufrufreihenfolge)
         */
        final List<String> protokoll = new ArrayList<>();

        /**
         * Kontostand, den nachAbhebung beim Aufruf vorgefunden hat
         */
        Geldbetrag standBeiNachAbhebung;

        TestKonto(Kunde inhaber, long kontonummer) {
            super(inhaber, kontonummer);
        }

        TestKonto() {
            super();
        }

        @Override
        protected boolean pruefeAbhebung(Geldbetrag betrag) {
            protokoll.add("pruefeAbhebung " + betrag.getBetrag());
            return erlaubt;
        }

        @Override
        protected void nachAbhebung(Geldbetrag betrag) {
            protokoll.add("nachAbhebung " + betrag.getBetrag());
            standBeiNachAbhebung = getKontostand();
        }
    }

    private Kunde inhaber;
    private TestKonto konto;

    @BeforeEach
    void setUp() {
        inhaber = new Kunde("Max", "Müller", "Weg 1", LocalDate.of(2000, 1, 1));
        konto = new TestKonto(inhaber, 1234567);
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztInhaberUndNummer_undStartetLeerUndEntsperrt() {
            assertSame(inhaber, konto.getInhaber());
            assertEquals(1234567, konto.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
            assertFalse(konto.isGesperrt());
            assertTrue(konto.isImPlus());
        }

        @Test
        void standardkonstruktor_nutztMustermannUndStandardnummer() {
            TestKonto standard = new TestKonto();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(1234567, standard.getKontonummer());
        }

        @Test
        void konstruktor_wirftNullPointerException_wennInhaberNull() {
            assertThrows(NullPointerException.class, () -> new TestKonto(null, 1));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, 9_999_999_999L})
        void konstruktor_akzeptiertGrenzwerteDerKontonummer(long nummer) {
            assertEquals(nummer, new TestKonto(inhaber, nummer).getKontonummer());
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 10_000_000_000L, Long.MIN_VALUE, Long.MAX_VALUE})
        void konstruktor_wirftUngueltigeKontonummerException_ausserhalbDesBereichs(long nummer) {
            assertThrows(UngueltigeKontonummerException.class, () -> new TestKonto(inhaber, nummer));
        }
    }

    @Nested
    class EinzahlenTests {
        @Test
        void einzahlen_erhoehtKontostand() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(100));
            konto.einzahlen(new Geldbetrag(23.5));
            assertEquals(123.5, konto.getKontostand().getBetrag(), 0.0001);
        }

        @Test
        void einzahlen_vonNullEuro_istErlaubt() throws GesperrtException {
            konto.einzahlen(Geldbetrag.NULL_EURO);
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void einzahlen_inFremdwaehrung_wirdInKontowaehrungUmgerechnet() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(61.5, Waehrung.DENAR));
            assertEquals(1.0, konto.getKontostand().getBetrag(), 0.0001);
            assertEquals(Waehrung.EURO, konto.getKontostand().getWaehrung());
        }

        @Test
        void einzahlen_wirftIllegalArgumentException_beiNullOderNegativ() {
            assertThrows(IllegalArgumentException.class, () -> konto.einzahlen(null));
            assertThrows(IllegalArgumentException.class, () -> konto.einzahlen(new Geldbetrag(-1)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }

        @Test
        void einzahlen_wirftGesperrtException_wennKontoGesperrt_undKontostandBleibt() {
            // Ein gesperrtes Konto lässt keine Buchungen zu - auch keine Einzahlung
            konto.sperren();
            assertThrows(GesperrtException.class, () -> konto.einzahlen(new Geldbetrag(10)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
        }
    }

    /**
     * Tests der Template-Methode abheben: Ablauf Prüfung der Parameter -> Prüfung der Sperre
     * -> pruefeAbhebung -> Abbuchung -> nachAbhebung.
     */
    @Nested
    class AbhebenTemplateMethodeTests {
        @BeforeEach
        void guthaben() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(100));
        }

        @Test
        void abheben_istFinalUndSynchronized() throws NoSuchMethodException {
            // Unterklassen dürfen den Ablauf nicht überschreiben (Template-Methode)
            Method abheben = Konto.class.getMethod("abheben", Geldbetrag.class);
            assertTrue(Modifier.isFinal(abheben.getModifiers()), "abheben muss final sein");
            assertTrue(Modifier.isSynchronized(abheben.getModifiers()), "abheben muss synchronized sein");
        }

        @Test
        void abheben_erlaubt_buchtAb_undRuftEinschubmethodenInRichtigerReihenfolge() throws GesperrtException {
            assertTrue(konto.abheben(new Geldbetrag(30)));

            assertEquals(new Geldbetrag(70), konto.getKontostand());
            assertEquals(List.of("pruefeAbhebung 30.0", "nachAbhebung 30.0"), konto.protokoll);
            assertEquals(new Geldbetrag(70), konto.standBeiNachAbhebung,
                    "nachAbhebung muss nach der Abbuchung aufgerufen werden");
        }

        @Test
        void abheben_abgelehnt_liefertFalse_ohneAbbuchungUndOhneNachAbhebung() throws GesperrtException {
            konto.erlaubt = false;

            assertFalse(konto.abheben(new Geldbetrag(30)));

            assertEquals(new Geldbetrag(100), konto.getKontostand());
            assertEquals(List.of("pruefeAbhebung 30.0"), konto.protokoll);
        }

        @Test
        void abheben_gesperrt_wirftGesperrtException_ohneEinschubmethoden() {
            konto.sperren();

            GesperrtException e = assertThrows(GesperrtException.class, () -> konto.abheben(new Geldbetrag(30)));

            assertTrue(e.getMessage().contains("1234567"), "Meldung soll die Kontonummer enthalten");
            assertEquals(new Geldbetrag(100), konto.getKontostand());
            assertTrue(konto.protokoll.isEmpty());
        }

        @Test
        void abheben_ungueltigerBetrag_wirftIllegalArgumentException_ohneEinschubmethoden() {
            assertThrows(IllegalArgumentException.class, () -> konto.abheben(null));
            assertThrows(IllegalArgumentException.class, () -> konto.abheben(new Geldbetrag(-1)));
            assertEquals(new Geldbetrag(100), konto.getKontostand());
            assertTrue(konto.protokoll.isEmpty());
        }

        @Test
        void abheben_prueftBetragVorDerSperre() {
            // Reihenfolge der Template-Methode: erst Parameter, dann Sperre
            konto.sperren();
            assertThrows(IllegalArgumentException.class, () -> konto.abheben(null));
        }

        @Test
        void abheben_nachEntsperrenWiederMoeglich() throws GesperrtException {
            konto.sperren();
            konto.entsperren();
            assertTrue(konto.abheben(new Geldbetrag(10)));
            assertEquals(new Geldbetrag(90), konto.getKontostand());
        }
    }

    @Nested
    class SperreUndInhaberTests {
        @Test
        void sperrenUndEntsperren_aendernZustandUndText() {
            assertEquals("", konto.getGesperrtText());
            konto.sperren();
            assertTrue(konto.isGesperrt());
            assertEquals("GESPERRT", konto.getGesperrtText());
            konto.entsperren();
            assertFalse(konto.isGesperrt());
            assertEquals("", konto.getGesperrtText());
        }

        @Test
        void setInhaber_wechseltInhaber() throws GesperrtException {
            Kunde neu = new Kunde("Erika", "Muster", "Weg 2", LocalDate.of(1990, 2, 2));
            konto.setInhaber(neu);
            assertSame(neu, konto.getInhaber());
        }

        @Test
        void setInhaber_wirftNullPointerException_beiNull() {
            assertThrows(NullPointerException.class, () -> konto.setInhaber(null));
            assertSame(inhaber, konto.getInhaber());
        }

        @Test
        void setInhaber_wirftGesperrtException_wennGesperrt() {
            konto.sperren();
            Kunde neu = new Kunde("Erika", "Muster", "Weg 2", LocalDate.of(1990, 2, 2));
            assertThrows(GesperrtException.class, () -> konto.setInhaber(neu));
            assertSame(inhaber, konto.getInhaber());
        }
    }

    @Nested
    class FormatierungTests {
        @Test
        void getKontonummerFormatiert_rechtsbuendigAufZehnStellen() {
            assertEquals("   1234567", konto.getKontonummerFormatiert());
        }

        @Test
        void getIban_liefertBekannteGueltigeIban() {
            // Bekanntes Beispiel: BLZ 370 400 44, Konto 0532013000 -> DE89 3704 0044 0532 0130 00
            TestKonto beispiel = new TestKonto(inhaber, 532013000L);
            assertEquals("DE89370400440532013000", beispiel.getIban(37040044L));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, 10020030L, 99999999L})
        void getIban_bestehtDiePruefsummenkontrolle(long blz) {
            String iban = konto.getIban(blz);
            assertEquals(22, iban.length());
            assertTrue(iban.startsWith("DE"));
            // Prüfung nach ISO 13616: BBAN + "DE" als Zahl (1314) + Prüfziffern, mod 97 == 1
            String umgestellt = iban.substring(4) + "1314" + iban.substring(2, 4);
            assertEquals(1, new BigInteger(umgestellt).mod(BigInteger.valueOf(97)).intValue());
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 100_000_000L})
        void getIban_liefertLeerenString_beiUngueltigerBlz(long blz) {
            assertEquals("", konto.getIban(blz));
        }

        @Test
        void toString_enthaeltNummerInhaberUndSperrvermerk() {
            String text = konto.toString();
            assertTrue(text.contains("1234567"));
            assertTrue(text.contains(inhaber.toString()));
            assertFalse(text.contains("GESPERRT"));
            konto.sperren();
            assertTrue(konto.toString().contains("GESPERRT"));
        }
    }

    @Nested
    class VergleichTests {
        @Test
        void equals_gleicheNummer_auchBeiAnderemInhaberUndKontotyp() {
            Kunde anderer = new Kunde("Erika", "Muster", "Weg 2", LocalDate.of(1990, 2, 2));
            assertEquals(konto, new TestKonto(anderer, 1234567));
            assertEquals(konto, new Girokonto(anderer, 1234567, new Geldbetrag(100)));
            assertEquals(konto.hashCode(), new TestKonto(anderer, 1234567).hashCode());
        }

        @Test
        void equals_trueFuerDasselbeObjekt() {
            assertTrue(konto.equals(konto));
        }

        @Test
        void equals_falseBeiAndererNummerNullUndFremdemTyp() {
            // equals direkt aufrufen: assertNotEquals(null, x) bzw. assertNotEquals("…", x)
            // würde nicht Konto.equals, sondern nur die Nullprüfung bzw. String.equals ausführen
            assertFalse(konto.equals(new TestKonto(inhaber, 7654321)));
            assertFalse(konto.equals(null));
            assertFalse(konto.equals("1234567"));
        }

        @Test
        void compareTo_sortiertNachKontonummer() {
            TestKonto kleiner = new TestKonto(inhaber, 1);
            assertTrue(kleiner.compareTo(konto) < 0);
            assertTrue(konto.compareTo(kleiner) > 0);
            assertEquals(0, konto.compareTo(new TestKonto(inhaber, 1234567)));
        }
    }

    @Nested
    class WaehrungswechselTests {
        @Test
        void waehrungswechsel_rechnetKontostandUm() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(10));
            konto.waehrungswechsel(Waehrung.DENAR);
            assertEquals(615.0, konto.getKontostand().getBetrag(), 0.0001);
            assertEquals(Waehrung.DENAR, konto.getKontostand().getWaehrung());
        }

        @Test
        void waehrungswechsel_wirftNullPointerException_beiNull() {
            assertThrows(NullPointerException.class, () -> konto.waehrungswechsel(null));
        }
    }

    @Nested
    class KontostandSetzenTests {
        @Test
        void setKontostand_ignoriertNull() throws GesperrtException {
            konto.einzahlen(new Geldbetrag(5));
            konto.setKontostand(null);
            assertEquals(new Geldbetrag(5), konto.getKontostand());
        }
    }

    /**
     * Tests der JavaFX-Properties. Sie funktionieren ohne gestartetes JavaFX-Toolkit.
     */
    @Nested
    class PropertyTests {
        @Test
        void kontostandProperty_meldetAenderungen() throws GesperrtException {
            List<Geldbetrag> neueWerte = new ArrayList<>();
            konto.kontostandProperty().addListener((obs, alt, neu) -> neueWerte.add(neu));

            konto.einzahlen(new Geldbetrag(10));

            assertEquals(List.of(new Geldbetrag(10)), neueWerte);
            assertEquals(new Geldbetrag(10), konto.kontostandProperty().get());
        }

        @Test
        void kontostandProperty_istVonAussenNichtBeschreibbar() {
            assertFalse(konto.kontostandProperty() instanceof WritableValue,
                    "Kontostand darf über die Property nicht verändert werden können");
        }

        @Test
        void imPlusProperty_folgtDemKontostand() throws GesperrtException {
            assertTrue(konto.imPlusProperty().get());

            konto.abheben(new Geldbetrag(0.01));   // TestKonto erlaubt das Überziehen
            assertFalse(konto.isImPlus());
            assertFalse(konto.imPlusProperty().get());

            konto.einzahlen(new Geldbetrag(0.01));
            assertTrue(konto.isImPlus(), "Kontostand 0 zählt als im Plus");
        }

        @Test
        void gesperrtProperty_istMitSperreGekoppelt() {
            konto.sperren();
            assertTrue(konto.gesperrtProperty().get());

            konto.gesperrtProperty().set(false);
            assertFalse(konto.isGesperrt());
        }
    }

    @Nested
    class SerialisierungTests {
        private TestKonto rundreise(TestKonto original) throws Exception {
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
                out.writeObject(original);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
                return (TestKonto) in.readObject();
            }
        }

        @Test
        void serialisierung_erhaeltNummerInhaberKontostandUndSperre() throws Exception {
            konto.einzahlen(new Geldbetrag(42.5));
            konto.sperren();

            TestKonto gelesen = rundreise(konto);

            assertEquals(1234567, gelesen.getKontonummer());
            assertEquals(inhaber, gelesen.getInhaber());
            assertEquals(new Geldbetrag(42.5), gelesen.getKontostand());
            assertTrue(gelesen.isGesperrt());
        }

        @Test
        void serialisierung_bautFunktionierendePropertiesWiederAuf() throws Exception {
            TestKonto gelesen = rundreise(konto);

            assertNotNull(gelesen.kontostandProperty());
            assertNotNull(gelesen.gesperrtProperty());
            assertTrue(gelesen.isImPlus());

            // Die Bindung von imPlus an den Kontostand muss nach dem Einlesen weiter wirken
            gelesen.abheben(new Geldbetrag(1));
            assertFalse(gelesen.isImPlus());
        }
    }
}
