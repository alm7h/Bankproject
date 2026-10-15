package bankprojekt.oberflaeche;

import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kontostatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Controller von „Meine Konten" im Kundenportal ({@code portalKonten.fxml}): die
 * eigenen Konten mit Gesamtsaldo, Details mit Live-Kontostand und Kontoauszug sowie
 * die Aktionen Überweisen, Umbuchen und Konto sperren.
 * @author m.alharethi
 */
public class PortalKontenController implements Portalansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public PortalKontenController() {
    }

    /** Tabelle der eigenen Konten */
    @FXML
    private TableView<Kontoakte> tabelle;
    /** Spalte Konto (IBAN und Kontoart) */
    @FXML
    private TableColumn<Kontoakte, String> kontoSpalte;
    /** Spalte Kontostand (live) */
    @FXML
    private TableColumn<Kontoakte, Geldbetrag> standSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Kontoakte, String> statusSpalte;
    /** Gesamtsaldo */
    @FXML
    private Label gesamt;
    /** Hinweis ohne Auswahl */
    @FXML
    private Label keineAuswahl;
    /** Detailbereich */
    @FXML
    private VBox detailInhalt;
    /** Detail: Kontoart und IBAN */
    @FXML
    private Label detailTitel;
    /** Detail: Kontostand */
    @FXML
    private Label detailStand;
    /** Detail: Status */
    @FXML
    private Label detailStatus;
    /** Knopf „Überweisen" */
    @FXML
    private Button ueberweisenKnopf;
    /** Knopf „Umbuchen" */
    @FXML
    private Button umbuchenKnopf;
    /** Knopf „Konto sperren" */
    @FXML
    private Button sperrenKnopf;
    /** Hinweis zu den Aktionen */
    @FXML
    private Label aktionHinweis;
    /** Controller des eingebundenen Kontoauszugs */
    @FXML
    private KontoauszugController auszugController;

    /**
     * die Sitzung des angemeldeten Kunden
     */
    private Kundensitzung sitzung;

    /**
     * Richtet Spalten und Detailbereich ein.
     */
    @FXML
    private void initialize() {
        kontoSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().toString()));
        standSpalte.setCellValueFactory(z -> z.getValue().kontostandProperty());
        standSpalte.setCellFactory(s -> Darstellung.betragZelle());
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().toString()));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        Darstellung.zeilenEinrichten(tabelle, k -> k.getStatus() == Kontostatus.AUFGELOEST, null);
        tabelle.getSelectionModel().selectedItemProperty().addListener((b, alt, neu) -> detailsZeigen(neu));
        detailInhalt.managedProperty().bind(detailInhalt.visibleProperty());
        keineAuswahl.managedProperty().bind(keineAuswahl.visibleProperty());
        keineAuswahl.visibleProperty().bind(detailInhalt.visibleProperty().not());
        detailsZeigen(null);
    }

    @Override
    public void setSitzung(Kundensitzung sitzung) {
        this.sitzung = sitzung;
    }

    /**
     * Liest die eigenen Konten neu; die Auswahl bleibt erhalten, sonst wird das erste
     * Konto gewählt.
     */
    @Override
    public void aktualisieren() {
        Kontoakte ausgewaehlt = tabelle.getSelectionModel().getSelectedItem();
        List<Kontoakte> konten = sitzung.portal().getKonten(sitzung.zugang());
        tabelle.getItems().setAll(konten);
        if (ausgewaehlt != null && konten.contains(ausgewaehlt))
            tabelle.getSelectionModel().select(ausgewaehlt);
        else
            tabelle.getSelectionModel().selectFirst();
        tabelle.refresh();
        detailsZeigen(tabelle.getSelectionModel().getSelectedItem());
        gesamt.setText(konten.isEmpty() ? "" : "Gesamtsaldo (in EUR umgerechnet): " + gesamtsaldo(konten));
    }

    /**
     * Summiert die Kontostände der nicht aufgelösten Konten in Euro.
     * @param konten die Konten
     * @return die Summe
     */
    private static Geldbetrag gesamtsaldo(List<Kontoakte> konten) {
        double summe = konten.stream()
                .filter(k -> k.getStatus() != Kontostatus.AUFGELOEST)
                .mapToDouble(k -> k.getKontostand().umrechnen(Waehrung.EURO).getBetrag())
                .sum();
        return new Geldbetrag(Math.round(summe * 100) / 100.0);
    }

    /**
     * Zeigt das gewählte Konto mit Kontoauszug und passt die Aktionen an.
     * @param konto das Konto oder null
     */
    private void detailsZeigen(Kontoakte konto) {
        detailStand.textProperty().unbind();
        detailInhalt.setVisible(konto != null);
        if (konto == null)
            return;
        detailTitel.setText(konto.getKontoart() + " · " + konto.getIbanFormatiert());
        detailStand.textProperty().bind(konto.kontostandProperty().asString());
        detailStand.getStyleClass().remove("negativ");
        if (konto.getKontostand().isNegativ())
            detailStand.getStyleClass().add("negativ");
        Kontostatus status = konto.getStatus();
        detailStatus.setText(status.toString());
        detailStatus.getStyleClass().setAll("label", "status", "status-" + Darstellung.cssName(status.toString()));

        boolean aktiv = status == Kontostatus.AKTIV;
        boolean giro = konto.getKontoart() == Kontoart.GIROKONTO;
        ueberweisenKnopf.setDisable(!aktiv || !giro);
        umbuchenKnopf.setDisable(!aktiv || andereAktiveKonten(konto).isEmpty());
        sperrenKnopf.setDisable(!aktiv);
        aktionHinweis.setText(switch (status) {
            case GESPERRT -> "Dieses Konto ist gesperrt. Zum Entsperren wenden Sie sich bitte an Ihre Bank.";
            case AUFGELOEST -> "Dieses Konto ist aufgelöst.";
            case AKTIV -> giro ? "" : "Überweisungen sind nur vom Girokonto aus möglich.";
        });
        auszugController.setBuchungen(sitzung.portal().getKontoauszug(sitzung.zugang(), konto.getKontonummer()));
    }

    /**
     * Liefert die anderen aktiven eigenen Konten (Ziele einer Umbuchung).
     * @param konto das abgebende Konto
     * @return die übrigen aktiven Konten
     */
    private List<Kontoakte> andereAktiveKonten(Kontoakte konto) {
        return tabelle.getItems().stream()
                .filter(k -> k != konto && k.getStatus() == Kontostatus.AKTIV)
                .toList();
    }

    /**
     * Reaktion auf „Überweisen".
     */
    @FXML
    private void ueberweisen() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto == null)
            return;
        Dialoge.Geladen<UeberweisungFormularController> formular = Dialoge.laden("ueberweisungFormular.fxml");
        UeberweisungFormularController eingaben = formular.controller();
        eingaben.setKonto(konto);
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Überweisung", formular.wurzel(), eingaben.getFehlerfeld(),
                () -> sitzung.portal().ueberweisen(sitzung.zugang(), konto.getKontonummer(), eingaben.getIban(),
                        eingaben.getBetrag(), eingaben.getVerwendungszweck())))
            aktualisieren();
    }

    /**
     * Reaktion auf „Umbuchen".
     */
    @FXML
    private void umbuchen() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto == null)
            return;
        Dialoge.Geladen<UmbuchungFormularController> formular = Dialoge.laden("umbuchungFormular.fxml");
        UmbuchungFormularController eingaben = formular.controller();
        eingaben.setKonten(konto, andereAktiveKonten(konto));
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Umbuchung", formular.wurzel(), eingaben.getFehlerfeld(),
                () -> sitzung.portal().umbuchen(sitzung.zugang(), konto.getKontonummer(),
                        eingaben.getZiel().getKontonummer(), eingaben.getBetrag(), eingaben.getText())))
            aktualisieren();
    }

    /**
     * Reaktion auf „Konto sperren" (mit Rückfrage).
     */
    @FXML
    private void sperren() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto != null && Dialoge.bestaetigen(Dialoge.fenster(tabelle), "Konto sperren",
                "Soll das Konto " + konto + " sofort gesperrt werden?\n\n"
                        + "Danach sind keine Buchungen mehr möglich. Entsperren kann das Konto nur Ihre Bank.",
                "Sperren")) {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.portal().kontoSperren(sitzung.zugang(), konto.getKontonummer()));
            aktualisieren();
        }
    }
}
