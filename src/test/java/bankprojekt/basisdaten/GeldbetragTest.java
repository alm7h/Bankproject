package bankprojekt.basisdaten;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für den unveränderlichen Werttyp {@link Geldbetrag}.
 */
class GeldbetragTest {

    /**
     * Toleranz für Vergleiche von double-Beträgen.
     */
    private static final double DELTA = 0.0001;

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktorOhneWaehrung_erzeugtEuroBetrag() {
            Geldbetrag g = new Geldbetrag(12.34);
            assertEquals(12.34, g.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, g.getWaehrung());
        }

        @Test
        void konstruktorMitWaehrung_uebernimmtBetragUndWaehrung() {
            Geldbetrag g = new Geldbetrag(61.5, Waehrung.DENAR);
            assertEquals(61.5, g.getBetrag(), DELTA);
            assertEquals(Waehrung.DENAR, g.getWaehrung());
        }

        @Test
        void konstruktor_erlaubtNegativeBetraege() {
            Geldbetrag g = new Geldbetrag(-5);
            assertEquals(-5, g.getBetrag(), DELTA);
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void konstruktorOhneWaehrung_wirftIllegalArgumentException_beiNichtFinitemBetrag(double betrag) {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(betrag));
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void konstruktorMitWaehrung_wirftIllegalArgumentException_beiNichtFinitemBetrag(double betrag) {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(betrag, Waehrung.EURO));
        }

