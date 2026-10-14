package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

/**
 * Controller im Sinne von MVC für ein einzelnes Konto – jetzt
 * zugleich der <b>FXML-Controller</b> zur {@code KontoOberflaeche.fxml}.
 * <p>
 * Der MVC-Controller verbindet Oberfläche und Model: Die {@code @FXML}-Felder
 * werden vom {@link javafx.fxml.FXMLLoader} anhand der {@code fx:id}s der
 * FXML-Datei gefüllt, {@link #initialize()} wird nach dem Laden aufgerufen und
 * {@link #aktionAusloesen()} ist über {@code onAction="#aktionAusloesen"} in der
 * FXML-Datei verdrahtet. Model und View verbindet {@link #setKonto(Konto)}.
 * <p>
 * Gestartet wird die Anwendung über {@link Start} bzw. {@link KontoAnwendung};
 * dieser Controller wird nicht direkt ausgeführt.
 *
 * @see KontoAnwendung
 * @see Start
 * @author m.alharethi
 */
public class KontoController {
    /**
     * Wird vom FXMLLoader beim Laden der Oberfläche aufgerufen.
     */
    public KontoController() {
    }


    // Anzeige-Elemente – vom FXMLLoader über die fx:ids eingefügt.

    /** Überschrift, zeigt die Kontonummer */
    @FXML
    private Label ueberschrift;
    /** Anzeige des Kontostandes */
    @FXML
    private Label stand;
    /** Pleite-Bild, sichtbar bei negativem Kontostand */
    @FXML
    private ImageView pleiteBild;
    /** RadioButton für den gesperrten Zustand */
    @FXML
    private RadioButton gesperrtRadio;
    /** RadioButton für den freigegebenen Zustand */
    @FXML
    private RadioButton freigegebenRadio;
    /** Anzeige des Inhabernamens */
    @FXML
    private Label name;
    /** Eingabefeld für die Adresse des Inhabers */
    @FXML
    private TextArea adresse;
    /** Auswahl des Geldbetrags für eine Kontoaktion */
    @FXML
    private ChoiceBox<Geldbetrag> betragAuswahl;
    /** Auswahl der Aktion (Abheben/Einzahlen) */
    @FXML
    private ChoiceBox<String> aktionAuswahl;

    /**
     * Das Model – das anzuzeigende und zu verändernde Konto
     */
    private Konto konto;

    /**
     * Wird vom {@link javafx.fxml.FXMLLoader} nach dem Laden der
     * FXML-Datei automatisch aufgerufen. Hier stehen nur noch Initialisierungen,
     * die sich <i>nicht</i> in FXML ausdrücken lassen: Die {@link Geldbetrag}-Werte
     * der ersten ChoiceBox sind keine FXML-tauglichen Objekte und werden daher
     * hier gesetzt. Die zweite ChoiceBox (Aktionen) wird bereits in der FXML-Datei
     * gefüllt.
     */
    @FXML
    private void initialize() {
        // Die vorgegebenen Geldbeträge (100 Euro, 300 Denar, 10000 Franc, 500 Dobra)
        betragAuswahl.setItems(FXCollections.observableArrayList(
                new Geldbetrag(100, Waehrung.EURO),
                new Geldbetrag(300, Waehrung.DENAR),
                new Geldbetrag(10000, Waehrung.FRANC),
                new Geldbetrag(500, Waehrung.DOBRA)));
        betragAuswahl.getSelectionModel().selectFirst();
        // Standard-Aktion "Abheben" vorauswählen
        aktionAuswahl.getSelectionModel().selectFirst();
    }

    /**
     * Verbindet die Oberfläche mit dem
     * übergebenen Konto. Alle Anzeigeelemente werden an die Properties des Models
     * gebunden, sodass die Oberfläche immer den aktuellen Zustand zeigt. Adresse
     * und Gesperrt-Zustand sind beidseitig gebunden und über die Oberfläche änderbar.
     * <p>
     * Wird vom Bootstrapper {@link KontoAnwendung} nach dem Laden der FXML-Datei
     * aufgerufen.
     *
     * @param konto das anzuzeigende Konto (Model)
     */
    public void setKonto(Konto konto) {
        this.konto = konto;

        // Überschrift zeigt die Kontonummer (ändert sich nicht)
        ueberschrift.setText(Long.toString(konto.getKontonummer()));

        // Immer den aktuellen Kontostand anzeigen ...
        stand.textProperty().bind(konto.kontostandProperty().asString());
        // ... und das Pleite-Bild einblenden, sobald der Kontostand negativ wird
        pleiteBild.visibleProperty().bind(konto.imPlusProperty().not());

        // Name des Kontoinhabers anzeigen
        name.setText(konto.getInhaber().getName());

        // Adresse anzeigen und per Textfeld ändern (beidseitig)
        adresse.textProperty().bindBidirectional(konto.getInhaber().adresseProperty());

        // Gesperrt-Zustand beidseitig an die RadioButtons koppeln.
        // Da beide RadioButtons in derselben ToggleGroup liegen, genügt es, den
        // "gesperrt"-Knopf an die Property zu binden; der "freigegeben"-Knopf ist
        // automatisch das Gegenstück.
        gesperrtRadio.selectedProperty().bindBidirectional(konto.gesperrtProperty());
        freigegebenRadio.setSelected(!gesperrtRadio.isSelected());
    }

    /**
     * Reaktion auf den "Aktion auslösen"-Knopf (in FXML per
     * {@code onAction} verdrahtet). Liest den gewählten Geldbetrag und die
     * gewählte Aktion aus den ChoiceBoxen und führt sie auf dem Konto aus.
     * Meldungen werden über eine Alert-Box angezeigt.
     */
    @FXML
    private void aktionAusloesen() {
        Geldbetrag betrag = betragAuswahl.getValue();
        String aktion = aktionAuswahl.getValue();
        if (betrag == null || aktion == null) {
            zeigeMeldung("Bitte einen Betrag und eine Aktion auswählen.");
            return;
        }
        try {
            if ("Abheben".equals(aktion)) {
                boolean erfolg = konto.abheben(betrag);
                if (erfolg) {
                    zeigeMeldung("Abhebung von " + betrag + " erfolgreich.");
                } else {
                    zeigeMeldung("Abhebung abgelehnt: nicht genügend Deckung.");
                }
            } else {
                konto.einzahlen(betrag);
                zeigeMeldung("Einzahlung von " + betrag + " erfolgreich.");
            }
        } catch (GesperrtException e) {
            // Gilt für beide Aktionen: Auch Einzahlungen sind auf ein gesperrtes Konto nicht möglich
            zeigeMeldung("Das Konto ist gesperrt – " + aktion + " nicht möglich.");
        } catch (IllegalArgumentException e) {
            zeigeMeldung("Aktion nicht möglich: " + e.getMessage());
        }
    }

    /**
     * Zeigt eine Meldung in einer Alert-Box an.
     * @param text der anzuzeigende Text
     */
    private void zeigeMeldung(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
