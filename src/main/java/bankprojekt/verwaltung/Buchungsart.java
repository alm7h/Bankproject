package bankprojekt.verwaltung;

/**
 * Art einer Buchung im Kontoauszug.
 * @author m.alharethi
 */
public enum Buchungsart {
    /** Einzahlung am Schalter */
    BAREINZAHLUNG("Bareinzahlung"),
    /** Auszahlung am Schalter */
    BARAUSZAHLUNG("Barauszahlung"),
    /** Ausgehende Überweisung an ein anderes Girokonto */
    UEBERWEISUNG_AUSGANG("Überweisung (Ausgang)"),
    /** Eingehende Überweisung von einem anderen Girokonto */
    UEBERWEISUNG_EINGANG("Überweisung (Eingang)"),
    /** Umbuchung auf ein anderes eigenes Konto */
    UMBUCHUNG_AUSGANG("Umbuchung (Ausgang)"),
    /** Umbuchung von einem anderen eigenen Konto */
    UMBUCHUNG_EINGANG("Umbuchung (Eingang)");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt einen Wert mit seiner lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Buchungsart(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Gibt an, ob die Buchung Geld vom Konto abzieht.
     * @return true für Barauszahlung, ausgehende Überweisung und ausgehende Umbuchung
     */
    public boolean istAbgang() {
        return this == BARAUSZAHLUNG || this == UEBERWEISUNG_AUSGANG || this == UMBUCHUNG_AUSGANG;
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
