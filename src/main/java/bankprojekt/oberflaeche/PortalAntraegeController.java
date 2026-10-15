package bankprojekt.oberflaeche;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bankprojekt.verwaltung.Antragsstatus;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kundendaten;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller von „Anträge" im Kundenportal ({@code portalAntraege.fxml}): der Stand
 * der eigenen Kontoanträge mit Begründung bei Ablehnung; Kunden beantragen hier ein
 * weiteres Konto, abgelehnte Neukunden stellen einen neuen Antrag.
 * @author m.alharethi
 */
public class PortalAntraegeController implements Portalansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public PortalAntraegeController() {
    }

    /** Hinweis zum Stand */
    @FXML
    private Label hinweis;
    /** Knopf „Weiteres Konto beantragen" (nur für Kunden) */
    @FXML
    private Button zusatzKnopf;
    /** Knopf „Neuen Antrag stellen" (nur nach Ablehnung eines Neukundenantrags) */
    @FXML
    private Button erneutKnopf;
    /** Tabelle der Anträge */
    @FXML
    private TableView<Kontoantrag> antragTabelle;
    /** Spalte Antragsnummer */
    @FXML
    private TableColumn<Kontoantrag, String> nummerSpalte;
    /** Spalte Art */
    @FXML
    private TableColumn<Kontoantrag, String> artSpalte;
    /** Spalte Kontoart */
    @FXML
    private TableColumn<Kontoantrag, String> kontoartSpalte;
    /** Spalte Währung */
    @FXML
    private TableColumn<Kontoantrag, String> waehrungSpalte;
    /** Spalte Eingereicht */
    @FXML
    private TableColumn<Kontoantrag, String> eingereichtSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Kontoantrag, String> statusSpalte;
    /** Spalte Bemerkung (Konto bzw. Begründung) */
    @FXML
    private TableColumn<Kontoantrag, String> bemerkungSpalte;

    /**
     * die Sitzung des angemeldeten Kunden
     */
    private Kundensitzung sitzung;

    /**
     * Richtet die Spalten ein.
     */
    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getAntragsnummer()));
        artSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getArt().toString()));
        kontoartSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKontoart().toString()));
        waehrungSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getWaehrung().getKuerzel()));
        eingereichtSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(Darstellung.zeitpunkt(z.getValue().getEingereichtAm())));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().toString()));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        bemerkungSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(bemerkung(z.getValue())));
    }

    @Override
    public void setSitzung(Kundensitzung sitzung) {
        this.sitzung = sitzung;
    }

    @Override
    public void aktualisieren() {
        List<Kontoantrag> antraege = new ArrayList<>(sitzung.portal().getAntraege(sitzung.zugang()));
        Collections.reverse(antraege);   // neueste zuerst
        antragTabelle.getItems().setAll(antraege);
        boolean istKunde = sitzung.zugang().istKunde();
        boolean erneut = sitzung.portal().kannErneutBeantragen(sitzung.zugang());
        sichtbar(zusatzKnopf, istKunde);
        sichtbar(erneutKnopf, erneut);
        boolean offen = antraege.stream().anyMatch(a -> a.getStatus() == Antragsstatus.OFFEN);
        if (istKunde)
            hinweis.setText("Hier sehen Sie Ihre Kontoanträge. Ein weiteres Konto können Sie jederzeit beantragen.");
        else if (offen)
            hinweis.setText("Ihr Antrag wird geprüft. Sobald er genehmigt ist, sehen Sie Ihr Konto nach der "
                    + "nächsten Anmeldung unter „Meine Konten“.");
        else
            hinweis.setText("Ihr Antrag wurde leider abgelehnt. Den Grund sehen Sie unten – Sie können gern "
                    + "einen neuen Antrag stellen.");
    }

    /**
     * Blendet einen Knoten ein oder aus (ohne Platz zu belegen).
     * @param knoten der Knoten
     * @param sichtbar ob er sichtbar sein soll
     */
    private static void sichtbar(Node knoten, boolean sichtbar) {
        knoten.setVisible(sichtbar);
        knoten.setManaged(sichtbar);
    }

    /**
     * Liefert die Bemerkung zu einem Antrag.
     * @param antrag der Antrag
     * @return "wird geprüft", das eröffnete Konto oder die Begründung der Ablehnung
     */
    private String bemerkung(Kontoantrag antrag) {
        return switch (antrag.getStatus()) {
            case OFFEN -> "wird geprüft";
            case GENEHMIGT -> sitzung.portal().getKonten(sitzung.zugang()).stream()
                    .filter(k -> antrag.getKontonummer().equals(k.getKontonummer()))
                    .findFirst().map(Kontoakte::toString).map(k -> "Konto eröffnet: " + k)
                    .orElse("Konto eröffnet – bitte neu anmelden");
            case ABGELEHNT -> "Begründung: " + antrag.getBegruendung();
        };
    }

    /**
     * Reaktion auf „Weiteres Konto beantragen".
     */
    @FXML
    private void zusatzkontoBeantragen() {
        Dialoge.Geladen<KontoantragFormularController> formular = Dialoge.laden("kontoantragFormular.fxml");
        KontoantragFormularController eingaben = formular.controller();
        if (Dialoge.formular(Dialoge.fenster(antragTabelle), "Weiteres Konto beantragen", formular.wurzel(),
                eingaben.getFehlerfeld(), () -> sitzung.portal().zusatzkontoBeantragen(sitzung.zugang(),
                        eingaben.getKontoart(), eingaben.getWaehrung())))
            aktualisieren();
    }

    /**
     * Reaktion auf „Neuen Antrag stellen": vorbelegt mit den Daten des letzten Antrags.
     */
    @FXML
    private void erneutBeantragen() {
        List<Kontoantrag> antraege = sitzung.portal().getAntraege(sitzung.zugang());
        Kundendaten bisher = antraege.isEmpty() ? null : antraege.get(antraege.size() - 1).getKundendaten();
        Dialoge.Geladen<KontoantragFormularController> formular = Dialoge.laden("kontoantragFormular.fxml");
        KontoantragFormularController eingaben = formular.controller();
        eingaben.setNeukunde(bisher);
        if (Dialoge.formular(Dialoge.fenster(antragTabelle), "Neuen Antrag stellen", formular.wurzel(),
                eingaben.getFehlerfeld(), () -> sitzung.portal().erneutBeantragen(sitzung.zugang(),
                        eingaben.getKundendaten(), eingaben.getKontoart(), eingaben.getWaehrung())))
            aktualisieren();
    }
}
