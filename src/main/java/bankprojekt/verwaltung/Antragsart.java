package bankprojekt.verwaltung;

/**
 * Art eines Kontoantrags im Kundenportal.
 * @author m.alharethi
 */
public enum Antragsart {
    /** Antrag eines neuen Kunden: Kunde und Konto werden bei Genehmigung angelegt */
    NEUKUNDE("Neukunde"),
    /** Antrag eines bestehenden Kunden auf ein weiteres Konto */
    ZUSATZKONTO("Weiteres Konto");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt einen Wert mit seiner lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Antragsart(String bezeichnung) {
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
