package bankprojekt.verwaltung;

/**
 * Status eines Kunden in der {@link Bankverwaltung}. Kunden werden nie gelöscht,
 * sondern nur deaktiviert ({@link #GESPERRT}).
 * @author m.alharethi
 */
public enum Kundenstatus {
    /** Kunde ist aktiv, für ihn können Konten eröffnet werden */
    AKTIV("aktiv"),
    /** Kunde ist deaktiviert; für ihn werden keine neuen Konten eröffnet */
    GESPERRT("gesperrt");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt einen Status mit seiner lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Kundenstatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Liefert die lesbare Bezeichnung des Status.
     * @return die Bezeichnung, z. B. "aktiv"
     */
    @Override
    public String toString() {
        return bezeichnung;
    }
}