        @Test
        void konstruktorMitWaehrung_wirftIllegalArgumentException_wennWaehrungNull() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1, null));
        }

        @Test
        void nullEuro_istNullInEuro() {
            assertEquals(0.0, Geldbetrag.NULL_EURO.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, Geldbetrag.NULL_EURO.getWaehrung());
        }
    }

    @Nested
    class IsNegativTests {
        @Test
        void isNegativ_trueBeiNegativemBetrag() {
            assertTrue(new Geldbetrag(-0.01).isNegativ());
        }

        @Test
        void isNegativ_falseBeiNull() {
            assertFalse(Geldbetrag.NULL_EURO.isNegativ());
        }

        @Test
        void isNegativ_falseBeiPositivemBetrag() {
            assertFalse(new Geldbetrag(0.01).isNegativ());
        }
    }

    @Nested
    class PlusTests {
        @Test
        void plus_addiertGleicheWaehrung() {
            Geldbetrag summe = new Geldbetrag(10).plus(new Geldbetrag(5.5));
            assertEquals(15.5, summe.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, summe.getWaehrung());
        }

        @Test
        void plus_rechnetSummandenInWaehrungVonThisUm() {
            // 61,5 MKD entsprechen genau 1 €
            Geldbetrag summe = new Geldbetrag(1).plus(new Geldbetrag(61.5, Waehrung.DENAR));
            assertEquals(2.0, summe.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, summe.getWaehrung());
        }

        @Test
        void plus_ergebnisHatWaehrungVonThis() {
            Geldbetrag summe = new Geldbetrag(61.5, Waehrung.DENAR).plus(new Geldbetrag(1));
            assertEquals(123.0, summe.getBetrag(), DELTA);
            assertEquals(Waehrung.DENAR, summe.getWaehrung());
        }

        @Test
        void plus_wirftIllegalArgumentException_beiNull() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).plus(null));
        }

        @Test
        void plus_wirftIllegalArgumentException_beiNegativemSummanden() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).plus(new Geldbetrag(-1)));
        }

        @Test
        void plus_veraendertOperandenNicht() {
            Geldbetrag a = new Geldbetrag(10);
            Geldbetrag b = new Geldbetrag(5);
            Geldbetrag summe = a.plus(b);
            assertNotSame(a, summe);
            assertEquals(10, a.getBetrag(), DELTA);
            assertEquals(5, b.getBetrag(), DELTA);
        }
    }

    @Nested
    class MinusTests {
        @Test
        void minus_subtrahiertGleicheWaehrung() {
            Geldbetrag differenz = new Geldbetrag(10).minus(new Geldbetrag(2.5));
            assertEquals(7.5, differenz.getBetrag(), DELTA);
        }

        @Test
        void minus_darfNegativesErgebnisLiefern() {
            Geldbetrag differenz = new Geldbetrag(1).minus(new Geldbetrag(3));
            assertEquals(-2, differenz.getBetrag(), DELTA);
            assertTrue(differenz.isNegativ());
        }

        @Test
        void minus_rechnetSubtrahendenInWaehrungVonThisUm() {
            Geldbetrag differenz = new Geldbetrag(10).minus(new Geldbetrag(61.5, Waehrung.DENAR));
            assertEquals(9.0, differenz.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, differenz.getWaehrung());
        }

        @Test
        void minus_wirftIllegalArgumentException_beiNull() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).minus(null));
        }

        @Test
        void minus_wirftIllegalArgumentException_beiNegativemSubtrahenden() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).minus(new Geldbetrag(-1)));
        }

        @Test
        void minus_veraendertOperandenNicht() {
            Geldbetrag a = new Geldbetrag(10);
            a.minus(new Geldbetrag(4));
            assertEquals(10, a.getBetrag(), DELTA);
        }
    }

    @Nested
    class MalTests {
        @Test
        void mal_multipliziertBetragUndBehaeltWaehrung() {
            Geldbetrag ergebnis = new Geldbetrag(2.5, Waehrung.DOBRA).mal(4);
            assertEquals(10, ergebnis.getBetrag(), DELTA);
            assertEquals(Waehrung.DOBRA, ergebnis.getWaehrung());
        }

        @Test
        void mal_mitNegativemFaktor_liefertNegativenBetrag() {
            assertTrue(new Geldbetrag(3).mal(-1).isNegativ());
        }

        @Test
        void mal_mitNull_liefertNull() {
            assertEquals(0, new Geldbetrag(3).mal(0).getBetrag(), DELTA);
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void mal_wirftIllegalArgumentException_beiNichtFinitemFaktor(double faktor) {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).mal(faktor));
        }

        @Test
        void mal_veraendertThisNicht() {
            Geldbetrag a = new Geldbetrag(3);
            a.mal(10);
            assertEquals(3, a.getBetrag(), DELTA);
        }
    }

    /**
     * Ergebnisse von plus, minus und mal werden auf Cent gerundet, damit sich keine
     * double-Rundungsfehler über mehrere Buchungen aufsummieren. Hier wird bewusst
     * exakt (ohne DELTA) verglichen.
     */
    @Nested
    class CentRundungTests {
        @Test
        void plus_liefertExaktenCentbetrag() {
            // ohne Rundung: 0.30000000000000004
            assertEquals(0.3, new Geldbetrag(0.1).plus(new Geldbetrag(0.2)).getBetrag());
            assertEquals(new Geldbetrag(0.3), new Geldbetrag(0.1).plus(new Geldbetrag(0.2)));
        }

        @Test
        void minus_liefertExaktenCentbetrag() {
            // ohne Rundung: 0.09999999999999998
            assertEquals(0.1, new Geldbetrag(1).minus(new Geldbetrag(0.9)).getBetrag());
        }

        @Test
        void mehrereBuchungen_summierenKeinenFehlerAuf() {
            Geldbetrag rest = new Geldbetrag(100);
            for (int i = 0; i < 3; i++) {
                rest = rest.minus(new Geldbetrag(33.33));
            }
            assertEquals(0.01, rest.getBetrag());
        }

        @Test
        void mal_rundetAufCent() {
            assertEquals(3.33, new Geldbetrag(10).mal(1.0 / 3).getBetrag());
            assertEquals(0.67, new Geldbetrag(2).mal(1.0 / 3).getBetrag());
            assertEquals(-3.33, new Geldbetrag(10).mal(-1.0 / 3).getBetrag());
        }
    }

    /**
     * Hält Ergebnisse fest, die sich bei der Umstellung des Betrags von double auf
     * BigDecimal nicht ändern dürfen. Die Beträge werden wie in der Oberfläche aus ganzen
     * Cent gebildet. Erwartet wird jeweils der exakte Wert, mit BigDecimal berechnet und
     * kaufmännisch (HALF_UP) auf Cent gerundet. Gemeldet werden die ersten Abweichungen.
     */
    @Nested
    class ExakteErgebnisseTests {
        /** obere Grenze der Raster in Cent (1.000,00) */
        private static final int MAX_CENT = 100_000;

        /** so viele Abweichungen werden höchstens gemeldet */
        private static final int MAX_MELDUNGEN = 5;

        @Test
        void plusUndMinus_ergebenFuerCentbetraegeDenExaktenWert() {
            List<String> abweichungen = new ArrayList<>();
            for (int a = -MAX_CENT; a <= MAX_CENT; a += 211) {
                for (int b = 0; b <= MAX_CENT; b += 97) {
                    Geldbetrag x = new Geldbetrag(a / 100.0);
                    Geldbetrag y = new Geldbetrag(b / 100.0);
                    double summe = x.plus(y).getBetrag();
                    double differenz = x.minus(y).getBetrag();
                    if (summe != (a + b) / 100.0 && abweichungen.size() < MAX_MELDUNGEN)
                        abweichungen.add(a + " + " + b + " Cent ergibt " + summe);
                    if (differenz != (a - b) / 100.0 && abweichungen.size() < MAX_MELDUNGEN)
                        abweichungen.add(a + " - " + b + " Cent ergibt " + differenz);
                }
            }
            assertEquals(List.of(), abweichungen);
        }

        @Test
        void mal_mitGanzzahligemFaktor_ergibtDenExaktenWert() {
            List<String> abweichungen = new ArrayList<>();
            for (int cent = -MAX_CENT; cent <= MAX_CENT; cent += 113) {
                Geldbetrag betrag = new Geldbetrag(cent / 100.0);
                for (int faktor = 0; faktor <= 100; faktor++) {
                    double produkt = betrag.mal(faktor).getBetrag();
                    if (produkt != cent * faktor / 100.0 && abweichungen.size() < MAX_MELDUNGEN)
                        abweichungen.add(cent + " Cent mal " + faktor + " ergibt " + produkt);
                }
            }
            assertEquals(List.of(), abweichungen);
        }

        /**
         * In diesen Richtungen liegt das exakte Ergebnis nie genau auf einem halben Cent,
         * daher ergibt jeder Betrag von 0,01 bis 1.000,00 den exakten, kaufmännisch
         * gerundeten Wert.
         * @param von Währung des Ausgangsbetrags
         * @param nach Zielwährung
         */
        @ParameterizedTest
        @CsvSource({
                "DENAR, EURO", "DOBRA, EURO", "FRANC, EURO",
                "FRANC, DENAR", "FRANC, DOBRA", "DENAR, DOBRA", "DOBRA, DENAR"
        })
        void umrechnen_ohneHalbeCent_ergibtDenExaktenWert(Waehrung von, Waehrung nach) {
            BigDecimal kursVon = BigDecimal.valueOf(von.getUmrechnungskursZuEuro());
            BigDecimal kursNach = BigDecimal.valueOf(nach.getUmrechnungskursZuEuro());
            List<String> abweichungen = new ArrayList<>();
            for (int cent = 1; cent <= MAX_CENT; cent++) {
                double erwartet = BigDecimal.valueOf(cent, 2).multiply(kursNach)
                        .divide(kursVon, 2, RoundingMode.HALF_UP).doubleValue();
                double ist = new Geldbetrag(cent / 100.0, von).umrechnen(nach).getBetrag();
                if (ist != erwartet && abweichungen.size() < MAX_MELDUNGEN)
                    abweichungen.add(cent + " Cent " + von + " ergeben " + ist + " " + nach + " statt " + erwartet);
            }
            assertEquals(List.of(), abweichungen);
        }

        /**
         * Von Euro in Denar (Kurs 61,5) und Dobra (Kurs 24,5) ergibt ein gerader
         * Centbetrag genau zwei Nachkommastellen, es muss also nichts gerundet werden.
         * Ungerade Centbeträge enden dagegen auf einen halben Cent; die double-Rechnung
         * rundet sie je nach Binärdarstellung mal ab und mal auf, deshalb werden sie hier
         * bewusst nicht festgehalten.
         * @param nach Zielwährung
         */
        @ParameterizedTest
        @EnumSource(value = Waehrung.class, names = {"DENAR", "DOBRA"})
        void umrechnen_vonEuroBeiGeradenCentbetraegen_ergibtDenExaktenWert(Waehrung nach) {
            BigDecimal kurs = BigDecimal.valueOf(nach.getUmrechnungskursZuEuro());
            List<String> abweichungen = new ArrayList<>();
            for (int cent = 2; cent <= MAX_CENT; cent += 2) {
                double erwartet = BigDecimal.valueOf(cent, 2).multiply(kurs).doubleValue();
                double ist = new Geldbetrag(cent / 100.0).umrechnen(nach).getBetrag();
                if (ist != erwartet && abweichungen.size() < MAX_MELDUNGEN)
                    abweichungen.add(cent + " Cent EUR ergeben " + ist + " " + nach + " statt " + erwartet);
            }
            assertEquals(List.of(), abweichungen);
        }

        /**
         * Der Konstruktor rundet nicht: getBetrag liefert genau den übergebenen Wert,
         * auch mit mehr als zwei Nachkommastellen (z. B. für den ungerundeten Aktienkurs).
         * @param wert der übergebene Betrag
         */
        @ParameterizedTest
        @ValueSource(doubles = {0.0, 12.34, -5.0, -0.01, 1234.567, 0.30000000000000004, 0.00001,
                123456789.12, 491.96775})
        void getBetrag_liefertGenauDenUebergebenenWert(double wert) {
            assertEquals(wert, new Geldbetrag(wert).getBetrag());
            assertEquals(wert, new Geldbetrag(wert, Waehrung.FRANC).getBetrag());
        }
    }

    /**
     * Geldbeträge, die noch mit dem double-Feld gespeichert wurden, müssen unverändert
     * gelesen werden. Die Datei hat {@code bankprojekt.verwaltung.AlteDateienErzeuger} auf
     * Commit d7ba47f erzeugt; dort steht auch, wie man sie neu erzeugt.
     */
    @Nested
    class AlteDateienTests {
        @Test
        void geldbetraegeImDoubleFormat_werdenUnveraendertGelesen() throws IOException, ClassNotFoundException {
            InputStream datei = GeldbetragTest.class.getResourceAsStream("geldbetraege-double.ser");
            assertNotNull(datei, "Testdatei fehlt");
            Object gelesen;
            try (ObjectInputStream ein = new ObjectInputStream(datei)) {
                gelesen = ein.readObject();
            }

            double[] betraege = {0, 12.34, -5, 61.5, 491.97, 24.5, 1234.567, 0.30000000000000004, 9_999_999.99};
            Waehrung[] waehrungen = {Waehrung.EURO, Waehrung.EURO, Waehrung.EURO, Waehrung.DENAR, Waehrung.FRANC,
                    Waehrung.DOBRA, Waehrung.DENAR, Waehrung.EURO, Waehrung.EURO};
            List<?> liste = assertInstanceOf(List.class, gelesen);
            assertEquals(betraege.length, liste.size());
            for (int i = 0; i < betraege.length; i++) {
                Geldbetrag betrag = assertInstanceOf(Geldbetrag.class, liste.get(i));
                assertEquals(betraege[i], betrag.getBetrag(), "Betrag Nr. " + i);
                assertEquals(waehrungen[i], betrag.getWaehrung(), "Währung Nr. " + i);
            }
        }
    }

    @Nested
    class UmrechnenTests {
        @Test
        void umrechnen_inGleicheWaehrung_liefertDasselbeObjekt() {
            Geldbetrag g = new Geldbetrag(5);
            assertSame(g, g.umrechnen(Waehrung.EURO));
        }

        @Test
        void umrechnen_vonEuroInFremdwaehrung() {
            Geldbetrag g = new Geldbetrag(100).umrechnen(Waehrung.DENAR);
            assertEquals(6150.0, g.getBetrag(), DELTA);
            assertEquals(Waehrung.DENAR, g.getWaehrung());
        }

        @Test
        void umrechnen_vonFremdwaehrungInEuro() {
            Geldbetrag g = new Geldbetrag(49, Waehrung.DOBRA).umrechnen(Waehrung.EURO);
            assertEquals(2.0, g.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, g.getWaehrung());
        }

        @Test
        void umrechnen_zwischenZweiFremdwaehrungen() {
            // 61,5 MKD = 1 € = 24,5 STN
            Geldbetrag g = new Geldbetrag(61.5, Waehrung.DENAR).umrechnen(Waehrung.DOBRA);
            assertEquals(24.5, g.getBetrag(), DELTA);
        }

        @Test
        void umrechnen_rundetAufZweiNachkommastellen() {
            // 1 € = 491,96775 KMF -> gerundet 491,97
            Geldbetrag g = new Geldbetrag(1).umrechnen(Waehrung.FRANC);
            assertEquals(491.97, g.getBetrag(), 0.0);
        }

        @Test
        void umrechnen_veraendertThisNicht() {
            Geldbetrag g = new Geldbetrag(100);
            g.umrechnen(Waehrung.DENAR);
            assertEquals(100, g.getBetrag(), DELTA);
            assertEquals(Waehrung.EURO, g.getWaehrung());
        }

        /**
         * Laut JavaDoc wirft umrechnen(null) eine IllegalArgumentException.
         */
        @Test
        void umrechnen_wirftIllegalArgumentException_beiNull() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(1).umrechnen(null));
        }
    }

    @Nested
    class VergleichTests {
        @Test
        void compareTo_kleinerGleichGroesser() {
            Geldbetrag eins = new Geldbetrag(1);
            Geldbetrag zwei = new Geldbetrag(2);
            assertTrue(eins.compareTo(zwei) < 0);
            assertTrue(zwei.compareTo(eins) > 0);
            assertEquals(0, eins.compareTo(new Geldbetrag(1)));
        }

        @Test
        void compareTo_vergleichtUeberDenEurowert() {
            // 61,5 MKD sind 1 €, also weniger als 2 €, obwohl die Zahl größer ist
            Geldbetrag denar = new Geldbetrag(61.5, Waehrung.DENAR);
            assertEquals(0, denar.compareTo(new Geldbetrag(1)));
            assertTrue(denar.compareTo(new Geldbetrag(2)) < 0);
        }

        @Test
        void compareTo_wirftNullPointerException_beiNull() {
            assertThrows(NullPointerException.class, () -> new Geldbetrag(1).compareTo(null));
        }

        @Test
        void equals_gleicherBetragGleicheWaehrung() {
            assertEquals(new Geldbetrag(3.5), new Geldbetrag(3.5));
        }

        @Test
        void equals_gleicherWertInVerschiedenenWaehrungen() {
            assertEquals(new Geldbetrag(1), new Geldbetrag(61.5, Waehrung.DENAR));
        }

        @Test
        void equals_falseBeiUnterschiedlichemBetrag() {
            assertNotEquals(new Geldbetrag(1), new Geldbetrag(1.01));
        }

        @Test
        void equals_falseBeiNullUndFremdemTyp() {
            // equals direkt aufrufen: assertNotEquals(null, x) bzw. assertNotEquals("…", x)
            // würde nicht Geldbetrag.equals, sondern nur die Nullprüfung bzw. String.equals ausführen
            Geldbetrag g = new Geldbetrag(1);
            assertFalse(g.equals(null));
            assertFalse(g.equals("1,00 EUR"));
        }

        @Test
        void hashCode_gleichFuerGleicheBetraegeInGleicherWaehrung() {
            assertEquals(new Geldbetrag(7).hashCode(), new Geldbetrag(7).hashCode());
        }

        /**
         * Vertrag von Object: gleiche Objekte (equals == true) müssen den
         * gleichen hashCode liefern – auch bei verschiedenen Währungen.
         */
        @Test
        void hashCode_gleichFuerGleicheWerteInVerschiedenenWaehrungen() {
            Geldbetrag euro = new Geldbetrag(1);
            Geldbetrag denar = new Geldbetrag(61.5, Waehrung.DENAR);
            assertEquals(euro, denar);
            assertEquals(euro.hashCode(), denar.hashCode());
        }

        /**
         * 10 € entsprechen genau 615 MKD bzw. 245 STN.
         * @param betrag der Gegenwert
         * @param waehrung die Währung des Gegenwerts
         */
        @ParameterizedTest
        @CsvSource({"615, DENAR", "245, DOBRA"})
        void equalsUndHashCode_zehnEuroGleichDemExaktenGegenwert(double betrag, Waehrung waehrung) {
            Geldbetrag zehnEuro = new Geldbetrag(10);
            Geldbetrag gegenwert = new Geldbetrag(betrag, waehrung);
            assertEquals(zehnEuro, gegenwert);
            assertEquals(gegenwert, zehnEuro);
            assertEquals(0, zehnEuro.compareTo(gegenwert));
            assertEquals(zehnEuro.hashCode(), gegenwert.hashCode());
        }
    }

    @Nested
    class ToStringTests {
        private Locale vorher;

        @BeforeEach
        void localeSetzen() {
            // toString formatiert abhängig vom Standard-Locale, daher fest auf Deutsch
            vorher = Locale.getDefault();
            Locale.setDefault(Locale.GERMANY);
        }

        @AfterEach
        void localeZuruecksetzen() {
            Locale.setDefault(vorher);
        }

        @Test
        void toString_zweiNachkommastellenUndKuerzel() {
            assertEquals("12,50 EUR", new Geldbetrag(12.5).toString());
        }

        @Test
        void toString_mitTausenderpunktUndFremdwaehrung() {
            assertEquals("1.234,57 MKD", new Geldbetrag(1234.567, Waehrung.DENAR).toString());
        }

        @Test
        void toString_negativerBetrag() {
            assertEquals("-3,00 EUR", new Geldbetrag(-3).toString());
        }

        /**
         * Die Ausgabe rundet kaufmännisch auf zwei Nachkommastellen, und zwar den
         * Dezimalwert (1,005 wird zu 1,01), nicht die Binärdarstellung des double.
         * @param betrag der Betrag
         * @param waehrung die Währung
         * @param erwartet die erwartete Ausgabe
         */
        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "0           | EURO  | 0,00 EUR",
                "9999999.99  | EURO  | 9.999.999,99 EUR",
                "9999999.995 | EURO  | 10.000.000,00 EUR",
                "-1234.5     | EURO  | -1.234,50 EUR",
                "491.96775   | FRANC | 491,97 KMF",
                "1234.565    | DENAR | 1.234,57 MKD",
                "1.005       | DOBRA | 1,01 STN"
        })
        void toString_rundetKaufmaennischAufZweiNachkommastellen(double betrag, Waehrung waehrung, String erwartet) {
            assertEquals(erwartet, new Geldbetrag(betrag, waehrung).toString());
        }
    }

    /**
     * Der Betrag wird exakt als BigDecimal gespeichert. assertEquals auf BigDecimal prüft
     * auch die Anzahl der Nachkommastellen (1.0 ist dort nicht gleich 1.00).
     */
    @Nested
    class BigDecimalTests {
        @Test
        void konstruktorMitBigDecimal_uebernimmtBetragExaktUndUngerundet() {
            BigDecimal wert = new BigDecimal("1234.5678");
            Geldbetrag g = new Geldbetrag(wert, Waehrung.DENAR);
            assertEquals(wert, g.getBetragExakt());
            assertEquals(Waehrung.DENAR, g.getWaehrung());
        }

        @Test
        void konstruktorMitBigDecimalOhneWaehrung_erzeugtEuroBetrag() {
            Geldbetrag g = new Geldbetrag(new BigDecimal("12.34"));
            assertEquals(new BigDecimal("12.34"), g.getBetragExakt());
            assertEquals(12.34, g.getBetrag());
            assertEquals(Waehrung.EURO, g.getWaehrung());
        }

        @Test
        void konstruktorMitBigDecimal_wirftIllegalArgumentException_beiNull() {
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag((BigDecimal) null));
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag((BigDecimal) null, Waehrung.EURO));
            assertThrows(IllegalArgumentException.class, () -> new Geldbetrag(BigDecimal.ONE, null));
        }

        /**
         * Der double-Konstruktor speichert den Dezimalwert, mit dem Java den double ausgibt,
         * nicht dessen Binärdarstellung (0,1 ist binär 0,1000000000000000055…).
         * @param text der Betrag als Dezimaltext
         */
        @ParameterizedTest
        @ValueSource(strings = {"0.1", "0.3", "12.34", "1234.567", "491.96775", "0.30000000000000004"})
        void konstruktorMitDouble_speichertDenDezimalwert(String text) {
            BigDecimal erwartet = new BigDecimal(text);
            double betrag = Double.parseDouble(text);
            assertEquals(0, erwartet.compareTo(new Geldbetrag(betrag).getBetragExakt()));
            assertEquals(0, erwartet.compareTo(new Geldbetrag(betrag, Waehrung.FRANC).getBetragExakt()));
        }

        @Test
        void plusMinusMal_rechnenExaktUndLiefernGenauZweiNachkommastellen() {
            Geldbetrag a = new Geldbetrag(new BigDecimal("0.1"));
            Geldbetrag b = new Geldbetrag(new BigDecimal("0.2"));
            assertEquals(new BigDecimal("0.30"), a.plus(b).getBetragExakt());
            assertEquals(new BigDecimal("-0.10"), a.minus(b).getBetragExakt());
            assertEquals(new BigDecimal("3.33"), new Geldbetrag(BigDecimal.TEN).mal(1.0 / 3).getBetragExakt());
            assertEquals(new BigDecimal("12.35"), new Geldbetrag(new BigDecimal("12.345")).mal(1).getBetragExakt());
        }

        @Test
        void isNegativ_auchUnterEinemCent() {
            assertTrue(new Geldbetrag(new BigDecimal("-0.001")).isNegativ());
            assertFalse(new Geldbetrag(new BigDecimal("0.001")).isNegativ());
            assertFalse(new Geldbetrag(new BigDecimal("-0.00")).isNegativ());
        }

        /**
         * Geldbeträge sind gleich, wenn ihr Wert gleich ist – unabhängig davon, mit wie
         * vielen Nachkommastellen der BigDecimal angegeben ist.
         * @param text der Gegenwert von 1 € als Dezimaltext
         * @param waehrung die Währung des Gegenwerts
         */
        @ParameterizedTest
        @CsvSource({"1, EURO", "1.0, EURO", "1.000, EURO", "61.5, DENAR", "61.50, DENAR", "24.500, DOBRA"})
        void equalsUndHashCode_unabhaengigVonDerAnzahlDerNachkommastellen(String text, Waehrung waehrung) {
            Geldbetrag einEuro = new Geldbetrag(new BigDecimal("1.00"));
            Geldbetrag gegenwert = new Geldbetrag(new BigDecimal(text), waehrung);
            assertEquals(einEuro, gegenwert);
            assertEquals(gegenwert, einEuro);
            assertEquals(0, gegenwert.compareTo(einEuro));
            assertEquals(einEuro.hashCode(), gegenwert.hashCode());
        }

        @Test
        void toString_unabhaengigVonDerAnzahlDerNachkommastellen() {
            String erwartet = new Geldbetrag(new BigDecimal("1.00")).toString();
            assertEquals(erwartet, new Geldbetrag(new BigDecimal("1")).toString());
            assertEquals(erwartet, new Geldbetrag(new BigDecimal("1.000")).toString());
        }
    }

    /**
     * Zwei Randfälle verhalten sich seit der Umstellung auf BigDecimal anders als mit
     * double. In der Oberfläche können solche Beträge nicht entstehen.
     */
    @Nested
    class RandfaelleTests {
        /**
         * BigDecimal kennt keine negative Null: -0.0 wird zu 0, ist also gleich NULL_EURO
         * (mit gleichem Hashcode) und wird nicht als "-0,00" ausgegeben.
         */
        @Test
        void negativeNull_wirdZuNull() {
            Geldbetrag g = new Geldbetrag(-0.0);
            // assertEquals auf double unterscheidet 0.0 und -0.0
            assertEquals(0.0, g.getBetrag());
            assertFalse(g.isNegativ());
            assertEquals(Geldbetrag.NULL_EURO, g);
            assertEquals(Geldbetrag.NULL_EURO.hashCode(), g.hashCode());
            assertEquals(Geldbetrag.NULL_EURO.toString(), g.toString());
        }

        /**
         * Operanden mit mehr als zwei Nachkommastellen werden über ihren Dezimalwert
         * gerundet: 1,005 ergibt 1,01, obwohl der double 1.005 binär knapp darunter liegt
         * (1,00499999…).
         */
        @Test
        void operandMitMehrAlsZweiNachkommastellen_rundetDenDezimalwert() {
            assertEquals(1.01, new Geldbetrag(1.005).mal(1).getBetrag());
            assertEquals(1.01, new Geldbetrag(1.005).plus(Geldbetrag.NULL_EURO).getBetrag());
            assertEquals(1.02, new Geldbetrag(1.015).minus(Geldbetrag.NULL_EURO).getBetrag());
        }
    }

    /**
     * Das Speicherformat: Der Betrag steht als BigDecimal im Feld wert. Ströme im alten
     * Format (double im Feld betrag) werden beim Lesen umgerechnet, beschädigte abgelehnt.
     */
    @Nested
    class SpeicherformatTests {
        /** wie das alte Format: Betrag als double im Feld betrag */
        private record AltesFormat(double betrag, Waehrung waehrung) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
        }

        /** wie das neue Format, aber mit beliebigen Werten in den Feldern */
        private record NeuesFormat(Object wert, Object waehrung) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
        }

        /** ohne Betrag, weder im alten noch im neuen Format */
        private record OhneBetrag(Waehrung waehrung) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
        }

        /** altes Feld betrag, aber kein double */
        private record BetragAlsText(String betrag, Waehrung waehrung) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
        }

        @Test
        void speicherformat_hatBigDecimalImFeldWertUndDieBisherigeVersionsnummer() {
            ObjectStreamClass format = ObjectStreamClass.lookup(Geldbetrag.class);
            // dieselbe Versionsnummer, sonst ließen sich alte Dateien nicht mehr lesen
            assertEquals(1L, format.getSerialVersionUID());
            assertEquals(2, format.getFields().length);
            assertEquals(BigDecimal.class, format.getField("wert").getType());
            assertEquals(Waehrung.class, format.getField("waehrung").getType());
            assertNull(format.getField("betrag"));
        }

        /**
         * Gespeichert und wieder gelesen ergibt sich genau derselbe BigDecimal, auch mit
         * derselben Anzahl Nachkommastellen.
         * @param text der Betrag als Dezimaltext
         * @param waehrung die Währung
         */
        @ParameterizedTest
        @CsvSource({"12.34, EURO", "-5, EURO", "0, EURO", "0.00, EURO", "1234.567, DENAR",
                "0.30000000000000004, EURO", "9999999.99, FRANC", "123456789012345678901234567890.123456789, DOBRA"})
        void rundreise_liefertGenauDenselbenBetrag(String text, Waehrung waehrung) throws Exception {
            Geldbetrag vorher = new Geldbetrag(new BigDecimal(text), waehrung);
            Geldbetrag nachher = assertInstanceOf(Geldbetrag.class, lesen(speichern(vorher)));
            assertEquals(vorher.getBetragExakt(), nachher.getBetragExakt());
            assertEquals(waehrung, nachher.getWaehrung());
            assertEquals(vorher, nachher);
        }

        /**
         * Der double aus dem alten Format wird wie im Konstruktor in seinen Dezimalwert
         * umgerechnet, z. B. genau 12,34. Neu gespeichert und gelesen bleibt er gleich.
         */
        @Test
        void altesFormat_ergibtDenDezimalwertUndBleibtBeimNeuenSpeichernGleich() throws Exception {
            InputStream datei = GeldbetragTest.class.getResourceAsStream("geldbetraege-double.ser");
            assertNotNull(datei, "Testdatei fehlt");
            List<?> alt;
            try (ObjectInputStream ein = new ObjectInputStream(datei)) {
                alt = assertInstanceOf(List.class, ein.readObject());
            }
            List<?> neu = assertInstanceOf(List.class, lesen(speichern(alt)));

            String[] erwartet = {"0", "12.34", "-5", "61.5", "491.97", "24.5", "1234.567", "0.30000000000000004",
                    "9999999.99"};
            assertEquals(erwartet.length, alt.size());
            assertEquals(erwartet.length, neu.size());
            for (int i = 0; i < erwartet.length; i++) {
                BigDecimal ausAlt = ((Geldbetrag) alt.get(i)).getBetragExakt();
                assertEquals(0, new BigDecimal(erwartet[i]).compareTo(ausAlt), "Betrag Nr. " + i);
                assertEquals(ausAlt, ((Geldbetrag) neu.get(i)).getBetragExakt(), "Betrag Nr. " + i);
                assertEquals(((Geldbetrag) alt.get(i)).getWaehrung(), ((Geldbetrag) neu.get(i)).getWaehrung());
            }
        }

        @Test
        void altesFormat_wirdUeberDenDezimalwertUmgerechnet() throws Exception {
            Object gelesen = lesen(alsGeldbetrag(new AltesFormat(0.1, Waehrung.DENAR)));
            Geldbetrag g = assertInstanceOf(Geldbetrag.class, gelesen);
            assertEquals(0, new BigDecimal("0.1").compareTo(g.getBetragExakt()));
            assertEquals(Waehrung.DENAR, g.getWaehrung());
        }

        @Test
        void neuesFormat_ausDerHilfsklasse_wirdGelesen() throws Exception {
            Object gelesen = lesen(alsGeldbetrag(new NeuesFormat(new BigDecimal("2.50"), Waehrung.DOBRA)));
            Geldbetrag g = assertInstanceOf(Geldbetrag.class, gelesen);
            assertEquals(new BigDecimal("2.50"), g.getBetragExakt());
            assertEquals(Waehrung.DOBRA, g.getWaehrung());
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void altesFormat_wirftInvalidObjectException_beiNichtFinitemBetrag(double betrag) throws IOException {
            byte[] strom = alsGeldbetrag(new AltesFormat(betrag, Waehrung.EURO));
            assertThrows(InvalidObjectException.class, () -> lesen(strom));
        }

        @Test
        void wirftInvalidObjectException_wennDerBetragFehlt() throws IOException {
            byte[] strom = alsGeldbetrag(new OhneBetrag(Waehrung.EURO));
            assertThrows(InvalidObjectException.class, () -> lesen(strom));
        }

        @Test
        void altesFormat_wirftInvalidObjectException_wennDerBetragKeinDoubleIst() throws IOException {
            byte[] strom = alsGeldbetrag(new BetragAlsText("12.34", Waehrung.EURO));
            assertThrows(InvalidObjectException.class, () -> lesen(strom));
        }

        @Test
        void neuesFormat_wirftInvalidObjectException_beiFehlendemOderFalschemWert() throws IOException {
            for (NeuesFormat kaputt : List.of(new NeuesFormat(null, Waehrung.EURO),
                    new NeuesFormat("12.34", Waehrung.EURO), new NeuesFormat(12.34, Waehrung.EURO))) {
                byte[] strom = alsGeldbetrag(kaputt);
                assertThrows(InvalidObjectException.class, () -> lesen(strom), kaputt.toString());
            }
        }

        @Test
        void wirftInvalidObjectException_beiFehlenderOderFalscherWaehrung() throws IOException {
            for (NeuesFormat kaputt : List.of(new NeuesFormat(BigDecimal.ONE, null),
                    new NeuesFormat(BigDecimal.ONE, "EUR"))) {
                byte[] strom = alsGeldbetrag(kaputt);
                assertThrows(InvalidObjectException.class, () -> lesen(strom), kaputt.toString());
            }
        }

        /**
         * Serialisiert ein Objekt.
         * @param objekt das Objekt
         * @return die Bytes des Stroms
         * @throws IOException bei einem Fehler beim Schreiben
         */
        private static byte[] speichern(Object objekt) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream aus = new ObjectOutputStream(bytes)) {
                aus.writeObject(objekt);
            }
            return bytes.toByteArray();
        }

        /**
         * Liest ein Objekt aus einem Strom.
         * @param strom die Bytes des Stroms
         * @return das gelesene Objekt
         * @throws IOException bei einem Fehler beim Lesen
         * @throws ClassNotFoundException falls eine Klasse nicht gefunden wird
         */
        private static Object lesen(byte[] strom) throws IOException, ClassNotFoundException {
            try (ObjectInputStream ein = new ObjectInputStream(new ByteArrayInputStream(strom))) {
                return ein.readObject();
            }
        }

        /**
         * Serialisiert ein Objekt einer Hilfsklasse und ersetzt im Strom ihren Klassennamen
         * durch Geldbetrag. Die Felder der Hilfsklasse bleiben, so entsteht ein Strom, wie
         * ihn eine ältere oder beschädigte Datei enthalten kann. Der Name steht im Strom mit
         * vorangestellter Länge (writeUTF); ISO-8859-1 bildet jedes Byte auf genau ein
         * Zeichen ab, daher bleibt der übrige Strom beim Ersetzen unverändert.
         * @param objekt das Objekt der Hilfsklasse
         * @return der Strom mit dem Klassennamen Geldbetrag
         * @throws IOException bei einem Fehler beim Schreiben
         */
        private static byte[] alsGeldbetrag(Serializable objekt) throws IOException {
            String strom = new String(speichern(objekt), StandardCharsets.ISO_8859_1);
            String hilfsklasse = klassenname(objekt.getClass());
            assertTrue(strom.contains(hilfsklasse), "Klassenname nicht im Strom");
            return strom.replace(hilfsklasse, klassenname(Geldbetrag.class)).getBytes(StandardCharsets.ISO_8859_1);
        }

        /**
         * Liefert den Klassennamen so, wie er im Strom steht: mit vorangestellter Länge.
         * @param klasse die Klasse
         * @return der Name in der Form von writeUTF, als ISO-8859-1-Text
         * @throws IOException bei einem Fehler beim Schreiben
         */
        private static String klassenname(Class<?> klasse) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream aus = new DataOutputStream(bytes)) {
                aus.writeUTF(klasse.getName());
            }
            return new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
        }
    }
}
