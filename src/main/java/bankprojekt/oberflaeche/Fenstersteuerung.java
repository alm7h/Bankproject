package bankprojekt.oberflaeche;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Datenspeicher;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Steuert, was das Fenster der {@link BankAnwendung} zeigt: Nach dem Laden der
 * Daten die Anmeldemaske, nach erfolgreicher Anmeldung das Hauptfenster mit einer
 * neuen {@link Sitzung}, nach dem Abmelden wieder die Anmeldemaske. Ohne Anmeldung
 * ist also keine Verwaltungsansicht erreichbar.
 * <p>
 * Die {@link Anmeldung} bleibt über Ab- und Anmeldungen hinweg bestehen, damit eine
 * Sperre nach Fehlversuchen nicht durch Abmelden umgangen werden kann.
 * @author m.alharethi
 */
final class Fenstersteuerung {
    /**
     * das Fenster der Anwendung
     */
    private final Stage buehne;
    /**
     * lädt und speichert die Daten
     */
    private final Datenspeicher speicher;
    /**
     * die geladene Verwaltung
     */
    private Bankverwaltung verwaltung;
    /**
     * prüft Anmeldungen
     */
    private Anmeldung anmeldung;

    /**
     * Erstellt die Steuerung für ein Fenster.
     * @param buehne das Fenster
     * @param speicher der Datenspeicher, aus dem die Verwaltung geladen wird
     */
    Fenstersteuerung(Stage buehne, Datenspeicher speicher) {
        this.buehne = Objects.requireNonNull(buehne);
        this.speicher = Objects.requireNonNull(speicher);
    }

    /**
     * Lädt die Daten und zeigt die Anmeldemaske. Können die Daten nicht geladen
     * werden, erscheint eine Fehlermeldung.
     * @return true, wenn die Anwendung bereit ist; false, wenn nicht geladen werden konnte
     */
    boolean starten() {
        try {
            verwaltung = speicher.laden();
        } catch (IOException e) {
            Dialoge.fehler(null, "Start nicht möglich",
                    "Die Daten konnten nicht aus " + speicher.getDatei() + " geladen werden:\n" + e.getMessage());
            return false;
        }
        anmeldung = new Anmeldung(verwaltung);
        anmeldemaskeZeigen();
        return true;
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
