package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.management.Attribute;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das dauerhafte Speichern der Verwaltung ({@link Datenspeicher}).
 */
class DatenspeicherTest {

    @TempDir
    Path ordner;

    /**
     * Erzeugt gültige Kundendaten.
     * @return die Kundendaten
     */
    private static Kundendaten anna() {
        return new Kundendaten("Anna", "Schmidt", LocalDate.of(1990, 5, 1), "Berlin", "", "anna@example.org");
    }

    /**
     * Schreibt ein beliebiges Objekt serialisiert in eine Datei.
     * @param datei die Datei
     * @param objekt das Objekt
     * @throws IOException bei Schreibfehlern
     */
    private static void schreibe(Path datei, Object objekt) throws IOException {
        try (ObjectOutputStream aus = new ObjectOutputStream(Files.newOutputStream(datei))) {
            aus.writeObject(objekt);
        }
    }

    @Test
    void laden_ohneDatei_richtetStartbenutzerEin_undSpeichertSofort() throws IOException {
        Path datei = ordner.resolve("unter/ordner/verwaltung.ser");
        Bankverwaltung verwaltung = new Datenspeicher(datei).laden();

        assertTrue(Files.exists(datei), "Datei und Verzeichnisse werden angelegt");
        assertEquals(Datenspeicher.BANKLEITZAHL, verwaltung.getBankleitzahl());
        Benutzer start = verwaltung.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        assertTrue(start.isAktiv());
        assertTrue(start.getPasswortHash().startsWith("pbkdf2-sha256$"), "nur der Hash wird gespeichert");
        assertTrue(new Anmeldung(verwaltung).anmelden("m.alharethi", "1234".toCharArray()).isErfolgreich());
        assertEquals(Protokollaktion.ERSTEINRICHTUNG, verwaltung.getProtokoll().get(0).aktion());
    }

    @Test
    void aenderungen_werdenAutomatischGespeichert_undWiederGeladen() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        Bankverwaltung verwaltung = new Datenspeicher(datei).laden();
        Benutzer start = verwaltung.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        verwaltung.kundeAnlegen(start, anna());
        for (Kontoart art : Kontoart.values())
            verwaltung.kontoEroeffnen(start, "K000001", art, Waehrung.EURO, new Geldbetrag(100));
        verwaltung.kontoSperren(start, 1L);

        Bankverwaltung geladen = new Datenspeicher(datei).laden();

        assertEquals("Schmidt, Anna", geladen.findeKunde("K000001").orElseThrow().getName());
        assertEquals(Kontoart.values().length, geladen.getKonten().size());
        assertEquals(Kontostatus.GESPERRT, geladen.findeKonto(1L).orElseThrow().getStatus());
        assertEquals(verwaltung.getProtokoll(), geladen.getProtokoll());
        assertEquals(1, geladen.getBenutzer().size(), "kein zweiter Startbenutzer");
    }

    @Test
    void geladeneVerwaltung_speichertWeitereAenderungen() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        new Datenspeicher(datei).laden();
        Bankverwaltung zweiterStart = new Datenspeicher(datei).laden();
        Benutzer start = zweiterStart.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        zweiterStart.kundeAnlegen(start, anna());

        assertEquals(1, new Datenspeicher(datei).laden().getKunden().size());
    }

    @Test
    void speichern_hinterlaesstKeineTemporaerenDateien() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        Bankverwaltung verwaltung = new Datenspeicher(datei).laden();
        Benutzer start = verwaltung.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        verwaltung.kundeAnlegen(start, anna());
        try (Stream<Path> dateien = Files.list(ordner)) {
            assertEquals(1, dateien.count());
        }
    }

    @Test
    void speicherfehler_wirdAlsUncheckedIOExceptionGemeldet() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        Bankverwaltung verwaltung = new Datenspeicher(datei).laden();
        Benutzer start = verwaltung.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        // an Stelle der Datei steht jetzt ein nicht leeres Verzeichnis -> Ersetzen scheitert
        Files.delete(datei);
        Files.createDirectory(datei);
        Files.createFile(datei.resolve("blockiert"));

        UncheckedIOException e = assertThrows(UncheckedIOException.class,
                () -> verwaltung.kundeAnlegen(start, anna()));
        assertTrue(e.getMessage().startsWith("Die Änderung konnte nicht gespeichert werden"));
    }

    @Test
    void speichern_aufDasWurzelverzeichnis_wirftIOException() {
        Path wurzel = ordner.getRoot();
        assertThrows(IOException.class,
                () -> new Datenspeicher(wurzel).speichern(new Bankverwaltung(new Bank(1L))));
    }

    @Test
    void laden_beschaedigteDatei_wirftIOException() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        Files.writeString(datei, "keine Bankverwaltung");
        assertThrows(IOException.class, () -> new Datenspeicher(datei).laden());
    }

    @Test
    void laden_dateiMitAnderemObjekt_wirftInvalidObjectException() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        schreibe(datei, "nur ein String");
        assertThrows(InvalidObjectException.class, () -> new Datenspeicher(datei).laden());
    }

    @Test
    void laden_lehntFremdeKlassenAb() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        // serialisierbar, aber weder aus bankprojekt noch aus java.*
        schreibe(datei, new Attribute("name", "wert"));
        assertThrows(InvalidClassException.class, () -> new Datenspeicher(datei).laden());
    }

    @Test
    void standardDatei_liegtImBenutzerverzeichnis_oderKommtAusDerSystemeigenschaft() {
        String vorher = System.getProperty(Datenspeicher.DATEI_EIGENSCHAFT);
        try {
            System.clearProperty(Datenspeicher.DATEI_EIGENSCHAFT);
            assertEquals(Path.of(System.getProperty("user.home"), ".bankprojekt", "verwaltung.ser"),
                    Datenspeicher.standardDatei());
            System.setProperty(Datenspeicher.DATEI_EIGENSCHAFT, " ");
            assertEquals(Path.of(System.getProperty("user.home"), ".bankprojekt", "verwaltung.ser"),
                    Datenspeicher.standardDatei());
            System.setProperty(Datenspeicher.DATEI_EIGENSCHAFT, ordner.resolve("x.ser").toString());
            assertEquals(ordner.resolve("x.ser"), Datenspeicher.standardDatei());
        } finally {
            if (vorher == null)
                System.clearProperty(Datenspeicher.DATEI_EIGENSCHAFT);
            else
                System.setProperty(Datenspeicher.DATEI_EIGENSCHAFT, vorher);
        }
    }

    @Test
    void konstruktor_mitNull_wirftNullPointerException_undGetDateiLiefertDenPfad() {
        assertThrows(NullPointerException.class, () -> new Datenspeicher(null));
        Path datei = ordner.resolve("v.ser");
        assertEquals(datei, new Datenspeicher(datei).getDatei());
    }
}
