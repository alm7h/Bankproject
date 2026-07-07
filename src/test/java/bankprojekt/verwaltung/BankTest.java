package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.fabriken.GirokontoFabrik;
import bankprojekt.fabriken.SparbuchFabrik;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Bank-Verwaltung.
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
            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),k1);
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

            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),k1);
            long nr2 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),k2);

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
            assertThrows(NullPointerException.class, () -> bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),null),
                    "Null-Inhaber sollte NullPointerException werfen");

            // Es darf kein Konto angelegt worden sein
            assertTrue(bank.getKonten().isEmpty(),
                    "Nach fehlgeschlagener Kontoanlage sollte die Konten-Map leer bleiben");

            // Nach dem Fehlversuch muss die erste gültige Kontonummer weiterhin 1 sein
            long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
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

            long nr1 = bank.kontoErstellen(new SparbuchFabrik(),k1);
            long nr2 = bank.kontoErstellen(new SparbuchFabrik(),k2);

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

            assertThrows(NullPointerException.class, () -> bank.kontoErstellen(new SparbuchFabrik(),null),
                    "Null-Inhaber sollte NullPointerException werfen");

            assertTrue(bank.getKonten().isEmpty(),
                    "Nach fehlgeschlagener Kontoanlage sollte die Konten-Map leer bleiben");

            long nr = bank.kontoErstellen(new SparbuchFabrik(),Kunde.MUSTERMANN);
            assertEquals(1L, nr,
                    "Die erste vergebene Kontonummer sollte nach Fehlversuch weiterhin 1 sein");
            assertEquals(1, bank.getKonten().size(), "Es sollte genau ein Konto existieren");
            assertInstanceOf(Sparbuch.class, bank.getKonten().get(nr));
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
            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
            long nr2 = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Eva","Erd","Eichenweg 5",1991,1,2));

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
            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
            long nr2 = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Fritz","Flink","Forstweg 7",1990,2,3));

            java.util.Set<Long> nummern = bank.getAlleKontonummern();

            assertEquals(2, nummern.size(), "Es sollten genau zwei verschiedene Kontonummern enthalten sein");
            assertTrue(nummern.contains(nr1), "Set sollte Kontonummer des Girokontos enthalten");
            assertTrue(nummern.contains(nr2), "Set sollte Kontonummer des Sparbuchs enthalten");
        }

        @Test
        void getAlleKontonummern_liefertDefensiveKopie_DieBankBleibtUnveraendert() {
            Bank bank = new Bank(90909090L);
            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);

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
            long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),alice);
            long nr2 = bank.kontoErstellen(new SparbuchFabrik(),alice);
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
            bank.kontoErstellen(new SparbuchFabrik(),david);
            bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),clara);
            bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),benno);

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
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
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
        long nr = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Ina", "Igel", "Immenweg 9", 1993, 4, 3));
        assertTrue(bank.kontoLoeschen(nr));
        // erneuter Löschversuch derselben Nummer
        assertFalse(bank.kontoLoeschen(nr), "Zweiter Löschversuch derselben Nummer muss false liefern");
    }

    @Test
    void kontoLoeschen_beeinflusstKontonummernZaehlerNicht() {
        Bank bank = new Bank(40404040L);
        long nr1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Paul", "Probst", "Parkweg 1", 1990, 1, 1));
        long nr2 = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Quinn", "Quelle", "Quellenweg 2", 1991, 2, 2));
        assertEquals(1L, nr1);
        assertEquals(2L, nr2);

        // Lösche das erste Konto
        assertTrue(bank.kontoLoeschen(nr1));
        assertFalse(bank.getKonten().containsKey(nr1));
        assertEquals(1, bank.getKonten().size());

        // Neues Konto bekommt die nächste freie Nummer (3), keine Wiederverwendung
        long nr3 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Rita", "Reim", "Ring 3", 1992, 3, 3));
        assertEquals(3L, nr3, "Nach dem Löschen dürfen Kontonummern nicht wiederverwendet werden");
        assertTrue(bank.getKonten().containsKey(nr2));
        assertTrue(bank.getKonten().containsKey(nr3));
        assertEquals(2, bank.getKonten().size());
    }
}


