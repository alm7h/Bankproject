package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Ein unveränderlicher Eintrag im Protokoll der {@link Bankverwaltung}:
 * wer hat wann was getan.
 *
 * @param zeitpunkt Zeitpunkt der Aktion
 * @param benutzer Benutzername des Ausführenden (bei gescheiterten Anmeldungen der
 *                 eingegebene Name, bei der Ersteinrichtung "System")
 * @param aktion Art der Aktion
 * @param details nähere Beschreibung, z. B. die betroffene Kunden- oder Kontonummer
 * @author m.alharethi
 */
public record Protokolleintrag(LocalDateTime zeitpunkt, String benutzer,
                               Protokollaktion aktion, String details) implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
