package bankprojekt.oberflaeche;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundendaten;
import bankprojekt.verwaltung.Kundenportal;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * Controller der Registrierung eines Neukunden ({@code registrierung.fxml}): ein
 * Assistent mit Begrüßung, persönlichen Daten, Kontowahl, Zugangsdaten und
 * Zusammenfassung. Jeder Schritt wird beim Klick auf „Weiter" geprüft; Fehler
 * erscheinen unter dem Schritt. „Antrag absenden" registriert den Kunden im
 * {@link Kundenportal}, danach folgt eine Dankesseite.
 * @author m.alharethi
 */
public class RegistrierungController {
    /**
     * Wird vom FXMLLoader beim Laden der Registrierung aufgerufen.
     */
    public RegistrierungController() {
    }

    /** Index der Dankesseite (nach den fünf Schritten) */
    static final int DANKE = 5;
    /** Überschriften der Schritte und der Dankesseite */
    private static final List<String> TITEL = List.of(
            "Willkommen bei unserer Bank!",
            "Erzählen Sie uns von sich",
            "Welches Konto darf es sein?",
            "Ihr Zugang zum Kundenportal",
            "Alles richtig?",
            "Vielen Dank!");

    /** Anzeige „Schritt x von 5" */
    @FXML
    private Label schrittAnzeige;
    /** Überschrift des Schritts */
    @FXML
    private Label titel;
    /** Begrüßungstext */
    @FXML
    private Label willkommenText;
    /** Seite 1: Begrüßung */
    @FXML
    private VBox seiteWillkommen;
    /** Seite 2: persönliche Daten */
    @FXML
    private VBox seiteDaten;
    /** Controller des eingebundenen Kundenformulars */
    @FXML
    private KundeFormularController datenformularController;
    /** Seite 3: Kontowahl */
    @FXML
    private VBox seiteKonto;
    /** Auswahl der Kontoarten (vom Controller gefüllt) */
    @FXML
    private VBox kontoarten;
    /** Auswahl der Währung */
    @FXML
    private ComboBox<Waehrung> waehrung;
    /** Seite 4: Zugangsdaten */
    @FXML
    private VBox seiteZugang;
    /** Eingabe Benutzername */
    @FXML
    private TextField benutzername;
    /** Eingabe Passwort */
    @FXML
    private PasswordField passwort;
    /** Eingabe Passwortwiederholung */
    @FXML
    private PasswordField wiederholung;
    /** Seite 5: Zusammenfassung */
    @FXML
    private VBox seiteZusammenfassung;
    /** Text der Zusammenfassung */
    @FXML
    private Label zusammenfassung;
    /** Abschluss: Dankesseite */
    @FXML
    private VBox seiteDanke;
    /** Text der Dankesseite */
    @FXML
    private Label dankeText;
    /** Anzeige von Fehlern des aktuellen Schritts */
    @FXML
    private Label schrittFehler;
    /** Knopf „Abbrechen" */
    @FXML
    private Button abbrechenKnopf;
    /** Knopf „Zurück" */
    @FXML
    private Button zurueckKnopf;
    /** Knopf „Weiter" / „Antrag absenden" / „Zur Anmeldung" */
    @FXML
    private Button weiterKnopf;

    /**
     * Gruppe der Kontoart-Auswahlknöpfe
     */
    private final ToggleGroup kontoartGruppe = new ToggleGroup();
    /**
     * das Portal, bei dem registriert wird
     */
    private Kundenportal portal;
    /**
     * kehrt zur Anmeldemaske zurück; bekommt den neuen Benutzernamen oder null (Abbruch)
     */
    private Consumer<String> zurAnmeldung;
    /**
     * aktueller Schritt (0 = Begrüßung, {@link #DANKE} = Dankesseite)
     */
    private int schritt;
    /**
     * geprüfte persönliche Daten aus Schritt 2
     */
    private Kundendaten daten;
    /**
     * geprüfter Benutzername aus Schritt 4
     */
    private String gepruefterName;

    /**
     * Füllt Kontoarten und Währungen und zeigt die Begrüßung.
     */
    @FXML
    private void initialize() {
        for (Kontoart art : Kontoart.values()) {
            RadioButton knopf = new RadioButton(art.toString());
            knopf.setUserData(art);
            knopf.setToggleGroup(kontoartGruppe);
            knopf.getStyleClass().add("kontoart-wahl");
            Label beschreibung = new Label(art.getBeschreibung());
            beschreibung.getStyleClass().add("kontoart-beschreibung");
            kontoarten.getChildren().addAll(knopf, beschreibung);
        }
        waehrung.getItems().setAll(Waehrung.values());
        waehrung.setValue(Waehrung.EURO);
        willkommenText.setText("""
                Schön, dass Sie sich für uns entscheiden! In wenigen Schritten beantragen Sie Ihr Konto:

                1. Ihre persönlichen Daten
                2. Die Wahl Ihres Kontos
                3. Ihr Zugang zum Kundenportal
                4. Ein letzter Blick auf Ihre Angaben

                Unsere Mitarbeiterinnen und Mitarbeiter prüfen Ihren Antrag und eröffnen Ihr Konto. \
                Den Stand können Sie jederzeit im Kundenportal verfolgen.""");
        seiteZeigen(0);
    }

    /**
     * Übergibt das Portal und die Rückkehr zur Anmeldemaske.
     * @param portal das Kundenportal
     * @param zurAnmeldung bekommt nach dem Abschluss den neuen Benutzernamen, beim
     *                     Abbrechen null
     */
    public void setPortal(Kundenportal portal, Consumer<String> zurAnmeldung) {
        this.portal = Objects.requireNonNull(portal);
        this.zurAnmeldung = Objects.requireNonNull(zurAnmeldung);
    }

