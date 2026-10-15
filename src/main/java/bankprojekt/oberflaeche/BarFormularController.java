package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Kontoakte;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller des Formulars für Bareinzahlungen und Barauszahlungen am Schalter
 * ({@code barFormular.fxml}). Der Betrag wird in der Kontowährung eingegeben.
 * @author m.alharethi
 */
public class BarFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public BarFormularController() {
    }

    /** Anzeige des Kontos */
    @FXML
    private Label konto;
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
     * das gebuchte Konto
     */
    private Kontoakte kontoakte;

    /**
     * Legt das Konto fest.
     * @param kontoakte das Konto
     */
    public void setKonto(Kontoakte kontoakte) {
        this.kontoakte = kontoakte;
        konto.setText(kontoakte + "\n" + kontoakte.getInhaberName() + ", Kontostand " + kontoakte.getKontostand());
        waehrung.setText(kontoakte.getWaehrung().getKuerzel());
    }

    /**
     * Liest den Betrag in der Kontowährung.
     * @return der Betrag
     * @throws IllegalArgumentException wenn er fehlt oder ungültig ist
     */
    public Geldbetrag getBetrag() {
        return new Geldbetrag(Darstellung.betragLesen(betrag.getText(), "Betrag"), kontoakte.getWaehrung());
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
