package bankprojekt.oberflaeche;

import java.util.Objects;

import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;

/**
 * Die Sitzung eines angemeldeten Kunden im Kundenportal: das Portal, über das alle
 * Aktionen laufen, und der Zugang, in dessen Namen sie ausgeführt werden. Eine
 * Kundensitzung entsteht nur nach erfolgreicher Anmeldung und wird beim Abmelden verworfen.
 *
 * @param portal das Kundenportal
 * @param zugang der angemeldete Kundenzugang
 * @author m.alharethi
 */
public record Kundensitzung(Kundenportal portal, Kundenzugang zugang) {
    /**
     * Prüft, dass Portal und Zugang gesetzt sind.
     * @throws NullPointerException wenn einer der Werte null ist
     */
    public Kundensitzung {
        Objects.requireNonNull(portal, "Portal darf nicht null sein");
        Objects.requireNonNull(zugang, "Zugang darf nicht null sein");
    }
}
