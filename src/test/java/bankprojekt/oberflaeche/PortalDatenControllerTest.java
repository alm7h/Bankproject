package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für „Meine Daten" im Kundenportal ({@link PortalDatenController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class PortalDatenControllerTest {

    private Kundenakte anna;
    private Kundenzugang zugang;
    private Parent ansicht;

    @BeforeEach
    void setUp() {
        Bankverwaltung verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        Benutzer admin = Testdaten.admin(verwaltung);
        anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
        Dialoge.Geladen<PortalDatenController> geladen = FxHilfe.laden("portalDaten.fxml");
        ansicht = geladen.wurzel();
        geladen.controller().setSitzung(new Kundensitzung(new Kundenportal(verwaltung), zugang));
        geladen.controller().aktualisieren();
    }

    private Label meldung() {
        return FxHilfe.suche(ansicht, "meldung");
    }

    @Test
    void zeigtStammdaten_undKontaktdaten() {
        assertEquals("K000001", FxHilfe.text(ansicht, "kundennummer"));
        assertEquals("Anna Schmidt", FxHilfe.text(ansicht, "name"));
        assertEquals("01.05.1990", FxHilfe.text(ansicht, "geburtsdatum"));
        assertEquals("anna.s", FxHilfe.text(ansicht, "benutzername"));
        assertEquals("Berlin", FxHilfe.<TextArea>suche(ansicht, "adresse").getText());
        assertEquals("anna@example.org", FxHilfe.<TextField>suche(ansicht, "email").getText());
        assertEquals("", meldung().getText());
    }

    @Test
    void kontaktdatenSpeichern_mitErfolgsmeldung() {
        FxHilfe.tippe(ansicht, "adresse", "Hamburg");
        FxHilfe.tippe(ansicht, "telefon", "040 123456");
        FxHilfe.druecke(ansicht, "speichernKnopf");
        assertEquals("Hamburg", anna.getAdresse());
        assertEquals("040 123456", anna.getTelefon());
        assertEquals("Ihre Kontaktdaten wurden gespeichert.", meldung().getText());
        assertTrue(meldung().getStyleClass().contains("erfolgsmeldung"));
    }

    @Test
    void ungueltigeKontaktdaten_mitFehlermeldung() {
        FxHilfe.tippe(ansicht, "email", "kaputt");
        FxHilfe.druecke(ansicht, "speichernKnopf");
        assertTrue(meldung().getText().contains("E-Mail"));
        assertTrue(meldung().getStyleClass().contains("fehlermeldung"));
        assertEquals("anna@example.org", anna.getEmail());
    }

    @Test
    void passwortAendern_imDialog() {
        Dialogantworten antworten = new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            FxHilfe.tippe(formular, "altesPasswort", "falsch");
            FxHilfe.tippe(formular, "neuesPasswort", "neu-1234");
            FxHilfe.tippe(formular, "wiederholung", "neu-1234");
            Dialogantworten.knopf(d, "Speichern").fire();
            assertEquals("Das bisherige Passwort ist falsch.", FxHilfe.text(formular, "fehler"));
            FxHilfe.tippe(formular, "altesPasswort", Testdaten.PASSWORT);
            Dialogantworten.knopf(d, "Speichern").fire();
        });
        FxHilfe.druecke(ansicht, "passwortKnopf");
        assertEquals("Passwort ändern", antworten.letzter().getHeaderText());
        assertEquals("Ihr Passwort wurde geändert.", meldung().getText());
    }

    @Test
    void passwortAendern_abgebrochen() {
        new Dialogantworten().klicke("Abbrechen");
        FxHilfe.druecke(ansicht, "passwortKnopf");
        assertEquals("", meldung().getText());
    }
}