    /**
     * Zeigt einen Schritt und passt Überschrift und Knöpfe an.
     * @param neuerSchritt der Schritt (0 bis {@link #DANKE})
     */
    private void seiteZeigen(int neuerSchritt) {
        schritt = neuerSchritt;
        List<Node> seiten = List.of(seiteWillkommen, seiteDaten, seiteKonto, seiteZugang, seiteZusammenfassung,
                seiteDanke);
        for (int i = 0; i < seiten.size(); i++)
            seiten.get(i).setVisible(i == schritt);
        titel.setText(TITEL.get(schritt));
        schrittAnzeige.setText(schritt < DANKE ? "Schritt " + (schritt + 1) + " von " + DANKE : "Antrag eingegangen");
        zurueckKnopf.setVisible(schritt > 0 && schritt < DANKE);
        abbrechenKnopf.setVisible(schritt < DANKE);
        weiterKnopf.setText(switch (schritt) {
            case 4 -> "Antrag absenden";
            case DANKE -> "Zur Anmeldung";
            default -> "Weiter";
        });
        schrittFehler.setText("");
    }

    /**
     * Liefert die gewählte Kontoart.
     * @return die Kontoart oder null, wenn keine gewählt ist
     */
    private Kontoart gewaehlteKontoart() {
        return kontoartGruppe.getSelectedToggle() == null ? null
                : (Kontoart) kontoartGruppe.getSelectedToggle().getUserData();
    }

    /**
     * Reaktion auf „Weiter": prüft den aktuellen Schritt und geht zum nächsten. Im
     * letzten Schritt wird der Antrag gestellt, auf der Dankesseite geht es zur Anmeldung.
     */
    @FXML
    private void weiter() {
        try {
            switch (schritt) {
                case 1 -> daten = datenformularController.auslesen();
                case 2 -> {
                    if (gewaehlteKontoart() == null)
                        throw new IllegalArgumentException("Bitte wählen Sie eine Kontoart.");
                }
                case 3 -> zugangPruefen();
                case 4 -> antragAbsenden();
                case DANKE -> {
                    zurAnmeldung.accept(gepruefterName);
                    return;
                }
                default -> { }
            }
            seiteZeigen(schritt + 1);
            if (schritt == 4)
                zusammenfassung.setText(zusammenfassungText());
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            schrittFehler.setText(e.getMessage());
        }
    }

    /**
     * Prüft die Zugangsdaten (Schritt 4).
     * @throws IllegalArgumentException wenn sie ungültig sind oder die Wiederholung abweicht
     */
    private void zugangPruefen() {
        char[] eins = passwort.getText().toCharArray();
        char[] zwei = wiederholung.getText().toCharArray();
        try {
            if (!Arrays.equals(eins, zwei))
                throw new IllegalArgumentException("Die beiden Passwörter stimmen nicht überein.");
            gepruefterName = portal.zugangsdatenPruefen(benutzername.getText(), eins);
        } finally {
            Arrays.fill(eins, '\0');
            Arrays.fill(zwei, '\0');
        }
    }

    /**
     * Stellt den Antrag (Schritt 5) und füllt die Dankesseite.
     */
    private void antragAbsenden() {
        char[] geheim = passwort.getText().toCharArray();
        try {
            Kontoantrag antrag = portal.neukundeRegistrieren(daten, gewaehlteKontoart(), waehrung.getValue(),
                    gepruefterName, geheim);
            dankeText.setText("Vielen Dank, " + daten.vorname() + "! Ihr Antrag " + antrag.getAntragsnummer()
                    + " auf ein " + antrag.getKontoart() + " ist bei uns eingegangen. Wir prüfen ihn so schnell "
                    + "wie möglich.\n\nSie können sich ab sofort mit Ihrem Benutzernamen „" + gepruefterName
                    + "“ anmelden und den Stand Ihres Antrags im Kundenportal verfolgen.");
        } finally {
            Arrays.fill(geheim, '\0');
        }
    }

    /**
     * Baut den Text der Zusammenfassung.
     * @return die Angaben in lesbarer Form
     */
    private String zusammenfassungText() {
        String telefon = daten.telefon().isEmpty() ? "–" : daten.telefon();
        return "Name: " + daten.vorname() + " " + daten.nachname()
                + "\nGeburtsdatum: " + Darstellung.datum(daten.geburtsdatum())
                + "\nAdresse: " + daten.adresse().replace("\n", ", ")
                + "\nTelefon: " + telefon
                + "\nE-Mail: " + daten.email()
                + "\n\nKonto: " + gewaehlteKontoart() + " in " + waehrung.getValue()
                + "\nBenutzername: " + gepruefterName;
    }

    /**
     * Reaktion auf „Zurück".
     */
    @FXML
    private void zurueck() {
        seiteZeigen(schritt - 1);
    }

    /**
     * Reaktion auf „Abbrechen": Nach der Begrüßung wird nachgefragt, weil Eingaben
     * verloren gehen.
     */
    @FXML
    private void abbrechen() {
        if (schritt == 0 || Dialoge.bestaetigen(Dialoge.fenster(weiterKnopf), "Registrierung abbrechen",
                "Möchten Sie die Registrierung abbrechen? Ihre Eingaben werden verworfen.", "Verwerfen"))
            zurAnmeldung.accept(null);
    }
}
