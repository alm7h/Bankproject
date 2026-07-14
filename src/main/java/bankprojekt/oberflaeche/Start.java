package bankprojekt.oberflaeche;

import javafx.application.Application;

/**
 * Übung 13: Startklasse für die Konto-Anwendung.
 * <p>
 * Diese Klasse erweitert bewusst <b>nicht</b> {@link Application}. Dadurch lässt
 * sich die JavaFX-Anwendung auch dann per „Run" starten, wenn JavaFX nur über
 * den Klassenpfad (als Maven-Abhängigkeit) und nicht über den Modulpfad
 * eingebunden ist. Startet man dagegen die {@link Application}-Klasse
 * {@link KontoController} direkt, bricht die JVM schon vor dem eigentlichen
 * Programm mit der Meldung „JavaFX runtime components are missing" ab.
 * <p>
 * Also: <b>diese Klasse ausführen</b>, nicht {@link KontoController}.
 *
 * @see KontoController
 */
public class Start {

    /**
     * Startet die JavaFX-Anwendung, indem der {@link KontoController} als
     * JavaFX-{@link Application} hochgefahren wird.
     * @param args Kommandozeilenargumente (werden nicht ausgewertet)
     */
    public static void main(String[] args) {
        Application.launch(KontoController.class, args);
    }
}
