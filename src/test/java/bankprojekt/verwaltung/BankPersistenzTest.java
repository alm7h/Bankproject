package bankprojekt.verwaltung;

import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Kinderkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.fabriken.AktienkontoFabrik;
import bankprojekt.fabriken.FestgeldkontoFabrik;
import bankprojekt.fabriken.GirokontoFabrik;
import bankprojekt.fabriken.KinderkontoFabrik;
import bankprojekt.fabriken.SparbuchFabrik;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für das Speichern und Einlesen einer {@link Bank}
 * ({@link Bank#speichern(java.io.OutputStream)} / {@link Bank#einlesen(java.io.InputStream)}).
 */
class BankPersistenzTest {

    private Bank bank;
    private final Kunde anna = new Kunde("Anna", "Schmidt", "Berlin", 1990, 5, 1);
    private final Kunde bert = new Kunde("Bert", "Müller", "Bonn", 1985, 3, 3);
    private long giro;
    private long spar;
    private long kind;
    private long fest;
    private long aktie;

    @BeforeEach
    void setUp() throws Exception {
        bank = new Bank(10010010L);
        giro = bank.kontoErstellen(new GirokontoFabrik(new Geldbetrag(750)), anna);
        spar = bank.kontoErstellen(new SparbuchFabrik(), bert);
        kind = bank.kontoErstellen(new KinderkontoFabrik(), anna);
        fest = bank.kontoErstellen(new FestgeldkontoFabrik(), bert);
        aktie = bank.kontoErstellen(new AktienkontoFabrik(), anna);

        bank.geldEinzahlen(spar, new Geldbetrag(1000));
        bank.geldEinzahlen(kind, new Geldbetrag(20));
        bank.geldEinzahlen(fest, new Geldbetrag(5000));
        bank.geldEinzahlen(aktie, new Geldbetrag(300));
        bank.geldAbheben(giro, new Geldbetrag(100.5));   // negativer Kontostand
        bank.getKonten().get(kind).sperren();
    }

    /**
     * Speichert die Bank in einen Byte-Puffer und liest sie wieder ein.
     * @param original die zu speichernde Bank
     * @return die eingelesene Kopie
     * @throws IOException bei einem Fehler beim Schreiben
     */
    private static Bank speichernUndEinlesen(Bank original) throws IOException {
        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        original.speichern(puffer);
        return Bank.einlesen(new ByteArrayInputStream(puffer.toByteArray()));
    }

    @Nested
    class RundreiseTests {
        @Test
        void einlesen_liefertNeueBankMitGleicherBankleitzahlUndKontonummern() throws IOException {
            Bank kopie = speichernUndEinlesen(bank);
            assertNotSame(bank, kopie);
            assertEquals(10010010L, kopie.getBankleitzahl());
            assertEquals(bank.getAlleKontonummern(), kopie.getAlleKontonummern());
        }

        @Test
        void einlesen_stelltDenLaufzeittypJedesKontosWiederHer() throws IOException {
            Bank kopie = speichernUndEinlesen(bank);
            assertInstanceOf(Girokonto.class, kopie.getKonten().get(giro));
            assertInstanceOf(Sparbuch.class, kopie.getKonten().get(spar));
            assertInstanceOf(Kinderkonto.class, kopie.getKonten().get(kind));
            assertInstanceOf(Festgeldkonto.class, kopie.getKonten().get(fest));
            assertInstanceOf(Aktienkonto.class, kopie.getKonten().get(aktie));
        }

        @Test
        void einlesen_stelltKontostaendeInhaberUndSperreWiederHer() throws IOException {
            Bank kopie = speichernUndEinlesen(bank);
            for (long nr : bank.getAlleKontonummern()) {
                Konto vorher = bank.getKonten().get(nr);
                Konto nachher = kopie.getKonten().get(nr);
                assertEquals(vorher.getKontostand(), nachher.getKontostand(), "Kontostand von " + nr);
                assertEquals(vorher.getInhaber(), nachher.getInhaber(), "Inhaber von " + nr);
                assertEquals(vorher.isGesperrt(), nachher.isGesperrt(), "Sperre von " + nr);
            }
            assertEquals(new Geldbetrag(-100.5), kopie.getKontostand(giro));
            assertTrue(kopie.getKonten().get(kind).isGesperrt());
        }

        @Test
        void einlesen_stelltKontospezifischeWerteWiederHer() throws Exception {
            Bank kopie = speichernUndEinlesen(bank);
            Girokonto g = (Girokonto) kopie.getKonten().get(giro);
            assertEquals(new Geldbetrag(750), g.getDispo());
            // Dispo greift weiterhin: -100,50 € + 649,50 € = -750 € ist erlaubt, mehr nicht
            assertFalse(g.abheben(new Geldbetrag(649.51)));
            assertTrue(g.abheben(new Geldbetrag(649.5)));
        }

        @Test
        void eingeleseneKundenSindDieselbenObjekteInAllenKonten() throws IOException {
            // Die Serialisierung erhält Objektidentitäten: Annas Konten teilen sich einen Kunden
            Bank kopie = speichernUndEinlesen(bank);
            Kunde annaGiro = kopie.getKonten().get(giro).getInhaber();
            assertSame(annaGiro, kopie.getKonten().get(kind).getInhaber());
            assertSame(annaGiro, kopie.getKonten().get(aktie).getInhaber());
            assertEquals("Berlin", annaGiro.getAdresse(), "Adresse steckt in einer transienten Property");
        }

        @Test
        void eingeleseneBank_vergibtDieNaechsteFreieKontonummerWeiter() throws IOException {
            Bank kopie = speichernUndEinlesen(bank);
            long neu = kopie.kontoErstellen(new SparbuchFabrik(), bert);
            assertEquals(aktie + 1, neu, "Der Kontonummernzähler muss mitgespeichert werden");
        }

        @Test
        void aenderungenAnDerKopie_wirkenNichtAufDasOriginal() throws Exception {
            Bank kopie = speichernUndEinlesen(bank);
            kopie.geldEinzahlen(spar, new Geldbetrag(1));
            kopie.kontoLoeschen(fest);
            assertEquals(new Geldbetrag(1000), bank.getKontostand(spar));
            assertTrue(bank.getKonten().containsKey(fest));
        }

        @Test
        void leereBank_laesstSichSpeichernUndEinlesen() throws IOException {
            Bank kopie = speichernUndEinlesen(new Bank(42));
            assertEquals(42, kopie.getBankleitzahl());
            assertTrue(kopie.getKonten().isEmpty());
        }
    }

    @Nested
    class FehlerfallTests {
        @Test
        void einlesen_liefertLeereBankMitBlzNull_beiLeererQuelle() {
            Bank ergebnis = Bank.einlesen(new ByteArrayInputStream(new byte[0]));
            assertEquals(0, ergebnis.getBankleitzahl());
            assertTrue(ergebnis.getKonten().isEmpty());
        }

        @Test
        void einlesen_liefertLeereBankMitBlzNull_beiUngueltigenDaten() {
            Bank ergebnis = Bank.einlesen(new ByteArrayInputStream("keine Bank".getBytes()));
            assertEquals(0, ergebnis.getBankleitzahl());
            assertTrue(ergebnis.getKonten().isEmpty());
        }

        @Test
        void einlesen_liefertLeereBankMitBlzNull_beiAbgeschnittenenDaten() throws IOException {
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            bank.speichern(puffer);
            byte[] haelfte = Arrays.copyOf(puffer.toByteArray(), puffer.size() / 2);
            Bank ergebnis = Bank.einlesen(new ByteArrayInputStream(haelfte));
            assertEquals(0, ergebnis.getBankleitzahl());
            assertTrue(ergebnis.getKonten().isEmpty());
        }

        @Test
        void einlesen_liefertLeereBankMitBlzNull_wennKeineBankGespeichertWurde() throws IOException {
            // Ein gültiger Objektstrom, der aber einen Kunden statt einer Bank enthält
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
                out.writeObject(anna);
            }
            Bank ergebnis = Bank.einlesen(new ByteArrayInputStream(puffer.toByteArray()));
            assertEquals(0, ergebnis.getBankleitzahl());
            assertTrue(ergebnis.getKonten().isEmpty());
        }
    }

    /**
     * Die Ströme werden bewusst nicht geschlossen, damit mehrere Banken in
     * aufeinanderfolgende Einträge einer ZIP-Datei geschrieben werden können.
     */
    @Nested
    class StromTests {
        @Test
        void speichern_schliesstDenStromNicht() throws IOException {
            ByteArrayOutputStream ziel = spy(new ByteArrayOutputStream());
            bank.speichern(ziel);
            verify(ziel, never()).close();
            assertTrue(ziel.size() > 0, "Die Daten müssen bereits geschrieben (geflusht) sein");
        }

        @Test
        void einlesen_schliesstDenStromNicht() throws IOException {
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            bank.speichern(puffer);
            ByteArrayInputStream quelle = spy(new ByteArrayInputStream(puffer.toByteArray()));
            Bank.einlesen(quelle);
            verify(quelle, never()).close();
        }

        @Test
        void zweiBanken_inEinerZipDatei_speichernUndEinlesen() throws IOException {
            Bank zweite = new Bank(20020020L);
            long clara = zweite.kontoErstellen(new SparbuchFabrik(), new Kunde("Clara", "Weber", "Köln", 2000, 7, 7));
            zweite.geldEinzahlen(clara, new Geldbetrag(42));

            ByteArrayOutputStream zipPuffer = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipPuffer)) {
                zip.putNextEntry(new ZipEntry("bank1.ser"));
                bank.speichern(zip);
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("bank2.ser"));
                zweite.speichern(zip);
                zip.closeEntry();
            }

            Bank eins;
            Bank zwei;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipPuffer.toByteArray()))) {
                assertEquals("bank1.ser", zip.getNextEntry().getName());
                eins = Bank.einlesen(zip);
                assertEquals("bank2.ser", zip.getNextEntry().getName());
                zwei = Bank.einlesen(zip);
                assertNull(zip.getNextEntry());
            }

            assertEquals(10010010L, eins.getBankleitzahl());
            assertEquals(5, eins.getKonten().size());
            assertEquals(20020020L, zwei.getBankleitzahl());
            assertEquals(new Geldbetrag(42), zwei.getKontostand(clara));
        }
    }
}
