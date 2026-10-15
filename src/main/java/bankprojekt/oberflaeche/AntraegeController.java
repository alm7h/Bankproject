package bankprojekt.oberflaeche;

import java.util.List;

import bankprojekt.verwaltung.Antragsart;
import bankprojekt.verwaltung.Antragsstatus;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kundendaten;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Controller der Antragsansicht der Verwaltung ({@code antraege.fxml}): Kontoanträge
 * aus dem Kundenportal ansehen, genehmigen (beim Girokonto mit Dispo) oder mit
 * Begründung ablehnen. Vorausgewählt sind die offenen Anträge.
 * @author m.alharethi
 */
public class AntraegeController implements Ansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public AntraegeController() {
    }

    /** Filter nach Status; null für alle */
    @FXML
    private ComboBox<Antragsstatus> statusFilter;
    /** Tabelle der Anträge */
    @FXML
    private TableView<Kontoantrag> tabelle;
    /** Spalte Antragsnummer */
    @FXML
    private TableColumn<Kontoantrag, String> nummerSpalte;
    /** Spalte Eingereicht */
    @FXML
    private TableColumn<Kontoantrag, String> eingereichtSpalte;
    /** Spalte Art */
    @FXML
    private TableColumn<Kontoantrag, String> artSpalte;
    /** Spalte Antragsteller */
    @FXML
    private TableColumn<Kontoantrag, String> antragstellerSpalte;
    /** Spalte Kontoart */
    @FXML
    private TableColumn<Kontoantrag, String> kontoartSpalte;
    /** Spalte Währung */
    @FXML
    private TableColumn<Kontoantrag, String> waehrungSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Kontoantrag, String> statusSpalte;
    /** Hinweis ohne Auswahl */
    @FXML
    private Label keineAuswahl;
    /** Detailbereich */
    @FXML
    private VBox detailInhalt;
    /** alle Angaben des gewählten Antrags */
    @FXML
    private Label details;
    /** Knopf „Genehmigen" */
    @FXML
    private Button genehmigenKnopf;
    /** Knopf „Ablehnen" */
    @FXML
    private Button ablehnenKnopf;
    /** Anzeige der Trefferzahl */
    @FXML
    private Label anzahl;

    /**
     * die Sitzung des angemeldeten Mitarbeiters
     */
    private Sitzung sitzung;
    /**
     * wird nach einer Entscheidung aufgerufen (z. B. um die Zahl offener Anträge in
     * der Navigation aufzufrischen); darf null sein
     */
    private Runnable nachEntscheidung;

    /**
     * Richtet Spalten, Filter (vorausgewählt: offen) und Detailbereich ein.
     */
    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getAntragsnummer()));
        eingereichtSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(Darstellung.zeitpunkt(z.getValue().getEingereichtAm())));
        artSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getArt().toString()));
        antragstellerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getAntragsteller()));
        kontoartSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKontoart().toString()));
        waehrungSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getWaehrung().getKuerzel()));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().toString()));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        Darstellung.zeilenEinrichten(tabelle, a -> a.getStatus() != Antragsstatus.OFFEN, null);

        statusFilter.setItems(FXCollections.observableArrayList(null, Antragsstatus.OFFEN, Antragsstatus.GENEHMIGT,
                Antragsstatus.ABGELEHNT));
        statusFilter.setButtonCell(Darstellung.auswahlZelle("Alle"));
        statusFilter.setCellFactory(l -> Darstellung.auswahlZelle("Alle"));
        statusFilter.setValue(Antragsstatus.OFFEN);
        statusFilter.valueProperty().addListener((b, alt, neu) -> aktualisieren());
        tabelle.getSelectionModel().selectedItemProperty().addListener((b, alt, neu) -> detailsZeigen(neu));

        detailInhalt.managedProperty().bind(detailInhalt.visibleProperty());
        keineAuswahl.managedProperty().bind(keineAuswahl.visibleProperty());
        keineAuswahl.visibleProperty().bind(detailInhalt.visibleProperty().not());
        detailsZeigen(null);
    }

    @Override
    public void setSitzung(Sitzung sitzung) {
        this.sitzung = sitzung;
    }

    /**
     * Legt fest, was nach einer Entscheidung zusätzlich geschehen soll.
     * @param nachEntscheidung die Aktion oder null
     */
    public void setNachEntscheidung(Runnable nachEntscheidung) {
        this.nachEntscheidung = nachEntscheidung;
    }

    @Override
    public void aktualisieren() {
        if (sitzung == null)
            return;
        Kontoantrag ausgewaehlt = tabelle.getSelectionModel().getSelectedItem();
        List<Kontoantrag> treffer = sitzung.verwaltung().getAntraege(statusFilter.getValue());
        tabelle.getItems().setAll(treffer);
        if (ausgewaehlt != null && treffer.contains(ausgewaehlt))
            tabelle.getSelectionModel().select(ausgewaehlt);
        tabelle.refresh();
        detailsZeigen(tabelle.getSelectionModel().getSelectedItem());
        long offen = sitzung.verwaltung().getAnzahlOffeneAntraege();
        anzahl.setText(treffer.size() + (treffer.size() == 1 ? " Antrag" : " Anträge") + " angezeigt, "
                + offen + " offen");
    }

    /**
     * Zeigt alle Angaben eines Antrags; entscheiden lassen sich nur offene Anträge.
     * @param antrag der Antrag oder null
     */
    private void detailsZeigen(Kontoantrag antrag) {
        detailInhalt.setVisible(antrag != null);
        if (antrag == null)
            return;
        StringBuilder text = new StringBuilder()
                .append(antrag.getAntragsnummer()).append(" – ").append(antrag.getArt())
                .append("\nEingereicht: ").append(Darstellung.zeitpunkt(antrag.getEingereichtAm()))
                .append("\nBenutzername: ").append(antrag.getBenutzername())
                .append("\nGewünscht: ").append(antrag.getKontoart()).append(" in ").append(antrag.getWaehrung());
        if (antrag.getArt() == Antragsart.NEUKUNDE) {
            Kundendaten d = antrag.getKundendaten();
            text.append("\n\nName: ").append(d.vorname()).append(" ").append(d.nachname())
                    .append("\nGeburtsdatum: ").append(Darstellung.datum(d.geburtsdatum()))
                    .append("\nAdresse: ").append(d.adresse().replace("\n", ", "))
                    .append("\nTelefon: ").append(d.telefon().isEmpty() ? "–" : d.telefon())
                    .append("\nE-Mail: ").append(d.email());
        } else {
            text.append("\n\nKunde: ").append(antrag.getKundennummer()).append(" – ").append(antrag.getAntragsteller());
        }
        if (antrag.getStatus() != Antragsstatus.OFFEN) {
            text.append("\n\nStatus: ").append(antrag.getStatus())
                    .append(" von ").append(antrag.getBearbeitetVon())
                    .append(" am ").append(Darstellung.zeitpunkt(antrag.getBearbeitetAm()));
            if (antrag.getBegruendung() != null)
                text.append("\nBegründung: ").append(antrag.getBegruendung());
        }
        details.setText(text.toString());
        boolean offen = antrag.getStatus() == Antragsstatus.OFFEN;
        genehmigenKnopf.setDisable(!offen);
        ablehnenKnopf.setDisable(!offen);
    }

    /**
     * Reaktion auf „Genehmigen": beim Girokonto mit Eingabe des Dispos.
     */
    @FXML
    private void genehmigen() {
        Kontoantrag antrag = tabelle.getSelectionModel().getSelectedItem();
        if (antrag == null)
            return;
        Dialoge.Geladen<GenehmigungFormularController> formular = Dialoge.laden("genehmigungFormular.fxml");
        GenehmigungFormularController eingaben = formular.controller();
        eingaben.setAntrag(antrag);
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Antrag " + antrag.getAntragsnummer() + " genehmigen",
                formular.wurzel(), eingaben.getFehlerfeld(), "Genehmigen",
                () -> sitzung.verwaltung().antragGenehmigen(sitzung.benutzer(), antrag.getAntragsnummer(),
                        eingaben.getDispo())))
            entschieden();
    }

    /**
     * Reaktion auf „Ablehnen": mit Pflicht-Begründung, die der Kunde im Portal sieht.
     */
    @FXML
    private void ablehnen() {
        Kontoantrag antrag = tabelle.getSelectionModel().getSelectedItem();
        if (antrag == null)
            return;
        Dialoge.Geladen<AblehnungFormularController> formular = Dialoge.laden("ablehnungFormular.fxml");
        AblehnungFormularController eingaben = formular.controller();
        eingaben.setAntrag(antrag);
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Antrag " + antrag.getAntragsnummer() + " ablehnen",
                formular.wurzel(), eingaben.getFehlerfeld(), "Ablehnen",
                () -> sitzung.verwaltung().antragAblehnen(sitzung.benutzer(), antrag.getAntragsnummer(),
                        eingaben.getBegruendung())))
            entschieden();
    }

    /**
     * Frischt nach einer Entscheidung die Ansicht (und ggf. die Navigation) auf.
     */
    private void entschieden() {
        aktualisieren();
        if (nachEntscheidung != null)
            nachEntscheidung.run();
    }
}
