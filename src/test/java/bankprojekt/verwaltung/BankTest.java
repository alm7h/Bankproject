package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Bank-Verwaltung.
 * Fokus: Tests für den Konstruktor von {@link Bank} und die Methoden getKonten, getBankleitzahl, girokontoErstellen.
 */
class BankTest {

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktorInitialisiertFelderKorrekt() {
            // Arrange & Act
            Bank bank = new Bank(12345678L);

            // Assert
            assertEquals(12345678L, bank.getBankleitzahl(), "Bankleitzahl sollte korrekt gesetzt werden");
            assertNotNull(bank.getKonten(), "Konten-Map darf nicht null sein");
            assertTrue(bank.getKonten().isEmpty(), "Konten-Map sollte initial leer sein");
            assertEquals(new Geldbetrag(500), bank.dispo_default, "Standard-Dispo sollte 500 EUR sein");
        }

        @Test
        void konstruktorWirftExceptionBeiNegativerBLZ() {
            assertThrows(IllegalArgumentException.class, () -> new Bank(-1L),
                    "Negative Bankleitzahl sollte IllegalArgumentException werfen");
        }
    }

    @Nested
    class MethodenTests {
        @Test
        void getBankleitzahl_gibtInitialenWertZurueck() {
            Bank bank = new Bank(47110000L);
            assertEquals(47110000L, bank.getBankleitzahl());
        }

        @Test
        void getKonten_istInitialLeer_undWirdNachKontoanlageGefuellt() {
            Bank bank = new Bank(10020030L);
            Map<Long, Konto> konten = bank.getKonten();
            assertNotNull(konten, "Konten-Map darf nicht null sein");
            assertTrue(konten.isEmpty(), "Konten-Map sollte initial leer sein");

            // Ein Konto anlegen
            Kunde k1 = Kunde.MUSTERMANN;
            long nr1 = bank.girokontoErstellen(k1);
            assertEquals(1L, nr1, "Erste Kontonummer sollte 1 sein");

            assertEquals(1, konten.size(), "Nach Anlage eines Kontos sollte genau ein Eintrag existieren");
            assertTrue(konten.containsKey(nr1), "Map sollte die vergebene Kontonummer als Schlüssel enthalten");
            assertInstanceOf(Girokonto.class, konten.get(nr1), "Gespeichertes Konto sollte ein Girokonto sein");
        }

        @Test
        void girokontoErstellen_vergibtFortlaufendeNummern_undSpeichertKontoKorrekt() {
            Bank bank = new Bank(98765432L);
            Kunde k1 = new Kunde("Alice", "Anders", "Allee 1", 1985, 5, 20);
            Kunde k2 = new Kunde("Bob", "Bauer", "Bergweg 2", 1980, 12, 31);

            long nr1 = bank.girokontoErstellen(k1);
            long nr2 = bank.girokontoErstellen(k2);

            assertEquals(1L, nr1, "Erste Kontonummer sollte 1 sein");
            assertEquals(2L, nr2, "Zweite Kontonummer sollte 2 sein");
            assertNotEquals(nr1, nr2, "Kontonummern müssen eindeutig sein");

            Map<Long, Konto> konten = bank.getKonten();
            assertEquals(2, konten.size(), "Es sollten zwei Konten gespeichert sein");

            // Konto 1 prüfen
            Konto konto1 = konten.get(nr1);
            assertNotNull(konto1);
            assertEquals(nr1, konto1.getKontonummer());
            assertEquals(k1, konto1.getInhaber());
            assertInstanceOf(Girokonto.class, konto1);
            assertEquals(bank.dispo_default, ((Girokonto) konto1).getDispo(),
                    "Girokonto sollte mit dem Standard-Dispo der Bank angelegt werden");

            // Konto 2 prüfen
            Konto konto2 = konten.get(nr2);
            assertNotNull(konto2);
            assertEquals(nr2, konto2.getKontonummer());
            assertEquals(k2, konto2.getInhaber());
            assertInstanceOf(Girokonto.class, konto2);
            assertEquals(bank.dispo_default, ((Girokonto) konto2).getDispo());
        }
    }
}