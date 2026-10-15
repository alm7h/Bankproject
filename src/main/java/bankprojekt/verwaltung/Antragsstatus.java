package bankprojekt.verwaltung;

/**
 * Bearbeitungsstand eines Kontoantrags.
 * @author m.alharethi
 */
public enum Antragsstatus {
    /** Der Antrag wartet auf die Prüfung durch die Mitarbeiter */
    OFFEN("offen"),
    /** Der Antrag wurde genehmigt, das Konto ist eröffnet */
    GENEHMIGT("genehmigt"),
    /** Der Antrag wurde mit Begründung abgelehnt */
    ABGELEHNT("abgelehnt");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt einen Wert mit seiner lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Antragsstatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Liefert die lesbare Bezeichnung.
     * @return die Bezeichnung
     */
    @Override
    public String toString() {
        return bezeichnung;
    }
}
