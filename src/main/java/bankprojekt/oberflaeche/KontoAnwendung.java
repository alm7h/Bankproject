package bankprojekt.oberflaeche;

import java.time.LocalDate;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Übung 14 a): JavaFX-Anwendung, die Model und View zusammenbringt.
 * <p>
 * Im Sinne der MVC-Aufteilung erzeugt diese Klasse das {@link Konto} (Model) und
 * lädt die Oberfläche. Gegenüber Übung 13 ist <b>nur der Konstruktoraufruf für die
 * View ersetzt</b>: Statt {@code new KontoOberflaeche()} wird jetzt die FXML-Datei
 * über den {@link FXMLLoader} geladen. Der im FXML angegebene {@link KontoController}
 * wird dabei automatisch erzeugt; ihm wird anschließend das Model übergeben.
 *
 * @see KontoController
 * @see Start
 */
public class KontoAnwendung extends Application {

    /**
     * Übung 14: Erzeugt das Model, lädt die FXML-Oberfläche und verbindet beide.
     * @param buehne die von JavaFX bereitgestellte Hauptbühne
     * @throws Exception wenn die FXML-Datei nicht geladen werden kann
     */
    @Override
    public void start(Stage buehne) throws Exception {
        // Übung 14 (= Übung 13, 2b): Model erzeugen – Inhaber und Konto frei festgelegt.
        // Girokonto mit Dispo, damit sich das Pleite-Bild durch Abheben zeigen lässt.
        Kunde inhaber = new Kunde("Max", "Mustermann",
                "Musterstraße 1\n12345 Musterstadt", LocalDate.of(1990, 1, 1));
        Konto konto = new Girokonto(inhaber, 556662233, new Geldbetrag(1000));

        // Übung 14: Statt "new KontoOberflaeche()" jetzt die FXML-Datei laden.
        FXMLLoader lader = new FXMLLoader(getClass().getResource("KontoOberflaeche.fxml"));
        Parent wurzel = lader.load();

        // Übung 14: Der vom Loader erzeugte Controller bekommt das Model (View <-> Model).
        KontoController controller = lader.getController();
        controller.setKonto(konto);

        Scene szene = new Scene(wurzel);
        buehne.setTitle("Kontoaktionen");
        buehne.setScene(szene);
        buehne.show();
    }
}
