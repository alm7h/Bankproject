package bankprojekt.oberflaeche;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

/**
 * Controller des Hauptfensters ({@code hauptfenster.fxml}): Navigation zwischen
 * den Ansichten und Abmelden.
 * <p>
 * Jede Ansicht wird beim ersten Aufruf geladen, bekommt die {@link Sitzung} und
 * wird danach bei jedem Wechsel nur noch aufgefrischt.
 * @author m.alharethi
 */
public class HauptfensterController {
    /**
     * Wird vom FXMLLoader beim Laden des Hauptfensters aufgerufen.
     */
    public HauptfensterController() {
    }

    /** Gruppe der Navigationsknöpfe (genau einer ist ausgewählt) */
    @FXML
    private ToggleGroup navigation;
    /** Navigationsknopf „Kunden" */
    @FXML
    private ToggleButton kundenKnopf;
    /** Arbeitsbereich für die aktuelle Ansicht */
    @FXML
    private StackPane inhalt;
    /** Anzeige des angemeldeten Benutzers */
    @FXML
    private Label angemeldetAls;

    /**
     * die Sitzung des angemeldeten Benutzers
     */
    private Sitzung sitzung;
    /**
     * wird beim Abmelden aufgerufen
     */
    private Runnable abmeldeAktion;
    /**
     * bereits geladene Ansichten nach FXML-Dateiname
     */
    private final Map<String, Dialoge.Geladen<? extends Ansicht>> ansichten = new HashMap<>();

    /**
     * Sorgt dafür, dass immer ein Navigationsknopf ausgewählt bleibt (ein Klick auf
     * den aktiven Knopf würde ihn sonst abwählen).
     */
    @FXML
    private void initialize() {
        navigation.selectedToggleProperty().addListener((beobachtet, alt, neu) -> {
            if (neu == null && alt != null)
                alt.setSelected(true);
        });
    }

    /**
     * Übergibt die Sitzung und die Abmeldeaktion und zeigt die Kundenansicht.
     * @param sitzung die Sitzung des angemeldeten Benutzers
     * @param abmeldeAktion wird beim Abmelden aufgerufen
     */
    public void setSitzung(Sitzung sitzung, Runnable abmeldeAktion) {
        this.sitzung = Objects.requireNonNull(sitzung);
        this.abmeldeAktion = Objects.requireNonNull(abmeldeAktion);
        angemeldetAls.setText("Angemeldet als\n" + sitzung.benutzer().getAnzeigename()
                + " (" + sitzung.benutzer().getBenutzername() + ")");
        kundenZeigen();
    }

    /**
     * Zeigt die Kundenansicht.
     */
    @FXML
    private void kundenZeigen() {
        zeigen("kunden.fxml", kundenKnopf);
    }

    /**
     * Lädt eine Ansicht beim ersten Aufruf, zeigt sie an und frischt sie auf.
     * @param fxml Dateiname der Ansicht
     * @param knopf zugehöriger Navigationsknopf
     * @param <C> Typ des Controllers
     * @return der Controller der Ansicht
     */
    @SuppressWarnings("unchecked")
    private <C extends Ansicht> C zeigen(String fxml, ToggleButton knopf) {
        Dialoge.Geladen<? extends Ansicht> ansicht = ansichten.computeIfAbsent(fxml, datei -> {
            Dialoge.Geladen<Ansicht> geladen = Dialoge.laden(datei);
            geladen.controller().setSitzung(sitzung);
            return geladen;
        });
        knopf.setSelected(true);
        inhalt.getChildren().setAll(ansicht.wurzel());
        ansicht.controller().aktualisieren();
        return (C) ansicht.controller();
    }

    /**
     * Reaktion auf „Abmelden".
     */
    @FXML
    private void abmelden() {
        abmeldeAktion.run();
    }
}
