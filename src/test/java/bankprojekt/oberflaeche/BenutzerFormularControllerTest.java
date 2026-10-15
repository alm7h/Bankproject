package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das Benutzerformular in seinen drei Modi ({@link BenutzerFormularController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class BenutzerFormularControllerTest {

    private Parent formular;
    private BenutzerFormularController controller;
    private Benutzer admin;

    @BeforeEach
    void setUp() {
        Dialoge.Geladen<BenutzerFormularController> geladen = FxHilfe.laden("benutzerFormular.fxml");
        formular = geladen.wurzel();
        controller = geladen.controller();
        Bankverwaltung verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
    }

    private boolean passwortSichtbar() {
        return FxHilfe.suche(formular, "passwort").isVisible()
                && FxHilfe.suche(formular, "passwort").isManaged()
                && FxHilfe.suche(formular, "wiederholung").isVisible()
                && FxHilfe.suche(formular, "passwortBeschriftung").isVisible()
                && FxHilfe.suche(formular, "wiederholungBeschriftung").isVisible();
    }

    @Test
    void neu_alleFelderBearbeitbar() {
        controller.setModus(BenutzerFormularController.Modus.NEU, null);
        assertFalse(FxHilfe.suche(formular, "benutzername").isDisabled());
        assertFalse(FxHilfe.suche(formular, "anzeigename").isDisabled());
        assertTrue(passwortSichtbar());
        assertEquals("Passwort *", FxHilfe.text(formular, "passwortBeschriftung"));
        assertTrue(FxHilfe.text(formular, "hinweis").startsWith("Benutzername: 3–30 Zeichen"));
        FxHilfe.tippe(formular, "benutzername", "erika");
        FxHilfe.tippe(formular, "anzeigename", "Erika");
        assertEquals("erika", controller.getBenutzername());
        assertEquals("Erika", controller.getAnzeigename());
        assertSame(FxHilfe.suche(formular, "fehler"), controller.getFehlerfeld());
    }

    @Test
    void bearbeiten_nurAnzeigename_ohnePasswort() {
        controller.setModus(BenutzerFormularController.Modus.BEARBEITEN, admin);
        assertTrue(FxHilfe.suche(formular, "benutzername").isDisabled());
        assertFalse(FxHilfe.suche(formular, "anzeigename").isDisabled());
        assertFalse(FxHilfe.suche(formular, "passwort").isVisible());
        assertFalse(FxHilfe.suche(formular, "passwort").isManaged());
        assertEquals("admin", FxHilfe.<TextField>suche(formular, "benutzername").getText());
        assertEquals("Admin", FxHilfe.<TextField>suche(formular, "anzeigename").getText());
        assertEquals("Der Benutzername kann nicht geändert werden.", FxHilfe.text(formular, "hinweis"));
    }

    @Test
    void passwort_nurDasPasswort() {
        controller.setModus(BenutzerFormularController.Modus.PASSWORT, admin);
        assertTrue(FxHilfe.suche(formular, "benutzername").isDisabled());
        assertTrue(FxHilfe.suche(formular, "anzeigename").isDisabled());
        assertTrue(passwortSichtbar());
        assertEquals("Neues Passwort *", FxHilfe.text(formular, "passwortBeschriftung"));
        assertEquals("Das neue Passwort muss mindestens 4 Zeichen lang sein.", FxHilfe.text(formular, "hinweis"));
    }

    @Test
    void getPasswort_nurBeiGleicherWiederholung() {
        controller.setModus(BenutzerFormularController.Modus.NEU, null);
        FxHilfe.tippe(formular, "passwort", "geheim");
        FxHilfe.tippe(formular, "wiederholung", "Geheim");
        assertEquals("Die beiden Passwörter stimmen nicht überein.",
                assertThrows(IllegalArgumentException.class, controller::getPasswort).getMessage());
        FxHilfe.tippe(formular, "wiederholung", "geheim");
        assertArrayEquals("geheim".toCharArray(), controller.getPasswort());
    }
}
