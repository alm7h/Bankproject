package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Waehrung;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.management.Attribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * Kopiert die Testdatei im alten Format (Beträge als double, erzeugt von
     * {@link AlteDateienErzeuger} auf Commit d7ba47f) in das temporäre Verzeichnis.
     * @return die Kopie
     * @throws IOException bei Lese- oder Schreibfehlern
     */
    private Path alteDatei() throws IOException {
        Path datei = ordner.resolve("verwaltung.ser");
        try (InputStream alt = DatenspeicherTest.class.getResourceAsStream("verwaltung-double.ser")) {
            assertNotNull(alt, "Testdatei fehlt");
            Files.copy(alt, datei);
        }
        return datei;
    }

    /**
     * Prüft Währung und Betrag eines Geldbetrags exakt, ohne Toleranz.
     * @param betrag erwarteter Betrag
     * @param waehrung erwartete Währung
     * @param ist der geprüfte Geldbetrag
     */
    private static void assertBetrag(double betrag, Waehrung waehrung, Geldbetrag ist) {
        assertEquals(waehrung, ist.getWaehrung());
        assertEquals(betrag, ist.getBetrag());
    }

    @Test
    void laden_dateiMitBetraegenAlsDouble_liefertAlleBetraegeUnveraendert() throws IOException {
        Bankverwaltung geladen = new Datenspeicher(alteDatei()).laden();

        assertEquals("Hoffmann, Lena", geladen.findeKunde("K000001").orElseThrow().getName());
        assertEquals("Becker, Jonas", geladen.findeKunde("K000002").orElseThrow().getName());
        assertBetrag(1234.56, Waehrung.EURO, geladen.kontoHolen(1).getKontostand());
        assertBetrag(500, Waehrung.EURO, ((Girokonto) geladen.kontoHolen(1).getKonto()).getDispo());
        assertBetrag(-20.5, Waehrung.EURO, geladen.kontoHolen(2).getKontostand());
        assertBetrag(300, Waehrung.EURO, ((Girokonto) geladen.kontoHolen(2).getKonto()).getDispo());
        assertBetrag(6149.5, Waehrung.DENAR, geladen.kontoHolen(3).getKontostand());
        assertBetrag(10_000, Waehrung.EURO, geladen.kontoHolen(4).getKontostand());
        assertBetrag(2500, Waehrung.EURO,
                ((Festgeldkonto) geladen.kontoHolen(4).getKonto()).getGekuendigterBetrag());

        // Kontoauszug des Sparbuchs: Einzahlung in Denar, Einzahlung in Euro, Auszahlung
        List<Buchung> auszug = geladen.getBuchungen(3);
        assertEquals(List.of(6150.0, 615.0, -615.5), auszug.stream().map(b -> b.betrag().getBetrag()).toList());
        assertEquals(List.of(6150.0, 6765.0, 6149.5),
                auszug.stream().map(b -> b.saldoDanach().getBetrag()).toList());
        assertTrue(auszug.stream().allMatch(b -> b.betrag().getWaehrung() == Waehrung.DENAR
                && b.saldoDanach().getWaehrung() == Waehrung.DENAR));
    }

    @Test
    void alteDatei_laesstSichWeiterbearbeitenUndWiederLaden() throws IOException {
        Path datei = alteDatei();
        Bankverwaltung geladen = new Datenspeicher(datei).laden();
        Benutzer admin = Testdaten.admin(geladen);
        geladen.bareinzahlung(admin, 1, new Geldbetrag(0.44), null);
        geladen.barauszahlung(admin, 3, new Geldbetrag(0.5, Waehrung.DENAR), null);

        Bankverwaltung wieder = new Datenspeicher(datei).laden();

        assertBetrag(1235, Waehrung.EURO, wieder.kontoHolen(1).getKontostand());
        assertBetrag(6149, Waehrung.DENAR, wieder.kontoHolen(3).getKontostand());
        assertEquals(4, wieder.getBuchungen(3).size());
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
