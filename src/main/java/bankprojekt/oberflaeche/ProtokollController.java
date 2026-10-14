package bankprojekt.oberflaeche;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import bankprojekt.verwaltung.Protokollaktion;
import bankprojekt.verwaltung.Protokolleintrag;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Controller der Protokollansicht ({@code protokoll.fxml}): zeigt alle
 * protokollierten Aktionen, die neuesten zuerst, mit Textsuche und Filter nach
 * Aktion. Das Protokoll ist nur lesbar.
 * @author m.alharethi
 */
public class ProtokollController implements Ansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public ProtokollController() {
    }

    /** Suchfeld (Benutzer und Details) */
    @FXML
    private TextField suche;
    /** Filter nach Aktion; null für alle */
    @FXML
    private ComboBox<Protokollaktion> aktionFilter;
    /** Tabelle der Einträge */
    @FXML
    private TableView<Protokolleintrag> tabelle;
    /** Spalte Zeitpunkt */
    @FXML
    private TableColumn<Protokolleintrag, String> zeitSpalte;
    /** Spalte Benutzer */
    @FXML
    private TableColumn<Protokolleintrag, String> benutzerSpalte;
    /** Spalte Aktion */
    @FXML
    private TableColumn<Protokolleintrag, String> aktionSpalte;
    /** Spalte Details */
    @FXML
    private TableColumn<Protokolleintrag, String> detailsSpalte;
    /** Anzeige der Trefferzahl */
    @FXML
    private Label anzahl;

    /**
     * die Sitzung des angemeldeten Benutzers
     */
    private Sitzung sitzung;

    /**
     * Richtet Spalten und Filter ein.
     */
    @FXML
    private void initialize() {
        zeitSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(Darstellung.zeitpunkt(z.getValue().zeitpunkt())));
        benutzerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().benutzer()));
        aktionSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().aktion().toString()));
        detailsSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().details()));

        List<Protokollaktion> aktionen = new ArrayList<>();
        aktionen.add(null);   // steht für "Alle Aktionen"
        Collections.addAll(aktionen, Protokollaktion.values());
        aktionFilter.setItems(FXCollections.observableArrayList(aktionen));
        aktionFilter.setButtonCell(Darstellung.auswahlZelle("Alle Aktionen"));
        aktionFilter.setCellFactory(l -> Darstellung.auswahlZelle("Alle Aktionen"));

        suche.textProperty().addListener((b, alt, neu) -> aktualisieren());
        aktionFilter.valueProperty().addListener((b, alt, neu) -> aktualisieren());
    }

    @Override
    public void setSitzung(Sitzung sitzung) {
        this.sitzung = sitzung;
    }

    @Override
    public void aktualisieren() {
        if (sitzung == null)
            return;
        List<Protokolleintrag> alle = new ArrayList<>(sitzung.verwaltung().getProtokoll());
        Collections.reverse(alle);   // neueste zuerst
        String begriff = suche.getText() == null ? "" : suche.getText().strip().toLowerCase(Locale.ROOT);
        Protokollaktion aktion = aktionFilter.getValue();
        List<Protokolleintrag> treffer = alle.stream()
                .filter(e -> aktion == null || e.aktion() == aktion)
                .filter(e -> begriff.isEmpty()
                        || e.benutzer().toLowerCase(Locale.ROOT).contains(begriff)
                        || e.details().toLowerCase(Locale.ROOT).contains(begriff))
                .toList();
        tabelle.getItems().setAll(treffer);
        anzahl.setText(treffer.size() == alle.size()
                ? alle.size() + " Einträge"
                : treffer.size() + " von " + alle.size() + " Einträgen");
    }
}
