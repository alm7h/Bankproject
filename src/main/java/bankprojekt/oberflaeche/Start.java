package bankprojekt.oberflaeche;

import javafx.application.Application;

/**
 * Übung 13/14: Startklasse für die Konto-Anwendung.
 * <p>
 * Diese Klasse erweitert bewusst <b>nicht</b> {@link Application}. Dadurch lässt
 * sich die JavaFX-Anwendung auch dann per „Run" starten, wenn JavaFX nur über
 * den Klassenpfad (als Maven-Abhängigkeit) und nicht über den Modulpfad
 * eingebunden ist. Startet man dagegen die {@link Application}-Klasse
 * {@link KontoAnwendung} direkt, bricht die JVM schon vor dem eigentlichen
 * Programm mit der Meldung „JavaFX runtime components are missing" ab.
 * <p>
 * Also: <b>diese Klasse ausführen</b>, nicht {@link KontoAnwendung}.
 *
 * @see KontoAnwendung
 */
public class Start {

    /**
     * Startet die JavaFX-Anwendung, indem die {@link KontoAnwendung} als
     * JavaFX-{@link Application} hochgefahren wird.
     * @param args Kommandozeilenargumente (werden nicht ausgewertet)
     */
    public static void main(String[] args) {
        // Übung 14: startet jetzt die FXML-basierte KontoAnwendung
        Application.launch(KontoAnwendung.class, args);
    }
}
