package bankprojekt.oberflaeche;

import java.util.Arrays;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * Controller des Formulars zum Ändern des eigenen Passworts ({@code passwortFormular.fxml}).
 * Geprüft wird das Passwort vom {@link bankprojekt.verwaltung.Kundenportal}; hier nur,
 * ob die Wiederholung stimmt.
 * @author m.alharethi
 */
public class PasswortFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public PasswortFormularController() {
    }

    /** Hinweis über den Feldern */
    @FXML
    private Label hinweis;
    /** Eingabe bisheriges Passwort */
    @FXML
    private PasswordField altesPasswort;
    /** Eingabe neues Passwort */
    @FXML
    private PasswordField neuesPasswort;
    /** Eingabe Wiederholung */
    @FXML
    private PasswordField wiederholung;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Setzt den Hinweis über den Feldern (z. B. beim erzwungenen Wechsel).
     * @param text der Hinweis
     */
    public void setHinweis(String text) {
        hinweis.setText(text);
    }

    /**
     * Liefert das bisherige Passwort.
     * @return das eingegebene bisherige Passwort
     */
    public char[] getAltesPasswort() {
        return altesPasswort.getText().toCharArray();
    }

    /**
     * Liefert das neue Passwort, wenn die Wiederholung übereinstimmt.
     * @return das neue Passwort
     * @throws IllegalArgumentException wenn die Wiederholung abweicht
     */
    public char[] getNeuesPasswort() {
        char[] neu = neuesPasswort.getText().toCharArray();
        char[] nochmal = wiederholung.getText().toCharArray();
        try {
            if (!Arrays.equals(neu, nochmal))
                throw new IllegalArgumentException("Die beiden neuen Passwörter stimmen nicht überein.");
            return neu.clone();
        } finally {
            Arrays.fill(neu, '\0');
            Arrays.fill(nochmal, '\0');
        }
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
