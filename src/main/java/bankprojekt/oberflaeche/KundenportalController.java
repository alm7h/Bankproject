package bankprojekt.oberflaeche;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import bankprojekt.verwaltung.Antragsstatus;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenzugang;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

/**
 * Controller des Kundenportals ({@code kundenportal.fxml}): Begrüßung, Navigation
 * zwischen „Meine Konten", „Anträge" und „Meine Daten" sowie Abmelden.
 * <p>
 * Solange der Neukundenantrag nicht genehmigt ist, gibt es noch keine Konten und
 * Kundendaten; dann zeigt das Portal nur die Anträge.
 * @author m.alharethi
 */
public class KundenportalController {
    /**
     * Wird vom FXMLLoader beim Laden des Kundenportals aufgerufen.
     */
    public KundenportalController() {
    }

    /** Gruppe der Navigationsknöpfe (genau einer ist ausgewählt) */
    @FXML
    private ToggleGroup navigation;
    /** Navigationsknopf „Meine Konten" */
    @FXML
    private ToggleButton kontenKnopf;
    /** Navigationsknopf „Anträge" */
    @FXML
    private ToggleButton antraegeKnopf;
    /** Navigationsknopf „Meine Daten" */
    @FXML
    private ToggleButton datenKnopf;
    /** Begrüßung nach Tageszeit */
    @FXML
    private Label begruessung;
    /** Zeile unter der Begrüßung */
    @FXML
    private Label unterzeile;
    /** Anzeige des angemeldeten Zugangs */
    @FXML
    private Label angemeldetAls;
    /** Bereich für die aktuelle Ansicht */
    @FXML
    private StackPane inhalt;

    /**
     * die Sitzung des angemeldeten Kunden
     */
    private Kundensitzung sitzung;
    /**
     * wird beim Abmelden aufgerufen
     */
    private Runnable abmeldeAktion;
    /**
     * bereits geladene Ansichten nach FXML-Dateiname
     */
    private final Map<String, Dialoge.Geladen<? extends Portalansicht>> ansichten = new HashMap<>();

    /**
     * Sorgt dafür, dass immer ein Navigationsknopf ausgewählt bleibt.
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
     * Übergibt die Sitzung, begrüßt den Kunden und zeigt seine Konten – bzw. als
     * Neukunde mit noch offenem Antrag nur die Anträge.
     * @param sitzung die Kundensitzung
     * @param abmeldeAktion wird beim Abmelden aufgerufen
     */
    public void setSitzung(Kundensitzung sitzung, Runnable abmeldeAktion) {
        this.sitzung = Objects.requireNonNull(sitzung);
        this.abmeldeAktion = Objects.requireNonNull(abmeldeAktion);
        Kundenzugang zugang = sitzung.zugang();
        angemeldetAls.setText("Angemeldet als\n" + zugang.getBenutzername());
        Optional<Kundenakte> kunde = sitzung.portal().getKunde(zugang);
        List<Kontoantrag> antraege = sitzung.portal().getAntraege(zugang);
        String vorname = kunde.map(Kundenakte::getVorname).orElseGet(() -> antraege.isEmpty()
                ? zugang.getBenutzername() : antraege.get(antraege.size() - 1).getKundendaten().vorname());
        begruessung.setText(Darstellung.begruessung(LocalTime.now(), vorname));

        boolean istKunde = kunde.isPresent();
        for (ToggleButton knopf : List.of(kontenKnopf, datenKnopf)) {
            knopf.setVisible(istKunde);
            knopf.setManaged(istKunde);
        }
        if (istKunde) {
            unterzeile.setText("Schön, dass Sie da sind. Hier sehen Sie Ihre Konten und Umsätze.");
            kontenZeigen();
        } else {
            boolean offen = antraege.stream().anyMatch(a -> a.getStatus() == Antragsstatus.OFFEN);
            unterzeile.setText(offen
                    ? "Ihr Antrag wird von unseren Mitarbeiterinnen und Mitarbeitern geprüft."
                    : "Zu Ihrem Antrag gibt es eine Entscheidung – die Einzelheiten sehen Sie hier.");
            antraegeZeigen();
        }
    }

    /**
     * Zeigt „Meine Konten".
     */
    @FXML
    private void kontenZeigen() {
        zeigen("portalKonten.fxml", kontenKnopf);
    }

    /**
     * Zeigt die Anträge.
     */
    @FXML
    private void antraegeZeigen() {
        zeigen("portalAntraege.fxml", antraegeKnopf);
    }

    /**
     * Zeigt „Meine Daten".
     */
    @FXML
    private void datenZeigen() {
        zeigen("portalDaten.fxml", datenKnopf);
    }

    /**
     * Lädt eine Ansicht beim ersten Aufruf, zeigt sie an und frischt sie auf.
     * @param fxml Dateiname der Ansicht
     * @param knopf zugehöriger Navigationsknopf
     */
    private void zeigen(String fxml, ToggleButton knopf) {
        Dialoge.Geladen<? extends Portalansicht> ansicht = ansichten.computeIfAbsent(fxml, datei -> {
            Dialoge.Geladen<Portalansicht> geladen = Dialoge.laden(datei);
            geladen.controller().setSitzung(sitzung);
            return geladen;
        });
        knopf.setSelected(true);
        inhalt.getChildren().setAll(ansicht.wurzel());
        ansicht.controller().aktualisieren();
    }

    /**
     * Reaktion auf „Abmelden".
     */
    @FXML
    private void abmelden() {
        abmeldeAktion.run();
    }
}