@Nested
class GetKontostandTests {
    @Test
    void getKontostand_gibtNull_wennKontoNichtExistiert() {
        Bank bank = new Bank(51515151L);
        assertNull(bank.getKontostand(999L), "Für unbekannte Kontonummern sollte null zurückgegeben werden");
    }

    @Test
    void getKontostand_gibtNullEuro_fuerNeuAngelegtesKonto() {
        Bank bank = new Bank(61616161L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        Geldbetrag stand = bank.getKontostand(nr);
        assertNotNull(stand, "Kontostand eines existierenden Kontos darf nicht null sein");
        assertEquals(Geldbetrag.NULL_EURO, stand, "Neues Konto sollte 0,00 EUR Kontostand haben");
    }

    @Test
    void getKontostand_spiegeltEinUndAuszahlungen() {
        Bank bank = new Bank(71717171L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Tom", "Tester", "Testweg 1", 1990, 1, 1));
        Konto konto = bank.getKonten().get(nr);

        // Einzahlung 10
        konto.einzahlen(new Geldbetrag(10));
        assertEquals(new Geldbetrag(10), bank.getKontostand(nr), "Nach Einzahlung sollten 10,00 EUR vorhanden sein");

        // Auszahlung 3 (innerhalb Dispo und Kontostand)
        try {
            boolean abgehoben = konto.abheben(new Geldbetrag(3));
            assertTrue(abgehoben, "Abheben von 3,00 EUR sollte erfolgreich sein");
        } catch (bankprojekt.exceptions.GesperrtException e) {
            fail("Konto sollte nicht gesperrt sein");
        }
        assertEquals(new Geldbetrag(7), bank.getKontostand(nr), "Nach Einzahlung 10 und Abhebung 3 sollten 7,00 EUR verbleiben");
    }

    @Test
    void getKontostand_gibtNullNachLoeschen() {
        Bank bank = new Bank(81818181L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Lena", "Licht", "Laternenweg 2", 1992, 2, 2));
        assertNotNull(bank.getKontostand(nr));

        // Konto löschen -> danach sollte getKontostand null liefern
        assertTrue(bank.kontoLoeschen(nr));
        assertNull(bank.getKontostand(nr), "Nach dem Löschen sollte der Kontostand für die Nummer null sein");
    }
}


@Nested
class GeldAbhebenTests {
    @Test
    void geldAbheben_gibtFalse_wennKontoNichtExistiert() {
        Bank bank = new Bank(91919191L);
        try {
            boolean erfolg = bank.geldAbheben(42L, new Geldbetrag(10));
            assertFalse(erfolg, "Abheben bei unbekannter Kontonummer muss false liefern");
        } catch (bankprojekt.exceptions.GesperrtException e) {
            fail("Bei unbekannter Kontonummer darf keine GesperrtException auftreten");
        }
    }

    @Test
    void geldAbheben_ziehtBetragAb_beiGirokontoInnerhalbDispo() throws bankprojekt.exceptions.GesperrtException {
        Bank bank = new Bank(92929292L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        // Keine Einzahlung, aber Dispo 500 erlaubt Überziehung
        boolean erfolg = bank.geldAbheben(nr, new Geldbetrag(100));
        assertTrue(erfolg, "Abheben innerhalb des Dispos sollte erfolgreich sein");
        assertEquals(new Geldbetrag(-100), bank.getKontostand(nr), "Kontostand sollte um 100,00 EUR sinken (Überziehung)");
    }

    @Test
    void geldAbheben_gibtFalse_wennUeberDispo() throws bankprojekt.exceptions.GesperrtException {
        Bank bank = new Bank(92929293L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        boolean erfolg = bank.geldAbheben(nr, new Geldbetrag(600));
        assertFalse(erfolg, "Abheben über Dispo-Grenze muss false liefern");
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(nr), "Kontostand darf sich bei fehlgeschlagener Abhebung nicht ändern");
    }

    @Test
    void geldAbheben_beachtetSparbuchRegeln_minimumUndMonatslimit() throws Exception {
        Bank bank = new Bank(93939393L);
        long nr = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Sara", "Spar", "Sparkassenweg 1", 1990, 1, 1));
        Konto spar = bank.getKonten().get(nr);
        // Anfangs 0,00: Abheben > 0 führt unter Minimum -> false
        boolean erfolg1 = bank.geldAbheben(nr, new Geldbetrag(0.6));
        assertFalse(erfolg1, "Sparbuch darf Minimum nicht unterschreiten");

        // Guthaben aufbauen und nahe Minimum abheben
        spar.einzahlen(new Geldbetrag(100));
        boolean erfolg2 = bank.geldAbheben(nr, new Geldbetrag(99.6));
        assertFalse(erfolg2, "Abhebung, die den Stand unter 0,50 EUR drücken würde, muss false liefern");
        assertEquals(new Geldbetrag(100), bank.getKontostand(nr));

        boolean erfolg3 = bank.geldAbheben(nr, new Geldbetrag(99.5));
        assertTrue(erfolg3, "Abhebung bis genau auf das Minimum 0,50 EUR ist erlaubt");
        assertEquals(new Geldbetrag(0.5), bank.getKontostand(nr));

        // Monatslimit testen
        spar.einzahlen(new Geldbetrag(5000));
        boolean erfolg4 = bank.geldAbheben(nr, new Geldbetrag(1500));
        assertTrue(erfolg4, "Erste Abhebung innerhalb 2000 EUR/Monat sollte klappen");
        boolean erfolg5 = bank.geldAbheben(nr, new Geldbetrag(600));
        assertFalse(erfolg5, "Überschreitung des 2000-EUR-Monatslimits muss false liefern");
    }

    @Test
    void geldAbheben_wirftIllegalArgumentException_beiUngueltigemBetrag() {
        Bank bank = new Bank(94949494L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        // null
        assertThrows(IllegalArgumentException.class, () -> bank.geldAbheben(nr, null));
        // negativ
        assertThrows(IllegalArgumentException.class, () -> bank.geldAbheben(nr, new Geldbetrag(-1)));
    }

    @Test
    void geldAbheben_wirftGesperrtException_wennKontoGesperrt() {
        Bank bank = new Bank(95959595L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        Konto k = bank.getKonten().get(nr);
        // Konto sperren
        k.sperren();
        assertThrows(bankprojekt.exceptions.GesperrtException.class,
                () -> bank.geldAbheben(nr, new Geldbetrag(10)),
                "Bei gesperrtem Konto muss eine GesperrtException geworfen werden");
    }
}



@Nested
class GeldEinzahlenTests {
    @Test
    void geldEinzahlen_hatKeineWirkung_wennKontoNichtExistiert() {
        Bank bank = new Bank(60606060L);
        // Kein Konto vorhanden, Einzahlung auf unbekannte Nummer darf nichts bewirken und keine Exception werfen
        bank.geldEinzahlen(42L, new Geldbetrag(10));
        assertTrue(bank.getKonten().isEmpty(), "Konten-Map bleibt leer, keine Seiteneffekte");
    }

    @Test
    void geldEinzahlen_erhoehtKontostand_beiGirokonto() {
        Bank bank = new Bank(70707070L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(nr));

        bank.geldEinzahlen(nr, new Geldbetrag(10));
        assertEquals(new Geldbetrag(10), bank.getKontostand(nr),
                "Einzahlung sollte den Kontostand entsprechend erhöhen");

        bank.geldEinzahlen(nr, new Geldbetrag(5.75));
        assertEquals(new Geldbetrag(15.75), bank.getKontostand(nr),
                "Mehrfache Einzahlungen addieren sich auf");
    }

    @Test
    void geldEinzahlen_erhoehtKontostand_beiSparbuch() {
        Bank bank = new Bank(80808080L);
        long nr = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Susi", "Sparsam", "Sparallee 1", 1990, 1, 1));
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(nr));

        bank.geldEinzahlen(nr, new Geldbetrag(100));
        assertEquals(new Geldbetrag(100), bank.getKontostand(nr));
    }

    @Test
    void geldEinzahlen_wirftIllegalArgumentException_beiNullOderNegativ() {
        Bank bank = new Bank(99990000L);
        long nr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        // null-Betrag
        assertThrows(IllegalArgumentException.class, () -> bank.geldEinzahlen(nr, null));
        // negativer Betrag
        assertThrows(IllegalArgumentException.class, () -> bank.geldEinzahlen(nr, new Geldbetrag(-0.01)));
    }
}


@Nested
class GeldUeberweisenTests {
    @Test
    void geldUeberweisen_transferiertZwischenZweiGirokonten() throws bankprojekt.exceptions.GesperrtException {
        Bank bank = new Bank(11110000L);
        long senderNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Alice", "Anders", "Allee 1", 1990, 1, 1));
        long empfaengerNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Bob", "Bauer", "Berg 2", 1991, 2, 2));
        // Startguthaben beim Sender aufbauen
        bank.geldEinzahlen(senderNr, new Geldbetrag(50));

        boolean ok = bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(20), "Miete");
        assertTrue(ok, "Ueberweisung sollte erfolgreich sein");
        assertEquals(new Geldbetrag(30), bank.getKontostand(senderNr));
        assertEquals(new Geldbetrag(20), bank.getKontostand(empfaengerNr));
    }

    @Test
    void geldUeberweisen_gibtFalse_wennSenderOderEmpfaengerNichtExistiert() throws Exception {
        Bank bank = new Bank(11110001L);
        long senderNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        bank.geldEinzahlen(senderNr, new Geldbetrag(10));
        assertFalse(bank.geldUeberweisen(senderNr, 999L, new Geldbetrag(5), "Test"));
        assertFalse(bank.geldUeberweisen(999L, senderNr, new Geldbetrag(5), "Test"));
        assertEquals(new Geldbetrag(10), bank.getKontostand(senderNr), "Kontostand darf sich nicht aendern");
    }

    @Test
    void geldUeberweisen_gibtFalse_wennNichtUeberweisungsfaehig() throws Exception {
        Bank bank = new Bank(11110002L);
        long sparNr = bank.kontoErstellen(new SparbuchFabrik(),new Kunde("Susi", "Sparsam", "Str 1", 1992, 3, 3));
        long giroNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        bank.geldEinzahlen(giroNr, new Geldbetrag(10));
        assertFalse(bank.geldUeberweisen(sparNr, giroNr, new Geldbetrag(5), "Test"));
        assertFalse(bank.geldUeberweisen(giroNr, sparNr, new Geldbetrag(5), "Test"));
        assertEquals(new Geldbetrag(10), bank.getKontostand(giroNr));
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(sparNr));
    }

    @Test
    void geldUeberweisen_wirftGesperrtException_wennSenderGesperrt() {
        Bank bank = new Bank(11110003L);
        long senderNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        long empfaengerNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Eva", "Empf", "Ecke 5", 1993, 4, 4));
        // Sperren des Senderkontos
        bank.getKonten().get(senderNr).sperren();
        assertThrows(bankprojekt.exceptions.GesperrtException.class,
                () -> bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(1), "Test"));
    }

    @Test
    void geldUeberweisen_wirftIllegalArgumentException_beiUngueltigenParametern() {
        Bank bank = new Bank(11110004L);
        long a = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        long b = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Ina", "Igel", "Im Weg", 1994, 5, 5));
        // null Betrag
        assertThrows(IllegalArgumentException.class, () -> bank.geldUeberweisen(a, b, null, "x"));
        // negativer Betrag
        assertThrows(IllegalArgumentException.class, () -> bank.geldUeberweisen(a, b, new Geldbetrag(-1), "x"));
        // null Verwendungszweck
        assertThrows(IllegalArgumentException.class, () -> bank.geldUeberweisen(a, b, new Geldbetrag(1), null));
        // gleiche Kontonummern
        assertThrows(IllegalArgumentException.class, () -> bank.geldUeberweisen(a, a, new Geldbetrag(1), "x"));
    }

    @Test
    void geldUeberweisen_gibtFalse_wennDeckungNichtAusreicht() throws Exception {
        Bank bank = new Bank(11110005L);
        long senderNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),Kunde.MUSTERMANN);
        long empfaengerNr = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),new Kunde("Tom", "Top", "Tor 7", 1995, 6, 6));
        // Ohne Guthaben: max Dispo 500
        boolean ok1 = bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(600), "Test");
        assertFalse(ok1);
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(senderNr));
        assertEquals(Geldbetrag.NULL_EURO, bank.getKontostand(empfaengerNr));
    }
}


