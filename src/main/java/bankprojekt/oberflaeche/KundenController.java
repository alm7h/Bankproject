package bankprojekt.oberflaeche;

import java.util.List;
import java.util.function.Consumer;

import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenstatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Controller der Kundenansicht ({@code kunden.fxml}): Kunden suchen, filtern,
 * anlegen, bearbeiten und deaktivieren bzw. wieder aktivieren. Kunden werden nie
 * gelöscht.
 * @author m.alharethi
 */
public class KundenController implements Ansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public KundenController() {
    }

    /** Suchfeld (Name oder Kundennummer) */
    @FXML
    private TextField suche;
    /** Filter nach Status; null für alle */
    @FXML
    private ComboBox<Kundenstatus> statusFilter;
    /** Knopf „Bearbeiten" */
    @FXML
    private Button bearbeitenKnopf;
    /** Knopf „Konten anzeigen" */
    @FXML
    private Button kontenAnzeigenKnopf;
    /** Knopf „Deaktivieren" bzw. „Aktivieren" */
    @FXML
    private Button statusKnopf;
    /** Tabelle der Kunden */
    @FXML
    private TableView<Kundenakte> tabelle;
    /** Spalte Kundennummer */
    @FXML
    private TableColumn<Kundenakte, String> nummerSpalte;
    /** Spalte Name */
    @FXML
    private TableColumn<Kundenakte, String> nameSpalte;
    /** Spalte Geburtsdatum */
    @FXML
    private TableColumn<Kundenakte, String> geburtsdatumSpalte;
    /** Spalte E-Mail */
    @FXML
    private TableColumn<Kundenakte, String> emailSpalte;
    /** Spalte Telefon */
    @FXML
    private TableColumn<Kundenakte, String> telefonSpalte;
    /** Spalte Anzahl Konten */
    @FXML
    private TableColumn<Kundenakte, Integer> kontenSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Kundenakte, String> statusSpalte;
    /** Anzeige der Trefferzahl */
    @FXML
    private Label anzahl;

    /**
     * die Sitzung des angemeldeten Benutzers
     */
    private Sitzung sitzung;
    /**
     * zeigt die Konten eines Kunden in der Kontenansicht; null, solange nicht gesetzt
     */
    private Consumer<Kundenakte> kontenAnzeigen;

    /**
     * Richtet Spalten, Filter und Reaktionen auf Auswahl und Doppelklick ein.
     */
    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundennummer()));
        nameSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getName()));
        geburtsdatumSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(Darstellung.datum(z.getValue().getGeburtsdatum())));
        emailSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getEmail()));
        telefonSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getTelefon()));
        kontenSpalte.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(
                sitzung.verwaltung().getKontenVon(z.getValue().getKundennummer()).size()));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().toString()));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        // gesperrte Kunden ausgegraut; Doppelklick öffnet das Bearbeiten-Formular
        Darstellung.zeilenEinrichten(tabelle, k -> !k.isAktiv(), k -> bearbeiten());

        statusFilter.setItems(FXCollections.observableArrayList(null, Kundenstatus.AKTIV, Kundenstatus.GESPERRT));
        statusFilter.setButtonCell(Darstellung.auswahlZelle("Alle Status"));
        statusFilter.setCellFactory(l -> Darstellung.auswahlZelle("Alle Status"));

        suche.textProperty().addListener((b, alt, neu) -> aktualisieren());
        statusFilter.valueProperty().addListener((b, alt, neu) -> aktualisieren());
        tabelle.getSelectionModel().selectedItemProperty().addListener((b, alt, neu) -> knoepfeAnpassen());
        knoepfeAnpassen();
    }

    @Override
    public void setSitzung(Sitzung sitzung) {
        this.sitzung = sitzung;
    }

    /**
     * Setzt die Aktion für „Konten anzeigen" (Wechsel in die Kontenansicht).
     * @param kontenAnzeigen bekommt den ausgewählten Kunden
     */
    public void setKontenAnzeigen(Consumer<Kundenakte> kontenAnzeigen) {
        this.kontenAnzeigen = kontenAnzeigen;
        knoepfeAnpassen();
    }

    @Override
    public void aktualisieren() {
        if (sitzung == null)
            return;
        Kundenakte ausgewaehlt = tabelle.getSelectionModel().getSelectedItem();
        List<Kundenakte> treffer = sitzung.verwaltung().kundenSuchen(suche.getText(), statusFilter.getValue());
        tabelle.getItems().setAll(treffer);
        if (ausgewaehlt != null && treffer.contains(ausgewaehlt))
            tabelle.getSelectionModel().select(ausgewaehlt);
        tabelle.refresh();
        int gesamt = sitzung.verwaltung().getKunden().size();
        anzahl.setText(treffer.size() == gesamt
                ? gesamt + (gesamt == 1 ? " Kunde" : " Kunden")
                : treffer.size() + " von " + gesamt + " Kunden");
        knoepfeAnpassen();
    }

    /**
     * Passt die Knöpfe an die Auswahl an.
     */
    private void knoepfeAnpassen() {
        Kundenakte kunde = tabelle.getSelectionModel().getSelectedItem();
        bearbeitenKnopf.setDisable(kunde == null);
        kontenAnzeigenKnopf.setDisable(kunde == null || kontenAnzeigen == null);
        statusKnopf.setDisable(kunde == null);
        boolean aktivieren = kunde != null && !kunde.isAktiv();
        statusKnopf.setText(aktivieren ? "Aktivieren" : "Deaktivieren");
        statusKnopf.getStyleClass().remove("gefahr");
        if (!aktivieren)
            statusKnopf.getStyleClass().add("gefahr");
    }

    /**
     * Reaktion auf „Neuer Kunde": Formular zeigen und den Kunden anlegen.
     */
    @FXML
    private void neuerKunde() {
        Dialoge.Geladen<KundeFormularController> formular = Dialoge.laden("kundeFormular.fxml");
        Kundenakte[] neu = new Kundenakte[1];
        boolean gespeichert = Dialoge.formular(Dialoge.fenster(tabelle), "Neuer Kunde", formular.wurzel(),
                formular.controller().getFehlerfeld(),
                () -> neu[0] = sitzung.verwaltung().kundeAnlegen(sitzung.benutzer(), formular.controller().auslesen()));
        if (gespeichert) {
            suche.clear();
            statusFilter.setValue(null);
            aktualisieren();
            tabelle.getSelectionModel().select(neu[0]);
            tabelle.scrollTo(neu[0]);
        }
    }

    /**
     * Reaktion auf „Bearbeiten": Formular mit den Daten des ausgewählten Kunden zeigen.
     */
    @FXML
    private void bearbeiten() {
        Kundenakte kunde = tabelle.getSelectionModel().getSelectedItem();
        if (kunde == null)
            return;
        Dialoge.Geladen<KundeFormularController> formular = Dialoge.laden("kundeFormular.fxml");
        formular.controller().setKunde(kunde);
        boolean gespeichert = Dialoge.formular(Dialoge.fenster(tabelle), "Kunde " + kunde.getKundennummer()
                        + " bearbeiten", formular.wurzel(), formular.controller().getFehlerfeld(),
                () -> sitzung.verwaltung().kundeBearbeiten(sitzung.benutzer(), kunde.getKundennummer(),
                        formular.controller().auslesen()));
        if (gespeichert)
            aktualisieren();
    }

    /**
     * Reaktion auf „Konten anzeigen": wechselt zu den Konten des ausgewählten Kunden.
     */
    @FXML
    private void kontenAnzeigen() {
        Kundenakte kunde = tabelle.getSelectionModel().getSelectedItem();
        if (kunde != null && kontenAnzeigen != null)
            kontenAnzeigen.accept(kunde);
    }

    /**
     * Reaktion auf „Deaktivieren"/„Aktivieren". Das Deaktivieren muss bestätigt werden.
     */
    @FXML
    private void statusWechseln() {
        Kundenakte kunde = tabelle.getSelectionModel().getSelectedItem();
        if (kunde == null)
            return;
        if (kunde.isAktiv()) {
            boolean bestaetigt = Dialoge.bestaetigen(Dialoge.fenster(tabelle), "Kunde deaktivieren",
                    "Soll der Kunde " + kunde + " wirklich deaktiviert werden?\n\n"
                            + "Der Kunde bleibt mit allen Konten gespeichert, es können aber keine neuen "
                            + "Konten für ihn eröffnet werden. Die Deaktivierung lässt sich rückgängig machen.",
                    "Deaktivieren");
            if (bestaetigt)
                Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                        () -> sitzung.verwaltung().kundeDeaktivieren(sitzung.benutzer(), kunde.getKundennummer()));
        } else {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.verwaltung().kundeAktivieren(sitzung.benutzer(), kunde.getKundennummer()));
        }
        aktualisieren();
    }
}
