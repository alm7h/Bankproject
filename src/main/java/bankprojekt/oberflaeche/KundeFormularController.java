package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundendaten;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller des Kundenformulars ({@code kundeFormular.fxml}) zum Anlegen und
 * Bearbeiten eines Kunden. Geprüft werden die Eingaben von {@link Kundendaten}.
 * @author m.alharethi
 */
public class KundeFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public KundeFormularController() {
    }

    /** Anzeige der Kundennummer */
    @FXML
    private Label kundennummer;
    /** Eingabe Vorname */
    @FXML
    private TextField vorname;
    /** Eingabe Nachname */
    @FXML
    private TextField nachname;
    /** Eingabe Geburtsdatum (TT.MM.JJJJ) */
    @FXML
    private TextField geburtsdatum;
    /** Eingabe Adresse */
    @FXML
    private TextArea adresse;
    /** Eingabe Telefon */
    @FXML
    private TextField telefon;
    /** Eingabe E-Mail */
    @FXML
    private TextField email;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Ein neuer Kunde hat noch keine Kundennummer.
     */
    @FXML
    private void initialize() {
        kundennummer.setText("wird automatisch vergeben");
    }

    /**
     * Füllt das Formular mit den Daten eines vorhandenen Kunden.
     * @param kunde der zu bearbeitende Kunde
     */
    public void setKunde(Kundenakte kunde) {
        kundennummer.setText(kunde.getKundennummer());
        vorname.setText(kunde.getVorname());
        nachname.setText(kunde.getNachname());
        geburtsdatum.setText(Darstellung.datum(kunde.getGeburtsdatum()));
        adresse.setText(kunde.getAdresse());
        telefon.setText(kunde.getTelefon());
        email.setText(kunde.getEmail());
    }

    /**
     * Liest die Eingaben aus und prüft sie.
     * @return die geprüften Kundendaten
     * @throws IllegalArgumentException wenn eine Eingabe fehlt oder ungültig ist
     */
    public Kundendaten auslesen() {
        return new Kundendaten(vorname.getText(), nachname.getText(),
                Darstellung.datumLesen(geburtsdatum.getText()),
                adresse.getText(), telefon.getText(), email.getText());
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
