package bankprojekt.oberflaeche;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;

import bankprojekt.verwaltung.Anmeldeergebnis;
import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Datenspeicher;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Steuert, was das Fenster der {@link BankAnwendung} zeigt: Nach dem Laden der
 * Daten die gemeinsame Anmeldemaske; nach erfolgreicher Anmeldung eines Mitarbeiters
 * das Hauptfenster der Verwaltung, eines Kunden das Kundenportal; nach dem Abmelden
 * wieder die Anmeldemaske. Von der Anmeldemaske aus registrieren sich Neukunden. Ohne
 * Anmeldung ist also weder eine Verwaltungsansicht noch ein Konto erreichbar.
 * <p>
 * Die {@link Anmeldung} bleibt über Ab- und Anmeldungen hinweg bestehen, damit eine
 * Sperre nach Fehlversuchen nicht durch Abmelden umgangen werden kann. Wird das
 * Fenster geschlossen, während jemand angemeldet ist, wird das als Abmeldung
 * protokolliert.
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
     * die Kundenseite der Verwaltung
     */
    private Kundenportal portal;
    /**
     * prüft Anmeldungen
     */
    private Anmeldung anmeldung;
    /**
     * protokolliert die Abmeldung, wenn das Fenster bei bestehender Anmeldung
     * geschlossen wird; null, solange niemand angemeldet ist
     */
    private Runnable abmeldungBeimSchliessen;

    /**
     * Erstellt die Steuerung für ein Fenster.
     * @param buehne das Fenster
     * @param speicher der Datenspeicher, aus dem die Verwaltung geladen wird
     */
    Fenstersteuerung(Stage buehne, Datenspeicher speicher) {
        this.buehne = Objects.requireNonNull(buehne);
        this.speicher = Objects.requireNonNull(speicher);
        buehne.setOnCloseRequest(ereignis -> fensterSchliessen());
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
        portal = new Kundenportal(verwaltung);
        anmeldemaskeZeigen(null);
        return true;
    }

    /**
     * Zeigt die Anmeldemaske.
     * @param benutzername vorzubelegender Benutzername (z. B. nach der Registrierung) oder null
     */
    private void anmeldemaskeZeigen(String benutzername) {
        abmeldungBeimSchliessen = null;
        Dialoge.Geladen<LoginController> login = Dialoge.laden("login.fxml");
        login.controller().setAnmeldung(anmeldung, ergebnis -> {
            login.controller().beenden();
            angemeldet(ergebnis);
        });
        login.controller().setRegistrierung(this::registrierungZeigen);
        if (benutzername != null)
            login.controller().setBenutzername(benutzername);
        szeneSetzen(new Scene(login.wurzel()), 520, 560, false);
        buehne.setTitle("Bank – Anmeldung");
    }

    /**
     * Öffnet nach erfolgreicher Anmeldung die Verwaltung bzw. das Kundenportal.
     * @param ergebnis das erfolgreiche Anmeldeergebnis
     */
    private void angemeldet(Anmeldeergebnis ergebnis) {
        if (ergebnis.isKunde())
            kundenportalOeffnen(ergebnis.kundenzugang());
        else
            hauptfensterZeigen(ergebnis.benutzer());
    }

    /**
     * Zeigt die Registrierung eines Neukunden.
     */
    private void registrierungZeigen() {
        Dialoge.Geladen<RegistrierungController> registrierung = Dialoge.laden("registrierung.fxml");
        registrierung.controller().setPortal(portal, this::anmeldemaskeZeigen);
        szeneSetzen(new Scene(registrierung.wurzel()), 720, 720, false);
        buehne.setTitle("Bank – Konto eröffnen");
    }

    /**
     * Zeigt nach erfolgreicher Anmeldung eines Mitarbeiters das Hauptfenster der Verwaltung.
     * @param benutzer der angemeldete Mitarbeiter
     */
    private void hauptfensterZeigen(Benutzer benutzer) {
        Sitzung sitzung = new Sitzung(verwaltung, benutzer);
        Runnable abmelden = () -> anmeldung.abmelden(benutzer);
        Dialoge.Geladen<HauptfensterController> haupt = Dialoge.laden("hauptfenster.fxml");
        haupt.controller().setSitzung(sitzung, () -> abmelden(abmelden));
        abmeldungBeimSchliessen = abmelden;
        szeneSetzen(new Scene(haupt.wurzel()), 1240, 760, true);
        buehne.setTitle("Bankverwaltung – " + benutzer.getAnzeigename());
    }

    /**
     * Öffnet nach erfolgreicher Anmeldung eines Kunden das Kundenportal. Hat die Bank
     * das Passwort vergeben, muss der Kunde zuerst ein eigenes festlegen; bricht er
     * das ab, wird er wieder abgemeldet.
     * @param zugang der angemeldete Kundenzugang
     */
    private void kundenportalOeffnen(Kundenzugang zugang) {
        Runnable abmelden = () -> anmeldung.abmelden(zugang);
        if (zugang.mussPasswortAendern() && !eigenesPasswortFestlegen(zugang)) {
            abmelden(abmelden);
            return;
        }
        Dialoge.Geladen<KundenportalController> kundenportal = Dialoge.laden("kundenportal.fxml");
        kundenportal.controller().setSitzung(new Kundensitzung(portal, zugang), () -> abmelden(abmelden));
        abmeldungBeimSchliessen = abmelden;
        szeneSetzen(new Scene(kundenportal.wurzel()), 1240, 760, true);
        buehne.setTitle("Kundenportal – " + zugang.getBenutzername());
    }

    /**
     * Lässt den Kunden ein eigenes Passwort festlegen.
     * @param zugang der Kundenzugang
     * @return true, wenn das Passwort geändert wurde
     */
    private boolean eigenesPasswortFestlegen(Kundenzugang zugang) {
        Dialoge.Geladen<PasswortFormularController> formular = Dialoge.laden("passwortFormular.fxml");
        PasswortFormularController eingaben = formular.controller();
        eingaben.setHinweis("Ihr Passwort wurde von der Bank vergeben. Bitte legen Sie jetzt ein eigenes fest "
                + "(mindestens 4 Zeichen). Als bisheriges Passwort geben Sie das Passwort der Bank ein.");
        return Dialoge.formular(buehne, "Eigenes Passwort festlegen", formular.wurzel(), eingaben.getFehlerfeld(),
                () -> {
                    char[] alt = eingaben.getAltesPasswort();
                    char[] neu = eingaben.getNeuesPasswort();
                    try {
                        portal.passwortAendern(zugang, alt, neu);
                    } finally {
                        Arrays.fill(alt, '\0');
                        Arrays.fill(neu, '\0');
                    }
                });
    }

    /**
     * Meldet ab und kehrt zur Anmeldemaske zurück.
     * @param protokollieren protokolliert die Abmeldung
     */
    private void abmelden(Runnable protokollieren) {
        try {
            protokollieren.run();
        } catch (UncheckedIOException e) {
            // Abgemeldet wird trotzdem; nur der Protokolleintrag ist nicht gespeichert
            Dialoge.fehler(buehne, e.getMessage());
        }
        anmeldemaskeZeigen(null);
    }

    /**
     * Wird aufgerufen, wenn das Fenster geschlossen werden soll: Ist jemand
     * angemeldet, wird die Abmeldung protokolliert.
     */
    private void fensterSchliessen() {
        if (abmeldungBeimSchliessen == null)
            return;
        Runnable abmelden = abmeldungBeimSchliessen;
        abmeldungBeimSchliessen = null;
        try {
            abmelden.run();
        } catch (UncheckedIOException e) {
            // Die Anwendung wird beendet; eine Meldung könnte niemand mehr lesen
            System.getLogger(Fenstersteuerung.class.getName())
                    .log(System.Logger.Level.WARNING, "Abmeldung nicht gespeichert: " + e.getMessage());
        }
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
