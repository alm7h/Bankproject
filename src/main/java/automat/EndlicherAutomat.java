package automat;

/**
 * Beschreibt einen Endlichen Automaten aus der Theoretischen Informatik.
 */
public interface EndlicherAutomat {

    /**
     * Versetzt den Automaten in den Startzustand.
     */
    void starten();

    /**
     * Führt einen Zustandswechsel basierend auf dem übergebenen Zeichen durch.
     * @param zeichen das zu verarbeitende Zeichen
     */
    void zustandswechsel(char zeichen);

    /**
     * Gibt an, ob der aktuelle Zustand ein akzeptierender Zustand ist.
     * @return true, wenn der Zustand akzeptierend ist
     */
    boolean istInAkzeptierendemZustand();

    /**
     * Prüft, ob eine Zeichenkette vom Automaten akzeptiert wird.
     * @param zeichenkette die zu prüfende Kette
     * @return true, wenn akzeptiert
     */
    default boolean testen(String zeichenkette) {
        starten(); // Jetzt sind wir im Zustand 0.

        // Führt für jedes Zeichen einen Zustandswechsel durch
        for (int i = 0; i < zeichenkette.length(); i++) {
            zustandswechsel(zeichenkette.charAt(i));
        }

        // Gibt zurück, ob er in einem akzeptierenden Zustand angekommen ist [cite: 30]
        return istInAkzeptierendemZustand();
    }
}