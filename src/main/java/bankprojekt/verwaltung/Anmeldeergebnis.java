package bankprojekt.verwaltung;

import java.time.Duration;

/**
 * Ergebnis eines Anmeldeversuchs bei der {@link Anmeldung}. Mitarbeiter und Kunden
 * melden sich über dieselbe Maske an; bei Erfolg ist genau einer von {@code benutzer}
 * (Mitarbeiter) und {@code kundenzugang} (Kunde) gesetzt.
 *
 * @param status wie der Versuch ausgegangen ist
 * @param benutzer der angemeldete Mitarbeiter; sonst null
 * @param kundenzugang der angemeldete Kunde; sonst null
 * @param verbleibendeVersuche wie viele Fehlversuche bis zur Sperre noch möglich sind
 * @param restsperre wie lange die Anmeldung noch gesperrt ist; {@link Duration#ZERO}, wenn sie es nicht ist
 * @author m.alharethi
 */
public record Anmeldeergebnis(Status status, Benutzer benutzer, Kundenzugang kundenzugang,
                              int verbleibendeVersuche, Duration restsperre) {

    /**
     * Mögliche Ausgänge eines Anmeldeversuchs.
     */
    public enum Status {
        /** Benutzername und Passwort stimmen, der Zugang ist aktiv */
        ERFOLGREICH,
        /** Benutzername unbekannt oder Passwort falsch */
        FALSCHE_ZUGANGSDATEN,
        /** Zugangsdaten stimmen, der Zugang ist aber deaktiviert (bzw. der Kunde gesperrt) */
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

    /**
     * Gibt an, ob sich ein Kunde (und nicht ein Mitarbeiter) angemeldet hat.
     * @return true, wenn ein Kundenzugang angemeldet wurde
     */
    public boolean isKunde() {
        return kundenzugang != null;
    }
}
