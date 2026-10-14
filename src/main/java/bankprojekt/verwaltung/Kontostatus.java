package bankprojekt.verwaltung;

/**
 * Status eines Kontos in der {@link Bankverwaltung}.
 * @author m.alharethi
 */
public enum Kontostatus {
    /** Konto ist offen und nicht gesperrt */
    AKTIV("aktiv"),
    /** Konto ist gesperrt; es sind keine Buchungen möglich */
    GESPERRT("gesperrt"),
    /** Konto ist aufgelöst; es bleibt nur zur Nachvollziehbarkeit erhalten */
    AUFGELOEST("aufgelöst");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt einen Status mit seiner lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Kontostatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Liefert die lesbare Bezeichnung des Status.
     * @return die Bezeichnung, z. B. "aufgelöst"
     */
    @Override
    public String toString() {
        return bezeichnung;
    }
}
