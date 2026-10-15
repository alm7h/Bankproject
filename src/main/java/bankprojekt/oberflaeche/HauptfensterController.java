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
    /** Navigationsknopf „Konten" */
    @FXML
    private ToggleButton kontenKnopf;
    /** Navigationsknopf „Anträge" (mit Zahl der offenen Anträge) */
    @FXML
    private ToggleButton antraegeKnopf;
    /** Navigationsknopf „Benutzer" */
    @FXML
    private ToggleButton benutzerKnopf;
    /** Navigationsknopf „Protokoll" */
    @FXML
    private ToggleButton protokollKnopf;
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
        // Wechselt die Auswahl auf null, war vorher zwangsläufig ein Knopf gewählt
        navigation.selectedToggleProperty().addListener((beobachtet, alt, neu) -> {
            if (neu == null)
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
     * Zeigt die Zahl der offenen Kontoanträge in der Navigation, z. B. „Anträge (2)".
     */
    private void antraegeZaehlen() {
        long offen = sitzung.verwaltung().getAnzahlOffeneAntraege();
        antraegeKnopf.setText(offen == 0 ? "Anträge" : "Anträge (" + offen + ")");
    }

    /**
     * Zeigt die Kundenansicht.
     */
    @FXML
    private void kundenZeigen() {
        KundenController kunden = zeigen("kunden.fxml", kundenKnopf);
        kunden.setKontenAnzeigen(kunde -> kontenAnsicht().kontenVonKundeZeigen(kunde));
    }

    /**
     * Zeigt die Kontenansicht.
     */
    @FXML
    private void kontenZeigen() {
        kontenAnsicht();
    }

    /**
     * Zeigt die Kontenansicht und liefert ihren Controller.
     * @return der Controller der Kontenansicht
     */
    private KontenController kontenAnsicht() {
        return zeigen("konten.fxml", kontenKnopf);
    }

    /**
     * Zeigt die Kontoanträge aus dem Kundenportal.
     */
    @FXML
    private void antraegeZeigen() {
        AntraegeController antraege = zeigen("antraege.fxml", antraegeKnopf);
        antraege.setNachEntscheidung(this::antraegeZaehlen);
    }

    /**
     * Zeigt die Benutzerverwaltung.
     */
    @FXML
    private void benutzerZeigen() {
        zeigen("benutzer.fxml", benutzerKnopf);
    }

    /**
     * Zeigt das Protokoll.
     */
    @FXML
    private void protokollZeigen() {
        zeigen("protokoll.fxml", protokollKnopf);
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
        antraegeZaehlen();
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
