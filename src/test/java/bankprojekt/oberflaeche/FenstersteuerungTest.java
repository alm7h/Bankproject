package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Datenspeicher;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.Protokollaktion;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für den Wechsel zwischen Anmeldemaske und Hauptfenster ({@link Fenstersteuerung}).
 * Das Fenster wird nie angezeigt.
 */
@ExtendWith(JavaFxErweiterung.class)
class FenstersteuerungTest {

    @TempDir
    Path ordner;

    private Path datei;
    private Stage buehne;
    private Fenstersteuerung steuerung;

    @BeforeEach
    void setUp() {
        datei = ordner.resolve("verwaltung.ser");
        buehne = new Stage();
        steuerung = new Fenstersteuerung(buehne, new Datenspeicher(datei));
    }

    private Parent inhalt() {
        return buehne.getScene().getRoot();
    }

    private boolean zeigtAnmeldung() {
        return inhalt().lookup("#anmeldenKnopf") != null;
    }

    private boolean zeigtHauptfenster() {
        return inhalt().lookup("#abmeldenKnopf") != null;
    }

    /**
     * Meldet sich in der angezeigten Anmeldemaske an.
     * @param name Benutzername
     * @param passwort Passwort
     */
    private void anmelden(String name, String passwort) {
        FxHilfe.tippe(inhalt(), "benutzername", name);
        FxHilfe.tippe(inhalt(), "passwort", passwort);
        FxHilfe.druecke(inhalt(), "anmeldenKnopf");
    }

    @Test
    void starten_ohneDatei_richtetEin_undZeigtDieAnmeldemaske() {
        assertTrue(steuerung.starten());
        assertTrue(Files.exists(datei));
        assertTrue(zeigtAnmeldung());
        assertEquals("Bank – Anmeldung", buehne.getTitle());
        assertFalse(buehne.isResizable());
        assertEquals(520, buehne.getWidth());
        assertFalse(buehne.isShowing(), "angezeigt wird erst in BankAnwendung.start");
    }

    @Test
    void anmelden_zeigtDasHauptfenster_undAbmelden_wiederDieAnmeldemaske() throws IOException {
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");

        assertTrue(zeigtHauptfenster());
        assertEquals("Bankverwaltung – M. Alharethi", buehne.getTitle());
        assertTrue(buehne.isResizable());
        assertEquals(1000, buehne.getMinWidth());
        assertEquals(640, buehne.getMinHeight());

        FxHilfe.druecke(inhalt(), "abmeldenKnopf");

        assertTrue(zeigtAnmeldung());
        assertEquals(0, buehne.getMinWidth());
        List<Protokollaktion> aktionen = new Datenspeicher(datei).laden().getProtokoll().stream()
                .map(e -> e.aktion()).toList();
        assertEquals(List.of(Protokollaktion.ERSTEINRICHTUNG, Protokollaktion.ANMELDUNG, Protokollaktion.ABMELDUNG),
                aktionen);
    }

    @Test
    void nachDemAbmelden_istErneutesAnmeldenMoeglich() {
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        FxHilfe.druecke(inhalt(), "abmeldenKnopf");
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        assertTrue(zeigtHauptfenster());
    }

