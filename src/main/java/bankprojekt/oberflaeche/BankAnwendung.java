package bankprojekt.oberflaeche;

import java.io.IOException;
import java.io.UncheckedIOException;

import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Datenspeicher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * JavaFX-Anwendung der Bankverwaltung.
 * <p>
 * Beim Start wird die {@link Bankverwaltung} aus dem {@link Datenspeicher} geladen
 * (beim allerersten Start mit dem Startbenutzer eingerichtet) und die Anmeldemaske
 * gezeigt. Erst nach erfolgreicher Anmeldung entsteht eine {@link Sitzung} und das
 * Hauptfenster wird geladen; beim Abmelden wird es verworfen und wieder die
 * Anmeldemaske gezeigt. Ohne Anmeldung ist also keine Verwaltungsansicht erreichbar.
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
     * das einzige Fenster der Anwendung
     */
    private Stage buehne;
    /**
     * die geladene Verwaltung
     */
    private Bankverwaltung verwaltung;
    /**
     * prüft Anmeldungen; bleibt über Ab- und Anmeldungen hinweg bestehen, damit
     * eine Sperre nach Fehlversuchen nicht durch Abmelden umgangen wird
     */
    private Anmeldung anmeldung;

    /**
     * Lädt die Daten und zeigt die Anmeldemaske. Können die Daten nicht geladen
     * werden, erscheint eine Fehlermeldung und die Anwendung wird beendet.
     * @param buehne die von JavaFX bereitgestellte Hauptbühne
     */
    @Override
    public void start(Stage buehne) {
        this.buehne = buehne;
        Datenspeicher speicher = new Datenspeicher(Datenspeicher.standardDatei());
        try {
            verwaltung = speicher.laden();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Die Daten konnten nicht aus " + speicher.getDatei() + " geladen werden:\n"
                            + e.getMessage(), ButtonType.OK);
            alert.setHeaderText("Start nicht möglich");
            alert.showAndWait();
            Platform.exit();
            return;
        }
        anmeldung = new Anmeldung(verwaltung);
        buehne.setTitle("Bankverwaltung");
        anmeldemaskeZeigen();
        buehne.show();
    }

    /**
     * Zeigt die Anmeldemaske.
     */
    private void anmeldemaskeZeigen() {
        Dialoge.Geladen<LoginController> login = Dialoge.laden("login.fxml");
        login.controller().setAnmeldung(anmeldung, benutzer -> {
            login.controller().beenden();
            hauptfensterZeigen(benutzer);
        });
        szeneSetzen(new Scene(login.wurzel()), 520, 460, false);
        buehne.setTitle("Bankverwaltung – Anmeldung");
    }

    /**
     * Zeigt nach erfolgreicher Anmeldung das Hauptfenster.
     * @param benutzer der angemeldete Benutzer
     */
    private void hauptfensterZeigen(Benutzer benutzer) {
        Sitzung sitzung = new Sitzung(verwaltung, benutzer);
        Dialoge.Geladen<HauptfensterController> haupt = Dialoge.laden("hauptfenster.fxml");
        haupt.controller().setSitzung(sitzung, () -> abmelden(sitzung));
        szeneSetzen(new Scene(haupt.wurzel()), 1240, 760, true);
        buehne.setTitle("Bankverwaltung – " + benutzer.getAnzeigename());
    }

    /**
     * Meldet den Benutzer der Sitzung ab und kehrt zur Anmeldemaske zurück.
     * @param sitzung die zu beendende Sitzung
     */
    private void abmelden(Sitzung sitzung) {
        try {
            anmeldung.abmelden(sitzung.benutzer());
        } catch (UncheckedIOException e) {
            // Abgemeldet wird trotzdem; nur der Protokolleintrag ist nicht gespeichert
            Dialoge.fehler(buehne, e.getMessage());
        }
        anmeldemaskeZeigen();
    }

    /**
     * Setzt eine neue Szene und passt die Fenstergröße an.
     * @param szene die Szene
     * @param breite gewünschte Breite
     * @param hoehe gewünschte Höhe
     * @param veraenderbar ob der Benutzer die Größe ändern darf
     */
    private void szeneSetzen(Scene szene, double breite, double hoehe, boolean veraenderbar) {
        buehne.setScene(szene);
        buehne.setResizable(veraenderbar);
        buehne.setMinWidth(veraenderbar ? 1000 : 0);
        buehne.setMinHeight(veraenderbar ? 640 : 0);
        buehne.setWidth(breite);
        buehne.setHeight(hoehe);
        buehne.centerOnScreen();
    }
}
