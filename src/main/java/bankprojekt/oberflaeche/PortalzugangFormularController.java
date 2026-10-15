package bankprojekt.oberflaeche;

import java.util.Arrays;

import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenzugang;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller des Formulars für den Portalzugang eines Kunden
 * ({@code portalzugangFormular.fxml}): neuen Zugang einrichten oder das Passwort eines
 * bestehenden Zugangs zurücksetzen. In beiden Fällen muss der Kunde das Passwort bei
 * der nächsten Anmeldung ändern.
 * @author m.alharethi
 */
public class PortalzugangFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public PortalzugangFormularController() {
    }

    /** Anzeige des Kunden */
    @FXML
    private Label kunde;
    /** Eingabe Benutzername (beim Zurücksetzen nur angezeigt) */
    @FXML
    private TextField benutzername;
    /** Beschriftung des Passwortfelds */
    @FXML
    private Label passwortBeschriftung;
    /** Eingabe Passwort */
    @FXML
    private PasswordField passwort;
    /** Eingabe Wiederholung */
    @FXML
    private PasswordField wiederholung;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Stellt das Formular ein.
     * @param kundenakte der Kunde
     * @param zugang sein bestehender Zugang (Passwort zurücksetzen) oder null (einrichten)
     */
    public void setKunde(Kundenakte kundenakte, Kundenzugang zugang) {
        kunde.setText(kundenakte.toString());
        if (zugang != null) {
            benutzername.setText(zugang.getBenutzername());
            benutzername.setDisable(true);
            passwortBeschriftung.setText("Neues Passwort *");
        }
    }

    /**
     * Liefert den eingegebenen Benutzernamen (geprüft wird er von der Verwaltung).
     * @return der Benutzername
     */
    public String getBenutzername() {
        return benutzername.getText();
    }

    /**
     * Liefert das Passwort, wenn die Wiederholung übereinstimmt.
     * @return das Passwort
     * @throws IllegalArgumentException wenn die Wiederholung abweicht
     */
    public char[] getPasswort() {
        char[] eins = passwort.getText().toCharArray();
        char[] zwei = wiederholung.getText().toCharArray();
        try {
            if (!Arrays.equals(eins, zwei))
                throw new IllegalArgumentException("Die beiden Passwörter stimmen nicht überein.");
            return eins.clone();
        } finally {
            Arrays.fill(eins, '\0');
            Arrays.fill(zwei, '\0');
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
