package bankprojekt.oberflaeche;

/**
 * Gemeinsame Schnittstelle der Controller für die Ansichten des Hauptfensters
 * (Kunden, Konten, ...). Das {@link HauptfensterController Hauptfenster} übergibt
 * nach dem Laden die {@link Sitzung} und frischt die Ansicht bei jedem Wechsel auf.
 * @author m.alharethi
 */
public interface Ansicht {
    /**
     * Übergibt die Sitzung des angemeldeten Benutzers. Wird einmal direkt nach
     * dem Laden der Ansicht aufgerufen.
     * @param sitzung die Sitzung
     */
    void setSitzung(Sitzung sitzung);

    /**
     * Liest die angezeigten Daten neu aus der Verwaltung.
     */
    void aktualisieren();
}
