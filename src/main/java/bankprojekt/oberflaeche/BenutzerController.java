package bankprojekt.oberflaeche;

import java.util.Arrays;
import java.util.List;

import bankprojekt.verwaltung.Benutzer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

/**
 * Controller der Benutzerverwaltung ({@code benutzer.fxml}): Mitarbeiterzugänge
 * anlegen, bearbeiten, Passwort zurücksetzen, deaktivieren und aktivieren.
 * Benutzer werden nie gelöscht; der angemeldete Benutzer kann sich nicht selbst
 * deaktivieren.
 * @author m.alharethi
 */
public class BenutzerController implements Ansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public BenutzerController() {
    }

    /** Knopf „Bearbeiten" */
    @FXML
    private Button bearbeitenKnopf;
    /** Knopf „Passwort zurücksetzen" */
    @FXML
    private Button passwortKnopf;
    /** Knopf „Deaktivieren" bzw. „Aktivieren" */
    @FXML
    private Button statusKnopf;
    /** Tabelle der Benutzer */
    @FXML
    private TableView<Benutzer> tabelle;
    /** Spalte Benutzername */
    @FXML
    private TableColumn<Benutzer, String> nameSpalte;
    /** Spalte Anzeigename */
    @FXML
    private TableColumn<Benutzer, String> anzeigeSpalte;
    /** Spalte Status */
    @FXML
    private TableColumn<Benutzer, String> statusSpalte;
    /** Spalte Anlagezeitpunkt */
    @FXML
    private TableColumn<Benutzer, String> angelegtSpalte;
    /** Spalte letzte Anmeldung */
    @FXML
    private TableColumn<Benutzer, String> anmeldungSpalte;
    /** Anzeige der Anzahl */
    @FXML
    private Label anzahl;

    /**
     * die Sitzung des angemeldeten Benutzers
     */
    private Sitzung sitzung;

    /**
     * Richtet Spalten und die Reaktion auf die Auswahl ein.
     */
    @FXML
    private void initialize() {
        nameSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBenutzername()
                + (istSelbst(z.getValue()) ? "  (Sie)" : "")));
        anzeigeSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getAnzeigename()));
        statusSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(z.getValue().isAktiv() ? "aktiv" : "deaktiviert"));
        statusSpalte.setCellFactory(s -> Darstellung.statusZelle());
        angelegtSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(Darstellung.zeitpunkt(z.getValue().getAngelegtAm())));
        anmeldungSpalte.setCellValueFactory(z ->
                new ReadOnlyStringWrapper(Darstellung.zeitpunkt(z.getValue().getLetzteAnmeldung())));
        Darstellung.zeilenEinrichten(tabelle, b -> !b.isAktiv(), b -> bearbeiten());
        tabelle.getSelectionModel().selectedItemProperty().addListener((b, alt, neu) -> knoepfeAnpassen());
        knoepfeAnpassen();
    }

    /**
     * Prüft, ob ein Benutzer der angemeldete Benutzer ist.
     * @param benutzer der Benutzer
     * @return true für den angemeldeten Benutzer
     */
    private boolean istSelbst(Benutzer benutzer) {
        return sitzung != null && sitzung.benutzer() == benutzer;
    }

    @Override
    public void setSitzung(Sitzung sitzung) {
        this.sitzung = sitzung;
    }

    @Override
    public void aktualisieren() {
        if (sitzung == null)
            return;
        Benutzer ausgewaehlt = tabelle.getSelectionModel().getSelectedItem();
        List<Benutzer> alle = sitzung.verwaltung().getBenutzer();
        tabelle.getItems().setAll(alle);
        if (ausgewaehlt != null && alle.contains(ausgewaehlt))
            tabelle.getSelectionModel().select(ausgewaehlt);
        tabelle.refresh();
        long aktiv = alle.stream().filter(Benutzer::isAktiv).count();
        anzahl.setText(alle.size() + " Benutzer, davon " + aktiv + " aktiv");
        knoepfeAnpassen();
    }

    /**
     * Passt die Knöpfe an die Auswahl an. Sich selbst kann man nicht deaktivieren.
     */
    private void knoepfeAnpassen() {
        Benutzer benutzer = tabelle.getSelectionModel().getSelectedItem();
        bearbeitenKnopf.setDisable(benutzer == null);
        passwortKnopf.setDisable(benutzer == null);
        boolean aktivieren = benutzer != null && !benutzer.isAktiv();
        statusKnopf.setText(aktivieren ? "Aktivieren" : "Deaktivieren");
        statusKnopf.setDisable(benutzer == null || istSelbst(benutzer));
        statusKnopf.setTooltip(benutzer != null && istSelbst(benutzer)
                ? new Tooltip("Sie können sich nicht selbst deaktivieren.") : null);
        statusKnopf.getStyleClass().remove("gefahr");
        if (!aktivieren)
            statusKnopf.getStyleClass().add("gefahr");
    }

    /**
     * Reaktion auf „Neuer Benutzer".
     */
    @FXML
    private void neuerBenutzer() {
        Dialoge.Geladen<BenutzerFormularController> formular = Dialoge.laden("benutzerFormular.fxml");
        BenutzerFormularController eingaben = formular.controller();
        eingaben.setModus(BenutzerFormularController.Modus.NEU, null);
        Benutzer[] neu = new Benutzer[1];
        boolean gespeichert = Dialoge.formular(Dialoge.fenster(tabelle), "Neuer Benutzer", formular.wurzel(),
                eingaben.getFehlerfeld(), () -> {
                    char[] passwort = eingaben.getPasswort();
                    try {
                        neu[0] = sitzung.verwaltung().benutzerAnlegen(sitzung.benutzer(),
                                eingaben.getBenutzername(), eingaben.getAnzeigename(), passwort);
                    } finally {
                        Arrays.fill(passwort, '\0');
                    }
                });
        if (gespeichert) {
            aktualisieren();
            tabelle.getSelectionModel().select(neu[0]);
        }
    }

    /**
     * Reaktion auf „Bearbeiten": Anzeigenamen ändern.
     */
    @FXML
    private void bearbeiten() {
        Benutzer benutzer = tabelle.getSelectionModel().getSelectedItem();
        if (benutzer == null)
            return;
        Dialoge.Geladen<BenutzerFormularController> formular = Dialoge.laden("benutzerFormular.fxml");
        BenutzerFormularController eingaben = formular.controller();
        eingaben.setModus(BenutzerFormularController.Modus.BEARBEITEN, benutzer);
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Benutzer " + benutzer.getBenutzername() + " bearbeiten",
                formular.wurzel(), eingaben.getFehlerfeld(),
                () -> sitzung.verwaltung().benutzerBearbeiten(sitzung.benutzer(), benutzer.getBenutzername(),
                        eingaben.getAnzeigename())))
            aktualisieren();
    }

    /**
     * Reaktion auf „Passwort zurücksetzen": neues Passwort zweimal eingeben.
     */
    @FXML
    private void passwortZuruecksetzen() {
        Benutzer benutzer = tabelle.getSelectionModel().getSelectedItem();
        if (benutzer == null)
            return;
        Dialoge.Geladen<BenutzerFormularController> formular = Dialoge.laden("benutzerFormular.fxml");
        BenutzerFormularController eingaben = formular.controller();
        eingaben.setModus(BenutzerFormularController.Modus.PASSWORT, benutzer);
        if (Dialoge.formular(Dialoge.fenster(tabelle), "Passwort von " + benutzer.getBenutzername()
                        + " zurücksetzen", formular.wurzel(), eingaben.getFehlerfeld(), () -> {
                    char[] passwort = eingaben.getPasswort();
                    try {
                        sitzung.verwaltung().passwortZuruecksetzen(sitzung.benutzer(), benutzer.getBenutzername(),
                                passwort);
                    } finally {
                        Arrays.fill(passwort, '\0');
                    }
                }))
            aktualisieren();
    }

    /**
     * Reaktion auf „Deaktivieren"/„Aktivieren". Das Deaktivieren muss bestätigt werden.
     */
    @FXML
    private void statusWechseln() {
        Benutzer benutzer = tabelle.getSelectionModel().getSelectedItem();
        if (benutzer == null)
            return;
        if (benutzer.isAktiv()) {
            if (Dialoge.bestaetigen(Dialoge.fenster(tabelle), "Benutzer deaktivieren",
                    "Soll der Benutzer " + benutzer + " deaktiviert werden?\n\n"
                            + "Er kann sich danach nicht mehr anmelden. Der Zugang bleibt gespeichert und "
                            + "kann wieder aktiviert werden.", "Deaktivieren"))
                Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                        () -> sitzung.verwaltung().benutzerDeaktivieren(sitzung.benutzer(), benutzer.getBenutzername()));
        } else {
            Dialoge.ausfuehren(Dialoge.fenster(tabelle),
                    () -> sitzung.verwaltung().benutzerAktivieren(sitzung.benutzer(), benutzer.getBenutzername()));
        }
        aktualisieren();
    }
}
