package bankprojekt.verwaltung;

/**
 * Art einer Aktion, die im Protokoll der {@link Bankverwaltung} festgehalten wird.
 * @author m.alharethi
 */
public enum Protokollaktion {
    /** Erstes Einrichten der Verwaltung mit dem Startbenutzer */
    ERSTEINRICHTUNG("Ersteinrichtung"),
    /** Erfolgreiche Anmeldung */
    ANMELDUNG("Anmeldung"),
    /** Gescheiterter Anmeldeversuch */
    ANMELDUNG_FEHLGESCHLAGEN("Anmeldung fehlgeschlagen"),
    /** Anmeldung nach zu vielen Fehlversuchen vorübergehend gesperrt */
    ANMELDUNG_GESPERRT("Anmeldung gesperrt"),
    /** Abmeldung */
    ABMELDUNG("Abmeldung"),
    /** Neuer Kunde angelegt */
    KUNDE_ANGELEGT("Kunde angelegt"),
    /** Kundendaten geändert */
    KUNDE_BEARBEITET("Kunde bearbeitet"),
    /** Kunde deaktiviert (gesperrt) */
    KUNDE_DEAKTIVIERT("Kunde deaktiviert"),
    /** Kunde wieder aktiviert */
    KUNDE_AKTIVIERT("Kunde aktiviert"),
    /** Neues Konto eröffnet */
    KONTO_EROEFFNET("Konto eröffnet"),
    /** Konto gesperrt */
    KONTO_GESPERRT("Konto gesperrt"),
    /** Konto entsperrt */
    KONTO_ENTSPERRT("Konto entsperrt"),
    /** Konto aufgelöst */
    KONTO_AUFGELOEST("Konto aufgelöst"),
    /** Neuer Benutzer (Mitarbeiter) angelegt */
    BENUTZER_ANGELEGT("Benutzer angelegt"),
    /** Benutzerdaten geändert */
    BENUTZER_BEARBEITET("Benutzer bearbeitet"),
    /** Passwort eines Benutzers zurückgesetzt */
    PASSWORT_ZURUECKGESETZT("Passwort zurückgesetzt"),
    /** Benutzer deaktiviert */
    BENUTZER_DEAKTIVIERT("Benutzer deaktiviert"),
    /** Benutzer wieder aktiviert */
    BENUTZER_AKTIVIERT("Benutzer aktiviert");

    /**
     * Lesbare Bezeichnung der Aktion für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt eine Aktion mit ihrer lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Protokollaktion(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Liefert die lesbare Bezeichnung der Aktion.
     * @return die Bezeichnung, z. B. "Konto gesperrt"
     */
    @Override
    public String toString() {
        return bezeichnung;
    }
}
