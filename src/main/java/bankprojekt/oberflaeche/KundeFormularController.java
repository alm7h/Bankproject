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
        setKundendaten(kunde.getKundendaten());
    }

    /**
     * Füllt die Eingabefelder mit Stammdaten vor (z. B. aus einem früheren Antrag).
     * @param daten die Stammdaten
     */
    public void setKundendaten(Kundendaten daten) {
        vorname.setText(daten.vorname());
        nachname.setText(daten.nachname());
        geburtsdatum.setText(Darstellung.datum(daten.geburtsdatum()));
        adresse.setText(daten.adresse());
        telefon.setText(daten.telefon());
        email.setText(daten.email());
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
