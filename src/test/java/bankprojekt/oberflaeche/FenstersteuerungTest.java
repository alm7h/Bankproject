package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Datenspeicher;
import bankprojekt.verwaltung.Protokollaktion;
import javafx.scene.Parent;
import javafx.stage.Stage;
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
        assertEquals("Bankverwaltung – Anmeldung", buehne.getTitle());
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
                bankprojekt.verwaltung.Testdaten.kunde("Anna", "Schmidt"));
        steuerung.starten();
        anmelden(Datenspeicher.STARTBENUTZER, "1234");
        assertEquals("1 Kunde", FxHilfe.<javafx.scene.control.Label>suche(inhalt(), "anzahl").getText());
    }
}
