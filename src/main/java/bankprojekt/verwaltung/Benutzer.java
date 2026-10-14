package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Ein Benutzer (Mitarbeiter) der {@link Bankverwaltung}. Das Passwort wird nur
 * als Hash gespeichert (siehe {@link Passwortschutz}).
 * <p>
 * Benutzer werden nie gelöscht, sondern nur deaktiviert; ein deaktivierter
 * Benutzer kann sich nicht anmelden. Geändert wird ein Benutzer nur über die
 * {@link Bankverwaltung}.
 * @author m.alharethi
 */
public final class Benutzer implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * eindeutiger Benutzername in Kleinbuchstaben, z. B. "m.alharethi"
     */
    private final String benutzername;
    /**
     * angezeigter Name
     */
    private String anzeigename;
    /**
     * Hash des Passworts
     */
    private String passwortHash;
    /**
     * ob sich der Benutzer anmelden darf
     */
    private boolean aktiv = true;
    /**
     * Zeitpunkt der Anlage
     */
    private final LocalDateTime angelegtAm;
    /**
     * Zeitpunkt der letzten erfolgreichen Anmeldung, null wenn es noch keine gab
     */
    private LocalDateTime letzteAnmeldung;

    /**
     * Legt einen aktiven Benutzer an.
     * @param benutzername der (bereits geprüfte) Benutzername
     * @param anzeigename der angezeigte Name
     * @param passwortHash Hash des Passworts
     * @param angelegtAm Zeitpunkt der Anlage
     */
    Benutzer(String benutzername, String anzeigename, String passwortHash, LocalDateTime angelegtAm) {
        this.benutzername = benutzername;
        this.anzeigename = anzeigename;
        this.passwortHash = passwortHash;
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
     * Setzt den Anzeigenamen.
     * @param anzeigename der neue Anzeigename
     */
    void setAnzeigename(String anzeigename) {
        this.anzeigename = anzeigename;
    }

    /**
     * Setzt einen neuen Passwort-Hash.
     * @param passwortHash der neue Hash
     */
    void setPasswortHash(String passwortHash) {
        this.passwortHash = passwortHash;
    }

    /**
     * Aktiviert oder deaktiviert den Benutzer.
     * @param aktiv true zum Aktivieren
     */
    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
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
     * Liefert den Anzeigenamen.
     * @return der Anzeigename
     */
    public String getAnzeigename() {
        return anzeigename;
    }

    /**
     * Gibt an, ob sich der Benutzer anmelden darf.
     * @return true, wenn der Benutzer aktiv ist
     */
    public boolean isAktiv() {
        return aktiv;
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
     * @return der Zeitpunkt oder null, wenn sich der Benutzer noch nie angemeldet hat
     */
    public LocalDateTime getLetzteAnmeldung() {
        return letzteAnmeldung;
    }

    /**
     * Liefert Anzeige- und Benutzernamen.
     * @return z. B. "M. Alharethi (m.alharethi)"
     */
    @Override
    public String toString() {
        return anzeigename + " (" + benutzername + ")";
    }
}