@Nested
class GesamtkontostaendeTests {
    @Test
    void getGesamtkontostaende_gibtLeereMapWennKeineKonten() {
        Bank bank = new Bank(12120000L);
        Map<Kunde, Geldbetrag> map = bank.getGesamtkontostaende();
        assertNotNull(map, "Zurueckgegebene Map darf nicht null sein");
        assertTrue(map.isEmpty(), "Ohne Konten soll eine leere Map geliefert werden");
    }

    @Test
    void getGesamtkontostaende_fasstKontostaendeProKundeZusammen_inklusiveNegativerStaende() throws Exception {
        Bank bank = new Bank(12120001L);
        Kunde alice = new Kunde("Alice", "Anders", "Allee 1", 1990, 1, 1);
        Kunde bob = new Kunde("Bob", "Bauer", "Berg 2", 1991, 2, 2);

        long aGiro = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),alice);
        long aSpar = bank.kontoErstellen(new SparbuchFabrik(),alice);
        long bSpar = bank.kontoErstellen(new SparbuchFabrik(),bob);

        // Alice: Giro -20,00 EUR (50 einzahlen, 70 abheben); Spar +50,00 EUR => Summe 30,00 EUR
        bank.geldEinzahlen(aGiro, new Geldbetrag(50));
        assertTrue(bank.geldAbheben(aGiro, new Geldbetrag(70)), "Abheben innerhalb Dispo sollte klappen");
        bank.geldEinzahlen(aSpar, new Geldbetrag(50));

        // Bob: Spar +5,00 EUR
        bank.geldEinzahlen(bSpar, new Geldbetrag(5));

        Map<Kunde, Geldbetrag> map = bank.getGesamtkontostaende();
        assertEquals(2, map.size(), "Map sollte zwei Kunden enthalten");
        assertEquals(new Geldbetrag(30), map.get(alice), "Alice sollte insgesamt 30,00 EUR haben");
        assertEquals(new Geldbetrag(5), map.get(bob), "Bob sollte insgesamt 5,00 EUR haben");
    }

    @Test
    void getGesamtkontostaende_enthaeltJedenKundenGenauEinmal() {
        Bank bank = new Bank(12120002L);
        Kunde clara = new Kunde("Clara", "Clever", "City 3", 1992, 3, 14);
        long c1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),clara);
        long c2 = bank.kontoErstellen(new SparbuchFabrik(),clara);
        bank.geldEinzahlen(c1, new Geldbetrag(10));
        bank.geldEinzahlen(c2, new Geldbetrag(15));

        Map<Kunde, Geldbetrag> map = bank.getGesamtkontostaende();
        assertEquals(1, map.size(), "Clara sollte nur einmal als Schluessel erscheinen");
        assertTrue(map.containsKey(clara));
        assertEquals(new Geldbetrag(25), map.get(clara));
    }
}



