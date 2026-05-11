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

    @Nested
    class AlleKontonummernTests {
        @Test
        void getAlleKontonummern_gibtLeeresSetWennKeineKonten() {
            Bank bank = new Bank(33334444L);
            java.util.Set<Long> nummern = bank.getAlleKontonummern();
            assertNotNull(nummern, "Das zurückgegebene Set darf nicht null sein");
            assertTrue(nummern.isEmpty(), "Ohne Konten soll ein leeres Set zurückgegeben werden");
        }

        @Test
        void getAlleKontonummern_enthaeltSaemtlicheVergebenenNummern_ohneDuplikate() {
            Bank bank = new Bank(22221111L);
            long nr1 = bank.girokontoErstellen(Kunde.MUSTERMANN);
            long nr2 = bank.sparbuchErstellen(new Kunde("Fritz","Flink","Forstweg 7",1990,2,3));

            java.util.Set<Long> nummern = bank.getAlleKontonummern();

            assertEquals(2, nummern.size(), "Es sollten genau zwei verschiedene Kontonummern enthalten sein");
            assertTrue(nummern.contains(nr1), "Set sollte Kontonummer des Girokontos enthalten");
            assertTrue(nummern.contains(nr2), "Set sollte Kontonummer des Sparbuchs enthalten");
        }

        @Test
        void getAlleKontonummern_liefertDefensiveKopie_DieBankBleibtUnveraendert() {
            Bank bank = new Bank(90909090L);
            long nr1 = bank.girokontoErstellen(Kunde.MUSTERMANN);

            java.util.Set<Long> nummern = bank.getAlleKontonummern();
            // Manipuliere das zurückgegebene Set
            nummern.clear();
            nummern.add(999L);

            // Hole frische Sicht aus der Bank und prüfe, dass sie unverändert ist
            java.util.Set<Long> nummernNeu = bank.getAlleKontonummern();
            assertEquals(1, nummernNeu.size(), "Die Bank-internen Kontonummern dürfen von externen Änderungen unberührt bleiben");
            assertTrue(nummernNeu.contains(nr1), "Die echte Kontonummer sollte weiterhin enthalten sein");
            assertFalse(nummernNeu.contains(999L), "Fremde Nummern dürfen nicht auftauchen");
        }
    }

    @Nested
    class AlleKundenTests {
        @Test
        void getAlleKunden_gibtLeeresSetWennKeineKonten() {
            Bank bank = new Bank(12121212L);
            java.util.SortedSet<Kunde> kunden = bank.getAlleKunden();
            assertNotNull(kunden, "Zurückgegebenes SortedSet darf nicht null sein");
            assertTrue(kunden.isEmpty(), "Ohne Konten soll ein leeres Set von Kunden zurückgegeben werden");
        }

        @Test
        void getAlleKunden_enthaeltJedenKundenNurEinmal_auchBeiMehrerenKonten() {
            Bank bank = new Bank(56565656L);
            Kunde alice = new Kunde("Alice", "Anders", "Allee 1", 1995, 5, 5);
            long nr1 = bank.girokontoErstellen(alice);
            long nr2 = bank.sparbuchErstellen(alice);
            assertNotEquals(nr1, nr2);

            java.util.SortedSet<Kunde> kunden = bank.getAlleKunden();
            assertEquals(1, kunden.size(), "Der gleiche Kunde mit mehreren Konten darf nur einmal erscheinen");
            assertTrue(kunden.contains(alice), "Das Set sollte Alice enthalten");
        }

        @Test
        void getAlleKunden_sortiertNachGeburtstagAbsteigend_undBeiGleichheitNachNameAufsteigend() {
            Bank bank = new Bank(78787878L);
            // Drei Kunden: zwei mit gleichem Geburtstag
            Kunde clara = new Kunde("Clara", "Clever", "City 3", 1992, 3, 14); // 1992-03-14
            Kunde david = new Kunde("David", "Dorn", "Dorf 4", 1979, 7, 9);   // 1979-07-09 (ältester)
            Kunde benno = new Kunde("Benno", "Bauer", "Bergweg 2", 1992, 3, 14); // gleiches Datum wie Clara, Name kommt vor Clara lexikografisch

            // Konten anlegen (Reihenfolge absichtlich durcheinander)
            bank.sparbuchErstellen(david);
            bank.girokontoErstellen(clara);
            bank.girokontoErstellen(benno);

            java.util.SortedSet<Kunde> kunden = bank.getAlleKunden();
            assertEquals(3, kunden.size(), "Alle drei unterschiedlichen Kunden sollten enthalten sein");

            java.util.Iterator<Kunde> it = kunden.iterator();
            assertTrue(it.hasNext());
            Kunde first = it.next(); // juengster (1992-03-14), bei Gleichstand Name aufsteigend => Benno vor Clara
            assertEquals(1992, first.getGeburtstag().getYear());
            assertEquals("Bauer, Benno", first.getName());

            assertTrue(it.hasNext());
            Kunde second = it.next();
            assertEquals(1992, second.getGeburtstag().getYear());
            assertEquals("Clever, Clara", second.getName());

            assertTrue(it.hasNext());
            Kunde third = it.next();
            assertEquals(1979, third.getGeburtstag().getYear());
            assertEquals("Dorn, David", third.getName());
            assertFalse(it.hasNext());
        }
    }


