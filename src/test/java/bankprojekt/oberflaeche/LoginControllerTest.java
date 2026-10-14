package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Datenspeicher;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Anmeldemaske ({@link LoginController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class LoginControllerTest {

    @TempDir
    Path ordner;

    private VerstellbareUhr uhr;
    private Bankverwaltung verwaltung;
    private Anmeldung anmeldung;
    private Parent maske;
    private LoginController controller;
    private final List<Benutzer> angemeldet = new ArrayList<>();

    @BeforeEach
    void setUp() {
        uhr = new VerstellbareUhr();
        verwaltung = Testdaten.verwaltung(uhr);
        anmeldung = new Anmeldung(verwaltung);
        maskeLaden();
    }

    @AfterEach
    void tearDown() {
        controller.beenden();
    }

    /**
     * Lädt eine frische Anmeldemaske für die aktuelle Anmeldung.
     */
    private void maskeLaden() {
        Dialoge.Geladen<LoginController> geladen = FxHilfe.laden("login.fxml");
        maske = geladen.wurzel();
        controller = geladen.controller();
        controller.setAnmeldung(anmeldung, angemeldet::add);
    }

    /**
     * Gibt Zugangsdaten ein und drückt „Anmelden".
     * @param name Benutzername
     * @param passwort Passwort
     */
    private void anmelden(String name, String passwort) {
        FxHilfe.tippe(maske, "benutzername", name);
        FxHilfe.tippe(maske, "passwort", passwort);
        FxHilfe.druecke(maske, "anmeldenKnopf");
    }

    private String meldung() {
        return FxHilfe.<Label>suche(maske, "meldung").getText();
    }

    private boolean eingabeGesperrt() {
        return FxHilfe.suche(maske, "benutzername").isDisabled()
                && FxHilfe.suche(maske, "passwort").isDisabled()
                && FxHilfe.suche(maske, "anmeldenKnopf").isDisabled();
    }

    @Test
    void anmeldenKnopf_istStandardknopf_EnterMeldetAlsoAn() {
        assertTrue(FxHilfe.<Button>suche(maske, "anmeldenKnopf").isDefaultButton());
        assertFalse(eingabeGesperrt());
    }

    @Test
    void richtigeZugangsdaten_meldenAn() {
        anmelden("Admin", Testdaten.PASSWORT);
        assertEquals(List.of(Testdaten.admin(verwaltung)), angemeldet);
        assertEquals("", meldung());
        assertEquals("", FxHilfe.<PasswordField>suche(maske, "passwort").getText());
    }

    @Test
    void falschesPasswort_meldetFehler_undLeertDasPasswortfeld() {
        anmelden("admin", "falsch");
        assertTrue(angemeldet.isEmpty());
        assertEquals("Benutzername oder Passwort ist falsch. Noch 4 Versuche bis zur vorübergehenden Sperre.",
                meldung());
        assertEquals("", FxHilfe.<PasswordField>suche(maske, "passwort").getText());
        assertEquals("admin", FxHilfe.<TextField>suche(maske, "benutzername").getText(), "Name bleibt stehen");
    }

    @Test
    void vierterFehlversuch_meldetNochEinenVersuch_imSingular() {
        for (int i = 0; i < 4; i++)
            anmelden("admin", "falsch");
        assertTrue(meldung().endsWith("Noch 1 Versuch bis zur vorübergehenden Sperre."), meldung());
    }

    @Test
    void deaktivierterBenutzer_bekommtEigeneMeldung() {
        Benutzer admin = Testdaten.admin(verwaltung);
        verwaltung.benutzerAnlegen(admin, "bert", "Bert", "abcd".toCharArray());
        verwaltung.benutzerDeaktivieren(admin, "bert");
        anmelden("bert", "abcd");
        assertTrue(angemeldet.isEmpty());
        assertTrue(meldung().startsWith("Dieser Zugang ist deaktiviert."), meldung());
    }

    @Test
    void fuenfFehlversuche_sperrenDieEingabe_mitCountdown_bisZumAblauf() {
        for (int i = 0; i < Anmeldung.MAX_FEHLVERSUCHE; i++)
            anmelden("admin", "falsch");
        assertTrue(eingabeGesperrt());
        assertEquals("Zu viele Fehlversuche. Die Anmeldung ist noch 60 Sekunden gesperrt.", meldung());

        uhr.vorstellen(Duration.ofSeconds(20));
        controller.sperreAktualisieren();
        assertTrue(meldung().contains("noch 40 Sekunden"), meldung());

        uhr.vorstellen(Duration.ofMillis(39_500));
        controller.sperreAktualisieren();
        assertTrue(meldung().contains("noch 1 Sekunden"), "Restzeit wird aufgerundet: " + meldung());
        assertTrue(eingabeGesperrt());

        uhr.vorstellen(Duration.ofMillis(500));
        controller.sperreAktualisieren();
        assertFalse(eingabeGesperrt());
        assertEquals("Sie können sich wieder anmelden.", meldung());

        anmelden("admin", Testdaten.PASSWORT);
        assertEquals(1, angemeldet.size());
    }

    @Test
    void neueMaske_waehrendEinerSperre_zeigtDieSperreSofort() {
        for (int i = 0; i < Anmeldung.MAX_FEHLVERSUCHE; i++)
            anmelden("admin", "falsch");
        controller.beenden();
        maskeLaden();
        assertTrue(eingabeGesperrt());
        assertTrue(meldung().startsWith("Zu viele Fehlversuche."));
    }

    @Test
    void erneuteSperre_ersetztDenLaufendenCountdown() {
        for (int i = 0; i < Anmeldung.MAX_FEHLVERSUCHE; i++)
            anmelden("admin", "falsch");
        uhr.vorstellen(Anmeldung.SPERRDAUER);
        controller.sperreAktualisieren();
        for (int i = 0; i < Anmeldung.MAX_FEHLVERSUCHE; i++)
            anmelden("admin", "falsch");
        assertTrue(eingabeGesperrt());
        assertEquals("Zu viele Fehlversuche. Die Anmeldung ist noch 60 Sekunden gesperrt.", meldung());
    }

    @Test
    void beenden_ohneCountdown_istUnproblematisch() {
        controller.beenden();
        controller.beenden();
        assertFalse(eingabeGesperrt());
    }

    @Test
    void speicherfehler_beiDerAnmeldung_wirdAngezeigt() throws IOException {
        Path datei = ordner.resolve("v.ser");
        verwaltung = new Datenspeicher(datei).laden();
        anmeldung = new Anmeldung(verwaltung);
        maskeLaden();
        // an Stelle der Datei steht ein nicht leeres Verzeichnis -> Speichern scheitert
        Files.delete(datei);
        Files.createDirectories(datei.resolve("blockiert"));

        anmelden(Datenspeicher.STARTBENUTZER, "falsch");

        assertTrue(meldung().startsWith("Die Änderung konnte nicht gespeichert werden"), meldung());
        assertEquals("", FxHilfe.<PasswordField>suche(maske, "passwort").getText());
        assertTrue(angemeldet.isEmpty());
    }

    @Test
    void setAnmeldung_mitNull_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> controller.setAnmeldung(null, b -> { }));
        assertThrows(NullPointerException.class, () -> controller.setAnmeldung(anmeldung, null));
    }
}
