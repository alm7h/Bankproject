package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Datenspeicher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * JavaFX-Anwendung der Bankverwaltung.
 * <p>
 * Beim Start wird die Bankverwaltung aus dem {@link Datenspeicher} geladen (beim
 * allerersten Start mit dem Startbenutzer eingerichtet) und die Anmeldemaske gezeigt;
 * den Wechsel zwischen Anmeldemaske und Hauptfenster übernimmt die
 * {@link Fenstersteuerung}. Können die Daten nicht geladen werden, wird die Anwendung
 * nach einer Fehlermeldung beendet.
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
     * Lädt die Daten aus der Standarddatei und zeigt das Fenster mit der Anmeldemaske.
     * @param buehne die von JavaFX bereitgestellte Hauptbühne
     */
    @Override
    public void start(Stage buehne) {
        Fenstersteuerung steuerung = new Fenstersteuerung(buehne, new Datenspeicher(Datenspeicher.standardDatei()));
        if (steuerung.starten())
            buehne.show();
        else
            Platform.exit();
    }
}