@Nested
class KontoLoeschenTests {
    @Test
    void kontoLoeschen_gibtFalse_wennNummerNichtExistiert() {
        Bank bank = new Bank(10101010L);
        // Noch keine Konten vorhanden
        assertFalse(bank.kontoLoeschen(1L), "Nicht vorhandenes Konto darf nicht gelöscht werden");
        assertTrue(bank.getKonten().isEmpty(), "Konten-Map bleibt leer");
    }

    @Test
    void kontoLoeschen_loeschtExistierendesKonto_undGibtTrue() {
        Bank bank = new Bank(20202020L);
        long nr = bank.girokontoErstellen(Kunde.MUSTERMANN);
        assertEquals(1L, nr);
        assertEquals(1, bank.getKonten().size());

        boolean geloescht = bank.kontoLoeschen(nr);
        assertTrue(geloescht, "Löschen eines existierenden Kontos sollte true liefern");
        assertFalse(bank.getKonten().containsKey(nr), "Gelöschte Kontonummer darf nicht mehr vorhanden sein");
        assertEquals(0, bank.getKonten().size(), "Map sollte nach dem Löschen leer sein");
    }

    @Test
    void kontoLoeschen_istIdempotent_zweiteLoeschungGibtFalse() {
        Bank bank = new Bank(30303030L);
        long nr = bank.sparbuchErstellen(new Kunde("Ina", "Igel", "Immenweg 9", 1993, 4, 3));
        assertTrue(bank.kontoLoeschen(nr));
        // erneuter Löschversuch derselben Nummer
        assertFalse(bank.kontoLoeschen(nr), "Zweiter Löschversuch derselben Nummer muss false liefern");
    }

    @Test
    void kontoLoeschen_beeinflusstKontonummernZaehlerNicht() {
        Bank bank = new Bank(40404040L);
        long nr1 = bank.girokontoErstellen(new Kunde("Paul", "Probst", "Parkweg 1", 1990, 1, 1));
        long nr2 = bank.sparbuchErstellen(new Kunde("Quinn", "Quelle", "Quellenweg 2", 1991, 2, 2));
        assertEquals(1L, nr1);
        assertEquals(2L, nr2);

        // Lösche das erste Konto
        assertTrue(bank.kontoLoeschen(nr1));
        assertFalse(bank.getKonten().containsKey(nr1));
        assertEquals(1, bank.getKonten().size());

        // Neues Konto bekommt die nächste freie Nummer (3), keine Wiederverwendung
        long nr3 = bank.girokontoErstellen(new Kunde("Rita", "Reim", "Ring 3", 1992, 3, 3));
        assertEquals(3L, nr3, "Nach dem Löschen dürfen Kontonummern nicht wiederverwendet werden");
        assertTrue(bank.getKonten().containsKey(nr2));
        assertTrue(bank.getKonten().containsKey(nr3));
        assertEquals(2, bank.getKonten().size());
    }
}
