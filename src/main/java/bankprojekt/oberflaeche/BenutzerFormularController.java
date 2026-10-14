package bankprojekt.oberflaeche;

import java.util.Arrays;

import bankprojekt.verwaltung.Benutzer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller des Benutzerformulars ({@code benutzerFormular.fxml}). Je nach
 * {@link Modus} sind andere Felder bearbeitbar bzw. sichtbar.
 * @author m.alharethi
 */
public class BenutzerFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public BenutzerFormularController() {
    }

    /**
     * Verwendungszweck des Formulars.
     */
    public enum Modus {
        /** neuen Benutzer anlegen: alle Felder */
        NEU,
        /** Benutzer bearbeiten: nur der Anzeigename */
        BEARBEITEN,
        /** Passwort zurücksetzen: nur das Passwort */
        PASSWORT
    }

    /** Eingabe Benutzername */
    @FXML
    private TextField benutzername;
    /** Eingabe Anzeigename */
    @FXML
    private TextField anzeigename;
    /** Beschriftung des Passwortfelds */
    @FXML
    private Label passwortBeschriftung;
    /** Eingabe Passwort */
    @FXML
    private PasswordField passwort;
    /** Beschriftung der Passwortwiederholung */
    @FXML
    private Label wiederholungBeschriftung;
    /** Eingabe Passwortwiederholung */
    @FXML
    private PasswordField wiederholung;
    /** Hinweis zu den Regeln */
    @FXML
    private Label hinweis;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Stellt das Formular auf einen Verwendungszweck ein.
     * @param modus der Verwendungszweck
     * @param benutzer der vorhandene Benutzer; bei {@link Modus#NEU} null
     */
    public void setModus(Modus modus, Benutzer benutzer) {
        if (benutzer != null) {
            benutzername.setText(benutzer.getBenutzername());
            anzeigename.setText(benutzer.getAnzeigename());
        }
        benutzername.setDisable(modus != Modus.NEU);
        anzeigename.setDisable(modus == Modus.PASSWORT);
        boolean mitPasswort = modus != Modus.BEARBEITEN;
        for (Node n : new Node[]{passwortBeschriftung, passwort, wiederholungBeschriftung, wiederholung}) {
            n.setVisible(mitPasswort);
            n.setManaged(mitPasswort);
        }
        if (modus == Modus.PASSWORT)
            passwortBeschriftung.setText("Neues Passwort *");
        hinweis.setText(switch (modus) {
            case NEU -> "Benutzername: 3–30 Zeichen (Buchstaben, Ziffern, . _ -). Passwort: mindestens 4 Zeichen.";
            case BEARBEITEN -> "Der Benutzername kann nicht geändert werden.";
            case PASSWORT -> "Das neue Passwort muss mindestens 4 Zeichen lang sein.";
        });
    }

    /**
     * Liefert den eingegebenen Benutzernamen (geprüft wird er von der Verwaltung).
     * @return der Benutzername
     */
    public String getBenutzername() {
        return benutzername.getText();
    }

    /**
     * Liefert den eingegebenen Anzeigenamen (geprüft wird er von der Verwaltung).
     * @return der Anzeigename
     */
    public String getAnzeigename() {
        return anzeigename.getText();
    }

    /**
     * Liefert das eingegebene Passwort, wenn beide Eingaben übereinstimmen.
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
