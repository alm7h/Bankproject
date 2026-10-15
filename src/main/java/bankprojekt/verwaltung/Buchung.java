package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import bankprojekt.basisdaten.Geldbetrag;

/**
 * Eine Zeile im Kontoauszug: eine Buchung auf einem Konto.
 *
 * @param zeitpunkt Zeitpunkt der Buchung
 * @param kontonummer das gebuchte Konto
 * @param art Art der Buchung
 * @param betrag gebuchter Betrag in der Kontowährung; negativ für Abgänge
 * @param saldoDanach Kontostand nach der Buchung
 * @param text Buchungstext, z. B. Empfänger und Verwendungszweck
 * @param ausgefuehrtVon Benutzername dessen, der gebucht hat (Kunde oder Mitarbeiter)
 * @author m.alharethi
 */
public record Buchung(LocalDateTime zeitpunkt, long kontonummer, Buchungsart art, Geldbetrag betrag,
                      Geldbetrag saldoDanach, String text, String ausgefuehrtVon) implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
