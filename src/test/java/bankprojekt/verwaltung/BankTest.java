package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
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
    }

    @Nested
    class GirokontoErstellenTests {
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

        @Test
        void girokontoErstellen_wirftNullPointerException_wennInhaberNull_undZaehlerBleibtUnveraendert() {
            Bank bank = new Bank(55555555L);

            // Act & Assert: Null-Inhaber -> NullPointerException
            assertThrows(NullPointerException.class, () -> bank.girokontoErstellen(null),
                    "Null-Inhaber sollte NullPointerException werfen");

            // Es darf kein Konto angelegt worden sein
            assertTrue(bank.getKonten().isEmpty(),
                    "Nach fehlgeschlagener Kontoanlage sollte die Konten-Map leer bleiben");

            // Nach dem Fehlversuch muss die erste gültige Kontonummer weiterhin 1 sein
            long nr = bank.girokontoErstellen(Kunde.MUSTERMANN);
            assertEquals(1L, nr,
                    "Die erste vergebene Kontonummer sollte nach Fehlversuch weiterhin 1 sein");
            assertEquals(1, bank.getKonten().size(), "Es sollte genau ein Konto existieren");
        }
    }

    @Nested
    class SparbuchErstellenTests {
        @Test
        void sparbuchErstellen_vergibtFortlaufendeNummern_undSpeichertKontoKorrekt() {
            Bank bank = new Bank(24681357L);
            Kunde k1 = new Kunde("Clara", "Clever", "City 3", 1992, 3, 14);
            Kunde k2 = new Kunde("David", "Dorn", "Dorf 4", 1979, 7, 9);

            long nr1 = bank.sparbuchErstellen(k1);
            long nr2 = bank.sparbuchErstellen(k2);

            assertEquals(1L, nr1, "Erste Kontonummer sollte 1 sein");
            assertEquals(2L, nr2, "Zweite Kontonummer sollte 2 sein");
            assertNotEquals(nr1, nr2, "Kontonummern müssen eindeutig sein");

            Map<Long, Konto> konten = bank.getKonten();
            assertEquals(2, konten.size(), "Es sollten zwei Konten gespeichert sein");

            Konto konto1 = konten.get(nr1);
            assertNotNull(konto1);
            assertEquals(nr1, konto1.getKontonummer());
            assertEquals(k1, konto1.getInhaber());
            assertInstanceOf(Sparbuch.class, konto1, "Gespeichertes Konto sollte ein Sparbuch sein");

            Konto konto2 = konten.get(nr2);
            assertNotNull(konto2);
            assertEquals(nr2, konto2.getKontonummer());
            assertEquals(k2, konto2.getInhaber());
            assertInstanceOf(Sparbuch.class, konto2);
        }

        @Test
        void sparbuchErstellen_wirftNullPointerException_wennInhaberNull_undZaehlerBleibtUnveraendert() {
            Bank bank = new Bank(44444444L);

            assertThrows(NullPointerException.class, () -> bank.sparbuchErstellen(null),
                    "Null-Inhaber sollte NullPointerException werfen");

            assertTrue(bank.getKonten().isEmpty(),
                    "Nach fehlgeschlagener Kontoanlage sollte die Konten-Map leer bleiben");

            long nr = bank.sparbuchErstellen(Kunde.MUSTERMANN);
            assertEquals(1L, nr,
                    "Die erste vergebene Kontonummer sollte nach Fehlversuch weiterhin 1 sein");
            assertEquals(1, bank.getKonten().size(), "Es sollte genau ein Konto existieren");
            assertInstanceOf(Sparbuch.class, bank.getKonten().get(nr));
        }
    }
}

    @Nested
    class AlleKontenAusgabeTests {
        @Test
        void getAlleKonten_gibtLeerenStringWennKeineKonten() {
            Bank bank = new Bank(12312312L);
            String ausgabe = bank.getAlleKonten();
            assertEquals("", ausgabe, "Ohne Konten soll ein leerer String zurückgegeben werden");
        }

        @Test
        void getAlleKonten_listetSaemtlicheKonten_mitNummerUndKontostand_jeZeile() {
            Bank bank = new Bank(11112222L);
            long nr1 = bank.girokontoErstellen(Kunde.MUSTERMANN);
            long nr2 = bank.sparbuchErstellen(new Kunde("Eva","Erd","Eichenweg 5",1991,1,2));

            Map<Long, Konto> konten = bank.getKonten();
            Konto k1 = konten.get(nr1);
            Konto k2 = konten.get(nr2);

            k1.einzahlen(new Geldbetrag(10));
            k2.einzahlen(new Geldbetrag(20));

            String expected1 = nr1 + ": " + k1.getKontostand().toString();
            String expected2 = nr2 + ": " + k2.getKontostand().toString();

            String ausgabe = bank.getAlleKonten();
            String[] lines = ausgabe.split("\\n");
            // Leere Zeilen (z. B. durch abschließenden Zeilenumbruch) entfernen
            java.util.List<String> nichtLeereZeilen = new java.util.ArrayList<>();
            for (String line : lines) {
                if (!line.isEmpty()) nichtLeereZeilen.add(line);
            }

            assertEquals(2, nichtLeereZeilen.size(), "Es sollten genau zwei Zeilen ausgegeben werden");
            java.util.Set<String> zeilenSet = new java.util.HashSet<>(nichtLeereZeilen);
            assertTrue(zeilenSet.contains(expected1), "Zeile für Konto 1 fehlt oder ist falsch");
            assertTrue(zeilenSet.contains(expected2), "Zeile für Konto 2 fehlt oder ist falsch");
        }
    }
