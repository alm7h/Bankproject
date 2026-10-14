package bankprojekt.verwaltung;

import java.time.Duration;

/**
 * Ergebnis eines Anmeldeversuchs bei der {@link Anmeldung}.
 *
 * @param status wie der Versuch ausgegangen ist
 * @param benutzer der angemeldete Benutzer; nur bei {@link Status#ERFOLGREICH} gesetzt, sonst null
 * @param verbleibendeVersuche wie viele Fehlversuche bis zur Sperre noch möglich sind
 * @param restsperre wie lange die Anmeldung noch gesperrt ist; {@link Duration#ZERO}, wenn sie es nicht ist
 * @author m.alharethi
 */
public record Anmeldeergebnis(Status status, Benutzer benutzer, int verbleibendeVersuche, Duration restsperre) {

    /**
     * Mögliche Ausgänge eines Anmeldeversuchs.
     */
    public enum Status {
        /** Benutzername und Passwort stimmen, der Benutzer ist aktiv */
        ERFOLGREICH,
        /** Benutzername unbekannt oder Passwort falsch */
        FALSCHE_ZUGANGSDATEN,
        /** Zugangsdaten stimmen, der Benutzer ist aber deaktiviert */
        DEAKTIVIERT,
        /** Zu viele Fehlversuche: Die Anmeldung ist vorübergehend gesperrt */
        GESPERRT
    }

    /**
     * Gibt an, ob die Anmeldung geklappt hat.
     * @return true bei {@link Status#ERFOLGREICH}
     */
    public boolean isErfolgreich() {
        return status == Status.ERFOLGREICH;
    }
}
