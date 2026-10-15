package bankprojekt.oberflaeche;

import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Kontoakte;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller des Umbuchungsformulars ({@code umbuchungFormular.fxml}): Betrag vom
 * gewählten eigenen Konto auf ein anderes eigenes Konto.
 * @author m.alharethi
 */
public class UmbuchungFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public UmbuchungFormularController() {
    }

    /** Anzeige des abgebenden Kontos */
    @FXML
    private Label von;
    /** Auswahl des Zielkontos */
    @FXML
    private ComboBox<Kontoakte> ziel;
    /** Eingabe Betrag */
    @FXML
    private TextField betrag;
    /** Anzeige der Währung */
    @FXML
    private Label waehrung;
    /** Eingabe Buchungstext */
    @FXML
    private TextField text;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * das abgebende Konto
     */
    private Kontoakte konto;

    /**
     * Legt das abgebende Konto und die möglichen Zielkonten fest. Gibt es nur ein
     * Zielkonto, ist es vorausgewählt.
     * @param konto das abgebende Konto
     * @param ziele die anderen aktiven eigenen Konten
     */
    public void setKonten(Kontoakte konto, List<Kontoakte> ziele) {
        this.konto = konto;
        von.setText(konto + "\nKontostand: " + konto.getKontostand());
        waehrung.setText(konto.getWaehrung().getKuerzel());
        ziel.getItems().setAll(ziele);
        if (ziele.size() == 1)
            ziel.setValue(ziele.get(0));
    }

    /**
     * Liefert das gewählte Zielkonto.
     * @return das Zielkonto
     * @throws IllegalArgumentException wenn keines gewählt ist
     */
    public Kontoakte getZiel() {
        if (ziel.getValue() == null)
            throw new IllegalArgumentException("Bitte ein Zielkonto wählen.");
        return ziel.getValue();
    }

    /**
     * Liest den Betrag in der Währung des abgebenden Kontos.
     * @return der Betrag
     * @throws IllegalArgumentException wenn er fehlt oder ungültig ist
     */
    public Geldbetrag getBetrag() {
        return new Geldbetrag(Darstellung.betragLesen(betrag.getText(), "Betrag"), konto.getWaehrung());
    }

    /**
     * Liefert den Buchungstext.
     * @return der Text (darf leer sein)
     */
    public String getText() {
        return text.getText();
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