@Nested
class KontenEinesKundenLoeschenTests {
    @Test
    void kontenEinesKundenLoeschen_gibtNullWennKundeKeineKontenHat() {
        Bank bank = new Bank(12345000L);
        Kunde alice = new Kunde("Alice", "Anders", "Allee 1", 1990, 1, 1);
        // Alice hat (noch) keine Konten bei dieser Bank
        int geloescht = bank.kontenEinesKundenLoeschen(alice);
        assertEquals(0, geloescht, "Ohne Konten sollten 0 geloescht werden");
        assertTrue(bank.getKonten().isEmpty(), "Konten-Map bleibt leer");
    }

    @Test
    void kontenEinesKundenLoeschen_loeschtAlleKontenDesKunden_undNurDiese() {
        Bank bank = new Bank(12345001L);
        Kunde alice = new Kunde("Alice", "Anders", "Allee 1", 1990, 1, 1);
        Kunde bob   = new Kunde("Bob", "Bauer", "Berg 2", 1991, 2, 2);

        long a1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),alice);
        long a2 = bank.kontoErstellen(new SparbuchFabrik(),alice);
        long b1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),bob);

        assertEquals(3, bank.getKonten().size(), "Vorher sollten 3 Konten existieren");

        int geloescht = bank.kontenEinesKundenLoeschen(alice);
        assertEquals(2, geloescht, "Alle Konten von Alice (2 Stueck) sollten geloescht werden");

        // Nur Bobs Konto darf uebrig bleiben
        assertEquals(1, bank.getKonten().size(), "Es sollte genau 1 Konto uebrig bleiben");
        assertFalse(bank.getKonten().containsKey(a1));
        assertFalse(bank.getKonten().containsKey(a2));
        assertTrue(bank.getKonten().containsKey(b1));
        assertEquals(bob, bank.getKonten().get(b1).getInhaber());
    }

    @Test
    void kontenEinesKundenLoeschen_wirftNullPointerException_beiNullInhaber() {
        Bank bank = new Bank(12345002L);
        assertThrows(NullPointerException.class, () -> bank.kontenEinesKundenLoeschen(null),
                "Null-Inhaber sollte NullPointerException werfen");
    }

    @Test
    void kontenEinesKundenLoeschen_beeinflusstKontonummernzaehlerNicht() {
        Bank bank = new Bank(12345003L);
        Kunde alice = new Kunde("Alice", "Anders", "Allee 1", 1990, 1, 1);
        Kunde bob   = new Kunde("Bob", "Bauer", "Berg 2", 1991, 2, 2);

        long a1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),alice); // 1
        long b1 = bank.kontoErstellen(new GirokontoFabrik(bank.dispo_default),bob);   // 2
        long a2 = bank.kontoErstellen(new SparbuchFabrik(),alice);  // 3
        assertEquals(1L, a1);
        assertEquals(2L, b1);
        assertEquals(3L, a2);

        // Loesche alle Alice-Konten (1 und 3)
        int geloescht = bank.kontenEinesKundenLoeschen(alice);
        assertEquals(2, geloescht);
        assertFalse(bank.getKonten().containsKey(a1));
        assertFalse(bank.getKonten().containsKey(a2));
        assertTrue(bank.getKonten().containsKey(b1));

        // Neues Konto fuer Bob bekommt die naechste freie Nummer (4), keine Wiederverwendung von 1/3
        long b2 = bank.kontoErstellen(new SparbuchFabrik(),bob);
        assertEquals(4L, b2, "Kontonummernzaehler darf durch Massenloeschung nicht zurueckgesetzt werden");
    }
}
}