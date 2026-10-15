package bankprojekt.oberflaeche;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Beantwortet modale Dialoge im Test, ohne sie anzuzeigen: Der Test legt vorher fest,
 * was mit dem nächsten Dialog geschehen soll (Felder ausfüllen, Knopf drücken).
 * Ein Dialog ohne vorbereitete Antwort lässt den Test scheitern.
 */
final class Dialogantworten {
    /** vorbereitete Antworten, in Reihenfolge */
    private final Deque<Consumer<DialogPane>> antworten = new ArrayDeque<>();
    /** alle bisher „gezeigten" Dialoge */
    private final List<DialogPane> gezeigt = new ArrayList<>();

    /**
     * Meldet sich als Anzeige für Dialoge an.
     */
    Dialogantworten() {
        Dialoge.setAnzeige(this::beantworten);
    }

    /**
     * Bereitet die Antwort auf den nächsten Dialog vor.
     * @param antwort was mit dem Dialog geschehen soll
     * @return this
     */
    Dialogantworten dann(Consumer<DialogPane> antwort) {
        antworten.add(antwort);
        return this;
    }

    /**
     * Bereitet für den nächsten Dialog einen Klick auf einen Knopf vor.
     * @param knopftext Beschriftung des Knopfs
     * @return this
     */
    Dialogantworten klicke(String knopftext) {
        return dann(d -> knopf(d, knopftext).fire());
    }

    /**
     * Bereitet vor, dass der nächste Dialog ohne Knopfdruck geschlossen wird (Fenster-X).
     * @return this
     */
    Dialogantworten schliesse() {
        return dann(d -> { });
    }

    /**
     * Beantwortet einen Dialog mit der nächsten vorbereiteten Antwort.
     * @param dialog der Dialog
     * @return seine Antwort
     */
    private Optional<ButtonType> beantworten(Dialog<ButtonType> dialog) {
        gezeigt.add(dialog.getDialogPane());
        Consumer<DialogPane> antwort = antworten.poll();
        if (antwort == null)
            throw new AssertionError("Unerwarteter Dialog: " + dialog.getHeaderText());
        antwort.accept(dialog.getDialogPane());
        return Optional.ofNullable(dialog.getResult());
    }

    /**
     * Liefert alle bisher beantworteten Dialoge.
     * @return die Dialogflächen in Reihenfolge
     */
    List<DialogPane> gezeigt() {
        return gezeigt;
    }

    /**
     * Liefert den zuletzt beantworteten Dialog.
     * @return die Dialogfläche
     */
    DialogPane letzter() {
        return gezeigt.get(gezeigt.size() - 1);
    }

    /**
     * Prüft, dass alle vorbereiteten Antworten verbraucht wurden.
     */
    void allesBeantwortet() {
        assertTrue(antworten.isEmpty(), "nicht alle erwarteten Dialoge sind erschienen");
    }

    /**
     * Sucht einen Knopf eines Dialogs über seine Beschriftung.
     * @param dialog die Dialogfläche
     * @param text die Beschriftung
     * @return der Knopf
     */
    static Button knopf(DialogPane dialog, String text) {
        return dialog.getButtonTypes().stream()
                .map(typ -> (Button) dialog.lookupButton(typ))
                .filter(b -> b.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Knopf \"" + text + "\" fehlt"));
    }

    /**
     * Liefert den Inhalt (das Formular) eines Dialogs.
     * @param dialog die Dialogfläche
     * @return der Inhalt
     */
    static Parent inhalt(DialogPane dialog) {
        return (Parent) dialog.getContent();
    }
}
