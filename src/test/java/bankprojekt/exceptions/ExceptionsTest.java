package bankprojekt.exceptions;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die eigenen Exceptions der Bank.
 */
class ExceptionsTest {

    private static final Kunde INHABER = new Kunde("Max", "Mustermann", "Weg 1", 1990, 1, 1);

    @Nested
    class GesperrtExceptionTests {
        @Test
        void istEineGepruefteException() {
            // geprüft (checked): muss deklariert bzw. abgefangen werden
            assertTrue(Exception.class.isAssignableFrom(GesperrtException.class));
            assertFalse(RuntimeException.class.isAssignableFrom(GesperrtException.class));
        }

        @Test
        void meldungEnthaeltDieKontonummer() {
            assertEquals("Zugriff auf gesperrtes Konto mit Kontonummer 4711",
                    new GesperrtException(4711).getMessage());
        }

        @Test
        void wirdVomGesperrtenKontoMitSeinerKontonummerGeworfen() {
            Girokonto konto = new Girokonto(INHABER, 98765, new Geldbetrag(500));
            konto.sperren();
            GesperrtException e = assertThrows(GesperrtException.class,
                    () -> konto.abheben(new Geldbetrag(1)));
            assertTrue(e.getMessage().endsWith(" 98765"), e.getMessage());
        }
    }

    @Nested
    class UngueltigeKontonummerExceptionTests {
        @Test
        void istEineUngepruefteException() {
            // ungeprüft (unchecked): muss nicht deklariert werden
            assertInstanceOf(RuntimeException.class, new UngueltigeKontonummerException());
        }

        @Test
        void hatKeineMeldung() {
            assertNull(new UngueltigeKontonummerException().getMessage());
        }

        @Test
        void wirdBeiNegativerOderZuLangerKontonummerGeworfen() {
            // laut JavaDoc: negativ oder mehr als 10 Stellen
            assertThrows(UngueltigeKontonummerException.class, () -> new Sparbuch(INHABER, -1));
            assertThrows(UngueltigeKontonummerException.class, () -> new Sparbuch(INHABER, 10_000_000_000L));
            // genau 10 Stellen und 0 sind erlaubt
            assertDoesNotThrow(() -> new Sparbuch(INHABER, 9_999_999_999L));
            assertDoesNotThrow(() -> new Sparbuch(INHABER, 0));
        }
    }
}
