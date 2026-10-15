package bankprojekt.verwaltung;

import java.time.LocalDate;

/**
 * Die bearbeitbaren Stammdaten eines Kunden, wie sie beim Anlegen und Bearbeiten
 * eingegeben werden. Die Daten werden schon beim Erzeugen geprüft und bereinigt
 * (Leerzeichen am Rand entfernt): Ein {@code Kundendaten}-Objekt ist immer gültig.
 * <p>
 * Pflichtfelder sind Vorname, Nachname, Geburtsdatum, Adresse und E-Mail; die
 * Telefonnummer ist freiwillig.
 *
 * @param vorname Vorname
 * @param nachname Nachname
 * @param geburtsdatum Geburtsdatum (nicht in der Zukunft, nicht vor 1900)
 * @param adresse Anschrift
 * @param telefon Telefonnummer, "" wenn keine angegeben ist
 * @param email E-Mail-Adresse
 * @author m.alharethi
 */
public record Kundendaten(String vorname, String nachname, LocalDate geburtsdatum,
                          String adresse, String telefon, String email) {
    /**
     * Prüft und bereinigt die Kundendaten.
     * @throws IllegalArgumentException wenn ein Pflichtfeld fehlt oder ein Wert ungültig ist
     */
    public Kundendaten {
        vorname = Eingabepruefung.pflichtfeld(vorname, "Vorname");
        nachname = Eingabepruefung.pflichtfeld(nachname, "Nachname");
        geburtsdatum = Eingabepruefung.geburtsdatum(geburtsdatum, LocalDate.now());
        adresse = Eingabepruefung.pflichtfeld(adresse, "Adresse");
        telefon = Eingabepruefung.telefon(telefon);
        email = Eingabepruefung.email(email);
    }
}
