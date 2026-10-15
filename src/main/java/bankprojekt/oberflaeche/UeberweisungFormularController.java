package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Kontoakte;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller des Überweisungsformulars ({@code ueberweisungFormular.fxml}). Der Betrag
 * wird in der Währung des Absenderkontos eingegeben; geprüft wird die Überweisung vom
 * {@link bankprojekt.verwaltung.Kundenportal}.
 * @author m.alharethi
 */
public class UeberweisungFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public UeberweisungFormularController() {
    }

    /** Anzeige des Absenderkontos */
    @FXML
    private Label von;
    /** Eingabe Empfänger-IBAN */
    @FXML
    private TextField iban;
    /** Eingabe Betrag */
    @FXML
    private TextField betrag;
    /** Anzeige der Währung */
    @FXML
    private Label waehrung;
    /** Eingabe Verwendungszweck */
    @FXML
    private TextField zweck;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * das Absenderkonto
     */
    private Kontoakte konto;

    /**
     * Legt das Absenderkonto fest.
     * @param konto das eigene Girokonto
     */
    public void setKonto(Kontoakte konto) {
        this.konto = konto;
        von.setText(konto.getIbanFormatiert() + "\nKontostand: " + konto.getKontostand());
        waehrung.setText(konto.getWaehrung().getKuerzel());
    }

    /**
     * Liefert die eingegebene IBAN.
     * @return die IBAN (geprüft wird sie vom Kundenportal)
     */
    public String getIban() {
        return iban.getText();
    }

    /**
     * Liest den Betrag in der Währung des Absenderkontos.
     * @return der Betrag
     * @throws IllegalArgumentException wenn er fehlt oder ungültig ist
     */
    public Geldbetrag getBetrag() {
        return new Geldbetrag(Darstellung.betragLesen(betrag.getText(), "Betrag"), konto.getWaehrung());
    }

    /**
     * Liefert den Verwendungszweck.
     * @return der Verwendungszweck (darf leer sein)
     */
    public String getVerwendungszweck() {
        return zweck.getText();
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
