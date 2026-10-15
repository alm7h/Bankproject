package bankprojekt.oberflaeche;

import java.util.ArrayList;
import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontostatus;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenstatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller der Kontenansicht ({@code konten.fxml}): Konten suchen und filtern,
 * eröffnen, Details mit aktuellem Kontostand ansehen, sperren, entsperren und
 * auflösen. Ein Konto kann nur bei Kontostand 0 aufgelöst werden.
 * @author m.alharethi
 */
public class KontenController implements Ansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public KontenController() {
    }

    /** Suchfeld (IBAN, Kontonummer, Inhaber) */
    @FXML
    private TextField suche;
    /** Filter nach Kunde; null für alle */
    @FXML
    private ComboBox<Kundenakte> kundeFilter;
    /** Filter nach Status; null für alle */
    @FXML
    private ComboBox<Kontostatus> statusFilter;
    /** Tabelle der Konten */
    @FXML
    private TableView<Kontoakte> tabelle;
    /** Spalte IBAN */
    @FXML
    private TableColumn<Kontoakte, String> ibanSpalte;
    /** Spalte Kontoart */
    @FXML
    private TableColumn<Kontoakte, String> artSpalte;
    /** Spalte Inhaber */
    @FXML
    private TableColumn<Kontoakte, String> inhaberSpalte;
    /** Spalte Kundennummer */
    @FXML
    private TableColumn<Kontoakte, String> kundennummerSpalte;
    /** Spalte Kontostand (live an das Konto gebunden) */
    @FXML
    private TableColumn<Kontoakte, Geldbetrag> standSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Kontoakte, String> statusSpalte;
    /** Anzeige der Trefferzahl */
    @FXML
    private Label anzahl;
    /** Hinweis, solange kein Konto ausgewählt ist */
    @FXML
    private Label keineAuswahl;
    /** Inhalt des Detailbereichs */
    @FXML
    private VBox detailInhalt;
    /** Detail: IBAN */
    @FXML
    private Label detailIban;
    /** Detail: Kontostand */
    @FXML
    private Label detailStand;
    /** Detail: Kontonummer */
    @FXML
    private Label detailNummer;
    /** Detail: Kontoart */
    @FXML
    private Label detailArt;
    /** Detail: Inhaber */
    @FXML
    private Label detailInhaber;
    /** Detail: Währung */
    @FXML
    private Label detailWaehrung;
    /** Detail: Status */
    @FXML
    private Label detailStatus;
    /** Detail: Eröffnungsdatum */
    @FXML
    private Label detailEroeffnet;
    /** Detail: Auflösungsdatum */
    @FXML
    private Label detailAufgeloest;
    /** Knopf „Sperren" */
    @FXML
    private Button sperrenKnopf;
    /** Knopf „Entsperren" */
    @FXML
    private Button entsperrenKnopf;
    /** Knopf „Auflösen" */
    @FXML
    private Button aufloesenKnopf;
    /** Hinweis, warum nicht aufgelöst werden kann */
    @FXML
    private Label aufloesenHinweis;

    /**
     * die Sitzung des angemeldeten Benutzers
     */
    private Sitzung sitzung;

    /**
     * Richtet Spalten, Filter und den Detailbereich ein.
     */
    @FXML
    private void initialize() {
        ibanSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getIbanFormatiert()));
        artSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKontoart().toString()));
        inhaberSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getInhaberName()));
        kundennummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundennummer()));
        // Der Kontostand hängt direkt an der Property des Kontos und ist damit immer aktuell
        standSpalte.setCellValueFactory(z -> z.getValue().kontostandProperty());
        standSpalte.setCellFactory(s -> new TableCell<>() {
            @Override
            protected void updateItem(Geldbetrag betrag, boolean leer) {
                super.updateItem(betrag, leer);
                setText(leer || betrag == null ? null : betrag.toString());
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().toString()));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        Darstellung.zeilenEinrichten(tabelle, k -> k.getStatus() == Kontostatus.AUFGELOEST, null);

        kundeFilter.setButtonCell(Darstellung.auswahlZelle("Alle Kunden"));
        kundeFilter.setCellFactory(l -> Darstellung.auswahlZelle("Alle Kunden"));
        statusFilter.setItems(FXCollections.observableArrayList(null, Kontostatus.AKTIV, Kontostatus.GESPERRT,
                Kontostatus.AUFGELOEST));
        statusFilter.setButtonCell(Darstellung.auswahlZelle("Alle Status"));
        statusFilter.setCellFactory(l -> Darstellung.auswahlZelle("Alle Status"));

        suche.textProperty().addListener((b, alt, neu) -> suchen());
        kundeFilter.valueProperty().addListener((b, alt, neu) -> suchen());
        statusFilter.valueProperty().addListener((b, alt, neu) -> suchen());
        tabelle.getSelectionModel().selectedItemProperty().addListener((b, alt, neu) -> detailsZeigen(neu));

        // Detailinhalt nur zeigen (und Platz belegen), wenn ein Konto gewählt ist
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
     * Frischt die Kundenauswahl des Filters auf und sucht neu.
     */
    @Override
    public void aktualisieren() {
        if (sitzung == null)
            return;
        Kundenakte gewaehlt = kundeFilter.getValue();
        List<Kundenakte> kunden = sitzung.verwaltung().getKunden();
        List<Kundenakte> eintraege = new ArrayList<>();
        eintraege.add(null);   // steht für "Alle Kunden"
        eintraege.addAll(kunden);
        kundeFilter.getItems().setAll(eintraege);
        kundeFilter.setValue(kunden.contains(gewaehlt) ? gewaehlt : null);
        suchen();
    }

    /**
     * Zeigt nur die Konten eines Kunden (z. B. aus der Kundenansicht heraus).
     * @param kunde der Kunde
     */
    public void kontenVonKundeZeigen(Kundenakte kunde) {
        suche.clear();
        statusFilter.setValue(null);
        kundeFilter.setValue(kunde);
        suchen();
    }

    /**
     * Sucht mit den aktuellen Filtern und behält die Auswahl, falls möglich.
     */
    private void suchen() {
        if (sitzung == null)
            return;
        Kontoakte ausgewaehlt = tabelle.getSelectionModel().getSelectedItem();
        Kundenakte kunde = kundeFilter.getValue();
        List<Kontoakte> treffer = sitzung.verwaltung().kontenSuchen(suche.getText(),
                kunde == null ? null : kunde.getKundennummer(), statusFilter.getValue());
        tabelle.getItems().setAll(treffer);
        if (ausgewaehlt != null && treffer.contains(ausgewaehlt))
            tabelle.getSelectionModel().select(ausgewaehlt);
        tabelle.refresh();
        detailsZeigen(tabelle.getSelectionModel().getSelectedItem());
        int gesamt = sitzung.verwaltung().getKonten().size();
        anzahl.setText(treffer.size() == gesamt
                ? gesamt + (gesamt == 1 ? " Konto" : " Konten")
                : treffer.size() + " von " + gesamt + " Konten");
    }

    /**
     * Zeigt die Details eines Kontos und passt die Knöpfe an seinen Status an.
     * @param konto das Konto oder null für keine Auswahl
     */
    private void detailsZeigen(Kontoakte konto) {
        detailStand.textProperty().unbind();
        detailInhalt.setVisible(konto != null);
        if (konto == null)
            return;
        detailIban.setText(konto.getIbanFormatiert());
        // Kontostand live anzeigen, rot wenn negativ
        detailStand.textProperty().bind(konto.kontostandProperty().asString());
        detailStand.getStyleClass().remove("negativ");
        if (konto.getKontostand().isNegativ())
            detailStand.getStyleClass().add("negativ");
        detailNummer.setText(Long.toString(konto.getKontonummer()));
        detailArt.setText(konto.getKontoart().toString());
        detailInhaber.setText(konto.getInhaberName() + " (" + konto.getKundennummer() + ")");
        detailWaehrung.setText(konto.getWaehrung().getKuerzel());
        Kontostatus status = konto.getStatus();
        detailStatus.setText(status.toString());
        detailStatus.getStyleClass().setAll("label", "status", "status-" + Darstellung.cssName(status.toString()));
        detailEroeffnet.setText(Darstellung.datum(konto.getEroeffnetAm()));
        detailAufgeloest.setText(Darstellung.datum(konto.getAufgeloestAm()));

        boolean guthabenNull = konto.getKontostand().compareTo(Geldbetrag.NULL_EURO) == 0;
        sperrenKnopf.setDisable(status != Kontostatus.AKTIV);
        entsperrenKnopf.setDisable(status != Kontostatus.GESPERRT);
        aufloesenKnopf.setDisable(status == Kontostatus.AUFGELOEST || !guthabenNull);
        aufloesenHinweis.setText(status != Kontostatus.AUFGELOEST && !guthabenNull
                ? "Auflösen ist nur bei einem Kontostand von 0 möglich." : "");
    }

    /**
     * Reaktion auf „Filter zurücksetzen".
     */
    @FXML
    private void filterZuruecksetzen() {
        suche.clear();
        kundeFilter.setValue(null);
        statusFilter.setValue(null);
    }

    /**
     * Reaktion auf „Neues Konto": Formular zeigen und das Konto eröffnen.
     */
    @FXML
    private void neuesKonto() {
        Dialoge.Geladen<KontoFormularController> formular = Dialoge.laden("kontoFormular.fxml");
        KontoFormularController eingaben = formular.controller();
        eingaben.setKunden(sitzung.verwaltung().kundenSuchen(null, Kundenstatus.AKTIV),
                kundeFilter.getValue());
        Kontoakte[] neu = new Kontoakte[1];
        boolean gespeichert = Dialoge.formular(Dialoge.fenster(tabelle), "Neues Konto eröffnen", formular.wurzel(),
                eingaben.getFehlerfeld(),
                () -> neu[0] = sitzung.verwaltung().kontoEroeffnen(sitzung.benutzer(),
                        eingaben.getKunde().getKundennummer(), eingaben.getKontoart(), eingaben.getWaehrung(),
                        eingaben.getDispo()));
        if (gespeichert) {
            suche.clear();
            statusFilter.setValue(null);
            aktualisieren();
            tabelle.getSelectionModel().select(neu[0]);
            tabelle.scrollTo(neu[0]);
        }
    }

    /**
     * Reaktion auf „Sperren" (mit Rückfrage).
     */
    @FXML
    private void sperren() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto != null && Dialoge.bestaetigen(Dialoge.fenster(tabelle), "Konto sperren",
                "Soll das Konto " + konto + " von " + konto.getInhaberName()
                        + " gesperrt werden?\n\nAuf einem gesperrten Konto sind keine Buchungen möglich.",
                "Sperren")) {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.verwaltung().kontoSperren(sitzung.benutzer(), konto.getKontonummer()));
            suchen();
        }
    }

    /**
     * Reaktion auf „Entsperren".
     */
    @FXML
    private void entsperren() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto != null) {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.verwaltung().kontoEntsperren(sitzung.benutzer(), konto.getKontonummer()));
            suchen();
        }
    }

    /**
     * Reaktion auf „Auflösen" (mit Rückfrage).
     */
    @FXML
    private void aufloesen() {
        Kontoakte konto = tabelle.getSelectionModel().getSelectedItem();
        if (konto != null && Dialoge.bestaetigen(Dialoge.fenster(tabelle), "Konto auflösen",
                "Soll das Konto " + konto + " von " + konto.getInhaberName() + " wirklich aufgelöst werden?\n\n"
                        + "Das Konto wird dauerhaft gesperrt und kann nicht wieder geöffnet werden. "
                        + "Es bleibt zur Nachvollziehbarkeit gespeichert.",
                "Auflösen")) {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.verwaltung().kontoAufloesen(sitzung.benutzer(), konto.getKontonummer()));
            suchen();
        }
    }
}
