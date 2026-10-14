package bankprojekt.oberflaeche;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;

/**
 * Kleine Hilfen für Oberflächentests: FXML in eine (nicht angezeigte) Szene laden,
 * Knoten finden, Knöpfe drücken.
 */
final class FxHilfe {
    private FxHilfe() {
    }

    /**
     * Lädt eine FXML-Datei in eine Szene fester Größe und legt sie einmal aus,
     * damit z. B. Tabellenzellen entstehen.
     * @param fxml Dateiname
     * @param <C> Typ des Controllers
     * @return Wurzel und Controller
     */
    static <C> Dialoge.Geladen<C> laden(String fxml) {
        Dialoge.Geladen<C> geladen = Dialoge.laden(fxml);
        new Scene(geladen.wurzel(), 1240, 760);
        auslegen(geladen.wurzel());
        return geladen;
    }

    /**
     * Wendet CSS an und legt den Knoten aus (erzeugt z. B. Tabellenzeilen).
     * @param wurzel der Knoten
     */
    static void auslegen(Parent wurzel) {
        wurzel.applyCss();
        wurzel.layout();
    }

    /**
     * Sucht einen Knoten über seine fx:id.
     * @param wurzel wo gesucht wird
     * @param id die fx:id
     * @param <T> erwarteter Typ
     * @return der Knoten
     */
    @SuppressWarnings("unchecked")
    static <T extends Node> T suche(Parent wurzel, String id) {
        Node knoten = wurzel.lookup("#" + id);
        if (knoten == null)
            throw new AssertionError("#" + id + " nicht gefunden");
        return (T) knoten;
    }

    /**
     * Sucht einen Knopf über seine Beschriftung.
     * @param wurzel wo gesucht wird
     * @param text die Beschriftung
     * @return der Knopf
     */
    static Button knopf(Parent wurzel, String text) {
        return wurzel.lookupAll(".button").stream()
                .filter(n -> n instanceof Button b && text.equals(b.getText()))
                .map(n -> (Button) n)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Knopf \"" + text + "\" fehlt"));
    }

    /**
     * Liefert den angezeigten Wert einer Tabellenzelle.
     * @param tabelle die Tabelle
     * @param spaltentitel Überschrift der Spalte
     * @param zeile der Zeileninhalt
     * @param <S> Typ der Zeilen
     * @return der Zellwert
     */
    static <S> Object zelle(TableView<S> tabelle, String spaltentitel, S zeile) {
        TableColumn<S, ?> spalte = tabelle.getColumns().stream()
                .filter(s -> spaltentitel.equals(s.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Spalte \"" + spaltentitel + "\" fehlt"));
        return spalte.getCellData(zeile);
    }

    /**
     * Liefert die Tabelle einer Ansicht.
     * @param wurzel die Ansicht
     * @param <S> Typ der Zeilen
     * @return die Tabelle mit der fx:id "tabelle"
     */
    static <S> TableView<S> tabelle(Parent wurzel) {
        return suche(wurzel, "tabelle");
    }

    /**
     * Liefert den Text eines Labels.
     * @param wurzel wo gesucht wird
     * @param id die fx:id des Labels
     * @return der Text
     */
    static String text(Parent wurzel, String id) {
        return FxHilfe.<Label>suche(wurzel, id).getText();
    }

    /**
     * Drückt einen Knopf über seine fx:id.
     * @param wurzel wo gesucht wird
     * @param id die fx:id
     */
    static void druecke(Parent wurzel, String id) {
        FxHilfe.<ButtonBase>suche(wurzel, id).fire();
    }

    /**
     * Setzt den Text eines Eingabefelds.
     * @param wurzel wo gesucht wird
     * @param id die fx:id des Felds
     * @param text der Text
     */
    static void tippe(Parent wurzel, String id, String text) {
        FxHilfe.<TextInputControl>suche(wurzel, id).setText(text);
    }
}
