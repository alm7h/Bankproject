package bankprojekt.oberflaeche;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX-Anwendung der Bankverwaltung.
 * <p>
 * Vorläufig zeigt sie nur ein leeres Fenster; Anmeldung und Verwaltung
 * folgen.
 *
 * @see Start
 * @author m.alharethi
 */
public class BankAnwendung extends Application {
    /**
     * Wird von JavaFX beim Start der Anwendung aufgerufen.
     */
    public BankAnwendung() {
    }


    /**
     * Zeigt das (vorläufig leere) Hauptfenster.
     * @param buehne die von JavaFX bereitgestellte Hauptbühne
     */
    @Override
    public void start(Stage buehne) {
        StackPane wurzel = new StackPane(new Label("Bankverwaltung"));
        wurzel.setPadding(new Insets(40));
        buehne.setTitle("Bankverwaltung");
        buehne.setScene(new Scene(wurzel, 400, 200));
        buehne.show();
    }
}
