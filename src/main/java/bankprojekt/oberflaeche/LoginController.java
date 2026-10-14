package bankprojekt.oberflaeche;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import bankprojekt.verwaltung.Anmeldeergebnis;
import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Benutzer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller der Anmeldemaske ({@code login.fxml}).
 * <p>
 * Falsche Zugangsdaten werden unter den Feldern gemeldet und das Passwortfeld
 * geleert. Ist die Anmeldung nach zu vielen Fehlversuchen gesperrt, sind die
 * Felder deaktiviert und ein Countdown zeigt die Restdauer der Sperre.
 * @author m.alharethi
 */
public class LoginController {
    /**
     * Wird vom FXMLLoader beim Laden der Anmeldemaske aufgerufen.
     */
    public LoginController() {
    }

    /** Eingabefeld für den Benutzernamen */
    @FXML
    private TextField benutzername;
    /** Eingabefeld für das Passwort */
    @FXML
    private PasswordField passwort;
    /** Anzeige von Fehlermeldungen und der Sperre */
    @FXML
    private Label meldung;
    /** Knopf „Anmelden" */
    @FXML
    private Button anmeldenKnopf;

    /**
     * prüft die Zugangsdaten
     */
    private Anmeldung anmeldung;
    /**
     * wird nach erfolgreicher Anmeldung mit dem Benutzer aufgerufen
     */
    private Consumer<Benutzer> beiErfolg;
    /**
     * zählt während einer Sperre die Restdauer herunter
     */
    private Timeline countdown;

    /**
     * Übergibt die Anmeldung und die Aktion nach erfolgreicher Anmeldung. Ist die
     * Anmeldung gerade gesperrt (z. B. nach Abmelden während einer Sperre), wird
     * sofort der Countdown gezeigt.
     * @param anmeldung prüft die Zugangsdaten
     * @param beiErfolg wird mit dem angemeldeten Benutzer aufgerufen
     */
    public void setAnmeldung(Anmeldung anmeldung, Consumer<Benutzer> beiErfolg) {
        this.anmeldung = Objects.requireNonNull(anmeldung);
        this.beiErfolg = Objects.requireNonNull(beiErfolg);
        if (!anmeldung.getRestsperre().isZero())
            sperreAnzeigen();
        Platform.runLater(benutzername::requestFocus);
    }

    /**
     * Reaktion auf „Anmelden" (auch per Enter, da Standardknopf).
     */
    @FXML
    private void anmelden() {
        Anmeldeergebnis ergebnis;
        try {
            ergebnis = anmeldung.anmelden(benutzername.getText(), passwort.getText().toCharArray());
        } catch (UncheckedIOException e) {
            meldung.setText(e.getMessage());
            return;
        } finally {
            passwort.clear();
        }
        switch (ergebnis.status()) {
            case ERFOLGREICH -> {
                meldung.setText("");
                beiErfolg.accept(ergebnis.benutzer());
            }
            case FALSCHE_ZUGANGSDATEN -> fehlversuchMelden("Benutzername oder Passwort ist falsch.",
                    ergebnis.verbleibendeVersuche());
            case DEAKTIVIERT -> fehlversuchMelden(
                    "Dieser Zugang ist deaktiviert. Bitte wenden Sie sich an eine Administratorin "
                            + "oder einen Administrator.", ergebnis.verbleibendeVersuche());
            case GESPERRT -> sperreAnzeigen();
        }
    }

    /**
     * Meldet einen Fehlversuch mit der Zahl der verbleibenden Versuche.
     * @param text die Meldung
     * @param verbleibend verbleibende Versuche bis zur Sperre
     */
    private void fehlversuchMelden(String text, int verbleibend) {
        meldung.setText(text + " Noch " + verbleibend + (verbleibend == 1 ? " Versuch" : " Versuche")
                + " bis zur vorübergehenden Sperre.");
        passwort.requestFocus();
    }

    /**
     * Sperrt die Eingabe und zeigt die Restdauer der Sperre sekundengenau an,
     * bis sie abgelaufen ist.
     */
    private void sperreAnzeigen() {
        eingabeSperren(true);
        if (countdown != null)
            countdown.stop();
        countdown = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> sperreAktualisieren()));
        countdown.setCycleCount(Timeline.INDEFINITE);
        sperreAktualisieren();
        countdown.play();
    }

    /**
     * Aktualisiert die Anzeige der Sperre und gibt die Eingabe nach Ablauf wieder frei.
     */
    private void sperreAktualisieren() {
        Duration rest = anmeldung.getRestsperre();
        if (rest.isZero()) {
            countdown.stop();
            eingabeSperren(false);
            meldung.setText("Sie können sich wieder anmelden.");
            benutzername.requestFocus();
        } else {
            long sekunden = (rest.toMillis() + 999) / 1000;   // aufrunden
            meldung.setText("Zu viele Fehlversuche. Die Anmeldung ist noch " + sekunden
                    + " Sekunden gesperrt.");
        }
    }

    /**
     * Sperrt die Eingabefelder oder gibt sie frei.
     * @param gesperrt true zum Sperren
     */
    private void eingabeSperren(boolean gesperrt) {
        benutzername.setDisable(gesperrt);
        passwort.setDisable(gesperrt);
        anmeldenKnopf.setDisable(gesperrt);
    }

    /**
     * Hält den Countdown an, wenn die Anmeldemaske verlassen wird.
     */
    public void beenden() {
        if (countdown != null)
            countdown.stop();
    }
}
