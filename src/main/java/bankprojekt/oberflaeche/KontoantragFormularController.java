package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundendaten;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Controller des Kontoantragsformulars im Kundenportal ({@code kontoantragFormular.fxml}).
 * Zu jeder Kontoart wird eine kurze Beschreibung gezeigt; für einen neuen
 * Neukundenantrag gibt es zusätzlich die persönlichen Daten.
 * @author m.alharethi
 */
public class KontoantragFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public KontoantragFormularController() {
    }

    /** Auswahl der Kontoart */
    @FXML
    private ComboBox<Kontoart> kontoart;
    /** Beschreibung der gewählten Kontoart */
    @FXML
    private Label beschreibung;
    /** Auswahl der Währung */
    @FXML
    private ComboBox<Waehrung> waehrung;
    /** Bereich mit den persönlichen Daten (nur Neukunden) */
    @FXML
    private VBox datenBereich;
    /** Controller des eingebundenen Kundenformulars */
    @FXML
    private KundeFormularController datenformularController;
    /** Anzeige von Eingabefehlern (eigene ID, weil das eingebundene Kundenformular ein „fehler“ hat) */
    @FXML
    private Label antragFehler;

    /**
     * Füllt Kontoarten und Währungen; die Beschreibung folgt der Auswahl.
     */
    @FXML
    private void initialize() {
        kontoart.getItems().setAll(Kontoart.values());
        waehrung.getItems().setAll(Waehrung.values());
        waehrung.setValue(Waehrung.EURO);
        kontoart.valueProperty().addListener((b, alt, neu) ->
                beschreibung.setText(neu == null ? "" : neu.getBeschreibung()));
    }

    /**
     * Zeigt zusätzlich die persönlichen Daten (neuer Neukundenantrag).
     * @param vorbelegung Daten des früheren Antrags; null für ein leeres Formular
     */
    public void setNeukunde(Kundendaten vorbelegung) {
        datenBereich.setVisible(true);
        datenBereich.setManaged(true);
        if (vorbelegung != null)
            datenformularController.setKundendaten(vorbelegung);
    }

    /**
     * Liefert die gewählte Kontoart.
     * @return die Kontoart oder null (das Kundenportal meldet das)
     */
    public Kontoart getKontoart() {
        return kontoart.getValue();
    }

    /**
     * Liefert die gewählte Währung.
     * @return die Währung
     */
    public Waehrung getWaehrung() {
        return waehrung.getValue();
    }

    /**
     * Liest die persönlichen Daten aus.
     * @return die geprüften Kundendaten
     * @throws IllegalArgumentException wenn eine Eingabe fehlt oder ungültig ist
     */
    public Kundendaten getKundendaten() {
        return datenformularController.auslesen();
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return antragFehler;
    }
}
