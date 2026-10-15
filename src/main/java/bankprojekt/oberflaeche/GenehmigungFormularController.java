package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Antragsart;
import bankprojekt.verwaltung.Kontoantrag;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Controller des Formulars zur Genehmigung eines Kontoantrags
 * ({@code genehmigungFormular.fxml}). Nur beim Girokonto wird ein Dispo abgefragt.
 * @author m.alharethi
 */
public class GenehmigungFormularController {
    /**
     * Wird vom FXMLLoader beim Laden des Formulars aufgerufen.
     */
    public GenehmigungFormularController() {
    }

    /** Zusammenfassung des Antrags */
    @FXML
    private Label zusammenfassung;
    /** Zeile mit dem Dispo (nur Girokonto) */
    @FXML
    private HBox dispoZeile;
    /** Eingabe Dispo */
    @FXML
    private TextField dispo;
    /** Währung des Dispos */
    @FXML
    private Label waehrung;
    /** Hinweis zur Genehmigung */
    @FXML
    private Label hinweis;
    /** Anzeige von Eingabefehlern */
    @FXML
    private Label fehler;

    /**
     * der zu genehmigende Antrag
     */
    private Kontoantrag antrag;

    /**
     * Zeigt den Antrag und blendet den Dispo nur beim Girokonto ein.
     * @param antrag der Antrag
     */
    public void setAntrag(Kontoantrag antrag) {
        this.antrag = antrag;
        zusammenfassung.setText(antrag.getAntragsteller() + " beantragt ein " + antrag.getKontoart()
                + " in " + antrag.getWaehrung() + ".");
        boolean mitDispo = antrag.getKontoart().brauchtDispo();
        dispoZeile.setVisible(mitDispo);
        dispoZeile.setManaged(mitDispo);
        waehrung.setText(antrag.getWaehrung().getKuerzel());
        hinweis.setText((antrag.getArt() == Antragsart.NEUKUNDE
                ? "Bei der Genehmigung werden der Kunde und das Konto angelegt. "
                : "Bei der Genehmigung wird das Konto eröffnet. ")
                + "Der Kunde sieht es nach seiner nächsten Anmeldung im Kundenportal.");
    }

    /**
     * Liest den Dispo in der Kontowährung.
     * @return der Dispo; null, wenn die Kontoart keinen braucht
     * @throws IllegalArgumentException wenn er fehlt oder ungültig ist
     */
    public Geldbetrag getDispo() {
        if (!antrag.getKontoart().brauchtDispo())
            return null;
        return new Geldbetrag(Darstellung.betragLesen(dispo.getText(), "Dispo"), antrag.getWaehrung());
    }

    /**
     * Liefert das Feld für Fehlermeldungen.
     * @return das Fehlerfeld
     */
    public Label getFehlerfeld() {
        return fehler;
    }
}
