package bankprojekt.oberflaeche;

import java.util.Objects;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;

/**
 * Die Sitzung eines angemeldeten Benutzers: die Verwaltung, mit der gearbeitet
 * wird, und der Benutzer, in dessen Namen alle Aktionen ausgeführt werden.
 * Eine Sitzung entsteht nur nach erfolgreicher Anmeldung und wird beim Abmelden
 * verworfen.
 *
 * @param verwaltung die Bankverwaltung
 * @param benutzer der angemeldete Benutzer
 * @author m.alharethi
 */
public record Sitzung(Bankverwaltung verwaltung, Benutzer benutzer) {
    /**
     * Prüft, dass Verwaltung und Benutzer gesetzt sind.
     * @throws NullPointerException wenn einer der Werte null ist
     */
    public Sitzung {
        Objects.requireNonNull(verwaltung, "Verwaltung darf nicht null sein");
        Objects.requireNonNull(benutzer, "Benutzer darf nicht null sein");
    }
}