    @Test
    void falscheZugangsdaten_bleibenInDerAnmeldemaske() {
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "falsch");
        assertTrue(zeigtAnmeldung());
    }

    @Test
    void speicherfehler_beimAbmelden_wirdGemeldet_undAbgemeldetWirdTrotzdem() throws IOException {
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        Files.delete(datei);
        Files.createDirectories(datei.resolve("blockiert"));
        Dialogantworten antworten = new Dialogantworten().klicke("OK");

        FxHilfe.druecke(inhalt(), "abmeldenKnopf");

        assertTrue(zeigtAnmeldung());
        assertTrue(antworten.letzter().getContentText().startsWith("Die Änderung konnte nicht gespeichert werden"));
    }

    @Test
    void starten_mitBeschaedigterDatei_meldetDenFehler_undLiefertFalse() throws IOException {
        Files.writeString(datei, "keine Bankverwaltung");
        Dialogantworten antworten = new Dialogantworten().klicke("OK");

        assertFalse(steuerung.starten());

        assertEquals("Start nicht möglich", antworten.letzter().getHeaderText());
        assertTrue(antworten.letzter().getContentText().contains(datei.toString()));
        assertNull(buehne.getScene());
    }

    /**
     * Legt über die Verwaltung (mit Mitarbeiter) einen Kunden mit Portalzugang an.
     * @param startpasswort ob der Zugang ein Startpasswort der Bank bekommt (sonst eigenes)
     * @throws IOException bei Dateifehlern
     */
    private void kundeMitZugang(boolean startpasswort) throws IOException {
        Bankverwaltung v = new Datenspeicher(datei).laden();
        Benutzer chef = v.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow();
        Kundenakte anna = v.kundeAnlegen(chef, Testdaten.kunde("Anna", "Schmidt"));
        v.portalzugangEinrichten(chef, anna.getKundennummer(), "anna.s", "start1".toCharArray());
        if (!startpasswort)
            new Kundenportal(v).passwortAendern(v.findeKundenzugang("anna.s").orElseThrow(),
                    "start1".toCharArray(), "eigen1".toCharArray());
    }

    private boolean zeigtKundenportal() {
        return inhalt().lookup("#begruessung") != null;
    }

    @Test
    void kunde_kommtNachDerAnmeldungInsKundenportal() throws IOException {
        kundeMitZugang(false);
        steuerung.starten();
        anmelden("anna.s", "eigen1");
        assertTrue(zeigtKundenportal());
        assertEquals("Kundenportal – anna.s", buehne.getTitle());
        FxHilfe.druecke(inhalt(), "abmeldenKnopf");
        assertTrue(zeigtAnmeldung());
        List<Protokollaktion> aktionen = new Datenspeicher(datei).laden().getProtokoll().stream()
                .map(e -> e.aktion()).toList();
        assertEquals(Protokollaktion.ABMELDUNG, aktionen.get(aktionen.size() - 1));
    }

    @Test
    void startpasswort_mussVorDemPortalGeaendertWerden() throws IOException {
        kundeMitZugang(true);
        steuerung.starten();
        Dialogantworten antworten = new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            assertTrue(FxHilfe.text(formular, "hinweis").startsWith("Ihr Passwort wurde von der Bank vergeben"));
            FxHilfe.tippe(formular, "altesPasswort", "start1");
            FxHilfe.tippe(formular, "neuesPasswort", "eigen1");
            FxHilfe.tippe(formular, "wiederholung", "eigen1");
            Dialogantworten.knopf(d, "Speichern").fire();
        });
        anmelden("anna.s", "start1");
        assertEquals("Eigenes Passwort festlegen", antworten.letzter().getHeaderText());
        assertTrue(zeigtKundenportal());
        assertFalse(new Datenspeicher(datei).laden().findeKundenzugang("anna.s").orElseThrow().mussPasswortAendern());
    }

    @Test
    void startpasswort_abgebrochen_meldetWiederAb() throws IOException {
        kundeMitZugang(true);
        steuerung.starten();
        new Dialogantworten().klicke("Abbrechen");
        anmelden("anna.s", "start1");
        assertTrue(zeigtAnmeldung());
    }

    @Test
    void registrierung_wirdVonDerAnmeldemaskeGestartet_undAbbrechenFuehrtZurueck() {
        steuerung.starten();
        FxHilfe.druecke(inhalt(), "registrierenKnopf");
        assertNotNull(inhalt().lookup("#weiterKnopf"), "Registrierung");
        assertEquals("Bank – Konto eröffnen", buehne.getTitle());
        FxHilfe.druecke(inhalt(), "abbrechenKnopf");   // auf der Begrüßung ohne Rückfrage
        assertTrue(zeigtAnmeldung());
    }

    @Test
    void nachDerRegistrierung_istDerBenutzernameVorbelegt() {
        steuerung.starten();
        FxHilfe.druecke(inhalt(), "registrierenKnopf");
        Parent assistent = inhalt();
        FxHilfe.druecke(assistent, "weiterKnopf");
        Parent daten = FxHilfe.suche(assistent, "datenformular");
        FxHilfe.tippe(daten, "vorname", "Anna");
        FxHilfe.tippe(daten, "nachname", "Schmidt");
        FxHilfe.tippe(daten, "geburtsdatum", "01.05.1990");
        FxHilfe.tippe(daten, "adresse", "Berlin");
        FxHilfe.tippe(daten, "email", "anna@example.org");
        FxHilfe.druecke(assistent, "weiterKnopf");
        ((javafx.scene.control.RadioButton) assistent.lookupAll(".radio-button").iterator().next()).fire();
        FxHilfe.druecke(assistent, "weiterKnopf");
        FxHilfe.tippe(assistent, "benutzername", "anna.s");
        FxHilfe.tippe(assistent, "passwort", "geheim1");
        FxHilfe.tippe(assistent, "wiederholung", "geheim1");
        FxHilfe.druecke(assistent, "weiterKnopf");
        FxHilfe.druecke(assistent, "weiterKnopf");   // Antrag absenden
        FxHilfe.druecke(assistent, "weiterKnopf");   // Zur Anmeldung

        assertTrue(zeigtAnmeldung());
        assertEquals("anna.s", FxHilfe.<javafx.scene.control.TextField>suche(inhalt(), "benutzername").getText());
        anmelden("anna.s", "geheim1");
        assertTrue(zeigtKundenportal(), "Neukunde sieht schon das Portal (mit seinem Antrag)");
    }

    @Test
    void fensterSchliessen_beiAnmeldung_wirdAlsAbmeldungProtokolliert() throws IOException {
        steuerung.starten();
        buehne.fireEvent(new WindowEvent(buehne, WindowEvent.WINDOW_CLOSE_REQUEST));   // niemand angemeldet
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        buehne.fireEvent(new WindowEvent(buehne, WindowEvent.WINDOW_CLOSE_REQUEST));
        buehne.fireEvent(new WindowEvent(buehne, WindowEvent.WINDOW_CLOSE_REQUEST));   // nur einmal protokolliert
        List<Protokollaktion> aktionen = new Datenspeicher(datei).laden().getProtokoll().stream()
                .map(e -> e.aktion()).toList();
        assertEquals(List.of(Protokollaktion.ERSTEINRICHTUNG, Protokollaktion.ANMELDUNG, Protokollaktion.ABMELDUNG),
                aktionen);
    }

    @Test
    void fensterSchliessen_mitSpeicherfehler_wirftNicht() throws IOException {
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        Files.delete(datei);
        Files.createDirectories(datei.resolve("blockiert"));
        assertDoesNotThrow(() -> buehne.fireEvent(new WindowEvent(buehne, WindowEvent.WINDOW_CLOSE_REQUEST)));
    }

    @Test
    void konstruktor_mitNull_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Fenstersteuerung(null, new Datenspeicher(datei)));
        assertThrows(NullPointerException.class, () -> new Fenstersteuerung(buehne, null));
    }

    @Test
    void geladeneVerwaltung_wirdWeiterVerwendet() throws IOException {
        // Daten, die schon in der Datei stehen, sind nach dem Start sichtbar
        Bankverwaltung vorher = new Datenspeicher(datei).laden();
        vorher.kundeAnlegen(vorher.findeBenutzer(Datenspeicher.STARTBENUTZER).orElseThrow(),
                Testdaten.kunde("Anna", "Schmidt"));
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        assertEquals("1 Kunde", FxHilfe.<javafx.scene.control.Label>suche(inhalt(), "anzahl").getText());
    }
}
