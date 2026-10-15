package bankprojekt.oberflaeche;

/**
 * Gemeinsame Schnittstelle der Controller für die Ansichten des Kundenportals
 * (Meine Konten, Anträge, Meine Daten). Der {@link KundenportalController} übergibt
 * nach dem Laden die {@link Kundensitzung} und frischt die Ansicht bei jedem Wechsel auf.
 * @author m.alharethi
 */
public interface Portalansicht {
    /**
     * Übergibt die Sitzung des angemeldeten Kunden. Wird einmal direkt nach dem Laden
     * der Ansicht aufgerufen.
     * @param sitzung die Kundensitzung
     */
    void setSitzung(Kundensitzung sitzung);

    /**
     * Liest die angezeigten Daten neu aus dem Kundenportal.
     */
    void aktualisieren();
}
