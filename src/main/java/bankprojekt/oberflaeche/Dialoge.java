package bankprojekt.oberflaeche;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Window;

/**
 * Hilfsmethoden für die Oberfläche: FXML laden, Rückfragen, Fehlermeldungen und
 * Formular-Dialoge in einheitlicher Gestaltung.
 * @author m.alharethi
 */
final class Dialoge {
    /**
     * Name der gemeinsamen CSS-Datei
     */
    static final String STIL = "verwaltung.css";

    /**
     * Nicht instanziierbar: Die Klasse enthält nur statische Methoden.
     */
    private Dialoge() {
    }

    /**
     * Eine geladene FXML-Datei: ihr Wurzelknoten und ihr Controller.
     * @param wurzel der Wurzelknoten
     * @param controller der Controller
     * @param <C> Typ des Controllers
     */
    record Geladen<C>(Parent wurzel, C controller) {
    }

    /**
     * Lädt eine FXML-Datei aus dem Paket der Oberfläche.
     * @param fxml Dateiname, z. B. "kunden.fxml"
     * @param <C> Typ des Controllers
     * @return Wurzelknoten und Controller
     * @throws UncheckedIOException wenn die Datei nicht geladen werden kann
     */
    static <C> Geladen<C> laden(String fxml) {
        FXMLLoader lader = new FXMLLoader(Dialoge.class.getResource(fxml));
        try {
            Parent wurzel = lader.load();
            return new Geladen<>(wurzel, lader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Die Oberfläche " + fxml + " konnte nicht geladen werden.", e);
        }
    }

    /**
     * Liefert das Fenster, zu dem ein Knoten gehört (als Besitzer für Dialoge).
     * @param knoten ein angezeigter Knoten
     * @return sein Fenster oder null, wenn er (noch) nicht angezeigt wird
     */
    static Window fenster(Node knoten) {
        return knoten.getScene() == null ? null : knoten.getScene().getWindow();
    }

    /**
     * Stellt eine Ja/Nein-Rückfrage, z. B. vor dem Sperren oder Auflösen.
     * Vorausgewählt ist „Abbrechen", damit nichts versehentlich bestätigt wird.
     * @param besitzer Fenster, über dem die Frage erscheint
     * @param titel Titel der Rückfrage
     * @param frage die Frage
     * @param aktion Beschriftung des Bestätigungsknopfs, z. B. "Sperren"
     * @return true, wenn bestätigt wurde
     */
    static boolean bestaetigen(Window besitzer, String titel, String frage, String aktion) {
        ButtonType ja = new ButtonType(aktion, ButtonData.OK_DONE);
        ButtonType abbrechen = new ButtonType("Abbrechen", ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, frage, ja, abbrechen);
        alert.initOwner(besitzer);
        alert.setTitle(titel);
        alert.setHeaderText(titel);
        gestalten(alert.getDialogPane());
        ((Button) alert.getDialogPane().lookupButton(ja)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(abbrechen)).setDefaultButton(true);
        return alert.showAndWait().filter(ja::equals).isPresent();
    }

    /**
     * Zeigt eine Fehlermeldung.
     * @param besitzer Fenster, über dem die Meldung erscheint
     * @param text die Meldung
     */
    static void fehler(Window besitzer, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        alert.initOwner(besitzer);
        alert.setTitle("Fehler");
        alert.setHeaderText("Die Aktion konnte nicht ausgeführt werden");
        gestalten(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Zeigt ein Formular als Dialog. Beim Klick auf „Speichern" wird die Aktion
     * ausgeführt; meldet sie einen Eingabefehler ({@link IllegalArgumentException}
     * oder {@link IllegalStateException}), bleibt der Dialog offen und die Meldung
     * erscheint im Fehlerfeld des Formulars. Scheitert nur das Speichern auf die
     * Festplatte, wird das gemeldet und der Dialog trotzdem geschlossen.
     * @param besitzer Fenster, über dem der Dialog erscheint
     * @param titel Titel des Dialogs
     * @param inhalt das Formular
     * @param fehlerfeld Feld im Formular für Fehlermeldungen
     * @param speichern die Aktion, die die Eingaben übernimmt
     * @return true, wenn gespeichert wurde
     */
    static boolean formular(Window besitzer, String titel, Parent inhalt, Label fehlerfeld, Runnable speichern) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(besitzer);
        dialog.setTitle(titel);
        dialog.setHeaderText(titel);
        ButtonType speichernTyp = new ButtonType("Speichern", ButtonData.OK_DONE);
        // eigene Beschriftung statt ButtonType.CANCEL, das je nach Locale "Cancel" heißt
        ButtonType abbrechenTyp = new ButtonType("Abbrechen", ButtonData.CANCEL_CLOSE);
        DialogPane flaeche = dialog.getDialogPane();
        flaeche.setContent(inhalt);
        flaeche.getButtonTypes().addAll(speichernTyp, abbrechenTyp);
        gestalten(flaeche);
        fehlerfeld.setText("");
        Button speichernKnopf = (Button) flaeche.lookupButton(speichernTyp);
        speichernKnopf.getStyleClass().add("primaer");
        speichernKnopf.addEventFilter(ActionEvent.ACTION, ereignis -> {
            try {
                speichern.run();
            } catch (IllegalArgumentException | IllegalStateException e) {
                fehlerfeld.setText(e.getMessage());
                ereignis.consume();   // Dialog bleibt offen
            } catch (UncheckedIOException e) {
                fehler(besitzer, e.getMessage());
            }
        });
        Optional<ButtonType> ergebnis = dialog.showAndWait();
        return ergebnis.filter(speichernTyp::equals).isPresent();
    }

    /**
     * Führt eine Aktion aus und zeigt Fehler als Meldung an.
     * @param besitzer Fenster für eine eventuelle Fehlermeldung
     * @param aktion die Aktion
     * @return true, wenn die Aktion ohne Fehler ausgeführt wurde
     */
    static boolean ausfuehren(Window besitzer, Runnable aktion) {
        try {
            aktion.run();
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            fehler(besitzer, e.getMessage());
            return false;
        }
    }

    /**
     * Gibt einem Dialog die gemeinsame Gestaltung.
     * @param flaeche die Dialogfläche
     */
    private static void gestalten(DialogPane flaeche) {
        flaeche.getStylesheets().add(Dialoge.class.getResource(STIL).toExternalForm());
    }
}
