package bankprojekt.basisdaten;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            Geldbetrag g = new Geldbetrag(1);
            assertNotEquals(null, g);
            assertNotEquals("1,00 EUR", g);
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
    }
}
