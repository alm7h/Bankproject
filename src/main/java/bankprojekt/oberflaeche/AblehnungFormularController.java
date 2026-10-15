package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Kontoantrag;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/**
 * Controller des Formulars zur Ablehnung eines Kontoantrags ({@code ablehnungFormular.fxml}).
 * Die Begründung ist Pflicht; geprüft wird sie von der Verwaltung.
 * @author m.alharethi
 */
public class AblehnungFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public AblehnungFormularController() {
    }

    /** Zusammenfassung des Antrags */
    @FXML
    private Label zusammenfassung;
    /** Eingabe der Begründung */
    @FXML
    private TextArea begruendung;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * Zeigt den abzulehnenden Antrag.
     * @param antrag der Antrag
     */
    public void setAntrag(Kontoantrag antrag) {
        zusammenfassung.setText(antrag.getAntragsteller() + " hat ein " + antrag.getKontoart() + " in "
                + antrag.getWaehrung() + " beantragt.");
    }

    /**
     * Liefert die eingegebene Begründung.
     * @return die Begründung
     */
    public String getBegruendung() {
        return begruendung.getText();
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
