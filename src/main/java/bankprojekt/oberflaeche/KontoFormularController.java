package bankprojekt.oberflaeche;

import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller des Formulars zum Eröffnen eines Kontos ({@code kontoFormular.fxml}).
 * Das Dispo-Feld ist nur beim Girokonto aktiv.
 * @author m.alharethi
 */
public class KontoFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public KontoFormularController() {
    }

    /** Auswahl des Kunden (nur aktive Kunden) */
    @FXML
    private ComboBox<Kundenakte> kunde;
    /** Auswahl der Kontoart */
    @FXML
    private ComboBox<Kontoart> kontoart;
    /** Auswahl der Währung */
    @FXML
    private ComboBox<Waehrung> waehrung;
    /** Beschriftung des Dispo-Felds */
    @FXML
    private Label dispoBeschriftung;
    /** Eingabe des Dispos (nur Girokonto) */
    @FXML
    private TextField dispo;
    /** Anzeige des Eröffnungssaldos */
    @FXML
    private Label saldo;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Füllt Kontoart und Währung und koppelt das Dispo-Feld an die Kontoart.
     */
    @FXML
    private void initialize() {
        kontoart.getItems().setAll(Kontoart.values());
        waehrung.getItems().setAll(Waehrung.values());
        waehrung.setValue(Waehrung.EURO);
        dispo.disableProperty().bind(kontoart.valueProperty().isNotEqualTo(Kontoart.GIROKONTO));
        dispoBeschriftung.disableProperty().bind(dispo.disableProperty());
        // Eröffnungssaldo 0 in der gewählten Währung, formatiert wie alle anderen Beträge
        saldo.textProperty().bind(Bindings.createStringBinding(() -> waehrung.getValue() == null ? ""
                : new Geldbetrag(0, waehrung.getValue()).toString(), waehrung.valueProperty()));
    }

    /**
     * Setzt die auswählbaren Kunden.
     * @param kunden die aktiven Kunden
     * @param vorauswahl vorausgewählter Kunde, darf null sein
     */
    public void setKunden(List<Kundenakte> kunden, Kundenakte vorauswahl) {
        kunde.getItems().setAll(kunden);
        if (vorauswahl != null && kunden.contains(vorauswahl))
            kunde.setValue(vorauswahl);
    }

    /**
     * Liefert den gewählten Kunden.
     * @return der Kunde
     * @throws IllegalArgumentException wenn kein Kunde gewählt ist
     */
    public Kundenakte getKunde() {
        if (kunde.getValue() == null)
            throw new IllegalArgumentException("Bitte einen Kunden auswählen.");
        return kunde.getValue();
    }

    /**
     * Liefert die gewählte Kontoart.
     * @return die Kontoart oder null, wenn keine gewählt ist (die Verwaltung meldet das)
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
     * Liest den Dispo in der gewählten Währung.
     * @return der Dispo oder null, wenn die Kontoart keinen braucht
     * @throws IllegalArgumentException wenn der Dispo fehlt oder ungültig ist
     */
    public Geldbetrag getDispo() {
        if (kontoart.getValue() == null || !kontoart.getValue().brauchtDispo() || waehrung.getValue() == null)
            return null;
        return new Geldbetrag(Darstellung.betragLesen(dispo.getText(), "Dispo"), waehrung.getValue());
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
