package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Der Zugang eines Kunden zum Kundenportal: Benutzername und Passwort (nur als
 * Hash, siehe {@link Passwortschutz}).
 * <p>
 * Ein Zugang entsteht entweder bei der Registrierung eines Neukunden – dann ist er
 * zunächst noch keinem Kunden zugeordnet, bis die Mitarbeiter den Antrag genehmigen –
 * oder durch einen Mitarbeiter für einen bestehenden Kunden; dann muss der Kunde das
 * Startpasswort bei der ersten Anmeldung ändern. Benutzernamen sind über Mitarbeiter
 * und Kunden hinweg eindeutig. Geändert wird ein Zugang nur über die
 * {@link Bankverwaltung} bzw. das {@link Kundenportal}.
 * @author m.alharethi
 */
public final class Kundenzugang implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * eindeutiger Benutzername in Kleinbuchstaben
     */
    private final String benutzername;
    /**
     * Hash des Passworts
     */
    private String passwortHash;
    /**
     * Kundennummer des zugehörigen Kunden; null, solange der Neukundenantrag nicht genehmigt ist
     */
    private String kundennummer;
    /**
     * ob der Kunde sein Passwort bei der nächsten Anmeldung ändern muss
     */
    private boolean passwortAendern;
    /**
     * Zeitpunkt der Anlage
     */
    private final LocalDateTime angelegtAm;
    /**
     * Zeitpunkt der letzten erfolgreichen Anmeldung, null wenn es noch keine gab
     */
    private LocalDateTime letzteAnmeldung;

    /**
     * Legt einen Zugang an.
     * @param benutzername der (bereits geprüfte) Benutzername
     * @param passwortHash Hash des Passworts
     * @param kundennummer Kundennummer oder null (Neukunde, noch nicht genehmigt)
     * @param passwortAendern ob das Passwort bei der ersten Anmeldung geändert werden muss
     * @param angelegtAm Zeitpunkt der Anlage
     */
    Kundenzugang(String benutzername, String passwortHash, String kundennummer, boolean passwortAendern,
                 LocalDateTime angelegtAm) {
        this.benutzername = benutzername;
        this.passwortHash = passwortHash;
        this.kundennummer = kundennummer;
        this.passwortAendern = passwortAendern;
        this.angelegtAm = angelegtAm;
    }

    /**
     * Prüft ein eingegebenes Passwort.
     * @param passwort das eingegebene Passwort
     * @return true, wenn es zum gespeicherten Hash passt
     */
    boolean passwortPruefen(char[] passwort) {
        return Passwortschutz.pruefen(passwort, passwortHash);
    }

    /**
     * Liefert den gespeicherten Passwort-Hash.
     * @return der Hash (nie das Passwort selbst)
     */
    String getPasswortHash() {
        return passwortHash;
    }

    /**
     * Setzt ein neues Passwort.
     * @param neuerHash Hash des neuen Passworts
     * @param mussGeaendertWerden ob der Kunde es bei der nächsten Anmeldung ändern muss
     */
    void setPasswort(String neuerHash, boolean mussGeaendertWerden) {
        this.passwortHash = neuerHash;
        this.passwortAendern = mussGeaendertWerden;
    }

    /**
     * Ordnet den Zugang einem Kunden zu.
     * @param kundennummer die Kundennummer
     */
    void setKundennummer(String kundennummer) {
        this.kundennummer = kundennummer;
    }

    /**
     * Vermerkt eine erfolgreiche Anmeldung.
     * @param zeitpunkt Zeitpunkt der Anmeldung
     */
    void setLetzteAnmeldung(LocalDateTime zeitpunkt) {
        this.letzteAnmeldung = zeitpunkt;
    }

    /**
     * Liefert den Benutzernamen.
     * @return der Benutzername
     */
    public String getBenutzername() {
        return benutzername;
    }

    /**
     * Liefert die Kundennummer des zugehörigen Kunden.
     * @return die Kundennummer oder null, solange der Neukundenantrag nicht genehmigt ist
     */
    public String getKundennummer() {
        return kundennummer;
    }

    /**
     * Gibt an, ob der Zugang schon zu einem Kunden gehört (Antrag genehmigt).
     * @return true, wenn eine Kundennummer zugeordnet ist
     */
    public boolean istKunde() {
        return kundennummer != null;
    }

    /**
     * Gibt an, ob der Kunde sein Passwort bei der nächsten Anmeldung ändern muss
     * (nach einem von Mitarbeitern gesetzten Start- oder Ersatzpasswort).
     * @return true, wenn das Passwort geändert werden muss
     */
    public boolean mussPasswortAendern() {
        return passwortAendern;
    }

    /**
     * Liefert den Zeitpunkt der Anlage.
     * @return der Zeitpunkt der Anlage
     */
    public LocalDateTime getAngelegtAm() {
        return angelegtAm;
    }

    /**
     * Liefert den Zeitpunkt der letzten erfolgreichen Anmeldung.
     * @return der Zeitpunkt oder null, wenn sich der Kunde noch nie angemeldet hat
     */
    public LocalDateTime getLetzteAnmeldung() {
        return letzteAnmeldung;
    }

    /**
     * Liefert den Benutzernamen.
     * @return der Benutzername
     */
    @Override
    public String toString() {
        return benutzername;
    }
}
