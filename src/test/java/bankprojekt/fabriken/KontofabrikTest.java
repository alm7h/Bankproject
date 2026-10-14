package bankprojekt.fabriken;

import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Kinderkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.exceptions.UngueltigeKontonummerException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die konkreten Fabriken des Abstract-Factory-Musters ({@link Kontofabrik}).
 */
class KontofabrikTest {

    private static final Kunde INHABER = new Kunde("Max", "Mustermann", "Weg 1", 1990, 1, 1);

    /**
     * Alle konkreten Fabriken zusammen mit dem Kontotyp, den sie erzeugen sollen.
     * @return Paare aus Fabrik und erwarteter Kontoklasse
     */
    static Stream<Arguments> fabriken() {
        return Stream.of(
                Arguments.of(new GirokontoFabrik(new Geldbetrag(500)), Girokonto.class),
                Arguments.of(new SparbuchFabrik(), Sparbuch.class),
                Arguments.of(new KinderkontoFabrik(), Kinderkonto.class),
                Arguments.of(new FestgeldkontoFabrik(), Festgeldkonto.class),
                Arguments.of(new AktienkontoFabrik(), Aktienkonto.class));
    }

    /**
     * Gemeinsamer Vertrag, den jede konkrete Fabrik erfüllen muss.
     */
    @Nested
    class GemeinsameTests {
        @ParameterizedTest
        @MethodSource("bankprojekt.fabriken.KontofabrikTest#fabriken")
        void erstellen_liefertKontoDesRichtigenTypsMitInhaberUndNummer(Kontofabrik fabrik, Class<? extends Konto> typ) {
            Konto konto = fabrik.erstellen(INHABER, 4711);
            assertEquals(typ, konto.getClass());
            assertSame(INHABER, konto.getInhaber());
            assertEquals(4711, konto.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
            assertFalse(konto.isGesperrt());
        }

        @ParameterizedTest
        @MethodSource("bankprojekt.fabriken.KontofabrikTest#fabriken")
        void erstellen_liefertBeiJedemAufrufEinNeuesKonto(Kontofabrik fabrik, Class<? extends Konto> typ) {
            Konto erstes = fabrik.erstellen(INHABER, 1);
            Konto zweites = fabrik.erstellen(INHABER, 2);
            assertNotSame(erstes, zweites);
            assertEquals(1, erstes.getKontonummer());
            assertEquals(2, zweites.getKontonummer());
        }

        @ParameterizedTest
        @MethodSource("bankprojekt.fabriken.KontofabrikTest#fabriken")
        void erstellen_wirftNullPointerException_wennInhaberNull(Kontofabrik fabrik, Class<? extends Konto> typ) {
            assertThrows(NullPointerException.class, () -> fabrik.erstellen(null, 4711));
        }

        @ParameterizedTest
        @MethodSource("bankprojekt.fabriken.KontofabrikTest#fabriken")
        void erstellen_wirftUngueltigeKontonummerException_beiUngueltigerNummer(Kontofabrik fabrik, Class<? extends Konto> typ) {
            assertThrows(UngueltigeKontonummerException.class, () -> fabrik.erstellen(INHABER, -1));
            assertThrows(UngueltigeKontonummerException.class, () -> fabrik.erstellen(INHABER, 10_000_000_000L));
        }
    }

    @Nested
    class GirokontoFabrikTests {
        @Test
        void erstellen_uebernimmtDenDispoDerFabrik() {
            Girokonto konto = (Girokonto) new GirokontoFabrik(new Geldbetrag(750)).erstellen(INHABER, 4711);
            assertEquals(new Geldbetrag(750), konto.getDispo());
        }

        @Test
        void alleKontenEinerFabrik_habenDenselbenDispo() {
            GirokontoFabrik fabrik = new GirokontoFabrik(new Geldbetrag(300));
            Girokonto a = (Girokonto) fabrik.erstellen(INHABER, 1);
            Girokonto b = (Girokonto) fabrik.erstellen(INHABER, 2);
            assertEquals(new Geldbetrag(300), a.getDispo());
            assertEquals(new Geldbetrag(300), b.getDispo());
        }

        @Test
        void ungueltigerDispo_fuehrtBeimErstellenZuIllegalArgumentException() {
            // Die Fabrik prüft den Dispo nicht selbst, sondern Girokonto.setDispo beim Erstellen
            GirokontoFabrik ohneDispo = new GirokontoFabrik(null);
            GirokontoFabrik nullDispo = new GirokontoFabrik(Geldbetrag.NULL_EURO);
            GirokontoFabrik negativerDispo = new GirokontoFabrik(new Geldbetrag(-1));
            assertThrows(IllegalArgumentException.class, () -> ohneDispo.erstellen(INHABER, 4711));
            assertThrows(IllegalArgumentException.class, () -> nullDispo.erstellen(INHABER, 4711));
            assertThrows(IllegalArgumentException.class, () -> negativerDispo.erstellen(INHABER, 4711));
        }
    }
}
